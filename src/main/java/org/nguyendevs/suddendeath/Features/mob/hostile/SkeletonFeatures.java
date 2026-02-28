package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.NoInteractItemEntity;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.Objects;
import java.util.logging.Level;

public class SkeletonFeatures extends AbstractFeature {

    private static final double TICK_INTERVAL = 0.5;
    private static final int MAX_TICKS = 20;

    @Override
    public String getName() {
        return "Skeleton Features";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (!Feature.BONE_WIZARDS.isEnabled(world)) continue;
                        for (Skeleton skeleton : world.getEntitiesByClass(Skeleton.class)) {
                            if (skeleton.getTarget() instanceof Player && isBoneWizard(skeleton)) {
                                loop3s_skeleton(skeleton);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Skeleton loop", e);
                }
            }
        }.runTaskTimer(plugin, 20L, 60L));
    }

    @EventHandler
    public void onSkeletonShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!Feature.BONE_GRENADES.isEnabled(skeleton)) return;
        if (!(skeleton.getTarget() instanceof Player target)) return;

        try {
            double chance = Feature.BONE_GRENADES.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() >= chance) return;

            event.setCancelled(true);
            double damage = Feature.BONE_GRENADES.getDouble("damage");
            skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);

            NoInteractItemEntity grenade = new NoInteractItemEntity(
                    skeleton.getEyeLocation(), new ItemStack(Material.SKELETON_SKULL));
            grenade.getEntity().setVelocity(
                    target.getLocation().subtract(skeleton.getLocation()).toVector().multiply(0.05).setY(0.6));

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
                        grenadeEntity.getWorld().spawnParticle(Particle.SMOKE, grenadeEntity.getLocation(), 0);
                        if (grenadeEntity.isOnGround()) {
                            Location loc = grenadeEntity.getLocation();
                            World world = grenadeEntity.getWorld();
                            grenade.close();
                            world.spawnParticle(Particle.POOF, loc, 24, 3, 3, 3, 0);
                            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                            for (Entity nearby : grenadeEntity.getNearbyEntities(6, 6, 6)) {
                                if (nearby instanceof Player player) {
                                    Utils.damage(player, damage, true);
                                }
                            }
                            cancel();
                        }
                    } catch (Exception e) {
                        grenade.close();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Bone Grenade", e);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Skeleton)) return;
        if (!Feature.SHOCKING_SKELETON_ARROWS.isEnabled(player)) return;

        try {
            double chancePercent = Feature.SHOCKING_SKELETON_ARROWS.getDouble("chance-percent");
            if (Math.random() * 100 > chancePercent) return;
            applyShockingArrows(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Shocking Skeleton Arrows", e);
        }
    }

    private void applyShockingArrows(Player player) {
        double duration = Feature.SHOCKING_SKELETON_ARROWS.getDouble("shock-duration");
        Location loc = player.getLocation();
        int durationTicks = (int) (duration * 20);

        new BukkitRunnable() {
            double ticks = 0;

            @Override
            public void run() {
                try {
                    for (int j = 0; j < 3; j++) {
                        ticks += Math.PI / 15;
                        loc.getWorld().spawnParticle(Particle.SMOKE,
                                loc.clone().add(Math.cos(ticks), 1, Math.sin(ticks)), 0);
                    }
                    if (ticks >= Math.PI * 2) cancel();
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        new BukkitRunnable() {
            int ticksPassed = 0;

            @Override
            public void run() {
                try {
                    if (ticksPassed++ > durationTicks) {
                        cancel();
                        return;
                    }
                    player.playHurtAnimation(0.003f);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void loop3s_skeleton(Skeleton skeleton) {
        if (skeleton == null || skeleton.getHealth() <= 0) return;
        if (!(skeleton.getTarget() instanceof Player target)) return;
        if (!target.getWorld().equals(skeleton.getWorld())) return;

        if (RANDOM.nextDouble() < 0.5) {
            spawnFireball(skeleton, target);
        } else {
            double damage = Feature.BONE_WIZARDS.getDouble("frost-curse-damage");
            double duration = Feature.BONE_WIZARDS.getDouble("frost-curse-duration");
            double amplifier = Feature.BONE_WIZARDS.getDouble("frost-curse-amplifier");
            applyFrostCurse(skeleton, target, damage, duration, amplifier);
        }
    }

    private void spawnFireball(Skeleton skeleton, Player target) {
        double damage = Feature.BONE_WIZARDS.getDouble("fireball-damage");
        double duration = Feature.BONE_WIZARDS.getDouble("fireball-duration");
        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0f, 0.0f);

        Vector direction = target.getLocation().add(0, 0.5, 0).toVector()
                .subtract(skeleton.getLocation().add(0, 0.75, 0).toVector())
                .normalize().multiply(TICK_INTERVAL);
        Location loc = skeleton.getEyeLocation();

        new BukkitRunnable() {
            double ticks = 0;

            @Override
            public void run() {
                try {
                    for (int j = 0; j < 2; j++) {
                        ticks += TICK_INTERVAL;
                        loc.add(direction);
                        loc.getWorld().spawnParticle(Particle.FLAME, loc, 4, 0.1, 0.1, 0.1, 0);
                        loc.getWorld().spawnParticle(Particle.LAVA, loc, 0);

                        for (Player player : skeleton.getWorld().getPlayers()) {
                            if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < 1.7 * 1.7) {
                                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 0);
                                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                                Utils.damage(player, damage, true);
                                if (duration > 0) player.setFireTicks((int) (duration * 20));
                                cancel();
                                return;
                            }
                        }
                    }
                    if (ticks > MAX_TICKS) cancel();
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void applyFrostCurse(Skeleton skeleton, Player target, double damage, double duration, double amplifier) {
        try {
            Location loc = target.getLocation();
            double radius = 4.0;
            double radiusSquared = radius * radius;

            new BukkitRunnable() {
                double ticks = 0;

                @Override
                public void run() {
                    try {
                        ticks++;
                        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 0);
                        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 2.0f, 2.0f);

                        if (ticks > 27) {
                            loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.0f);

                            for (double j = 0; j < Math.PI * 2; j += Math.PI / 36) {
                                loc.getWorld().spawnParticle(Particle.CLOUD,
                                        loc.clone().add(Math.cos(j) * radius, 0.1, Math.sin(j) * radius), 0);
                            }

                            for (Player player : skeleton.getWorld().getPlayers()) {
                                if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < radiusSquared) {
                                    Utils.damage(player, damage, true);
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                                            (int) (duration * 20), (int) amplifier));
                                    player.setFreezeTicks(140 + (int) (duration * 20));
                                    cancel();
                                    return;
                                }
                            }
                            cancel();
                        }
                    } catch (Exception e) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying frost curse", e);
        }
    }

    private boolean isBoneWizard(Skeleton skeleton) {
        return skeleton.customName() != null &&
                Objects.equals(skeleton.customName(), net.kyori.adventure.text.Component.text("Bone Wizard"));
    }
}