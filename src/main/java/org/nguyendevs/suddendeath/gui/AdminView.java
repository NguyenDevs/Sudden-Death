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

/**
 * A GUI inventory for managing SuddenDeath plugin features in the admin interface.
 */
public class AdminView extends PluginInventory {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.###");
    private static final int[] AVAILABLE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int ITEMS_PER_PAGE = 21;
    private static final int INVENTORY_SIZE = 45;
    private static final String TITLE_PREFIX = ChatColor.UNDERLINE + "SD Admin GUI (";
    private static final String NEXT_BUTTON_NAME = ChatColor.GREEN + "Next";
    private static final String PREVIOUS_BUTTON_NAME = ChatColor.GREEN + "Previous";

    private int page;

    /**
     * Constructs an AdminView GUI for the specified player.
     *
     * @param player The player interacting with the GUI.
     */
    public AdminView(Player player) {
        super(player);
    }

    /**
     * Creates and populates the inventory for the admin GUI.
     *
     * @return The populated inventory.
     */
    @Override
    public @NotNull Inventory getInventory() {
        int maxPage = (Feature.values().length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        Inventory inventory = Bukkit.createInventory(this, INVENTORY_SIZE, TITLE_PREFIX + (page + 1) + "/" + maxPage + ")");

        try {
            // Add feature items
            Feature[] features = Feature.values();
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(features.length, (page + 1) * ITEMS_PER_PAGE);

            for (int i = startIndex; i < endIndex; i++) {
                inventory.setItem(getAvailableSlot(inventory), createFeatureItem(features[i]));
            }

            // Add navigation buttons
            if (page > 0) {
                inventory.setItem(18, createNavigationItem(Material.ARROW, PREVIOUS_BUTTON_NAME));
            }
            if (endIndex < features.length) {
                inventory.setItem(26, createNavigationItem(Material.ARROW, NEXT_BUTTON_NAME));
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating AdminView inventory for player: " + player.getName(), e);
        }

        return inventory;
    }

    /**
     * Creates an item representing a feature.
     *
     * @param feature The feature to represent.
     * @return The created ItemStack.
     */
    private ItemStack createFeatureItem(Feature feature) {
        List<String> enabledWorlds = SuddenDeath.getInstance().getConfig().getStringList(feature.getPath());
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

    /**
     * Creates the lore for a feature item.
     *
     * @param feature         The feature.
     * @param enabledWorlds   The list of worlds where the feature is enabled.
     * @param isEnabledInWorld Whether the feature is enabled in the player's current world.
     * @return The lore list.
     */
    private List<String> createFeatureLore(Feature feature, List<String> enabledWorlds, boolean isEnabledInWorld) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        for (String line : feature.getLore()) {
            lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', statsInLore(feature, line)));
        }

        if (!enabledWorlds.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.GRAY + "This feature is enabled in:");
            for (String world : enabledWorlds) {
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + world);
            }
        }

        lore.add("");
        lore.add(isEnabledInWorld ? ChatColor.GREEN + "This feature is enabled in this world."
                : ChatColor.RED + "This feature is disabled in this world.");
        lore.add(ChatColor.YELLOW + "Click to " + (isEnabledInWorld ? "disable." : "enable."));

        return lore;
    }

    /**
     * Creates a navigation item (Next/Previous button).
     *
     * @param material The material for the item.
     * @param name     The display name of the item.
     * @return The created ItemStack.
     */
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

    /**
     * Handles inventory click events for the admin GUI.
     *
     * @param event The InventoryClickEvent.
     */
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
            if (NEXT_BUTTON_NAME.equals(displayName)) {
                page++;
                open();
                return;
            }
            if (PREVIOUS_BUTTON_NAME.equals(displayName)) {
                page--;
                open();
                return;
            }

            String featureId = meta.getPersistentDataContainer().get(Utils.nsk("featureId"), PersistentDataType.STRING);
            if (featureId == null || featureId.isEmpty()) {
                return;
            }

            Feature feature = Feature.valueOf(featureId);
            List<String> enabledWorlds = SuddenDeath.getInstance().getConfig().getStringList(feature.getPath());
            String worldName = player.getWorld().getName();

            if (enabledWorlds.contains(worldName)) {
                enabledWorlds.remove(worldName);
                player.sendMessage(ChatColor.YELLOW + "You disabled " + ChatColor.GOLD + feature.getName() +
                        ChatColor.YELLOW + " in " + ChatColor.GOLD + worldName + ChatColor.YELLOW + ".");
            } else {
                enabledWorlds.add(worldName);
                player.sendMessage(ChatColor.YELLOW + "You enabled " + ChatColor.GOLD + feature.getName() +
                        ChatColor.YELLOW + " in " + ChatColor.GOLD + worldName + ChatColor.YELLOW + ".");
            }

            SuddenDeath.getInstance().getConfig().set(feature.getPath(),enabledWorlds);
            SuddenDeath.getInstance().saveConfig();
            SuddenDeath.getInstance().getLogger().info("Updated config for " + feature.getName()+": " + enabledWorlds);
            SuddenDeath.getInstance().reloadConfig();
            open();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling InventoryClickEvent for player: " + player.getName(), e);
            player.sendMessage(ChatColor.RED + "An error occurred while processing your action.");
        }
    }

    /**
     * Replaces placeholders in lore with formatted stat values.
     *
     * @param feature The feature containing the stats.
     * @param lore    The lore string with placeholders.
     * @return The lore string with placeholders replaced.
     */
    public static String statsInLore(Feature feature, String lore) {
        if (lore.contains("#")) {
            String[] parts = lore.split("#", 3);
            if (parts.length >= 2) {
                String stat = parts[1];
                return statsInLore(feature, parts[0] + ChatColor.WHITE + DECIMAL_FORMAT.format(feature.getDouble(stat)) + ChatColor.GRAY + parts[2]);
            }
        }
        return lore;
    }

    /**
     * Finds the first available slot in the inventory from the predefined slots.
     *
     * @param inventory The inventory to check.
     * @return The index of the first available slot, or -1 if none are available.
     */
    private int getAvailableSlot(Inventory inventory) {
        for (int slot : AVAILABLE_SLOTS) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return -1;
    }
}