package org.nguyendevs.suddendeath.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.nguyendevs.suddendeath.FadingType;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Event listener for handling blood effect calculations based on player damage in the SuddenDeath plugin.
 */
public class BloodEffectListener implements Listener {
    private final SuddenDeath plugin;

    /**
     * Constructs a BloodEffectListener with the specified plugin instance.
     *
     * @param plugin The SuddenDeath plugin instance.
     */
    public BloodEffectListener(SuddenDeath plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles damage events to calculate and update the blood effect distance for players.
     *
     * @param event The EntityDamageEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        try {
            // Calculate fake distance based on world border
            double distance = player.getWorld().getWorldBorder().getSize() / 2.0 - player.getLocation().distance(player.getWorld().getWorldBorder().getCenter());
            int fakeDistance = (int) (distance * plugin.getConfiguration().getInterval());

            // Adjust fake distance based on fading mode
            FadingType mode = plugin.getConfiguration().getMode();
            if (mode == FadingType.DAMAGE) {
                fakeDistance = (int) (fakeDistance * event.getDamage());
            } else if (mode == FadingType.HEALTH) {
                int health = (int) (player.getMaxHealth() - player.getHealth());
                health = Math.max(health, 1); // Ensure health is at least 1
                fakeDistance *= health;
            }

            // Update player's blood effect distance
            plugin.getPlayers().put(player, Math.abs(fakeDistance));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error handling EntityDamageEvent for player: " + player.getName(), e);
        }
    }
}