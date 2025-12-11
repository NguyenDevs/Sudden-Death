package org.nguyendevs.suddendeath.features.world;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.Set;
import java.util.logging.Level;

public class WhispersOfTheDesertFeature extends AbstractFeature {

    private static final Set<String> ARID_BIOMES = Set.of(
            "DESERT", "BADLANDS", "ERODED_BADLANDS", "WOODED_BADLANDS",
            "SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_SAVANNA"
    );

    @Override
    public String getName() {
        return "Whispers of the Desert";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (!Feature.WHISPERS_OF_THE_DESERT.isEnabled(world)) continue;
                        if (world.getEnvironment() != World.Environment.NORMAL) continue;

                        for (Player player : world.getPlayers()) {
                            if (Utils.hasCreativeGameMode(player)) continue;

                            if (isAridBiome(player.getLocation().getBlock().getBiome()) &&
                                    RANDOM.nextDouble() * 100 < Feature.WHISPERS_OF_THE_DESERT.getDouble("chance-percent")) {

                                int maxMobs = (int) Feature.WHISPERS_OF_THE_DESERT.getDouble("max-mobs");
                                int count = RANDOM.nextInt(maxMobs) + 1;

                                for (int i = 0; i < count; i++) {
                                    spawnRisingHusk(player);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Whispers of the Desert task", e);
                }
            }
        }.runTaskTimer(plugin, 100L, 120L));
    }

    private boolean isAridBiome(org.bukkit.block.Biome biome) {
        return ARID_BIOMES.contains(biome.name());
    }

    private void spawnRisingHusk(Player player) {
        Location playerLoc = player.getLocation();
        double offsetX = (RANDOM.nextDouble() * 10) - 5;
        double offsetZ = (RANDOM.nextDouble() * 10) - 5;

        int x = (int) (playerLoc.getX() + offsetX);
        int z = (int) (playerLoc.getZ() + offsetZ);
        int y = player.getWorld().getHighestBlockYAt(x, z);

        Location spawnLoc = new Location(player.getWorld(), x + 0.5, y, z + 0.5);
        Block blockUnder = spawnLoc.clone().add(0, -1, 0).getBlock();

        if (!blockUnder.getType().isSolid()) return;

        Husk husk = (Husk) player.getWorld().spawnEntity(spawnLoc, EntityType.HUSK);
        husk.setAI(false);
        husk.setInvulnerable(true);

        Location graveLoc = spawnLoc.clone().add(0, -2.0, 0);
        husk.teleport(graveLoc);

        BlockData particleData = blockUnder.getBlockData();
        float spawnDuration = 2.2f;
        float step = (2.0f / (spawnDuration * 20));

        new BukkitRunnable() {
            int ticks = 0;
            int soundTicker = 0;
            final Location currentLoc = graveLoc.clone();

            @Override
            public void run() {
                if (!husk.isValid() || ticks > spawnDuration * 20 + 20) {
                    if (husk.isValid()) {
                        husk.setAI(true);
                        husk.setInvulnerable(false);
                    }
                    cancel();
                    return;
                }

                if (currentLoc.getBlockY() < spawnLoc.getY()) {
                    currentLoc.add(0, step, 0);
                    husk.teleport(currentLoc);

                    husk.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                            currentLoc.clone().add(0, 1.5, 0),
                            10, 0.3, 0.0, 0.3, 0.0,
                            particleData);

                    if (soundTicker % 4 == 0) {
                        husk.getWorld().playSound(currentLoc, Sound.BLOCK_GRAVEL_HIT, 0.2f, 0.1f);
                    }
                    soundTicker++;
                } else {
                    husk.teleport(spawnLoc);
                    husk.setAI(true);
                    husk.setInvulnerable(false);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}