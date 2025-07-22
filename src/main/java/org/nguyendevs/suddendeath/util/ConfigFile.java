package org.nguyendevs.suddendeath.util;

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
     * @param name The name of the configuration file (without .yml extension).
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
     * @param path The directory path relative to the plugin's data folder.
     * @param name The name of the configuration file (without .yml extension).
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
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Saves the configuration to its file.
     */
    public void save() {
        try {
            config.save(configFile);
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
     * Sets up the configuration file by creating its directory and file if they do not exist.
     */
    public void setup() {
        try {
            File directory = new File(plugin.getDataFolder(), path);
            if (!directory.exists() && !directory.mkdirs()) {
                plugin.getLogger().log(Level.WARNING, "Failed to create directory: " + directory.getPath());
            }

            if (!configFile.exists() && !configFile.createNewFile()) {
                plugin.getLogger().log(Level.WARNING, "Failed to create configuration file: " + name + ".yml");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting up configuration file: " + name + ".yml", e);
        }
    }
}