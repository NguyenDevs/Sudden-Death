package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class WitchScrollsFeature extends AbstractFeature {

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
                        if (!Feature.WITCH_SCROLLS.isEnabled(world)) continue;
                        world.getEntitiesByClass(Witch.class).forEach(WitchScrollsFeature.this::loop4s_witch);
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
            int slowDuration = (int) (Feature.WITCH_SCROLLS.getDouble("slow-duration") * 20);
            int weakDuration = (int) (Feature.WITCH_SCROLLS.getDouble("weak-duration") * 20);
            double damage = Feature.WITCH_SCROLLS.getDouble("damage");

            for (Entity entity : witch.getNearbyEntities(10, 10, 10)) {
                if (!(entity instanceof Player player)) continue;
                if (Utils.hasCreativeGameMode(player) || !witch.hasLineOfSight(player)) continue;

                witch.getWorld().playSound(witch.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.0f, 2.0f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weakDuration, 1));
                Utils.damage(player, damage, true);

                Location playerLoc = entity.getLocation().add(0, 1, 0);
                Location witchLoc = witch.getLocation().add(0, 1, 0);
                Vector direction = witchLoc.toVector().subtract(playerLoc.toVector());
                for (double j = 0; j < 1.0; j += 0.04) {
                    Location particleLoc = playerLoc.clone().add(direction.clone().multiply(j));
                    particleLoc.getWorld().spawnParticle(Particle.WITCH, particleLoc, 4, 0.1, 0.1, 0.1, 0);
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
                    for (int j = 0; j < 3; j++) {
                        step += Math.PI / 20;
                        for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                            Location particleLoc = loc.clone().add(
                                    radius * Math.cos(i) * Math.sin(step),
                                    radius * (1 + Math.cos(step)),
                                    radius * Math.sin(i) * Math.sin(step));
                            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 0,
                                    new Particle.DustOptions(Color.WHITE, 1));
                        }
                    }
                    if (step >= Math.PI * 2) cancel();
                } catch (Exception e) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}