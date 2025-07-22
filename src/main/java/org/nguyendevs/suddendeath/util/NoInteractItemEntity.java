package org.nguyendevs.suddendeath.util;

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

/**
 * Manages an item entity that cannot be picked up by players or inventories.
 */
public class NoInteractItemEntity implements Listener {
	private static final int MAX_PICKUP_DELAY = 1_000_000;
	private final Item item;

	/**
	 * Creates a non-interactable item entity at the specified location.
	 *
	 * @param location The location to spawn the item.
	 * @param itemStack The ItemStack to spawn.
	 * @throws IllegalArgumentException if location or itemStack is null, or if the world is invalid.
	 */
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

	/**
	 * Gets the item entity.
	 *
	 * @return The Item entity.
	 */
	public Item getEntity() {
		return item;
	}

	/**
	 * Removes the item entity and unregisters the event listener.
	 */
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

	/**
	 * Prevents the item from being picked up by inventories.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryPickupItem(InventoryPickupItemEvent event) {
		if (event.getItem().equals(item)) {
			event.setCancelled(true);
		}
	}
}