package org.nguyendevs.suddendeath.GUI.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.nguyendevs.suddendeath.GUI.PluginInventory;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class GuiListener implements Listener {

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