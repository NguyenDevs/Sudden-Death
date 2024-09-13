package org.nguyendevs.suddendeath.scheduler;

import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.Map;

public class BloodEffectRunnable implements Runnable {
    private final SuddenDeath plugin;

    public BloodEffectRunnable(SuddenDeath plugin) {
        this.plugin = plugin;
    }

    public void run() {
        for (Map.Entry<Player, Integer> entry : (Iterable<Map.Entry<Player, Integer>>)this.plugin.getPlayers().entrySet()) {
            Player player = entry.getKey();
            if (!player.isOnline())
                this.plugin.getPlayers().remove(player);
            WorldBorder border = player.getWorld().getWorldBorder();
            int minDistance = (int)(border.getSize() / 2.0D - player.getLocation().distance(border.getCenter()));
            Integer distance = entry.getValue();
            this.plugin.getPacketSender().fading(player, distance.intValue());
            distance = Integer.valueOf((int)(distance.intValue() * this.plugin.getConfiguration().getCoefficient()));
            entry.setValue(distance);
            if (minDistance >= distance.intValue() || player.isDead() || this.plugin.getConfig().getBoolean("blood-enable") == false) {
                this.plugin.getPlayers().remove(player);
                this.plugin.getPacketSender().fading(player, border.getWarningDistance());
            }
        }
    }
}

