package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.logging.Level;

public class BreezeFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Breeze Dash";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable breezeLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (!Feature.BREEZE_DASH.isEnabled(world)) continue;
                        for (Entity entity : world.getEntities()) {
                            if (!(entity instanceof Breeze breeze)) continue;
                            if (breeze.getTarget() instanceof Player target) {
                                applyBreezeDash(breeze, target);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Breeze Dash task", e);
                }
            }
        };
        registerTask(breezeLoop.runTaskTimer(plugin, 0L, 60L));
    }

    private void applyBreezeDash(Breeze breeze, Player target) {
        if (breeze.getHealth() <= 0 || !target.isOnline()) return;
        if (!target.getWorld().equals(breeze.getWorld())) return;

        try {
            double chance = Feature.BREEZE_DASH.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            double duration = Feature.BREEZE_DASH.getDouble("duration");
            int amplifier = (int) Feature.BREEZE_DASH.getDouble("amplifier") - 1;
            int durationTicks = (int) (duration * 20);

            breeze.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, amplifier));
            breeze.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, amplifier));
            breeze.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, amplifier));

            breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR, 1.0f, 0.5f);
            breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.0f, 0.1f);

            breeze.setVelocity(breeze.getVelocity().setY(0));
            breeze.setGravity(true);

            startShootTask(breeze, target, duration);
            startTrailTask(breeze, duration);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Breeze Dash", e);
        }
    }

    private void startShootTask(Breeze breeze, Player target, double duration) {
        int maxShots = (int) Feature.BREEZE_DASH.getDouble("shoot-amount");

        new BukkitRunnable() {
            int shots = 0;

            @Override
            public void run() {
                if (breeze.isDead() || !target.isOnline()) { cancel(); return; }
                if (shots >= maxShots) { cancel(); return; }

                Location origin = breeze.getLocation().add(0, 1, 0);
                Vector direction = target.getLocation().subtract(origin).toVector().normalize();
                WindCharge windCharge = breeze.getWorld().spawn(origin, WindCharge.class);
                windCharge.setVelocity(direction.multiply(1.5));

                startWindChargeTrailTask(breeze, windCharge);
                shots++;
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    private void startWindChargeTrailTask(Breeze breeze, WindCharge windCharge) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!windCharge.isValid()) { cancel(); return; }
                Vector velocity = windCharge.getVelocity().normalize();
                Location particleLoc = windCharge.getLocation().subtract(velocity.multiply(0.5));
                breeze.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void startTrailTask(Breeze breeze, double duration) {
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(189, 235, 255), 1.0f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!breeze.isValid() || breeze.isDead()) { cancel(); return; }

                if (ticks >= duration * 20) {
                    breeze.setVelocity(breeze.getVelocity().setY(0.1));
                    breeze.setGravity(true);
                    cancel();
                    return;
                }

                Vector velocity = breeze.getVelocity().normalize();
                if (velocity.lengthSquared() > 0.1) {
                    Location trailLoc = breeze.getLocation().add(0, 0.5, 0).subtract(velocity.multiply(0.5));
                    breeze.getWorld().spawnParticle(Particle.DUST, trailLoc, 2, 0.1, 0.1, 0.1, 0, dust);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}