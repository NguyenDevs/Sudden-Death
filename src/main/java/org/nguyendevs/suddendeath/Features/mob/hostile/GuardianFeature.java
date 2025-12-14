package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.logging.Level;

public class GuardianFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Abyssal Vortex";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable guardianLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (Feature.ABYSSAL_VORTEX.isEnabled(world)) {
                            for (Guardian guardian : world.getEntitiesByClass(Guardian.class)) {
                                if (guardian.getTarget() instanceof Player)
                                    applyAbyssalVortex(guardian);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Abyssal Vortex task", e);
                }
            }
        };
        registerTask(guardianLoop.runTaskTimer(plugin, 0L, 80L));
    }

    private void applyAbyssalVortex(Guardian guardian) {
        try {
            if (guardian == null || guardian.getHealth() <= 0 || guardian.getTarget() == null || !(guardian.getTarget() instanceof Player target)) return;
            if (!target.getWorld().equals(guardian.getWorld())) return;

            double chance = Feature.ABYSSAL_VORTEX.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int duration = (int) (Feature.ABYSSAL_VORTEX.getDouble("duration") * 20);
            double strength = Feature.ABYSSAL_VORTEX.getDouble("strength");

            final Location fixedGuardianLoc = guardian.getEyeLocation().clone();
            final Location fixedTargetLoc = target.getLocation().clone();
            final Vector fixedDirection = fixedTargetLoc.toVector().subtract(fixedGuardianLoc.toVector()).normalize();
            final double vortexLength = 20;

            guardian.setAI(false);
            guardian.setInvulnerable(true);
            guardian.getWorld().playSound(fixedGuardianLoc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;
                final double maxRadius = 1.5;
                final double baseRadius = 0.2;

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !guardian.isValid()) {
                            guardian.setAI(true);
                            guardian.setInvulnerable(false);
                            cancel();
                            return;
                        }

                        Vector axis1, axis2;
                        if (Math.abs(fixedDirection.getY()) < 0.9) {
                            axis1 = new Vector(0, 1, 0).subtract(fixedDirection.clone().multiply(fixedDirection.getY())).normalize();
                        } else {
                            axis1 = new Vector(1, 0, 0).subtract(fixedDirection.clone().multiply(fixedDirection.getX())).normalize();
                        }
                        axis2 = fixedDirection.clone().crossProduct(axis1).normalize();

                        if (ticks % 3 == 0) {
                            for (int spiralIndex = 0; spiralIndex < 3; spiralIndex++) {
                                double phaseOffset = spiralIndex * (Math.PI * 2 / 3);
                                for (double distance = 0; distance <= vortexLength; distance += 0.7) {
                                    double progress = distance / vortexLength;
                                    double currentRadius = baseRadius + (maxRadius - baseRadius) * progress;
                                    double spiralAngle = phaseOffset + (distance * 1.2) + (ticks * 0.40);
                                    double x = Math.cos(spiralAngle) * currentRadius;
                                    double y = Math.sin(spiralAngle) * currentRadius;
                                    Vector spiralOffset = axis1.clone().multiply(x).add(axis2.clone().multiply(y));
                                    guardian.getWorld().spawnParticle(Particle.WATER_BUBBLE, fixedGuardianLoc.clone().add(fixedDirection.clone().multiply(distance)).add(spiralOffset), 1, 0, 0, 0, 0);
                                }
                            }
                        }

                        double searchRadius = Math.max(maxRadius + 2, vortexLength);
                        for (Entity entity : guardian.getWorld().getNearbyEntities(fixedGuardianLoc, searchRadius, searchRadius, searchRadius)) {
                            if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player)) continue;
                            Location playerLoc = player.getLocation();
                            Vector toPlayer = playerLoc.toVector().subtract(fixedGuardianLoc.toVector());
                            if (toPlayer.length() < 0.1) continue;

                            double distanceAlongLaser = toPlayer.dot(fixedDirection);
                            Vector projectionOnLaser = fixedDirection.clone().multiply(distanceAlongLaser);
                            Vector offsetFromLaser = toPlayer.clone().subtract(projectionOnLaser);
                            double distanceFromLaser = offsetFromLaser.length();
                            double progress = Math.max(0, Math.min(1, distanceAlongLaser / vortexLength));
                            double radiusAtPlayer = baseRadius + (maxRadius - baseRadius) * progress;

                            if (distanceFromLaser <= radiusAtPlayer && distanceAlongLaser >= 0 && distanceAlongLaser <= vortexLength) {
                                Vector pullForce = fixedDirection.clone().multiply(-strength / 20.0);
                                Vector spiralForce = (distanceFromLaser > 0.1) ? offsetFromLaser.clone().normalize().multiply(-strength / 30.0) : new Vector(0, 0, 0);
                                Vector totalForce = pullForce.add(spiralForce);
                                if (isValidVector(totalForce)) player.setVelocity(player.getVelocity().add(totalForce));

                                if (ticks % 40 == 0) {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (RANDOM.nextDouble() * 40 + 60), 1));
                                    guardian.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 1.2f);
                                }
                                if (playerLoc.distance(fixedGuardianLoc) <= 2.5) {
                                    Utils.damage(player, 2.0, true);
                                    guardian.getWorld().spawnParticle(Particle.WATER_SPLASH, playerLoc.add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
                                    guardian.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 0.9f);
                                }
                            }
                        }
                        ticks++;
                    } catch (Exception e) {
                        guardian.setAI(true);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Abyssal Vortex", e);
        }
    }

    private boolean isValidVector(Vector vector) {
        return vector != null && Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ()) &&
                Math.abs(vector.getX()) < 50 && Math.abs(vector.getY()) < 50 && Math.abs(vector.getZ()) < 50;
    }
}