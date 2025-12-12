package org.nguyendevs.suddendeath.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.CustomItem;

public class RecipeDiscoveryListener implements Listener {

    private final SuddenDeath plugin;

    public RecipeDiscoveryListener(SuddenDeath plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getRecipeManager().discoverAllRecipesForPlayer(player);
            }
        }, 100L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        CustomItem customItem = CustomItem.fromItemStack(event.getRecipe().getResult());
        if (customItem != null) {
            plugin.getRecipeManager().discoverRecipeForPlayer(player, customItem);
        }
    }

    public void discoverRecipeIfHasMaterials(Player player, CustomItem item) {
        if (!plugin.getRecipeManager().isRecipeEnabled(item)) {
            return;
        }

        boolean hasMaterials = checkPlayerHasMaterials(player, item);

        if (hasMaterials) {
            plugin.getRecipeManager().discoverRecipeForPlayer(player, item);
        }
    }

    private boolean checkPlayerHasMaterials(Player player, CustomItem item) {
        if (item.getCraft() == null) {
            return false;
        }
        return true;
    }
}