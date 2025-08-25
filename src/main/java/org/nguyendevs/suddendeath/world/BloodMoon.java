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
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;

import java.util.logging.Level;

public class BloodMoon extends WorldEventHandler {
	private static final String BLOODMOON_METADATA = "BloodmoonMob";
	private static final int WATER_CHECK_RADIUS = 5;
	private static final int PARTICLE_COUNT = 64;
	private static final int EFFECT_DURATION_TICKS = 1_000_000;
	private static final PotionEffectType[] ENHANCED_EFFECTS = {
			PotionEffectType.SPEED,
			PotionEffectType.INCREASE_DAMAGE,
			PotionEffectType.DAMAGE_RESISTANCE
	};

	public BloodMoon(World world) {
		super(world, 3 * 20, WorldStatus.BLOOD_MOON);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player) || !event.getEntity().getWorld().equals(getWorld())) {
			return;
		}

		try {
			Player player = (Player) event.getEntity();
			if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EVENT)) {
				return;
			}
			double damageMultiplier = 1 + (Feature.BLOOD_MOON.getDouble("damage-percent") / 100.0);
			event.setDamage(event.getDamage() * damageMultiplier);

			if (event.getDamage() < player.getHealth()) {
				int slowDuration = (int) (Feature.BLOOD_MOON.getDouble("slow-duration") * 20);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, 2));
				player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0),
						PARTICLE_COUNT, 0.1, 0.3, 0.1, 0.2, Material.REDSTONE_BLOCK.createBlockData());
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling entity damage in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		LivingEntity entity = event.getEntity();
		if (!entity.getWorld().equals(getWorld()) || !(entity instanceof Creature)) {
			return;
		}

		// Skip if entity is an NPC
		if (isNPC(entity)) {
			return;
		}

		try {
			Location loc = event.getEntity().getLocation();
			if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowedAtLocation(loc, CustomFlag.SDS_EVENT)) {
				return;
			}
			if (isWaterNearby(loc) && entity instanceof Zombie) {
				event.setCancelled(true);
				return;
			}

			if (!(entity instanceof Zombie)) {
				event.setCancelled(true);
				spawnEnhancedZombie(loc);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling creature spawn in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Creeper) || !entity.hasMetadata(BLOODMOON_METADATA)) {
			return;
		}

		try {
			Creeper creeper = (Creeper) event.getEntity();
			for (PotionEffectType effectType : ENHANCED_EFFECTS) {
				creeper.removePotionEffect(effectType);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling entity explosion in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	@Override
	public void run() {
		try {
			for (Entity entity : getWorld().getEntities()) {
				if (entity instanceof Monster && !(entity instanceof Zombie)) {
					// Skip if entity is an NPC
					if (isNPC(entity)) {
						continue;
					}

					Location loc = entity.getLocation();
					if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowedAtLocation(loc, CustomFlag.SDS_EVENT)) {
						continue;
					}
					if (!isWaterNearby(loc)) {
						entity.remove();
						spawnEnhancedZombie(loc);
					}
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Error running Blood Moon task in world: " + getWorld().getName(), e);
		}
	}

	@Override
	public void close() {
		super.close();
		try {
			for (Entity entity : getWorld().getEntities()) {
				if (entity instanceof Creeper && entity.hasMetadata(BLOODMOON_METADATA)) {
					Creeper creeper = (Creeper) entity;
					for (PotionEffectType effectType : ENHANCED_EFFECTS) {
						creeper.removePotionEffect(effectType);
					}
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error cleaning up Blood Moon effects in world: " + getWorld().getName(), e);
		}
	}

	/**
	 * Check if an entity is an NPC from various NPC plugins
	 */
	private boolean isNPC(Entity entity) {
		try {
			// Citizens NPC check
			if (entity.hasMetadata("NPC")) {
				return true;
			}

			// Citizens API check (if available)
			try {
				Class.forName("net.citizensnpcs.api.CitizensAPI");
				Object citizensAPI = Class.forName("net.citizensnpcs.api.CitizensAPI").getMethod("getNPCRegistry").invoke(null);
				boolean isNPC = (boolean) citizensAPI.getClass().getMethod("isNPC", Entity.class).invoke(citizensAPI, entity);
				if (isNPC) return true;
			} catch (Exception ignored) {
				// Citizens not available, continue with other checks
			}

			// Check for common NPC metadata keys
			String[] npcMetadataKeys = {
					"npc", "NPC", "citizens-npc", "znpcs", "fancynpcs",
					"simplenpc", "npclib", "custom-npc", "denizen-npc"
			};

			for (String key : npcMetadataKeys) {
				if (entity.hasMetadata(key)) {
					return true;
				}
			}

			// Check entity name patterns (many NPC plugins use specific naming)
			String customName = entity.getCustomName();
			if (customName != null && !customName.isEmpty()) {
				// Check if entity has custom name but no AI (typical NPC behavior)
				if (entity instanceof Creature) {
					Creature creature = (Creature) entity;
					if (creature.getTarget() == null && !creature.isAware()) {
						return true;
					}
				}
			}

			// Check for persistent entity that doesn't despawn (common NPC trait)
			if (entity.isPersistent() && entity.getCustomName() != null) {
				return true;
			}

			// Additional check for entities with disabled AI
			if (entity instanceof Mob) {
				Mob mob = (Mob) entity;
				if (!mob.isAware()) {
					return true;
				}
			}

		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking if entity is NPC: " + entity.getType(), e);
		}

		return false;
	}

	private void spawnEnhancedZombie(Location location) {
		try {

			if (isWaterNearby(location)) return;

			if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowedAtLocation(location, CustomFlag.SDS_EVENT)) {
				return;
			}
			Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
			for (PotionEffectType effectType : ENHANCED_EFFECTS) {
				int amplifier = (int) Feature.BLOOD_MOON.getDouble(
						effectType.getName().toLowerCase().replace("_", "-"));
				zombie.addPotionEffect(new PotionEffect(effectType, EFFECT_DURATION_TICKS, amplifier));
			}
			zombie.setMetadata(BLOODMOON_METADATA, new FixedMetadataValue(SuddenDeath.getInstance(), true));

			new ZombieSpawnEffectTask(zombie.getLocation().clone())
					.runTaskTimer(SuddenDeath.getInstance(), 0L, 1L);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error spawning enhanced zombie in Blood Moon at: " + location, e);
		}
	}


	private boolean isWaterNearby(Location location) {
		try {
			for (int x = -WATER_CHECK_RADIUS; x <= WATER_CHECK_RADIUS; x++) {
				for (int y = -WATER_CHECK_RADIUS; y <= WATER_CHECK_RADIUS; y++) {
					for (int z = -WATER_CHECK_RADIUS; z <= WATER_CHECK_RADIUS; z++) {
						if (location.clone().add(x, y, z).getBlock().getType() == Material.WATER) {
							return true;
						}
					}
				}
			}
			return false;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking for water nearby in Blood Moon at: " + location, e);
			return false;
		}
	}

	private static class ZombieSpawnEffectTask extends BukkitRunnable {
		private final Location location;
		private double time = 0.0;
		ZombieSpawnEffectTask(Location location) {
			this.location = location;
		}
		@Override
		public void run() {
			try {
				if (time > 2.0) {
					cancel();
					return;
				}
				World world = location.getWorld();
				if (world == null) {
					cancel();
					return;
				}
				for (int j = 0; j < 2; j++) {
					time += 0.12;
					for (double angle = 0.0; angle < 2 * Math.PI; angle += Math.PI / 8) {
						if (getRandom().nextDouble() < 0.4) {
							continue;
						}
						Location effectLoc = location.clone().add(
								Math.cos(angle) * 0.8, time, Math.sin(angle) * 0.8);
						world.spawnParticle(Particle.REDSTONE, effectLoc, 0,
								new Particle.DustOptions(Color.BLACK, 1.0F));
					}
				}
			} catch (Exception e) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Error in zombie spawn effect task at: " + location, e);
				cancel();
			}
		}
	}
}