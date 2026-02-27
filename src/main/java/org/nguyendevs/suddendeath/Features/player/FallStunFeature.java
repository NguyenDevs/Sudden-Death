package org.nguyendevs.suddendeath.Features.player;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;


public class FallStunFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Fall Stun";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (!Feature.FALL_STUN.isEnabled(player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        try {
            player.removePotionEffect(PotionEffectType.SLOW);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
                    (int) (event.getDamage() * 10 * Feature.FALL_STUN.getDouble("duration-amplifier")), 2));

            Location loc = player.getLocation().clone();
            new BukkitRunnable() {
                double ticks = 0;

                @Override
                public void run() {
                    try {
                        ticks += 0.25;
                        for (double j = 0; j < Math.PI * 2; j += Math.PI / 16) {
                            Location particleLoc = loc.clone().add(
                                    Math.cos(j) * ticks, 0.1, Math.sin(j) * ticks);
                            particleLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                                    particleLoc, 0, Material.DIRT.createBlockData());
                        }
                        loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 2.0f, 2.0f);
                        if (ticks >= 2) {
                            cancel();
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Error in FallStunFeature particle task", e);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in FallStunFeature.onEntityDamage", e);
        }
    }
}