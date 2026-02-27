package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.NoInteractItemEntity;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.UUID;
import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class WitherSkeletonFeature extends AbstractFeature {

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
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (!Feature.WITHER_RUSH.isEnabled(world)) continue;
                        for (WitherSkeleton witherSkeleton : world.getEntitiesByClass(WitherSkeleton.class)) {
                            if (witherSkeleton.getTarget() instanceof Player) {
                                loop6s_wither_skeleton(witherSkeleton);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in WitherSkeleton loop", e);
                }
            }
        }.runTaskTimer(plugin, 20L, 120L));
    }

    private void loop6s_wither_skeleton(Creature witherSkeleton) {
        if (witherSkeleton == null || witherSkeleton.getHealth() <= 0 || !(witherSkeleton.getTarget() instanceof Player target)) return;
        try {
            if (!target.getWorld().equals(witherSkeleton.getWorld())) return;

            if (Feature.WITHER_MACHINEGUN.isEnabled(witherSkeleton) && RANDOM.nextDouble() < 0.5) {
                double damage = Feature.WITHER_MACHINEGUN.getDouble("damage");
                launchWitherMachineGun(witherSkeleton, target, damage);
            } else {
                double damage = Feature.WITHER_RUSH.getDouble("damage");
                applyWitherRush(witherSkeleton, target, damage);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in WitherSkeleton loop for entity: " + witherSkeleton.getUniqueId(), e);
        }
    }

    private void launchWitherMachineGun(Creature witherSkeleton, Player target, double damage) {
        try {
            for (int delay = 0; delay < 12; delay += 3) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0F, 2.0F);
                            ItemStack stack = new ItemStack(Material.COAL);
                            ItemMeta meta = stack.getItemMeta();
                            if (meta != null) {
                                meta.setDisplayName("SUDDEN_DEATH:" + UUID.randomUUID().toString());
                                stack.setItemMeta(meta);
                            }
                            NoInteractItemEntity item = new NoInteractItemEntity(witherSkeleton.getLocation().add(0, 1, 0), stack);
                            item.getEntity().setVelocity(target.getLocation().add(0, 2, 0).toVector().subtract(witherSkeleton.getLocation().add(0, 1, 0).toVector()).normalize().multiply(2));

                            new BukkitRunnable() {
                                double ticks = 0;
                                @Override
                                public void run() {
                                    try {
                                        ticks++;
                                        Item entityItem = item.getEntity();
                                        if (ticks >= 20 || entityItem.isDead()) { item.close(); cancel(); return; }
                                        entityItem.getWorld().spawnParticle(Particle.SMOKE_NORMAL, entityItem.getLocation(), 0);
                                        for (Entity nearby : entityItem.getNearbyEntities(1.3, 1.3, 1.3)) {
                                            if (nearby instanceof Player player) {
                                                item.close();
                                                Utils.damage(player, damage, true);
                                                cancel();
                                                return;
                                            }
                                        }
                                    } catch (Exception e) { item.close(); cancel(); }
                                }
                            }.runTaskTimer(plugin, 0, 1);
                        } catch (Exception e) {}
                    }
                }.runTaskLater(plugin, delay);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in WitherSkeleton machine gun for entity: " + witherSkeleton.getUniqueId(), e);
        }
    }

    private void applyWitherRush(Creature witherSkeleton, Player target, double damage) {
        try {
            witherSkeleton.getWorld().playSound(witherSkeleton.getLocation(), Sound.ENTITY_WITHER_SPAWN, 4.0f, 2.0f);
            witherSkeleton.removePotionEffect(PotionEffectType.SLOW);
            witherSkeleton.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 255));

            new BukkitRunnable() {
                double ticks = 0;
                @Override
                public void run() {
                    try {
                        if (witherSkeleton.getHealth() <= 0) { cancel(); return; }
                        ticks += Math.PI / 20;
                        Location loc = witherSkeleton.getLocation();
                        for (int j = 0; j < 2; j++) {
                            Location circleLoc = loc.clone().add(Math.cos(j * Math.PI + ticks), 2.2, Math.sin(j * Math.PI + ticks));
                            circleLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, circleLoc, 0);
                        }
                        if (ticks >= Math.PI) {
                            Location start = witherSkeleton.getLocation().add(0, 1, 0);
                            Vector direction = target.getLocation().add(0, 1, 0).toVector().subtract(start.toVector());
                            for (double j = 0; j < 1; j += 0.03) {
                                Location point = start.clone().add(direction.getX() * j, direction.getY() * j, direction.getZ() * j);
                                point.getWorld().spawnParticle(Particle.SMOKE_LARGE, point, 0);
                            }
                            witherSkeleton.getWorld().playSound(witherSkeleton.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.0f);
                            witherSkeleton.teleport(target);
                            Utils.damage(target, damage, true);
                            cancel();
                        }
                    } catch (Exception e) { cancel(); }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying WitherSkeleton rush for entity: " + witherSkeleton.getUniqueId(), e);
        }
    }
}