package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.logging.Level;

public class BlazeFeatures extends AbstractFeature {

    @Override
    public String getName() {
        return "Blaze Features";
    }

    @Override
    protected void onEnable() {
        // Everburning Blazes Loop
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (Feature.EVERBURNING_BLAZES.isEnabled(world)) {
                            for (Blaze blaze : world.getEntitiesByClass(Blaze.class)) {
                                if (blaze.getTarget() instanceof Player) {
                                    loop3s_blaze(blaze);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Blaze loop task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L));

        // Homing Flame Barrage Loop
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
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
        }.runTaskTimer(plugin, 0L, 100L));
    }

    private void loop3s_blaze(Blaze blaze) {
        if (blaze == null || blaze.getHealth() <= 0) return;
        try {
            for (Entity entity : blaze.getNearbyEntities(10, 10, 10)) {
                if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player) || !blaze.hasLineOfSight(player)) continue;

                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 2f);
                double duration = Feature.EVERBURNING_BLAZES.getDouble("burn-duration") * 20;
                player.setFireTicks((int) duration);

                Location playerLoc = player.getLocation().add(0.0D, 0.75D, 0.0D);
                Location blazeLoc = blaze.getLocation().add(0.0D, 1.0D, 0.0D);
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
            plugin.getLogger().log(Level.WARNING, "Error in Blaze loop for entity: " + blaze.getUniqueId(), e);
        }
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
            for (int i = 0; i < amount; i++) angles[i] = (2 * Math.PI / amount) * i;

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

                        try {
                            Sound breezeSound = Sound.valueOf("ENTITY_BREEZE_IDLE_AIR");
                            target.getWorld().playSound(target.getLocation(), breezeSound, 1.0f, 1.5f);
                        } catch (IllegalArgumentException | NullPointerException ignored) {

                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 2.0f, 0.1f);
                        }

                        shootHomingBeam(blazeEyeLoc, beamDirection, target, damage);
                        beamCount++;
                    } catch (Exception e) {
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
                    if (ticks >= maxTicks || !target.isOnline()) { cancel(); return; }
                    if (ticks < 10) currentLoc.add(initialVelocity);
                    else {
                        Location targetLoc = target.getLocation().add(0, 1, 0);
                        Vector dir = targetLoc.subtract(currentLoc).toVector().normalize();
                        currentLoc.add(dir.multiply(0.8));
                    }
                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 1, 0, 0, 0, 0);
                    if (currentLoc.distanceSquared(target.getLocation().add(0, 1, 0)) < 1.0) {
                        if (!Utils.hasCreativeGameMode(target)) {
                            Utils.damage(target, damage, true);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 0.5f);
                            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                        }
                        cancel();
                        return;
                    }
                    ticks++;
                } catch (Exception e) { cancel(); }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}