package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import org.bukkit.NamespacedKey;
import java.util.logging.Level;

public class EvokerFeature extends AbstractFeature {

    private NamespacedKey totemUsed;

    @Override
    public String getName() {
        return "Immortal Evoker";
    }

    @Override
    protected void onEnable() {
        totemUsed = new NamespacedKey(plugin, "totem_used");

        BukkitRunnable fangLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.IMMORTAL_EVOKER.isEnabled(world)) {
                            for (Evoker evoker : world.getEntitiesByClass(Evoker.class)) {
                                if (evoker.getTarget() instanceof Player &&
                                        evoker.getPersistentDataContainer().has(totemUsed, PersistentDataType.BYTE) &&
                                        evoker.getPersistentDataContainer().get(totemUsed, PersistentDataType.BYTE) == 1) {
                                    applyImmortalEvokerFangs(evoker);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Evoker Fangs loop", e);
                }
            }
        };
        registerTask(fangLoop.runTaskTimer(plugin, 0L, 100L));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Evoker evoker)) return;
        if (event.getDamage() <= 0 || evoker.hasMetadata("NPC")) return;
        if (!Feature.IMMORTAL_EVOKER.isEnabled(evoker)) return;

        try {
            if (evoker.getHealth() - event.getFinalDamage() <= 0) {
                double chance = Feature.IMMORTAL_EVOKER.getDouble("chance-percent") / 100.0;
                if (RANDOM.nextDouble() <= chance) {
                    event.setCancelled(true);
                    evoker.setHealth(evoker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
                    evoker.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200,
                            (int) Feature.IMMORTAL_EVOKER.getDouble("resistance-amplifier") - 1));
                    evoker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));
                    evoker.getPersistentDataContainer().set(totemUsed, PersistentDataType.BYTE, (byte) 1);

                    evoker.getWorld().playSound(evoker.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    evoker.getWorld().spawnParticle(Particle.TOTEM,
                            evoker.getLocation().add(0, 1, 0), 100, 0.7, 0.7, 0.7, 0.3);

                    createTotemParticles(evoker);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in EvokerFeature.onEntityDamage", e);
        }
    }

    private void createTotemParticles(Evoker evoker) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                try {
                    if (ticks >= 40 || !evoker.isValid()) {
                        cancel();
                        return;
                    }
                    for (int i = 0; i < 8; i++) {
                        double angle = (ticks + i * Math.PI / 4) % (Math.PI * 2);
                        double x = Math.cos(angle) * 0.8;
                        double z = Math.sin(angle) * 0.8;
                        evoker.getWorld().spawnParticle(Particle.TOTEM,
                                evoker.getLocation().add(x, 1.2 + Math.sin(ticks * 0.2) * 0.3, z),
                                1, 0, 0, 0, 0.2);
                    }
                    ticks++;
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void applyImmortalEvokerFangs(Evoker evoker) {
        if (evoker == null || evoker.getHealth() <= 0 || !(evoker.getTarget() instanceof Player player)) return;

        try {
            Location initialLoc = player.getLocation().clone();
            World world = evoker.getWorld();

            new BukkitRunnable() {
                int ticks = 0;
                final int duration = 20;

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !player.isValid() || player.isDead()) {
                            cancel();
                            return;
                        }
                        Location fangLoc = initialLoc.clone().add(
                                RANDOM.nextDouble() * 1 - 0.5, -1 + (ticks * 0.2), RANDOM.nextDouble() * 1 - 0.5);
                        world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
                        world.spawnParticle(Particle.SPELL_WITCH, fangLoc.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.05);
                        world.spawnParticle(Particle.SMOKE_NORMAL, fangLoc, 10, 0.3, 0.3, 0.3, 0.05);
                        world.playSound(fangLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, 0.8f, 1.2f);
                        ticks++;
                    } catch (Exception e) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 2);

            if (!Utils.hasCreativeGameMode(player)) {
                new BukkitRunnable() {
                    int ticks = 0;
                    final int duration = 30;
                    final double maxDistance = 2.0;

                    @Override
                    public void run() {
                        try {
                            if (ticks >= duration || !player.isValid() || player.isDead()) {
                                cancel();
                                return;
                            }
                            if (player.getLocation().distanceSquared(initialLoc) > maxDistance * maxDistance) {
                                cancel();
                                return;
                            }
                            double progress = (double) ticks / duration;
                            Location pullLoc = initialLoc.clone().subtract(0, 3 * progress, 0);
                            player.teleport(pullLoc);
                            world.spawnParticle(Particle.BLOCK_CRACK, pullLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0,
                                    pullLoc.getBlock().getType().createBlockData());
                            world.spawnParticle(Particle.SPELL_MOB, pullLoc, 15, 0.5, 0.5, 0.5, 0,
                                    Color.fromRGB(75, 0, 130));
                            world.playSound(pullLoc, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.9f);
                            ticks++;
                        } catch (Exception e) {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 20, 1);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Immortal Evoker Fangs", e);
        }
    }
}