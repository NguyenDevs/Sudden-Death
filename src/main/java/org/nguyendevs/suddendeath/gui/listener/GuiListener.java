package org.nguyendevs.suddendeath.gui.listener;

import org.nguyendevs.suddendeath.gui.PluginInventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {
	@EventHandler
	public void a(InventoryClickEvent event) {
		if (event.getInventory().getHolder() instanceof PluginInventory)
			((PluginInventory) event.getInventory().getHolder()).whenClicked(event);
	}
}
