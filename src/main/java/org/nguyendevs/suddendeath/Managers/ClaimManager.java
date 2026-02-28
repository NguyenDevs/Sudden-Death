package org.nguyendevs.suddendeath.Managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class ClaimManager {
    private final SuddenDeath plugin;

    private boolean landsEnabled = false;
    private boolean superiorSkyblockEnabled = false;
    private boolean griefPreventionEnabled = false;

    // Lands reflection
    private Object landsIntegration;
    private Method landsIsClaimedMethod;

    public ClaimManager(SuddenDeath plugin) {
        this.plugin = plugin;
        checkHooks();
    }

    private void checkHooks() {
        Plugin landsConfig = Bukkit.getPluginManager().getPlugin("Lands");
        if (landsConfig != null && landsConfig.isEnabled()) {
            try {
                Class<?> landsIntegrationClass = Class.forName("me.angeschossen.lands.api.LandsIntegration");
                Method ofMethod = landsIntegrationClass.getMethod("of", Plugin.class);
                landsIntegration = ofMethod.invoke(null, plugin);
                landsIsClaimedMethod = landsIntegrationClass.getMethod("isClaimed", Location.class);
                landsEnabled = true;
                Bukkit.getConsoleSender()
                        .sendMessage(Utils.color("&6[&cSudden&4Death&6] &aHooked into Lands for claim protection."));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to hook into Lands API", e);
            }
        }

        Plugin superiorSkyblockPlugin = Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2");
        if (superiorSkyblockPlugin != null && superiorSkyblockPlugin.isEnabled()) {
            superiorSkyblockEnabled = true;
            Bukkit.getConsoleSender().sendMessage(
                    Utils.color("&6[&cSudden&4Death&6] &aHooked into SuperiorSkyblock2 for claim protection."));
        }

        Plugin griefPreventionPlugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
        if (griefPreventionPlugin != null && griefPreventionPlugin.isEnabled()) {
            griefPreventionEnabled = true;
            Bukkit.getConsoleSender().sendMessage(
                    Utils.color("&6[&cSudden&4Death&6] &aHooked into GriefPrevention for claim protection."));
        }
    }

    public boolean isClaimed(Location location) {
        if (location == null)
            return false;

        if (landsEnabled) {
            try {
                Boolean isClaimed = (Boolean) landsIsClaimedMethod.invoke(landsIntegration, location);
                if (isClaimed != null && isClaimed) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }

        if (superiorSkyblockEnabled) {
            try {
                // SuperiorSkyblockAPI.getGrid().getIslandAt(location) != null
                Class<?> apiClass = Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
                Object grid = apiClass.getMethod("getGrid").invoke(null);

                Object island = grid.getClass().getMethod("getIslandAt", Location.class).invoke(grid, location);
                if (island != null) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }

        if (griefPreventionEnabled) {
            try {
                // GriefPrevention.instance.dataStore.getClaimAt(location, false, null) != null
                Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                Object gpInstance = gpClass.getField("instance").get(null);

                Object dataStore = gpClass.getField("dataStore").get(gpInstance);

                Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
                Object claim = dataStore.getClass().getMethod("getClaimAt", Location.class, boolean.class, claimClass)
                        .invoke(dataStore, location, false, null);

                if (claim != null) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }

        return false;
    }
}
