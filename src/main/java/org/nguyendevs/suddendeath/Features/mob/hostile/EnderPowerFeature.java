package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class EnderPowerFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Ender Power";
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isEnderEntity(event.getEntity())) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (Utils.hasCreativeGameMode(player)) return;
        if (!Feature.ENDER_POWER.isEnabled(event.getEntity())) return;

        try {
            double chance = Feature.ENDER_POWER.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            double duration = Feature.ENDER_POWER.getDouble("duration");

            spawnEnderParticles(player);
            event.getEntity().getWorld().playSound(
                    event.getEntity().getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 2.0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (duration * 20), 0));

            Location loc = player.getLocation().clone();
            loc.setYaw(player.getEyeLocation().getYaw() - 180);
            loc.setPitch(player.getEyeLocation().getPitch());
            player.teleport(loc);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in EnderFeatures.onEntityDamageByEntity", e);
        }
    }

    private boolean isEnderEntity(Entity entity) {
        return entity instanceof Enderman
                || entity instanceof Shulker
                || entity instanceof Endermite
                || entity instanceof EnderDragon;
    }

    private void spawnEnderParticles(Player player) {
        Particle.DustOptions blackDust = new Particle.DustOptions(Color.BLACK, 1);

        new BukkitRunnable() {
            double y = 0;

            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {
                        y += 0.07;
                        for (int j = 0; j < 3; j++) {
                            double angle = y * Math.PI + (j * Math.PI * 2.0 / 3);
                            double radius = (3 - y) / 2.5;
                            Location loc = player.getLocation().clone().add(
                                    Math.cos(angle) * radius,
                                    y,
                                    Math.sin(angle) * radius);
                            player.getWorld().spawnParticle(Particle.DUST, loc, 0, blackDust);
                        }
                    }
                    if (y > 3) cancel();
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}