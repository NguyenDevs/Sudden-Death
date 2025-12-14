package org.nguyendevs.suddendeath.Features.items;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.CustomItem;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.logging.Level;

public class AdvancedPlayerDropsFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Advanced Player Drops";
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasMetadata("NPC")) return;
        if (!Feature.ADVANCED_PLAYER_DROPS.isEnabled(player)) return;

        try {
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                data.setBleeding(false);
                data.setInfected(false);
            }

            FileConfiguration config = Feature.ADVANCED_PLAYER_DROPS.getConfigFile().getConfig();

            if (config.getBoolean("drop-skull", false)) {
                ItemStack skull = new ItemStack(config.getBoolean("player-skull", false) ?
                        Material.PLAYER_HEAD : Material.SKELETON_SKULL);
                if (skull.getType() == Material.PLAYER_HEAD) {
                    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    if (skullMeta != null) {
                        skullMeta.setOwningPlayer(player);
                        skull.setItemMeta(skullMeta);
                    }
                }
                player.getWorld().dropItemNaturally(player.getLocation(), skull);
            }

            int boneAmount = config.getInt("dropped-bones", 0);
            if (boneAmount > 0) {
                ItemStack bone = CustomItem.HUMAN_BONE.a().clone();
                bone.setAmount(boneAmount);
                player.getWorld().dropItemNaturally(player.getLocation(), bone);
            }

            int fleshAmount = config.getInt("dropped-flesh", 0);
            if (fleshAmount > 0) {
                ItemStack flesh = CustomItem.RAW_HUMAN_FLESH.a().clone();
                flesh.setAmount(fleshAmount);
                player.getWorld().dropItemNaturally(player.getLocation(), flesh);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in AdvancedPlayerDropsFeature.onPlayerDeath", e);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        try {
            ItemStack item = event.getSource();
            if (Utils.isPluginItem(item, false) && item.isSimilar(CustomItem.RAW_HUMAN_FLESH.a())) {
                event.setResult(CustomItem.COOKED_HUMAN_FLESH.a());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling FurnaceSmeltEvent", e);
        }
    }
}