package org.nguyendevs.suddendeath.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Abstract base class for custom inventory GUIs in the SuddenDeath plugin.
 */
public abstract class PluginInventory implements InventoryHolder {
    protected final Player player;

    /**
     * Constructs a PluginInventory for the specified player.
     *
     * @param player The player associated with this inventory.
     * @throws IllegalArgumentException if the player is null.
     */
    public PluginInventory(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        this.player = player;
    }

    /**
     * Gets the player associated with this inventory.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Creates and returns the inventory for this GUI.
     *
     * @return The non-null inventory.
     */
    @Override
    public abstract @NotNull Inventory getInventory();

    /**
     * Handles inventory click events for this GUI.
     *
     * @param event The InventoryClickEvent.
     */
    public abstract void whenClicked(InventoryClickEvent event);

    /**
     * Opens the inventory for the player, ensuring it runs on the main thread.
     */
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