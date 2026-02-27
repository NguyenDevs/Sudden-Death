package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class PillagerFireWorkFeature extends AbstractFeature {

    private static final String FIREWORK_ARROW_KEY = "SD_FireworkArrow";

    @Override
    public String getName() {
        return "Pillager Firework Feature";
    }

    @EventHandler
    public void onPillagerShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Pillager pillager))
            return;
        if (event.getBow() == null || event.getBow().getType() != Material.CROSSBOW)
            return;
        if (!Feature.FIREWORK_ARROWS.isEnabled(pillager))
            return;
        if (!(event.getProjectile() instanceof Arrow arrow))
            return;

        try {
            double chance = Feature.FIREWORK_ARROWS.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance)
                return;

            Vector velocity = arrow.getVelocity();
            velocity.setY(velocity.getY() * 0.7);

            Location spawnLoc = pillager.getEyeLocation()
                    .add(pillager.getLocation().getDirection().multiply(0.5))
                    .subtract(0, 0.2, 0);

            event.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    Firework firework = pillager.getWorld().spawn(spawnLoc, Firework.class);
                    FireworkMeta meta = firework.getFireworkMeta();

                    Color brown = Color.fromRGB(160, 82, 45);
                    Color maroon = Color.MAROON;

                    FireworkEffect effect = FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL)
                            .withColor(brown, maroon)
                            .withFade(Color.BLACK)
                            .trail(true)
                            .flicker(true)
                            .build();
                    meta.addEffect(effect);
                    meta.setPower(0);
                    firework.setFireworkMeta(meta);

                    firework.setVelocity(velocity);
                    firework.setShooter(pillager);

                    firework.setMetadata(FIREWORK_ARROW_KEY, new FixedMetadataValue(plugin, true));

                    pillager.getWorld().playSound(pillager.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f,
                            1.0f);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (firework.isDead() || !firework.isValid()) {
                                cancel();
                                return;
                            }
                            firework.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, firework.getLocation(), 1, 0, 0,
                                    0, 0);
                        }
                    }.runTaskTimer(plugin, 1L, 1L);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in PillagerFireworkFeature shoot event", e);
                }
            }, RANDOM.nextInt(5) + 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in PillagerFireworkFeature shoot event", e);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Firework firework))
            return;
        if (!firework.hasMetadata(FIREWORK_ARROW_KEY))
            return;

        firework.detonate();
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        if (event.getEntity().hasMetadata(FIREWORK_ARROW_KEY)) {
            handleExplosion(event.getEntity());
        }
    }

    private void handleExplosion(Firework firework) {
        try {
            Location loc = firework.getLocation();
            double range = Feature.FIREWORK_ARROWS.getDouble("area");
            double damage = Feature.FIREWORK_ARROWS.getDouble("damage");
            double duration = Feature.FIREWORK_ARROWS.getDouble("duration");

            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
            loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.0f);

            for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
                if (entity instanceof Player player && !Utils.hasCreativeGameMode(player)) {
                    Utils.damage(player, damage, true);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (duration * 20), 1));
                    try {
                        player.getClass().getMethod("playHurtAnimation", float.class).invoke(player, 0.003f);
                    } catch (Exception e) {
                        try {
                            player.getClass().getMethod("playHurtAnimation").invoke(player);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in PillagerFireworkFeature explode event", e);
        }
    }
}