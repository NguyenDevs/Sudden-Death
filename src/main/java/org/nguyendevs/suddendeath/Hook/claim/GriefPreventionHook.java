package org.nguyendevs.suddendeath.Hook.claim;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Claim provider for the GriefPrevention plugin.
 * Uses GriefPrevention's DataStore to check whether locations fall within
 * player claims.
 */
public class GriefPreventionHook implements ClaimProvider {

    private boolean available = false;

    public GriefPreventionHook() {
        try {
            if (SuddenDeath.getInstance().getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
                Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                this.available = true;
            }
        } catch (ClassNotFoundException e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "GriefPrevention plugin found but API classes not available", e);
            this.available = false;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Failed to hook into GriefPrevention plugin", e);
            this.available = false;
        }
    }

    @Override
    public String getName() {
        return "GriefPrevention";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public boolean isProtected(Location location) {
        if (!available || location == null || location.getWorld() == null)
            return false;
        try {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            return claim != null;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking GriefPrevention protection at " + location, e);
            return false;
        }
    }

    @Override
    public boolean isProtected(Location location, Player player) {
        if (!available || location == null || location.getWorld() == null || player == null)
            return false;
        try {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            if (claim == null)
                return false;

            // If the player has build trust in this claim, it's not protected from them
            // allowBuild returns null if the player IS allowed to build
            return claim.allowBuild(player, location.getBlock().getType()) != null;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking GriefPrevention protection for player " + player.getName(), e);
            return false;
        }
    }
}
