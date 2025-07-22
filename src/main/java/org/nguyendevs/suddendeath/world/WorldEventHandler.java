package org.nguyendevs.suddendeath.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;

import java.util.Random;
import java.util.logging.Level;

/**
 * Abstract base class for handling world-specific events in the SuddenDeath plugin.
 * Extends BukkitRunnable for scheduled tasks and implements Listener for event handling.
 */
public abstract class WorldEventHandler extends BukkitRunnable implements Listener, StatusRetriever {
	private static final Random RANDOM = new Random();
	protected final World world;
	protected final WorldStatus status;

	/**
	 * Constructs a WorldEventHandler with the specified world, tick interval, and status.
	 *
	 * @param world  The world this handler operates in.
	 * @param tick   The interval (in ticks) for the scheduled task.
	 * @param status The WorldStatus associated with this event.
	 * @throws IllegalArgumentException if world or status is null.
	 */
	protected WorldEventHandler(World world, int tick, WorldStatus status) {
		if (world == null || status == null) {
			throw new IllegalArgumentException("World and status cannot be null");
		}
		this.world = world;
		this.status = status;

		try {
			runTaskTimer(SuddenDeath.getInstance(), 20L, tick);
			Bukkit.getPluginManager().registerEvents(this, SuddenDeath.getInstance());
		} catch (IllegalStateException e) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Failed to initialize WorldEventHandler for world: " + world.getName(), e);
		}
	}

	/**
	 * Gets the world associated with this event handler.
	 *
	 * @return The world instance.
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * Gets the status of this event handler.
	 *
	 * @return The WorldStatus.
	 */
	@Override
	public WorldStatus getStatus() {
		return status;
	}

	/**
	 * Closes the event handler by canceling the task and unregistering listeners.
	 */
	public void close() {
		try {
			cancel();
		} catch (IllegalStateException e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Attempted to cancel an already cancelled task for world: " + world.getName(), e);
		}
		HandlerList.unregisterAll(this);
	}

	/**
	 * Provides access to a shared Random instance for use in subclasses.
	 *
	 * @return A Random instance.
	 */
	protected static Random getRandom() {
		return RANDOM;
	}
}