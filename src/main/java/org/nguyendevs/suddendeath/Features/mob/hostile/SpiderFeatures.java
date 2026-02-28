package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.NoInteractItemEntity;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class SpiderFeatures extends AbstractFeature {

    private static final int[][] OFFSETS = {
            {-1,-1,-1},{-1,-1,0},{-1,-1,1},{-1,0,-1},{-1,0,0},{-1,0,1},{-1,1,-1},{-1,1,0},{-1,1,1},
            {0,-1,-1},{0,-1,0},{0,-1,1},{0,0,-1},{0,0,1},{0,1,-1},{0,1,0},{0,1,1},
            {1,-1,-1},{1,-1,0},{1,-1,1},{1,0,-1},{1,0,0},{1,0,1},{1,1,-1},{1,1,0},{1,1,1}
    };

    @Override
    public String getName() {
        return "Spider Features";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (!Feature.ANGRY_SPIDERS.isEnabled(world) && !Feature.LEAPING_SPIDERS.isEnabled(world)) continue;
                        for (Spider spider : world.getEntitiesByClass(Spider.class)) {
                            if (spider.getTarget() instanceof Player) loop3s_spider(spider);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Spider loop task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L));

        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (!Feature.SPIDER_WEB.isEnabled(world)) continue;
                        for (CaveSpider caveSpider : world.getEntitiesByClass(CaveSpider.class)) {
                            if (caveSpider.getTarget() instanceof Player) applyCaveSpiderWeb(caveSpider);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Cave Spider Web task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L));
    }

    private void loop3s_spider(Spider spider) {
        if (spider == null || spider.getHealth() <= 0 || !(spider.getTarget() instanceof Player target)) return;
        if (!target.getWorld().equals(spider.getWorld())) return;

        try {
            if (Feature.ANGRY_SPIDERS.isEnabled(spider) && RANDOM.nextDouble() < 0.5) {
                double damage = Feature.ANGRY_SPIDERS.getDouble("damage");
                int duration = (int) (Feature.ANGRY_SPIDERS.getDouble("duration") * 20);
                int amplifier = (int) Feature.ANGRY_SPIDERS.getDouble("amplifier");

                spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
                shootWebProjectile(spider.getLocation().add(0, 1, 0), target, 0.4, item -> {
                    item.getEntity().getNearbyEntities(1, 1, 1).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .findFirst()
                            .ifPresent(hitPlayer -> {
                                item.close();
                                hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier));
                                Utils.damage(hitPlayer, damage, true);
                            });
                });
                return;
            }

            if (Feature.LEAPING_SPIDERS.isEnabled(spider)) {
                spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
                spider.getWorld().spawnParticle(Particle.POOF, spider.getLocation(), 8, 0, 0, 0, 0.1);
                Vector direction = target.getEyeLocation().toVector()
                        .subtract(spider.getLocation().toVector()).multiply(0.3).setY(0.3);
                spider.setVelocity(direction);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Spider loop for entity: " + spider.getUniqueId(), e);
        }
    }

    private void applyCaveSpiderWeb(CaveSpider spider) {
        if (spider == null || spider.getHealth() <= 0 || !(spider.getTarget() instanceof Player target)) return;
        if (!target.getWorld().equals(spider.getWorld())) return;

        try {
            double chance = Feature.SPIDER_WEB.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int shootAmount = (int) Feature.SPIDER_WEB.getDouble("amount-per-shoot");
            if (shootAmount <= 2) {
                shootSingleWeb(spider, target, shootAmount);
            } else {
                shootContinuousWebs(spider, target, shootAmount);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Cave Spider Web", e);
        }
    }

    private void shootSingleWeb(CaveSpider spider, Player target, int amount) {
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
        shootWebProjectile(spider.getLocation().add(0, 1, 0), target, 1.0, item -> {
            item.getEntity().getNearbyEntities(1, 1, 1).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .findFirst()
                    .ifPresent(hitPlayer -> {
                        item.close();
                        placeCobwebsAroundPlayer(hitPlayer, amount);
                    });
        });
    }

    private void shootContinuousWebs(CaveSpider spider, Player target, int totalAmount) {
        new BukkitRunnable() {
            int shotsFired = 0;

            @Override
            public void run() {
                if (shotsFired >= totalAmount || spider.isDead() || !target.isOnline()) {
                    cancel();
                    return;
                }
                try {
                    spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
                    shootWebProjectile(spider.getLocation().add(0, 1, 0), target, 1.0, item -> {
                        item.getEntity().getNearbyEntities(1, 1, 1).stream()
                                .filter(e -> e instanceof Player)
                                .map(e -> (Player) e)
                                .findFirst()
                                .ifPresent(hitPlayer -> {
                                    item.close();
                                    placeCobwebsAroundPlayer(hitPlayer, 1);
                                    shotsFired++;
                                });
                    });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error shooting continuous web", e);
                }
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    @FunctionalInterface
    private interface WebHitHandler {
        void onTick(NoInteractItemEntity item);
    }

    private void shootWebProjectile(Location origin, Player target, double speed, WebHitHandler onTick) {
        NoInteractItemEntity item = new NoInteractItemEntity(origin, new ItemStack(Material.COBWEB));
        item.getEntity().setVelocity(
                target.getLocation().add(0, 1, 0).subtract(origin).toVector().normalize().multiply(speed));

        new BukkitRunnable() {
            int ti = 0;

            @Override
            public void run() {
                try {
                    ti++;
                    if (ti > 20 || item.getEntity().isDead()) {
                        item.close();
                        cancel();
                        return;
                    }
                    item.getEntity().getWorld().spawnParticle(Particle.CRIT, item.getEntity().getLocation(), 0);
                    onTick.onTick(item);
                    if (item.getEntity().isDead()) cancel();
                } catch (Exception e) {
                    item.close();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void placeCobwebsAroundPlayer(Player player, int amount) {
        Location center = player.getLocation();
        int[] indices = new int[OFFSETS.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        for (int i = indices.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            int tmp = indices[j]; indices[j] = indices[i]; indices[i] = tmp;
        }

        int placed = 0;
        for (int idx : indices) {
            if (placed >= amount) break;
            int[] offset = OFFSETS[idx];
            Location loc = center.clone().add(offset[0], offset[1], offset[2]);
            if (loc.getBlock().getType() == Material.AIR) {
                loc.getBlock().setType(Material.COBWEB);
                loc.getWorld().playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 2.0f);
                placed++;
            }
        }

        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0, Material.COBWEB.createBlockData());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 2.0f);
    }
}