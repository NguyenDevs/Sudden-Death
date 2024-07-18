package org.nguyendevs.suddendeath.gui;

import org.nguyendevs.suddendeath.SuddenDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class PluginInventory implements InventoryHolder {
    protected final Player player;

    public PluginInventory(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public abstract Inventory getInventory();

    public abstract void whenClicked(InventoryClickEvent event);

    public void open() {
        if (Bukkit.isPrimaryThread())
            player.openInventory(getInventory());
        else
            Bukkit.getScheduler().runTask(SuddenDeath.plugin, () -> player.openInventory(getInventory()));
    }
}
