package org.nguyendevs.suddendeath.Features.nether;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class NetherShieldFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Nether Shield";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!isNetherEntity(entity)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!Feature.NETHER_SHIELD.isEnabled(entity)) return;

        try {
            double chance = Feature.NETHER_SHIELD.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            event.setCancelled(true);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 0.5f);
            int radius = entity instanceof MagmaCube magmaCube && magmaCube.getSize() == 4 ? 2 : 1;

            for (double j = 0; j < Math.PI * 2; j += 0.3) {
                double x = Math.cos(j) * radius;
                double z = Math.sin(j) * radius;
                for (double y = 0; y < 2; y += 0.2) {
                    if (RANDOM.nextDouble() < 0.3) continue;
                    Location loc = entity.getLocation().clone().add(x, y, z);
                    if (loc.getBlock().getType().isSolid()) continue;
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 0);
                    if (RANDOM.nextDouble() < 0.45) loc.getWorld().spawnParticle(Particle.SMOKE, loc, 0);
                }
            }

            player.setVelocity(player.getEyeLocation().getDirection().multiply(-0.6).setY(0.3));
            player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 0);
            player.setFireTicks((int) Feature.NETHER_SHIELD.getDouble("burn-duration"));
            Utils.damage(player, event.getDamage() * Feature.NETHER_SHIELD.getDouble("dmg-reflection-percent") / 100.0, true);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Nether Shield", e);
        }
    }

    private boolean isNetherEntity(Entity entity) {
        return entity instanceof PigZombie || entity instanceof MagmaCube || entity instanceof Blaze
                || entity instanceof Piglin || entity instanceof Strider || entity instanceof Hoglin
                || entity instanceof Zoglin || entity instanceof PiglinBrute;
    }
}