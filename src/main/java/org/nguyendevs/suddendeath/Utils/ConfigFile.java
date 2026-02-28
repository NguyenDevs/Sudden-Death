package org.nguyendevs.suddendeath.Utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigFile {
    private static final boolean DEFAULT_UPDATE_NOTIFY = true;

    private final Plugin plugin;
    private final String path;
    private final String name;
    private final FileConfiguration config;
    private final File configFile;

    public ConfigFile(String name) {
        this(SuddenDeath.getInstance(), "", name);
    }

    public ConfigFile(Plugin plugin, String name) {
        this(plugin, "", name);
    }

    public ConfigFile(EntityType mobType) {
        this(SuddenDeath.getInstance(), "/customMobs", mobType != null ? Utils.lowerCaseId(mobType.name()) : null);
    }

    public ConfigFile(String path, String name) {
        this(SuddenDeath.getInstance(), path, name);
    }

    public ConfigFile(Plugin plugin, String path, String name) {
        if (plugin == null || path == null || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin, path, and name cannot be null or empty");
        }
        this.plugin = plugin;
        this.path = path.endsWith("/") ? path : path + "/";
        this.name = name.endsWith(".yml") ? name.substring(0, name.length() - 4) : name;
        this.configFile = new File(plugin.getDataFolder(), this.path + this.name + ".yml");
        this.config = new YamlConfiguration();
        setup();
    }

    public void setup() {
        try {
            File directory = new File(plugin.getDataFolder(), path);
            if (!directory.exists() && !directory.mkdirs()) {
                plugin.getLogger().log(Level.WARNING, "Failed to create directory: " + directory.getPath());
            }

            if (!configFile.exists()) {
                String resourcePath = (path.startsWith("/") ? path.substring(1) : path) + name + ".yml";
                if (plugin.getResource(resourcePath) != null) {
                    try {
                        plugin.saveResource(resourcePath, false);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to copy " + resourcePath + " from resources", e);
                        if (!configFile.createNewFile()) {
                            plugin.getLogger().log(Level.WARNING,
                                    "Failed to create configuration file: " + name + ".yml");
                        }
                    }
                } else if (name.equals("config") && path.isEmpty()) {
                    try {
                        plugin.saveResource("config.yml", false);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to copy config.yml from resources, creating empty file with defaults", e);
                        setDefaultConfigValues();
                        save();
                    }
                } else {
                    if (!configFile.createNewFile()) {
                        plugin.getLogger().log(Level.WARNING, "Failed to create configuration file: " + name + ".yml");
                    }
                }
            }
            load();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting up configuration file: " + name + ".yml", e);
        }
    }

    private void setDefaultConfigValues() {
        if (name.equals("config") && path.isEmpty()) {
            config.set("update-notify", DEFAULT_UPDATE_NOTIFY);
        }
    }

    public void load() {
        try {
            if (configFile.exists()) {
                config.load(configFile);
            } else {
                plugin.getLogger().log(Level.WARNING, "Configuration file does not exist: " + name + ".yml");
            }
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to load configuration file: " + name + ".yml. Error: " + e.getMessage(), e);
        }
    }

    public void reload() {
        try {
            if (configFile.exists()) {
                config.load(configFile);
            } else {
                plugin.getLogger().log(Level.WARNING, "Configuration file does not exist: " + name + ".yml");
                setup();
            }
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to reload configuration file: " + name + ".yml. Error: " + e.getMessage(), e);
        }
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save configuration file: " + name + ".yml", e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }
}