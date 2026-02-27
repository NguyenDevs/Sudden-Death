package org.nguyendevs.suddendeath.Features.mob.attributes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;


public class ForceOfUndeadFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Force Of The Undead";
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (event.getEntity().hasMetadata("NPC")) return;
        if (!Feature.FORCE_OF_THE_UNDEAD.isEnabled(monster)) return;

        try {
            AttributeInstance attackDamage = monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackDamage != null) {
                double multiplier = 1 + Feature.FORCE_OF_THE_UNDEAD.getDouble(
                        "additional-ad-percent." + monster.getType().name()) / 100.0;
                attackDamage.setBaseValue(attackDamage.getBaseValue() * multiplier);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error in ForceOfUndeadFeature.onEntitySpawn", e);
        }
    }
}