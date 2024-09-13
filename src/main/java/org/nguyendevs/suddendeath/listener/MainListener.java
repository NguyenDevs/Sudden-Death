package org.nguyendevs.suddendeath.listener;

import org.nguyendevs.suddendeath.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainListener implements Listener {
    private static final Set<UUID> noDrop = new HashSet<>();

    @EventHandler
    public void a(PlayerJoinEvent event) {
        PlayerData.setup(event.getPlayer());
    }

    @EventHandler
    public void b(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (noDrop.contains(player.getUniqueId())) {
            noDrop.remove(player.getUniqueId());
            event.setCancelled(true);
        }
    }

    public static void cancelNextDrop(Player player) {
        noDrop.add(player.getUniqueId());
    }
}
