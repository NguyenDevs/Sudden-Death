package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import java.util.logging.Level;

public class BreezeFeature extends AbstractFeature {

    private boolean isSupported;
    private String breezeTypeName = "BREEZE";
    private String windChargeTypeName = "WIND_CHARGE";

    @Override
    public String getName() {
        return "Breeze Dash";
    }

    @Override
    public void initialize(org.nguyendevs.suddendeath.SuddenDeath plugin) {
        try {
            EntityType.valueOf(breezeTypeName);
            this.isSupported = true;
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aBreeze feature disabled &c(requires 1.21+)&a."));
            this.isSupported = false;
            return;
        }
        super.initialize(plugin);
    }

    @Override
    protected void onEnable() {
        if (!isSupported) return;

        BukkitRunnable breezeLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (Feature.BREEZE_DASH.isEnabled(world)) {
                            for (Entity entity : world.getEntities()) {
                                if (entity.getType().name().equals(breezeTypeName)) {
                                    if (entity instanceof Mob) {
                                        Mob breeze = (Mob) entity;
                                        if (breeze.getTarget() instanceof Player) {
                                            applyBreezeDash(breeze, (Player) breeze.getTarget());
                                        }
                                    }
                                }
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

    private void applyBreezeDash(Mob breeze, Player target) {
        if (breeze == null || breeze.getHealth() <= 0 || !target.isOnline()) return;
        if (!target.getWorld().equals(breeze.getWorld())) return;

        try {
            double chance = Feature.BREEZE_DASH.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() <= chance) {
                double duration = Feature.BREEZE_DASH.getDouble("duration");
                int coefficient = (int) Feature.BREEZE_DASH.getDouble("amplifier");

                breeze.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) (duration * 20), coefficient - 1));
                breeze.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration * 20), coefficient - 1));
                breeze.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (duration * 20), coefficient - 1));
                breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR, 1.0f, 0.5f);
                breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.0f, 0.1f);

                breeze.setVelocity(breeze.getVelocity().setY(0));
                breeze.setGravity(true);

                new BukkitRunnable() {
                    int shots = 0;
                    @Override
                    public void run() {
                        try {
                            if (breeze.isDead() || !target.isOnline()) {
                                cancel();
                                return;
                            }
                            if (shots < Feature.BREEZE_DASH.getDouble("shoot-amount")) {
                                Location breezeLoc = breeze.getLocation();
                                Vector direction = target.getLocation().subtract(breezeLoc).toVector().normalize();

                                Entity windCharge = breeze.getWorld().spawnEntity(breezeLoc.add(0, 1, 0), EntityType.valueOf(windChargeTypeName));
                                windCharge.setVelocity(direction.multiply(1.5));

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (windCharge.isValid()) {
                                                Location chargeLoc = windCharge.getLocation();
                                                Vector velocity = windCharge.getVelocity().normalize();
                                                Location particleLoc = chargeLoc.clone().subtract(velocity.multiply(0.5));
                                                breeze.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, particleLoc, 1, 0, 0, 0, 0);
                                            } else {
                                                cancel();
                                            }
                                        } catch (Exception e) {
                                            cancel();
                                        }
                                    }
                                }.runTaskTimer(plugin, 0, 1);
                                shots++;
                            } else {
                                cancel();
                            }
                        } catch (Exception e) {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 10);

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        try {
                            if (breeze.isValid() && !breeze.isDead()) {
                                Location loc = breeze.getLocation().add(0, 0.5, 0);
                                Vector velocity = breeze.getVelocity().normalize();


                                if (velocity.lengthSquared() > 0.1 && ticks < duration * 20) {
                                    Location trailLoc = loc.subtract(velocity.multiply(0.5));
                                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(189, 235, 255), 1.0f);
                                    breeze.getWorld().spawnParticle(Particle.REDSTONE, trailLoc, 2, 0.1, 0.1, 0.1, 0, dustOptions);
                                }
                                ticks++;
                                if (ticks >= duration * 20) {
                                    breeze.setVelocity(breeze.getVelocity().setY(0.1));
                                    breeze.setGravity(true);
                                    cancel();
                                }
                            } else {
                                cancel();
                            }
                        } catch (Exception e) {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Breeze Dash", e);
        }
    }
}