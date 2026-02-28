package org.nguyendevs.suddendeath.Hook.claim;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.LandWorld;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Claim provider for the Lands plugin.
 * Uses LandsIntegration to check whether locations fall within claimed land
 * areas.
 */
public class LandsHook implements ClaimProvider {

    private LandsIntegration landsIntegration;
    private boolean available = false;

    public LandsHook() {
        try {
            if (SuddenDeath.getInstance().getServer().getPluginManager().getPlugin("Lands") != null) {
                this.landsIntegration = LandsIntegration.of(SuddenDeath.getInstance());
                this.available = true;
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Failed to hook into Lands plugin", e);
            this.available = false;
        }
    }

    @Override
    public String getName() {
        return "Lands";
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
            Area area = landsIntegration.getArea(location);
            return area != null;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking Lands protection at " + location, e);
            return false;
        }
    }

    @Override
    public boolean isProtected(Location location, Player player) {
        if (!available || location == null || location.getWorld() == null || player == null)
            return false;
        try {
            LandWorld landWorld = landsIntegration.getWorld(location.getWorld());
            if (landWorld == null)
                return false;

            Area area = landsIntegration.getArea(location);
            if (area == null)
                return false;

            // Check if the player has build trust in this area
            // If they can build, the area is not "protected" from them
            return !area.isTrusted(player.getUniqueId());
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking Lands protection for player " + player.getName(), e);
            return false;
        }
    }
}
