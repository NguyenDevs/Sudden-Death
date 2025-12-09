package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.Particle;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;

import java.util.logging.Level;

public class UndeadRageFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Undead Rage";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (event.getDamage() <= 0 || zombie.hasMetadata("NPC")) return;
        if (!Feature.UNDEAD_RAGE.isEnabled(zombie)) return;

        try {
            applyUndeadRage(zombie);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in onEntityDamage (Undead Rage)", e);
        }
    }

    private void applyUndeadRage(Zombie zombie) {
        int duration = (int) (Feature.UNDEAD_RAGE.getDouble("rage-duration") * 20);
        zombie.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, zombie.getLocation().add(0, 1.7, 0), 6, 0.35, 0.35, 0.35, 0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 1));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
    }
}