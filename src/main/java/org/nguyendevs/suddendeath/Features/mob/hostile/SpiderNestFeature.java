package org.nguyendevs.suddendeath.Features.mob.hostile;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.logging.Level;


public class SpiderNestFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Spider Nest";
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCobwebBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.COBWEB)
            return;

        Player player = event.getPlayer();
        World world = player.getWorld();

        if (!Feature.SPIDER_NEST.isEnabled(world))
            return;

        try {
            double chance = Feature.SPIDER_NEST.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance)
                return;

            int min = (int) Feature.SPIDER_NEST.getDouble("min");
            int max = (int) Feature.SPIDER_NEST.getDouble("max");
            if (min < 1)
                min = 1;
            if (max < min)
                max = min;
            int count = (max > min) ? min + RANDOM.nextInt(max - min + 1) : min;

            double caveChance = Feature.SPIDER_NEST.getDouble("cave-spider-chance") / 100.0;

            Location spawnLoc = event.getBlock().getLocation().add(0.5, 0, 0.5);

            for (int i = 0; i < count; i++) {
                if (RANDOM.nextDouble() < caveChance) {
                    world.spawnEntity(spawnLoc, EntityType.CAVE_SPIDER);
                } else {
                    world.spawnEntity(spawnLoc, EntityType.SPIDER);
                }
            }

            world.spawnParticle(Particle.CRIT, spawnLoc, 20, 0.4, 0.4, 0.4, 0.05);
            world.playSound(spawnLoc, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.7f);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in SpiderNestFeature for player: " + player.getName(), e);
        }
    }
}
