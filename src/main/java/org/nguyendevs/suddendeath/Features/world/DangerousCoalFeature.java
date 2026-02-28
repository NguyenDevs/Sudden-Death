package org.nguyendevs.suddendeath.Features.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class DangerousCoalFeature extends AbstractFeature {

    private final Set<Location> activeFuses = new HashSet<>();

    @Override
    public String getName() {
        return "Dangerous Coal";
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!Feature.DANGEROUS_COAL.isEnabled(player) || Utils.hasCreativeGameMode(player)) {
            return;
        }

        if (block.getType() != Material.COAL_ORE && block.getType() != Material.DEEPSLATE_COAL_ORE) {
            return;
        }

        triggerCoal(block, 39);
    }

    private void triggerCoal(Block block, int fuseTicks) {
        if (SuddenDeath.getInstance().getClaimManager().isClaimed(block.getLocation())) {
            return;
        }

        Location baseLoc = block.getLocation();
        if (activeFuses.contains(baseLoc)) {
            return;
        }

        try {
            double chance = Feature.DANGEROUS_COAL.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() >= chance) {
                return;
            }

            activeFuses.add(baseLoc);
            double radius = Feature.DANGEROUS_COAL.getDouble("radius");
            block.getWorld().playSound(baseLoc, Sound.ENTITY_TNT_PRIMED, 2.0f, 1.0f);

            new BukkitRunnable() {
                double ticks = 0;

                @Override
                public void run() {
                    try {
                        ticks++;
                        block.getWorld().spawnParticle(Particle.LARGE_SMOKE, baseLoc.clone().add(0.5, 0, 0.5), 0);
                        if (ticks > fuseTicks) {
                            activeFuses.remove(baseLoc);

                            // Chain reaction check
                            int r = (int) Math.ceil(radius);
                            for (int x = -r; x <= r; x++) {
                                for (int y = -r; y <= r; y++) {
                                    for (int z = -r; z <= r; z++) {
                                        if (x == 0 && y == 0 && z == 0)
                                            continue;
                                        Block nearby = block.getRelative(x, y, z);
                                        if (nearby.getLocation().distance(baseLoc) <= radius) {
                                            if (nearby.getType() == Material.COAL_ORE
                                                    || nearby.getType() == Material.DEEPSLATE_COAL_ORE) {
                                                triggerCoal(nearby, 20);
                                            }
                                        }
                                    }
                                }
                            }

                            block.setType(Material.AIR);
                            block.getWorld().createExplosion(baseLoc, (float) radius);
                            cancel();
                        }
                    } catch (Exception e) {
                        activeFuses.remove(baseLoc);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Dangerous Coal feature", e);
        }
    }
}