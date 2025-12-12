package org.nguyendevs.suddendeath.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.CustomItem;

public class RecipeBookListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        unlockRecipes(event.getPlayer());
    }

    public static void unlockRecipes(Player player) {
        SuddenDeath plugin = SuddenDeath.getInstance();
        FileConfiguration itemsConfig = plugin.items.getConfig();

        for (CustomItem item : CustomItem.values()) {
            String itemKey = item.name();

            if (itemsConfig.getBoolean(itemKey + ".craft-enabled", false) && item.getCraft() != null) {
                NamespacedKey key = new NamespacedKey(plugin, "suddendeath_" + item.name().toLowerCase());
                player.discoverRecipe(key);
            }
        }
    }
}