package org.nguyendevs.suddendeath.features.player;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.FadingType;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.logging.Level;

public class BloodScreenFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Blood Screen";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    var players = plugin.getPlayers();
                    for (var entry : players.entrySet()) {
                        Player player = entry.getKey();
                        if (!player.isOnline() || player.isDead()) {
                            players.remove(player);
                            continue;
                        }

                        WorldBorder border = player.getWorld().getWorldBorder();
                        double distanceToBorder = border.getSize() / 2.0 -
                                player.getLocation().distance(border.getCenter());
                        int currentDistance = entry.getValue();
                        plugin.getPacketSender().fading(player, currentDistance);

                        double coefficient = Feature.BLOOD_SCREEN.getDouble("coefficient");
                        int newDistance = (int) (currentDistance * coefficient);
                        entry.setValue(newDistance);

                        if (distanceToBorder >= currentDistance) {
                            players.remove(player);
                            plugin.getPacketSender().fading(player, border.getWarningDistance());
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in BloodScreenFeature loop", e);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 0L);
        registerTask((BukkitTask) task);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (!Feature.BLOOD_SCREEN.isEnabled(player)) return;

        try {
            double distance = player.getWorld().getWorldBorder().getSize() / 2.0 -
                    player.getLocation().distance(player.getWorld().getWorldBorder().getCenter());
            int fakeDistance = (int) (distance * Feature.BLOOD_SCREEN.getDouble("interval"));

            FadingType mode = FadingType.valueOf(Feature.BLOOD_SCREEN.getString("mode"));
            if (mode == FadingType.DAMAGE) {
                fakeDistance = (int) (fakeDistance * event.getDamage());
            } else if (mode == FadingType.HEALTH) {
                int health = (int) (player.getMaxHealth() - player.getHealth());
                health = Math.max(health, 1);
                fakeDistance *= health;
            }

            if (player.isOnGround() && !Utils.hasCreativeGameMode(player)) {
                double offsetX = (Math.random() - 0.5D) * 0.4D;
                double offsetY = 1.0 + ((Math.random() - 0.5D) * 0.5D);
                double offsetZ = (Math.random() - 0.5D) * 2.0D;

                player.getWorld().spawnParticle(
                        Particle.BLOCK_CRACK,
                        player.getLocation().add(offsetX, offsetY, offsetZ),
                        30,
                        Material.REDSTONE_BLOCK.createBlockData()
                );
            }
            plugin.getPlayers().put(player, Math.abs(fakeDistance));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in BloodScreenFeature.onEntityDamage", e);
        }
    }
}
