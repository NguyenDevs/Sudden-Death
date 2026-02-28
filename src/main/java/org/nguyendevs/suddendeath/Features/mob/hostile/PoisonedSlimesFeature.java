package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class PoisonedSlimesFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Poisoned Slimes";
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;

        try {
            if (event.getEntity() instanceof Slime slime &&
                    event.getDamager() instanceof Player player &&
                    !Utils.hasCreativeGameMode(player) &&
                    Feature.POISONED_SLIMES.isEnabled(slime)) {
                applyPoisonEffect(player, slime);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in PoisonedSlimes event", e);
        }
    }

    private void applyPoisonEffect(Player player, Slime slime) {
        double chance = Feature.POISONED_SLIMES.getDouble("chance-percent") / 100.0;
        if (RANDOM.nextDouble() > chance) return;

        double duration = Feature.POISONED_SLIMES.getDouble("duration");
        int amplifier = (int) Feature.POISONED_SLIMES.getDouble("amplifier");

        Location loc = slime.getLocation();
        World world = slime.getWorld();

        world.spawnParticle(Particle.ITEM_SLIME, loc, 32, 1, 1, 1, 0);
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 24, 1, 1, 1, 0);
        world.playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 2.0f, 1.0f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (duration * 20), amplifier));
    }
}