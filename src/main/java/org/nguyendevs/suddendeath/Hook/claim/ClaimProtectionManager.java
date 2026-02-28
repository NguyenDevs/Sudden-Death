package org.nguyendevs.suddendeath.Hook.claim;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Central manager for all claim protection hooks.
 * Checks all active providers — if ANY provider says a location is protected,
 * the action is blocked.
 */
public class ClaimProtectionManager {

    private final SuddenDeath plugin;
    private final List<ClaimProvider> activeProviders = new ArrayList<>();
    private boolean enabled = false;

    public ClaimProtectionManager(SuddenDeath plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the manager; reads config and attempts to hook into each enabled
     * plugin.
     * Should be called in onEnable() after config is loaded.
     */
    public void initialize() {
        activeProviders.clear();

        this.enabled = plugin.getConfiguration().getConfig().getBoolean("claim-protection.enabled", true);
        if (!enabled) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &6Claim protection is disabled in config."));
            return;
        }

        // Try each hook if enabled in config
        if (plugin.getConfiguration().getConfig().getBoolean("claim-protection.lands", true)) {
            tryHook(() -> new LandsHook());
        }

        if (plugin.getConfiguration().getConfig().getBoolean("claim-protection.saber-factions", true)) {
            tryHook(() -> new SaberFactionsHook());
        }

        if (plugin.getConfiguration().getConfig().getBoolean("claim-protection.superior-skyblock", true)) {
            tryHook(() -> new SuperiorSkyblockHook());
        }

        if (plugin.getConfiguration().getConfig().getBoolean("claim-protection.grief-prevention", true)) {
            tryHook(() -> new GriefPreventionHook());
        }

        if (activeProviders.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &6No claim protection plugins detected."));
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &aLoaded " + activeProviders.size() +
                            " claim protection hook(s)."));
        }
    }

    private void tryHook(java.util.function.Supplier<ClaimProvider> factory) {
        try {
            ClaimProvider provider = factory.get();
            if (provider.isAvailable()) {
                activeProviders.add(provider);
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6[&cSudden&4Death&6] &aHooked into " + provider.getName()));
            }
        } catch (NoClassDefFoundError ignored) {
            // Plugin API classes not on classpath — silently skip
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error trying to load a claim provider", e);
        }
    }

    /**
     * Check if a location is protected by ANY active claim provider.
     * Returns false immediately if claim protection is disabled or no providers are
     * loaded.
     *
     * @param location the location to check
     * @return true if the location is in a claimed/protected area
     */
    public boolean isProtected(Location location) {
        if (!enabled || activeProviders.isEmpty() || location == null)
            return false;
        for (ClaimProvider provider : activeProviders) {
            try {
                if (provider.isProtected(location)) {
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Error checking " + provider.getName() + " protection", e);
            }
        }
        return false;
    }

    /**
     * Check if a location is protected from a specific player by ANY active claim
     * provider.
     * Returns false if the player owns or is trusted in the area.
     *
     * @param location the location to check
     * @param player   the player to evaluate permissions for
     * @return true if the location is claimed by someone other than this player
     */
    public boolean isProtected(Location location, Player player) {
        if (!enabled || activeProviders.isEmpty() || location == null)
            return false;
        for (ClaimProvider provider : activeProviders) {
            try {
                if (provider.isProtected(location, player)) {
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Error checking " + provider.getName() + " protection for player " +
                                (player != null ? player.getName() : "null"),
                        e);
            }
        }
        return false;
    }

    /**
     * @return true if claim protection is enabled and has at least one active
     *         provider
     */
    public boolean isEnabled() {
        return enabled && !activeProviders.isEmpty();
    }

    /**
     * @return list of currently active provider names
     */
    public List<String> getActiveProviderNames() {
        List<String> names = new ArrayList<>();
        for (ClaimProvider provider : activeProviders) {
            names.add(provider.getName());
        }
        return names;
    }
}
