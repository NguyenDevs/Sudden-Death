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

public class MainListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(MainListener.class.getName());

    private static final Set<UUID> noDropPlayers = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                PlayerData.setup(player);
            }
        } catch (Exception e) {
            LOGGER.severe("Error setting up player data for " +
                    (event.getPlayer() != null ? event.getPlayer().getName() : "unknown player") +
                    ": " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                UUID playerId = player.getUniqueId();
                // Cleanup để tránh memory leak
                noDropPlayers.remove(playerId);
            }
        } catch (Exception e) {
            LOGGER.warning("Error cleaning up player data: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                UUID playerId = player.getUniqueId();

                if (noDropPlayers.contains(playerId)) {
                    noDropPlayers.remove(playerId);
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error handling item drop event for " +
                    (event.getPlayer() != null ? event.getPlayer().getName() : "unknown player") +
                    ": " + e.getMessage());
            if (event.getPlayer() != null) {
                noDropPlayers.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    public static void cancelNextDrop(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        try {
            UUID playerId = player.getUniqueId();
            noDropPlayers.add(playerId);
        } catch (Exception e) {
            LOGGER.warning("Error adding player to no-drop list: " + e.getMessage());
        }
    }

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

    public static void clearAllNoDropPlayers() {
        try {
            int count = noDropPlayers.size();
            noDropPlayers.clear();
        } catch (Exception e) {
            LOGGER.warning("Error clearing no-drop list: " + e.getMessage());
        }
    }

    public static int getNoDropPlayersCount() {
        try {
            return noDropPlayers.size();
        } catch (Exception e) {
            LOGGER.warning("Error getting no-drop players count: " + e.getMessage());
            return 0;
        }
    }
}