package org.nguyendevs.suddendeath.features.mob.hostile;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ZombiePlaceFeature extends AbstractFeature {

    private final Map<UUID, PathfinderData> zombiePathData = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activePlacingTasks = new ConcurrentHashMap<>();
    private final Map<Location, Long> recentlyPlacedBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> zombiePlaceCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> zombiesWithCustomAI = ConcurrentHashMap.newKeySet();
    private Set<Material> placeableBlocks;

    private static final double MAX_TARGET_DISTANCE = 150.0;
    private static final double MAX_TARGET_DISTANCE_SQUARED = MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE;

    @Override
    public String getName() {
        return "Zombie Place Feature";
    }

    @Override
    protected void onEnable() {
        // Load placeable blocks from config
        loadPlaceableBlocks();

        // Main pathfinding and placing task
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ZOMBIE_PLACE_BLOCK.isEnabled(world)) {
                            List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));

                            for (int i = 0; i < zombies.size(); i++) {
                                final Zombie zombie = zombies.get(i);
                                final long delay = i; // tick delay for stagger

                                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                                    try {
                                        if (zombie.isValid() && zombie.getTarget() instanceof Player) {
                                            processZombiePlacement(zombie);
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().log(Level.WARNING, "Error processing zombie placement", e);
                                    }
                                }, delay);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Zombie Place task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));

        // Cleanup old data
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldData();
            }
        }.runTaskTimerAsynchronously(plugin, 200L, 200L));
    }

    @Override
    protected void onDisable() {
        activePlacingTasks.values().forEach(BukkitTask::cancel);
        activePlacingTasks.clear();
        zombiePathData.clear();
        recentlyPlacedBlocks.clear();
        zombiePlaceCooldown.clear();

        for (UUID uuid : zombiesWithCustomAI) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof Zombie zombie) {
                removeCustomAI(zombie);
            }
        }
        zombiesWithCustomAI.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            cleanupZombie(zombie);
        }
    }

    private void loadPlaceableBlocks() {
        placeableBlocks = new HashSet<>();
        String blocksStr = Feature.ZOMBIE_PLACE_BLOCK.getString("placeable-blocks");

        if (blocksStr != null && !blocksStr.isEmpty()) {
            for (String materialName : blocksStr.split(",")) {
                try {
                    Material material = Material.valueOf(materialName.trim().toUpperCase());
                    if (material.isBlock() && material.isSolid()) {
                        placeableBlocks.add(material);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in placeable-blocks: " + materialName);
                }
            }
        }

        // Default blocks if config is empty
        if (placeableBlocks.isEmpty()) {
            placeableBlocks.addAll(Arrays.asList(
                    Material.COBBLESTONE, Material.STONE, Material.DIRT,
                    Material.NETHERRACK, Material.SAND, Material.GRAVEL
            ));
        }
    }

    private void processZombiePlacement(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();

        // Check cooldown from config
        long cooldownMs = (long) Feature.ZOMBIE_PLACE_BLOCK.getDouble("placement-cooldown-ms");
        Long lastPlace = zombiePlaceCooldown.get(zombieUUID);
        long now = System.currentTimeMillis();
        if (lastPlace != null && now - lastPlace < cooldownMs) {
            return;
        }

        // Ensure zombie has custom AI for safe placement if enabled
        if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("ai-freeze-while-placing") &&
                !zombiesWithCustomAI.contains(zombieUUID)) {
            Bukkit.getScheduler().runTask(plugin, () -> injectCustomAI(zombie));
            zombiesWithCustomAI.add(zombieUUID);
        }

        if (!(zombie.getTarget() instanceof Player target)) return;
        if (!plugin.getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), CustomFlag.SDS_PLACE))
            return;

        if (activePlacingTasks.containsKey(zombieUUID)) return;

        Bukkit.getScheduler().runTask(plugin, () -> analyzeAndPlace(zombie, target));
    }

    private void analyzeAndPlace(Zombie zombie, Player target) {
        UUID zombieUUID = zombie.getUniqueId();
        Location zombieLoc = zombie.getLocation();
        Location targetLoc = target.getLocation();

        ItemStack blocks = getPlaceableBlocks(zombie);
        if (blocks == null || blocks.getType() == Material.AIR) return;
        if (!placeableBlocks.contains(blocks.getType())) return;

        PathfinderData pathData = zombiePathData.computeIfAbsent(zombieUUID, k -> new PathfinderData());

        PlacementSituation situation = analyzeSituation(zombie, target, pathData);

        if (situation.needsPlacement) {
            Location placeLocation = situation.placeLocation;
            BlockFace placeFace = situation.placeFace;

            // Check block place cooldown from config
            long blockCooldownMs = (long) Feature.ZOMBIE_PLACE_BLOCK.getDouble("placement-cooldown-ms") * 2;
            if (wasRecentlyPlaced(placeLocation, blockCooldownMs)) return;

            executePlacement(zombie, target, placeLocation, placeFace, blocks, situation.type);
        } else {
            pathData.lastTargetLocation = targetLoc.clone();
            pathData.lastCheckTime = System.currentTimeMillis();
        }
    }

    private PlacementSituation analyzeSituation(Zombie zombie, Player target, PathfinderData pathData) {
        Location zombieLoc = zombie.getLocation();
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.toVector().subtract(zombieLoc.toVector()).normalize();

        PlacementSituation situation = new PlacementSituation();
        double checkRadius = Feature.ZOMBIE_PLACE_BLOCK.getDouble("check-radius");

        // Check 1: Gap/Void ahead (if bridge building enabled)
        if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("enable-bridge-building")) {
            GapInfo gapInfo = detectGap(zombie, direction, checkRadius);
            if (gapInfo.hasGap) {
                int maxBridgeLength = (int) Feature.ZOMBIE_PLACE_BLOCK.getDouble("max-bridge-length");
                if (gapInfo.gapDistance <= maxBridgeLength) {
                    situation.needsPlacement = true;
                    situation.placeLocation = gapInfo.placeLocation;
                    situation.placeFace = gapInfo.placeFace;
                    situation.type = PlacementType.BRIDGE;
                    return situation;
                }
            }
        }

        // Check 2: Height difference (if climbing enabled)
        if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("enable-climbing")) {
            double heightDiff = targetLoc.getY() - zombieLoc.getY();
            if (heightDiff > 1.5) {
                ClimbInfo climbInfo = findClimbPlacement(zombie, target, direction);
                if (climbInfo.canClimb) {
                    int maxClimbHeight = (int) Feature.ZOMBIE_PLACE_BLOCK.getDouble("max-climb-height");
                    if (climbInfo.climbHeight <= maxClimbHeight) {
                        situation.needsPlacement = true;
                        situation.placeLocation = climbInfo.placeLocation;
                        situation.placeFace = climbInfo.placeFace;
                        situation.type = PlacementType.CLIMB;
                        return situation;
                    }
                }
            }
        }

        // Check 3: Target is lower (if descending enabled)
        if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("enable-descending")) {
            double heightDiff = targetLoc.getY() - zombieLoc.getY();
            if (heightDiff < -2.0) {
                DescendInfo descendInfo = findDescendPlacement(zombie, target, direction);
                if (descendInfo.needsPlatform) {
                    situation.needsPlacement = true;
                    situation.placeLocation = descendInfo.placeLocation;
                    situation.placeFace = descendInfo.placeFace;
                    situation.type = PlacementType.DESCEND;
                    return situation;
                }
            }
        }

        // Check 4: Blocked by obstacle (if around obstacles enabled)
        if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("enable-around-obstacles")) {
            ObstacleInfo obstacleInfo = detectObstacle(zombie, direction);
            if (obstacleInfo.isBlocked) {
                AroundInfo aroundInfo = findAroundPlacement(zombie, target, direction);
                if (aroundInfo.canGoAround) {
                    situation.needsPlacement = true;
                    situation.placeLocation = aroundInfo.placeLocation;
                    situation.placeFace = aroundInfo.placeFace;
                    situation.type = PlacementType.AROUND;
                    return situation;
                }
            }
        }

        situation.needsPlacement = false;
        return situation;
    }

    private GapInfo detectGap(Zombie zombie, Vector direction, double checkRadius) {
        GapInfo info = new GapInfo();
        Location checkLoc = zombie.getLocation().clone();

        int maxCheck = (int) Math.min(checkRadius, 3);
        for (int i = 1; i <= maxCheck; i++) {
            checkLoc.add(direction.clone().multiply(1));
            Block checkBlock = checkLoc.getBlock();
            Block belowBlock = checkBlock.getRelative(BlockFace.DOWN);
            Block twoBelow = belowBlock.getRelative(BlockFace.DOWN);

            if (!checkBlock.getType().isSolid() &&
                    !belowBlock.getType().isSolid() &&
                    !twoBelow.getType().isSolid()) {

                info.hasGap = true;
                info.placeLocation = belowBlock.getLocation();
                info.placeFace = findSupportingFace(belowBlock);
                info.gapDistance = i;
                return info;
            }
        }

        info.hasGap = false;
        return info;
    }

    private ClimbInfo findClimbPlacement(Zombie zombie, Player target, Vector direction) {
        ClimbInfo info = new ClimbInfo();
        Location zombieLoc = zombie.getLocation();

        Location frontLoc = zombieLoc.clone().add(direction.clone().multiply(0.5));
        Block frontBlock = frontLoc.getBlock();

        int maxHeight = (int) Feature.ZOMBIE_PLACE_BLOCK.getDouble("max-climb-height");
        for (int height = 1; height <= Math.min(maxHeight, 3); height++) {
            Location placeLoc = frontBlock.getLocation().add(0, height, 0);
            Block placeBlock = placeLoc.getBlock();

            if (!placeBlock.getType().isSolid() && placeBlock.getType().isAir()) {
                Block belowPlace = placeBlock.getRelative(BlockFace.DOWN);

                if (belowPlace.getType().isSolid()) {
                    info.canClimb = true;
                    info.placeLocation = placeLoc;
                    info.placeFace = BlockFace.UP;
                    info.climbHeight = height;
                    return info;
                }
            }
        }

        info.canClimb = false;
        return info;
    }

    private DescendInfo findDescendPlacement(Zombie zombie, Player target, Vector direction) {
        DescendInfo info = new DescendInfo();
        Location zombieLoc = zombie.getLocation();

        Location frontLoc = zombieLoc.clone().add(direction.clone().multiply(1.5));
        Block frontDown = frontLoc.getBlock().getRelative(BlockFace.DOWN);

        if (!frontDown.getType().isSolid() && frontDown.getType().isAir()) {
            info.needsPlatform = true;
            info.placeLocation = frontDown.getLocation();
            info.placeFace = findSupportingFace(frontDown);
            return info;
        }

        info.needsPlatform = false;
        return info;
    }

    private AroundInfo findAroundPlacement(Zombie zombie, Player target, Vector direction) {
        AroundInfo info = new AroundInfo();
        Location zombieLoc = zombie.getLocation();

        Vector[] sideVectors = {
                direction.clone().rotateAroundY(Math.PI / 2),
                direction.clone().rotateAroundY(-Math.PI / 2)
        };

        for (Vector sideDir : sideVectors) {
            Location sideLoc = zombieLoc.clone().add(sideDir.multiply(1));
            Block sideBlock = sideLoc.getBlock();
            Block belowSide = sideBlock.getRelative(BlockFace.DOWN);

            if (!sideBlock.getType().isSolid() && !belowSide.getType().isSolid()) {
                info.canGoAround = true;
                info.placeLocation = belowSide.getLocation();
                info.placeFace = findSupportingFace(belowSide);
                return info;
            }
        }

        info.canGoAround = false;
        return info;
    }

    private ObstacleInfo detectObstacle(Zombie zombie, Vector direction) {
        ObstacleInfo info = new ObstacleInfo();
        Location checkLoc = zombie.getEyeLocation();

        for (int i = 1; i <= 2; i++) {
            checkLoc.add(direction.clone().multiply(1));
            Block block = checkLoc.getBlock();

            if (block.getType().isSolid()) {
                info.isBlocked = true;
                info.obstacleLocation = block.getLocation();
                return info;
            }
        }

        info.isBlocked = false;
        return info;
    }

    private BlockFace findSupportingFace(Block block) {
        BlockFace[] faces = {BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST};

        for (BlockFace face : faces) {
            Block adjacent = block.getRelative(face);
            if (adjacent.getType().isSolid()) {
                return face.getOppositeFace();
            }
        }

        return BlockFace.UP;
    }

    private void executePlacement(Zombie zombie, Player target, Location placeLocation,
                                  BlockFace placeFace, ItemStack blocks, PlacementType type) {
        UUID zombieUUID = zombie.getUniqueId();

        BukkitRunnable placeTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!zombie.isValid() || !target.isValid() ||
                            zombie.getLocation().distanceSquared(target.getLocation()) > MAX_TARGET_DISTANCE_SQUARED) {
                        activePlacingTasks.remove(zombieUUID);
                        cancel();
                        return;
                    }

                    Block block = placeLocation.getBlock();

                    if (block.getType().isSolid()) {
                        activePlacingTasks.remove(zombieUUID);
                        cancel();
                        return;
                    }

                    lookAtLocation(zombie, placeLocation);
                    zombie.swingMainHand();

                    Material blockType = blocks.getType();
                    block.setType(blockType);

                    // Effects (check config)
                    if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("play-sounds")) {
                        zombie.getWorld().playSound(placeLocation,
                                blockType.createBlockData().getSoundGroup().getPlaceSound(),
                                1.0f, 1.0f);
                    }

                    if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("show-particles")) {
                        zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                                placeLocation.clone().add(0.5, 0.5, 0.5),
                                10, 0.2, 0.2, 0.2, 0.1, blockType.createBlockData());
                    }

                    recentlyPlacedBlocks.put(placeLocation.clone(), System.currentTimeMillis());
                    zombiePlaceCooldown.put(zombieUUID, System.currentTimeMillis());

                    // Consume block if enabled
                    if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("block-consume-on-place")) {
                        blocks.setAmount(blocks.getAmount() - 1);
                    }

                    activePlacingTasks.remove(zombieUUID);
                    cancel();

                    // Schedule next check
                    long cooldown = (long) Feature.ZOMBIE_PLACE_BLOCK.getDouble("placement-cooldown-ms") / 50;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (zombie.isValid() && target.isValid()) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                                    () -> processZombiePlacement(zombie));
                        }
                    }, cooldown);

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error placing block", e);
                    activePlacingTasks.remove(zombieUUID);
                    cancel();
                }
            }
        };

        BukkitTask task = placeTask.runTaskTimer(plugin, 10L, 2L);
        activePlacingTasks.put(zombieUUID, task);
    }

    private void injectCustomAI(Zombie zombie) {
        try {
            com.destroystokyo.paper.entity.ai.MobGoals goals = Bukkit.getMobGoals();
            GoalKey<Zombie> safeMovementKey = GoalKey.of(Zombie.class,
                    new NamespacedKey(plugin, "safe_placement_movement"));

            SafePlacementGoal safeGoal = new SafePlacementGoal(zombie, this);
            goals.addGoal(zombie, 1, safeGoal);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to inject custom AI", e);
        }
    }

    private void removeCustomAI(Zombie zombie) {
        try {
            com.destroystokyo.paper.entity.ai.MobGoals goals = Bukkit.getMobGoals();
            GoalKey<Zombie> safeMovementKey = GoalKey.of(Zombie.class,
                    new NamespacedKey(plugin, "safe_placement_movement"));
            goals.removeGoal(zombie, safeMovementKey);
        } catch (Exception e) {
            // Ignore
        }
    }

    private ItemStack getPlaceableBlocks(Zombie zombie) {
        ItemStack mainHand = zombie.getEquipment().getItemInMainHand();
        if (mainHand != null && mainHand.getType().isBlock() && mainHand.getType().isSolid()) {
            return mainHand;
        }

        ItemStack offHand = zombie.getEquipment().getItemInOffHand();
        if (offHand != null && offHand.getType().isBlock() && offHand.getType().isSolid()) {
            return offHand;
        }

        return null;
    }

    private void lookAtLocation(Zombie zombie, Location target) {
        Location zombieLoc = zombie.getEyeLocation();
        Vector direction = target.toVector().subtract(zombieLoc.toVector()).normalize();

        Location lookAt = zombieLoc.clone();
        lookAt.setDirection(direction);

        zombie.teleport(new Location(zombie.getWorld(),
                zombie.getLocation().getX(),
                zombie.getLocation().getY(),
                zombie.getLocation().getZ(),
                lookAt.getYaw(),
                lookAt.getPitch()));
    }

    private boolean wasRecentlyPlaced(Location location, long cooldownMs) {
        long now = System.currentTimeMillis();
        return recentlyPlacedBlocks.entrySet().stream()
                .anyMatch(entry ->
                        entry.getKey().getWorld().equals(location.getWorld()) &&
                                entry.getKey().distanceSquared(location) < 1 &&
                                now - entry.getValue() < cooldownMs
                );
    }

    private void cleanupOldData() {
        long now = System.currentTimeMillis();
        long maxAge = (long) Feature.ZOMBIE_PLACE_BLOCK.getDouble("placement-cooldown-ms") * 5;

        recentlyPlacedBlocks.entrySet().removeIf(entry -> now - entry.getValue() > maxAge);
        zombiePlaceCooldown.entrySet().removeIf(entry -> now - entry.getValue() > maxAge);
    }

    private void cleanupZombie(Zombie zombie) {
        UUID uuid = zombie.getUniqueId();
        BukkitTask task = activePlacingTasks.remove(uuid);
        if (task != null) task.cancel();
        zombiePathData.remove(uuid);
        zombiePlaceCooldown.remove(uuid);
        zombiesWithCustomAI.remove(uuid);
    }

    private class SafePlacementGoal implements Goal<Zombie> {
        private final Zombie zombie;
        private final ZombiePlaceFeature feature;
        private final GoalKey<Zombie> key;

        public SafePlacementGoal(Zombie zombie, ZombiePlaceFeature feature) {
            this.zombie = zombie;
            this.feature = feature;
            this.key = GoalKey.of(Zombie.class,
                    new NamespacedKey(plugin, "safe_placement_movement"));
        }

        @Override
        public boolean shouldActivate() {
            return zombie.getTarget() instanceof Player &&
                    feature.activePlacingTasks.containsKey(zombie.getUniqueId());
        }

        @Override
        public void tick() {
            Location loc = zombie.getLocation();
            Block below = loc.getBlock().getRelative(BlockFace.DOWN);

            if (!below.getType().isSolid()) {
                zombie.setVelocity(new Vector(0, zombie.getVelocity().getY(), 0));
            }
        }

        @Override
        public GoalKey<Zombie> getKey() {
            return key;
        }

        @Override
        public EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.MOVE);
        }
    }

    private static class PathfinderData {
        Location lastTargetLocation;
        long lastCheckTime;
        List<Location> plannedPath = new ArrayList<>();
    }

    private static class PlacementSituation {
        boolean needsPlacement;
        Location placeLocation;
        BlockFace placeFace;
        PlacementType type;
    }

    private enum PlacementType {
        BRIDGE, CLIMB, DESCEND, AROUND
    }

    private static class GapInfo {
        boolean hasGap;
        Location placeLocation;
        BlockFace placeFace;
        int gapDistance;
    }

    private static class ClimbInfo {
        boolean canClimb;
        Location placeLocation;
        BlockFace placeFace;
        int climbHeight;
    }

    private static class DescendInfo {
        boolean needsPlatform;
        Location placeLocation;
        BlockFace placeFace;
    }

    private static class AroundInfo {
        boolean canGoAround;
        Location placeLocation;
        BlockFace placeFace;
    }

    private static class ObstacleInfo {
        boolean isBlocked;
        Location obstacleLocation;
    }
}