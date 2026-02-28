package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Bukkit;
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

public class AbyssalVortexFeature extends AbstractFeature {

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
                    for (World world : Bukkit.getWorlds()) {
                        if (!Feature.ABYSSAL_VORTEX.isEnabled(world)) continue;
                        for (Guardian guardian : world.getEntitiesByClass(Guardian.class)) {
                            if (guardian.getTarget() instanceof Player) applyAbyssalVortex(guardian);
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
            if (guardian.getHealth() <= 0 || !(guardian.getTarget() instanceof Player target)) return;
            if (!target.getWorld().equals(guardian.getWorld())) return;

            double chance = Feature.ABYSSAL_VORTEX.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int duration = (int) (Feature.ABYSSAL_VORTEX.getDouble("duration") * 20);
            double strength = Feature.ABYSSAL_VORTEX.getDouble("strength");

            final Location fixedGuardianLoc = guardian.getEyeLocation().clone();
            final Vector fixedDirection = target.getLocation().toVector()
                    .subtract(fixedGuardianLoc.toVector()).normalize();

            guardian.setAI(false);
            guardian.setInvulnerable(true);
            guardian.getWorld().playSound(fixedGuardianLoc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !guardian.isValid()) {
                            guardian.setAI(true);
                            guardian.setInvulnerable(false);
                            cancel();
                            return;
                        }

                        if (ticks % 3 == 0) spawnVortexParticles(fixedGuardianLoc, fixedDirection);
                        applyVortexForces(guardian, fixedGuardianLoc, fixedDirection, strength, ticks);

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

    private void spawnVortexParticles(Location origin, Vector direction) {
        Vector[] axes = getPerpendicularAxes(direction);
        Vector axis1 = axes[0];
        Vector axis2 = axes[1];

        for (int spiralIndex = 0; spiralIndex < 3; spiralIndex++) {
            double phaseOffset = spiralIndex * (Math.PI * 2.0 / 3);
            for (double dist = 0; dist <= 20.0; dist += 0.7) {
                double progress = dist / 20.0;
                double radius = 0.2 + (1.5 - 0.2) * progress;
                double angle = phaseOffset + (dist * 1.2);
                Vector offset = axis1.clone().multiply(Math.cos(angle) * radius)
                        .add(axis2.clone().multiply(Math.sin(angle) * radius));
                Location loc = origin.clone().add(direction.clone().multiply(dist)).add(offset);
                origin.getWorld().spawnParticle(Particle.BUBBLE, loc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void applyVortexForces(Guardian guardian, Location origin, Vector direction,
                                   double strength, int ticks) {
        double searchRadius = Math.max(1.5 + 2, 20.0);

        for (Entity entity : guardian.getWorld().getNearbyEntities(origin, searchRadius, searchRadius, searchRadius)) {
            if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player)) continue;

            Location playerLoc = player.getLocation();
            Vector toPlayer = playerLoc.toVector().subtract(origin.toVector());
            if (toPlayer.length() < 0.1) continue;

            double distAlong = toPlayer.dot(direction);
            if (distAlong < 0 || distAlong > 20.0) continue;

            Vector offsetFromLaser = toPlayer.subtract(direction.clone().multiply(distAlong));
            double distFromLaser = offsetFromLaser.length();
            double progress = distAlong / 20.0;
            double radiusAtPlayer = 0.2 + (1.5 - 0.2) * progress;

            if (distFromLaser > radiusAtPlayer) continue;

            Vector pullForce = direction.clone().multiply(-strength / 20.0);
            Vector spiralForce = distFromLaser > 0.1
                    ? offsetFromLaser.normalize().multiply(-strength / 30.0)
                    : new Vector(0, 0, 0);
            Vector totalForce = pullForce.add(spiralForce);
            if (isValidVector(totalForce)) player.setVelocity(player.getVelocity().add(totalForce));

            if (ticks % 40 == 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA,
                        (int) (RANDOM.nextDouble() * 40 + 60), 1));
                guardian.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 1.2f);
            }

            if (playerLoc.distance(origin) <= 2.5) {
                Utils.damage(player, 2.0, true);
                guardian.getWorld().spawnParticle(Particle.SPLASH, playerLoc.clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
                guardian.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 0.9f);
            }
        }
    }

    private Vector[] getPerpendicularAxes(Vector direction) {
        Vector axis1 = Math.abs(direction.getY()) < 0.9
                ? new Vector(0, 1, 0).subtract(direction.clone().multiply(direction.getY())).normalize()
                : new Vector(1, 0, 0).subtract(direction.clone().multiply(direction.getX())).normalize();
        Vector axis2 = direction.clone().crossProduct(axis1).normalize();
        return new Vector[]{axis1, axis2};
    }

    private boolean isValidVector(Vector v) {
        return Double.isFinite(v.getX()) && Double.isFinite(v.getY()) && Double.isFinite(v.getZ())
                && Math.abs(v.getX()) < 50 && Math.abs(v.getY()) < 50 && Math.abs(v.getZ()) < 50;
    }
}