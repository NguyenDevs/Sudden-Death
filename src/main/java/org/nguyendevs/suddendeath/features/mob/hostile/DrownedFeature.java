package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.logging.Level;

public class DrownedFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Trident's Wrath";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable drownedLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.TRIDENT_WRATH.isEnabled(world)) {
                            for (Drowned drowned : world.getEntitiesByClass(Drowned.class)) {
                                if (drowned.getTarget() instanceof Player)
                                    applyTridentWrath(drowned);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Drowned loop task", e);
                }
            }
        };
        drownedLoop.runTaskTimer(plugin, 0L, 100L);
        registerTask((BukkitTask) drownedLoop);
    }

    private void applyTridentWrath(Drowned drowned) {
        try {
            if (drowned == null || drowned.getHealth() <= 0 || drowned.getTarget() == null || !(drowned.getTarget() instanceof Player target)) return;
            if (!target.getWorld().equals(drowned.getWorld()) || drowned.getEquipment().getItemInMainHand().getType() != Material.TRIDENT) return;

            double chance = Feature.TRIDENT_WRATH.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int duration = (int) (Feature.TRIDENT_WRATH.getDouble("duration") * 20);
            double speed = Feature.TRIDENT_WRATH.getDouble("speed");
            double spinSpeed = 30.0;

            drowned.getWorld().playSound(drowned.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;
                final Location startLoc = drowned.getLocation().clone();
                final Vector direction = target.getLocation().add(0, 1, 0).subtract(startLoc).toVector().normalize();

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !drowned.isValid() || !target.isOnline()) {
                            cancel();
                            return;
                        }

                        Vector velocity = direction.clone().multiply(speed);
                        drowned.setVelocity(velocity);

                        float pitch = (float) ((drowned.getLocation().getPitch() + spinSpeed) % 360);
                        drowned.setRotation(drowned.getLocation().getYaw(), pitch);

                        Location loc = drowned.getLocation().clone().add(0, 1, 0);
                        double radius = 0.6;
                        double particleSpinSpeed = 0.3;
                        for (double height = -0.8; height <= 0.8; height += 0.2) {
                            double spiralAngle = (height * Math.PI * 2) + (ticks * particleSpinSpeed);
                            double x = Math.cos(spiralAngle) * radius;
                            double z = Math.sin(spiralAngle) * radius;
                            loc.getWorld().spawnParticle(Particle.WATER_SPLASH, loc.clone().add(x, height, z), 1, 0, 0, 0, 0);
                            loc.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc.clone().add(x, height, z), 1, 0, 0, 0, 0);
                        }

                        if (loc.distanceSquared(target.getLocation()) < 2.0) {
                            if (!Utils.hasCreativeGameMode(target)) {
                                Utils.damage(target, Feature.TRIDENT_WRATH.getDouble("damage"), true);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 1.0f);
                                target.getWorld().spawnParticle(Particle.WATER_SPLASH, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0);
                            }
                            cancel();
                        }
                        ticks++;
                    } catch (Exception e) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Trident Wrath", e);
        }
    }
}