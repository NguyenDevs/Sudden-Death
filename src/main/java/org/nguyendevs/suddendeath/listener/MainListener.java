package org.nguyendevs.suddendeath.listener;

import org.nguyendevs.suddendeath.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Main event listener for handling player interactions in the SuddenDeath plugin
 * Handles player join events and item drop prevention
 */
public class MainListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(MainListener.class.getName());

    // Thread-safe set để lưu trữ UUID của players có item drop bị hủy
    private static final Set<UUID> noDropPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Handles player join event
     * Sets up player data when a player joins the server
     *
     * @param event PlayerJoinEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                PlayerData.setup(player);
                LOGGER.info("Setup player data for: " + player.getName());
            }
        } catch (Exception e) {
            LOGGER.severe("Error setting up player data for " +
                    (event.getPlayer() != null ? event.getPlayer().getName() : "unknown player") +
                    ": " + e.getMessage());
        }
    }

    /**
     * Handles player quit event
     * Cleans up player data when player leaves to prevent memory leaks
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                UUID playerId = player.getUniqueId();
                // Cleanup để tránh memory leak
                noDropPlayers.remove(playerId);
                LOGGER.fine("Cleaned up data for player: " + player.getName());
            }
        } catch (Exception e) {
            LOGGER.warning("Error cleaning up player data: " + e.getMessage());
        }
    }

    /**
     * Handles player item drop event
     * Prevents item dropping for players in the no-drop set
     *
     * @param event PlayerDropItemEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                UUID playerId = player.getUniqueId();

                if (noDropPlayers.contains(playerId)) {
                    // Remove player từ set và hủy event
                    noDropPlayers.remove(playerId);
                    event.setCancelled(true);

                    LOGGER.fine("Cancelled item drop for player: " + player.getName());
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error handling item drop event for " +
                    (event.getPlayer() != null ? event.getPlayer().getName() : "unknown player") +
                    ": " + e.getMessage());
            // Trong trường hợp lỗi, vẫn remove player khỏi set để tránh stuck state
            if (event.getPlayer() != null) {
                noDropPlayers.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    /**
     * Adds a player to the no-drop set, preventing their next item drop
     * Thread-safe method for adding players to the prevention list
     *
     * @param player The player whose next drop should be cancelled
     * @throws IllegalArgumentException if player is null
     */
    public static void cancelNextDrop(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        try {
            UUID playerId = player.getUniqueId();
            noDropPlayers.add(playerId);
            LOGGER.fine("Added player to no-drop list: " + player.getName());
        } catch (Exception e) {
            LOGGER.warning("Error adding player to no-drop list: " + e.getMessage());
        }
    }

    /**
     * Removes a player from the no-drop set
     * Useful for manually clearing the prevention state
     *
     * @param player The player to remove from the no-drop list
     */
    public static void allowNextDrop(Player player) {
        if (player != null) {
            try {
                UUID playerId = player.getUniqueId();
                boolean wasRemoved = noDropPlayers.remove(playerId);
                if (wasRemoved) {
                    LOGGER.fine("Removed player from no-drop list: " + player.getName());
                }
            } catch (Exception e) {
                LOGGER.warning("Error removing player from no-drop list: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if a player is currently in the no-drop state
     *
     * @param player The player to check
     * @return true if the player's next drop will be cancelled
     */
    public static boolean isDropCancelled(Player player) {
        if (player == null) {
            return false;
        }

        try {
            return noDropPlayers.contains(player.getUniqueId());
        } catch (Exception e) {
            LOGGER.warning("Error checking drop status for player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears all players from the no-drop set
     * Useful for plugin reload or cleanup
     */
    public static void clearAllNoDropPlayers() {
        try {
            int count = noDropPlayers.size();
            noDropPlayers.clear();
            LOGGER.info("Cleared " + count + " players from no-drop list");
        } catch (Exception e) {
            LOGGER.warning("Error clearing no-drop list: " + e.getMessage());
        }
    }

    /**
     * Gets the current count of players in the no-drop state
     *
     * @return number of players currently in no-drop state
     */
    public static int getNoDropPlayersCount() {
        try {
            return noDropPlayers.size();
        } catch (Exception e) {
            LOGGER.warning("Error getting no-drop players count: " + e.getMessage());
            return 0;
        }
    }
}