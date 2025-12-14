package org.nguyendevs.suddendeath.Features.items;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.logging.Level;

public class RealisticPickupFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Realistic Pickup";
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Feature.REALISTIC_PICKUP.isEnabled(player) ||
                Utils.hasCreativeGameMode(player)) {
            return;
        }

        try {
            Item item = event.getItem();
            if ((player.getEyeLocation().getPitch() > 70 || item.getLocation().getY() >= player.getLocation().getY() + 1) &&
                    player.isSneaking()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 10, 1));
            } else {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in RealisticPickupFeature", e);
        }
    }
}