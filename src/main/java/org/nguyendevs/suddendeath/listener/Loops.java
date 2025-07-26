package org.nguyendevs.suddendeath.listener;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.NoInteractItemEntity;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Utility class for handling periodic tasks (loops) for various entities and players in the SuddenDeath plugin.
 */
public final class Loops {
	private static final Random random = new Random();
	private static final double TICK_INTERVAL = 0.5;
	private static final int MAX_TICKS = 20;

	private Loops() {
		// Prevent instantiation
	}

	/**
	 * Periodic task for Blaze entities to apply a burning effect to nearby players.
	 *
	 * @param blaze The Blaze entity.
	 */
	public static void loop3s_blaze(Blaze blaze) {
		if (blaze == null || blaze.getHealth() <= 0) {
			return;
		}

		try {
			for (Entity entity : blaze.getNearbyEntities(10, 10, 10)) {
				if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player) || !blaze.hasLineOfSight(player)) {
					continue;
				}

				// Play sound at player's location
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 2.0f);
				double duration = Feature.EVERBURNING_BLAZES.getDouble("burn-duration") * 20;
				player.setFireTicks((int) duration);

				// Define locations with slight offsets for visual effect
				Location playerLoc = player.getLocation().add(0.0D, 0.75D, 0.0D);
				Location blazeLoc = blaze.getLocation().add(0.0D, 1.0D, 0.0D);
				World world = blaze.getWorld(); // Use Blaze's world for consistency

