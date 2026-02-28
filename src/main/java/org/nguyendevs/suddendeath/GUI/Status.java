package org.nguyendevs.suddendeath.GUI;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Status extends PluginInventory {
    private static final int[] STATUS_SLOTS = { 10, 11, 12, 13, 14, 15, 16 };
    private final PlayerData data;

    public Status(Player player) {
        super(player);
        this.data = PlayerData.get(player);
    }

    @Override
    public @NotNull Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory(this, 27, Utils.color(Utils.msg("gui-status-name")));

        try {
            if (Feature.BLEEDING.isEnabled(player) && data.isBleeding()) {
                inventory.setItem(getAvailableSlot(inventory),
                        createStatusItem(Material.RED_DYE, "gui-bleeding-name", "gui-bleeding-lore"));
            }

            if (Feature.INFECTION.isEnabled(player) && data.isInfected()) {
                inventory.setItem(getAvailableSlot(inventory),
                        createStatusItem(Material.ROTTEN_FLESH, "gui-infected-name", "gui-infected-lore"));
            }

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

        meta.displayName(Utils.color("&a" + Utils.msg(nameKey)));
        List<Component> lore = new ArrayList<>();
        for (String line : Utils.msgList(loreKey)) {
            lore.add(Utils.color("&7" + line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int getAvailableSlot(Inventory inventory) {
        for (int slot : Status.STATUS_SLOTS) {
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