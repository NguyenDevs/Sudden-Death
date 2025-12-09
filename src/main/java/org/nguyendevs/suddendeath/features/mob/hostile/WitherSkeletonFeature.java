package org.nguyendevs.suddendeath.features.mob.hostile;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.listener.Loops;
import org.nguyendevs.suddendeath.util.*;
import java.util.logging.Level;
public class WitherSkeletonFeature extends AbstractFeature {
    @Override
    public String getName() {
        return "Wither Rush";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable witherLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (!Feature.WITHER_RUSH.isEnabled(world)) continue;
                        for (WitherSkeleton witherSkeleton : world.getEntitiesByClass(WitherSkeleton.class)) {
                            if (witherSkeleton.getTarget() instanceof Player) {
                                Loops.loop6s_wither_skeleton(witherSkeleton);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in WitherSkeleton loop", e);
                }
            }
        };
        witherLoop.runTaskTimer(plugin, 20L, 120L);
        registerTask((BukkitTask) witherLoop);
    }
}