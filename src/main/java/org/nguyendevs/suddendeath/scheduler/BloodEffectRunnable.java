package org.nguyendevs.suddendeath.scheduler;

import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.ConfigFile;

import java.util.Map;
import java.util.logging.Level;

/**
 * Runnable task that applies a blood effect to players based on their distance from the world border.
 */
public class BloodEffectRunnable implements Runnable {
    private static final String BLOOD_ENABLE_PATH = "blood-enable";
    private final SuddenDeath plugin;

    /**
     * Constructs a BloodEffectRunnable with the specified plugin instance.
     *
     * @param plugin The SuddenDeath plugin instance.
     * @throws IllegalArgumentException if plugin is null.
     */
    public BloodEffectRunnable(SuddenDeath plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
    }

    /**
     * Runs the blood effect task, updating the fading effect for players based on their distance
     * from the world border and removing them when conditions are met.
     */
    @Override
    public void run() {
        try {
            Map<Player, Integer> players = plugin.getPlayers();
            ConfigFile config = plugin.getConfiguration();
            boolean isBloodEnabled = config.getConfig().getBoolean(BLOOD_ENABLE_PATH, false);

            for (Map.Entry<Player, Integer> entry : players.entrySet()) {
                Player player = entry.getKey();
                if (!player.isOnline() || player.isDead()) {
                    players.remove(player);
                    continue;
                }

                try {
                    WorldBorder border = player.getWorld().getWorldBorder();
                    double distanceToBorder = border.getSize() / 2.0 - player.getLocation().distance(border.getCenter());
                    int currentDistance = entry.getValue();

                    // Apply fading effect
                    plugin.getPacketSender().fading(player, currentDistance);

                    // Update distance with coefficient
                    int newDistance = (int) (currentDistance * config.getConfig().getDouble("coefficient", 1.0));
                    entry.setValue(newDistance);

                    // Remove player if they are too close to the border, dead, or blood effect is disabled
                    if (distanceToBorder >= currentDistance || !isBloodEnabled) {
                        players.remove(player);
                        plugin.getPacketSender().fading(player, border.getWarningDistance());
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Error processing blood effect for player: " + player.getName(), e);
                    players.remove(player);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error running BloodEffectRunnable", e);
        }
    }
}