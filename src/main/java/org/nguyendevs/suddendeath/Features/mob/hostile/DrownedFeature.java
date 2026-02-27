package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.lang.reflect.Method;
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

    private void setPoseSafely(Drowned drowned, Pose pose) {
        try {
            Method setPoseMethod = drowned.getClass().getMethod("setPose", Pose.class);
            setPoseMethod.invoke(drowned, pose);
        } catch (Exception ignored) {
        }
    }

    private void applyTridentWrath(Drowned drowned) {
        try {
            if (drowned == null || drowned.getHealth() <= 0 || !(drowned.getTarget() instanceof Player target))
                return;
            if (!target.getWorld().equals(drowned.getWorld())
                    || drowned.getEquipment().getItemInMainHand().getType() != Material.TRIDENT)
                return;

            double chance = Feature.TRIDENT_WRATH.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance)
                return;

            int duration = (int) (Feature.TRIDENT_WRATH.getDouble("duration") * 20);
            double speed = Feature.TRIDENT_WRATH.getDouble("speed");

            drowned.getWorld().playSound(drowned.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);

            setPoseSafely(drowned, Pose.SPIN_ATTACK);
            new BukkitRunnable() {
                int ticks = 0;
                final Vector direction = target.getLocation().add(0, 1, 0).subtract(drowned.getLocation()).toVector()
                        .normalize();

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !drowned.isValid() || !target.isOnline() || drowned.isDead()) {
                            setPoseSafely(drowned, Pose.STANDING);
                            cancel();
                            return;
                        }
                        Vector velocity = direction.clone().multiply(speed);
                        drowned.setVelocity(velocity);
                        Location loc = drowned.getLocation();
                        loc.setDirection(direction);
                        try {
                            drowned.setRotation(loc.getYaw(), loc.getPitch());
                        } catch (NoSuchMethodError | Exception e) {
                            drowned.teleport(loc);
                            drowned.setVelocity(velocity);
                        }

                        Location center = drowned.getLocation().add(0, 0.5, 0);
                        drowned.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, center, 5, 0.3, 0.3, 0.3, 0.1);

                        if (center.distanceSquared(target.getLocation()) < 2.5) {
                            if (!Utils.hasCreativeGameMode(target)) {
                                Utils.damage(target, Feature.TRIDENT_WRATH.getDouble("damage"), true);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f,
                                        1.0f);
                                target.getWorld().spawnParticle(Particle.WATER_SPLASH,
                                        target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                            }
                            setPoseSafely(drowned, Pose.STANDING);
                            cancel();
                        }
                        if (drowned.getPose() != Pose.SPIN_ATTACK) {
                            setPoseSafely(drowned, Pose.SPIN_ATTACK);
                        }

                        ticks++;
                    } catch (Exception e) {
                        if (drowned != null)
                            setPoseSafely(drowned, Pose.STANDING);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Trident Wrath", e);
        }
    }
}