				// Create particle trail from Blaze to Player
				Vector direction = playerLoc.toVector().subtract(blazeLoc.toVector());
				for (double j = 0.0D; j <= 1.0D; j += 0.04D) {
					Location particleLoc = blazeLoc.clone().add(direction.clone().multiply(j));
					if (particleLoc.getWorld() != null) {
						particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 8, 0.2D, 0.2D, 0.2D, 0.0D);
						particleLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 8, 0.2D, 0.2D, 0.2D, 0.0D);
					}
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in Blaze loop for entity: " + blaze.getUniqueId(), e);
		}
	}

	/**
	 * Periodic task for Skeleton entities to launch fireballs or apply frost curses.
	 *
	 * @param skeleton The Skeleton entity.
	 */
	public static void loop3s_skeleton(Skeleton skeleton) {
		if (skeleton == null || skeleton.getHealth() <= 0 || skeleton.getTarget() == null || !(skeleton.getTarget() instanceof Player target)) {
			return;
		}

		try {
			if (!target.getWorld().equals(skeleton.getWorld())) {
				return;
			}

			if (random.nextDouble() < 0.5) {
				// Fireball attack
				double damage = Feature.BONE_WIZARDS.getDouble("fireball-damage");
				double duration = Feature.BONE_WIZARDS.getDouble("fireball-duration");
				skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0f, 0.0f);
				launchProjectile(skeleton, target, Particle.FLAME, Particle.LAVA, damage, duration, Sound.ENTITY_GENERIC_EXPLODE);
			} else {
				// Frost curse attack
				double damage = Feature.BONE_WIZARDS.getDouble("frost-curse-damage");
				double duration = Feature.BONE_WIZARDS.getDouble("frost-curse-duration");
				double amplifier = Feature.BONE_WIZARDS.getDouble("frost-curse-amplifier");
				applyFrostCurse(skeleton, target, damage, duration, amplifier);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in Skeleton loop for entity: " + skeleton.getUniqueId(), e);
		}
	}



	/**
	 * Periodic task for Zombie entities to launch explosive projectiles.
	 *
	 * @param zombie The Zombie entity.
	 */
	public static void loop3s_zombie(Zombie zombie) {
		if (zombie == null || zombie.getHealth() <= 0 || zombie.getTarget() == null || !(zombie.getTarget() instanceof Player target)) {
			return;
		}

		try {
			if (!target.getWorld().equals(zombie.getWorld())) {
				return;
			}

			double damage = Feature.UNDEAD_GUNNERS.getDouble("damage");
			zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.0f);
			launchProjectile(zombie, target, Particle.CLOUD, null, damage, 0.0, Sound.ENTITY_GENERIC_EXPLODE, 2.3, () -> {
				double blockDmg = Feature.UNDEAD_GUNNERS.getDouble("block-damage");
				if (blockDmg > 0) {
					zombie.getWorld().createExplosion(zombie.getLocation(), (float) blockDmg);
				}
			});
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in Zombie loop for entity: " + zombie.getUniqueId(), e);
		}
	}

	/**
	 * Periodic task for Witch entities to apply slowing effects and damage to nearby players.
	 *
	 * @param witch The Witch entity.
	 */
	public static void loop4s_witch(Witch witch) {
		if (witch == null || witch.getHealth() <= 0) {
			return;
		}

		try {
			for (Entity entity : witch.getNearbyEntities(10, 10, 10)) {
				if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player) || !witch.hasLineOfSight(player)) {
					continue;
				}

				witch.getWorld().playSound(witch.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.0f, 2.0f);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
						(int) (Feature.WITCH_SCROLLS.getDouble("slow-duration") * 20), 1));
				Utils.damage(player, Feature.WITCH_SCROLLS.getDouble("damage"), true);

				Location loc = entity.getLocation().add(0.0D, 1.0D, 0.0D);
				Location loc1 = witch.getLocation().add(0.0D, 1.0D, 0.0D);

				for(double j = 0.0D; j < 1.0D; j += 0.04D) {
					Vector d = loc1.toVector().subtract(loc.toVector());
					Location loc2 = loc.clone().add(d.multiply(j));
					((World)Objects.requireNonNull(loc2.getWorld())).spawnParticle(Particle.SPELL_WITCH, loc2, 4, 0.1D, 0.1D, 0.1D, 0.0D);
				}
				/*
				Location playerLoc = player.getLocation().add(0, 1, 0);
				Location witchLoc = witch.getLocation().add(0, 1, 0);
				spawnParticleTrail(witch.getWorld(), witchLoc, playerLoc, Particle.SPELL_WITCH, null);
			*/
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in Witch loop for entity: " + witch.getUniqueId(), e);
		}
	}

	/**
	 * Periodic task for WitherSkeleton entities to perform machine gun or rush attacks.
	 *
	 * @param witherSkeleton The WitherSkeleton entity.
	 */
	public static void loop6s_wither_skeleton(Creature witherSkeleton) {
		if (witherSkeleton == null || witherSkeleton.getHealth() <= 0 || !(witherSkeleton.getTarget() instanceof Player target)) {
			return;
		}

		try {
			if (!target.getWorld().equals(witherSkeleton.getWorld())) {
				return;
			}

			if (Feature.WITHER_MACHINEGUN.isEnabled(witherSkeleton) && random.nextDouble() < 0.5) {
				double damage = Feature.WITHER_MACHINEGUN.getDouble("damage");
				launchWitherMachineGun(witherSkeleton, target, damage);
			} else {
				double damage = Feature.WITHER_RUSH.getDouble("damage");
				applyWitherRush(witherSkeleton, target, damage);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in WitherSkeleton loop for entity: " + witherSkeleton.getUniqueId(), e);
		}
	}
	/**
	 * Periodic task for Spider entities to apply web trap or leap attacks.
	 *
	 * @param spider The Spider entity.
	 */
	public static void loop3s_spider(Spider spider) {
		if (spider == null || spider.getHealth() <= 0 || !(spider.getTarget() instanceof Player target)) {
			return;
		}

		try {
			if (!target.getWorld().equals(spider.getWorld())) {
				return;
			}

			if (Feature.ANGRY_SPIDERS.isEnabled(spider) && random.nextDouble() < 0.5) {
				double damage = Feature.ANGRY_SPIDERS.getDouble("damage");
				double duration = Feature.ANGRY_SPIDERS.getDouble("duration") * 20;
				double amplifier = Feature.ANGRY_SPIDERS.getDouble("amplifier");

				spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
				NoInteractItemEntity item = new NoInteractItemEntity(
						spider.getLocation().add(0, 1, 0),
						new ItemStack(Material.COBWEB)
				);
				item.getEntity().setVelocity(
						target.getLocation().add(0, 1, 0)
								.subtract(spider.getLocation().add(0, 1, 0))
								.toVector().multiply(0.4)
				);

				new BukkitRunnable() {
					int ti = 0;

					@Override
					public void run() {
						try {
							ti++;
							if (ti > 20 || item.getEntity().isDead()) {
								item.close();
								cancel();
								return;
							}

							item.getEntity().getWorld().spawnParticle(Particle.CRIT, item.getEntity().getLocation(), 0);
							for (Entity entity : item.getEntity().getNearbyEntities(1, 1, 1)) {
								if (entity instanceof Player hitPlayer) {
									item.close();
									hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) duration, (int) amplifier));
									Utils.damage(hitPlayer, damage, true);
									cancel();
									return;
								}
							}
						} catch (Exception e) {
							SuddenDeath.getInstance().getLogger().log(Level.WARNING,
									"Error in AngrySpider projectile task for spider: " + spider.getUniqueId(), e);
							item.close();
							cancel();
						}
					}
				}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
				return;
			}

			if (Feature.LEAPING_SPIDERS.isEnabled(spider)) {
				spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
				spider.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, spider.getLocation(), 8, 0, 0, 0, 0.1);
				Vector direction = target.getEyeLocation().toVector().subtract(spider.getLocation().toVector()).multiply(0.3).setY(0.3);
				spider.setVelocity(direction);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in Spider loop for entity: " + spider.getUniqueId(), e);
		}
	}

	/**
	 * Periodic task for players to apply hunger nausea, bleeding, and infection effects.
	 *
	 * @param player The Player to process.
	 */
	public static void loop3s_player(Player player) {
		if (player == null || Utils.hasCreativeGameMode(player)) {
			return;
		}

		try {
			PlayerData data = PlayerData.get(player);
			if (data == null) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"PlayerData not found for player: " + player.getName());
				return;
			}

			// Hunger Nausea
			if (Feature.HUNGER_NAUSEA.isEnabled(player) && player.getFoodLevel() < 8) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 0));
			}

			// Bleeding
			if (Feature.BLEEDING.isEnabled(player) && data.isBleeding() &&
					SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT) &&
					player.getHealth() >= Feature.BLEEDING.getDouble("health-min")) {
				Utils.damage(player, Feature.BLEEDING.getDouble("dps") * 3, Feature.BLEEDING.getBoolean("tug"));
			}

			// Infection
			if (Feature.INFECTION.isEnabled(player) && data.isInfected() &&
					SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT) &&
					player.getHealth() >= Feature.INFECTION.getDouble("health-min")) {
				Utils.damage(player, Feature.INFECTION.getDouble("dps") * 3, Feature.INFECTION.getBoolean("tug"));
				player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 600, 0));
				if (Feature.INFECTION.getBoolean("sound")) {
					player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 666.0f, 0.1f);
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in player loop for player: " + player.getName(), e);
		}
	}

	/**
	 * Spawns a particle trail between two locations.
	 *
	 * @param world     The world to spawn particles in.
	 * @param start     The starting location.
	 * @param end       The ending location.
	 * @param primary   The primary particle type.
	 * @param secondary The secondary particle type (can be null).

	private static void spawnParticleTrail(World world, Location start, Location end, Particle primary, Particle secondary) {
		try {
			Vector direction = end.toVector().subtract(start.toVector());
			for (double j = 0; j <= 1; j += 0.04) {
				Location point = start.clone().add(direction.multiply(j));
				world.spawnParticle(primary, point, 4, 0.1, 0.1, 0.1, 0);
				if (secondary != null) {
					world.spawnParticle(secondary, point, 4, 0.1, 0.1, 0.1, 0);
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error spawning particle trail", e);
		}
	}
	*/

	/**
	 * Launches a projectile from an entity towards a target player.
	 *
	 * @param entity     The source entity.
	 * @param target     The target player.
	 * @param primary    The primary particle type.
	 * @param secondary  The secondary particle type (can be null).
	 * @param damage     The damage to apply on hit.
	 * @param duration   The duration for fire ticks (if applicable).
	 * @param hitSound   The sound to play on hit.
	 * @param hitRadius  The radius to check for player hits.
	 * @param onHit      Optional action to perform on hit.
	 */
	private static void launchProjectile(LivingEntity entity, Player target, Particle primary, Particle secondary,
										 double damage, double duration, Sound hitSound, double hitRadius,
										 Runnable onHit) {
		try {
			Vector direction = target.getLocation().add(0, 0.5, 0).toVector()
					.subtract(entity.getLocation().add(0, 0.75, 0).toVector()).normalize().multiply(TICK_INTERVAL);
			Location loc = entity.getEyeLocation();

			new BukkitRunnable() {
				double ticks = 0;

				@Override
				public void run() {
					try {
						for (int j = 0; j < 2; j++) {
							ticks += TICK_INTERVAL;
							loc.add(direction);
							loc.getWorld().spawnParticle(primary, loc, 4, 0.1, 0.1, 0.1, 0);
							if (secondary != null) {
								loc.getWorld().spawnParticle(secondary, loc, 0);
							}

							for (Player player : entity.getWorld().getPlayers()) {
								if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < hitRadius * hitRadius) {
									loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 0);
									loc.getWorld().playSound(loc, hitSound, 1.0f, 1.0f);
									Utils.damage(player, damage, true);
									if (duration > 0) {
										player.setFireTicks((int) (duration * 20));
									}
									if (onHit != null) {
										onHit.run();
									}
									cancel();
									return;
								}
							}
						}
						if (ticks > MAX_TICKS) {
							cancel();
						}
					} catch (Exception e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Error in projectile task for entity: " + entity.getUniqueId(), e);
						cancel();
					}
				}
			}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error launching projectile for entity: " + entity.getUniqueId(), e);
		}
	}

	/**
	 * Overloaded method for simpler projectile launches without additional on-hit actions.
	 */
	private static void launchProjectile(LivingEntity entity, Player target, Particle primary, Particle secondary,
										 double damage, double duration, Sound hitSound) {
		launchProjectile(entity, target, primary, secondary, damage, duration, hitSound, 1.7, null);
	}

	/**
	 * Applies a frost curse effect to players near the target location.
	 *
	 * @param skeleton  The Skeleton entity.
	 * @param target    The target player.
	 * @param damage    The damage to apply.
	 * @param duration  The duration of the slow effect.
	 * @param amplifier The amplifier for the slow effect.
	 */
	private static void applyFrostCurse(Skeleton skeleton, Player target, double damage, double duration, double amplifier) {
		try {
			Location loc = target.getLocation();
			double radius = 4.0;

			new BukkitRunnable() {
				double ticks = 0;

				@Override
				public void run() {
					try {
						ticks += 1;
						loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 0);
						loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 2.0f, 2.0f);

						if (ticks > 27) {
							loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.0f);
							for (double j = 0; j < Math.PI * 2; j += Math.PI / 36) {
								Location circleLoc = loc.clone().add(Math.cos(j) * radius, 0.1, Math.sin(j) * radius);
								loc.getWorld().spawnParticle(Particle.CLOUD, circleLoc, 0);
							}
							for (Player player : skeleton.getWorld().getPlayers()) {
								if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < radius * radius) {
									Utils.damage(player, damage, true);
									player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
											(int) (duration * 20), (int) amplifier));
									cancel();
									return;
								}
							}
							cancel();
						}
					} catch (Exception e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Error in frost curse task for skeleton: " + skeleton.getUniqueId(), e);
						cancel();
					}
				}
			}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error applying frost curse for skeleton: " + skeleton.getUniqueId(), e);
		}
	}

	/**
	 * Launches a machine gun-like attack from a WitherSkeleton.
	 *
	 * @param witherSkeleton The WitherSkeleton entity.
	 * @param target        The target player.
	 * @param damage        The damage to apply on hit.
	 */
	private static void launchWitherMachineGun(Creature witherSkeleton, Player target, double damage) {
		try {
			for (int delay = 0; delay < 12; delay += 3) {
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							ItemStack stack = new ItemStack(Material.COAL);
							ItemMeta meta = stack.getItemMeta();
							if (meta == null) {
								SuddenDeath.getInstance().getLogger().log(Level.WARNING,
										"ItemMeta is null for WitherSkeleton projectile");
								return;
							}
							meta.setDisplayName("SUDDEN_DEATH:" + UUID.randomUUID().toString());
							stack.setItemMeta(meta);

							NoInteractItemEntity item = new NoInteractItemEntity(witherSkeleton.getLocation().add(0, 1, 0), stack);
							item.getEntity().setVelocity(target.getLocation().add(0, 2, 0).toVector()
									.subtract(witherSkeleton.getLocation().add(0, 1, 0).toVector()).normalize().multiply(2));

							new BukkitRunnable() {
								double ticks = 0;

								@Override
								public void run() {
									try {
										ticks++;
										Item entityItem = item.getEntity();
										if (ticks >= MAX_TICKS || entityItem.isDead()) {
											item.close();
											cancel();
											return;
										}

										entityItem.getWorld().spawnParticle(Particle.SMOKE_NORMAL, entityItem.getLocation(), 0);
										for (Entity nearby : entityItem.getNearbyEntities(1.3, 1.3, 1.3)) {
											if (nearby instanceof Player player) {
												item.close();
												Utils.damage(player, damage, true);
												cancel();
												return;
											}
										}
									} catch (Exception e) {
										SuddenDeath.getInstance().getLogger().log(Level.WARNING,
												"Error in WitherSkeleton machine gun task for entity: " + witherSkeleton.getUniqueId(), e);
										item.close();
										cancel();
									}
								}
							}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
						} catch (Exception e) {
							SuddenDeath.getInstance().getLogger().log(Level.WARNING,
									"Error launching WitherSkeleton projectile for entity: " + witherSkeleton.getUniqueId(), e);
						}
					}
				}.runTaskLater(SuddenDeath.getInstance(), delay);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in WitherSkeleton machine gun for entity: " + witherSkeleton.getUniqueId(), e);
		}
	}

	/**
	 * Applies a rush attack from a WitherSkeleton towards the target.
	 *
	 * @param witherSkeleton The WitherSkeleton entity.
	 * @param target        The target player.
	 * @param damage        The damage to apply on hit.
	 */
	private static void applyWitherRush(Creature witherSkeleton, Player target, double damage) {
		try {
			witherSkeleton.getWorld().playSound(witherSkeleton.getLocation(), Sound.ENTITY_WITHER_SPAWN, 4.0f, 2.0f);
			witherSkeleton.removePotionEffect(PotionEffectType.SLOW);
			witherSkeleton.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 255));

			new BukkitRunnable() {
				double ticks = 0;

				@Override
				public void run() {
					try {
						if (witherSkeleton.getHealth() <= 0) {
							cancel();
							return;
						}

						ticks += Math.PI / 20;
						Location loc = witherSkeleton.getLocation();
						for (int j = 0; j < 2; j++) {
							Location circleLoc = loc.clone().add(Math.cos(j * Math.PI + ticks), 2.2, Math.sin(j * Math.PI + ticks));
							circleLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, circleLoc, 0);
						}

						if (ticks >= Math.PI) {
							Location start = witherSkeleton.getLocation().add(0, 1, 0);
							Vector direction = target.getLocation().add(0, 1, 0).toVector().subtract(start.toVector());
							for (double j = 0; j < 1; j += 0.03) {
								Location point = start.clone().add(direction.getX() * j, direction.getY() * j, direction.getZ() * j);
								point.getWorld().spawnParticle(Particle.SMOKE_LARGE, point, 0);
							}
							witherSkeleton.getWorld().playSound(witherSkeleton.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.0f);
							witherSkeleton.teleport(target);
							Utils.damage(target, damage, true);
							cancel();
						}
					} catch (Exception e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Error in WitherSkeleton rush task for entity: " + witherSkeleton.getUniqueId(), e);
						cancel();
					}
				}
			}.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error applying WitherSkeleton rush for entity: " + witherSkeleton.getUniqueId(), e);
		}
	}
}