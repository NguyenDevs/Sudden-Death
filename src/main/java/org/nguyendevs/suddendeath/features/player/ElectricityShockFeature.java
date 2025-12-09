package org.nguyendevs.suddendeath.features.player;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.logging.Level;

public class ElectricityShockFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Electricity Shock";
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !hasMovedBlock(event)) return;
        if (!Feature.ELECTRICITY_SHOCK.isEnabled(player)) return;
        if (Utils.hasCreativeGameMode(player)) return;

        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) return;
            if (data.isOnCooldown(Feature.ELECTRICITY_SHOCK)) return;
            if (!isPoweredRedstoneBlock(player.getLocation().getBlock())) return;

            data.applyCooldown(Feature.ELECTRICITY_SHOCK, 3);
            applyElectricityShock(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in ElectricityShockFeature.onPlayerMove", e);
        }
    }

    private void applyElectricityShock(Player player) {
        player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 16, 0, 0, 0, 0.15);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 24, 0, 0, 0, 0.15);
        Utils.damage(player, Feature.ELECTRICITY_SHOCK.getDouble("damage"), true);

        new BukkitRunnable() {
            int ticksPassed = 0;
            @Override
            public void run() {
                try {
                    ticksPassed++;
                    if (ticksPassed > 15) {
                        cancel();
                        return;
                    }
                    player.playHurtAnimation(0.005f);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Error in ElectricityShockFeature task", e);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private boolean isPoweredRedstoneBlock(Block block) {
        if (!block.isBlockPowered()) return false;
        Material type = block.getType();
        return type == Material.REDSTONE_WIRE || type == Material.COMPARATOR ||
                type == Material.REPEATER || type == Material.REDSTONE_TORCH;
    }

    private boolean hasMovedBlock(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null &&
                (from.getBlockX() != to.getBlockX() ||
                        from.getBlockY() != to.getBlockY() ||
                        from.getBlockZ() != to.getBlockZ());
    }
}