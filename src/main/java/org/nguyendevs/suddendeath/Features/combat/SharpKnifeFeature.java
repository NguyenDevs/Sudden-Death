package org.nguyendevs.suddendeath.Features.combat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Features.player.BleedingFeature;
import org.nguyendevs.suddendeath.Utils.CustomItem;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.logging.Level;

public class SharpKnifeFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Sharp Knife";
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (!Feature.INFECTION.isEnabled(player)) return;

        try {
            ItemStack item = damager.getInventory().getItemInMainHand();
            if (Utils.isPluginItem(item, false) && item.isSimilar(CustomItem.SHARP_KNIFE.a())) {
                org.nguyendevs.suddendeath.Player.PlayerData data =
                        org.nguyendevs.suddendeath.Player.PlayerData.get(player);
                if (data != null && !data.isBleeding()) {
                    double chance = Feature.BLEEDING.getDouble("chance-percent") / 100.0;
                    if (RANDOM.nextDouble() <= chance) {
                        data.setBleeding(true);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                Utils.msg("prefix") + " " + Utils.msg("now-bleeding")));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error in SharpKnifeFeature.onEntityDamageByEntity", e);
        }
    }
}