package org.nguyendevs.suddendeath.ballistics;


import com.gmail.uprial.takeaim.fixtures.FireballAdapter;
import com.gmail.uprial.takeaim.fixtures.FireballAdapterNotSupported;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.SuddenDeath;

public class ProjectileHoming {
    private final SuddenDeath plugin;

    private Boolean isFireballAdapterSupported = null;

    private static final double ARROW_TERMINAL_VELOCITY = 100.0D / 20;

    private static final double DRAG_APPROXIMATION_EPSILON = 0.04D;

    private static final int ARROW_DRAG_APPROXIMATION_ATTEMPTS = 5;
    private static final int FIREBALL_DRAG_APPROXIMATION_ATTEMPTS = 10;

    public ProjectileHoming(final SuddenDeath plugin) {
        this.plugin = plugin;

    }

    public boolean hasProjectileMotion(final Projectile projectile) {
        return ProjectileMotion.getProjectileMotion(projectile) != null;
    }

    public void aimProjectile(final LivingEntity projectileSource, final Projectile projectile, final Player targetPlayer) {
        if (projectile instanceof Fireball) {
            if(isFireballAdapterSupported == null || isFireballAdapterSupported) {
                try {
                    aimFireball(projectileSource, (Fireball) projectile, targetPlayer);
                    isFireballAdapterSupported = true;
                } catch (FireballAdapterNotSupported e) {

                    isFireballAdapterSupported = false;
                }
            }
        } else {
            aimArrow(projectileSource, projectile, targetPlayer);
        }
    }

