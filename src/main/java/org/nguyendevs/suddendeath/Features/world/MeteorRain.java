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
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Hook.CustomFlag;
import org.nguyendevs.suddendeath.Managers.EventManager.WorldStatus;
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

    private static class CraterData {
        Location center;
        Map<Location, Material> originalBlocks;
        int radius;
        long impactTime;

        CraterData(Location center, int radius) {
            this.center = center;
            this.radius = radius;
            this.originalBlocks = new HashMap<>();
            this.impactTime = System.currentTimeMillis();
        }
    }

    private final List<CraterData> cratersToRecover = new ArrayList<>();

    private int spawningTaskId = -1;

    public MeteorRain(World world) {
        super(world, 20 * 10, WorldStatus.METEOR_RAIN);
        Bukkit.getPluginManager().registerEvents(this, SuddenDeath.getInstance());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        if (e.getWorld().equals(getWorld())) close();
    }

    @Override
    public void run() {
        try {
            long time = getWorld().getTime();
            if (time < 13000 || time > 23000) {
                stopSpawning();
                return;
            }
            if (spawningTaskId == -1) startSpawning();
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
            List<MeteorTask> tasks = new ArrayList<>(activeMeteors.values());
            for (MeteorTask t : tasks) t.cancel();
            activeMeteors.clear();
            clearAllPhantomBlocks();

            boolean craterRecovery = Feature.METEOR_RAIN.getBoolean("crater-recovery");
            if (craterRecovery && !cratersToRecover.isEmpty()) {
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
        if (world == null) return;

        for (Map.Entry<BlockPosition, Set<UUID>> entry : phantomBlockTracking.entrySet()) {
            BlockPosition pos = entry.getKey();
            for (Player player : world.getPlayers()) {
                sendBlockChange(player, pos, WrappedBlockData.createData(Material.AIR.createBlockData()));
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
        } catch (Exception ignored) {}
    }

    private boolean isProtected(Player p) {
        if (p == null || !p.isOnline()) return true;
        GameMode gm = p.getGameMode();
        return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
    }

    private void spawnMeteorShower() {
        try {
            List<Player> worldPlayers = getWorld().getPlayers();
            if (worldPlayers.isEmpty()) return;

            List<Player> eligible = new ArrayList<>();
            for (Player p : worldPlayers) if (!isProtected(p)) eligible.add(p);
            if (eligible.isEmpty()) return;

            int meteorCount = ThreadLocalRandom.current().nextInt(1, 5);
            for (int i = 0; i < meteorCount; i++) {
                Player targetPlayer = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
                long delay = i * ThreadLocalRandom.current().nextInt(3, 11);
                Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(),
                        () -> spawnMeteorNearPlayer(targetPlayer), delay);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error spawning meteor shower", e);
        }
    }

    private void spawnMeteorNearPlayer(Player targetPlayer) {
        try {
            if (isProtected(targetPlayer)) return;

            Location playerLoc = targetPlayer.getLocation();
            double distance = ThreadLocalRandom.current().nextDouble(10, 60);
            double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);

            Location targetLocation = playerLoc.clone().add(offsetX, 0, offsetZ);
            int groundY = getWorld().getHighestBlockYAt(targetLocation);
            targetLocation.setY(groundY);

            if (!SuddenDeath.getInstance().getWorldGuard()
                    .isFlagAllowedAtLocation(targetLocation, CustomFlag.SDS_EVENT)) return;

            Vector diagonalDirection = generateRandomDiagonalDirection();
            double spawnDistance = ThreadLocalRandom.current().nextDouble(80, 120);

            Location startLocation = targetLocation.clone()
                    .add(diagonalDirection.getX() * spawnDistance,
                            SPAWN_HEIGHT,
                            diagonalDirection.getZ() * spawnDistance);

            int meteorSize = ThreadLocalRandom.current().nextInt(MIN_METEOR_SIZE, MAX_METEOR_SIZE + 1);
            UUID meteorId = UUID.randomUUID();

            MeteorTask meteorTask = new MeteorTask(startLocation, targetLocation, meteorSize, meteorId);
            activeMeteors.put(meteorId, meteorTask);
            meteorTask.runTaskTimer(SuddenDeath.getInstance(), 0L, 1L);

        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error spawning meteor near player", e);
        }
    }

    private Vector generateRandomDiagonalDirection() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.3, 2 * Math.PI - 0.3);
        double[] avoidAngles = {0, Math.PI/2, Math.PI, 3*Math.PI/2};
        for (double avoidAngle : avoidAngles) {
            if (Math.abs(angle - avoidAngle) < 0.2) angle += 0.3;
        }
        double x = Math.cos(angle);
        double z = Math.sin(angle);
        return new Vector(x, 0, z).normalize();
    }

    private static boolean inHorizontalRange(Player p, Location c, int range) {
        if (!p.getWorld().equals(c.getWorld())) return false;
        Location pl = p.getLocation();
        double dx = pl.getX() - c.getX();
        double dz = pl.getZ() - c.getZ();
        return (dx * dx + dz * dz) <= (range * range);
    }

    private enum MeteorShape {
        SPHERICAL,
        ARROW,
        IRREGULAR,
        TAILED,
        COMPLEX
    }

    private class MeteorTask extends BukkitRunnable {
        private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        private final Location startLocation;
        private final Location targetLocation;
        private final int meteorSize;
        private final UUID meteorId;
        private final Vector flightDirection;
        private final MeteorShape meteorShape;
        private Location currentLocation;
        private int tickCount = 0;
        private boolean hasImpacted = false;
        private final double speed;
        private final Map<UUID, Set<BlockPosition>> lastSentPerPlayer = new HashMap<>();
        private final List<MeteorVoxel> meteorVoxels;
        private final int MAX_TICKS = 20 * 40;
        private final Map<Location, Material> destroyedBlocksInFlight = new HashMap<>();



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
                    baseDirection.getZ() + randomOffset
            ).normalize();
            this.speed = ThreadLocalRandom.current().nextDouble(2.0, 3.5);
            this.meteorVoxels = generateMeteorVoxels(size, meteorId, meteorShape);
        }

        private MeteorShape selectRandomMeteorShape(int size) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (size >= 9) {
                double rand = random.nextDouble();
                if (rand < 0.25) return MeteorShape.COMPLEX;
                if (rand < 0.45) return MeteorShape.IRREGULAR;
                if (rand < 0.65) return MeteorShape.TAILED;
                if (rand < 0.85) return MeteorShape.ARROW;
                return MeteorShape.SPHERICAL;
            } else if (size >= 6) {
                double rand = random.nextDouble();
                if (rand < 0.2) return MeteorShape.COMPLEX;
                if (rand < 0.4) return MeteorShape.IRREGULAR;
                if (rand < 0.6) return MeteorShape.TAILED;
                if (rand < 0.8) return MeteorShape.ARROW;
                return MeteorShape.SPHERICAL;
            } else {
                double rand = random.nextDouble();
                if (rand < 0.1) return MeteorShape.COMPLEX;
                if (rand < 0.3) return MeteorShape.IRREGULAR;
                if (rand < 0.5) return MeteorShape.TAILED;
                if (rand < 0.7) return MeteorShape.ARROW;
                return MeteorShape.SPHERICAL;
            }
        }

        @Override
        public void run() {
            try {
                if (hasImpacted || tickCount++ > MAX_TICKS) {
                    if (!hasImpacted) impact(currentLocation.clone());
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
                            FluidCollisionMode.NEVER, true
                    );
                    if (r != null && r.getHitPosition() != null) {
                        Block hitBlock = r.getHitBlock();
                        if (hitBlock != null && isSolidTerrain(hitBlock.getType())) {

                            saveAndDestroyBlocksInPath(prev, currentLocation);

                            Location hit = new Location(w,
                                    r.getHitPosition().getX(),
                                    r.getHitPosition().getY(),
                                    r.getHitPosition().getZ());
                            hasImpacted = true;
                            impact(hit);
                            cancel();
                            return;
                        }
                    }
                    checkAndDestroyBlocksAlongPath();
                }

                createTrailEffect();
                if (tickCount % 6 == 0) playMeteorFlightSound();
                if (tickCount % 2 == 0) renderFakeMeteor();

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
            if (world == null) return;

            int checkRadius = Math.max(2, meteorSize / 3);

            for (int x = -checkRadius; x <= checkRadius; x++) {
                for (int y = -checkRadius; y <= checkRadius; y++) {
                    for (int z = -checkRadius; z <= checkRadius; z++) {
                        double distance = Math.sqrt(x * x + y * y + z * z);

                        if (distance <= checkRadius * 0.7) {
                            Location blockLoc = currentLocation.clone().add(x, y, z);
                            Block block = world.getBlockAt(blockLoc);

                            if (isSolidTerrain(block.getType()) &&
                                    block.getType() != Material.BEDROCK &&
                                    !destroyedBlocksInFlight.containsKey(blockLoc)) {

                                destroyedBlocksInFlight.put(blockLoc.clone(), block.getType());

                                block.setType(Material.AIR, false);

                                world.spawnParticle(Particle.BLOCK_CRACK,
                                        blockLoc.add(0.5, 0.5, 0.5), 3,
                                        0.3, 0.3, 0.3, 0, block.getBlockData());
                            }
                        }
                    }
                }
            }
        }

        private void saveAndDestroyBlocksInPath(Location start, Location end) {
            World world = start.getWorld();
            if (world == null) return;

            Vector direction = end.toVector().subtract(start.toVector());
            double distance = direction.length();
            direction.normalize();

            int checkRadius = Math.max(2, meteorSize / 3);
            int steps = (int) Math.ceil(distance);

            for (int step = 0; step < steps; step++) {
                Location checkLoc = start.clone().add(direction.clone().multiply(step));

                for (int x = -checkRadius; x <= checkRadius; x++) {
                    for (int y = -checkRadius; y <= checkRadius; y++) {
                        for (int z = -checkRadius; z <= checkRadius; z++) {
                            double blockDist = Math.sqrt(x * x + y * y + z * z);

                            if (blockDist <= checkRadius * 0.7) {
                                Location blockLoc = checkLoc.clone().add(x, y, z);
                                Block block = world.getBlockAt(blockLoc);

                                if (isSolidTerrain(block.getType()) &&
                                        block.getType() != Material.BEDROCK &&
                                        !destroyedBlocksInFlight.containsKey(blockLoc)) {

                                    destroyedBlocksInFlight.put(blockLoc.clone(), block.getType());
                                    block.setType(Material.AIR, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void createTrailEffect() {
            World world = currentLocation.getWorld();
            if (world == null) return;
            double offset = meteorSize / 2.0;
            world.spawnParticle(Particle.FLAME, currentLocation, 20, offset, offset, offset, 0.03);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation, 15, offset, offset, offset, 0.05);
            world.spawnParticle(Particle.LAVA, currentLocation, 8, offset / 2, offset / 2, offset / 2, 0);
            world.spawnParticle(Particle.CRIT, currentLocation, 10, offset, offset, offset, 0.1);
            world.spawnParticle(Particle.EXPLOSION_NORMAL, currentLocation, 5, offset / 3, offset / 3, offset / 3, 0.02);
        }

        private void playMeteorFlightSound() {
            World world = currentLocation.getWorld();
            if (world == null) return;
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(currentLocation) <= SOUND_RADIUS) {
                    player.playSound(currentLocation, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.AMBIENT, 0.8f, 0.6f);
                    player.playSound(currentLocation, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.AMBIENT, 1.0f, 0.4f);
                    player.playSound(currentLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.AMBIENT, 0.5f, 0.8f);
                }
            }
        }

        private void renderFakeMeteor() {
            World world = currentLocation.getWorld();
            if (world == null) return;

            Map<BlockPosition, WrappedBlockData> currentBlocks = new HashMap<>();

            for (MeteorVoxel voxel : meteorVoxels) {
                Location blockLoc = currentLocation.clone().add(voxel.offset);
                BlockPosition pos = new BlockPosition(
                        blockLoc.getBlockX(),
                        blockLoc.getBlockY(),
                        blockLoc.getBlockZ()
                );
                currentBlocks.put(pos, voxel.data);
            }

            for (Player player : world.getPlayers()) {
                if (!inHorizontalRange(player, currentLocation, VIEW_RANGE)) continue;

                Set<BlockPosition> lastSent = lastSentPerPlayer.computeIfAbsent(
                        player.getUniqueId(), k -> new HashSet<>()
                );

                for (BlockPosition oldPos : lastSent) {
                    sendBlockChange(player, oldPos, WrappedBlockData.createData(Material.AIR.createBlockData()));
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
            if (players != null) {
                players.remove(playerId);
                if (players.isEmpty()) {
                    phantomBlockTracking.remove(pos);
                }
            }
        }

        private void clearAllFakeBlocks() {
            for (Map.Entry<UUID, Set<BlockPosition>> entry : lastSentPerPlayer.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) continue;

                for (BlockPosition pos : entry.getValue()) {
                    sendBlockChange(player, pos, WrappedBlockData.createData(Material.AIR.createBlockData()));
                    removePhantomBlockTracking(pos, entry.getKey());
                }
                entry.getValue().clear();
            }
        }

        private void impact(Location impactPoint) {
            try {
                World world = impactPoint.getWorld();
                if (world == null) return;

                clearAllFakeBlocks();

                world.playSound(impactPoint, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 2.5f, 0.5f);
                world.playSound(impactPoint, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 1.5f, 0.7f);
                world.playSound(impactPoint, Sound.BLOCK_ANVIL_PLACE, SoundCategory.AMBIENT, 1.2f, 0.4f);

                world.spawnParticle(Particle.EXPLOSION_LARGE, impactPoint, 10, 3, 3, 3, 0);
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
            if (world == null) return loc.getBlockY();

            for (int y = loc.getBlockY(); y >= world.getMinHeight(); y--) {
                Material mat = world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType();
                if (isSolidTerrain(mat)) {
                    return y;
                }
            }
            return world.getMinHeight();
        }

        private boolean isSolidTerrain(Material material) {
            return material.isSolid() &&
                    material != Material.WATER &&
                    !material.name().contains("LEAVES") &&
                    !material.name().contains("LOG") &&
                    !material.name().contains("WOOD") &&
                    !isVegetation(material);
        }

        private boolean isVegetation(Material material) {
            return material == Material.SHORT_GRASS ||
                    material == Material.TALL_GRASS ||
                    material == Material.FERN ||
                    material == Material.LARGE_FERN ||
                    material == Material.DEAD_BUSH ||
                    material == Material.DANDELION ||
                    material == Material.POPPY ||
                    material == Material.BLUE_ORCHID ||
                    material == Material.ALLIUM ||
                    material == Material.AZURE_BLUET ||
                    material == Material.RED_TULIP ||
                    material == Material.ORANGE_TULIP ||
                    material == Material.WHITE_TULIP ||
                    material == Material.PINK_TULIP ||
                    material == Material.OXEYE_DAISY ||
                    material == Material.SUNFLOWER ||
                    material == Material.LILAC ||
                    material == Material.ROSE_BUSH ||
                    material == Material.PEONY ||
                    material == Material.GLOWSTONE ||
                    material.name().contains("SAPLING") ||
                    material.name().contains("MUSHROOM");
        }

        private void applyShockwaveAt(Location center) {
            World world = center.getWorld();
            if (world == null) return;

            double knockRadius = meteorSize * 2.5;

            for (Entity entity : world.getNearbyEntities(center, knockRadius, knockRadius, knockRadius)) {
                if (!(entity instanceof LivingEntity livingEntity)) continue;
                if (entity instanceof ArmorStand) continue;

                double distance = livingEntity.getLocation().distance(center);
                if (distance <= knockRadius) {
                    Vector direction = livingEntity.getLocation().toVector().subtract(center.toVector()).normalize();

                    double strength = (1.0 - distance / knockRadius) * (meteorSize / 3.5) + 1.5;

                    livingEntity.setVelocity(direction.multiply(strength).add(new Vector(0, 0.8 + meteorSize * 0.04, 0)));

                    double baseDamage = 5.0 + meteorSize * 1.0;
                    double damage = Math.max(1.0, baseDamage * (1.0 - (distance / knockRadius)));
                    livingEntity.damage(damage);

                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 4));
                    livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 2));

                    world.spawnParticle(Particle.CRIT, livingEntity.getEyeLocation().add(0, 0.5, 0), 5, 0.3, 0.1, 0.3, 0.1);
                }
            }
        }

        private void createEnhancedMeteorCrater(Location impactPoint) {
            World world = impactPoint.getWorld();
            if (world == null) return;

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

            clearVegetationAndFloatingObjects(impactPoint);

            CraterData craterData = craterRecovery ? new CraterData(impactPoint, meteorSize) : null;

            if (craterData != null) {
                craterData.originalBlocks.putAll(destroyedBlocksInFlight);
            }

            createInstantDrillingCraterWithTracking(impactPoint, craterData);
            createMinimalScorchedArea(impactPoint);

            Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
                performFinalCleanup(impactPoint);

                if (craterRecovery && craterData != null && craterData.originalBlocks.size() > 0) {
                    cratersToRecover.add(craterData);
                }
            }, 60L);
        }

        private void performFinalCleanup(Location impactPoint) {
            World world = impactPoint.getWorld();
            if (world == null) return;

            int radius = Math.max(8, meteorSize + 6);

            for (int x = -radius; x <= radius; x++) {
                for (int y = -15; y <= 10; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        double distance = Math.sqrt(x * x + z * z);
                        if (distance > radius) continue;

                        Location loc = impactPoint.clone().add(x, y, z);
                        Block block = world.getBlockAt(loc);

                        if ((block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE)) {
                            Block below = world.getBlockAt(loc.clone().subtract(0, 1, 0));
                            if (!below.getType().isSolid()) block.setType(Material.AIR, false);
                        }
                    }
                }
            }

            smoothCraterEdges(impactPoint);
        }


        private void smoothCraterEdges(Location center) {
            World world = center.getWorld();
            if (world == null) return;

            ThreadLocalRandom random = ThreadLocalRandom.current();
            double smoothRadius = meteorSize * 1.5;

            for (double angle = 0; angle < 2 * Math.PI; angle += 0.1) {
                for (double r = smoothRadius * 0.8; r <= smoothRadius * 1.2; r += 0.3) {
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;

                    Location edgeLoc = center.clone().add(x, 0, z);
                    int groundY = world.getHighestBlockYAt(edgeLoc);

                    boolean isEdge = false;
                    for (int checkY = groundY - 1; checkY >= groundY - 5; checkY--) {
                        Block checkBlock = world.getBlockAt(edgeLoc.getBlockX(), checkY, edgeLoc.getBlockZ());
                        if (checkBlock.getType().isAir()) {
                            isEdge = true;
                            break;
                        }
                    }

                    if (isEdge && random.nextDouble() < 0.6) {
                        for (int step = 0; step < 3; step++) {
                            Location stepLoc = new Location(world,
                                    edgeLoc.getBlockX(),
                                    groundY - step,
                                    edgeLoc.getBlockZ());
                            Block stepBlock = world.getBlockAt(stepLoc);

                            if (stepBlock.getType().isAir() && random.nextDouble() < 0.4) {
                                Material material;
                                if (step == 0) {
                                    material = SCORCHED_MATERIALS[random.nextInt(SCORCHED_MATERIALS.length)];
                                } else {
                                    material = METEOR_CRUST_MATERIALS[random.nextInt(METEOR_CRUST_MATERIALS.length)];
                                }
                                stepBlock.setType(material, false);
                            }
                        }
                    }
                }
            }
        }

        private void clearVegetationAndFloatingObjects(Location center) {
            World world = center.getWorld();
            int radius = Math.max(6, meteorSize + 4);

            for (int x = -radius; x <= radius; x++) {
                for (int y = -12; y <= 12; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        double distance = Math.sqrt(x * x + z * z);
                        if (distance > radius) continue;

                        Location loc = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(loc);

                        if (isVegetation(block.getType()) ||
                                block.getType() == Material.FIRE ||
                                block.getType() == Material.SOUL_FIRE ||
                                block.getType().name().contains("TORCH") ||
                                block.getType().name().contains("SIGN") ||
                                block.getType().name().contains("BANNER") ||
                                block.getType() == Material.SNOW ||
                                block.getType() == Material.POWDER_SNOW ||
                                (!block.getType().isSolid() && block.getType() != Material.AIR && block.getType() != Material.WATER)) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }


        private boolean isStableTerrainMaterial(Material material) {
            return material == Material.STONE ||
                    material == Material.DEEPSLATE ||
                    material == Material.COBBLESTONE ||
                    material == Material.COBBLED_DEEPSLATE ||
                    material == Material.BLACKSTONE ||
                    material == Material.MAGMA_BLOCK ||
                    material == Material.BEDROCK ||
                    material == Material.DIRT ||
                    material == Material.GRASS_BLOCK ||
                    material == Material.ANDESITE ||
                    material == Material.GRANITE ||
                    material == Material.DIORITE ||
                    material.name().contains("_ORE");
        }

        private void createInstantDrillingCraterWithTracking(Location impactPoint, CraterData craterData) {
            World world = impactPoint.getWorld();
            if (world == null) return;

            Vector direction = flightDirection.clone().normalize();
            int totalDepth = meteorSize * 2 + 8;
            double maxRadius = meteorSize * 1.2;

            new BukkitRunnable() {
                int currentDepth = 0;
                double rotationAngle = 0;
                boolean craterComplete = false;

                @Override
                public void run() {
                    if (currentDepth >= totalDepth) {
                        if (!craterComplete) {
                            placeMeteorWithCrust(impactPoint, direction, totalDepth);
                            craterComplete = true;
                        }
                        cancel();
                        return;
                    }

                    for (int layer = 0; layer < 4 && currentDepth < totalDepth; layer++) {
                        double depthRatio = (double) currentDepth / totalDepth;
                        double currentRadius = maxRadius * (1.0 - depthRatio * 0.7);
                        if (currentRadius < 1.0) currentRadius = 1.0;

                        drillCraterLayerWithTracking(impactPoint, direction, currentDepth, currentRadius, rotationAngle, craterData);
                        currentDepth++;
                        rotationAngle += 0.15;
                    }

                    if (currentDepth % 6 == 0) {
                        Location drillPoint = impactPoint.clone().add(direction.clone().multiply(currentDepth * 0.8));
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, drillPoint, 2, 0.4, 0.4, 0.4, 0.01);

                        for (Player player : world.getPlayers()) {
                            if (player.getLocation().distance(drillPoint) <= 20) {
                                player.playSound(drillPoint, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.AMBIENT, 0.15f, 1.9f);
                            }
                        }
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 3L, 1L);
        }

        private void drillCraterLayerWithTracking(Location impactPoint, Vector direction, int depth,
                                                  double radius, double rotation, CraterData craterData) {
            World world = impactPoint.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            Location layerCenter = impactPoint.clone().add(direction.clone().multiply(depth * 0.8));

            if (depth < 4) {
                addCrustAndScorchedMaterials(layerCenter, radius * 1.8, depth);
            }

            Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
            Vector up = direction.clone().crossProduct(right).normalize();

            int numPoints = Math.max(12, (int) (radius * 4));
            for (int i = 0; i < numPoints; i++) {
                double angle = (2.0 * Math.PI * i / numPoints) + rotation;

                for (double r = 0; r <= radius; r += 0.5) {
                    double localX = Math.cos(angle) * r;
                    double localY = Math.sin(angle) * r;

                    Vector offset = right.clone().multiply(localX).add(up.clone().multiply(localY));
                    Location blockLoc = layerCenter.clone().add(offset);
                    Block block = world.getBlockAt(blockLoc);

                    if (block.getType() != Material.BEDROCK && block.getType().isSolid()) {
                        if (craterData != null) {
                            craterData.originalBlocks.put(blockLoc.clone(), block.getType());
                        }

                        if (random.nextDouble() < 0.4) {
                            world.spawnParticle(Particle.BLOCK_CRACK, blockLoc.add(0.5, 0.5, 0.5), 2,
                                    0.2, 0.2, 0.2, 0, block.getBlockData());
                        }

                        if (depth <= 2 && random.nextDouble() < 0.2) {
                            Material scorchedMaterial = SCORCHED_MATERIALS[random.nextInt(SCORCHED_MATERIALS.length)];
                            block.setType(scorchedMaterial, false);
                        } else {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }

            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    Vector offset = right.clone().multiply(x * 0.5).add(up.clone().multiply(y * 0.5));
                    Location centerBlockLoc = layerCenter.clone().add(offset);
                    Block centerBlock = world.getBlockAt(centerBlockLoc);

                    if (centerBlock.getType() != Material.BEDROCK && centerBlock.getType().isSolid()) {
                        if (craterData != null) {
                            craterData.originalBlocks.put(centerBlockLoc.clone(), centerBlock.getType());
                        }

                        if (depth <= 3 && random.nextDouble() < 0.3) {
                            Material scorchedMaterial = SCORCHED_MATERIALS[random.nextInt(SCORCHED_MATERIALS.length)];
                            centerBlock.setType(scorchedMaterial, false);
                        } else {
                            centerBlock.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        private void createMeteorCrust(Location center, double yaw, double pitch) {
            World world = center.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            double crustRadius = meteorSize * 0.7;

            for (double x = -crustRadius; x <= crustRadius; x += 0.8) {
                for (double y = -crustRadius; y <= crustRadius; y += 0.8) {
                    for (double z = -crustRadius; z <= crustRadius; z += 0.8) {
                        double distance = Math.sqrt(x * x + y * y + z * z);

                        if (distance >= crustRadius * 0.4 && distance <= crustRadius && random.nextDouble() < 0.8) {
                            Vector offset = new Vector(x, y, z);
                            Vector rotatedOffset = rotateOffset(offset, yaw, pitch);
                            Location blockLoc = center.clone().add(rotatedOffset);

                            Block block = world.getBlockAt(blockLoc);
                            if (block.getType() != Material.BEDROCK && block.getType().isAir()) {
                                Material crustMaterial;
                                double materialRoll = random.nextDouble();
                                if (materialRoll < 0.15) {
                                    crustMaterial = Material.MAGMA_BLOCK;
                                } else if (materialRoll < 0.25) {
                                    crustMaterial = Material.GILDED_BLACKSTONE;
                                } else {
                                    crustMaterial = METEOR_CRUST_MATERIALS[random.nextInt(METEOR_CRUST_MATERIALS.length)];
                                }
                                block.setType(crustMaterial, false);
                            }
                        }
                    }
                }
            }
        }

        private void addCrustAndScorchedMaterials(Location center, double radius, int depth) {
            World world = center.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (double angle = 0; angle < 2 * Math.PI; angle += 0.3) {
                for (double r = radius * 0.6; r <= radius * 1.2; r += 0.5) {
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;

                    for (int y = -1; y <= 1; y++) {
                        Location loc = center.clone().add(x, y + random.nextInt(2), z);
                        Block block = world.getBlockAt(loc);

                        if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                            double placementChance = depth == 0 ? 0.4 : 0.25;

                            if (random.nextDouble() < placementChance) {
                                if (random.nextDouble() < 0.6) {
                                    Material scorchedMaterial = SCORCHED_MATERIALS[random.nextInt(SCORCHED_MATERIALS.length)];
                                    block.setType(scorchedMaterial, false);
                                } else {
                                    Material crustMaterial = METEOR_CRUST_MATERIALS[random.nextInt(METEOR_CRUST_MATERIALS.length)];
                                    block.setType(crustMaterial, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void placeMeteorWithCrust(Location impactPoint, Vector direction, int depth) {
            World world = impactPoint.getWorld();

            Location meteorCenter = impactPoint.clone().add(direction.clone().multiply(depth * 0.8));

            double yaw = Math.atan2(-direction.getX(), direction.getZ());
            double pitch = Math.atan2(-direction.getY(), Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ()));

            ThreadLocalRandom random = ThreadLocalRandom.current();

            createMeteorCrust(meteorCenter, yaw, pitch);

            int totalVoxels = meteorVoxels.size();
            int coreVoxelsToPlace = (int) (totalVoxels * 0.6);

            for (int i = 0; i < coreVoxelsToPlace && i < totalVoxels; i++) {
                MeteorVoxel voxel = meteorVoxels.get(i);

                if (voxel.offset.length() <= meteorSize * 0.4) {
                    Vector rotatedOffset = rotateOffset(voxel.offset.clone(), yaw, pitch);
                    Location blockLoc = meteorCenter.clone().add(rotatedOffset);

                    Block block = world.getBlockAt(blockLoc);
                    if (block.getType() != Material.BEDROCK) {
                        Material material;
                        if (random.nextDouble() < 0.4) {
                            material = selectOreMaterial();
                        } else {
                            material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                        }
                        block.setType(material, false);
                    }
                }
            }

            world.spawnParticle(Particle.END_ROD, meteorCenter, 10, 0.8, 0.8, 0.8, 0.01);
            world.spawnParticle(Particle.FLAME, meteorCenter, 8, 0.6, 0.6, 0.6, 0.03);
        }

        private void createMinimalScorchedArea(Location center) {
            World world = center.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            int radius = Math.max(3, meteorSize / 2);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= radius) {
                        double placementChance = distance <= radius * 0.5 ? 0.15 : 0.05;

                        if (random.nextDouble() < placementChance) {
                            int groundY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);

                            for (int y = 0; y >= -1; y--) {
                                Location blockLoc = new Location(world, center.getBlockX() + x, groundY + y, center.getBlockZ() + z);
                                Block block = blockLoc.getBlock();

                                if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                                    double replaceChance = y == 0 ? 0.6 : 0.3;

                                    if (random.nextDouble() < replaceChance) {
                                        Material scorchedMaterial;
                                        if (distance <= radius * 0.3) {
                                            double rand = random.nextDouble();
                                            if (rand < 0.3) scorchedMaterial = Material.MAGMA_BLOCK;
                                            else if (rand < 0.6) scorchedMaterial = Material.DEEPSLATE;
                                            else scorchedMaterial = Material.COBBLESTONE;
                                        } else {
                                            scorchedMaterial = random.nextDouble() < 0.7 ?
                                                    Material.COBBLESTONE : Material.DEEPSLATE;
                                        }
                                        block.setType(scorchedMaterial, false);
                                    }
                                }
                            }
                        }

                        if (random.nextDouble() < 0.08 && distance <= radius * 0.6) {
                            int groundY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                            Location fireLoc = new Location(world, center.getBlockX() + x, groundY + 1, center.getBlockZ() + z);
                            Block fireBlock = world.getBlockAt(fireLoc);
                            Block belowFire = world.getBlockAt(center.getBlockX() + x, groundY, center.getBlockZ() + z);

                            if (fireBlock.getType().isAir() && belowFire.getType().isSolid()) {
                                fireBlock.setType(Material.FIRE, false);

                                int burnTime = 200 + random.nextInt(400);
                                Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
                                    if (fireBlock.getType() == Material.FIRE) {
                                        Block supportBlock = world.getBlockAt(fireBlock.getLocation().subtract(0, 1, 0));
                                        if (!supportBlock.getType().isSolid()) {
                                            fireBlock.setType(Material.AIR, false);
                                            return;
                                        }
                                        if (random.nextDouble() < 0.5) {
                                            return;
                                        }
                                        fireBlock.setType(Material.AIR, false);
                                    }
                                }, burnTime);
                            }
                        }
                    }
                }
            }

            int fireRadius = radius + 2;
            for (int x = -fireRadius; x <= fireRadius; x++) {
                for (int z = -fireRadius; z <= fireRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance > radius && distance <= fireRadius && random.nextDouble() < 0.04) {

                        int groundY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                        Location groundLoc = new Location(world, center.getBlockX() + x, groundY, center.getBlockZ() + z);
                        Block groundBlock = world.getBlockAt(groundLoc);

                        if (groundBlock.getType().isSolid() && random.nextDouble() < 0.2) {
                            Material scorchedMaterial = random.nextDouble() < 0.8 ?
                                    Material.COBBLESTONE : Material.DEEPSLATE;
                            groundBlock.setType(scorchedMaterial, false);
                        }

                        if (groundBlock.getType().isSolid()) {
                            Location fireLoc = groundLoc.clone().add(0, 1, 0);
                            Block fireBlock = world.getBlockAt(fireLoc);

                            if (fireBlock.getType().isAir()) {
                                boolean nearLeaves = checkNearbyVegetation(fireLoc);

                                if (nearLeaves || random.nextDouble() < 0.3) {
                                    fireBlock.setType(Material.FIRE, false);

                                    Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
                                        Block supportCheck = world.getBlockAt(fireBlock.getLocation().subtract(0, 1, 0));
                                        if (!supportCheck.getType().isSolid()) {
                                            if (fireBlock.getType() == Material.FIRE) {
                                                fireBlock.setType(Material.AIR, false);
                                            }
                                        }
                                    }, 20L);
                                }
                            }
                        }
                    }
                }
            }
        }

        private boolean checkNearbyVegetation(Location fireLoc) {
            World world = fireLoc.getWorld();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        Block nearby = world.getBlockAt(fireLoc.clone().add(dx, dy, dz));
                        if (nearby.getType().name().contains("LEAVES") ||
                                nearby.getType().name().contains("LOG") ||
                                nearby.getType().name().contains("WOOD")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private Vector rotateOffset(Vector o, double yaw, double pitch) {
            double cy = Math.cos(yaw), sy = Math.sin(yaw);
            double x1 = o.getX() * cy - o.getZ() * sy;
            double z1 = o.getX() * sy + o.getZ() * cy;
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            double y2 = o.getY() * cp - z1 * sp;
            double z2 = o.getY() * sp + z1 * cp;
            return new Vector(x1, y2, z2);
        }

        private Material selectOreMaterial() {
            double rand = ThreadLocalRandom.current().nextDouble(100);
            double coalRate = Feature.METEOR_RAIN.getDouble("coal-ore-rate");
            double ironRate = Feature.METEOR_RAIN.getDouble("iron-ore-rate");
            double goldRate = Feature.METEOR_RAIN.getDouble("gold-ore-rate");
            double diamondRate = Feature.METEOR_RAIN.getDouble("diamond-ore-rate");
            double emeraldRate = Feature.METEOR_RAIN.getDouble("emerald-ore-rate");
            double ancientDebrisRate = Feature.METEOR_RAIN.getDouble("ancient-debris-rate");

            rand -= ancientDebrisRate;
            if (rand < 0) return Material.ANCIENT_DEBRIS;
            rand -= emeraldRate;
            if (rand < 0) return Material.EMERALD_ORE;
            rand -= diamondRate;
            if (rand < 0) return Material.DIAMOND_ORE;
            rand -= goldRate;
            if (rand < 0) return Material.GOLD_ORE;
            rand -= ironRate;
            if (rand < 0) return Material.IRON_ORE;
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
            long s = seed.getMostSignificantBits() ^ seed.getLeastSignificantBits();
            Random random = new Random(s);

            switch (shape) {
                case SPHERICAL:
                    return generateSphericalMeteor(size, random);
                case ARROW:
                    return generateArrowMeteor(size, random);
                case IRREGULAR:
                    return generateIrregularMeteor(size, random);
                case TAILED:
                    return generateTailedMeteor(size, random);
                case COMPLEX:
                    return generateComplexMeteor(size, random);
                default:
                    return generateSphericalMeteor(size, random);
            }
        }

        private List<MeteorVoxel> generateSphericalMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            double rx = size * (0.55 + random.nextDouble() * 0.25);
            double ry = size * (0.50 + random.nextDouble() * 0.25);
            double rz = size * (0.55 + random.nextDouble() * 0.25);
            int maxX = (int) Math.ceil(rx) + 1;
            int maxY = (int) Math.ceil(ry) + 1;
            int maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double nx = x / rx;
                        double ny = y / ry;
                        double nz = z / rz;
                        double distance = nx * nx + ny * ny + nz * nz;
                        double noise = (random.nextDouble() * 2 - 1) * 0.12;
                        if (distance <= 1.0 + noise && random.nextDouble() < 0.85) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, z),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
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
            int maxX = (int) Math.ceil(rx) + 1;
            int maxY = (int) Math.ceil(ry) + 1;
            int maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double tapering = z < 0 ? 1.0 + (z / rz) * 0.8 : 1.0;
                        double nx = x / (rx * tapering);
                        double ny = y / (ry * tapering);
                        double nz = z / rz;
                        double distance = nx * nx + ny * ny + nz * nz;
                        double noise = (random.nextDouble() * 2 - 1) * 0.1;
                        if (distance <= 1.0 + noise && random.nextDouble() < 0.82) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, z),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
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
                        random.nextDouble(-size * 0.6, size * 0.6)
                );
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
                            double nx = pos.getX() / rx;
                            double ny = pos.getY() / ry;
                            double nz = pos.getZ() / rz;
                            double distance = nx * nx + ny * ny + nz * nz;
                            double noise = (random.nextDouble() * 2 - 1) * 0.25;
                            if (distance <= 1.0 + noise && random.nextDouble() < 0.75) {
                                Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                                voxels.add(new MeteorVoxel(
                                        new Vector(x, y, z),
                                        WrappedBlockData.createData(material.createBlockData())
                                ));
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
            int maxX = (int) Math.ceil(rx) + 1;
            int maxY = (int) Math.ceil(ry) + 1;
            int maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double nx = x / rx;
                        double ny = y / ry;
                        double nz = z / rz;
                        double distance = nx * nx + ny * ny + nz * nz;
                        if (distance <= 1.0 && random.nextDouble() < 0.88) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, z),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
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
                        double distance = Math.sqrt(x * x + y * y);
                        if (distance <= tailRadius && random.nextDouble() < 0.4 * tailFactor) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, maxZ + t),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateComplexMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            voxels.addAll(generateSphericalMeteor(size, random));
            int numProtrusions = 2 + random.nextInt(3);
            for (int i = 0; i < numProtrusions; i++) {
                double angle1 = random.nextDouble() * 2 * Math.PI;
                double angle2 = random.nextDouble() * Math.PI;
                Vector direction = new Vector(
                        Math.cos(angle1) * Math.sin(angle2),
                        Math.cos(angle2),
                        Math.sin(angle1) * Math.sin(angle2)
                ).normalize();
                double protrusionSize = size * (0.3 + random.nextDouble() * 0.4);
                int protrusionLength = (int) (protrusionSize * (1.0 + random.nextDouble()));
                for (int j = 1; j <= protrusionLength; j++) {
                    double shrinkFactor = 1.0 - (double) j / protrusionLength * 0.8;
                    double radius = Math.max(1, protrusionSize * 0.3 * shrinkFactor);
                    Vector basePos = direction.clone().multiply(size * 0.7 + j);
                    for (int x = (int) -radius; x <= radius; x++) {
                        for (int y = (int) -radius; y <= radius; y++) {
                            for (int z = (int) -radius; z <= radius; z++) {
                                double distance = Math.sqrt(x * x + y * y + z * z);
                                if (distance <= radius && random.nextDouble() < 0.7) {
                                    Vector finalPos = basePos.clone().add(new Vector(x, y, z));
                                    Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                                    voxels.add(new MeteorVoxel(
                                            finalPos,
                                            WrappedBlockData.createData(material.createBlockData())
                                    ));
                                }
                            }
                        }
                    }
                }
            }
            return voxels;
        }
    }
    private void scheduleCraterRecovery(CraterData craterData) {
        int totalBlocks = craterData.originalBlocks.size();
        if (totalBlocks == 0) return;

        int blocksPerTick = Math.max(1, Math.min(5, totalBlocks / 200));

        List<Map.Entry<Location, Material>> blockList = new ArrayList<>(craterData.originalBlocks.entrySet());
        blockList.sort(Comparator.comparingInt(e -> e.getKey().getBlockY()));

        new BukkitRunnable() {
            int currentIndex = 0;

            @Override
            public void run() {
                if (currentIndex >= blockList.size()) {
                    cancel();
                    return;
                }

                int endIndex = Math.min(currentIndex + blocksPerTick, blockList.size());

                for (int i = currentIndex; i < endIndex; i++) {
                    Map.Entry<Location, Material> entry = blockList.get(i);
                    Location loc = entry.getKey();
                    Block block = loc.getBlock();

                    if (block.getType() == Material.AIR) {
                        block.setType(entry.getValue(), false);
                    }
                }

                currentIndex = endIndex;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 200L, 10L);
    }

    private void startCraterRecoveryProcess() {
        List<CraterData> cratersToProcess = new ArrayList<>(cratersToRecover);
        cratersToRecover.clear();

        for (CraterData craterData : cratersToProcess) {
            scheduleCraterRecovery(craterData);
        }
    }
}