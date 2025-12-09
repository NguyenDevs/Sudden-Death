package org.nguyendevs.suddendeath.features.world;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.logging.Level;

public class DangerousCoalFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Dangerous Coal";
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!Feature.DANGEROUS_COAL.isEnabled(player) || Utils.hasCreativeGameMode(player) ||
                block.getType() != Material.COAL_ORE) {
            return;
        }

        try {
            double chance = Feature.DANGEROUS_COAL.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() >= chance) {
                return;
            }

            double radius = Feature.DANGEROUS_COAL.getDouble("radius");
            block.getWorld().playSound(block.getLocation(), Sound.ENTITY_TNT_PRIMED, 2.0f, 1.0f);

            new BukkitRunnable() {
                double ticks = 0;
                @Override
                public void run() {
                    try {
                        ticks++;
                        block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(0.5, 0, 0.5), 0);
                        if (ticks > 39) {
                            block.getWorld().createExplosion(block.getLocation(), (float) radius);
                            cancel();
                        }
                    } catch (Exception e) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Dangerous Coal feature", e);
        }
    }
}