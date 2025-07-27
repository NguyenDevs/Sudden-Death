package org.nguyendevs.suddendeath.comp.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Interface for WorldGuard integration in the SuddenDeath plugin.
 * Provides methods to check PvP and custom flag states in WorldGuard regions.
 */
public interface WGPlugin {

	/**
	 * Checks if PvP is allowed at the specified location based on WorldGuard regions.
	 *
	 * @param location The location to check for PvP allowance.
	 * @return true if PvP is allowed, false otherwise.
	 * @throws IllegalArgumentException if the location is null.
	 */
	boolean isPvpAllowed(Location location);

	/**
	 * Checks if a custom flag is allowed for a player at their current location.
	 *
	 * @param player     The player to check the flag for.
	 * @param customFlag The custom flag to query.
	 * @return true if the flag is allowed, false otherwise.
	 * @throws IllegalArgumentException if the player or customFlag is null.
	 */
	boolean isFlagAllowed(Player player, CustomFlag customFlag);
}
