package org.nguyendevs.suddendeath.Features.player;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.FadingType;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class BloodScreenFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Blood Screen";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Iterator<Map.Entry<Player, Integer>> iterator = plugin.getPlayers().entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Player, Integer> entry = iterator.next();
                        Player player = entry.getKey();

                        if (!player.isOnline() || player.isDead()) {
                            iterator.remove();
                            if (player.isOnline()) player.setWorldBorder(null);
                            continue;
                        }

                        WorldBorder worldBorder = player.getWorld().getWorldBorder();
                        double distanceToBorder = worldBorder.getSize() / 2.0 -
                                player.getLocation().distance(worldBorder.getCenter());
                        int currentDistance = entry.getValue();

                        sendRedScreenEffect(player, currentDistance);

                        int newDistance = (int) (currentDistance * Feature.BLOOD_SCREEN.getDouble("coefficient"));
                        entry.setValue(newDistance);

                        if (distanceToBorder >= currentDistance) {
                            iterator.remove();
                            player.setWorldBorder(null);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in BloodScreenFeature loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 0L));
    }

    private void sendRedScreenEffect(Player player, int warningDistance) {
        WorldBorder original = player.getWorld().getWorldBorder();
        WorldBorder fakeBorder = Bukkit.createWorldBorder();
        fakeBorder.setSize(original.getSize());
        fakeBorder.setCenter(original.getCenter());
        fakeBorder.setDamageBuffer(original.getDamageBuffer());
        fakeBorder.setDamageAmount(original.getDamageAmount());
        fakeBorder.setWarningTime(original.getWarningTime());
        fakeBorder.setWarningDistance(warningDistance);
        player.setWorldBorder(fakeBorder);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (!Feature.BLOOD_SCREEN.isEnabled(player)) return;

        try {
            WorldBorder border = player.getWorld().getWorldBorder();
            double distance = border.getSize() / 2.0 - player.getLocation().distance(border.getCenter());
            int fakeDistance = (int) (distance * Feature.BLOOD_SCREEN.getDouble("interval"));

            FadingType mode = FadingType.valueOf(Feature.BLOOD_SCREEN.getString("mode"));
            if (mode == FadingType.DAMAGE) {
                fakeDistance = (int) (fakeDistance * event.getDamage());
            } else if (mode == FadingType.HEALTH) {
                fakeDistance *= Math.max((int) (Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue() - player.getHealth()), 1);
            }

            if (player.getVelocity().getY() == 0 && !Utils.hasCreativeGameMode(player)) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * 0.4;
                double offsetY = 1.0 + (RANDOM.nextDouble() - 0.5) * 0.5;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 2.0;
                player.getWorld().spawnParticle(Particle.BLOCK,
                        player.getLocation().add(offsetX, offsetY, offsetZ),
                        30, Material.REDSTONE_BLOCK.createBlockData());
            }

            plugin.getPlayers().put(player, Math.abs(fakeDistance));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in BloodScreenFeature.onEntityDamage", e);
        }
    }
}