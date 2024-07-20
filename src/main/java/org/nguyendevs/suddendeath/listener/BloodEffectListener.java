package org.nguyendevs.suddendeath.listener;

import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.nguyendevs.suddendeath.FadingType;
import org.nguyendevs.suddendeath.SuddenDeath;

public class BloodEffectListener implements Listener {
    private final SuddenDeath plugin;

    public BloodEffectListener(SuddenDeath plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player))
            return;
        Player player = (Player)event.getEntity();
        WorldBorder border = player.getWorld().getWorldBorder();
        int fakeDistance = (int)(border.getSize() / 2.0D - player.getLocation().distance(border.getCenter())) * this.plugin.getConfiguration().getInterval();
        if (this.plugin.getConfiguration().getMode() == FadingType.DAMAGE) {
            fakeDistance = (int)(fakeDistance * event.getDamage());
        } else if (this.plugin.getConfiguration().getMode() == FadingType.HEALTH) {
            int health = (int)(player.getMaxHealth() - player.getHealth());
            health = (health > 0) ? health : 1;
            fakeDistance *= health;
        }
        this.plugin.getPlayers().put(player, Integer.valueOf(Math.abs(fakeDistance)));
    }
}