package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
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
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
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
        registerTask(drownedLoop.runTaskTimer(plugin, 0L, 100L));
    }

    private void applyTridentWrath(Drowned drowned) {
        try {
            if (drowned == null || drowned.getHealth() <= 0 || drowned.getTarget() == null || !(drowned.getTarget() instanceof Player target)) return;
            if (!target.getWorld().equals(drowned.getWorld()) || drowned.getEquipment().getItemInMainHand().getType() != Material.TRIDENT) return;

            double chance = Feature.TRIDENT_WRATH.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int duration = (int) (Feature.TRIDENT_WRATH.getDouble("duration") * 20);
            double speed = Feature.TRIDENT_WRATH.getDouble("speed");
            float spinSpeed = 45.0f;

            drowned.getWorld().playSound(drowned.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;
                final Vector direction = target.getLocation().add(0, 1, 0).subtract(drowned.getLocation()).toVector().normalize();
                float currentYaw = drowned.getLocation().getYaw();

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !drowned.isValid() || !target.isOnline() || drowned.isDead()) {
                            cancel();
                            return;
                        }
                        Vector velocity = direction.clone().multiply(speed);
                        currentYaw = (currentYaw + spinSpeed) % 360;

                        try {

                            drowned.setRotation(currentYaw, 0f);
                            drowned.setVelocity(velocity);

                        } catch (NoSuchMethodError | Exception e) {
                            Location loc = drowned.getLocation();
                            loc.setYaw(currentYaw);
                            loc.setPitch(0f);

                            drowned.teleport(loc);
                            drowned.setVelocity(velocity);
                        }
                        Location particleLoc = drowned.getLocation().clone().add(0, 1, 0);
                        double radius = 0.7;
                        for (int i = 0; i < 2; i++) {
                            double angle = (ticks * 0.5) + (i * Math.PI);
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            particleLoc.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLoc.clone().add(x, 0, z), 2, 0, 0, 0, 0);
                            particleLoc.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, particleLoc.clone().add(x, -0.5, z), 1, 0, 0, 0, 0.1);
                        }

                        if (particleLoc.distanceSquared(target.getLocation()) < 2.5) {
                            if (!Utils.hasCreativeGameMode(target)) {
                                Utils.damage(target, Feature.TRIDENT_WRATH.getDouble("damage"), true);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 1.0f);
                                //target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1);
                                target.getWorld().spawnParticle(Particle.WATER_SPLASH, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
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