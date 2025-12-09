package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.listener.Loops;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.listener.Loops;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.logging.Level;

public class BlazeFeatures extends AbstractFeature {

    @Override
    public String getName() {
        return "Blaze Features (Everburning + Homing Flame Barrage)";
    }

    @Override
    protected void onEnable() {
        // Everburning Blazes loop
        BukkitRunnable blazeLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.EVERBURNING_BLAZES.isEnabled(world)) {
                            for (Blaze blaze : world.getEntitiesByClass(Blaze.class)) {
                                if (blaze.getTarget() instanceof Player) {
                                    Loops.loop3s_blaze(blaze);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Blaze loop task", e);
                }
            }
        };
        blazeLoop.runTaskTimer(plugin, 0L, 60L);
        registerTask((BukkitTask) blazeLoop);

        // Homing Flame Barrage loop
        BukkitRunnable homingLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.HOMING_FLAME_BARRAGE.isEnabled(world)) {
                            for (Blaze blaze : world.getEntitiesByClass(Blaze.class)) {
                                if (blaze.getTarget() instanceof Player) {
                                    applyHomingFlameBarrage(blaze);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Homing Flame Barrage task", e);
                }
            }
        };
        homingLoop.runTaskTimer(plugin, 0L, 100L);
        registerTask((BukkitTask) homingLoop);
    }

    private void applyHomingFlameBarrage(Blaze blaze) {
        if (blaze == null || blaze.getHealth() <= 0 || !(blaze.getTarget() instanceof Player target)) return;
        if (!target.getWorld().equals(blaze.getWorld())) return;

        try {
            double chance = Feature.HOMING_FLAME_BARRAGE.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int amount = (int) Feature.HOMING_FLAME_BARRAGE.getDouble("shoot-amount");
            double damage = Feature.HOMING_FLAME_BARRAGE.getDouble("damage");

            blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

            Location blazeEyeLoc = blaze.getLocation().add(0, 1.2, 0);
            Vector initialDirection = target.getLocation().add(0, 1, 0).subtract(blazeEyeLoc).toVector().normalize();

            double[] angles = new double[amount];
            for (int i = 0; i < amount; i++) {
                angles[i] = (2 * Math.PI / amount) * i;
            }

            new BukkitRunnable() {
                int beamCount = 0;

                @Override
                public void run() {
                    try {
                        if (beamCount >= amount || blaze.isDead() || !target.isOnline()) {
                            cancel();
                            return;
                        }

                        double angle = angles[beamCount];
                        Vector beamDirection = initialDirection.clone();
                        double cosAngle = Math.cos(angle);
                        double sinAngle = Math.sin(angle);
                        double y = beamDirection.getY() * cosAngle - beamDirection.getZ() * sinAngle;
                        double z = beamDirection.getY() * sinAngle + beamDirection.getZ() * cosAngle;
                        beamDirection.setY(y).setZ(z);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR, 1.0f, 1.5f);

                        shootHomingBeam(blazeEyeLoc, beamDirection, target, damage);
                        beamCount++;
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error in flame barrage sequence", e);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 5);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Homing Flame Barrage", e);
        }
    }

    private void shootHomingBeam(Location start, Vector direction, Player target, double damage) {
        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = start.clone();
            final int maxTicks = 80;
            Vector initialVelocity = direction.clone().multiply(0.5);

            @Override
            public void run() {
                try {
                    if (ticks >= maxTicks || !target.isOnline()) {
                        cancel();
                        return;
                    }

                    if (ticks < 10) {
                        currentLoc.add(initialVelocity);
                    } else {
                        Location targetLoc = target.getLocation().add(0, 1, 0);
                        Vector dir = targetLoc.subtract(currentLoc).toVector().normalize();
                        currentLoc.add(dir.multiply(0.8));
                    }

                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 1, 0, 0, 0, 0);

                    Location targetLoc = target.getLocation().add(0, 1, 0);
                    if (currentLoc.distanceSquared(targetLoc) < 1.0) {
                        if (!Utils.hasCreativeGameMode(target)) {
                            Utils.damage(target, damage, true);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 0.5f);
                            target.getWorld().spawnParticle(Particle.FLAME,
                                    target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                        }
                        cancel();
                        return;
                    }

                    ticks++;
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
