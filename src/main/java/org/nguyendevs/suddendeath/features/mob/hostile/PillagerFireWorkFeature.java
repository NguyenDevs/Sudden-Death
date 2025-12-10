package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.logging.Level;

public class PillagerFireWorkFeature extends AbstractFeature {

    private static final String FIREWORK_ARROW_KEY = "SD_FireworkArrow";

    @Override
    public String getName() {
        return "Pillager Firework Feature";
    }

    @EventHandler
    public void onPillagerShoot(EntityShootBowEvent event){
        if(!(event.getEntity() instanceof Pillager pillager)) return;
        if(event.getBow() == null || event.getBow().getType() != Material.CROSSBOW) return;
        if(!Feature.FIREWORK_ARROWS.isEnabled(pillager)) return;
        if(!(event.getProjectile() instanceof Arrow arrow)) return;

        try{
            double chance = Feature.FIREWORK_ARROWS.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            arrow.setMetadata(FIREWORK_ARROW_KEY, new FixedMetadataValue(plugin, true));
            pillager.getWorld().playSound(pillager.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

            new BukkitRunnable(){
                @Override
                public void run(){
                    if(arrow.isDead() || arrow.isOnGround()) {
                        cancel();
                        return;
                    }
                    arrow.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, arrow.getLocation(), 2, 0, 0, 0, 0);
                }
            }.runTaskTimer(plugin, 1L, 1L);
        } catch(Exception e){
            plugin.getLogger().log(Level.WARNING, "Error in PillagerFireworkFeature shoot event", e);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event){
        if(!(event.getEntity() instanceof Arrow arrow)) return;
        if(!arrow.hasMetadata(FIREWORK_ARROW_KEY)) return;

        try{
            Location loc = arrow.getLocation();

            explode(loc);
            arrow.remove();
        } catch (Exception e){
            plugin.getLogger().log(Level.WARNING, "Error in PillagerFireworkFeature hit event", e);
        }
    }

    private void explode(Location loc){
        World world = loc.getWorld();
        if(world == null) return;

        double range = Feature.FIREWORK_ARROWS.getDouble("area");
        double damage = Feature.FIREWORK_ARROWS.getDouble("damage");
        double duration = Feature.FIREWORK_ARROWS.getDouble("duration");

        Firework fw = (Firework) world.spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.fromRGB(139,69,19), Color.MAROON, Color.ORANGE)
                .withFade(Color.BLACK)
                .trail(true)
                .flicker(true)
                .build());
        FireworkMeta meta2 = fw.getFireworkMeta();
        meta2.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.fromRGB(139,69,19), Color.ORANGE, Color.PURPLE)
                .withFade(Color.GRAY)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        fw.detonate();
        meta2.setPower(0);
        fw.setFireworkMeta(meta2);
        fw.detonate();

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.0f);

        for(Entity entity : world.getNearbyEntities(loc, range, range, range)){
            if(entity instanceof Player player && !Utils.hasCreativeGameMode(player)){
                Utils.damage(player, damage, true);

                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int)(duration * 20), 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 4, 1));
            }
        }
    }
}
