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
    private static final double DEFAULT_COEFFICIENT = 0.95;
    private static final int DEFAULT_INTERVAL = 6;
    private static final String DEFAULT_MODE = "default";

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
     */
    public void setup() {
        try {
            File directory = new File(plugin.getDataFolder(), path);
            if (!directory.exists() && !directory.mkdirs()) {
                plugin.getLogger().log(Level.WARNING, "Failed to create directory: " + directory.getPath());
            }

            if (!configFile.exists()) {
                if (!configFile.createNewFile()) {
                    plugin.getLogger().log(Level.WARNING, "Failed to create configuration file: " + name + ".yml");
                }
            } else {
                // Tải file hiện có để giữ các thay đổi
                load();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting up configuration file: " + name + ".yml", e);
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
     * Retrieves the coefficient value from the configuration, ensuring it is less than 1.0.
     * Only applicable for config.yml.
     *
     * @return The coefficient value, defaulting to 0.95 if invalid.
     */
    public double getCoefficient() {
        if (!name.equals("config")) {
            plugin.getLogger().log(Level.WARNING, "getCoefficient is only applicable for config.yml, not " + name + ".yml");
            return DEFAULT_COEFFICIENT;
        }
        try {
            double coefficient = getConfig().getDouble("coefficient", DEFAULT_COEFFICIENT);
            if (coefficient >= 1.0) {
                plugin.getLogger().log(Level.WARNING,
                        "Invalid coefficient value (>= 1.0) in config.yml. Using default value: " + DEFAULT_COEFFICIENT);
                return DEFAULT_COEFFICIENT;
            }
            return coefficient;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error reading coefficient from config.yml. Using default value: " + DEFAULT_COEFFICIENT, e);
            return DEFAULT_COEFFICIENT;
        }
    }

    /**
     * Retrieves the fading mode from the configuration.
     * Only applicable for config.yml.
     *
     * @return The FadingType, defaulting to DEFAULT if invalid.
     */
    public FadingType getMode() {
        if (!name.equals("config")) {
            plugin.getLogger().log(Level.WARNING, "getMode is only applicable for config.yml, not " + name + ".yml");
            return FadingType.DEFAULT;
        }
        try {
            String type = getConfig().getString("mode", DEFAULT_MODE).toLowerCase();
            return switch (type) {
                case "default" -> FadingType.DEFAULT;
                case "health" -> FadingType.HEALTH;
                case "damage" -> FadingType.DAMAGE;
                default -> {
                    plugin.getLogger().log(Level.WARNING,
                            "Invalid mode '" + type + "' in config.yml. Using default mode: " + DEFAULT_MODE);
                    yield FadingType.DEFAULT;
                }
            };
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error reading mode from config.yml. Using default mode: " + DEFAULT_MODE, e);
            return FadingType.DEFAULT;
        }
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
     * Retrieves the interval value from the configuration.
     * Only applicable for config.yml.
     *
     * @return The interval value, defaulting to 6 if invalid.
     */
    public int getInterval() {
        if (!name.equals("config")) {
            plugin.getLogger().log(Level.WARNING, "getInterval is only applicable for config.yml, not " + name + ".yml");
            return DEFAULT_INTERVAL;
        }
        try {
            int interval = getConfig().getInt("interval", DEFAULT_INTERVAL);
            if (interval <= 0) {
                plugin.getLogger().log(Level.WARNING,
                        "Invalid interval value (<= 0) in config.yml. Using default value: " + DEFAULT_INTERVAL);
                return DEFAULT_INTERVAL;
            }
            return interval;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error reading interval from config.yml. Using default value: " + DEFAULT_INTERVAL, e);
            return DEFAULT_INTERVAL;
        }
    }
}