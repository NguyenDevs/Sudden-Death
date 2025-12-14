package org.nguyendevs.suddendeath.GUI;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

public abstract class PluginInventory implements InventoryHolder {
    protected final Player player;

    public PluginInventory(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public abstract @NotNull Inventory getInventory();

    public abstract void whenClicked(InventoryClickEvent event);

    public void open() {
        try {
            if (Bukkit.isPrimaryThread()) {
                player.openInventory(getInventory());
            } else {
                Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
                    try {
                        player.openInventory(getInventory());
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error opening inventory for player: " + player.getName(), e);
                    }
                });
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error scheduling inventory open for player: " + player.getName(), e);
        }
    }
}