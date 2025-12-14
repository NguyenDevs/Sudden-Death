package org.nguyendevs.suddendeath.Features.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Player.PlayerData;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerCoreFeature extends AbstractFeature {

    private static final Set<UUID> noDropPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public String getName() {
        return "Player Core";
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                PlayerData.setup(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting up player data: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            if (player != null) {
                noDropPlayers.remove(player.getUniqueId());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error cleaning up player data: " + e.getMessage());
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
            plugin.getLogger().log(Level.WARNING, "Error handling item drop: " + e.getMessage());
        }
    }

    public static void cancelNextDrop(Player player) {
        if (player == null) return;
        noDropPlayers.add(player.getUniqueId());
    }

    public static void allowNextDrop(Player player) {
        if (player == null) return;
        noDropPlayers.remove(player.getUniqueId());
    }
}