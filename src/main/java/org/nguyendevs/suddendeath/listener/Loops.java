package org.nguyendevs.suddendeath.listener;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.NoInteractItemEntity;
import org.nguyendevs.suddendeath.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

public class Loops {
	public static void loop3s_blaze(Blaze player) {
		if (player.getHealth() <= 0)
			return;
		for (Entity t : player.getNearbyEntities(10, 10, 10)) {
			if (t instanceof Player) {
				if (Utils.hasCreativeGameMode((Player) t))
					return;
				if (!player.hasLineOfSight(t))
					return;

				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1, 2);
				double duration = Feature.EVERBURNING_BLAZES.getDouble("burn-duration") * 20;
				t.setFireTicks((int) duration);
				Location loc = player.getLocation().add(0, .75, 0);
				Location loc1 = t.getLocation().add(0, 1, 0);
				for (double j = 0; j < 1; j += .04) {
					Vector d = loc1.toVector().subtract(loc.toVector());
					Location loc2 = loc.clone().add(d.multiply(j));
					loc2.getWorld().spawnParticle(Particle.FLAME, loc2, 4, .1, .1, .1, 0);
					loc2.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc2, 4, .1, .1, .1, 0);
				}
			}
		}
	}

	public static void loop3s_skeleton(Skeleton skeleton) {
		if (skeleton.getHealth() <= 0)
			return;

		Player target = (Player) skeleton.getTarget();
		if (!target.getWorld().equals(skeleton.getWorld()))
			return;
		
		if (new Random().nextDouble() < .5) {
			double damage = Feature.BONE_WIZARDS.getDouble("fireball-damage");
			double duration = Feature.BONE_WIZARDS.getDouble("fireball-duration");
			skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2, 0);
			Vector v1 = skeleton.getLocation().add(0, .75, 0).toVector();
			Vector v2 = target.getLocation().add(0, .5, 0).toVector();
			new BukkitRunnable() {
				Vector v = v2.subtract(v1).normalize().multiply(.5);
				Location loc = skeleton.getEyeLocation();
				double ti = 0;

				public void run() {
					for (int j = 0; j < 2; j++) {
						ti += .5;
						loc.add(v);
						loc.getWorld().spawnParticle(Particle.FLAME, loc, 4, .1, .1, .1, 0);
						loc.getWorld().spawnParticle(Particle.LAVA, loc, 0);
						for (Player player : skeleton.getWorld().getPlayers())
							if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < 1.7) {
								loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 0);
								loc.getWorld().spawnParticle(Particle.LAVA, loc, 8, 0, 0, 0, 0);
								loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
								Utils.damage(player, damage, true);
								player.setFireTicks((int) duration * 20);
								cancel();
								return;
							}
					}
					if (ti > 20)
						cancel();
				}
			}.runTaskTimer(SuddenDeath.plugin, 0, 1);
			return;
		}

		double damage = Feature.BONE_WIZARDS.getDouble("frost-curse-damage");
		double duration = Feature.BONE_WIZARDS.getDouble("frost-curse-duration");
		double amplifier = Feature.BONE_WIZARDS.getDouble("frost-curse-amplifier");
		new BukkitRunnable() {
			double ti = 0;
			double r = 4;
			final Location loc = target.getLocation();

			public void run() {
				ti += 1;
				loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 0);
				loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 2, 2);
				if (ti > 27) {
					loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 2, 0);
					for (double j = 0; j < Math.PI * 2; j += Math.PI / 36) {
						Location loc1 = loc.clone().add(Math.cos(j) * r, .1, Math.sin(j) * r);
						loc.getWorld().spawnParticle(Particle.CLOUD, loc1, 0);
					}
					for (Player player : skeleton.getWorld().getPlayers())
						if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < Math.pow(r, 2)) {
							Utils.damage(player, damage, true);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration * 20), (int) amplifier));
							cancel();
							return;
						}
					cancel();
				}
			}

		}.runTaskTimer(SuddenDeath.plugin, 0, 1);
	}

	public static void loop3s_zombie(Zombie zombie) {
		if (zombie.getHealth() <= 0)
			return;

		Player target = (Player) zombie.getTarget();
		if (!target.getWorld().equals(zombie.getWorld()))
			return;

		double damage = Feature.UNDEAD_GUNNERS.getDouble("damage");
		zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0);
		Vector v1 = zombie.getLocation().add(0, .75, 0).toVector();
		Vector v2 = target.getLocation().add(0, .5, 0).toVector();
		new BukkitRunnable() {
			Vector v = v2.subtract(v1).normalize().multiply(.5);
			Location loc = zombie.getEyeLocation();
			double ti = 0;

			public void run() {
				for (int j = 0; j < 2; j++) {
					ti += .5;
					loc.add(v);
					loc.getWorld().spawnParticle(Particle.CLOUD, loc, 0);
					loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 2, 0);
					for (Player player : zombie.getWorld().getPlayers()) {
						if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < 2.3) {
							loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0);
							loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 12, 2, 2, 2, 0);
							for (Player nearby : zombie.getWorld().getPlayers())
								if (loc.distanceSquared(nearby.getLocation().add(0, 1, 0)) < 10)
									Utils.damage(nearby, damage, true);
							double blockDmg = Feature.UNDEAD_GUNNERS.getDouble("block-damage");
							if (blockDmg > 0)
								loc.getWorld().createExplosion(loc, (float) blockDmg);
							cancel();
							return;
						}
					}
				}
				if (ti > 20)
					cancel();
			}
		}.runTaskTimer(SuddenDeath.plugin, 0, 1);
	}

	public static void loop4s_witch(Witch witch) {
		if (witch.getHealth() <= 0)
			return;

		for (Entity entity : witch.getNearbyEntities(10, 10, 10)) {
			if (entity instanceof Player) {
				if (Utils.hasCreativeGameMode((Player) entity))
					return;
				if (!witch.hasLineOfSight(entity))
					return;

				witch.getWorld().playSound(witch.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1, 2);
				((Player) entity)
						.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) Feature.WITCH_SCROLLS.getDouble("slow-duration") * 20, 1));
				Utils.damage((LivingEntity) entity, Feature.WITCH_SCROLLS.getDouble("damage"), true);
				Location loc = entity.getLocation().add(0, 1, 0);
				Location loc1 = witch.getLocation().add(0, 1, 0);
				for (double j = 0; j < 1; j += .04) {
					Vector d = loc1.toVector().subtract(loc.toVector());
					Location loc2 = loc.clone().add(d.multiply(j));
					loc2.getWorld().spawnParticle(Particle.SPELL_WITCH, loc2, 4, .1, .1, .1, 0);
				}
			}
		}
	}

	public static void loop6s_wither_skeleton(Creature entity) {
		if (entity.getHealth() <= 0)
			return;

		Player target = (Player) ((Creature) entity).getTarget();
		if (!target.getWorld().equals(entity.getWorld()))
			return;
		
		if (Feature.WITHER_MACHINEGUN.isEnabled(entity) && new Random().nextDouble() < .5) {
			double damage = Feature.WITHER_MACHINEGUN.getDouble("damage");
			for (int j = 0; j < 12; j += 3) {
				new BukkitRunnable() {
					public void run() {
						entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1, 2);
						ItemStack stack = new ItemStack(Material.COAL);
						ItemMeta stack_meta = stack.getItemMeta();
						stack_meta.setDisplayName("SUDDEN_DEATH:" + UUID.randomUUID().toString());
						stack.setItemMeta(stack_meta);

						NoInteractItemEntity item = new NoInteractItemEntity(entity.getLocation().add(0, 1, 0), stack);
						item.getEntity().setVelocity(target.getLocation().add(0, 2, 0).toVector()
								.subtract(entity.getLocation().add(0, 1, 0).toVector()).normalize().multiply(2));
						new BukkitRunnable() {
							double ti = 0;

							public void run() {
								ti++;
								if (ti >= 20 || item.getEntity().isDead()) {
									item.close();
									cancel();
									return;
								}

								item.getEntity().getWorld().spawnParticle(Particle.SMOKE_NORMAL, item.getEntity().getLocation(), 0);
								for (Entity target : item.getEntity().getNearbyEntities(1.3, 1.3, 1.3))
									if (target instanceof Player) {
										item.close();
										cancel();
										Utils.damage((LivingEntity) target, damage, true);
										return;
									}
							}
						}.runTaskTimer(SuddenDeath.plugin, 0, 1);
					}
				}.runTaskLater(SuddenDeath.plugin, j);
			}
			return;
		}
		double damage = Feature.WITHER_RUSH.getDouble("damage");
		entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SPAWN, 4, 2);
		entity.removePotionEffect(PotionEffectType.SLOW);
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 255));
		new BukkitRunnable() {
			final Location loc = entity.getLocation();
			double ti = 0;

			public void run() {
				if (entity.getHealth() <= 0) {
					cancel();
					return;
				}
				ti += Math.PI / 20;
				for (int j = 0; j < 2; j++) {
					Location loc1 = loc.clone().add(Math.cos(j * Math.PI + ti), 2.2, Math.sin(j * Math.PI + ti));
					loc1.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc1, 0);
				}
				if (ti >= Math.PI) {
					cancel();
					Location sta = entity.getLocation().add(0, 1, 0);
					Vector v = target.getLocation().add(0, 1, 0).toVector().subtract(sta.toVector());
					for (double j = 0; j < 1; j += .03) {
						Location loc1 = sta.clone().add(v.getX() * j, v.getY() * j, v.getZ() * j);
						loc1.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc1, 0);
					}
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 2, 0);
					entity.teleport(target);
					Utils.damage(target, damage, true);
				}
			}

		}.runTaskTimer(SuddenDeath.plugin, 0, 1);
	}

	public static void loop3s_spider(Spider spider) {
		if (spider.getHealth() <= 0)
			return;

		Player target = (Player) spider.getTarget();
		if (!target.getWorld().equals(spider.getWorld()))
			return;

		if (Feature.ANGRY_SPIDERS.isEnabled(spider) && new Random().nextDouble() < .5) {
			double damage = Feature.ANGRY_SPIDERS.getDouble("damage");
			double duration = Feature.ANGRY_SPIDERS.getDouble("duration") * 20;
			double amplifier = Feature.ANGRY_SPIDERS.getDouble("amplifier");

			spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
			NoInteractItemEntity item = new NoInteractItemEntity(spider.getLocation().add(0, 1, 0), new ItemStack(Material.COBWEB));
			item.getEntity().setVelocity(target.getLocation().add(0, 1, 0).subtract(spider.getLocation().add(0, 1, 0)).toVector().multiply(.4));
			new BukkitRunnable() {
				int ti = 0;

				public void run() {
					ti++;
					if (ti > 20 || item.getEntity().isDead()) {
						item.close();
						cancel();
						return;
					}

					item.getEntity().getWorld().spawnParticle(Particle.CRIT, item.getEntity().getLocation(), 0);
					for (Entity entity : item.getEntity().getNearbyEntities(1, 1, 1))
						if (entity instanceof Player) {
							item.close();
							cancel();

							((Player) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) duration, (int) amplifier));
							Utils.damage((LivingEntity) entity, damage, true);
							return;
						}
				}
			}.runTaskTimer(SuddenDeath.plugin, 0, 1);
			return;
		}
		if (Feature.LEAPING_SPIDERS.isEnabled(spider)) {
			spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
			spider.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, spider.getLocation(), 8, 0, 0, 0, .1);
			Vector vec1 = spider.getLocation().toVector();
			Vector vec2 = ((Player) target).getEyeLocation().toVector();
			Vector vec = vec2.subtract(vec1).multiply(.3).setY(.3);
			spider.setVelocity(vec);
		}
	}

	public static void loop3s_player(Player player) {
		if (Utils.hasCreativeGameMode(player))
			return;

		if (Feature.HUNGER_NAUSEA.isEnabled(player) && player.getFoodLevel() < 8)
			player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 600, 0));

		PlayerData data = PlayerData.get(player);
		if (Feature.BLEEDING.isEnabled(player) && data.isBleeding() && SuddenDeath.plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT)
				&& player.getHealth() >= Feature.BLEEDING.getDouble("health-min"))
			Utils.damage(player, Feature.BLEEDING.getDouble("dps") * 3, Feature.BLEEDING.getBoolean("tug"));

		if (Feature.INFECTION.isEnabled(player) && data.isInfected() && SuddenDeath.plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT)
				&& player.getHealth() >= Feature.INFECTION.getDouble("health-min")) {
			Utils.damage(player, Feature.INFECTION.getDouble("dps") * 3, Feature.INFECTION.getBoolean("tug"));
			player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 600, 0));
			if (Feature.INFECTION.getBoolean("sound"))
				player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 666, 0);
		}
	}
}
