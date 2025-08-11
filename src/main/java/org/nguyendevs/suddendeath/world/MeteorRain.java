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

    private static final int MIN_METEOR_SIZE = 5;
    private static final int MAX_METEOR_SIZE = 15;
    private static final int SOUND_RADIUS = 48;
    private static final int SPAWN_HEIGHT = 80;
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
        long delay = 200 + ThreadLocalRandom.current().nextInt(800);
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

    private void spawnMeteorShower() {
        try {
            List<Player> worldPlayers = getWorld().getPlayers();
            if (worldPlayers.isEmpty()) return;

            int meteorCount = ThreadLocalRandom.current().nextInt(1, 6);
            for (int i = 0; i < meteorCount; i++) {
                Player targetPlayer = worldPlayers.get(ThreadLocalRandom.current().nextInt(worldPlayers.size()));
                long delay = i * ThreadLocalRandom.current().nextInt(5, 20);
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
            Location playerLoc = targetPlayer.getLocation();

            // Tạo vị trí target ngẫu nhiên xung quanh player
            double distance = ThreadLocalRandom.current().nextDouble(10, 60);
            double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);

            Location targetLocation = playerLoc.clone().add(offsetX, 0, offsetZ);
            int groundY = getWorld().getHighestBlockYAt(targetLocation);
            targetLocation.setY(groundY);

            if (!SuddenDeath.getInstance().getWorldGuard()
                    .isFlagAllowedAtLocation(targetLocation, CustomFlag.SDS_EVENT)) return;

            // Tạo vị trí spawn với góc chéo ngẫu nhiên
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

        // Tạo góc chéo ngẫu nhiên (tránh góc thẳng)
        double angle = random.nextDouble(0.3, 2 * Math.PI - 0.3); // Tránh góc 0, π/2, π, 3π/2

        // Đảm bảo không phải góc thẳng bằng cách thêm offset nhỏ
        double[] avoidAngles = {0, Math.PI/2, Math.PI, 3*Math.PI/2};
        for (double avoidAngle : avoidAngles) {
            if (Math.abs(angle - avoidAngle) < 0.2) {
                angle += 0.3;
            }
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

    private class MeteorTask extends BukkitRunnable {
        private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        private final Location startLocation;
        private final Location targetLocation;
        private final int meteorSize;
        private final UUID meteorId;
        private final Vector flightDirection;

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

            // Tính toán hướng bay với góc chéo
            Vector baseDirection = target.toVector().subtract(start.toVector()).normalize();

            // Thêm thành phần chéo để tránh bay thẳng
            double randomOffset = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
            this.flightDirection = new Vector(
                    baseDirection.getX() + randomOffset,
                    Math.min(baseDirection.getY(), -0.4), // Đảm bảo bay xuống
                    baseDirection.getZ() + randomOffset
            ).normalize();

            this.speed = ThreadLocalRandom.current().nextDouble(2.0, 3.5);
            this.meteorVoxels = generateMeteorVoxels(size, meteorId);
        }

        @Override
        public void run() {
            try {
                if (hasImpacted || tickCount++ > MAX_TICKS) {
                    if (!hasImpacted) impact();
                    cancel();
                    return;
                }

                // Di chuyển thiên thạch theo hướng đã định
                Vector movement = flightDirection.clone().multiply(speed);
                currentLocation.add(movement);

                // Tạo hiệu ứng đuôi
                createTrailEffect();

                // Âm thanh bay
                if (tickCount % 6 == 0) playMeteorFlightSound();

                // Render thiên thạch ảo
                if (tickCount % 2 == 0) renderFakeMeteor();

                // Kiểm tra va chạm với mặt đất
                if (hasReachedGround()) {
                    hasImpacted = true;
                    impact();
                    cancel();
                }

            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in meteor task", e);
                cancel();
            }
        }

        private boolean hasReachedGround() {
            Block currentBlock = currentLocation.getBlock();
            return currentBlock.getType().isSolid() ||
                    currentLocation.getBlockY() <= targetLocation.getBlockY() + 2;
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

            // Tạo các block giả cho thiên thạch
            for (MeteorVoxel voxel : meteorVoxels) {
                Location blockLoc = currentLocation.clone().add(voxel.offset);
                BlockPosition pos = new BlockPosition(
                        blockLoc.getBlockX(),
                        blockLoc.getBlockY(),
                        blockLoc.getBlockZ()
                );
                currentBlocks.put(pos, voxel.data);
            }

            // Gửi packet cho từng player
            for (Player player : world.getPlayers()) {
                if (!inHorizontalRange(player, currentLocation, VIEW_RANGE)) continue;

                Set<BlockPosition> lastSent = lastSentPerPlayer.computeIfAbsent(
                        player.getUniqueId(), k -> new HashSet<>()
                );

                // Xóa blocks cũ
                for (BlockPosition oldPos : lastSent) {
                    sendBlockChange(player, oldPos, WrappedBlockData.createData(Material.AIR.createBlockData()));
                }
                lastSent.clear();

                // Gửi blocks mới
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

        private void impact() {
            try {
                World world = currentLocation.getWorld();
                if (world == null) return;

                clearAllFakeBlocks();

                // Âm thanh va chạm
                world.playSound(currentLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 2.5f, 0.5f);
                world.playSound(currentLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 1.5f, 0.7f);
                world.playSound(currentLocation, Sound.BLOCK_ANVIL_PLACE, SoundCategory.AMBIENT, 1.2f, 0.4f);

                // Hiệu ứng nổ
                world.spawnParticle(Particle.EXPLOSION_LARGE, currentLocation, 10, 3, 3, 3, 0);
                world.spawnParticle(Particle.LAVA, currentLocation, 50, 4, 4, 4, 0);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation, 30, 3, 3, 3, 0.05);
                world.spawnParticle(Particle.FLAME, currentLocation, 40, 3, 3, 3, 0.1);

                // Sóng xung kích
                applyShockwave();

                // Tạo hố thiên thạch theo góc bay
                createImpactCrater();

            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error creating meteor impact", e);
            }
        }

        private void applyShockwave() {
            World world = currentLocation.getWorld();
            double knockRadius = meteorSize * 2.5;

            for (Player player : world.getPlayers()) {
                if (!player.getWorld().equals(world)) continue;

                double distance = player.getLocation().distance(currentLocation);
                if (distance <= knockRadius) {
                    Vector direction = player.getLocation().toVector()
                            .subtract(currentLocation.toVector()).normalize();
                    double strength = (1.0 - distance / knockRadius) * (meteorSize / 3.5) + 1.5;

                    player.setVelocity(direction.multiply(strength)
                            .add(new Vector(0, 0.8 + meteorSize * 0.04, 0)));

                    double baseDamage = 5.0 + meteorSize * 1.0;
                    double damage = Math.max(1.0, baseDamage * (1.0 - (distance / knockRadius)));
                    player.damage(damage);
                }
            }
        }

        private void createImpactCrater() {
            World world = currentLocation.getWorld();

            // Tạo hố chính tại điểm va chạm
            int craterRadius = Math.max(3, meteorSize / 2 + 3);
            carveSphere(currentLocation, craterRadius);

            // Tạo đường hầm theo hướng bay (ghim sâu vào đất)
            createBurrowTunnel();

            // Tạo lõi thiên thạch ở cuối đường hầm
            Location coreLocation = currentLocation.clone()
                    .add(flightDirection.clone().multiply(meteorSize + 3))
                    .add(0, -2, 0);
            createMeteorCore(coreLocation);

            // Thiêu đốt xung quanh
            scorchSurroundings(currentLocation, craterRadius + 2);
        }

        private void createBurrowTunnel() {
            World world = currentLocation.getWorld();
            int tunnelLength = meteorSize + ThreadLocalRandom.current().nextInt(5, 12);

            for (int i = 0; i < tunnelLength; i++) {
                Vector stepMove = flightDirection.clone().multiply(i);
                stepMove.setY(stepMove.getY() - i * 0.3); // Đi sâu hơn theo từng bước

                Location stepLocation = currentLocation.clone().add(stepMove);
                int stepRadius = Math.max(1, (meteorSize / 2) - i / 3);

                carveSphere(stepLocation, stepRadius);

                // Thêm lửa ngẫu nhiên trong đường hầm
                if (ThreadLocalRandom.current().nextDouble() < 0.15) {
                    Block airBlock = stepLocation.clone().add(0, stepRadius - 1, 0).getBlock();
                    if (airBlock.getType().isAir()) {
                        airBlock.setType(Material.FIRE, false);
                    }
                }
            }
        }

        private void carveSphere(Location center, int radius) {
            World world = center.getWorld();

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance <= radius) {
                            Block block = center.clone().add(x, y, z).getBlock();
                            if (block.getType() != Material.BEDROCK) {
                                block.setType(Material.AIR, false);
                            }
                        }
                    }
                }
            }
        }

        private void scorchSurroundings(Location center, int radius) {
            World world = center.getWorld();

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance <= radius) {
                            Location blockLoc = center.clone().add(x, y, z);
                            Block block = blockLoc.getBlock();

                            if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                                if (ThreadLocalRandom.current().nextDouble() < 0.35) {
                                    Material scorchedMat = SCORCHED_MATERIALS[
                                            ThreadLocalRandom.current().nextInt(SCORCHED_MATERIALS.length)
                                            ];
                                    block.setType(scorchedMat, false);
                                }
                            } else if (block.getType().isAir() && ThreadLocalRandom.current().nextDouble() < 0.12) {
                                Block below = blockLoc.clone().subtract(0, 1, 0).getBlock();
                                if (below.getType().isSolid()) {
                                    block.setType(Material.FIRE, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void createMeteorCore(Location center) {
            World world = center.getWorld();
            int coreRadius = Math.max(2, meteorSize / 2);

            for (int x = -coreRadius; x <= coreRadius; x++) {
                for (int y = -coreRadius; y <= coreRadius; y++) {
                    for (int z = -coreRadius; z <= coreRadius; z++) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance <= coreRadius) {
                            Block block = center.clone().add(x, y, z).getBlock();

                            if (distance >= coreRadius - 1) {
                                // Lớp vỏ ngoài
                                Material meteorMaterial = METEOR_MATERIALS[
                                        ThreadLocalRandom.current().nextInt(METEOR_MATERIALS.length)
                                        ];
                                block.setType(meteorMaterial, false);
                            } else {
                                // Lõi bên trong chứa quặng
                                block.setType(selectOreMaterial(), false);
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

        private List<MeteorVoxel> generateMeteorVoxels(int size, UUID seed) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            long s = seed.getMostSignificantBits() ^ seed.getLeastSignificantBits();
            Random random = new Random(s);

            double rx = size * (0.5 + random.nextDouble() * 0.3);
            double ry = size * (0.5 + random.nextDouble() * 0.3);
            double rz = size * (0.6 + random.nextDouble() * 0.4);

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

                        double noise = (random.nextDouble() * 2 - 1) * 0.15;
                        if (distance <= 1.0 + noise) {
                            if (random.nextDouble() < 0.8) {
                                Material material = METEOR_MATERIALS[
                                        random.nextInt(METEOR_MATERIALS.length)
                                        ];
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
    }
}