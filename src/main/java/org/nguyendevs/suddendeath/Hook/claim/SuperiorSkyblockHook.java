package org.nguyendevs.suddendeath.Hook.claim;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Claim provider for the SuperiorSkyblock2 plugin.
 * Uses SuperiorSkyblockAPI to check whether locations fall within island areas.
 */
public class SuperiorSkyblockHook implements ClaimProvider {

    private boolean available = false;

    public SuperiorSkyblockHook() {
        try {
            if (SuddenDeath.getInstance().getServer().getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
                // Verify API class availability
                Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
                this.available = true;
            }
        } catch (ClassNotFoundException e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "SuperiorSkyblock2 plugin found but API classes not available", e);
            this.available = false;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Failed to hook into SuperiorSkyblock2 plugin", e);
            this.available = false;
        }
    }

    @Override
    public String getName() {
        return "SuperiorSkyblock2";
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
            Island island = SuperiorSkyblockAPI.getIslandAt(location);
            return island != null;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking SuperiorSkyblock2 protection at " + location, e);
            return false;
        }
    }

    @Override
    public boolean isProtected(Location location, Player player) {
        if (!available || location == null || location.getWorld() == null || player == null)
            return false;
        try {
            Island island = SuperiorSkyblockAPI.getIslandAt(location);
            if (island == null)
                return false;

            // Check if the player is a member of this island
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
            return !island.isMember(superiorPlayer);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking SuperiorSkyblock2 protection for player " + player.getName(), e);
            return false;
        }
    }
}
