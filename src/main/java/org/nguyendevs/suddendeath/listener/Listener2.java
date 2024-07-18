package org.nguyendevs.suddendeath.listener;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.ExperienceCalculator;
import org.nguyendevs.suddendeath.util.NoInteractItemEntity;
import org.nguyendevs.suddendeath.util.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class Listener2 implements Listener {
	private static final Random random = new Random();

	public Listener2() {
		new BukkitRunnable() {
			public void run() {
				for (World w : Bukkit.getWorlds())
					if (Feature.WITHER_RUSH.isEnabled(w))
						for (WitherSkeleton ws : w.getEntitiesByClass(WitherSkeleton.class))
							if (ws.getTarget() instanceof Player)
								Loops.loop6s_wither_skeleton(ws);
			}
		}.runTaskTimer(SuddenDeath.plugin, 20, 120);

		new BukkitRunnable() {
			public void run() {
				for (World w : Bukkit.getWorlds()) {
					if (Feature.UNDEAD_GUNNERS.isEnabled(w))
						for (Zombie z : w.getEntitiesByClass(Zombie.class))
							if (z.getTarget() instanceof Player && z.getCustomName() != null)
								if (z.getCustomName().equalsIgnoreCase("Undead Gunner"))
									Loops.loop3s_zombie(z);

					if (Feature.BONE_WIZARDS.isEnabled(w))
						for (Skeleton s : w.getEntitiesByClass(Skeleton.class))
							if (s.getTarget() instanceof Player && s.getCustomName() != null)
								if (s.getCustomName().equalsIgnoreCase("Bone Wizard"))
									Loops.loop3s_skeleton(s);
				}
			}
		}.runTaskTimer(SuddenDeath.plugin, 20, 60);
	}

	@EventHandler
	public void a(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Skeleton && event.getProjectile() instanceof Arrow && Feature.BONE_GRENADES.isEnabled(event.getEntity())) {
			Skeleton skeleton = (Skeleton) event.getEntity();
			if (!(skeleton.getTarget() instanceof Player))
				return;

			Player target = (Player) skeleton.getTarget();
			double chance = Feature.BONE_GRENADES.getDouble("chance-percent") / 100;
			double dmg = Feature.BONE_GRENADES.getDouble("damage");
			if (random.nextDouble() < chance) {
				event.setCancelled(true);

				skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_BAT_DEATH, 1, 1);
				NoInteractItemEntity grenade = new NoInteractItemEntity(skeleton.getEyeLocation(), new ItemStack(Material.SKELETON_SKULL));
				grenade.getEntity().setVelocity(target.getLocation().subtract(skeleton.getLocation()).toVector().multiply(.05).setY(.6));
				new BukkitRunnable() {
					double ti = 0;

					public void run() {
						ti++;
						if (ti > 40 || grenade.getEntity().isDead()) {
							grenade.close();
							cancel();
							return;
						}

						grenade.getEntity().getWorld().spawnParticle(Particle.SMOKE_NORMAL, grenade.getEntity().getLocation(), 0);
						if (grenade.getEntity().isOnGround()) {
							grenade.close();
							cancel();

							grenade.getEntity().getWorld().spawnParticle(Particle.EXPLOSION_LARGE, grenade.getEntity().getLocation(), 24, 3, 3, 3, 0);
							grenade.getEntity().getWorld().playSound(grenade.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
							for (Entity entity : grenade.getEntity().getNearbyEntities(6, 6, 6))
								if (entity instanceof Player)
									Utils.damage((LivingEntity) entity, dmg, true);
						}
					}
				}.runTaskTimer(SuddenDeath.plugin, 0, 1);
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void b(EntityDamageByEntityEvent event) {
		if (event.isCancelled() || event.getEntity().hasMetadata("NPC") || event.getDamager().hasMetadata("NPC") || event.getDamage() <= 0)
			return;

		// mob crits
		if (event.getEntity() instanceof Player && event.getDamager() instanceof LivingEntity) {
			Player player = (Player) event.getEntity();
			LivingEntity entity = (LivingEntity) event.getDamager();
			if (Feature.MOB_CRITICAL_STRIKES.isEnabled(player)) {
				double chance = Feature.MOB_CRITICAL_STRIKES.getDouble("crit-chance." + entity.getType().name()) / 100;
				if (random.nextDouble() <= chance) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 2, 1);
					player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 32, 0, 0, 0, .5);
					player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation().add(0, 1, 0), 0);
					double mul = Feature.MOB_CRITICAL_STRIKES.getDouble("damage-percent") / 100;
					double dmg = event.getDamage() * (1 + mul);
					event.setDamage(dmg);
				}
			}
		}

		// thief slimes
		if (event.getEntity() instanceof Player && (event.getDamager() instanceof Slime || event.getDamager() instanceof MagmaCube)) {
			Player player = (Player) event.getEntity();
			if (Feature.THIEF_SLIMES.isEnabled(player)) {
				double chance = Feature.THIEF_SLIMES.getDouble("chance-percent") / 100;
				if (random.nextDouble() <= chance) {
					int exp = (int) Feature.THIEF_SLIMES.getDouble("exp");
					ExperienceCalculator m = new ExperienceCalculator(player);
					int current = m.getTotalExperience();
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2, 1);
					String format = ChatColor.DARK_RED + Utils.msg("lost-exp").replace("#exp#", "" + exp);
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(format));
					for (int j = 0; j < 8; j++) {
						ItemStack stack = new ItemStack(Material.GOLD_NUGGET);
						ItemMeta stack_meta = stack.getItemMeta();
						stack_meta.setDisplayName("BOUNTYHUNTERS:chest " + player.getUniqueId().toString() + " " + j);
						stack.setItemMeta(stack_meta);

						NoInteractItemEntity item = new NoInteractItemEntity(player.getLocation(), stack);
						Bukkit.getScheduler().scheduleSyncDelayedTask(SuddenDeath.plugin, () -> item.close(), 30 + random.nextInt(30));
					}
					int newExp = (current - exp < 0 ? 0 : current - exp);
					m.setTotalExperience(newExp);
				}
			}
		}

		// poisoned slimes
		if (event.getEntity() instanceof Slime) {
			Slime slime = (Slime) event.getEntity();
			if (event.getDamager() instanceof Player && Feature.POISONED_SLIMES.isEnabled(slime)) {
				Player player = (Player) event.getDamager();
				if (Utils.hasCreativeGameMode(player))
					return;

				double chance = Feature.POISONED_SLIMES.getDouble("chance-percent") / 100;
				if (random.nextDouble() <= chance) {
					double duration = Feature.POISONED_SLIMES.getDouble("duration");
					int amplifier = (int) Feature.POISONED_SLIMES.getDouble("amplifier");
					slime.getWorld().spawnParticle(Particle.SLIME, slime.getLocation(), 32, 1, 1, 1, 0);
					slime.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, slime.getLocation(), 24, 1, 1, 1, 0);
					slime.getWorld().playSound(slime.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 2, 1);
					player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (duration * 20), amplifier));
				}
			}
		}

		// ender power
		if (event.getEntity() instanceof Enderman || event.getEntity().getType().name().equalsIgnoreCase("shulker")
				|| event.getEntity() instanceof Endermite || event.getEntity() instanceof EnderDragon) {
			Entity entity = event.getEntity();
			if (event.getDamager() instanceof Player && Feature.ENDER_POWER.isEnabled(entity)) {
				Player player = (Player) event.getDamager();
				if (Utils.hasCreativeGameMode(player))
					return;

				double chance = Feature.ENDER_POWER.getDouble("chance-percent") / 100;
				if (random.nextDouble() <= chance) {
					double duration = Feature.ENDER_POWER.getDouble("duration");
					new BukkitRunnable() {
						final Location loc = player.getLocation();
						double y = 0;

						public void run() {
							for (int j1 = 0; j1 < 3; j1++) {
								y += .07;
								int par_n = 3;
								for (int j = 0; j < par_n; j++)
									loc.getWorld().spawnParticle(Particle.REDSTONE,
											loc.clone().add(Math.cos(y * Math.PI + (j * Math.PI * 2 / par_n)) * (3 - y) / 2.5, y,
													Math.sin(y * Math.PI + (j * Math.PI * 2 / par_n)) * (3 - y) / 2.5),
											0, new Particle.DustOptions(Color.BLACK, 1));
							}
							if (y > 3)
								cancel();
						}
					}.runTaskTimer(SuddenDeath.plugin, 0, 1);
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2, 2);
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (duration * 20), 0));
					Location loc = player.getLocation();
					loc.setYaw(player.getEyeLocation().getYaw() - 180);
					loc.setPitch(player.getEyeLocation().getPitch());
					player.teleport(loc);
				}
			}
		}
	}

	// snow slow
	@EventHandler
	public void c(PlayerMoveEvent event) {
		if (event.getPlayer().hasMetadata("NPC"))
			return;

		Player player = event.getPlayer();
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
				&& event.getFrom().getBlockZ() == event.getTo().getBlockZ())
			return;

		if (Feature.SNOW_SLOW.isEnabled(player)) {
			Block block = player.getLocation().getBlock();
			if (!Utils.hasCreativeGameMode(player) && block.getType() == Material.SNOW) {
				if (player.getInventory().getBoots() == null)
					return;

				for (Material m : new Material[] { Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS })
					if (player.getInventory().getBoots().getType() == m) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
						break;
					}
			}
		}
	}

	// realistic pickup
	@EventHandler
	public void d(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		Player player = (Player) event.getEntity();
		Item item = event.getItem();
		if (!Feature.REALISTIC_PICKUP.isEnabled(player) || Utils.hasCreativeGameMode(player))
			return;

		if ((player.getEyeLocation().getPitch() > 70 || item.getLocation().getY() >= player.getLocation().getY() + 1) && player.isSneaking()) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1));
			return;
		}
		event.setCancelled(true);
	}

	// freddy summon
	@EventHandler
	public void e(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		if (!Feature.FREDDY.isEnabled(player) || Utils.hasCreativeGameMode(player))
			return;

		double chance = Feature.FREDDY.getDouble("chance-percent") / 100;
		if (random.nextDouble() < chance) {
			Enderman freddy = (Enderman) player.getWorld().spawnEntity(player.getLocation(), EntityType.ENDERMAN);
			freddy.setCustomName("Freddy");
			freddy.setCustomNameVisible(true);

			// hp
			AttributeInstance maxHealth = freddy.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			maxHealth.setBaseValue(maxHealth.getBaseValue() * 1.75);

			// ms
			AttributeInstance movementSpeed = freddy.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
			movementSpeed.setBaseValue(movementSpeed.getBaseValue() * 1.35);

			// atk
			AttributeInstance attackDamage = freddy.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
			attackDamage.setBaseValue(attackDamage.getBaseValue() * 1.35);

			freddy.setTarget(player);
			player.sendMessage(ChatColor.DARK_RED + Utils.msg("freddy-summoned"));
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2, 0);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
		}
	}

	// dangerous coal
	@EventHandler
	public void f(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (!Feature.DANGEROUS_COAL.isEnabled(player) || Utils.hasCreativeGameMode(player))
			return;

		if (block.getType() == Material.COAL_ORE) {
			double chance = Feature.DANGEROUS_COAL.getDouble("chance-percent") / 100;
			if (random.nextDouble() < chance) {
				double radius = Feature.DANGEROUS_COAL.getDouble("radius");
				block.getWorld().playSound(block.getLocation(), Sound.ENTITY_TNT_PRIMED, 2, 1);
				new BukkitRunnable() {
					double t = 0;

					public void run() {
						t++;
						block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(.5, 0, .5), 0);
						if (t > 39) {
							block.getWorld().createExplosion(block.getLocation(), (float) radius);
							cancel();
						}
					}
				}.runTaskTimer(SuddenDeath.plugin, 0, 1);
			}
		}
	}
}