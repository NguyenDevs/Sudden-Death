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
    private boolean scsEnabled = false;
    private boolean xclaimEnabled = false;

    private Object landsIntegration;
    private Method landsGetAreaMethod;

    private Object scsApiInstance;
    private Method scsIsClaimedMethod;

    private Method xclaimGetByChunkMethod;

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
                landsGetAreaMethod = landsIntegrationClass.getMethod("getArea", Location.class);
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

        Plugin scsPlugin = Bukkit.getPluginManager().getPlugin("SimpleClaimSystem");
        if (scsPlugin != null && scsPlugin.isEnabled()) {
            try {
                Class<?> apiProviderClass = Class.forName("fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider");
                Class<?> scsMainClass = Class.forName("fr.xyness.SCS.SimpleClaimSystem");
                Method initializeMethod = apiProviderClass.getMethod("initialize", scsMainClass);
                initializeMethod.invoke(null, scsPlugin);

                Method getApiMethod = apiProviderClass.getMethod("getAPI");
                scsApiInstance = getApiMethod.invoke(null);
                Class<?> apiClass = Class.forName("fr.xyness.SCS.API.SimpleClaimSystemAPI");
                scsIsClaimedMethod = apiClass.getMethod("isClaimed", org.bukkit.Chunk.class);
                scsEnabled = true;
                Bukkit.getConsoleSender().sendMessage(
                        Utils.color("&6[&cSudden&4Death&6] &aHooked into SimpleClaimSystem for claim protection."));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to hook into SimpleClaimSystem API", e);
            }
        }

        Plugin xclaimPlugin = Bukkit.getPluginManager().getPlugin("xclaim");
        if (xclaimPlugin != null && xclaimPlugin.isEnabled()) {
            try {
                Class<?> claimClass = Class.forName("codes.wasabi.xclaim.api.Claim");
                xclaimGetByChunkMethod = claimClass.getMethod("getByChunk", org.bukkit.Chunk.class);
                xclaimEnabled = true;
                Bukkit.getConsoleSender().sendMessage(
                        Utils.color("&6[&cSudden&4Death&6] &aHooked into xclaim for claim protection."));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to hook into xclaim API", e);
            }
        }
    }

    public boolean isClaimed(Location location) {
        if (location == null)
            return false;

        if (landsEnabled) {
            try {
                Object area = landsGetAreaMethod.invoke(landsIntegration, location);
                if (area != null) {
                    return true;
                }
            } catch (Exception e) {
            }
        }

        if (superiorSkyblockEnabled) {
            try {
                Class<?> apiClass = Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
                Object grid = apiClass.getMethod("getGrid").invoke(null);

                Object island = grid.getClass().getMethod("getIslandAt", Location.class).invoke(grid, location);
                if (island != null) {
                    return true;
                }
            } catch (Exception e) {
            }
        }

        if (griefPreventionEnabled) {
            try {
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
            }
        }

        if (scsEnabled) {
            try {
                Boolean isClaimed = (Boolean) scsIsClaimedMethod.invoke(scsApiInstance, location.getChunk());
                if (isClaimed != null && isClaimed) {
                    return true;
                }
            } catch (Exception e) {
            }
        }

        if (xclaimEnabled) {
            try {
                Object claim = xclaimGetByChunkMethod.invoke(null, location.getChunk());
                if (claim != null) {
                    return true;
                }
            } catch (Exception e) {
            }
        }

        return false;
    }
}
