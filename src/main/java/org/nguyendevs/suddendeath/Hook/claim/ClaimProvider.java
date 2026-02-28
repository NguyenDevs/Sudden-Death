package org.nguyendevs.suddendeath.Hook.claim;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Interface for claim/protection plugin hooks.
 * Each implementation checks whether a location is protected by a specific
 * claiming plugin.
 */
public interface ClaimProvider {

    /**
     * @return Human-readable name of this claim provider (e.g., "Lands",
     *         "SaberFactions").
     */
    String getName();

    /**
     * @return true if the backing plugin is present and loaded on this server.
     */
    boolean isAvailable();

    /**
     * Check if a location is inside a claimed/protected area (regardless of who
     * owns it).
     *
     * @param location the location to check
     * @return true if the location is in a claimed area
     */
    boolean isProtected(Location location);

    /**
     * Check if a location is inside a claimed area that the given player does NOT
     * own or
     * have build trust in. Returns false if the player is trusted/owns the area.
     *
     * @param location the location to check
     * @param player   the player to evaluate permissions for
     * @return true if the location is claimed by someone else (not this player)
     */
    boolean isProtected(Location location, Player player);
}
