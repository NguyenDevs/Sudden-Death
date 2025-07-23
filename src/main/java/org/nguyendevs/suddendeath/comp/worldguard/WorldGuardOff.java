package org.nguyendevs.suddendeath.comp.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Fallback implementation of WGPlugin when WorldGuard is not available.
 * Allows all PvP and custom flag checks by default.
 */
public class WorldGuardOff implements WGPlugin {

	/**
	 * Checks if PvP is allowed at the specified location.
	 * Always returns true as WorldGuard is not active.
	 *
	 * @param location The location to check for PvP allowance.
	 * @return true, as PvP is always allowed.
	 * @throws IllegalArgumentException if the location is null.
	 */
	@Override
	public boolean isPvpAllowed(Location location) {
		if (location == null) {
			throw new IllegalArgumentException("Location cannot be null");
		}
		return true;
	}

	/**
	 * Checks if a custom flag is allowed for a player at their current location.
	 * Always returns true as WorldGuard is not active.
	 *
	 * @param player     The player to check the flag for.
	 * @param customFlag The custom flag to query.
	 * @return true, as all flags are allowed.
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
		return true;
	}
}