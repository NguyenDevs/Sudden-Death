package org.nguyendevs.suddendeath.Features.mob.hostile;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.NoInteractItemEntity;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.UUID;
import java.util.logging.Level;

public class WitherMachineGunFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Wither MachineGun";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (!Feature.WITHER_MACHINEGUN.isEnabled(world))
                            continue;
                        for (WitherSkeleton ws : world.getEntitiesByClass(WitherSkeleton.class)) {
                            if (ws.getTarget() instanceof Player player) {
                                if (Math.random() * 100 <= Feature.WITHER_MACHINEGUN.getDouble("chance-percent")) {
                                    performMachineGun(ws, player);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in WitherMachineGun", e);
                }
            }
        }.runTaskTimer(plugin, 20L, 120L));
    }

    private void performMachineGun(Creature witherSkeleton, Player target) {
        double damage = Feature.WITHER_MACHINEGUN.getDouble("damage");

        for (int delay = 0; delay < 12; delay += 3) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (witherSkeleton.isDead())
                        return;

                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SKELETON_DEATH, 1.0F, 2.0F);

                    ItemStack stack = new ItemStack(Material.COAL);
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("SUDDEN_DEATH:" + UUID.randomUUID()));
                        stack.setItemMeta(meta);
                    }

                    NoInteractItemEntity item = new NoInteractItemEntity(
                            witherSkeleton.getLocation().add(0, 1, 0), stack);

                    Vector velocity = target.getLocation().add(0, 2, 0).toVector()
                            .subtract(witherSkeleton.getLocation().add(0, 1, 0).toVector())
                            .normalize().multiply(2);

                    item.getEntity().setVelocity(velocity);

                    new BukkitRunnable() {
                        double ticks = 0;

                        @Override
                        public void run() {
                            ticks++;
                            Item entityItem = item.getEntity();

                            if (ticks >= 20 || entityItem.isDead()) {
                                item.close();
                                cancel();
                                return;
                            }

                            entityItem.getWorld().spawnParticle(Particle.SMOKE, entityItem.getLocation(), 0);

                            for (Entity nearby : entityItem.getNearbyEntities(1.3, 1.3, 1.3)) {
                                if (nearby instanceof Player player) {
                                    item.close();
                                    Utils.damage(player, damage, true);
                                    cancel();
                                    return;
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
            }.runTaskLater(plugin, delay);
        }
    }
}