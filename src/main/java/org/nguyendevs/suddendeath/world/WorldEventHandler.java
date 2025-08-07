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

public abstract class WorldEventHandler extends BukkitRunnable implements Listener, StatusRetriever {
	private static final Random RANDOM = new Random();
	protected final World world;
	protected final WorldStatus status;
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

	public World getWorld() {
		return world;
	}

	@Override
	public WorldStatus getStatus() {
		return status;
	}

	public void close() {
		try {
			cancel();
		} catch (IllegalStateException e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Attempted to cancel an already cancelled task for world: " + world.getName(), e);
		}
		HandlerList.unregisterAll(this);
	}

	protected static Random getRandom() {
		return RANDOM;
	}
}