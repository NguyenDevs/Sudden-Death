package org.nguyendevs.suddendeath.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class AdminView extends PluginInventory {
    private static final String PREFIX = "&6[&cSudden&4Death&6]";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.###");
    private static final int[] AVAILABLE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int ITEMS_PER_PAGE = 21;
    private static final int INVENTORY_SIZE = 45;
    private int page;

    public AdminView(Player player) {
        super(player);
    }

    private static String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public @NotNull Inventory getInventory() {
        int maxPage = (Feature.values().length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        Inventory inventory = Bukkit.createInventory(this, INVENTORY_SIZE, translateColors(Utils.msg("gui-admin-name")) + " (" + (page + 1) + "/" + maxPage + ")");

        try {
            Feature[] features = Feature.values();
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(features.length, (page + 1) * ITEMS_PER_PAGE);

            for (int i = startIndex; i < endIndex; i++) {
                inventory.setItem(getAvailableSlot(inventory), createFeatureItem(features[i]));
            }
            if (page > 0) {
                inventory.setItem(18, createNavigationItem(Material.ARROW, translateColors(Utils.msg("gui-previous"))));
            }
            if (endIndex < features.length) {
                inventory.setItem(26, createNavigationItem(Material.ARROW, translateColors(Utils.msg("gui-next"))));
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating AdminView inventory for player: " + player.getName(), e);
        }
        return inventory;
    }

    private ItemStack createFeatureItem(Feature feature) {
        List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig().getStringList(feature.getPath());
        boolean isEnabledInWorld = enabledWorlds.contains(player.getWorld().getName());
        Material material = isEnabledInWorld ? (feature.isEvent() ? Material.LIGHT_BLUE_DYE : Material.LIME_DYE) : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "ItemMeta is null for feature: " + feature.getName());
            return item;
        }
        meta.setDisplayName(ChatColor.GOLD + feature.getName());
        meta.getPersistentDataContainer().set(Utils.nsk("featureId"), PersistentDataType.STRING, feature.name());
        meta.setLore(createFeatureLore(feature, enabledWorlds, isEnabledInWorld));
        item.setItemMeta(meta);
        return item;
    }

    private List<String> createFeatureLore(Feature feature, List<String> enabledWorlds, boolean isEnabledInWorld) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : feature.getLore()) {
            lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', statsInLore(feature, line)));
        }

        if (!enabledWorlds.isEmpty()) {
            lore.add("");
            lore.add(translateColors(Utils.msg("gui-features")));
            for (String world : enabledWorlds) {
                lore.add(ChatColor.WHITE + "► " + ChatColor.DARK_GREEN + world);
            }
        }
        lore.add("");
        lore.add(isEnabledInWorld ? translateColors(Utils.msg("gui-features-enabled"))
                : translateColors(Utils.msg("gui-features-disabled")));
        lore.add(ChatColor.YELLOW + "Click to " + (isEnabledInWorld ? "disable." : "enable."));
        return lore;
    }

    private ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "ItemMeta is null for navigation item: " + name);
            return item;
        }
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !Utils.isPluginItem(item, false)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "ItemMeta is null for clicked item in AdminView for player: " + player.getName());
            return;
        }
        try {
            String displayName = meta.getDisplayName();
            if (translateColors(Utils.msg("gui-next")).equals(displayName)) {
                page++;
                open();
                return;
            }
            if (translateColors(Utils.msg("gui-previous")).equals(displayName)) {
                page--;
                open();
                return;
            }
            String featureId = meta.getPersistentDataContainer().get(Utils.nsk("featureId"), PersistentDataType.STRING);
            if (featureId == null || featureId.isEmpty()) {
                return;
            }
            Feature feature = Feature.valueOf(featureId);
            List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig().getStringList(feature.getPath());
            String worldName = player.getWorld().getName();

            if (enabledWorlds.contains(worldName)) {
                enabledWorlds.remove(worldName);
                player.sendMessage(translateColors(PREFIX + " " + "&eYou disabled &6" + feature.getName() + " &ein &6" + worldName + "&e."));
            } else {
                enabledWorlds.add(worldName);
                player.sendMessage(translateColors(PREFIX + " " + "&eYou enabled &6" + feature.getName() + " &ein &6" + worldName + "&e."));
            }

            SuddenDeath.getInstance().getConfiguration().getConfig().set(feature.getPath(), enabledWorlds);
            SuddenDeath.getInstance().getConfiguration().save();
            SuddenDeath.getInstance().getConfiguration().reload();
            SuddenDeath.getInstance().getLogger().info("Updated config for " + feature.getName() + ": " + enabledWorlds);
            open();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling InventoryClickEvent for player: " + player.getName(), e);
            player.sendMessage(translateColors(PREFIX + " " + "An error occurred while processing your action."));
        }
    }
    public static String statsInLore(Feature feature, String lore) {
        if (lore.contains("#")) {
            String[] parts = lore.split("#", 3);
            if (parts.length >= 2) {
                String stat = parts[1];
                return statsInLore(feature, parts[0] + ChatColor.GREEN + DECIMAL_FORMAT.format(feature.getDouble(stat)) + ChatColor.GRAY + parts[2]);
            }
        }
        return lore;
    }
    private int getAvailableSlot(Inventory inventory) {
        for (int slot : AVAILABLE_SLOTS) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return -1;
    }
}