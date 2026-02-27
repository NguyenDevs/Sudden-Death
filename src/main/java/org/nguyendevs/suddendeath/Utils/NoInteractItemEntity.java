package org.nguyendevs.suddendeath.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class NoInteractItemEntity implements Listener {
	private static final int MAX_PICKUP_DELAY = 1_000_000;
	private final Item item;

	public NoInteractItemEntity(Location location, ItemStack itemStack) {
		if (location == null || itemStack == null) {
			throw new IllegalArgumentException("Location and ItemStack cannot be null");
		}
		if (location.getWorld() == null) {
			throw new IllegalArgumentException("Invalid world in location");
		}

		try {
			itemStack.setAmount(1);
			this.item = location.getWorld().dropItem(location, itemStack);
			this.item.setPickupDelay(MAX_PICKUP_DELAY);
			Bukkit.getPluginManager().registerEvents(this, SuddenDeath.getInstance());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Failed to create NoInteractItemEntity at location: " + location, e);
			throw new IllegalStateException("Could not spawn item entity", e);
		}
	}

	public Item getEntity() {
		return item;
	}

	public void close() {
		try {
			if (item != null && !item.isDead()) {
				item.remove();
			}
			InventoryPickupItemEvent.getHandlerList().unregister(this);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error closing NoInteractItemEntity", e);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryPickupItem(InventoryPickupItemEvent event) {
		if (event.getItem().equals(item)) {
			event.setCancelled(true);
		}
	}
}