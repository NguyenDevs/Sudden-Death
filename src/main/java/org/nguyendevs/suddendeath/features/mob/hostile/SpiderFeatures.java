package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.listener.Loops;
import org.nguyendevs.suddendeath.util.*;
import java.util.logging.Level;

public class SpiderFeatures extends AbstractFeature {

    @Override
    public String getName() {
        return "Spider Features (Angry/Leaping + Cave Spider Web)";
    }

    @Override
    protected void onEnable() {
        // Spider loop for Angry/Leaping
        BukkitRunnable spiderLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ANGRY_SPIDERS.isEnabled(world) || Feature.LEAPING_SPIDERS.isEnabled(world)) {
                            for (Spider spider : world.getEntitiesByClass(Spider.class)) {
                                if (spider.getTarget() instanceof Player) {
                                    Loops.loop3s_spider(spider);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Spider loop task", e);
                }
            }
        };
        spiderLoop.runTaskTimer(plugin, 0L, 40L);
        registerTask((BukkitTask) spiderLoop);

        // Cave Spider Web loop
        BukkitRunnable webLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.SPIDER_WEB.isEnabled(world)) {
                            for (CaveSpider caveSpider : world.getEntitiesByClass(CaveSpider.class)) {
                                if (caveSpider.getTarget() instanceof Player) {
                                    applyCaveSpiderWeb(caveSpider);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Cave Spider Web task", e);
                }
            }
        };
        webLoop.runTaskTimer(plugin, 0L, 40L);
        registerTask((BukkitTask) webLoop);
    }

    private void applyCaveSpiderWeb(CaveSpider spider) {
        if (spider == null || spider.getHealth() <= 0 || !(spider.getTarget() instanceof Player target)) return;
        if (!target.getWorld().equals(spider.getWorld())) return;

        try {
            double chance = Feature.SPIDER_WEB.getDouble("chance-percent") / 100.0;
            int shootAmount = (int) Feature.SPIDER_WEB.getDouble("amount-per-shoot");

            if (RANDOM.nextDouble() <= chance) {
                if (shootAmount <= 2) {
                    shootSingleWeb(spider, target, shootAmount);
                } else {
                    shootContinuousWebs(spider, target, shootAmount);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Cave Spider Web", e);
        }
    }

    private void shootSingleWeb(CaveSpider spider, Player target, int amount) {
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
        NoInteractItemEntity item = new NoInteractItemEntity(
                spider.getLocation().add(0, 1, 0), new ItemStack(Material.COBWEB));
        item.getEntity().setVelocity(
                target.getLocation().add(0, 1, 0).subtract(spider.getLocation().add(0, 1, 0))
                        .toVector().normalize().multiply(1.0));

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
                    for (Entity entity : item.getEntity().getNearbyEntities(1, 1, 1)) {
                        if (entity instanceof Player hitPlayer) {
                            item.close();
                            placeCobwebsAroundPlayer(hitPlayer, amount);
                            cancel();
                            return;
                        }
                    }
                } catch (Exception e) {
                    item.close();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void shootContinuousWebs(CaveSpider spider, Player target, int totalAmount) {
        new BukkitRunnable() {
            int shotsFired = 0;

            @Override
            public void run() {
                try {
                    if (shotsFired >= totalAmount || spider.isDead() || !target.isOnline()) {
                        cancel();
                        return;
                    }

                    spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
                    NoInteractItemEntity item = new NoInteractItemEntity(
                            spider.getLocation().add(0, 1, 0), new ItemStack(Material.COBWEB));
                    item.getEntity().setVelocity(
                            target.getLocation().add(0, 1, 0).subtract(spider.getLocation().add(0, 1, 0))
                                    .toVector().normalize().multiply(1.0));

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
                                for (Entity entity : item.getEntity().getNearbyEntities(1, 1, 1)) {
                                    if (entity instanceof Player hitPlayer) {
                                        item.close();
                                        placeCobwebsAroundPlayer(hitPlayer, 1);
                                        shotsFired++;
                                        cancel();
                                        return;
                                    }
                                }
                            } catch (Exception e) {
                                item.close();
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0, 1);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error shooting continuous web", e);
                }
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    private void placeCobwebsAroundPlayer(Player player, int amount) {
        Location center = player.getLocation();
        int placed = 0;

        int[][] offsets = {
                {-1, -1, -1}, {-1, -1, 0}, {-1, -1, 1},
                {-1, 0, -1},  {-1, 0, 0},  {-1, 0, 1},
                {-1, 1, -1},  {-1, 1, 0},  {-1, 1, 1},
                {0, -1, -1},  {0, -1, 0},  {0, -1, 1},
                {0, 0, -1},              {0, 0, 1},
                {0, 1, -1},   {0, 1, 0},   {0, 1, 1},
                {1, -1, -1},  {1, -1, 0},  {1, -1, 1},
                {1, 0, -1},   {1, 0, 0},   {1, 0, 1},
                {1, 1, -1},   {1, 1, 0},   {1, 1, 1}
        };

        for (int i = offsets.length - 1; i > 0; i--) {
            int index = RANDOM.nextInt(i + 1);
            int[] temp = offsets[index];
            offsets[index] = offsets[i];
            offsets[i] = temp;
        }

        for (int[] offset : offsets) {
            if (placed >= amount) break;
            Location loc = center.clone().add(offset[0], offset[1], offset[2]);
            if (loc.getBlock().getType() == Material.AIR) {
                loc.getBlock().setType(Material.COBWEB);
                loc.getWorld().playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 2.0f);
                placed++;
            }
        }

        player.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0,
                Material.COBWEB.createBlockData());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 2.0f);
    }
}