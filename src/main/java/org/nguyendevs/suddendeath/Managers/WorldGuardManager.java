package org.nguyendevs.suddendeath.Managers;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Hook.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.Hook.worldguard.WGPlugin;
import org.nguyendevs.suddendeath.Hook.worldguard.WorldGuardOff;
import org.nguyendevs.suddendeath.Hook.worldguard.WorldGuardOn;

import java.util.logging.Level;

public class WorldGuardManager {
    private final SuddenDeath plugin;
    private WGPlugin provider;
    private boolean isReady = false;

    public WorldGuardManager(SuddenDeath plugin) {
        this.plugin = plugin;
    }

    public void registerFlags() {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) return;
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            for (CustomFlag customFlag : CustomFlag.values()) {
                String flagPath = customFlag.getPath();
                if (registry.get(flagPath) == null) {
                    boolean defaultState = customFlag != CustomFlag.SDS_REMOVE;
                    StateFlag flag = new StateFlag(flagPath, defaultState);
                    registry.register(flag);
                }
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aWorldGuard flag registration completed."));
        } catch (FlagConflictException e) {
            plugin.getLogger().warning("Flag conflict: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error registering WorldGuard flags", e);
        }
    }

    public void initialize() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                org.bukkit.plugin.Plugin wgInfo = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aWorldGuard version: " + wgInfo.getDescription().getVersion()));

                this.provider = new WorldGuardOn();

                if (provider instanceof WorldGuardOn) {
                    WorldGuardOn wgOn = (WorldGuardOn) provider;
                    if (wgOn.isReady()) {
                        isReady = true;
                        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aWorldGuard integration ready immediately."));
                    } else {
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (wgOn.isReady()) {
                                isReady = true;
                                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aWorldGuard integration ready delayed."));
                            }
                        }, 40L);
                    }
                }
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &6WorldGuard not found, using fallback."));
                this.provider = new WorldGuardOff();
                isReady = true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize WorldGuard integration", e);
            this.provider = new WorldGuardOff();
            isReady = true;
        }
    }

    public WGPlugin getProvider() {
        return provider != null ? provider : new WorldGuardOff();
    }

    public boolean isReady() {
        return isReady;
    }
}