package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class UndeadGunnersFeature extends AbstractFeature {

    private static final double TICK_INTERVAL = 0.5;
    private static final int MAX_TICKS = 20;

    @Override
    public String getName() {
        return "Undead Gunners";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.UNDEAD_GUNNERS.isEnabled(world)) {
                            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                                if (zombie.getTarget() instanceof Player && isUndeadGunner(zombie)) {
                                    shootRocket(zombie);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Undead Gunners loop", e);
                }
            }
        }.runTaskTimer(plugin, 20L, 60L));
    }

    private boolean isUndeadGunner(Zombie zombie) {
        return zombie.getCustomName() != null && zombie.getCustomName().equalsIgnoreCase("Undead Gunner");
    }

    private void shootRocket(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0 || zombie.getTarget() == null || !(zombie.getTarget() instanceof Player target)) return;
        try {
            if (!target.getWorld().equals(zombie.getWorld())) return;

            double damage = Feature.UNDEAD_GUNNERS.getDouble("damage");
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.0f);

            Vector direction = target.getLocation().add(0, 0.5, 0).toVector()
                    .subtract(zombie.getLocation().add(0, 0.75, 0).toVector())
                    .normalize().multiply(TICK_INTERVAL);
            Location loc = zombie.getEyeLocation().clone();

            new BukkitRunnable() {
                double ticks = 0;
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < 2; j++) {
                            ticks += TICK_INTERVAL;
                            loc.add(direction);
                            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 4, 0.1, 0.1, 0.1, 0);
                            for (Player player : zombie.getWorld().getPlayers()) {
                                if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < 2.3 * 2.3) {
                                    loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 0);
                                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                                    player.damage(damage);

                                    double blockDmg = Feature.UNDEAD_GUNNERS.getDouble("block-damage");
                                    if (blockDmg > 0) {
                                        zombie.getWorld().createExplosion(zombie.getLocation(), (float) blockDmg);
                                    }
                                    cancel();
                                    return;
                                }
                            }
                        }
                        if (ticks > MAX_TICKS) cancel();
                    } catch (Exception e) { cancel(); }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Zombie loop", e);
        }
    }
}