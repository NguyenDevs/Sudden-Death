package org.nguyendevs.suddendeath.gui.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.nguyendevs.suddendeath.gui.PluginInventory;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Listener for handling clicks in custom plugin inventories.
 */
public class GuiListener implements Listener {

	/**
	 * Handles inventory click events for PluginInventory instances.
	 *
	 * @param event The InventoryClickEvent.
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		try {
			if (event.getInventory().getHolder() instanceof PluginInventory pluginInventory) {
				pluginInventory.whenClicked(event);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling InventoryClickEvent for inventory holder: " + event.getInventory().getHolder(), e);
		}
	}
}