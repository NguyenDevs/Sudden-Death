package org.nguyendevs.suddendeath.Features.mob.attributes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class QuickMobsFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Quick Mobs";
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (event.getEntity().hasMetadata("NPC")) return;
        if (!Feature.QUICK_MOBS.isEnabled(monster)) return;

        try {
            AttributeInstance movementSpeed = monster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (movementSpeed != null) {
                double multiplier = 1 + Feature.QUICK_MOBS.getDouble(
                        "additional-ms-percent." + monster.getType().name()) / 100.0;
                movementSpeed.setBaseValue(movementSpeed.getBaseValue() * multiplier);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error in QuickMobsFeature.onEntitySpawn", e);
        }
    }
}