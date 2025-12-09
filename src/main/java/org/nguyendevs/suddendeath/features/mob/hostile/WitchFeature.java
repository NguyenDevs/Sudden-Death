package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.Objects;
import java.util.logging.Level;

public class WitchFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Witch Scrolls";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (Feature.WITCH_SCROLLS.isEnabled(world)) {
                            world.getEntitiesByClass(Witch.class).forEach(WitchFeature.this::loop4s_witch);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Witch loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 80L));
    }

    private void loop4s_witch(Witch witch) {
        if (witch == null || witch.getHealth() <= 0) return;
        try {
            for (Entity entity : witch.getNearbyEntities(10, 10, 10)) {
                if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player) || !witch.hasLineOfSight(player)) continue;

                witch.getWorld().playSound(witch.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.0f, 2.0f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (Feature.WITCH_SCROLLS.getDouble("slow-duration") * 20), 1));
                Utils.damage(player, Feature.WITCH_SCROLLS.getDouble("damage"), true);

                Location loc = entity.getLocation().add(0.0D, 1.0D, 0.0D);
                Location loc1 = witch.getLocation().add(0.0D, 1.0D, 0.0D);
                for(double j = 0.0D; j < 1.0D; j += 0.04D) {
                    Vector d = loc1.toVector().subtract(loc.toVector());
                    Location loc2 = loc.clone().add(d.multiply(j));
                    ((World)Objects.requireNonNull(loc2.getWorld())).spawnParticle(Particle.SPELL_WITCH, loc2, 4, 0.1D, 0.1D, 0.1D, 0.0D);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Witch loop for entity: " + witch.getUniqueId(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Witch witch)) return;
        if (event.getDamage() <= 0 || witch.hasMetadata("NPC")) return;
        if (!Feature.WITCH_SCROLLS.isEnabled(witch)) return;

        try {
            double chance = Feature.WITCH_SCROLLS.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() <= chance) {
                event.setCancelled(true);
                applyWitchScrollsEffect(witch);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in WitchFeature.onEntityDamage", e);
        }
    }

    private void applyWitchScrollsEffect(Witch witch) {
        witch.getWorld().playSound(witch.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
        Location loc = witch.getLocation();
        double radius = 1.5;

        new BukkitRunnable() {
            double step = 0;
            @Override
            public void run() {
                try {
                    for (double j = 0; j < 3; j++) {
                        step += Math.PI / 20;
                        for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                            Location particleLoc = loc.clone().add(radius * Math.cos(i) * Math.sin(step), radius * (1 + Math.cos(step)), radius * Math.sin(i) * Math.sin(step));
                            particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 0, new Particle.DustOptions(Color.WHITE, 1));
                        }
                    }
                    if (step >= Math.PI * 2) cancel();
                } catch (Exception e) { cancel(); }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}