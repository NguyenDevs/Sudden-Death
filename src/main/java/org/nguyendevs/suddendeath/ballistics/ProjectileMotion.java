package org.nguyendevs.suddendeath.ballistics;

import org.bukkit.entity.*;

import java.util.HashMap;
import java.util.Map;

public class ProjectileMotion {
    public static final double DEFAULT_PLAYER_ACCELERATION = -0.0784;

    private static final double epsilon = 1.0E-6D;

    final private double gravityAcceleration;
    final private double drag;
    final private boolean dragAfter;

    final private static Map<EntityType,ProjectileMotion> CACHE = new HashMap<>();

    private ProjectileMotion(final double gravityAcceleration,
                             final double drag,
                             final boolean dragAfter) {
        this.gravityAcceleration = gravityAcceleration;
        this.drag = drag;
        this.dragAfter = dragAfter;
    }

    double getGravityAcceleration() {
        return gravityAcceleration;
    }

    double getDrag() {
        return drag;
    }

    boolean hasGravityAcceleration() {
        return gravityAcceleration < - epsilon;
    }

    boolean hasDrag() {
        return drag > epsilon;
    }

    boolean getDragAfter() {
        return dragAfter;
    }

    static ProjectileMotion getProjectileMotion(final Projectile projectile) {
        final EntityType projectileType = projectile.getType();

        final ProjectileMotion motion;
        if(CACHE.containsKey(projectileType)) {
            motion = CACHE.get(projectileType);
        } else {
            motion = getProjectileMotionWithoutCache(projectile);
            CACHE.put(projectileType, motion);
        }

        return motion;
    }

    private static ProjectileMotion getProjectileMotionWithoutCache(final Projectile projectile) {
        if(projectile.getClass().getName().endsWith("Arrow")
                || projectile.getClass().getName().endsWith("Trident")) {
            return new ProjectileMotion(-0.05, 0.01, false);
        } else if (projectile instanceof ThrownPotion) {
            return new ProjectileMotion(-0.05, 0.01, false);
        } else if (projectile instanceof WitherSkull) { // sub-instance of Fireball
            return new ProjectileMotion(0.0, 0.27, true);
        } else if (projectile instanceof Fireball) {
            return new ProjectileMotion(0.0, 0.05, true);
        } else if ((projectile instanceof Egg) || (projectile instanceof Snowball)
                || (projectile instanceof EnderPearl)) {
            return new ProjectileMotion(-0.03, 0.01, false);
        } else {
            return null;
        }
    }
}
