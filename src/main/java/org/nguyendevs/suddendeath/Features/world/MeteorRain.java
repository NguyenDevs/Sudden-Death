package org.nguyendevs.suddendeath.Features.world;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Hook.CustomFlag;
import org.nguyendevs.suddendeath.Managers.EventManager.WorldStatus;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class MeteorRain extends WorldEventHandler implements Listener {

    private static final int MIN_METEOR_SIZE = 3;
    private static final int MAX_METEOR_SIZE = 10;
    private static final int SOUND_RADIUS = 150;
    private static final int SPAWN_HEIGHT = 100;
    private static final int VIEW_RANGE = 192;

    private static final Material[] METEOR_MATERIALS = {
            Material.DEEPSLATE, Material.DEEPSLATE, Material.DEEPSLATE,
            Material.BLACKSTONE, Material.BLACKSTONE, Material.BLACKSTONE,
            Material.MAGMA_BLOCK, Material.GILDED_BLACKSTONE
    };

    private static final Material[] SCORCHED_MATERIALS = {
            Material.COBBLESTONE, Material.DEEPSLATE, Material.MAGMA_BLOCK
    };

    private static final Material[] METEOR_CRUST_MATERIALS = {
            Material.DEEPSLATE, Material.BLACKSTONE, Material.COBBLED_DEEPSLATE
    };

    private final Map<UUID, MeteorTask> activeMeteors = new ConcurrentHashMap<>();
    private final Map<BlockPosition, Set<UUID>> phantomBlockTracking = new ConcurrentHashMap<>();
    private final List<CraterData> cratersToRecover = new ArrayList<>();
    private int spawningTaskId = -1;

    private static class CraterData {
        final Location center;
        final Map<Location, Material> originalBlocks = new HashMap<>();
        final int radius;
        final long recoveryStartTime;

        CraterData(Location center, int radius) {
            this.center = center;
            this.radius = radius;
            this.recoveryStartTime = System.currentTimeMillis() + 30000 + ThreadLocalRandom.current().nextInt(60000);
        }
    }

    public MeteorRain(World world) {
        super(world, 20 * 10, WorldStatus.METEOR_RAIN);
        Bukkit.getPluginManager().registerEvents(this, SuddenDeath.getInstance());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        if (e.getWorld().equals(getWorld()))
            close();
    }

    @Override
    public void run() {
        try {
            long time = getWorld().getTime();
            if (time < 13000 || time > 23000) {
                stopSpawning();
                return;
            }
            if (spawningTaskId == -1)
                startSpawning();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
                    "Error running Meteor Rain task in world: " + getWorld().getName(), e);
        }
    }

    private void startSpawning() {
        spawnMeteorShower();
        long delay = 40 + ThreadLocalRandom.current().nextInt(160);
        spawningTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(
                SuddenDeath.getInstance(), this::startSpawning, delay);
    }

    private void stopSpawning() {
        if (spawningTaskId != -1) {
            Bukkit.getScheduler().cancelTask(spawningTaskId);
            spawningTaskId = -1;
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            stopSpawning();
            new ArrayList<>(activeMeteors.values()).forEach(MeteorTask::cancel);
            activeMeteors.clear();
            clearAllPhantomBlocks();

            if (Feature.METEOR_RAIN.getBoolean("crater-recovery") && !cratersToRecover.isEmpty()) {
                startCraterRecoveryProcess();
            } else {
                cratersToRecover.clear();
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error cleaning up Meteor Rain effects in world: " + getWorld().getName(), e);
        }
    }

    private void clearAllPhantomBlocks() {
        World world = getWorld();
        if (world == null)
            return;
        WrappedBlockData air = WrappedBlockData.createData(Material.AIR.createBlockData());
        for (BlockPosition pos : phantomBlockTracking.keySet()) {
            for (Player player : world.getPlayers()) {
                sendBlockChange(player, pos, air);
            }
        }
        phantomBlockTracking.clear();
    }

    private void sendBlockChange(Player player, BlockPosition pos, WrappedBlockData data) {
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
            packet.getBlockPositionModifier().write(0, pos);
            packet.getBlockData().write(0, data);
            pm.sendServerPacket(player, packet, false);
        } catch (Exception ignored) {
        }
    }

    private boolean isProtected(Player p) {
        if (p == null || !p.isOnline())
            return true;
        GameMode gm = p.getGameMode();
        return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
    }

    private boolean isLocationProtected(Location loc) {
        if (SuddenDeath.getInstance().getWorldGuard() == null)
            return false;
        return !SuddenDeath.getInstance().getWorldGuard()
                .isFlagAllowedAtLocation(loc, CustomFlag.SDS_EVENT);
    }

    private void spawnMeteorShower() {
        try {
            List<Player> worldPlayers = getWorld().getPlayers();
            if (worldPlayers.isEmpty())
                return;

            List<Player> eligible = worldPlayers.stream().filter(p -> !isProtected(p)).toList();
            if (eligible.isEmpty())
                return;

            int meteorCount = ThreadLocalRandom.current().nextInt(1, 5);
            for (int i = 0; i < meteorCount; i++) {
                Player targetPlayer = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
                long delay = (long) i * ThreadLocalRandom.current().nextInt(3, 11);
                Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(),
                        () -> spawnMeteorNearPlayer(targetPlayer), delay);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error spawning meteor shower", e);
        }
    }

    private void spawnMeteorNearPlayer(Player targetPlayer) {
        try {
            if (isProtected(targetPlayer))
                return;

            Location playerLoc = targetPlayer.getLocation();
            double distance = ThreadLocalRandom.current().nextDouble(10, 60);
            double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);

            Location targetLocation = playerLoc.clone().add(
                    (int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
            targetLocation.setY(getWorld().getHighestBlockYAt(targetLocation));

            if (isLocationProtected(targetLocation))
                return;

            Vector diagonalDirection = generateRandomDiagonalDirection();
            double spawnDistance = ThreadLocalRandom.current().nextDouble(80, 120);

            Location startLocation = targetLocation.clone()
                    .add(diagonalDirection.getX() * spawnDistance, SPAWN_HEIGHT,
                            diagonalDirection.getZ() * spawnDistance);

            int meteorSize = ThreadLocalRandom.current().nextInt(MIN_METEOR_SIZE, MAX_METEOR_SIZE + 1);
            UUID meteorId = UUID.randomUUID();

            MeteorTask meteorTask = new MeteorTask(startLocation, targetLocation, meteorSize, meteorId);
            activeMeteors.put(meteorId, meteorTask);
            meteorTask.runTaskTimer(SuddenDeath.getInstance(), 0L, 1L);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error spawning meteor near player", e);
        }
    }

    private Vector generateRandomDiagonalDirection() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.3, 2 * Math.PI - 0.3);
        double[] avoidAngles = { 0, Math.PI / 2, Math.PI, 3 * Math.PI / 2 };
        for (double avoidAngle : avoidAngles) {
            if (Math.abs(angle - avoidAngle) < 0.2)
                angle += 0.3;
        }
        return new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();
    }

    private static boolean inHorizontalRange(Player p, Location c) {
        if (!p.getWorld().equals(c.getWorld()))
            return false;
        Location pl = p.getLocation();
        double dx = pl.getX() - c.getX();
        double dz = pl.getZ() - c.getZ();
        return (dx * dx + dz * dz) <= (MeteorRain.VIEW_RANGE * MeteorRain.VIEW_RANGE);
    }

    private enum MeteorShape {
        SPHERICAL, ARROW, IRREGULAR, TAILED, COMPLEX
    }

    private class MeteorTask extends BukkitRunnable {
        private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        private final Location startLocation;
        private final Location targetLocation;
        private final int meteorSize;
        private final UUID meteorId;
        private final Vector flightDirection;
        private final MeteorShape meteorShape;
        private final double speed;
        private final Map<UUID, Set<BlockPosition>> lastSentPerPlayer = new HashMap<>();
        private final List<MeteorVoxel> meteorVoxels;
        private final Map<Location, Material> destroyedBlocksInFlight = new HashMap<>();
        private final int MAX_TICKS = 20 * 40;

        private Location currentLocation;
        private int tickCount = 0;
        private boolean hasImpacted = false;

        private static class MeteorVoxel {
            final Vector offset;
            final WrappedBlockData data;

            MeteorVoxel(Vector offset, WrappedBlockData data) {
                this.offset = offset;
                this.data = data;
            }
        }

        public MeteorTask(Location start, Location target, int size, UUID meteorId) {
            this.startLocation = start.clone();
            this.targetLocation = target.clone();
            this.meteorSize = size;
            this.meteorId = meteorId;
            this.currentLocation = start.clone();
            this.meteorShape = selectRandomMeteorShape(size);

            Vector baseDirection = target.toVector().subtract(start.toVector()).normalize();
            double randomOffset = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
            this.flightDirection = new Vector(
                    baseDirection.getX() + randomOffset,
                    Math.min(baseDirection.getY(), -0.55),
                    baseDirection.getZ() + randomOffset).normalize();
            this.speed = ThreadLocalRandom.current().nextDouble(2.0, 3.5);
            this.meteorVoxels = generateMeteorVoxels(size, meteorId, meteorShape);
        }

        private MeteorShape selectRandomMeteorShape(int size) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double rand = random.nextDouble();

            if (size >= 9) {
                if (rand < 0.25)
                    return MeteorShape.COMPLEX;
                if (rand < 0.45)
                    return MeteorShape.IRREGULAR;
                if (rand < 0.65)
                    return MeteorShape.TAILED;
                if (rand < 0.85)
                    return MeteorShape.ARROW;
            } else if (size >= 6) {
                if (rand < 0.20)
                    return MeteorShape.COMPLEX;
                if (rand < 0.40)
                    return MeteorShape.IRREGULAR;
                if (rand < 0.60)
                    return MeteorShape.TAILED;
                if (rand < 0.80)
                    return MeteorShape.ARROW;
            } else {
                if (rand < 0.10)
                    return MeteorShape.COMPLEX;
                if (rand < 0.30)
                    return MeteorShape.IRREGULAR;
                if (rand < 0.50)
                    return MeteorShape.TAILED;
                if (rand < 0.70)
                    return MeteorShape.ARROW;
            }
            return MeteorShape.SPHERICAL;
        }

        @Override
        public void run() {
            try {
                if (hasImpacted || tickCount++ > MAX_TICKS) {
                    if (!hasImpacted)
                        impact(currentLocation.clone());
                    cancel();
                    return;
                }

                Location prev = currentLocation.clone();
                Vector movement = flightDirection.clone().multiply(speed);
                currentLocation.add(movement);

                World w = currentLocation.getWorld();
                if (w != null) {
                    org.bukkit.util.RayTraceResult r = w.rayTraceBlocks(
                            prev, movement.normalize(), movement.length(),
                            FluidCollisionMode.NEVER, true);
                    if (r != null && r.getHitPosition() != null) {
                        Block hitBlock = r.getHitBlock();
                        if (hitBlock != null && isSolidTerrain(hitBlock.getType())) {
                            saveAndDestroyBlocksInPath(prev, currentLocation);
                            hasImpacted = true;
                            impact(new Location(w,
                                    r.getHitPosition().getX(),
                                    r.getHitPosition().getY(),
                                    r.getHitPosition().getZ()));
                            cancel();
                            return;
                        }
                    }
                    checkAndDestroyBlocksAlongPath();
                }

                createTrailEffect();
                if (tickCount % 6 == 0)
                    playMeteorFlightSound();
                if (tickCount % 2 == 0)
                    renderFakeMeteor();

                if (currentLocation.getBlockY() <= getGroundLevel(currentLocation)) {
                    saveAndDestroyBlocksInPath(prev, currentLocation);
                    hasImpacted = true;
                    impact(currentLocation.clone());
                    cancel();
                }
            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in meteor task", e);
                cancel();
            }
        }

        private void checkAndDestroyBlocksAlongPath() {
            World world = currentLocation.getWorld();
            if (world == null)
                return;

            int checkRadius = Math.max(2, meteorSize / 3);
            double threshold = checkRadius * 0.7;

            for (int x = -checkRadius; x <= checkRadius; x++) {
                for (int y = -checkRadius; y <= checkRadius; y++) {
                    for (int z = -checkRadius; z <= checkRadius; z++) {
                        if (Math.sqrt(x * x + y * y + z * z) > threshold)
                            continue;
                        Location blockLoc = currentLocation.clone().add(x, y, z);
                        if (isLocationProtected(blockLoc))
                            continue;
                        if (SuddenDeath.getInstance().getClaimManager().isClaimed(blockLoc))
                            continue;
                        Block block = world.getBlockAt(blockLoc);
                        if (!isSolidTerrain(block.getType()) || block.getType() == Material.BEDROCK)
                            continue;
                        if (destroyedBlocksInFlight.containsKey(blockLoc))
                            continue;

                        destroyedBlocksInFlight.put(blockLoc.clone(), block.getType());
                        world.spawnParticle(Particle.BLOCK, blockLoc.clone().add(0.5, 0.5, 0.5),
                                3, 0.3, 0.3, 0.3, 0, block.getBlockData());
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }

        private void saveAndDestroyBlocksInPath(Location start, Location end) {
            World world = start.getWorld();
            if (world == null)
                return;

            Vector direction = end.toVector().subtract(start.toVector());
            double distance = direction.length();
            direction.normalize();

            int checkRadius = Math.max(2, meteorSize / 3);
            double threshold = checkRadius * 0.7;
            int steps = (int) Math.ceil(distance);

            for (int step = 0; step < steps; step++) {
                Location checkLoc = start.clone().add(direction.clone().multiply(step));
                for (int x = -checkRadius; x <= checkRadius; x++) {
                    for (int y = -checkRadius; y <= checkRadius; y++) {
                        for (int z = -checkRadius; z <= checkRadius; z++) {
                            if (Math.sqrt(x * x + y * y + z * z) > threshold)
                                continue;
                            Location blockLoc = checkLoc.clone().add(x, y, z);
                            if (isLocationProtected(blockLoc))
                                continue;
                            if (SuddenDeath.getInstance().getClaimManager().isClaimed(blockLoc))
                                continue;
                            Block block = world.getBlockAt(blockLoc);
                            if (!isSolidTerrain(block.getType()) || block.getType() == Material.BEDROCK)
                                continue;
                            if (destroyedBlocksInFlight.containsKey(blockLoc))
                                continue;

                            destroyedBlocksInFlight.put(blockLoc.clone(), block.getType());
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        private void createTrailEffect() {
            World world = currentLocation.getWorld();
            if (world == null)
                return;
            double offset = meteorSize / 2.0;
            world.spawnParticle(Particle.FLAME, currentLocation, 20, offset, offset, offset, 0.03);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation, 15, offset, offset, offset, 0.05);
            world.spawnParticle(Particle.LAVA, currentLocation, 8, offset / 2, offset / 2, offset / 2, 0);
            world.spawnParticle(Particle.CRIT, currentLocation, 10, offset, offset, offset, 0.1);
            world.spawnParticle(Particle.EXPLOSION, currentLocation, 5, offset / 3, offset / 3, offset / 3, 0.02);
        }

        private void playMeteorFlightSound() {
            World world = currentLocation.getWorld();
            if (world == null)
                return;
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(currentLocation) > SOUND_RADIUS)
                    continue;
                player.playSound(currentLocation, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.AMBIENT, 0.8f, 0.6f);
                player.playSound(currentLocation, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.AMBIENT, 1.0f, 0.4f);
                player.playSound(currentLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.AMBIENT, 0.5f,
                        0.8f);
            }
        }

        private void renderFakeMeteor() {
            World world = currentLocation.getWorld();
            if (world == null)
                return;

            Map<BlockPosition, WrappedBlockData> currentBlocks = new HashMap<>();
            for (MeteorVoxel voxel : meteorVoxels) {
                Location blockLoc = currentLocation.clone().add(voxel.offset);
                currentBlocks.put(new BlockPosition(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ()),
                        voxel.data);
            }

            WrappedBlockData air = WrappedBlockData.createData(Material.AIR.createBlockData());

            for (Player player : world.getPlayers()) {
                if (!inHorizontalRange(player, currentLocation))
                    continue;

                Set<BlockPosition> lastSent = lastSentPerPlayer.computeIfAbsent(player.getUniqueId(),
                        k -> new HashSet<>());

                for (BlockPosition oldPos : lastSent) {
                    sendBlockChange(player, oldPos, air);
                    removePhantomBlockTracking(oldPos, player.getUniqueId());
                }
                lastSent.clear();

                for (Map.Entry<BlockPosition, WrappedBlockData> entry : currentBlocks.entrySet()) {
                    sendBlockChange(player, entry.getKey(), entry.getValue());
                    addPhantomBlockTracking(entry.getKey(), player.getUniqueId());
                }

                lastSent.addAll(currentBlocks.keySet());
            }
        }

        private void addPhantomBlockTracking(BlockPosition pos, UUID playerId) {
            phantomBlockTracking.computeIfAbsent(pos, k -> new HashSet<>()).add(playerId);
        }

        private void removePhantomBlockTracking(BlockPosition pos, UUID playerId) {
            Set<UUID> players = phantomBlockTracking.get(pos);
            if (players == null)
                return;
            players.remove(playerId);
            if (players.isEmpty())
                phantomBlockTracking.remove(pos);
        }

        private void clearAllFakeBlocks() {
            WrappedBlockData air = WrappedBlockData.createData(Material.AIR.createBlockData());
            for (Map.Entry<UUID, Set<BlockPosition>> entry : lastSentPerPlayer.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline())
                    continue;
                for (BlockPosition pos : entry.getValue()) {
                    sendBlockChange(player, pos, air);
                    removePhantomBlockTracking(pos, entry.getKey());
                }
                entry.getValue().clear();
            }
        }

        private void impact(Location impactPoint) {
            try {
                World world = impactPoint.getWorld();
                if (world == null)
                    return;

                clearAllFakeBlocks();

                world.playSound(impactPoint, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 2.5f, 0.5f);
                world.playSound(impactPoint, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 1.5f, 0.7f);
                world.playSound(impactPoint, Sound.BLOCK_ANVIL_PLACE, SoundCategory.AMBIENT, 1.2f, 0.4f);

                world.spawnParticle(Particle.EXPLOSION_EMITTER, impactPoint, 10, 3, 3, 3, 0);
                world.spawnParticle(Particle.LAVA, impactPoint, 50, 4, 4, 4, 0);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, impactPoint, 30, 3, 3, 3, 0.05);
                world.spawnParticle(Particle.FLAME, impactPoint, 40, 3, 3, 3, 0.1);

                applyShockwaveAt(impactPoint);
                createEnhancedMeteorCrater(impactPoint);
            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error creating meteor impact", e);
            }
        }

        private int getGroundLevel(Location loc) {
            World world = loc.getWorld();
            if (world == null)
                return loc.getBlockY();
            for (int y = loc.getBlockY(); y >= world.getMinHeight(); y--) {
                if (isSolidTerrain(world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType()))
                    return y;
            }
            return world.getMinHeight();
        }

        private boolean isSolidTerrain(Material material) {
            return material.isSolid()
                    && material != Material.WATER
                    && !material.name().contains("LEAVES")
                    && !material.name().contains("LOG")
                    && !material.name().contains("WOOD")
                    && !isVegetation(material);
        }

        private boolean isExcavatable(Material material) {
            if (material == Material.BEDROCK)
                return false;
            if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR)
                return false;
            return true;
        }

        private boolean isVegetation(Material material) {
            return switch (material) {
                case SHORT_GRASS, TALL_GRASS, FERN, LARGE_FERN, DEAD_BUSH,
                        DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET,
                        RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP,
                        OXEYE_DAISY, SUNFLOWER, LILAC, ROSE_BUSH, PEONY, GLOWSTONE ->
                    true;
                default -> material.name().contains("SAPLING") || material.name().contains("MUSHROOM");
            };
        }

        private void applyShockwaveAt(Location center) {
            World world = center.getWorld();
            if (world == null)
                return;

            double knockRadius = meteorSize * 2.5;

            for (Entity entity : world.getNearbyEntities(center, knockRadius, knockRadius, knockRadius)) {
                if (!(entity instanceof LivingEntity livingEntity) || entity instanceof ArmorStand)
                    continue;

                double distance = livingEntity.getLocation().distance(center);
                if (distance > knockRadius)
                    continue;

                double ratio = 1.0 - distance / knockRadius;
                Vector direction = livingEntity.getLocation().toVector().subtract(center.toVector()).normalize();
                double strength = ratio * (meteorSize / 3.5) + 1.5;
                livingEntity.setVelocity(direction.multiply(strength).add(new Vector(0, 0.8 + meteorSize * 0.04, 0)));
                livingEntity.damage(Math.max(1.0, (5.0 + meteorSize) * ratio));
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4));
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 2));

                world.spawnParticle(Particle.CRIT, livingEntity.getEyeLocation().add(0, 0.5, 0), 5, 0.3, 0.1, 0.3, 0.1);
            }
        }

        private void createEnhancedMeteorCrater(Location impactPoint) {
            World world = impactPoint.getWorld();
            if (world == null)
                return;

            boolean createCrater = Feature.METEOR_RAIN.getBoolean("meteor-crater");
            boolean craterRecovery = Feature.METEOR_RAIN.getBoolean("crater-recovery");

            if (!createCrater) {
                if (craterRecovery && !destroyedBlocksInFlight.isEmpty()) {
                    CraterData flightData = new CraterData(impactPoint, meteorSize);
                    flightData.originalBlocks.putAll(destroyedBlocksInFlight);
                    cratersToRecover.add(flightData);
                }
                return;
            }

            CraterData craterData = craterRecovery ? new CraterData(impactPoint, meteorSize) : null;

            if (craterData != null) {
                craterData.originalBlocks.putAll(destroyedBlocksInFlight);
            }

            excavateCraterAlongFlightPath(impactPoint, craterData);

            Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
                performPostCraterWork(impactPoint, craterData);
                if (craterRecovery && craterData != null && !craterData.originalBlocks.isEmpty()) {
                    cratersToRecover.add(craterData);
                }
            }, 80L);
        }

        private void excavateCraterAlongFlightPath(Location impactPoint, CraterData craterData) {
            World world = impactPoint.getWorld();
            if (world == null)
                return;

            int totalSpheres = meteorSize * 2 + 6;
            double maxRadius = meteorSize * 1.3;
            if (craterData != null) {
                preScanCraterVolume(impactPoint, totalSpheres, maxRadius, craterData);
            }

            new BukkitRunnable() {
                int sphereIndex = 0;
                boolean meteorPlaced = false;

                @Override
                public void run() {
                    if (sphereIndex >= totalSpheres) {
                        if (!meteorPlaced) {
                            double depthRatio = 1.0;
                            Location sphereCenter = computeSphereCenterAlongPath(impactPoint, sphereIndex - 1,
                                    totalSpheres);
                            double yaw = Math.atan2(-flightDirection.getX(), flightDirection.getZ());
                            double pitch = Math.atan2(-flightDirection.getY(),
                                    Math.sqrt(flightDirection.getX() * flightDirection.getX()
                                            + flightDirection.getZ() * flightDirection.getZ()));
                            placeMeteorCore(sphereCenter, yaw, pitch, craterData);
                            meteorPlaced = true;
                        }
                        cancel();
                        return;
                    }

                    for (int batch = 0; batch < 3 && sphereIndex < totalSpheres; batch++) {
                        double depthRatio = (double) sphereIndex / totalSpheres;

                        double currentRadius = maxRadius * Math.sqrt(1.0 - depthRatio * 0.75);
                        currentRadius = Math.max(1.5, currentRadius);

                        Location sphereCenter = computeSphereCenterAlongPath(impactPoint, sphereIndex, totalSpheres);

                        excavateSphere(world, sphereCenter, currentRadius, depthRatio, craterData);

                        if (sphereIndex % 5 == 0) {
                            for (Player player : world.getPlayers()) {
                                if (player.getLocation().distanceSquared(sphereCenter) <= 400) {
                                    player.playSound(sphereCenter, Sound.BLOCK_GRINDSTONE_USE,
                                            SoundCategory.AMBIENT, 0.12f, 1.8f);
                                }
                            }
                        }

                        sphereIndex++;
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 2L, 1L);
        }

        private Location computeSphereCenterAlongPath(Location impactPoint, int index, int totalSpheres) {
            double stepDistance = 0.85;
            return impactPoint.clone().add(flightDirection.clone().multiply(index * stepDistance));
        }

        private void excavateSphere(World world, Location center, double radius, double depthRatio,
                CraterData craterData) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int r = (int) Math.ceil(radius) + 1;
            double shellThickness = 1.5;

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        double dist = Math.sqrt(x * x + y * y + z * z);

                        if (dist > radius)
                            continue;

                        Location blockLoc = center.clone().add(x, y, z);
                        if (isLocationProtected(blockLoc))
                            continue;
                        Block block = world.getBlockAt(blockLoc);
                        Material type = block.getType();

                        if (type == Material.BEDROCK)
                            continue;

                        boolean isShell = dist >= (radius - shellThickness);
                        boolean isCore = !isShell;

                        if (isCore) {
                            if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR) {
                                if (craterData != null)
                                    craterData.originalBlocks.putIfAbsent(blockLoc.clone(), type);

                                if (type.isSolid() && random.nextDouble() < 0.15) {
                                    world.spawnParticle(Particle.BLOCK, blockLoc.clone().add(0.5, 0.5, 0.5),
                                            2, 0.2, 0.2, 0.2, 0, type.createBlockData());
                                }
                                block.setType(Material.AIR, false);
                            }
                        } else {
                            if (!type.isSolid() && type != Material.WATER && type != Material.LAVA)
                                continue;

                            if (craterData != null)
                                craterData.originalBlocks.putIfAbsent(blockLoc.clone(), type);

                            double scorchedChance = 0.55 + depthRatio * 0.3;
                            if (random.nextDouble() >= scorchedChance)
                                continue;

                            Material wallMaterial = selectWallMaterial(random, depthRatio, dist, radius);
                            block.setType(wallMaterial, false);
                        }
                    }
                }
            }
        }

        private Material selectWallMaterial(ThreadLocalRandom random, double depthRatio, double dist, double radius) {
            double normalizedDist = dist / radius;

            if (depthRatio > 0.7) {
                double roll = random.nextDouble();
                if (roll < 0.25)
                    return Material.MAGMA_BLOCK;
                if (roll < 0.5)
                    return Material.BLACKSTONE;
                return Material.COBBLED_DEEPSLATE;
            } else if (depthRatio > 0.3) {
                double roll = random.nextDouble();
                if (roll < 0.15)
                    return Material.MAGMA_BLOCK;
                if (roll < 0.35)
                    return Material.DEEPSLATE;
                if (roll < 0.6)
                    return Material.BLACKSTONE;
                return Material.COBBLESTONE;
            } else {
                double roll = random.nextDouble();
                if (roll < 0.08)
                    return Material.MAGMA_BLOCK;
                if (roll < 0.20)
                    return Material.GILDED_BLACKSTONE;
                if (roll < 0.45)
                    return Material.COBBLESTONE;
                if (roll < 0.7)
                    return Material.DEEPSLATE;
                return METEOR_CRUST_MATERIALS[random.nextInt(METEOR_CRUST_MATERIALS.length)];
            }
        }

        private void preScanCraterVolume(Location impactPoint, int totalSpheres, double maxRadius,
                CraterData craterData) {
            World world = impactPoint.getWorld();
            if (world == null)
                return;

            for (int i = 0; i < totalSpheres; i++) {
                double depthRatio = (double) i / totalSpheres;
                double currentRadius = maxRadius * Math.sqrt(1.0 - depthRatio * 0.75);
                currentRadius = Math.max(1.5, currentRadius);
                Location sphereCenter = computeSphereCenterAlongPath(impactPoint, i, totalSpheres);

                int r = (int) Math.ceil(currentRadius) + 1;
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            if (Math.sqrt(x * x + y * y + z * z) > currentRadius + 1.5)
                                continue;
                            Location blockLoc = sphereCenter.clone().add(x, y, z);
                            if (isLocationProtected(blockLoc))
                                continue;
                            Block block = world.getBlockAt(blockLoc);
                            Material type = block.getType();
                            if (type != Material.AIR && type != Material.CAVE_AIR
                                    && type != Material.VOID_AIR && type != Material.BEDROCK) {
                                craterData.originalBlocks.putIfAbsent(blockLoc.clone(), type);
                            }
                        }
                    }
                }
            }
        }

        private void placeMeteorCore(Location center, double yaw, double pitch, CraterData craterData) {
            World world = center.getWorld();
            if (world == null)
                return;

            ThreadLocalRandom random = ThreadLocalRandom.current();
            double crustRadius = meteorSize * 0.65;

            for (double x = -crustRadius; x <= crustRadius; x += 0.8) {
                for (double y = -crustRadius; y <= crustRadius; y += 0.8) {
                    for (double z = -crustRadius; z <= crustRadius; z += 0.8) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance < crustRadius * 0.35 || distance > crustRadius)
                            continue;
                        if (random.nextDouble() >= 0.75)
                            continue;

                        Location blockLoc = center.clone().add(rotateOffset(new Vector(x, y, z), yaw, pitch));
                        if (isLocationProtected(blockLoc))
                            continue;
                        Block block = world.getBlockAt(blockLoc);
                        if (block.getType() == Material.BEDROCK || !block.getType().isAir())
                            continue;

                        double materialRoll = random.nextDouble();
                        Material crustMaterial;
                        if (materialRoll < 0.15)
                            crustMaterial = Material.MAGMA_BLOCK;
                        else if (materialRoll < 0.25)
                            crustMaterial = Material.GILDED_BLACKSTONE;
                        else
                            crustMaterial = METEOR_CRUST_MATERIALS[random.nextInt(METEOR_CRUST_MATERIALS.length)];

                        block.setType(crustMaterial, false);
                        if (craterData != null)
                            craterData.originalBlocks.remove(blockLoc);
                    }
                }
            }

            int coreVoxelsToPlace = (int) (meteorVoxels.size() * 0.6);
            double coreThreshold = meteorSize * 0.4;

            for (int i = 0; i < coreVoxelsToPlace; i++) {
                MeteorVoxel voxel = meteorVoxels.get(i);
                if (voxel.offset.length() > coreThreshold)
                    continue;

                Location blockLoc = center.clone().add(rotateOffset(voxel.offset.clone(), yaw, pitch));
                if (isLocationProtected(blockLoc))
                    continue;
                Block block = world.getBlockAt(blockLoc);
                if (block.getType() == Material.BEDROCK)
                    continue;

                block.setType(random.nextDouble() < 0.4 ? selectOreMaterial()
                        : METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)], false);
                if (craterData != null)
                    craterData.originalBlocks.remove(blockLoc);
            }

            world.spawnParticle(Particle.END_ROD, center, 10, 0.8, 0.8, 0.8, 0.01);
            world.spawnParticle(Particle.FLAME, center, 8, 0.6, 0.6, 0.6, 0.03);
        }

        private void performPostCraterWork(Location impactPoint, CraterData craterData) {
            World world = impactPoint.getWorld();
            if (world == null)
                return;

            clearVegetationAndFloatingObjects(impactPoint);
            createScorchedSurface(impactPoint, craterData);
            cleanupFloatingBlocks(impactPoint);
        }

        private void clearVegetationAndFloatingObjects(Location center) {
            World world = center.getWorld();
            int radius = Math.max(6, meteorSize + 4);
            int craterDepth = meteorSize * 2 + 8;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -craterDepth; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.sqrt(x * x + z * z) > radius)
                            continue;
                        Location blockLoc = center.clone().add(x, y, z);
                        if (isLocationProtected(blockLoc))
                            continue;
                        Block block = world.getBlockAt(blockLoc);
                        Material type = block.getType();

                        boolean shouldClear = isVegetation(type)
                                || type == Material.FIRE || type == Material.SOUL_FIRE
                                || type.name().contains("TORCH") || type.name().contains("SIGN")
                                || type.name().contains("BANNER")
                                || type == Material.SNOW || type == Material.POWDER_SNOW;

                        if (shouldClear) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        private void createScorchedSurface(Location center, CraterData craterData) {
            World world = center.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int outerRadius = Math.max(4, (int) (meteorSize * 1.4));

            for (int x = -outerRadius; x <= outerRadius; x++) {
                for (int z = -outerRadius; z <= outerRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance > outerRadius)
                        continue;

                    double chance = 0.7 * (1.0 - distance / outerRadius);
                    if (random.nextDouble() >= chance)
                        continue;

                    int groundY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                    for (int dy = 0; dy >= -2; dy--) {
                        Location blockLoc = new Location(world, center.getBlockX() + x, groundY + dy,
                                center.getBlockZ() + z);
                        if (isLocationProtected(blockLoc))
                            continue;
                        Block block = world.getBlockAt(blockLoc);
                        Material type = block.getType();

                        if (!type.isSolid() || type == Material.BEDROCK)
                            continue;
                        if (craterData != null)
                            craterData.originalBlocks.putIfAbsent(blockLoc.clone(), type);

                        if (random.nextDouble() >= (dy == 0 ? 0.7 : 0.35))
                            continue;

                        double roll = random.nextDouble();
                        Material scorchedMat;
                        if (distance <= outerRadius * 0.4) {
                            if (roll < 0.25)
                                scorchedMat = Material.MAGMA_BLOCK;
                            else if (roll < 0.5)
                                scorchedMat = Material.BLACKSTONE;
                            else
                                scorchedMat = Material.DEEPSLATE;
                        } else if (distance <= outerRadius * 0.7) {
                            if (roll < 0.12)
                                scorchedMat = Material.MAGMA_BLOCK;
                            else if (roll < 0.35)
                                scorchedMat = Material.DEEPSLATE;
                            else
                                scorchedMat = Material.COBBLESTONE;
                        } else {
                            scorchedMat = roll < 0.7 ? Material.COBBLESTONE : Material.DEEPSLATE;
                        }
                        block.setType(scorchedMat, false);
                    }

                    if (distance <= outerRadius * 0.6 && random.nextDouble() < 0.1) {
                        int groundY2 = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                        Location fireLoc = new Location(world, center.getBlockX() + x, groundY2 + 1,
                                center.getBlockZ() + z);
                        if (isLocationProtected(fireLoc))
                            continue;
                        Block fireBlock = world.getBlockAt(fireLoc);
                        Block below = world.getBlockAt(center.getBlockX() + x, groundY2, center.getBlockZ() + z);

                        if (fireBlock.getType().isAir() && below.getType().isSolid()) {
                            fireBlock.setType(Material.FIRE, false);
                            int burnTime = 150 + random.nextInt(350);
                            Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
                                if (fireBlock.getType() != Material.FIRE)
                                    return;
                                if (!world.getBlockAt(fireBlock.getLocation().subtract(0, 1, 0)).getType().isSolid()) {
                                    fireBlock.setType(Material.AIR, false);
                                    return;
                                }
                                if (random.nextDouble() >= 0.5)
                                    fireBlock.setType(Material.AIR, false);
                            }, burnTime);
                        }
                    }
                }
            }
        }

        private void cleanupFloatingBlocks(Location center) {
            World world = center.getWorld();
            if (world == null)
                return;

            int scanRadius = (int) (meteorSize * 1.4) + 2;
            int scanDepth = meteorSize * 2 + 10;

            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int y = -scanDepth; y <= 2; y++) {
                    for (int z = -scanRadius; z <= scanRadius; z++) {
                        if (Math.sqrt(x * x + z * z) > scanRadius)
                            continue;

                        Location blockLoc = center.clone().add(x, y, z);
                        if (isLocationProtected(blockLoc))
                            continue;
                        Block block = world.getBlockAt(blockLoc);
                        Material type = block.getType();

                        if (!type.isSolid() || type == Material.BEDROCK)
                            continue;
                        if (isMeteorMaterial(type))
                            continue;

                        int airNeighbors = 0;
                        int[][] offsets = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
                                { 0, 0, -1 } };
                        for (int[] off : offsets) {
                            Material neighbor = world.getBlockAt(
                                    blockLoc.getBlockX() + off[0],
                                    blockLoc.getBlockY() + off[1],
                                    blockLoc.getBlockZ() + off[2]).getType();
                            if (neighbor == Material.AIR || neighbor == Material.CAVE_AIR) {
                                airNeighbors++;
                            }
                        }

                        if (airNeighbors >= 5) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        private boolean isMeteorMaterial(Material material) {
            return switch (material) {
                case DEEPSLATE, BLACKSTONE, COBBLED_DEEPSLATE, MAGMA_BLOCK, GILDED_BLACKSTONE,
                        DIAMOND_ORE, EMERALD_ORE, ANCIENT_DEBRIS, GOLD_ORE, IRON_ORE, COAL_ORE ->
                    true;
                default -> false;
            };
        }

        private Vector rotateOffset(Vector o, double yaw, double pitch) {
            double cy = Math.cos(yaw), sy = Math.sin(yaw);
            double x1 = o.getX() * cy - o.getZ() * sy;
            double z1 = o.getX() * sy + o.getZ() * cy;
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            return new Vector(x1, o.getY() * cp - z1 * sp, o.getY() * sp + z1 * cp);
        }

        private Material selectOreMaterial() {
            double rand = ThreadLocalRandom.current().nextDouble(100);
            rand -= Feature.METEOR_RAIN.getDouble("ancient-debris-rate");
            if (rand < 0)
                return Material.ANCIENT_DEBRIS;
            rand -= Feature.METEOR_RAIN.getDouble("emerald-ore-rate");
            if (rand < 0)
                return Material.EMERALD_ORE;
            rand -= Feature.METEOR_RAIN.getDouble("diamond-ore-rate");
            if (rand < 0)
                return Material.DIAMOND_ORE;
            rand -= Feature.METEOR_RAIN.getDouble("gold-ore-rate");
            if (rand < 0)
                return Material.GOLD_ORE;
            rand -= Feature.METEOR_RAIN.getDouble("iron-ore-rate");
            if (rand < 0)
                return Material.IRON_ORE;
            return Material.COAL_ORE;
        }

        @Override
        public void cancel() {
            super.cancel();
            try {
                clearAllFakeBlocks();
            } catch (Exception ignored) {
            }
            activeMeteors.remove(this.meteorId);
        }

        private List<MeteorVoxel> generateMeteorVoxels(int size, UUID seed, MeteorShape shape) {
            Random random = new Random(seed.getMostSignificantBits() ^ seed.getLeastSignificantBits());
            return switch (shape) {
                case ARROW -> generateArrowMeteor(size, random);
                case IRREGULAR -> generateIrregularMeteor(size, random);
                case TAILED -> generateTailedMeteor(size, random);
                case COMPLEX -> generateComplexMeteor(size, random);
                default -> generateSphericalMeteor(size, random);
            };
        }

        private List<MeteorVoxel> generateSphericalMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            double rx = size * (0.55 + random.nextDouble() * 0.25);
            double ry = size * (0.50 + random.nextDouble() * 0.25);
            double rz = size * (0.55 + random.nextDouble() * 0.25);
            int maxX = (int) Math.ceil(rx) + 1, maxY = (int) Math.ceil(ry) + 1, maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double nx = x / rx, ny = y / ry, nz = z / rz;
                        double noise = (random.nextDouble() * 2 - 1) * 0.12;
                        if (nx * nx + ny * ny + nz * nz <= 1.0 + noise && random.nextDouble() < 0.85) {
                            voxels.add(new MeteorVoxel(new Vector(x, y, z),
                                    WrappedBlockData
                                            .createData(METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)]
                                                    .createBlockData())));
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateArrowMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            double rx = size * (0.35 + random.nextDouble() * 0.2);
            double ry = size * (0.35 + random.nextDouble() * 0.2);
            double rz = size * (1.0 + random.nextDouble() * 0.5);
            int maxX = (int) Math.ceil(rx) + 1, maxY = (int) Math.ceil(ry) + 1, maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double tapering = z < 0 ? 1.0 + (z / rz) * 0.8 : 1.0;
                        double nx = x / (rx * tapering), ny = y / (ry * tapering), nz = z / rz;
                        double noise = (random.nextDouble() * 2 - 1) * 0.1;
                        if (nx * nx + ny * ny + nz * nz <= 1.0 + noise && random.nextDouble() < 0.82) {
                            voxels.add(new MeteorVoxel(new Vector(x, y, z),
                                    WrappedBlockData
                                            .createData(METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)]
                                                    .createBlockData())));
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateIrregularMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            int numChunks = 2 + random.nextInt(3);
            for (int chunk = 0; chunk < numChunks; chunk++) {
                Vector chunkCenter = new Vector(
                        random.nextDouble(-size * 0.6, size * 0.6),
                        random.nextDouble(-size * 0.6, size * 0.6),
                        random.nextDouble(-size * 0.6, size * 0.6));
                double chunkSize = size * (0.3 + random.nextDouble() * 0.4);
                double rx = chunkSize * (0.5 + random.nextDouble() * 0.5);
                double ry = chunkSize * (0.5 + random.nextDouble() * 0.5);
                double rz = chunkSize * (0.5 + random.nextDouble() * 0.5);
                int maxX = (int) Math.ceil(rx + Math.abs(chunkCenter.getX())) + 1;
                int maxY = (int) Math.ceil(ry + Math.abs(chunkCenter.getY())) + 1;
                int maxZ = (int) Math.ceil(rz + Math.abs(chunkCenter.getZ())) + 1;
                for (int x = -maxX; x <= maxX; x++) {
                    for (int y = -maxY; y <= maxY; y++) {
                        for (int z = -maxZ; z <= maxZ; z++) {
                            Vector pos = new Vector(x, y, z).subtract(chunkCenter);
                            double nx = pos.getX() / rx, ny = pos.getY() / ry, nz = pos.getZ() / rz;
                            double noise = (random.nextDouble() * 2 - 1) * 0.25;
                            if (nx * nx + ny * ny + nz * nz <= 1.0 + noise && random.nextDouble() < 0.75) {
                                voxels.add(new MeteorVoxel(new Vector(x, y, z),
                                        WrappedBlockData
                                                .createData(METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)]
                                                        .createBlockData())));
                            }
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateTailedMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            double headSize = size * 0.8;
            double rx = headSize * (0.55 + random.nextDouble() * 0.25);
            double ry = headSize * (0.50 + random.nextDouble() * 0.25);
            double rz = headSize * (0.35 + random.nextDouble() * 0.25);
            int maxX = (int) Math.ceil(rx) + 1, maxY = (int) Math.ceil(ry) + 1, maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double nx = x / rx, ny = y / ry, nz = z / rz;
                        if (nx * nx + ny * ny + nz * nz <= 1.0 && random.nextDouble() < 0.88) {
                            voxels.add(new MeteorVoxel(new Vector(x, y, z),
                                    WrappedBlockData
                                            .createData(METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)]
                                                    .createBlockData())));
                        }
                    }
                }
            }
            int tailLength = Math.max(3, size + random.nextInt(size));
            for (int t = 1; t <= tailLength; t++) {
                double tailFactor = 1.0 - (double) t / tailLength;
                double tailRadius = Math.max(1, rx * tailFactor * 0.6);
                for (int x = (int) -tailRadius; x <= tailRadius; x++) {
                    for (int y = (int) -tailRadius; y <= tailRadius; y++) {
                        if (Math.sqrt(x * x + y * y) <= tailRadius && random.nextDouble() < 0.4 * tailFactor) {
                            voxels.add(new MeteorVoxel(new Vector(x, y, maxZ + t),
                                    WrappedBlockData
                                            .createData(METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)]
                                                    .createBlockData())));
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateComplexMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>(generateSphericalMeteor(size, random));
            int numProtrusions = 2 + random.nextInt(3);
            for (int i = 0; i < numProtrusions; i++) {
                double angle1 = random.nextDouble() * 2 * Math.PI;
                double angle2 = random.nextDouble() * Math.PI;
                Vector direction = new Vector(
                        Math.cos(angle1) * Math.sin(angle2),
                        Math.cos(angle2),
                        Math.sin(angle1) * Math.sin(angle2)).normalize();
                double protrusionSize = size * (0.3 + random.nextDouble() * 0.4);
                int protrusionLength = (int) (protrusionSize * (1.0 + random.nextDouble()));
                for (int j = 1; j <= protrusionLength; j++) {
                    double shrinkFactor = 1.0 - (double) j / protrusionLength * 0.8;
                    double radius = Math.max(1, protrusionSize * 0.3 * shrinkFactor);
                    Vector basePos = direction.clone().multiply(size * 0.7 + j);
                    for (int x = (int) -radius; x <= radius; x++) {
                        for (int y = (int) -radius; y <= radius; y++) {
                            for (int z = (int) -radius; z <= radius; z++) {
                                if (Math.sqrt(x * x + y * y + z * z) <= radius && random.nextDouble() < 0.7) {
                                    voxels.add(new MeteorVoxel(basePos.clone().add(new Vector(x, y, z)),
                                            WrappedBlockData.createData(
                                                    METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)]
                                                            .createBlockData())));
                                }
                            }
                        }
                    }
                }
            }
            return voxels;
        }
    }

    private void startCraterRecoveryProcess() {
        List<CraterData> cratersToProcess = new ArrayList<>(cratersToRecover);
        cratersToRecover.clear();
        for (CraterData craterData : cratersToProcess) {
            long delayInTicks = Math.max(0, craterData.recoveryStartTime - System.currentTimeMillis()) / 50;
            Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(),
                    () -> scheduleCraterRecovery(craterData), delayInTicks);
        }
    }

    private void scheduleCraterRecovery(CraterData craterData) {
        if (craterData.originalBlocks.isEmpty())
            return;

        int totalBlocks = craterData.originalBlocks.size();
        int blocksPerTick = Math.max(1, Math.min(3, totalBlocks / 300));

        List<Map.Entry<Location, Material>> blockList = new ArrayList<>(craterData.originalBlocks.entrySet());
        blockList.sort(Comparator.comparingInt(e -> e.getKey().getBlockY()));

        ThreadLocalRandom random = ThreadLocalRandom.current();

        new BukkitRunnable() {
            int currentIndex = 0;

            @Override
            public void run() {
                if (currentIndex >= blockList.size()) {
                    cancel();
                    return;
                }

                int endIndex = Math.min(currentIndex + Math.max(1, blocksPerTick + random.nextInt(-1, 2)),
                        blockList.size());
                for (int i = currentIndex; i < endIndex; i++) {
                    Map.Entry<Location, Material> entry = blockList.get(i);
                    Location loc = entry.getKey();
                    Block block = loc.getBlock();
                    Material original = entry.getValue();

                    if (block.getType() != original && !isValuableOre(block.getType())) {
                        block.setType(original, false);
                        if (random.nextDouble() < 0.1) {
                            World world = loc.getWorld();
                            if (world != null) {
                                world.spawnParticle(Particle.BLOCK, loc.clone().add(0.5, 0.5, 0.5),
                                        1, 0.2, 0.2, 0.2, 0, original.createBlockData());
                            }
                        }
                    }
                }
                currentIndex = endIndex;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 200L, 15L + random.nextInt(10));
    }

    private boolean isValuableOre(Material material) {
        return switch (material) {
            case DIAMOND_ORE, EMERALD_ORE, ANCIENT_DEBRIS, GOLD_ORE, IRON_ORE, COAL_ORE -> true;
            default -> false;
        };
    }
}