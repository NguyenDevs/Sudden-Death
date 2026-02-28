package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class WitherRushFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Wither Rush";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (!Feature.WITHER_RUSH.isEnabled(world)) continue;
                        for (WitherSkeleton ws : world.getEntitiesByClass(WitherSkeleton.class)) {
                            if (ws.getTarget() instanceof Player player) {
                                if (!Feature.WITHER_MACHINEGUN.isEnabled(ws) || Math.random() >= 0.5) {
                                    performRush(ws, player);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in WitherRush", e);
                }
            }
        }.runTaskTimer(plugin, 20L, 120L));
    }

    private void performRush(Creature witherSkeleton, Player target) {
        double damage = Feature.WITHER_RUSH.getDouble("damage");

        witherSkeleton.getWorld().playSound(witherSkeleton.getLocation(), Sound.ENTITY_WITHER_SPAWN, 4.0f, 2.0f);
        witherSkeleton.removePotionEffect(PotionEffectType.SLOWNESS);
        witherSkeleton.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 255));

        new BukkitRunnable() {
            double ticks = 0;

            @Override
            public void run() {
                if (witherSkeleton.isDead() || witherSkeleton.getHealth() <= 0) {
                    cancel();
                    return;
                }

                ticks += Math.PI / 20;
                Location loc = witherSkeleton.getLocation();

                for (int j = 0; j < 2; j++) {
                    loc.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                            loc.clone().add(Math.cos(j * Math.PI + ticks), 2.2, Math.sin(j * Math.PI + ticks)), 0);
                }

                if (ticks >= Math.PI) {
                    Location start = witherSkeleton.getLocation().add(0, 1, 0);
                    Vector direction = target.getLocation().add(0, 1, 0).toVector()
                            .subtract(start.toVector());

                    for (double j = 0; j < 1; j += 0.03) {
                        start.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                                start.clone().add(direction.getX() * j, direction.getY() * j, direction.getZ() * j), 0);
                    }

                    witherSkeleton.getWorld().playSound(witherSkeleton.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.0f);
                    witherSkeleton.teleport(target.getLocation());
                    Utils.damage(target, damage, true);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}