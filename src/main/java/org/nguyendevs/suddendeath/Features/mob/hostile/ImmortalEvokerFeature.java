package org.nguyendevs.suddendeath.Features.mob.hostile;

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
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import org.bukkit.NamespacedKey;

import java.util.Objects;
import java.util.logging.Level;

public class ImmortalEvokerFeature extends AbstractFeature {

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
                        if (!Feature.IMMORTAL_EVOKER.isEnabled(world)) continue;
                        for (Evoker evoker : world.getEntitiesByClass(Evoker.class)) {
                            if (evoker.getTarget() instanceof Player
                                    && isTotemUsed(evoker)) {
                                applyImmortalEvokerFangs(evoker);
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
            if (evoker.getHealth() - event.getFinalDamage() > 0) return;

            double chance = Feature.IMMORTAL_EVOKER.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            event.setCancelled(true);
            evoker.setHealth(Objects.requireNonNull(evoker.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue());
            evoker.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200,
                    (int) Feature.IMMORTAL_EVOKER.getDouble("resistance-amplifier") - 1));
            evoker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
            evoker.getPersistentDataContainer().set(totemUsed, PersistentDataType.BYTE, (byte) 1);

            evoker.getWorld().playSound(evoker.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            evoker.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    evoker.getLocation().add(0, 1, 0), 100, 0.7, 0.7, 0.7, 0.3);

            createTotemParticles(evoker);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in EvokerFeature.onEntityDamage", e);
        }
    }

    private void createTotemParticles(Evoker evoker) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40 || !evoker.isValid()) { cancel(); return; }
                for (int i = 0; i < 8; i++) {
                    double angle = (ticks + i * Math.PI / 4) % (Math.PI * 2);
                    evoker.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                            evoker.getLocation().add(
                                    Math.cos(angle) * 0.8,
                                    1.2 + Math.sin(ticks * 0.2) * 0.3,
                                    Math.sin(angle) * 0.8),
                            1, 0, 0, 0, 0.2);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void applyImmortalEvokerFangs(Evoker evoker) {
        if (evoker.getHealth() <= 0 || !(evoker.getTarget() instanceof Player player)) return;

        try {
            Location initialLoc = player.getLocation().clone();
            World world = evoker.getWorld();

            new BukkitRunnable() {
                int ticks = 0;
                final int duration = 20;

                @Override
                public void run() {
                    if (ticks >= duration || !player.isValid() || player.isDead()) { cancel(); return; }

                    Location fangLoc = initialLoc.clone().add(
                            RANDOM.nextDouble() - 0.5, -1 + (ticks * 0.2), RANDOM.nextDouble() - 0.5);
                    world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
                    world.spawnParticle(Particle.WITCH, fangLoc.clone().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticle(Particle.SMOKE, fangLoc, 10, 0.3, 0.3, 0.3, 0.05);
                    world.playSound(fangLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, 0.8f, 1.2f);
                    ticks++;
                }
            }.runTaskTimer(plugin, 0, 2);

            if (!Utils.hasCreativeGameMode(player)) {
                new BukkitRunnable() {
                    int ticks = 0;
                    final int duration = 30;
                    final double maxDistanceSq = 4.0;

                    @Override
                    public void run() {
                        if (ticks >= duration || !player.isValid() || player.isDead()) { cancel(); return; }
                        if (player.getLocation().distanceSquared(initialLoc) > maxDistanceSq) { cancel(); return; }

                        double progress = (double) ticks / duration;
                        Location pullLoc = initialLoc.clone().subtract(0, 3 * progress, 0);
                        player.teleport(pullLoc);
                        world.spawnParticle(Particle.BLOCK, pullLoc.clone().add(0, 1, 0),
                                20, 0.5, 0.5, 0.5, 0, pullLoc.getBlock().getBlockData());
                        world.spawnParticle(Particle.ENTITY_EFFECT, pullLoc,
                                15, 0.5, 0.5, 0.5, 0, Color.fromRGB(75, 0, 130));
                        world.playSound(pullLoc, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.9f);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 20, 1);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying Immortal Evoker Fangs", e);
        }
    }

    private boolean isTotemUsed(Evoker evoker) {
        Byte val = evoker.getPersistentDataContainer().get(totemUsed, PersistentDataType.BYTE);
        return val != null && val == 1;
    }
}