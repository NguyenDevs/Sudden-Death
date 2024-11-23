package org.nguyendevs.suddendeath.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class AbstractConfig {
    private final FileConfiguration config;

private final File file;

private final Plugin plugin;

public AbstractConfig(Plugin plugin, String name) {
    this.plugin = plugin;
    this.config = (FileConfiguration)new YamlConfiguration();
    this.file = new File(plugin.getDataFolder(), name);
}

protected FileConfiguration getConfig() {
    return this.config;
}

private void create() {
    if (!this.file.exists()) {
        this.file.getParentFile().mkdirs();
        this.plugin.saveResource(this.file.getName(), false);
    }
}

public void load() {
    create();
    try {
        this.config.load(this.file);
    } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
        this.plugin.getLogger().warning(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Can't load the plugin configuration: " + e.getMessage());
    }
}

public void save() {
    try {
        this.config.save(this.file);
    } catch (IOException e) {
        this.plugin.getLogger().warning(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Can't save the plugin configuration: " + e.getMessage());
    }
}
}