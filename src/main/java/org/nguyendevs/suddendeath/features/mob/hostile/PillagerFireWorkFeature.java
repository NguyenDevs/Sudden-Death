package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.World;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;

import java.util.logging.Level;

public class PillagerFireWorkFeature extends AbstractFeature {
    @Override
    public String getName() {
        return "Pillager Firework Feature";
    }

    @Override
    protected void onEnable() {
        // FireworkArrow Loop
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : org.bukkit.Bukkit.getWorlds()) {
                        if (Feature.FIREWORK_ARROWS.isEnabled(world)) {
                            for (Pillager pillager : world.getEntitiesByClass(Pillager.class)) {
                                if (pillager.getTarget() instanceof Player) {
                                    applyFireworkArrows(pillager);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Firework Arrows task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L));
    }
    
    private void applyFireworkArrows(Pillager pillager){
        if (pillager == null || pillager.getHealth() <= 0 || !(pillager.getTarget() instanceof Player target)) return;
        if (!target.getWorld().equals(pillager.getWorld())) return;

        try {
            double chance = Feature.FIREWORK_ARROWS.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) return;

            int area = (int) Feature.FIREWORK_ARROWS.getDouble("area");
            double damage = Feature.FIREWORK_ARROWS.getDouble("damage");
            double duration = Feature.FIREWORK_ARROWS.getDouble("duration");
        }
        catch (Exception e) {
        plugin.getLogger().log(Level.WARNING, "Error applying Firework Arrows", e);
      }
    }
}
