package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;

public class StrayFrostFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Stray Frost";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;

        try {
            if (event.getEntity() instanceof Player player && event.getDamager() instanceof Arrow arrow &&
                    Feature.STRAY_FROST.isEnabled(player) && arrow.getShooter() instanceof Stray) {
                applyStrayFrost(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in StrayFeature", e);
        }
    }

    private void applyStrayFrost(Player player) {
        try {
            double duration = Feature.STRAY_FROST.getDouble("duration");
            double chance = Feature.STRAY_FROST.getDouble("chance-percent") / 100.0;
            Location loc = player.getLocation();

            if (RANDOM.nextDouble() <= chance) {
                player.setFreezeTicks((int) (duration * 20 + 140));
                player.getWorld().playSound(loc, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 1.0f);

                new BukkitRunnable() {
                    double ticks = 0;
                    @Override
                    public void run() {
                        try {
                            if (ticks < 10) {
                                for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
                                    Location particleLoc = loc.clone().add(Math.cos(i) * 0.5, 1.0 + (Math.sin(ticks * 0.1) * 0.2), Math.sin(i) * 0.5);
                                    particleLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                                }
                                ticks++;
                            } else {
                                cancel();
                            }
                        } catch (Exception e) {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 2);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Stray Frost", e);
        }
    }
}