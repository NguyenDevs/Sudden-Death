package org.nguyendevs.suddendeath.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.nguyendevs.suddendeath.SuddenDeath;

public class RecipeDiscoveryListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay 1 tick để đảm bảo player đã load hoàn toàn
        Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
            SuddenDeath.getInstance().unlockRecipesForPlayer(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Re-unlock recipes sau khi respawn (tùy chọn)
        Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
            SuddenDeath.getInstance().unlockRecipesForPlayer(player);
        }, 20L); // Delay 1 giây
    }
}