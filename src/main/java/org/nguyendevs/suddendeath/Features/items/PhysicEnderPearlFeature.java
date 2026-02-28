package org.nguyendevs.suddendeath.Features.items;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class PhysicEnderPearlFeature extends AbstractFeature {

    private final Set<UUID> allowed = new HashSet<>();
    private NamespacedKey bounces;

    @Override
    public String getName() {
        return "Physic EnderPearl";
    }

    @Override
    protected void onEnable() {
        this.bounces = new NamespacedKey(plugin, "bounces");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        try {
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
            Player player = event.getPlayer();
            if (!allowed.remove(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            if (Feature.PHYSIC_ENDER_PEARL.isEnabled(player.getWorld())) {
                player.getWorld().playSound(event.getFrom(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 1.0f);
                player.getWorld().playSound(event.getTo(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in PhysicEnderPearlFeature.onPlayerTeleport", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        try {
            if (event.getEntityType() != EntityType.ENDER_PEARL) return;
            if (!(event.getEntity().getShooter() instanceof Player player)) return;

            if (!Feature.PHYSIC_ENDER_PEARL.isEnabled(player.getWorld())) {
                allowed.add(player.getUniqueId());
                int overLimit = (int) Feature.PHYSIC_ENDER_PEARL.getDouble("max-bounces") + 1;
                event.getEntity().getPersistentDataContainer().set(bounces, PersistentDataType.INTEGER, overLimit);
            } else {
                event.getEntity().getPersistentDataContainer().set(bounces, PersistentDataType.INTEGER, 0);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in PhysicEnderPearlFeature.onProjectileLaunch", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        try {
            if (event.getEntityType() != EntityType.ENDER_PEARL) return;
            EnderPearl oldPearl = (EnderPearl) event.getEntity();

            Vector normal;
            if (event.getHitEntity() != null) {
                normal = oldPearl.getLocation().toVector()
                        .subtract(event.getHitEntity().getBoundingBox().getCenter()).normalize();
            } else if (event.getHitBlockFace() != null) {
                normal = event.getHitBlockFace().getDirection();
            } else {
                normal = new Vector(0, 1, 0);
            }

            PersistentDataContainer data = oldPearl.getPersistentDataContainer();
            int bounceCount = getBounceCount(data);
            double maxBounces = Feature.PHYSIC_ENDER_PEARL.getDouble("max-bounces");
            double minVelocityThreshold = Feature.PHYSIC_ENDER_PEARL.getDouble("min-velocity-threshold");

            if (bounceCount >= maxBounces
                    || oldPearl.getVelocity().lengthSquared() < minVelocityThreshold * minVelocityThreshold) {
                allowShooter(oldPearl);
                return;
            }

            Vector velocity = oldPearl.getVelocity().clone();
            double dotProduct = velocity.dot(normal);
            Vector reflection = velocity.subtract(normal.multiply(2.0 * dotProduct));
            double decayFactor = Math.pow(0.95, bounceCount + 1);
            double bounciness = Math.abs(normal.getY()) > 0.5
                    ? Feature.PHYSIC_ENDER_PEARL.getDouble("vertical-bounciness")
                    : Feature.PHYSIC_ENDER_PEARL.getDouble("bounciness");
            reflection = reflection.multiply(bounciness * Feature.PHYSIC_ENDER_PEARL.getDouble("friction") * decayFactor);

            if (reflection.lengthSquared() < minVelocityThreshold * minVelocityThreshold) {
                allowShooter(oldPearl);
                return;
            }

            EnderPearl newPearl = oldPearl.getWorld().spawn(oldPearl.getLocation(), EnderPearl.class);
            newPearl.setShooter(oldPearl.getShooter());

            ProjectileLaunchEvent launchEvent = new ProjectileLaunchEvent(newPearl);
            Bukkit.getPluginManager().callEvent(launchEvent);

            if (!launchEvent.isCancelled()) {
                newPearl.setVelocity(reflection);
                newPearl.getPersistentDataContainer().set(bounces, PersistentDataType.INTEGER, bounceCount + 1);
                newPearl.getWorld().playSound(newPearl.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.0f);
            }
            oldPearl.remove();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in PhysicEnderPearlFeature.onProjectileHit", e);
        }
    }

    private int getBounceCount(PersistentDataContainer data) {
        Integer value = data.get(bounces, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    private void allowShooter(EnderPearl pearl) {
        if (pearl.getShooter() instanceof Player player) {
            allowed.add(player.getUniqueId());
        }
    }
}