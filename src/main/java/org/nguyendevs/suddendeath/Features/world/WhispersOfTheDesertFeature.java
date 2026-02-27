package org.nguyendevs.suddendeath.Features.world;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public class WhispersOfTheDesertFeature extends AbstractFeature {

    private static final Set<String> ARID_BIOMES = Set.of(
            "DESERT", "BADLANDS", "ERODED_BADLANDS", "WOODED_BADLANDS",
            "SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_SAVANNA"
    );

    private static final String METADATA_KEY = "SD_WHISPER_HUSK";
    private static final double SPAWN_DEPTH = 2.5;
    private static final Material PARTICLE_BLOCK = Material.LAVA;
    private static final Sound SPAWN_SOUND = Sound.BLOCK_GRAVEL_HIT;
    private static final float SOUND_VOLUME = 0.2f;
    private static final float SOUND_PITCH = 0.1f;
    private static final int SOUND_DELAY = 4;

    private final Map<UUID, WaveData> playerWaves = new ConcurrentHashMap<>();

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

                            if (isAridBiome(player.getLocation().getBlock().getBiome())) {
                                handleWaveSpawning(player);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Whispers of the Desert task", e);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L));
    }

    private boolean isAridBiome(org.bukkit.block.Biome biome) {
        return ARID_BIOMES.contains(biome.name());
    }

    private void handleWaveSpawning(Player player) {
        UUID playerId = player.getUniqueId();
        WaveData wave = playerWaves.get(playerId);

        boolean shouldSpawn = false;

        if (wave == null) {
            shouldSpawn = true;
        } else {
            boolean cooldownExpired = (System.currentTimeMillis() - wave.startTime) > (5 * 60 * 1000);
            boolean distanceExceeded = player.getLocation().distance(wave.spawnCenter) > 50;

            wave.activeMobs.removeIf(uuid -> Bukkit.getEntity(uuid) == null || Bukkit.getEntity(uuid).isDead());
            boolean allKilled = wave.activeMobs.isEmpty();

            if (cooldownExpired || distanceExceeded || allKilled) {
                shouldSpawn = true;
            }
        }

        if (shouldSpawn) {
            if (RANDOM.nextDouble() * 100 < Feature.WHISPERS_OF_THE_DESERT.getDouble("chance-percent")) {
                startNewWave(player);
            }
        }
    }

    private void startNewWave(Player player) {
        int maxMobs = (int) Feature.WHISPERS_OF_THE_DESERT.getDouble("max-mobs");
        int count = RANDOM.nextInt(maxMobs) + 1;
        double ambushChance = Feature.WHISPERS_OF_THE_DESERT.getDouble("ambush-chance");

        WaveData newWave = new WaveData(player.getLocation());
        playerWaves.put(player.getUniqueId(), newWave);

        boolean ambushTriggered = false;

        for (int i = 0; i < count; i++) {
            boolean isAmbush = !ambushTriggered && RANDOM.nextDouble() * 100 < ambushChance;
            if (isAmbush) ambushTriggered = true;

            long delay = RANDOM.nextInt(80);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    UUID mobId = spawnRisingHusk(player, isAmbush);
                    if (mobId != null) {
                        newWave.activeMobs.add(mobId);
                    }
                }
            }.runTaskLater(plugin, delay);
        }
    }

    private UUID spawnRisingHusk(Player player, boolean isAmbush) {
        Location surfaceLoc;

        if (isAmbush) {
            surfaceLoc = player.getLocation().clone();
            int groundY = player.getWorld().getHighestBlockYAt(surfaceLoc);
            if (Math.abs(surfaceLoc.getY() - groundY) < 3) {
                surfaceLoc.setY(groundY + 1.0);
            } else {
                return null;
            }
        } else {
            Location playerLoc = player.getLocation();
            double offsetX = (RANDOM.nextDouble() * 10) - 10;
            double offsetZ = (RANDOM.nextDouble() * 10) - 10;

            int x = (int) (playerLoc.getX() + offsetX);
            int z = (int) (playerLoc.getZ() + offsetZ);
            int y = player.getWorld().getHighestBlockYAt(x, z);
            surfaceLoc = new Location(player.getWorld(), x + 0.5, y + 1.0, z + 0.5);
        }

        Block blockUnder = surfaceLoc.clone().add(0, -1, 0).getBlock();
        if (!blockUnder.getType().isSolid()) return null;

        Location graveLoc = surfaceLoc.clone().add(0, -SPAWN_DEPTH, 0);

        Husk husk = (Husk) player.getWorld().spawnEntity(graveLoc, EntityType.HUSK);

        husk.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
        husk.setAI(false);
        husk.setInvulnerable(true);
        husk.setSilent(true);
        husk.setGravity(false);
        husk.setRotation(RANDOM.nextFloat() * 360, 0);

        applyRandomBuffs(husk);

        BlockData particleData = Feature.WHISPERS_OF_THE_DESERT.getBoolean("use-block-under-entity-for-particles")
                ? blockUnder.getBlockData()
                : PARTICLE_BLOCK.createBlockData();

        double durationSeconds = isAmbush
                ? (0.5 + RANDOM.nextDouble() * 0.5)
                : (2.0 + RANDOM.nextDouble() * 1.5);

        double totalTicks = durationSeconds * 20;
        double stepPerTick = SPAWN_DEPTH / totalTicks;

        new BukkitRunnable() {
            int ticksRun = 0;
            int soundTicker = 0;
            final Location currentLoc = graveLoc.clone();
            boolean hasPulled = false;

            @Override
            public void run() {
                if (!husk.isValid() || ticksRun > totalTicks + 20) {
                    finalizeMob(husk);
                    cancel();
                    return;
                }

                if (isAmbush && !hasPulled && player.isOnline() && !Utils.hasCreativeGameMode(player)) {
                    double progress = (currentLoc.getY() - graveLoc.getY()) / SPAWN_DEPTH;
                    if (progress >= 0.5) {
                        hasPulled = true;

                        Location pullDest = surfaceLoc.clone().subtract(0, 1, 0);

                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HUSK_CONVERTED_TO_ZOMBIE, 1.0f, 0.1f);
                        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getEyeLocation(), 30, 0.5, 0.5, 0.5, blockUnder.getBlockData());

                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 255));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100, 255));

                        new BukkitRunnable() {
                            int pullFrame = 0;
                            final int pullDuration = 5;
                            final Location startPos = player.getLocation();

                            @Override
                            public void run() {
                                if (!player.isOnline() || pullFrame > pullDuration) {
                                    cancel();
                                    return;
                                }
                                double ratio = (double) pullFrame / pullDuration;
                                Location interp = startPos.clone().add(pullDest.clone().subtract(startPos).multiply(ratio));
                                interp.setYaw(player.getLocation().getYaw());
                                interp.setPitch(player.getLocation().getPitch());
                                player.teleport(interp);
                                pullFrame++;
                            }
                        }.runTaskTimer(plugin, 0L, 1L);
                    }
                }

                if (currentLoc.getY() < surfaceLoc.getY()) {
                    currentLoc.add(0, stepPerTick, 0);
                    husk.teleport(currentLoc);

                    if (Feature.WHISPERS_OF_THE_DESERT.getBoolean("spawn-particles")) {
                        husk.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                                surfaceLoc.clone().add(0, 0.2, 0),
                                15, 0.4, 0.1, 0.4, 0.0,
                                particleData);

                        if (ticksRun % 2 == 0) {
                            husk.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                                    surfaceLoc.clone().add(0, 0.5, 0),
                                    3, 0.2, 0.2, 0.2, 0.05);
                        }
                    }

                    if (soundTicker % SOUND_DELAY == 0) {
                        husk.getWorld().playSound(surfaceLoc, SPAWN_SOUND, SOUND_VOLUME, SOUND_PITCH);
                    }
                    soundTicker++;
                } else {
                    husk.teleport(surfaceLoc);
                    finalizeMob(husk);
                    cancel();
                }
                ticksRun++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return husk.getUniqueId();
    }

    private void applyRandomBuffs(Husk husk) {
        if (RANDOM.nextBoolean()) {
            int roll = RANDOM.nextInt(3);
            if (roll == 0) husk.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0));
            else if (roll == 1) husk.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 0));
            else husk.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 1));
        }
    }

    private void finalizeMob(Husk husk) {
        if (husk.isValid()) {
            husk.setAI(true);
            husk.setInvulnerable(false);
            husk.setSilent(false);
            husk.setGravity(true);
            husk.setVelocity(new Vector(0, 0.2, 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Husk husk)) return;

        if (husk.hasMetadata(METADATA_KEY)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.HUSK) return;

        UUID deadId = event.getEntity().getUniqueId();
        for (WaveData data : playerWaves.values()) {
            if (data.activeMobs.contains(deadId)) {
                data.activeMobs.remove(deadId);
                break;
            }
        }
    }

    private static class WaveData {
        List<UUID> activeMobs = new ArrayList<>();
        Location spawnCenter;
        long startTime;

        public WaveData(Location center) {
            this.spawnCenter = center;
            this.startTime = System.currentTimeMillis();
        }
    }
}