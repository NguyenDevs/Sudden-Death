package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nguyendevs.suddendeath.GUI.AdminView;
import org.nguyendevs.suddendeath.GUI.PluginInventory;
import org.nguyendevs.suddendeath.Commands.SuddenDeathMobCommand;
import org.nguyendevs.suddendeath.Commands.SuddenDeathStatusCommand;
import org.nguyendevs.suddendeath.Commands.completion.SuddenDeathMobCompletion;
import org.nguyendevs.suddendeath.Commands.completion.SuddenDeathStatusCompletion;
import org.nguyendevs.suddendeath.Utils.SuddenDeathPlaceholders;
import org.nguyendevs.suddendeath.Hook.WGPlugin;
import org.nguyendevs.suddendeath.Hook.WorldGuardOff;
import org.nguyendevs.suddendeath.Hook.claim.ClaimProtectionManager;
import org.nguyendevs.suddendeath.Features.CustomMobs;
import org.nguyendevs.suddendeath.GUI.listener.GuiListener;
import org.nguyendevs.suddendeath.Managers.ConfigurationManager;
import org.nguyendevs.suddendeath.Managers.EventManager;
import org.nguyendevs.suddendeath.Managers.FeatureManager;
import org.nguyendevs.suddendeath.Managers.WorldGuardManager;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.ConfigFile;
import org.nguyendevs.suddendeath.Utils.SpigotPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SuddenDeath extends JavaPlugin {
    private static SuddenDeath instance;
    private final Map<Player, Integer> players = new ConcurrentHashMap<>();

    public ConfigFile messages;
    public ConfigFile items;

    private ConfigurationManager configManager;
    private WorldGuardManager worldGuardManager;
    private FeatureManager featureManager;
    private EventManager eventManager;
    private ClaimProtectionManager claimProtectionManager;

    @Override
    public void onLoad() {
        instance = this;
        try {
            this.worldGuardManager = new WorldGuardManager(this);
            this.worldGuardManager.registerFlags();
        } catch (NoClassDefFoundError e) {
            // WorldGuard is not installed — manager will be created lazily in onEnable
            this.worldGuardManager = null;
        }
    }

    @Override
    public void onEnable() {
        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            if (protocolManager == null) {
                throw new IllegalStateException("ProtocolLib is required but not found");
            }

            this.configManager = new ConfigurationManager(this);
            this.configManager.initialize();

            if (this.worldGuardManager == null) {
                try {
                    this.worldGuardManager = new WorldGuardManager(this);
                } catch (NoClassDefFoundError ignored) {
                    // WorldGuard classes not available at all
                }
            }
            if (this.worldGuardManager != null) {
                this.worldGuardManager.initialize();
            }

            this.claimProtectionManager = new ClaimProtectionManager(this);
            this.claimProtectionManager.initialize();

            registerListeners();
            hookIntoPlaceholderAPI();

            this.eventManager = new EventManager();

            this.featureManager = new FeatureManager(this);
            this.featureManager.registerAllFeatures();

            registerCommands();

            Bukkit.getOnlinePlayers().forEach(PlayerData::setup);

            printLogo();
            new SpigotPlugin(119526, this).checkForUpdate();

            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &aSuddenDeath plugin enabled successfully!"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SuddenDeath plugin.", e);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (featureManager != null) {
                featureManager.shutdownAll();
            }
            if (eventManager != null) {
                eventManager.cancel();
            }
            savePlayerData();
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &cSuddenDeath plugin disabled.!"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while disabling plugin", e);
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new CustomMobs(), this);
    }

    private void registerCommands() {
        Optional.ofNullable(getCommand("sds")).ifPresent(cmd -> {
            cmd.setExecutor(new SuddenDeathStatusCommand());
            cmd.setTabCompleter(new SuddenDeathStatusCompletion());
        });
        Optional.ofNullable(getCommand("sdm")).ifPresent(cmd -> {
            cmd.setExecutor(new SuddenDeathMobCommand());
            cmd.setTabCompleter(new SuddenDeathMobCompletion());
        });
    }

    private void hookIntoPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SuddenDeathPlaceholders().register();
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aHooked into PlaceholderAPI"));
        }
    }

    private void savePlayerData() {
        PlayerData.getLoaded().forEach(data -> {
            try {
                ConfigFile file = new ConfigFile(this, "/userdata", data.getUniqueId().toString());
                data.save(file.getConfig());
                file.save();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to save player data for " + data.getUniqueId(), e);
            }
        });
    }

    public void reloadConfigFiles() {
        // Close open plugin GUIs before reload to prevent stale-state crashes
        List<Player> adminViewPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory() != null
                    && p.getOpenInventory().getTopInventory().getHolder() instanceof AdminView) {
                adminViewPlayers.add(p);
                p.closeInventory();
            } else if (p.getOpenInventory() != null
                    && p.getOpenInventory().getTopInventory().getHolder() instanceof PluginInventory) {
                p.closeInventory();
            }
        }

        savePlayerData();
        configManager.reloadAll();
        featureManager.reloadFeatures();
        if (eventManager != null)
            eventManager.refresh();
        Bukkit.getOnlinePlayers().forEach(PlayerData::setup);

        // Reopen AdminView for players who had it open
        for (Player p : adminViewPlayers) {
            if (p.isOnline()) {
                Bukkit.getScheduler().runTask(this, () -> {
                    try {
                        new AdminView(p).open();
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING, "Failed to reopen AdminView for " + p.getName(), e);
                    }
                });
            }
        }
    }

    public static SuddenDeath getInstance() {
        return instance;
    }

    public Map<Player, Integer> getPlayers() {
        return players;
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public ConfigFile getConfiguration() {
        return configManager.getMainConfig();
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public WGPlugin getWorldGuard() {
        if (worldGuardManager == null) {
            return new WorldGuardOff();
        }
        return worldGuardManager.getProvider();
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public ClaimProtectionManager getClaimProtection() {
        return claimProtectionManager;
    }

    public void printLogo() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c   ███████╗██╗   ██╗██████╗ ██████╗ ███████╗███╗   ██╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c   ██╔════╝██║   ██║██╔══██╗██╔══██╗██╔════╝████╗  ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c   ███████╗██║   ██║██║  ██║██║  ██║█████╗  ██╔██╗ ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c   ╚════██║██║   ██║██║  ██║██║  ██║██╔══╝  ██║╚██╗██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c   ███████║╚██████╔╝██████╔╝██████╔╝███████╗██║ ╚████║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c   ╚══════╝ ╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&4   ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗"));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&4   ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║"));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&4   ██║  ██║█████╗  ███████║   ██║   ███████║"));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&4   ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║"));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&4   ██████╔╝███████╗██║  ██║   ██║   ██║  ██║"));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&4   ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4         Sudden Death"));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', "&6         Version " + getDescription().getVersion()));
        Bukkit.getConsoleSender()
                .sendMessage(ChatColor.translateAlternateColorCodes('&', "&b         Development by NguyenDevs"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }
}