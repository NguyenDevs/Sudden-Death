package org.nguyendevs.suddendeath.features.mob.attributes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import java.util.logging.Level;

public class TankyMonstersFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Tanky Monsters";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (event.getEntity() instanceof Player) return;
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;
        if (!Feature.TANKY_MONSTERS.isEnabled(entity)) return;

        try {
            double reduction = Feature.TANKY_MONSTERS.getDouble(
                    "dmg-reduction-percent." + entity.getType().name()) / 100.0;
            event.setDamage(event.getDamage() * (1 - reduction));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error in TankyMonstersFeature.onEntityDamage", e);
        }
    }
}