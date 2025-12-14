package org.nguyendevs.suddendeath.Features.combat;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;

public class MobCriticalStrikesFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Mob Critical Strikes";
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity entity)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (!Feature.MOB_CRITICAL_STRIKES.isEnabled(player)) return;

        try {
            double chance = Feature.MOB_CRITICAL_STRIKES.getDouble(
                    "crit-chance." + entity.getType().name()) / 100.0;
            if (RANDOM.nextDouble() <= chance) {
                player.getWorld().playSound(player.getLocation(),
                        Sound.ENTITY_PLAYER_ATTACK_CRIT, 2.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.CRIT,
                        player.getLocation().add(0, 1, 0), 32, 0, 0, 0, 0.5);
                player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,
                        player.getLocation().add(0, 1, 0), 0);
                double multiplier = Feature.MOB_CRITICAL_STRIKES.getDouble("damage-percent") / 100.0;
                event.setDamage(event.getDamage() * (1 + multiplier));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error in MobCriticalStrikesFeature.onEntityDamageByEntity", e);
        }
    }
}
