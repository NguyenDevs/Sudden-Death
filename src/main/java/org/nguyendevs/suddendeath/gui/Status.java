package org.nguyendevs.suddendeath.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Status extends PluginInventory {
    private static final int[] STATUS_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final String GUI_TITLE = translateColors(Utils.msg("gui-status-name"));
    private final PlayerData data;

    private static String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    public Status(Player player) {
        super(player);
        this.data = PlayerData.get(player);
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(this, 27, GUI_TITLE);

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
                inventory.setItem(4, createStatusItem(Material.RED_STAINED_GLASS,
                        "gui-no-special-status-name", "gui-no-special-status-lore"));
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating inventory for player: " + player.getName(), e);
        }

        return inventory;
    }

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

    private int getAvailableSlot(Inventory inventory, int[] slots) {
        for (int slot : slots) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}