    private void aimArrow(final LivingEntity projectileSource, final Projectile projectile, final Player targetPlayer) {
        final Location targetLocation = getAimPoint(targetPlayer);

        targetLocation.subtract(projectile.getLocation());

        final Vector initialProjectileVelocity = projectile.getVelocity();


        final double ticksInFly = targetLocation.length() / initialProjectileVelocity.length();


        final Vector velocity = plugin.getPlayerTracker().getPlayerMovementVector(targetPlayer);
        targetLocation.add(velocity.clone().multiply(ticksInFly));

        final ProjectileMotion motion = ProjectileMotion.getProjectileMotion(projectile);

        if(motion.hasGravityAcceleration()) {
            final double maxHeight = -Math.pow(ARROW_TERMINAL_VELOCITY, 2.0D) / motion.getGravityAcceleration();

            if (targetLocation.getY() > maxHeight) {
                        targetLocation.getY(), ticksInFly, maxHeight));
                return;
            }
        }

        final Vector newVelocity;
        {
            final double vx = targetLocation.getX() / ticksInFly;
            final double vz = targetLocation.getZ() / ticksInFly;

            final double vy;

            if (motion.hasGravityAcceleration()) {
                final double y1 = 0.0D;
                final double y2 = targetLocation.getY();
                final double t = ticksInFly;

                vy = ((y2 - (motion.getGravityAcceleration() * t * t / 2.0D) - y1) / t);
            } else {
                vy = targetLocation.getY() / ticksInFly;
            }

            newVelocity = new Vector(vx, vy, vz);
        }

        // Consider environmental drag.
        if(motion.hasDrag()) {
            final double q = (1.0D - motion.getDrag());
            final double normalizedDistanceWithDrag = (1.0D - Math.pow(q, ticksInFly)) / (1.0D - q);
            final double dragFix = ticksInFly / normalizedDistanceWithDrag;

            if(motion.hasGravityAcceleration()) {
                newVelocity.setX(newVelocity.getX() * dragFix);
                newVelocity.setZ(newVelocity.getZ() * dragFix);

                double vy = newVelocity.getY()
                        + targetLocation.getY() / ticksInFly * (dragFix - 1.0D);

                int attempts = ARROW_DRAG_APPROXIMATION_ATTEMPTS;
                do {
                    attempts -= 1;

                    final double t_y = getAcceleratedAndDragged(
                            vy,
                            ticksInFly,
                            motion.getGravityAcceleration(),
                            motion.getDrag(),
                            motion.getDragAfter());

                    if(Math.abs(targetLocation.getY() - t_y) < DRAG_APPROXIMATION_EPSILON) {
                        break;
                    }

                    vy += (targetLocation.getY() - t_y) / ticksInFly * dragFix;

                } while (attempts > 0);

                newVelocity.setY(vy);
            } else {
                newVelocity.multiply(dragFix);
            }
        }
        projectile.setVelocity(newVelocity);

    }

    private void aimFireball(final LivingEntity projectileSource, final Fireball fireball, final Player targetPlayer) throws FireballAdapterNotSupported {
        final Vector velocity = plugin.getPlayerTracker().getPlayerMovementVector(targetPlayer);
        final Vector acceleration = FireballAdapter.getAcceleration(fireball);
        final ProjectileMotion motion = ProjectileMotion.getProjectileMotion(fireball);

        final Location location = getAimPoint(targetPlayer);
        location.subtract(fireball.getLocation());

        double ticksToCollide;
        final boolean isLowDrag;
        {
            final double lowDragTicksToCollide;
            {
                lowDragTicksToCollide = velocity.length()
                        + Math.pow(Math.pow(velocity.length(), 2.0D) + 2.0D * acceleration.length() * location.length(), 0.5D)
                        / (acceleration.length());
            }

            final double highDragTicksToCollide;
            {
                final double maxFireballVelocity = acceleration.length() / motion.getDrag() - acceleration.length();
                if (maxFireballVelocity <= velocity.length()) {
                    return;
                }

                highDragTicksToCollide = (location.length() + (1.0D - motion.getDrag()))
                        / (maxFireballVelocity - velocity.length());
            }

            if(lowDragTicksToCollide > highDragTicksToCollide) {
                isLowDrag = true;
                ticksToCollide = lowDragTicksToCollide;
            } else {
                isLowDrag = false;
                ticksToCollide = highDragTicksToCollide;
            }
        }

        Location targetLocation;

        int attempts = FIREBALL_DRAG_APPROXIMATION_ATTEMPTS;
        do {
            attempts -= 1;

            targetLocation = location.clone()
                    .add(velocity.clone().multiply(ticksToCollide));

            final double targetDistance = targetLocation.length();

            final double actualDistance = getAcceleratedAndDragged(
                    0.0D,
                    ticksToCollide,
                    acceleration.length(),
                    motion.getDrag(),
                    motion.getDragAfter());


            if(Math.abs(actualDistance - targetDistance) < DRAG_APPROXIMATION_EPSILON) {
                break;
            }
            if(isLowDrag) {
                ticksToCollide = ticksToCollide * Math.sqrt(targetDistance / actualDistance);
            } else {
                final double playerVelocity = (targetDistance - location.length()) / ticksToCollide;
                final double maxFireballVelocity = acceleration.length() / motion.getDrag() - acceleration.length();
                if (maxFireballVelocity <= playerVelocity) {
                    return;
                } else {
                    ticksToCollide += (targetDistance - actualDistance) / (maxFireballVelocity - playerVelocity);
                }
            }
        } while (attempts > 0);

        final Vector newAcceleration = targetLocation.toVector();
        newAcceleration.multiply(acceleration.length() / newAcceleration.length());
        FireballAdapter.setAcceleration(fireball, newAcceleration);

    }

    private double getAcceleratedAndDragged(double velocity,
                                            final double doubleTicks,
                                            final double acceleration,
                                            final double drag,
                                            final boolean dragAfter) {
        // Apply velocity 1st time before gravity and drag
        double position = velocity;
        int intTicks = (int)Math.floor(doubleTicks);
        for (int i = 0; i < intTicks; i++) {
            if(dragAfter) {
                velocity += acceleration;
                velocity *= (1 - drag);
            } else {
                velocity *= (1 - drag);
                velocity += acceleration;
            }
            position += velocity;
        }
        // Apply the last, potentially partial tick
        if(doubleTicks > intTicks) {
            if(dragAfter) {
                velocity += acceleration;
                velocity *= (1 - drag);
            } else {
                velocity *= (1 - drag);
                velocity += acceleration;
            }
            position += velocity * (doubleTicks - intTicks);
        }

        return position;
    }

    private Location getAimPoint(final Player targetPlayer) {
        return targetPlayer.getLocation()
                .add(targetPlayer.getEyeLocation())
                .multiply(0.5D);
    }

}