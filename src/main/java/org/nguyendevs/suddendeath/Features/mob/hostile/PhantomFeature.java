package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PhantomFeature extends AbstractFeature {

    private NamespacedKey phantomVanishKey;
    private NamespacedKey phantomOffsetKey;

    @Override
    public String getName() {
        return "Phantom Blade";
    }

    @Override
    protected void onEnable() {
        this.phantomVanishKey = new NamespacedKey(plugin, "phantom_vanish_time");
        this.phantomOffsetKey = new NamespacedKey(plugin, "phantom_time_offset");

        BukkitRunnable phantomLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.PHANTOM_BLADE.isEnabled(world)) {
                            for (Phantom phantom : world.getEntitiesByClass(Phantom.class)) {
                                if (phantom.getTarget() instanceof Player) {
                                    applyPhantomBlade(phantom);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Phantom Blade task", e);
                }
            }
        };
        registerTask(phantomLoop.runTaskTimer(plugin, 0L, 20L));

        BukkitRunnable hoverLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.PHANTOM_BLADE.isEnabled(world)) {
                            for (Phantom phantom : world.getEntitiesByClass(Phantom.class)) {
                                if (phantom.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                    handlePhantomHover(phantom);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        };
        registerTask(hoverLoop.runTaskTimer(plugin, 0, 2));
    }

    private void handlePhantomHover(Phantom phantom) {
        if (phantom.getTarget() instanceof Player target) {
            Location ghostLoc = phantom.getLocation();
            Location targetLoc = target.getLocation();
            double desiredY = targetLoc.getY() + 12;
            Vector velocity = phantom.getVelocity();
            velocity.setX(velocity.getX() * 0.05);
            velocity.setZ(velocity.getZ() * 0.05);
            if (ghostLoc.getY() < desiredY) {
                velocity.setY(0.25);
            } else {
                velocity.setY(0.04);
            }
            phantom.setVelocity(velocity);
        }
    }

    private void applyPhantomBlade(Phantom phantom) {
        if (phantom == null || phantom.getHealth() <= 0 || !(phantom.getTarget() instanceof Player target)) return;

        try {
            if (!target.getWorld().equals(phantom.getWorld())) return;

            PersistentDataContainer data = phantom.getPersistentDataContainer();
            long now = System.currentTimeMillis();

            if (!data.has(phantomOffsetKey, PersistentDataType.LONG)) {
                long randomOffset = (long) (RANDOM.nextDouble() * 5000);
                data.set(phantomOffsetKey, PersistentDataType.LONG, randomOffset);
            }
            long timeOffset = data.get(phantomOffsetKey, PersistentDataType.LONG);

            double intervalMillis = Feature.PHANTOM_BLADE.getDouble("invisibility-interval") * 1000;
            double durationTicks = Feature.PHANTOM_BLADE.getDouble("invisibility-duration") * 20;
            long lastVanish = data.has(phantomVanishKey, PersistentDataType.LONG) ? data.get(phantomVanishKey, PersistentDataType.LONG) : 0L;

            if ((now + timeOffset) - lastVanish > intervalMillis + (durationTicks * 50)) {
                phantom.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) durationTicks, 0, false, false));
                phantom.getWorld().playSound(phantom.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 2.0f, 0.5f);
                phantom.getWorld().spawnParticle(Particle.CLOUD, phantom.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
                data.set(phantomVanishKey, PersistentDataType.LONG, now + timeOffset);
            }

            if (phantom.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                double chance = Feature.PHANTOM_BLADE.getDouble("shoot-chance") / 100.0;
                if (RANDOM.nextDouble() <= chance) {
                    shootWindBlade(phantom, target);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Phantom Blade", e);
        }
    }

    private void shootWindBlade(Phantom phantom, Player target) {
        try {
            Location startLoc = phantom.getLocation().add(0, 0.5, 0);
            Location targetLoc = target.getLocation().add(0, 1.2, 0);
            final Vector fixedTarget = targetLoc.toVector();
            Vector direction = fixedTarget.subtract(startLoc.toVector()).normalize();

            double damage = Feature.PHANTOM_BLADE.getDouble("damage");
            int weaknessDuration = (int) (Feature.PHANTOM_BLADE.getDouble("weakness-duration") * 20);
            int amplifier = (int) Feature.PHANTOM_BLADE.getDouble("weakness-amplifier");
            double speed = 1.4;

            phantom.getWorld().playSound(startLoc, Sound.ENTITY_BREEZE_IDLE_AIR, 2.0f, 0.8f);
            phantom.getWorld().playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, 2.0f, 1.5f);

            Vector forward = direction.clone();
            Vector up = new Vector(0, 1, 0);
            Vector right = forward.clone().crossProduct(up).normalize();
            if (right.lengthSquared() < 0.01) right = new Vector(1, 0, 0);

            List<Vector> bladeShape = new ArrayList<>();
            for (double t = 0; t <= 1.0; t += 0.05) {
                double x = (1 - t) * 0.15;
                double z = t * 0.3;
                bladeShape.add(right.clone().multiply(x).add(forward.clone().multiply(z)));
                bladeShape.add(right.clone().multiply(-x).add(forward.clone().multiply(z)));
            }

            new BukkitRunnable() {
                int ticks = 0;
                final Location currentLoc = startLoc.clone();
                final Vector velocity = direction.multiply(speed);

                @Override
                public void run() {
                    try {
                        if (ticks > 40 || currentLoc.getWorld() == null) {
                            cancel();
                            return;
                        }
                        currentLoc.add(velocity);
                        for (Vector offset : bladeShape) {
                            Location pLoc = currentLoc.clone().add(offset);
                            currentLoc.getWorld().spawnParticle(Particle.REDSTONE, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(180, 180, 180), 0.25f));
                        }
                        currentLoc.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, currentLoc.clone().subtract(velocity.clone().multiply(0.5)), 3, 0.2, 0.2, 0.2, 0.05);

                        for (Entity entity : currentLoc.getWorld().getNearbyEntities(currentLoc, 2.0, 2.0, 2.0)) {
                            if (entity instanceof Player hitPlayer) {
                                if (Utils.hasCreativeGameMode(hitPlayer)) continue;
                                if (hitPlayer.getUniqueId().equals(phantom.getUniqueId())) continue;
                                Utils.damage(hitPlayer, damage, true);
                                hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, amplifier));

                                hitPlayer.getWorld().playSound(hitPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.8f);
                                hitPlayer.getWorld().spawnParticle(Particle.SWEEP_ATTACK, hitPlayer.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0);
                                try {
                                    Particle windParticle = Particle.valueOf("GUST");

                                    hitPlayer.getWorld().spawnParticle(windParticle, hitPlayer.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0.1);

                                    // hitPlayer.getWorld().playSound(hitPlayer.getLocation(), Sound.valueOf("ENTITY_WIND_CHARGE_WIND_BURST"), 1.0f, 1.0f);
                                } catch (IllegalArgumentException | NullPointerException ignored) {
                                }

                                cancel();
                                return;
                            }
                        }
                        if (currentLoc.getBlock().getType().isSolid()) {
                            currentLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK, currentLoc, 15, 0.3, 0.3, 0.3, currentLoc.getBlock().getBlockData());
                            currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.0f);
                            cancel();
                        }
                        ticks++;
                    } catch (Exception e) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error shooting Wind Blade", e);
        }
    }
}