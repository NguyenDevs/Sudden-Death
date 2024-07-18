package org.nguyendevs.suddendeath.util;

import org.nguyendevs.suddendeath.SuddenDeath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class NoInteractItemEntity implements Listener {
	private final Item item;

	public NoInteractItemEntity(Location loc, ItemStack item) {
		item.setAmount(1);
		
		this.item = loc.getWorld().dropItem(loc, item);
		this.item.setPickupDelay(1000000);

		Bukkit.getPluginManager().registerEvents(this, SuddenDeath.plugin);
	}

	public Item getEntity() {
		return item;
	}
	
	public void close() {
		item.remove();
		InventoryPickupItemEvent.getHandlerList().unregister(this);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void a(InventoryPickupItemEvent event) {
		if (event.getItem().equals(item))
			event.setCancelled(true);
	}
}
