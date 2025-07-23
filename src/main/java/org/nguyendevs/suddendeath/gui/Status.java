package org.nguyendevs.suddendeath.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.Difficulty;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * A GUI inventory for displaying player status and difficulty selection in the SuddenDeath plugin.
 */
public class Status extends PluginInventory {
    private static final int[] STATUS_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int[] DIFFICULTY_SLOTS = {29, 30, 31, 32, 33};
    private static final String GUI_TITLE = ChatColor.UNDERLINE + Utils.msg("gui-name");
    private final PlayerData data;

    /**
     * Constructs a Status GUI for the specified player.
     *
     * @param player The player for whom the GUI is created.
     */
    public Status(Player player) {
        super(player);
        this.data = PlayerData.get(player);
    }

    /**
     * Creates and populates the inventory for the player's status GUI.
     *
     * @return The populated inventory.
     */
    @Override
    public @NotNull Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(this, 45, GUI_TITLE);

        try {
            // Add bleeding status item
            if (Feature.BLEEDING.isEnabled(player) && data.isBleeding()) {
                inventory.setItem(getAvailableSlot(inventory, STATUS_SLOTS),
                        createStatusItem(Material.RED_DYE, "gui-bleeding-name", "gui-bleeding-lore"));
            }

            // Add infection status item
            if (Feature.INFECTION.isEnabled(player) && data.isInfected()) {
                inventory.setItem(getAvailableSlot(inventory, STATUS_SLOTS),
                        createStatusItem(Material.ROTTEN_FLESH, "gui-infected-name", "gui-infected-lore"));
            }

            // Add no-status item if no status effects are present
            if (inventory.getItem(10) == null) {
                inventory.setItem(13, createStatusItem(Material.RED_STAINED_GLASS,
                        "gui-no-special-status-name", "gui-no-special-status-lore"));
            }

            // Add difficulty selection items
            if (!SuddenDeath.getInstance().getConfig().getBoolean("disable-difficulties", false)) {
                for (Difficulty difficulty : Difficulty.values()) {
                    inventory.setItem(getAvailableSlot(inventory, DIFFICULTY_SLOTS),
                            createDifficultyItem(difficulty));
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating inventory for player: " + player.getName(), e);
        }

        return inventory;
    }

    /**
     * Creates a status item with the specified material and message keys.
     *
     * @param material The material of the item.
     * @param nameKey  The message key for the item name.
     * @param loreKey  The message key for the item lore.
     * @return The created ItemStack.
     */
    private ItemStack createStatusItem(Material material, String nameKey, String loreKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "ItemMeta is null for material: " + material);
            return item;
        }

        meta.setDisplayName(ChatColor.GREEN + Utils.msg(nameKey));
        List<String> lore = new ArrayList<>();
        for (String line : Utils.msgList(loreKey)) {
            lore.add(ChatColor.GRAY + line);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates an item representing a difficulty level.
     *
     * @param difficulty The difficulty level.
     * @return The created ItemStack.
     */
    private ItemStack createDifficultyItem(Difficulty difficulty) {
        boolean isCurrent = data.hasDifficulty(difficulty);
        ItemStack item = new ItemStack(isCurrent ? difficulty.getMaterial() : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "ItemMeta is null for difficulty: " + difficulty.name());
            return item;
        }

        String difficultyBar = "||||||||||||||||||||";
        int index = difficulty.getDifficultyIndex();
        difficultyBar = ChatColor.GREEN + difficultyBar.substring(0, index) + ChatColor.DARK_GRAY + difficultyBar.substring(index);

        meta.setDisplayName(ChatColor.GREEN + (isCurrent ? "[" + Utils.msg("current") + "] " : "") +
                ChatColor.translateAlternateColorCodes('&', difficulty.getName()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        for (String line : Utils.msgList("gui-difficulty-lore")) {
            lore.add(ChatColor.GRAY + line
                    .replace("#health-malus#", String.valueOf(difficulty.getHealthMalus()))
                    .replace("#increased-damage#", String.valueOf(difficulty.getIncreasedDamage()))
                    .replace("#difficulty#", difficultyBar));
        }
        lore.add("");
        for (String line : difficulty.getLore()) {
            lore.add(ChatColor.BLUE + ChatColor.translateAlternateColorCodes('&', line));
        }
        if (!lore.isEmpty() && lore.get(lore.size() - 1).isEmpty()) {
            lore.remove(lore.size() - 1);
        }
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
        if (!isCurrent) {
            lore.add("");
            lore.add(ChatColor.YELLOW + Utils.msg("gui-click-select-diff"));
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(Utils.nsk("difficultyId"), PersistentDataType.STRING, difficulty.name());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Finds the first available slot in the inventory from the provided slot list.
     *
     * @param inventory The inventory to check.
     * @param slots     The array of slots to search.
     * @return The index of the first available slot, or -1 if none are available.
     */
    private int getAvailableSlot(Inventory inventory, int[] slots) {
        for (int slot : slots) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Handles inventory click events to allow difficulty selection.
     *
     * @param event The InventoryClickEvent.
     */
    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);

        try {
            ItemStack item = event.getCurrentItem();
            if (item == null || !Utils.isPluginItem(item, true)) {
                return;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "ItemMeta is null for clicked item in Status GUI for player: " + player.getName());
                return;
            }

            String difficultyId = meta.getPersistentDataContainer().get(Utils.nsk("difficultyId"), PersistentDataType.STRING);
            if (difficultyId == null || difficultyId.isEmpty()) {
                return;
            }

            Difficulty difficulty;
            try {
                difficulty = Difficulty.valueOf(difficultyId);
            } catch (IllegalArgumentException e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Invalid difficulty ID: " + difficultyId + " for player: " + player.getName(), e);
                return;
            }

            if (!player.hasPermission(difficulty.getPermission())) {
                player.sendMessage(ChatColor.RED + Utils.msg("no-permission"));
                return;
            }

            data.setDifficulty(difficulty);
            difficulty.applyHealthMalus(data);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
            player.sendMessage(ChatColor.YELLOW + Utils.msg("chose-diff").replace("#difficulty#", difficulty.getName()));
            open();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling InventoryClickEvent for player: " + player.getName(), e);
        }
    }
}