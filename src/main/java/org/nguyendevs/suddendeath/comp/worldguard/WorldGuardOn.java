package org.nguyendevs.suddendeath.comp.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * WorldGuard integration for the SuddenDeath plugin, managing custom flags and region queries.
 */
public class WorldGuardOn implements WGPlugin {
	private final WorldGuard worldGuard;
	private final WorldGuardPlugin worldGuardPlugin;
	private final Map<String, StateFlag> customFlags;
	private final Set<String> failedFlags; // Track flags that failed to register
	private volatile boolean flagsRegistered = false;

	/**
	 * Constructs a WorldGuardOn instance and registers custom flags.
	 *
	 * @throws IllegalStateException if WorldGuard plugin is not found.
	 */
	public WorldGuardOn() {
		this.customFlags = new HashMap<>();
		this.failedFlags = new HashSet<>();
		this.worldGuard = WorldGuard.getInstance();

		WorldGuardPlugin plugin = (WorldGuardPlugin) SuddenDeath.getInstance().getServer()
				.getPluginManager().getPlugin("WorldGuard");
		if (plugin == null) {
			throw new IllegalStateException("WorldGuard plugin not found. Disabling WorldGuard integration.");
		}
		this.worldGuardPlugin = plugin;

		// Delay flag registration to ensure WorldGuard is fully loaded
		SuddenDeath.getInstance().getServer().getScheduler().runTaskLater(
				SuddenDeath.getInstance(), this::registerCustomFlags, 1L);
	}

	/**
	 * Registers custom flags with WorldGuard's flag registry.
	 */
	/**
	 * Loads custom flags from WorldGuard's flag registry.
	 */
	private void registerCustomFlags() {
		FlagRegistry registry = worldGuard.getFlagRegistry();
		int successCount = 0;

		for (CustomFlag customFlag : CustomFlag.values()) {
			String flagPath = customFlag.getPath();
			try {
				// Check if flag exists
				if (registry.get(flagPath) != null) {
					StateFlag existingFlag = (StateFlag) registry.get(flagPath);
					customFlags.put(flagPath, existingFlag);
					//SuddenDeath.getInstance().getLogger().log(Level.INFO,
						//	"Found existing WorldGuard flag: " + flagPath);
					successCount++;
				} else {
					failedFlags.add(flagPath);
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Custom flag not found in registry: " + flagPath);
				}
			} catch (Exception e) {
				failedFlags.add(flagPath);
				SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
						"Error loading WorldGuard flag: " + flagPath, e);
			}
		}

		flagsRegistered = true;
		//SuddenDeath.getInstance().getLogger().log(Level.INFO,
			//	"WorldGuard flag loading completed: " + successCount + "/" + CustomFlag.values().length + " flags loaded");
	}

	/**
	 * Checks if PvP is allowed at the specified location.
	 *
	 * @param location The location to check.
	 * @return true if PvP is allowed, false otherwise.
	 * @throws IllegalArgumentException if the location is null.
	 */
	@Override
	public boolean isPvpAllowed(Location location) {
		if (location == null) {
			throw new IllegalArgumentException("Location cannot be null");
		}

		try {
			ApplicableRegionSet regions = getApplicableRegion(location);
			return regions.queryState(null, Flags.PVP) != StateFlag.State.DENY;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking PvP state at location: " + location, e);
			return true; // Default to allowing PvP if an error occurs
		}
	}

	/**
	 * Checks if a custom flag is allowed for a player at their current location.
	 *
	 * @param player     The player to check.
	 * @param customFlag The custom flag to query.
	 * @return true if the flag is allowed, false otherwise.
	 * @throws IllegalArgumentException if the player or customFlag is null.
	 */
	@Override
	public boolean isFlagAllowed(Player player, CustomFlag customFlag) {
		if (player == null) {
			throw new IllegalArgumentException("Player cannot be null");
		}
		if (customFlag == null) {
			throw new IllegalArgumentException("CustomFlag cannot be null");
		}

		// If flags aren't registered yet, return DENY for SDS_REMOVE, ALLOW for others
		if (!flagsRegistered) {
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}

		String flagPath = customFlag.getPath();

		// If flag failed to register, return DENY for SDS_REMOVE, ALLOW for others
		if (failedFlags.contains(flagPath)) {
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}

		try {
			ApplicableRegionSet regions = getApplicableRegion(player.getLocation());
			StateFlag flag = customFlags.get(flagPath);

			if (flag == null) {
				// Only log once per flag, then add to failed flags to prevent spam
				if (!failedFlags.contains(flagPath)) {
					failedFlags.add(flagPath);
					//SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							//"Custom flag not found after registration: " + flagPath + " - Using default behavior");
				}
				return customFlag == CustomFlag.SDS_REMOVE ? false : true;
			}

			StateFlag.State state = regions.queryValue(worldGuardPlugin.wrapPlayer(player), flag);
			// Respect flag's default state if null
			return state == null ? (customFlag == CustomFlag.SDS_REMOVE ? false : true) : state == StateFlag.State.ALLOW;

		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking flag " + flagPath + " for player: " + player.getName(), e);
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}
	}

	/**
	 * Retrieves the applicable WorldGuard regions for a given location.
	 *
	 * @param location The location to query.
	 * @return The ApplicableRegionSet for the location.
	 * @throws IllegalStateException if the region container or query cannot be created.
	 */
	private ApplicableRegionSet getApplicableRegion(Location location) {
		try {
			return worldGuard.getPlatform().getRegionContainer()
					.createQuery()
					.getApplicableRegions(BukkitAdapter.adapt(location));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to query WorldGuard regions at location: " + location, e);
		}
	}

	/**
	 * Checks if the WorldGuard integration is ready to use.
	 *
	 * @return true if flags are registered and ready to use.
	 */
	public boolean isReady() {
		return flagsRegistered;
	}

	/**
	 * Gets the registered custom flags.
	 *
	 * @return A copy of the custom flags map.
	 */
	public Map<String, StateFlag> getRegisteredFlags() {
		return new HashMap<>(customFlags);
	}
}