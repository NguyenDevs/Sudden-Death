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
    // Map này lưu BukkitTask để có thể cancel khi cần
    private final Map<UUID, BukkitTask> activePlacingTasks = new ConcurrentHashMap<>();
    private final Map<Location, Long> recentlyPlacedBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> zombiePlaceCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> zombiesWithCustomAI = ConcurrentHashMap.newKeySet();
    private Set<Material> placeableBlocks;

    private static final double MAX_TARGET_DISTANCE_SQUARED = 150.0 * 150.0;

    @Override
    public String getName() {
        return "Zombie Place Feature";
    }

    @Override
    protected void onEnable() {
        loadPlaceableBlocks();

        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ZOMBIE_PLACE_BLOCK.isEnabled(world)) {
                            List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));
                            for (Zombie zombie : zombies) {
                                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                                    try {
                                        if (zombie.isValid() && zombie.getTarget() instanceof Player) {
                                            processZombiePlacement(zombie);
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().log(Level.WARNING, "Error processing zombie placement", e);
                                    }
                                }, 0L);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Zombie Place task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L));

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
            if (entity instanceof Zombie zombie) removeCustomAI(zombie);
        }
        zombiesWithCustomAI.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) cleanupZombie(zombie);
    }

    private void loadPlaceableBlocks() {
        placeableBlocks = new HashSet<>();
        String blocksStr = Feature.ZOMBIE_PLACE_BLOCK.getString("placeable-blocks");
        if (blocksStr != null && !blocksStr.isEmpty()) {
            for (String materialName : blocksStr.split(",")) {
                try {
                    Material material = Material.valueOf(materialName.trim().toUpperCase());
                    if (material.isBlock() && material.isSolid()) placeableBlocks.add(material);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (placeableBlocks.isEmpty()) {
            placeableBlocks.addAll(Arrays.asList(Material.COBBLESTONE, Material.STONE, Material.DIRT, Material.NETHERRACK));
        }
    }

    private void processZombiePlacement(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();

        long cooldownMs = 100;
        Long lastPlace = zombiePlaceCooldown.get(zombieUUID);
        if (lastPlace != null && System.currentTimeMillis() - lastPlace < cooldownMs) return;

        if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("ai-freeze-while-placing") && !zombiesWithCustomAI.contains(zombieUUID)) {
            Bukkit.getScheduler().runTask(plugin, () -> injectCustomAI(zombie));
            zombiesWithCustomAI.add(zombieUUID);
        }

        if (!(zombie.getTarget() instanceof Player target)) return;
        if (!plugin.getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), CustomFlag.SDS_PLACE)) return;
        if (activePlacingTasks.containsKey(zombieUUID)) return;

        ItemStack heldItem = getPlaceableBlocks(zombie);
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            Bukkit.getScheduler().runTask(plugin, () -> tryAcquireBlock(zombie));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> analyzeAndPlace(zombie, target));
    }

    private void tryAcquireBlock(Zombie zombie) {
        List<Entity> nearbyEntities = zombie.getNearbyEntities(3, 2, 3);
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Item itemEntity) {
                ItemStack stack = itemEntity.getItemStack();
                if (placeableBlocks.contains(stack.getType())) {
                    zombie.getEquipment().setItemInMainHand(stack);
                    zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                    itemEntity.remove();
                    return;
                }
            }
        }

        if (zombie.isOnGround()) {
            Block blockBelow = zombie.getLocation().getBlock().getRelative(BlockFace.DOWN);
            Material type = blockBelow.getType();
            if (placeableBlocks.contains(type) && type != Material.BEDROCK && type != Material.OBSIDIAN) {
                zombie.getEquipment().setItemInMainHand(new ItemStack(type, 64));
                zombie.getWorld().playEffect(blockBelow.getLocation(), Effect.STEP_SOUND, type);
                zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 0.5f);
            }
        }
    }

    private void analyzeAndPlace(Zombie zombie, Player target) {
        UUID zombieUUID = zombie.getUniqueId();
        Location targetLoc = target.getLocation();
        ItemStack blocks = getPlaceableBlocks(zombie);

        if (blocks == null || blocks.getType() == Material.AIR) return;

        PathfinderData pathData = zombiePathData.computeIfAbsent(zombieUUID, k -> new PathfinderData());
        PlacementSituation situation = analyzeSituation(zombie, target, pathData);

        if (situation.needsPlacement) {
            Location placeLocation = situation.placeLocation;
            if (wasRecentlyPlaced(placeLocation, 200)) return;
            executePlacement(zombie, target, placeLocation, situation.placeFace, blocks, situation.type);
        }
    }

    private PlacementSituation analyzeSituation(Zombie zombie, Player target, PathfinderData pathData) {
        Location zombieLoc = zombie.getLocation();
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.toVector().subtract(zombieLoc.toVector()).normalize();
        PlacementSituation situation = new PlacementSituation();
        double checkRadius = 3.0;

        if (targetLoc.getY() > zombieLoc.getY() + 2.5 && zombie.isOnGround()) {
            Location headLoc = zombieLoc.clone().add(0, 2, 0);
            if (headLoc.getBlock().getType().isAir()) {
                situation.needsPlacement = true;
                situation.placeLocation = zombieLoc.getBlock().getLocation();
                situation.placeFace = BlockFace.UP;
                situation.type = PlacementType.PILLAR;
                return situation;
            }
        }

        GapInfo gapInfo = detectGap(zombie, direction, checkRadius);
        if (gapInfo.hasGap) {
            situation.needsPlacement = true;
            situation.placeLocation = gapInfo.placeLocation;
            situation.placeFace = gapInfo.placeFace;
            situation.type = PlacementType.BRIDGE;
            return situation;
        }

        double heightDiff = targetLoc.getY() - zombieLoc.getY();
        if (heightDiff > 1.2) {
            ClimbInfo climbInfo = findClimbPlacement(zombie, direction);
            if (climbInfo.canClimb) {
                situation.needsPlacement = true;
                situation.placeLocation = climbInfo.placeLocation;
                situation.placeFace = climbInfo.placeFace;
                situation.type = PlacementType.CLIMB;
                return situation;
            }
        }

        situation.needsPlacement = false;
        return situation;
    }

    private GapInfo detectGap(Zombie zombie, Vector direction, double checkRadius) {
        GapInfo info = new GapInfo();
        Location checkLoc = zombie.getLocation().clone();
        for (int i = 1; i <= checkRadius; i++) {
            checkLoc.add(direction.clone().multiply(1));
            Block checkBlock = checkLoc.getBlock();
            Block belowBlock = checkBlock.getRelative(BlockFace.DOWN);
            if (!checkBlock.getType().isSolid() && !belowBlock.getType().isSolid()) {
                info.hasGap = true;
                info.placeLocation = belowBlock.getLocation();
                info.placeFace = BlockFace.UP;
                return info;
            }
        }
        info.hasGap = false;
        return info;
    }

    private ClimbInfo findClimbPlacement(Zombie zombie, Vector direction) {
        ClimbInfo info = new ClimbInfo();
        Location frontLoc = zombie.getLocation().clone().add(direction.clone().multiply(0.8));
        Block frontBlock = frontLoc.getBlock();

        if (!frontBlock.getType().isSolid()) {
            Block below = frontBlock.getRelative(BlockFace.DOWN);
            if (below.getType().isSolid()) {
                info.canClimb = true;
                info.placeLocation = frontBlock.getLocation();
                info.placeFace = BlockFace.UP;
                return info;
            }
        }
        info.canClimb = false;
        return info;
    }

    // === PHẦN SỬA LỖI CHÍNH ===
    private void executePlacement(Zombie zombie, Player target, Location placeLocation, BlockFace placeFace, ItemStack blocks, PlacementType type) {
        UUID zombieUUID = zombie.getUniqueId();

        if (type == PlacementType.PILLAR) {
            zombie.setVelocity(new Vector(0, 0.42, 0));

            // SỬA: Tạo BukkitRunnable và gọi runTaskLater để nhận về BukkitTask
            BukkitTask pillarTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!zombie.isValid()) return;
                    placeBlockLogic(zombie, placeLocation, blocks, true);
                    activePlacingTasks.remove(zombieUUID);
                }
            }.runTaskLater(plugin, 1L); // Lưu BukkitTask trả về, không phải BukkitRunnable

            activePlacingTasks.put(zombieUUID, pillarTask);
            return;
        }

        // SỬA: Tạo BukkitRunnable và gọi runTask để nhận về BukkitTask
        BukkitTask placeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!zombie.isValid() || !target.isValid()) {
                    activePlacingTasks.remove(zombieUUID);
                    cancel();
                    return;
                }
                placeBlockLogic(zombie, placeLocation, blocks, false);
                activePlacingTasks.remove(zombieUUID);
            }
        }.runTask(plugin); // Lưu BukkitTask trả về

        activePlacingTasks.put(zombieUUID, placeTask);
    }

    private void placeBlockLogic(Zombie zombie, Location loc, ItemStack blocks, boolean isPillar) {
        Block block = loc.getBlock();
        if (block.getType().isSolid() && !isPillar) return;

        lookAtLocation(zombie, loc);
        zombie.swingMainHand();

        Material blockType = blocks.getType();
        block.setType(blockType);

        zombie.getWorld().playSound(loc, blockType.createBlockData().getSoundGroup().getPlaceSound(), 1.0f, 1.0f);
        zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc.clone().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.1, blockType.createBlockData());

        recentlyPlacedBlocks.put(loc.clone(), System.currentTimeMillis());
        zombiePlaceCooldown.put(zombie.getUniqueId(), System.currentTimeMillis());

        if (Feature.ZOMBIE_PLACE_BLOCK.getBoolean("block-consume-on-place")) {
            blocks.setAmount(blocks.getAmount() - 1);
        }

        if (isPillar) {
            Location newLoc = zombie.getLocation();
            newLoc.setY(loc.getY() + 1.0);
            zombie.teleport(newLoc);
        }
    }

    private void injectCustomAI(Zombie zombie) {
        try {
            com.destroystokyo.paper.entity.ai.MobGoals goals = Bukkit.getMobGoals();
            GoalKey<Zombie> key = GoalKey.of(Zombie.class, new NamespacedKey(plugin, "safe_placement_movement"));
            goals.addGoal(zombie, 1, new SafePlacementGoal(zombie, this, key));
        } catch (Exception ignored) {}
    }

    private void removeCustomAI(Zombie zombie) {
        try {
            com.destroystokyo.paper.entity.ai.MobGoals goals = Bukkit.getMobGoals();
            GoalKey<Zombie> key = GoalKey.of(Zombie.class, new NamespacedKey(plugin, "safe_placement_movement"));
            goals.removeGoal(zombie, key);
        } catch (Exception ignored) {}
    }

    private ItemStack getPlaceableBlocks(Zombie zombie) {
        ItemStack main = zombie.getEquipment().getItemInMainHand();
        if (main != null && placeableBlocks.contains(main.getType())) return main;
        ItemStack off = zombie.getEquipment().getItemInOffHand();
        if (off != null && placeableBlocks.contains(off.getType())) return off;
        return null;
    }

    private void lookAtLocation(Zombie zombie, Location target) {
        Location zLoc = zombie.getEyeLocation();
        Vector dir = target.toVector().subtract(zLoc.toVector()).normalize();
        Location newLoc = zombie.getLocation();
        newLoc.setDirection(dir);
        zombie.teleport(newLoc);
    }

    private boolean wasRecentlyPlaced(Location location, long cooldownMs) {
        Long time = recentlyPlacedBlocks.get(location);
        return time != null && System.currentTimeMillis() - time < cooldownMs;
    }

    private void cleanupOldData() {
        long now = System.currentTimeMillis();
        recentlyPlacedBlocks.entrySet().removeIf(e -> now - e.getValue() > 2000);
        zombiePlaceCooldown.entrySet().removeIf(e -> now - e.getValue() > 2000);
    }

    private void cleanupZombie(Zombie zombie) {
        UUID uuid = zombie.getUniqueId();
        BukkitTask task = activePlacingTasks.remove(uuid);
        if (task != null) task.cancel();
        zombiePathData.remove(uuid);
        zombiePlaceCooldown.remove(uuid);
        zombiesWithCustomAI.remove(uuid);
    }

    private static class SafePlacementGoal implements Goal<Zombie> {
        private final Zombie zombie;
        private final ZombiePlaceFeature feature;
        private final GoalKey<Zombie> key;

        public SafePlacementGoal(Zombie zombie, ZombiePlaceFeature feature, GoalKey<Zombie> key) {
            this.zombie = zombie;
            this.feature = feature;
            this.key = key;
        }
        @Override public boolean shouldActivate() {
            return feature.activePlacingTasks.containsKey(zombie.getUniqueId());
        }
        @Override public void tick() {
            zombie.setVelocity(new Vector(0, zombie.getVelocity().getY(), 0));
        }
        @Override public GoalKey<Zombie> getKey() { return key; }
        @Override public EnumSet<GoalType> getTypes() { return EnumSet.of(GoalType.MOVE); }
    }

    private static class PathfinderData {
        Location lastTargetLocation;
        long lastCheckTime;
    }

    private static class PlacementSituation {
        boolean needsPlacement;
        Location placeLocation;
        BlockFace placeFace;
        PlacementType type;
    }

    private enum PlacementType { BRIDGE, CLIMB, PILLAR }

    private static class GapInfo {
        boolean hasGap;
        Location placeLocation;
        BlockFace placeFace;
    }

    private static class ClimbInfo {
        boolean canClimb;
        Location placeLocation;
        BlockFace placeFace;
    }
}