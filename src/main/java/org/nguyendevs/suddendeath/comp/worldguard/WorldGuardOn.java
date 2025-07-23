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
import java.util.logging.Level;

/**
 * WorldGuard integration for the SuddenDeath plugin, managing custom flags and region queries.
 */
public class WorldGuardOn implements WGPlugin {
	private final WorldGuard worldGuard;
	private final WorldGuardPlugin worldGuardPlugin;
	private final Map<String, StateFlag> customFlags;

	/**
	 * Constructs a WorldGuardOn instance and registers custom flags.
	 *
	 * @throws IllegalStateException if WorldGuard plugin is not found or flag registration fails.
	 */
	public WorldGuardOn() {
		this.customFlags = new HashMap<>();
		this.worldGuard = WorldGuard.getInstance();

		WorldGuardPlugin plugin = (WorldGuardPlugin) SuddenDeath.getInstance().getServer().getPluginManager().getPlugin("WorldGuard");
		if (plugin == null) {
			throw new IllegalStateException("WorldGuard plugin not found. Disabling WorldGuard integration.");
		}
		this.worldGuardPlugin = plugin;

		registerCustomFlags();
	}

	/**
	 * Registers custom flags with WorldGuard's flag registry.
	 */
	private void registerCustomFlags() {
		FlagRegistry registry = worldGuard.getFlagRegistry();
		for (CustomFlag customFlag : CustomFlag.values()) {
			try {
				StateFlag flag = new StateFlag(customFlag.getPath(), true);
				registry.register(flag);
				customFlags.put(customFlag.getPath(), flag);
				SuddenDeath.getInstance().getLogger().log(Level.INFO,
						"Registered custom WorldGuard flag: " + customFlag.getPath());
			} catch (FlagConflictException e) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Failed to register custom WorldGuard flag: " + customFlag.getPath(), e);
			} catch (Exception e) {
				SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
						"Unexpected error while registering custom WorldGuard flag: " + customFlag.getPath(), e);
			}
		}
	}

	/**
	 * Checks if PvP is allowed at the specified location.
	 *
	 * @param location The location to check.
	 * @return true if PvP is allowed, false otherwise.
	 */
	@Override
	public boolean isPvpAllowed(Location location) {
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
	 */
	@Override
	public boolean isFlagAllowed(Player player, CustomFlag customFlag) {
		try {
			ApplicableRegionSet regions = getApplicableRegion(player.getLocation());
			StateFlag flag = customFlags.get(customFlag.getPath());
			if (flag == null) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Custom flag not found: " + customFlag.getPath());
				return true; // Default to allowing if flag is not registered
			}
			return regions.queryValue(worldGuardPlugin.wrapPlayer(player), flag) != StateFlag.State.DENY;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking flag " + customFlag.getPath() + " for player: " + player.getName(), e);
			return true; // Default to allowing if an error occurs
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
			if (location == null) {
				throw new IllegalArgumentException("Location cannot be null");
			}
			return worldGuard.getPlatform().getRegionContainer()
					.createQuery()
					.getApplicableRegions(BukkitAdapter.adapt(location));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to query WorldGuard regions at location: " + location, e);
		}
	}
}