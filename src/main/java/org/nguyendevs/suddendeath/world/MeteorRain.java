package org.nguyendevs.suddendeath.world;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;
import org.nguyendevs.suddendeath.util.Feature;

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

    private final Map<UUID, MeteorTask> activeMeteors = new ConcurrentHashMap<>();
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
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error cleaning up Meteor Rain effects in world: " + getWorld().getName(), e);
        }
    }

    private boolean isProtected(Player p) {
        if (p == null) return true;
        if (!p.isOnline()) return true;
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return true;
        return false;
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

                createTrailEffect();
                if (tickCount % 6 == 0) playMeteorFlightSound();
                if (tickCount % 2 == 0) renderFakeMeteor();

                if (currentLocation.getBlockY() <= targetLocation.getBlockY() - 16) {
                    hasImpacted = true;
                    impact(currentLocation.clone());
                    cancel();
                }
            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in meteor task", e);
                cancel();
            }
        }

        private void createTrailEffect() {
            World world = currentLocation.getWorld();
            if (world == null) return;
            double offset = meteorSize / 2.0;
            world.spawnParticle(Particle.FLAME, currentLocation, 20, offset, offset, offset, 0.03);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation, 15, offset, offset, offset, 0.05);
            world.spawnParticle(Particle.LAVA, currentLocation, 8, offset/2, offset/2, offset/2, 0);
            world.spawnParticle(Particle.CRIT, currentLocation, 10, offset, offset, offset, 0.1);
            world.spawnParticle(Particle.EXPLOSION_NORMAL, currentLocation, 5, offset/3, offset/3, offset/3, 0.02);
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
                }
                lastSent.clear();

                for (Map.Entry<BlockPosition, WrappedBlockData> entry : currentBlocks.entrySet()) {
                    sendBlockChange(player, entry.getKey(), entry.getValue());
                }

                lastSent.addAll(currentBlocks.keySet());
            }
        }

        private void sendBlockChange(Player player, BlockPosition pos, WrappedBlockData data) {
            try {
                PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
                packet.getBlockPositionModifier().write(0, pos);
                packet.getBlockData().write(0, data);
                pm.sendServerPacket(player, packet, false);
            } catch (Exception ignored) {}
        }

        private void clearAllFakeBlocks() {
            for (Map.Entry<UUID, Set<BlockPosition>> entry : lastSentPerPlayer.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) continue;

                for (BlockPosition pos : entry.getValue()) {
                    sendBlockChange(player, pos, WrappedBlockData.createData(Material.AIR.createBlockData()));
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
                createImpactCrater(impactPoint);

            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error creating meteor impact", e);
            }
        }

        private void applyShockwaveAt(Location center) {
            World world = center.getWorld();
            double knockRadius = meteorSize * 2.5;
            for (Player player : world.getPlayers()) {
                if (!player.getWorld().equals(world)) continue;
                double distance = player.getLocation().distance(center);
                if (distance <= knockRadius) {
                    Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
                    double strength = (1.0 - distance / knockRadius) * (meteorSize / 3.5) + 1.5;
                    player.setVelocity(direction.multiply(strength).add(new Vector(0, 0.8 + meteorSize * 0.04, 0)));
                    double baseDamage = 5.0 + meteorSize * 1.0;
                    double damage = Math.max(1.0, baseDamage * (1.0 - (distance / knockRadius)));
                    player.damage(damage);
                }
            }
        }

        private void createImpactCrater(Location impactPoint) {
            int bowlRadius = Math.max(3, meteorSize + 1);

            carveCraterWithMeteorShape(impactPoint, bowlRadius);

            int floorY = findCavityLowestFloorY(
                    impactPoint,
                    bowlRadius + meteorSize + 3,
                    meteorSize * 2 + 8
            );

            Vector dir = flightDirection.clone().normalize();
            double yaw = Math.atan2(-dir.getX(), dir.getZ());
            double pitch = Math.atan2(-dir.getY(), Math.sqrt(dir.getX()*dir.getX()+dir.getZ()*dir.getZ()));

            int minYOffsetRot = minRotatedYOffset(yaw, pitch);

            Location finalCenter = new Location(
                    impactPoint.getWorld(),
                    impactPoint.getBlockX() + 0.5,
                    floorY - minYOffsetRot - 1,
                    impactPoint.getBlockZ() + 0.5
            );

            materializeMeteorRotated(finalCenter, yaw, pitch);
            scorchSurroundings(impactPoint, bowlRadius);
        }

        private int minRotatedYOffset(double yaw, double pitch) {
            int min = Integer.MAX_VALUE;
            for (MeteorVoxel v : meteorVoxels) {
                Vector o = rotateOffset(v.offset.clone(), yaw, pitch);
                int y = (int) Math.floor(o.getY());
                if (y < min) min = y;
            }
            return (min == Integer.MAX_VALUE) ? 0 : min;
        }

        private void materializeMeteorRotated(Location center, double yaw, double pitch) {
            World w = center.getWorld();
            double innerThreshold = Math.max(1.0, meteorSize * 0.45);

            for (MeteorVoxel v : meteorVoxels) {
                Vector o = rotateOffset(v.offset.clone(), yaw, pitch);
                Location loc = center.clone().add(o);
                double r = o.length();
                Material mat;
                if (r <= innerThreshold && ThreadLocalRandom.current().nextDouble() < 0.55) {
                    mat = selectOreMaterial();
                } else {
                    mat = METEOR_MATERIALS[ThreadLocalRandom.current().nextInt(METEOR_MATERIALS.length)];
                }
                Block b = w.getBlockAt(loc);
                if (b.getType() != Material.BEDROCK) b.setType(mat, false);
            }
        }

        private void carveCraterWithMeteorShape(Location impactPoint, int baseRadius) {
            World w = impactPoint.getWorld();
            Vector dir = flightDirection.clone().normalize();
            double yaw = Math.atan2(-dir.getX(), dir.getZ());
            double pitch = Math.atan2(-dir.getY(), Math.sqrt(dir.getX()*dir.getX()+dir.getZ()*dir.getZ()));

            int steps = meteorSize + 4;
            double forward = 0.6;
            double sink = 0.4 + meteorSize * 0.03;

            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double scale = 1.0 - t * 0.5;
                Location c = impactPoint.clone().add(dir.clone().multiply(i * forward));
                c.add(0, -i * sink, 0);

                for (MeteorVoxel v : meteorVoxels) {
                    Vector o = rotateOffset(v.offset.clone().multiply(scale), yaw, pitch);
                    Location p = c.clone().add(o);
                    Block b = w.getBlockAt(p);
                    if (b.getType() != Material.BEDROCK) b.setType(Material.AIR, false);
                }
            }
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

        private int findCavityLowestFloorY(Location center, int horizontalRadius, int verticalSearchDepth) {
            World w = center.getWorld();
            int bestY = Integer.MAX_VALUE;

            int startY = Math.min(center.getBlockY() + horizontalRadius, w.getMaxHeight() - 1);
            int endY   = Math.max(w.getMinHeight(), center.getBlockY() - verticalSearchDepth);

            int r2 = horizontalRadius * horizontalRadius;

            for (int y = startY; y >= endY; y--) {
                for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
                    for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                        if (dx*dx + dz*dz > r2) continue;

                        int bx = center.getBlockX() + dx;
                        int bz = center.getBlockZ() + dz;

                        Block air = w.getBlockAt(bx, y, bz);
                        if (!air.getType().isAir()) continue;

                        Block below = w.getBlockAt(bx, y - 1, bz);
                        if (below.getType().isSolid()) {
                            if (y < bestY) bestY = y;
                        }
                    }
                }
            }
            if (bestY == Integer.MAX_VALUE) return endY + 1;
            return bestY;
        }

        private void scorchSurroundings(Location center, int radius) {
            World world = center.getWorld();
            int surfaceRadius = Math.max(2, radius - 1);

            for (int x = -surfaceRadius; x <= surfaceRadius; x++) {
                for (int z = -surfaceRadius; z <= surfaceRadius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= surfaceRadius) {
                        int groundY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                        Location blockLoc = new Location(world, center.getBlockX() + x, groundY, center.getBlockZ() + z);
                        Block block = blockLoc.getBlock();

                        if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                            if (ThreadLocalRandom.current().nextDouble() < 0.25) {
                                Material scorchedMat = SCORCHED_MATERIALS[
                                        ThreadLocalRandom.current().nextInt(SCORCHED_MATERIALS.length)
                                        ];
                                block.setType(scorchedMat, false);
                            }
                        } else if (block.getType().isAir() && ThreadLocalRandom.current().nextDouble() < 0.08) {
                            Block below = blockLoc.clone().subtract(0, 1, 0).getBlock();
                            if (below.getType().isSolid()) {
                                block.setType(Material.FIRE, false);
                            }
                        }
                    }
                }
            }

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius/2; y <= radius/4; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance <= radius && distance > radius * 0.6) {
                            Location blockLoc = center.clone().add(x, y, z);
                            Block block = blockLoc.getBlock();
                            if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                                if (ThreadLocalRandom.current().nextDouble() < 0.15) {
                                    Material scorchedMat = SCORCHED_MATERIALS[
                                            ThreadLocalRandom.current().nextInt(SCORCHED_MATERIALS.length)
                                            ];
                                    block.setType(scorchedMat, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        private Material selectOreMaterial() {
            double rand = ThreadLocalRandom.current().nextDouble(100);
            double coalRate = Feature.METEOR_RAIN.getDouble("coal-ore-rate");
            double ironRate = Feature.METEOR_RAIN.getDouble("iron-ore-rate");
            double goldRate = Feature.METEOR_RAIN.getDouble("gold-ore-rate");
            double diamondRate = Feature.METEOR_RAIN.getDouble("diamond-ore-rate");
            double emeraldRate = Feature.METEOR_RAIN.getDouble("emerald-ore-rate");
            double ancientDebrisRate = Feature.METEOR_RAIN.getDouble("ancient-debris-rate");

            rand -= ancientDebrisRate; if (rand < 0) return Material.ANCIENT_DEBRIS;
            rand -= emeraldRate;      if (rand < 0) return Material.EMERALD_ORE;
            rand -= diamondRate;      if (rand < 0) return Material.DIAMOND_ORE;
            rand -= goldRate;         if (rand < 0) return Material.GOLD_ORE;
            rand -= ironRate;         if (rand < 0) return Material.IRON_ORE;
            return Material.COAL_ORE;
        }

        @Override
        public void cancel() {
            super.cancel();
            try {
                clearAllFakeBlocks();
            } catch (Exception ignored) {}
            activeMeteors.remove(this.meteorId);
        }

        private List<MeteorVoxel> generateMeteorVoxels(int size, UUID seed, MeteorShape shape) {
            List<MeteorVoxel> voxels = new ArrayList<>();
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
}