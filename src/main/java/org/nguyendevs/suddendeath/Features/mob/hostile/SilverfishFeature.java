package org.nguyendevs.suddendeath.Features.mob.hostile;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;
public class SilverfishFeature extends AbstractFeature {
    @Override
    public String getName() {
        return "Silverfish Summon";
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie)) return;
        if (event.getEntity().hasMetadata("NPC")) return;
        if (!Feature.SILVERFISHES_SUMMON.isEnabled(event.getEntity())) return;

        try {
            double chance = Feature.SILVERFISHES_SUMMON.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int min = (int) Feature.SILVERFISHES_SUMMON.getDouble("min");
            int max = (int) Feature.SILVERFISHES_SUMMON.getDouble("max");
            int count = min + RANDOM.nextInt(max);

            Entity entity = event.getEntity();
            for (int j = 0; j < count; j++) {
                Vector velocity = new Vector(
                        RANDOM.nextDouble() - 0.5,
                        RANDOM.nextDouble() - 0.5,
                        RANDOM.nextDouble() - 0.5);
                entity.getWorld().spawnParticle(Particle.LARGE_SMOKE, entity.getLocation(), 0);
                entity.getWorld().spawnEntity(entity.getLocation(), EntityType.SILVERFISH)
                        .setVelocity(velocity);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in SilverfishFeature.onEntityDeath", e);
        }
    }
}