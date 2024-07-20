package org.nguyendevs.suddendeath.world;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.manager.EventManager;

import java.util.Objects;
import java.util.Random;

public class BloodMoon extends WorldEventHandler {

	public BloodMoon(World world) {
		super(world, 3 * 20, EventManager.WorldStatus.BLOOD_MOON);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!event.getEntity().getWorld().equals(getWorld())) return;

		if (event.getEntity() instanceof Player) {
			event.setDamage(event.getDamage() * (1 + Feature.BLOOD_MOON.getDouble("damage-percent") / 100));
			Player player = (Player) event.getEntity();
			if (event.getDamage() < player.getHealth()) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) Feature.BLOOD_MOON.getDouble("slow-duration") * 20, 2));
				player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 64, .1, .3, .1, .2,
						Material.REDSTONE_BLOCK.createBlockData());
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		final LivingEntity entity = event.getEntity();
		if (!entity.getWorld().equals(getWorld()) || !(entity instanceof Creature)) return;

		if (!(entity instanceof Zombie) && !(entity instanceof Drowned)) {
			event.setCancelled(true);

			Location loc = entity.getLocation();
			boolean isWaterNearby = isWaterNearby(loc);

			// Tạo zombie hoặc drowned thay thế
			EntityType type = isWaterNearby ? EntityType.DROWNED : EntityType.ZOMBIE;
			Zombie zombie = (Zombie) entity.getWorld().spawnEntity(loc, type);
			for (PotionEffectType effectType : new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.INCREASE_DAMAGE, PotionEffectType.DAMAGE_RESISTANCE}) {
				zombie.addPotionEffect(new PotionEffect(effectType, 1000000,
						(int) Feature.BLOOD_MOON.getDouble(effectType.getName().toLowerCase().replace("_", "-"))));
			}
			zombie.setMetadata("BloodmoonMob", new FixedMetadataValue(SuddenDeath.plugin, Boolean.TRUE));

			new BukkitRunnable() {
				double ti = 0.0D;
				final Location loc = zombie.getLocation().clone();
				final Random r = new Random();

				public void run() {
					if (this.ti > 2.0D) {
						cancel();
						return;
					}
					for (int j = 0; j < 2; j++) {
						this.ti += 0.12D;
						for (double i = 0.0D; i < 6.283185307179586D; i += 0.39269908169872414D) {
							if (this.r.nextDouble() >= 0.4D) {
								// Add a height offset of 2 blocks to the y-coordinate
								Location loc1 = this.loc.clone().add(Math.cos(i) * 0.8D, this.ti + 2.0D, Math.sin(i) * 0.8D);
								Objects.requireNonNull(loc1.getWorld()).spawnParticle(Particle.REDSTONE, loc1, 0, new Particle.DustOptions(Color.BLACK, 1.0F));
							}
						}
					}
				}
			}.runTaskTimer(SuddenDeath.plugin, 0L, 1L);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.getEntity() instanceof Creeper && event.getEntity().hasMetadata("BloodmoonMob")) {
			for (PotionEffectType type : new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.INCREASE_DAMAGE,
					PotionEffectType.DAMAGE_RESISTANCE}) {
				((Creeper) event.getEntity()).removePotionEffect(type);
			}
		}
	}

	@Override
	public void run() {
		// Biến tất cả các quái vật hiện có thành Zombie hoặc Drowned
		for (Entity entity : getWorld().getEntities()) {
			if (entity instanceof Monster && !(entity instanceof Zombie || entity instanceof Drowned)) {
				Location loc = entity.getLocation();
				boolean isWaterNearby = isWaterNearby(loc);
				entity.remove();

				// Tạo zombie hoặc drowned thay thế
				EntityType type = isWaterNearby ? EntityType.DROWNED : EntityType.ZOMBIE;
				Zombie zombie = (Zombie) getWorld().spawnEntity(loc, type);
				for (PotionEffectType effectType : new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.INCREASE_DAMAGE, PotionEffectType.DAMAGE_RESISTANCE}) {
					zombie.addPotionEffect(new PotionEffect(effectType, 1000000,
							(int) Feature.BLOOD_MOON.getDouble(effectType.getName().toLowerCase().replace("_", "-"))));
				}
				zombie.setMetadata("BloodmoonMob", new FixedMetadataValue(SuddenDeath.plugin, Boolean.TRUE));
			}
		}
	}

	private boolean isWaterNearby(Location loc) {
		int radius = 5;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					Material material = loc.clone().add(x, y, z).getBlock().getType();
					if (material == Material.WATER) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void close() {
		super.close();
		// clear potion effects from creepers
		for (Entity entity : getWorld().getEntities()) {
			if (entity instanceof Creeper && entity.hasMetadata("BloodmoonMob")) {
				Creeper creeper = (Creeper) entity;
				for (PotionEffectType type : new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.INCREASE_DAMAGE,
						PotionEffectType.DAMAGE_RESISTANCE}) {
					creeper.removePotionEffect(type);
				}
			}
		}
	}
}
