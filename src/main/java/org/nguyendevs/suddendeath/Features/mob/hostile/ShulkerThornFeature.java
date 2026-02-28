package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class ShulkerThornFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Shulker Thorn";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Shulker shulker))
            return;
        if (!(event.getDamager() instanceof Player player))
            return;
        if (!Feature.SHULKER_THORN.isEnabled(shulker))
            return;

        try {
            if (shulker.getPeek() > 0.0f)
                return;

            shulker.getWorld().playSound(shulker.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1.0f, 1.0f);
            shulker.getWorld().playSound(shulker.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0f, 1.0f);

            shulker.getWorld().spawnParticle(Particle.WAX_OFF, shulker.getLocation().add(0, 0.5, 0), 10, 0.5,
                    0.5, 0.5, 0.1);

            double reflectionPercent = Feature.SHULKER_THORN.getDouble("reflection-percent");
            double reflectionDamage = event.getDamage() * (reflectionPercent / 100.0);

            Utils.damage(player, reflectionDamage, true);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Shulker Thorn", e);
        }
    }
}
