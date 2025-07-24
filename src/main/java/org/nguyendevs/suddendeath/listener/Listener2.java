package org.nguyendevs.suddendeath.listener;

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
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.ExperienceCalculator;
import org.nguyendevs.suddendeath.util.NoInteractItemEntity;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.Random;
import java.util.logging.Level;

/**
 * Event listener for various entity and player interactions in the SuddenDeath plugin.
 */
public class Listener2 implements Listener {
	private static final Random RANDOM = new Random();
	private static final long WITHER_SKELETON_LOOP_INTERVAL = 120L;
	private static final long ZOMBIE_SKELETON_LOOP_INTERVAL = 60L;
	private static final long INITIAL_DELAY = 20L;

	/**
	 * Initializes periodic tasks for WitherSkeleton and Zombie/Skeleton entities.
	 */
	public Listener2() {
		// WitherSkeleton loop
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					for (World world : Bukkit.getWorlds()) {
						if (!Feature.WITHER_RUSH.isEnabled(world)) {
							continue;
						}
						for (WitherSkeleton witherSkeleton : world.getEntitiesByClass(WitherSkeleton.class)) {
							if (witherSkeleton.getTarget() instanceof Player) {
								Loops.loop6s_wither_skeleton(witherSkeleton);
							}
						}
					}
				} catch (Exception e) {
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Error in WitherSkeleton loop task", e);
				}
			}
		}.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, WITHER_SKELETON_LOOP_INTERVAL);

		// Zombie and Skeleton loop
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					for (World world : Bukkit.getWorlds()) {
						if (Feature.UNDEAD_GUNNERS.isEnabled(world)) {
							for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
								if (zombie.getTarget() instanceof Player && isUndeadGunner(zombie)) {
									Loops.loop3s_zombie(zombie);
								}
							}
						}
						if (Feature.BONE_WIZARDS.isEnabled(world)) {
							for (Skeleton skeleton : world.getEntitiesByClass(Skeleton.class)) {
								if (skeleton.getTarget() instanceof Player && isBoneWizard(skeleton)) {
									Loops.loop3s_skeleton(skeleton);
								}
							}
						}
					}
				} catch (Exception e) {
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Error in Zombie/Skeleton loop task", e);
				}
			}
		}.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, ZOMBIE_SKELETON_LOOP_INTERVAL);
	}

	/**
	 * Checks if a Zombie is an "Undead Gunner" based on its custom name.
	 *
	 * @param zombie The Zombie entity.
	 * @return True if the Zombie is an Undead Gunner.
	 */
	private boolean isUndeadGunner(Zombie zombie) {
		return zombie.getCustomName() != null && zombie.getCustomName().equalsIgnoreCase("Undead Gunner");
	}

	/**
	 * Checks if a Skeleton is a "Bone Wizard" based on its custom name.
	 *
	 * @param skeleton The Skeleton entity.
	 * @return True if the Skeleton is a Bone Wizard.
	 */
	private boolean isBoneWizard(Skeleton skeleton) {
		return skeleton.getCustomName() != null && skeleton.getCustomName().equalsIgnoreCase("Bone Wizard");
	}

	/**
	 * Handles Skeleton shooting grenades instead of arrows.
	 *
	 * @param event The EntityShootBowEvent.
	 */
	@EventHandler
	public void onSkeletonShoot(EntityShootBowEvent event) {
		if (!(event.getEntity() instanceof Skeleton skeleton) || !(event.getProjectile() instanceof Arrow) ||
				!Feature.BONE_GRENADES.isEnabled(skeleton) || !(skeleton.getTarget() instanceof Player target)) {
			return;
		}

		try {
			double chance = Feature.BONE_GRENADES.getDouble("chance-percent") / 100.0;
			if (RANDOM.nextDouble() >= chance) {
				return;
			}

			event.setCancelled(true);
			double damage = Feature.BONE_GRENADES.getDouble("damage");
			skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);

			NoInteractItemEntity grenade = new NoInteractItemEntity(skeleton.getEyeLocation(), new ItemStack(Material.SKELETON_SKULL));
			grenade.getEntity().setVelocity(target.getLocation().subtract(skeleton.getLocation()).toVector().multiply(0.05).setY(0.6));

			new BukkitRunnable() {
				double ticks = 0;

				@Override
				public void run() {
					try {
						ticks++;
						Item grenadeEntity = grenade.getEntity();
						if (ticks > 40 || grenadeEntity.isDead()) {
							grenade.close();
							cancel();
							return;
						}

						grenadeEntity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, grenadeEntity.getLocation(), 0);
						if (grenadeEntity.isOnGround()) {
							grenade.close();
							grenadeEntity.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, grenadeEntity.getLocation(), 24, 3, 3, 3, 0);
							grenadeEntity.getWorld().playSound(grenadeEntity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
							for (Entity nearby : grenadeEntity.getNearbyEntities(6, 6, 6)) {
								if (nearby instanceof Player player) {
									Utils.damage(player, damage, true);
								}
							}
							cancel();
						}
					} catch (Exception e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Error in Bone Grenade task for skeleton: " + skeleton.getUniqueId(), e);
						grenade.close();
						cancel();
					}
				}
			}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling Bone Grenade for skeleton: " + skeleton.getUniqueId(), e);
		}
	}

	/**
	 * Handles various entity damage events (mob crits, thief slimes, poisoned slimes, ender power).
	 *
	 * @param event The EntityDamageByEntityEvent.
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC") || event.getDamager().hasMetadata("NPC")) {
			return;
		}

		try {
			// Mob Critical Strikes
			if (event.getEntity() instanceof Player player && event.getDamager() instanceof LivingEntity entity &&
					Feature.MOB_CRITICAL_STRIKES.isEnabled(player)) {
				double chance = Feature.MOB_CRITICAL_STRIKES.getDouble("crit-chance." + entity.getType().name()) / 100.0;
				if (RANDOM.nextDouble() <= chance) {
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 2.0f, 1.0f);
					player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 32, 0, 0, 0, 0.5);
					player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation().add(0, 1, 0), 0);
					double multiplier = Feature.MOB_CRITICAL_STRIKES.getDouble("damage-percent") / 100.0;
					event.setDamage(event.getDamage() * (1 + multiplier));
				}
			}

			// Thief Slimes
			if (event.getEntity() instanceof Player player && (event.getDamager() instanceof Slime || event.getDamager() instanceof MagmaCube) &&
					Feature.THIEF_SLIMES.isEnabled(player)) {
				double chance = Feature.THIEF_SLIMES.getDouble("chance-percent") / 100.0;
				if (RANDOM.nextDouble() <= chance) {
					int exp = (int) Feature.THIEF_SLIMES.getDouble("exp");
					ExperienceCalculator calculator = new ExperienceCalculator(player);
					int currentExp = calculator.getTotalExperience();
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f, 1.0f);
					String message = ChatColor.DARK_RED + Utils.msg("lost-exp").replace("#exp#", String.valueOf(exp));
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));

					for (int i = 0; i < 8; i++) {
						ItemStack stack = new ItemStack(Material.GOLD_NUGGET);
						ItemMeta meta = stack.getItemMeta();
						if (meta == null) {
							SuddenDeath.getInstance().getLogger().log(Level.WARNING,
									"ItemMeta is null for Thief Slime item");
							continue;
						}
						meta.setDisplayName("BOUNTYHUNTERS:chest " + player.getUniqueId() + " " + i);
						stack.setItemMeta(meta);

						NoInteractItemEntity item = new NoInteractItemEntity(player.getLocation(), stack);
						Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), item::close, 30 + RANDOM.nextInt(30));
					}
					calculator.setTotalExperience(Math.max(currentExp - exp, 0));
				}
			}

			// Poisoned Slimes
			if (event.getEntity() instanceof Slime slime && event.getDamager() instanceof Player player &&
					!Utils.hasCreativeGameMode(player) && Feature.POISONED_SLIMES.isEnabled(slime)) {
				double chance = Feature.POISONED_SLIMES.getDouble("chance-percent") / 100.0;
				if (RANDOM.nextDouble() <= chance) {
					double duration = Feature.POISONED_SLIMES.getDouble("duration");
					int amplifier = (int) Feature.POISONED_SLIMES.getDouble("amplifier");
					slime.getWorld().spawnParticle(Particle.SLIME, slime.getLocation(), 32, 1, 1, 1, 0);
					slime.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, slime.getLocation(), 24, 1, 1, 1, 0);
					slime.getWorld().playSound(slime.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 2.0f, 1.0f);
					player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (duration * 20), amplifier));
				}
			}

			// Ender Power
			if (isEnderEntity(event.getEntity()) && event.getDamager() instanceof Player player &&
					!Utils.hasCreativeGameMode(player) && Feature.ENDER_POWER.isEnabled(event.getEntity())) {
				double chance = Feature.ENDER_POWER.getDouble("chance-percent") / 100.0;
				if (RANDOM.nextDouble() <= chance) {
					double duration = Feature.ENDER_POWER.getDouble("duration");
					spawnEnderParticles(player);
					event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 2.0f);
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (duration * 20), 0));
					Location loc = player.getLocation();
					loc.setYaw(player.getEyeLocation().getYaw() - 180);
					loc.setPitch(player.getEyeLocation().getPitch());
					player.teleport(loc);
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling EntityDamageByEntityEvent for entity: " + event.getEntity().getType(), e);
		}
	}

	/**
	 * Checks if an entity is an Ender-related entity.
	 *
	 * @param entity The entity to check.
	 * @return True if the entity is an Enderman, Shulker, Endermite, or EnderDragon.
	 */
	private boolean isEnderEntity(Entity entity) {
		return entity instanceof Enderman || entity.getType().name().equalsIgnoreCase("SHULKER") ||
				entity instanceof Endermite || entity instanceof EnderDragon;
	}

	/**
	 * Spawns particle effects for the Ender Power feature.
	 *
	 * @param player The player to spawn particles around.
	 */
	private void spawnEnderParticles(Player player) {
		try {
			new BukkitRunnable() {
				double y = 0;

				@Override
				public void run() {
					try {
						for (int j1 = 0; j1 < 3; j1++) {
							y += 0.07;
							int particleCount = 3;
							for (int j = 0; j < particleCount; j++) {
								player.getWorld().spawnParticle(Particle.REDSTONE,
										player.getLocation().clone().add(
												Math.cos(y * Math.PI + (j * Math.PI * 2 / particleCount)) * (3 - y) / 2.5,
												y,
												Math.sin(y * Math.PI + (j * Math.PI * 2 / particleCount)) * (3 - y) / 2.5),
										0, new Particle.DustOptions(Color.BLACK, 1));
							}
						}
						if (y > 3) {
							cancel();
						}
					} catch (Exception e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Error in Ender Power particle task for player: " + player.getName(), e);
						cancel();
					}
				}
			}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error spawning Ender Power particles for player: " + player.getName(), e);
		}
	}

	/**
	 * Handles player movement to apply snow slow effects.
	 *
	 * @param event The PlayerMoveEvent.
	 */
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (player.hasMetadata("NPC") || !hasMovedBlock(event)) {
			return;
		}

		try {
			if (Feature.SNOW_SLOW.isEnabled(player) && !Utils.hasCreativeGameMode(player) &&
					player.getLocation().getBlock().getType() == Material.SNOW &&
					isWearingHeavyBoots(player)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling PlayerMoveEvent for player: " + player.getName(), e);
		}
	}

	/**
	 * Checks if the player has moved to a different block.
	 *
	 * @param event The PlayerMoveEvent.
	 * @return True if the player moved to a new block.
	 */
	private boolean hasMovedBlock(PlayerMoveEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();
		return to != null &&
				(from.getBlockX() != to.getBlockX() ||
						from.getBlockY() != to.getBlockY() ||
						from.getBlockZ() != to.getBlockZ());
	}

	/**
	 * Checks if the player is wearing heavy boots.
	 *
	 * @param player The player to check.
	 * @return True if the player is wearing iron, golden, or diamond boots.
	 */
	private boolean isWearingHeavyBoots(Player player) {
		ItemStack boots = player.getInventory().getBoots();
		if (boots == null) {
			return false;
		}
		Material type = boots.getType();
		return type == Material.IRON_BOOTS || type == Material.GOLDEN_BOOTS || type == Material.DIAMOND_BOOTS;
	}

	/**
	 * Handles item pickup to enforce realistic pickup mechanics.
	 *
	 * @param event The EntityPickupItemEvent.
	 */
	@EventHandler
	public void onEntityPickupItem(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player player) || !Feature.REALISTIC_PICKUP.isEnabled(player) ||
				Utils.hasCreativeGameMode(player)) {
			return;
		}

		try {
			Item item = event.getItem();
			if ((player.getEyeLocation().getPitch() > 70 || item.getLocation().getY() >= player.getLocation().getY() + 1) &&
					player.isSneaking()) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1));
			} else {
				event.setCancelled(true);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling EntityPickupItemEvent for player: " + player.getName(), e);
		}
	}

	/**
	 * Handles bed leaving to potentially spawn a "Freddy" Enderman.
	 *
	 * @param event The PlayerBedLeaveEvent.
	 */
	@EventHandler
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		if (!Feature.FREDDY.isEnabled(player) || Utils.hasCreativeGameMode(player)) {
			return;
		}

		try {
			double chance = Feature.FREDDY.getDouble("chance-percent") / 100.0;
			if (RANDOM.nextDouble() >= chance) {
				return;
			}

			Enderman freddy = (Enderman) player.getWorld().spawnEntity(player.getLocation(), EntityType.ENDERMAN);
			freddy.setCustomName("Freddy");
			freddy.setCustomNameVisible(true);

			// Set attributes
			try {
				AttributeInstance maxHealth = freddy.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if (maxHealth != null) {
					maxHealth.setBaseValue(maxHealth.getBaseValue() * 1.75);
				} else {
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Max health attribute not found for Freddy Enderman");
				}

				AttributeInstance movementSpeed = freddy.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				if (movementSpeed != null) {
					movementSpeed.setBaseValue(movementSpeed.getBaseValue() * 1.35);
				} else {
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Movement speed attribute not found for Freddy Enderman");
				}

				AttributeInstance attackDamage = freddy.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
				if (attackDamage != null) {
					attackDamage.setBaseValue(attackDamage.getBaseValue() * 1.35);
				} else {
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Attack damage attribute not found for Freddy Enderman");
				}
			} catch (Exception e) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Error setting attributes for Freddy Enderman", e);
			}

			freddy.setTarget(player);
			player.sendMessage(translateColors(Utils.msg("prefix") + " "+ Utils.msg("freddy-summoned")));
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.0f);
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling PlayerBedLeaveEvent for player: " + player.getName(), e);
		}
	}

	/**
	 * Handles block breaking to trigger dangerous coal explosions.
	 *
	 * @param event The BlockBreakEvent.
	 */
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (!Feature.DANGEROUS_COAL.isEnabled(player) || Utils.hasCreativeGameMode(player) ||
				block.getType() != Material.COAL_ORE) {
			return;
		}

		try {
			double chance = Feature.DANGEROUS_COAL.getDouble("chance-percent") / 100.0;
			if (RANDOM.nextDouble() >= chance) {
				return;
			}

			double radius = Feature.DANGEROUS_COAL.getDouble("radius");
			block.getWorld().playSound(block.getLocation(), Sound.ENTITY_TNT_PRIMED, 2.0f, 1.0f);

			new BukkitRunnable() {
				double ticks = 0;

				@Override
				public void run() {
					try {
						ticks++;
						block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(0.5, 0, 0.5), 0);
						if (ticks > 39) {
							block.getWorld().createExplosion(block.getLocation(), (float) radius);
							cancel();
						}
					} catch (Exception e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Error in Dangerous Coal task for player: " + player.getName(), e);
						cancel();
					}
				}
			}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling BlockBreakEvent for player: " + player.getName(), e);
		}
	}
	private String translateColors(String message) {
		return ChatColor.translateAlternateColorCodes('&', message);
	}

}