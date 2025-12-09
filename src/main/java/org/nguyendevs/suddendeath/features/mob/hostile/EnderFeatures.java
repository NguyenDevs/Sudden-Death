package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.logging.Level;

public class EnderFeatures extends AbstractFeature {

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
            if (RANDOM.nextDouble() <= chance) {
                double duration = Feature.ENDER_POWER.getDouble("duration");
                spawnEnderParticles(player);
                event.getEntity().getWorld().playSound(event.getEntity().getLocation(),
                        Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 2.0f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                        (int) (duration * 20), 0));
                Location loc = player.getLocation();
                loc.setYaw(player.getEyeLocation().getYaw() - 180);
                loc.setPitch(player.getEyeLocation().getPitch());
                player.teleport(loc);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in EnderFeatures.onEntityDamageByEntity", e);
        }
    }

    private boolean isEnderEntity(Entity entity) {
        return entity instanceof Enderman || entity.getType().name().equalsIgnoreCase("SHULKER") ||
                entity instanceof Endermite || entity instanceof EnderDragon;
    }

    private void spawnEnderParticles(Player player) {
        new BukkitRunnable() {
            double y = 0;

            @Override
            public void run() {
                try {
                    for (int j1 = 0; j1 < 3; j1++) {
                        y += 0.07;
                        int particleCount = 3;
                        for (int j = 0; j < particleCount; j++) {
                            player.getWorld().spawnParticle(Particle.REDSTONE,
                                    player.getLocation().clone().add(
                                            Math.cos(y * Math.PI + (j * Math.PI * 2 / particleCount)) * (3 - y) / 2.5,
                                            y,
                                            Math.sin(y * Math.PI + (j * Math.PI * 2 / particleCount)) * (3 - y) / 2.5),
                                    0, new Particle.DustOptions(Color.BLACK, 1));
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