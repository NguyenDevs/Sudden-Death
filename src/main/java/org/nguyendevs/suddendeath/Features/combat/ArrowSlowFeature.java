package org.nguyendevs.suddendeath.Features.combat;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;

public class ArrowSlowFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Arrow Slow";
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Arrow)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (!Feature.ARROW_SLOW.isEnabled(player)) return;

        try {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    (int) (Feature.ARROW_SLOW.getDouble("slow-duration") * 20), 2));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in ArrowSlowFeature.onEntityDamageByEntity", e);
        }
    }
}
