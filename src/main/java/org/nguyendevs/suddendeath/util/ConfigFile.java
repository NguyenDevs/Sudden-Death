package org.nguyendevs.suddendeath.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Utility class for managing configuration files in the SuddenDeath plugin.
 */
public class ConfigFile {
    private static final boolean DEFAULT_UPDATE_NOTIFY = true;

    private final Plugin plugin;
    private final String path;
    private final String name;
    private final FileConfiguration config;
    private final File configFile;

    /**
     * Constructs a ConfigFile with the default plugin and no path.
     *
     * @param name The name of the configuration file (without .yml extension).
     * @throws IllegalArgumentException if name is null or empty.
     */
    public ConfigFile(String name) {
        this(SuddenDeath.getInstance(), "", name);
    }

    /**
     * Constructs a ConfigFile with the specified plugin and no path.
     *
     * @param plugin The plugin instance.
     * @param name   The name of the configuration file (without .yml extension).
     * @throws IllegalArgumentException if plugin or name is null or empty.
     */
    public ConfigFile(Plugin plugin, String name) {
        this(plugin, "", name);
    }

    /**
     * Constructs a ConfigFile for a specific mob type in the customMobs directory.
     *
     * @param mobType The EntityType for the mob configuration.
     * @throws IllegalArgumentException if mobType is null.
     */
    public ConfigFile(EntityType mobType) {
        this(SuddenDeath.getInstance(), "/customMobs", mobType != null ? Utils.lowerCaseId(mobType.name()) : null);
    }

    /**
     * Constructs a ConfigFile with the specified path and name.
     *
     * @param path The directory path relative to the plugin's data folder.
     * @param name The name of the configuration file (without .yml extension).
     * @throws IllegalArgumentException if path or name is null or empty.
     */
    public ConfigFile(String path, String name) {
        this(SuddenDeath.getInstance(), path, name);
    }

    /**
     * Constructs a ConfigFile with the specified plugin, path, and name.
     *
     * @param plugin The plugin instance.
     * @param path   The directory path relative to the plugin's data folder.
     * @param name   The name of the configuration file (without .yml extension).
     * @throws IllegalArgumentException if plugin, path, or name is null or empty.
     */
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

    /**
     * Sets up the configuration file by creating its directory and file if they do not exist.
     * For config.yml, copies the file from resources if it does not exist.
     */
    public void setup() {
        try {
            File directory = new File(plugin.getDataFolder(), path);
            if (!directory.exists() && !directory.mkdirs()) {
                plugin.getLogger().log(Level.WARNING, "Failed to create directory: " + directory.getPath());
            }

            if (!configFile.exists()) {
                if (name.equals("config") && path.isEmpty()) {
                    try {
                        plugin.saveResource("config.yml", false);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to copy config.yml from resources, creating empty file with defaults", e);
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

    /**
     * Sets default values for config.yml if it cannot be copied from resources.
     */
    private void setDefaultConfigValues() {
        if (name.equals("config") && path.isEmpty()) {
            config.set("update-notify", DEFAULT_UPDATE_NOTIFY);
        }
    }

    /**
     * Loads the configuration from the file without overwriting in-memory changes.
     */
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

    /**
     * Reloads the configuration from the file, preserving user changes on disk.
     */
    public void reload() {
        try {
            if (configFile.exists()) {
                config.load(configFile);
            } else {
                plugin.getLogger().log(Level.WARNING, "Configuration file does not exist: " + name + ".yml");
                setup(); // Recreate the file if it’s missing
            }
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to reload configuration file: " + name + ".yml. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Saves the configuration to its file.
     */
    public void save() {
        if (config.getKeys(true).isEmpty()) {
            plugin.getLogger().log(Level.INFO, "No changes to save for configuration file: " + name + ".yml");
            return;
        }
        try {
            config.save(configFile);
            plugin.getLogger().log(Level.INFO, "Saved configuration file: " + name + ".yml");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save configuration file: " + name + ".yml", e);
        }
    }

    /**
     * Gets the configuration object.
     *
     * @return The FileConfiguration instance.
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Retrieves a colored string from the configuration with color codes translated.
     *
     * @param path         The configuration path.
     * @param defaultValue The default value if the path is not found.
     * @return The translated string with color codes applied.
     */
    public String getColoredString(String path, String defaultValue) {
        try {
            String value = getConfig().getString(path, defaultValue);
            return value != null ? ChatColor.translateAlternateColorCodes('&', value) : defaultValue;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error reading string '" + path + "' from " + name + ".yml. Using default value: " + defaultValue, e);
            return ChatColor.translateAlternateColorCodes('&', defaultValue);
        }
    }

    /**
     * Retrieves the update-notify value from the configuration.
     * Only applicable for config.yml.
     *
     * @return The update-notify value, defaulting to true if invalid.
     */
    public boolean getUpdateNotify() {
        if (!name.equals("config")) {
            plugin.getLogger().log(Level.WARNING, "getUpdateNotify is only applicable for config.yml, not " + name + ".yml");
            return DEFAULT_UPDATE_NOTIFY;
        }
        try {
            return getConfig().getBoolean("update-notify", DEFAULT_UPDATE_NOTIFY);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error reading update-notify from config.yml. Using default value: " + DEFAULT_UPDATE_NOTIFY, e);
            return DEFAULT_UPDATE_NOTIFY;
        }
    }

}