package org.nguyendevs.suddendeath.Features.mob.hostile;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import java.util.logging.Level;

public class CreeperFeature extends AbstractFeature {
    @Override
    public String getName() {
        return "Creeper Revenge";
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (event.getEntity().hasMetadata("NPC")) return;
        if (!Feature.CREEPER_REVENGE.isEnabled(creeper)) return;

        try {
            double chance = Feature.CREEPER_REVENGE.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() <= chance) {
                float power = creeper.isPowered() ? 6.0f : 3.0f;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            creeper.getWorld().createExplosion(creeper.getLocation(), power);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Error in Creeper Revenge explosion", e);
                        }
                    }
                }.runTaskLater(plugin, 15);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in CreeperFeature.onEntityDeath", e);
        }
    }
}