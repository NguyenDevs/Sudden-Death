package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class EverburningBlazesFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Everburning Blazes";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (!Feature.EVERBURNING_BLAZES.isEnabled(world)) continue;
                        for (Blaze blaze : world.getEntitiesByClass(Blaze.class)) {
                            if (blaze.getTarget() instanceof Player) {
                                applyEverburning(blaze);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in EverburningBlazes task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L));
    }

    private void applyEverburning(Blaze blaze) {
        if (blaze == null || blaze.getHealth() <= 0) return;
        try {
            for (Entity entity : blaze.getNearbyEntities(10, 10, 10)) {
                if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player) || !blaze.hasLineOfSight(player)) continue;

                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 2f);
                double duration = Feature.EVERBURNING_BLAZES.getDouble("burn-duration") * 20;
                player.setFireTicks((int) duration);

                Location playerLoc = player.getLocation().add(0.0D, 0.75D, 0.0D);
                Location blazeLoc = blaze.getLocation().add(0.0D, 1.0D, 0.0D);
                Vector direction = playerLoc.toVector().subtract(blazeLoc.toVector());

                for (double j = 0.0D; j <= 1.0D; j += 0.04D) {
                    Location particleLoc = blazeLoc.clone().add(direction.clone().multiply(j));
                    if (particleLoc.getWorld() != null) {
                        particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 8, 0.2D, 0.2D, 0.2D, 0.0D);
                        particleLoc.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 8, 0.2D, 0.2D, 0.2D, 0.0D);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in EverburningBlazes for entity: " + blaze.getUniqueId(), e);
        }
    }
}