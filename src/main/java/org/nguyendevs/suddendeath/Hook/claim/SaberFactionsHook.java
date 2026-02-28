package org.nguyendevs.suddendeath.Hook.claim;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Claim provider for the SaberFactions plugin.
 * Uses Board and FLocation to check whether locations fall within faction
 * territory.
 */
public class SaberFactionsHook implements ClaimProvider {

    private boolean available = false;

    public SaberFactionsHook() {
        try {
            if (SuddenDeath.getInstance().getServer().getPluginManager().getPlugin("Factions") != null) {
                // Verify the SaberFactions classes are available
                Class.forName("com.massivecraft.factions.Board");
                Class.forName("com.massivecraft.factions.FLocation");
                this.available = true;
            }
        } catch (ClassNotFoundException e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Factions plugin found but SaberFactions API classes not available", e);
            this.available = false;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Failed to hook into SaberFactions plugin", e);
            this.available = false;
        }
    }

    @Override
    public String getName() {
        return "SaberFactions";
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
            FLocation fLocation = new FLocation(location);
            Faction factionAt = Board.getInstance().getFactionAt(fLocation);
            // Wilderness (no faction) is not protected
            return factionAt != null && !factionAt.isWilderness();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking SaberFactions protection at " + location, e);
            return false;
        }
    }

    @Override
    public boolean isProtected(Location location, Player player) {
        if (!available || location == null || location.getWorld() == null || player == null)
            return false;
        try {
            FLocation fLocation = new FLocation(location);
            Faction factionAt = Board.getInstance().getFactionAt(fLocation);

            if (factionAt == null || factionAt.isWilderness())
                return false;

            // Check if the player is a member of the faction that owns this territory
            Faction playerFaction = FPlayers.getInstance().getByPlayer(player).getFaction();
            return !factionAt.equals(playerFaction);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking SaberFactions protection for player " + player.getName(), e);
            return false;
        }
    }
}
