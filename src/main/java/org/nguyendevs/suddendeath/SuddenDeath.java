package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nguyendevs.suddendeath.command.SuddenDeathMobCommand;
import org.nguyendevs.suddendeath.command.SuddenDeathStatusCommand;
import org.nguyendevs.suddendeath.command.completion.SuddenDeathMobCompletion;
import org.nguyendevs.suddendeath.command.completion.SuddenDeathStatusCompletion;
import org.nguyendevs.suddendeath.comp.SuddenDeathPlaceholders;
import org.nguyendevs.suddendeath.comp.worldguard.WGPlugin;
import org.nguyendevs.suddendeath.features.CustomMobs;
import org.nguyendevs.suddendeath.gui.listener.GuiListener;
import org.nguyendevs.suddendeath.manager.ConfigurationManager;
import org.nguyendevs.suddendeath.manager.EventManager;
import org.nguyendevs.suddendeath.manager.FeatureManager;
import org.nguyendevs.suddendeath.manager.WorldGuardManager;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.SpigotPlugin;

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

    @Override
    public void onLoad() {
        instance = this;
        this.worldGuardManager = new WorldGuardManager(this);
        this.worldGuardManager.registerFlags();
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

            this.worldGuardManager.initialize();

            registerListeners();
            hookIntoPlaceholderAPI();

            this.eventManager = new EventManager();

            this.featureManager = new FeatureManager(this);
            this.featureManager.registerAllFeatures();

            registerCommands();

            Bukkit.getOnlinePlayers().forEach(PlayerData::setup);

            printLogo();
            new SpigotPlugin(119526, this).checkForUpdate();

            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aSuddenDeath plugin enabled successfully!"));
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
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &cSuddenDeath plugin disabled.!"));
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
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aHooked onto PlaceholderAPI"));
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
        savePlayerData();
        configManager.reloadAll();
        featureManager.reloadFeatures();
        if (eventManager != null) eventManager.refresh();
        Bukkit.getOnlinePlayers().forEach(PlayerData::setup);
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
        return worldGuardManager.getProvider();
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void printLogo() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ███████╗██╗   ██╗██████╗ ██████╗ ███████╗███╗   ██╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ██╔════╝██║   ██║██╔══██╗██╔══██╗██╔════╝████╗  ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ███████╗██║   ██║██║  ██║██║  ██║█████╗  ██╔██╗ ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ╚════██║██║   ██║██║  ██║██║  ██║██╔══╝  ██║╚██╗██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ███████║╚██████╔╝██████╔╝██████╔╝███████╗██║ ╚████║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ╚══════╝ ╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██║  ██║█████╗  ███████║   ██║   ███████║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██████╔╝███████╗██║  ██║   ██║   ██║  ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4         Sudden Death"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6         Version " + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b         Development by NguyenDevs"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }
}