package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.listener.Loops;
import java.util.logging.Level;

public class WitchFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Witch Scrolls";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable witchLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.WITCH_SCROLLS.isEnabled(world)) {
                            world.getEntitiesByClass(Witch.class).forEach(Loops::loop4s_witch);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Witch loop", e);
                }
            }
        };
        witchLoop.runTaskTimer(plugin, 0L, 80L);
        registerTask((BukkitTask) witchLoop);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Witch witch)) return;
        if (event.getDamage() <= 0 || witch.hasMetadata("NPC")) return;
        if (!Feature.WITCH_SCROLLS.isEnabled(witch)) return;

        try {
            double chance = Feature.WITCH_SCROLLS.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() <= chance) {
                event.setCancelled(true);
                applyWitchScrollsEffect(witch);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in WitchFeature.onEntityDamage", e);
        }
    }

    private void applyWitchScrollsEffect(Witch witch) {
        witch.getWorld().playSound(witch.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
        Location loc = witch.getLocation();
        double radius = 1.5;

        new BukkitRunnable() {
            double step = 0;

            @Override
            public void run() {
                try {
                    for (double j = 0; j < 3; j++) {
                        step += Math.PI / 20;
                        for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                            Location particleLoc = loc.clone().add(
                                    radius * Math.cos(i) * Math.sin(step),
                                    radius * (1 + Math.cos(step)),
                                    radius * Math.sin(i) * Math.sin(step));
                            particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 0,
                                    new Particle.DustOptions(Color.WHITE, 1));
                        }
                    }
                    if (step >= Math.PI * 2) cancel();
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}