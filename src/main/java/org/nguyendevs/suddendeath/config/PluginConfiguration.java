package org.nguyendevs.suddendeath.config;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.nguyendevs.suddendeath.FadingType;

public class PluginConfiguration extends AbstractConfig {
    private final Plugin plugin;

    public PluginConfiguration(Plugin plugin) {
        super(plugin, "config.yml");
        this.plugin = plugin;
    }

    public double getCoefficient() {
        double coefficient = getConfig().getDouble("coefficient", 0.95D);
        if (coefficient >= 1.0D) {
            coefficient = 0.95D;
            this.plugin.getLogger()
                    .warning("You selected the wrong coefficient value, which is greater than or equal to one.The coefficient is set to the default value of 0.95.");
        }
        return coefficient;
    }

    public FadingType getMode() {
        FadingType mode = FadingType.DEFAULT;
        String type = getConfig().getString("mode", "default");
        return switch (type) {
            case "health" -> {
                mode = FadingType.HEALTH;
                yield mode;
            }
            case "damage" -> {
                mode = FadingType.DAMAGE;
                yield mode;
            }
            default -> mode;
        };

    }

    private String getColoredString(String name, String def) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(name, def));
    }

    public int getInterval() {
        return getConfig().getInt("interval", 6);
    }

    public String messageOnEnable() {
        return getColoredString("messages.onEnable", "&aYou have enabled the first-person blood effect");
    }

    public String messageOnDisable() {
        return getColoredString("messages.onDisable", "&cYou have disabled the first-person blood effect");
    }

    public String messageNoPermission() {
        return getColoredString("messages.noPermission", "&cYou don't have permission for this");
    }
}
