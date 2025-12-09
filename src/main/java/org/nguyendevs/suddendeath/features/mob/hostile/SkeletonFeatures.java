package org.nguyendevs.suddendeath.features.mob.hostile;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.listener.Loops;
import org.nguyendevs.suddendeath.util.*;
import java.util.logging.Level;
public class SkeletonFeatures extends AbstractFeature {
    @Override
    public String getName() {
        return "Skeleton Features (Bone Wizards + Bone Grenades + Shocking Arrows)";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable skeletonLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.BONE_WIZARDS.isEnabled(world)) {
                            for (Skeleton skeleton : world.getEntitiesByClass(Skeleton.class)) {
                                if (skeleton.getTarget() instanceof Player && isBoneWizard(skeleton)) {
                                    Loops.loop3s_skeleton(skeleton);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Skeleton loop", e);
                }
            }
        };
        skeletonLoop.runTaskTimer(plugin, 20L, 60L);
        registerTask((BukkitTask) skeletonLoop);
    }

    @EventHandler
    public void onSkeletonShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!Feature.BONE_GRENADES.isEnabled(skeleton)) return;
        if (!(skeleton.getTarget() instanceof Player target)) return;

        try {
            double chance = Feature.BONE_GRENADES.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() >= chance) return;

            event.setCancelled(true);
            double damage = Feature.BONE_GRENADES.getDouble("damage");
            skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);

            NoInteractItemEntity grenade = new NoInteractItemEntity(
                    skeleton.getEyeLocation(), new ItemStack(Material.SKELETON_SKULL));
            grenade.getEntity().setVelocity(
                    target.getLocation().subtract(skeleton.getLocation()).toVector().multiply(0.05).setY(0.6));

            new BukkitRunnable() {
                double ticks = 0;
                @Override
                public void run() {
                    try {
                        ticks++;
                        Item grenadeEntity = grenade.getEntity();
                        if (ticks > 40 || grenadeEntity.isDead()) {
                            grenade.close();
                            cancel();
                            return;
                        }
                        grenadeEntity.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                                grenadeEntity.getLocation(), 0);
                        if (grenadeEntity.isOnGround()) {
                            grenade.close();
                            grenadeEntity.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,
                                    grenadeEntity.getLocation(), 24, 3, 3, 3, 0);
                            grenadeEntity.getWorld().playSound(grenadeEntity.getLocation(),
                                    Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                            for (Entity nearby : grenadeEntity.getNearbyEntities(6, 6, 6)) {
                                if (nearby instanceof Player player) {
                                    Utils.damage(player, damage, true);
                                }
                            }
                            cancel();
                        }
                    } catch (Exception e) {
                        grenade.close();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Bone Grenade", e);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Skeleton)) return;
        if (!Feature.SHOCKING_SKELETON_ARROWS.isEnabled(player)) return;

        try {
            double chancePercent = Feature.SHOCKING_SKELETON_ARROWS.getDouble("chance-percent");
            if (Math.random() * 100 > chancePercent) return;

            applyShockingArrows(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Shocking Skeleton Arrows", e);
        }
    }

    private void applyShockingArrows(Player player) {
        double duration = Feature.SHOCKING_SKELETON_ARROWS.getDouble("shock-duration");
        Location loc = player.getLocation();

        // Smoke particles
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {
                try {
                    for (int j = 0; j < 3; j++) {
                        ticks += Math.PI / 15;
                        Location particleLoc = loc.clone().add(Math.cos(ticks), 1, Math.sin(ticks));
                        particleLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 0);
                    }
                    if (ticks >= Math.PI * 2) cancel();
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        // Shaking effect
        new BukkitRunnable() {
            int ticksPassed = 0;
            @Override
            public void run() {
                try {
                    ticksPassed++;
                    if (ticksPassed > duration * 20) {
                        cancel();
                        return;
                    }
                    player.playHurtAnimation(0.004f);
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 2);

        // Hurt sound
        new BukkitRunnable() {
            int playCount = 0;
            @Override
            public void run() {
                try {
                    if (playCount >= duration * 20) {
                        cancel();
                        return;
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
                    playCount++;
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private boolean isBoneWizard(Skeleton skeleton) {
        return skeleton.getCustomName() != null &&
                skeleton.getCustomName().equalsIgnoreCase("Bone Wizard");
    }
}