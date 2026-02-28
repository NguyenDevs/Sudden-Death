package org.nguyendevs.suddendeath.Features.mob.hostile;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Hook.CustomFlag;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class ZombieBreakBlockFeature extends AbstractFeature {

    private final Map<UUID, BukkitTask> activeBreakingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> zombieDroppedItems = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> persistentTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTargetSearchTime = new ConcurrentHashMap<>();

    private static final long TARGET_SEARCH_COOLDOWN = 2000;

    private double getMaxTargetDistanceSquared() {
        double dist = Feature.ZOMBIE_BREAK_BLOCK.getDouble("max-target-distance");
        return dist * dist;
    }

    private boolean isValidGameMode(GameMode mode) {
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    @Override
    public String getName() {
        return "Zombie Break Block";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (!Feature.ZOMBIE_BREAK_BLOCK.isEnabled(world)) continue;
                        List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));
                        for (Zombie zombie : zombies) {
                            long randomDelay = ThreadLocalRandom.current().nextLong(0, 5);
                            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                                try {
                                    if (zombie.isValid() &&
                                            (zombie.getTarget() instanceof Player || zombie.getTarget() instanceof Villager) &&
                                            plugin.getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), CustomFlag.SDS_BREAK)) {
                                        processZombieBreakBlock(zombie);
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.WARNING, "Error processing zombie break", e);
                                }
                            }, randomDelay);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Zombie Break Block loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L));

        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldMemories();
            }
        }.runTaskTimerAsynchronously(plugin, 200L, 200L));

        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                maintainPersistentTargets();
            }
        }.runTaskTimer(plugin, 0L, 10L));

        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        for (Zombie zombie : new ArrayList<>(world.getEntitiesByClass(Zombie.class))) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                try {
                                    searchForNearbyPlayers(zombie);
                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.WARNING, "Error searching for players", e);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in player search loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    @Override
    protected void onDisable() {
        activeBreakingTasks.values().forEach(BukkitTask::cancel);
        activeBreakingTasks.clear();
        persistentTargets.clear();
        lastTargetSearchTime.clear();
        zombieDroppedItems.clear();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        UUID uuid = zombie.getUniqueId();
        cleanupZombieBreaking(zombie);
        persistentTargets.remove(uuid);
        lastTargetSearchTime.remove(uuid);
        zombieDroppedItems.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        UUID zombieUUID = zombie.getUniqueId();

        if (event.getTarget() instanceof Player player) {
            if (!isValidGameMode(player.getGameMode())) {
                event.setCancelled(true);
                return;
            }
            persistentTargets.put(zombieUUID, player.getUniqueId());
            return;
        }

        if (event.getTarget() != null || !persistentTargets.containsKey(zombieUUID)) return;

        Player persistentPlayer = Bukkit.getPlayer(persistentTargets.get(zombieUUID));
        if (persistentPlayer == null || !persistentPlayer.isOnline()) return;
        if (!isValidGameMode(persistentPlayer.getGameMode())) {
            persistentTargets.remove(zombieUUID);
            return;
        }
        if (!zombie.getWorld().equals(persistentPlayer.getWorld()) ||
                zombie.getLocation().distanceSquared(persistentPlayer.getLocation()) > getMaxTargetDistanceSquared()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (zombie.isValid() && zombie.getTarget() == null) zombie.setTarget(persistentPlayer);
        });
        event.setCancelled(true);
    }

    private void searchForNearbyPlayers(Zombie zombie) {
        if (!zombie.isValid()) return;

        UUID zombieUUID = zombie.getUniqueId();
        Long lastSearch = lastTargetSearchTime.get(zombieUUID);
        if (lastSearch != null && System.currentTimeMillis() - lastSearch < TARGET_SEARCH_COOLDOWN) return;

        if (zombie.getTarget() instanceof Player target) {
            if (!isValidGameMode(target.getGameMode())) {
                Bukkit.getScheduler().runTask(plugin, () -> zombie.setTarget(null));
                persistentTargets.remove(zombieUUID);
            } else {
                lastTargetSearchTime.put(zombieUUID, System.currentTimeMillis());
            }
            return;
        }

        Location zombieLoc = zombie.getLocation();
        Player closestPlayer = null;
        double closestDistanceSq = getMaxTargetDistanceSquared();

        for (Player player : zombie.getWorld().getPlayers()) {
            if (!isValidGameMode(player.getGameMode()) || !player.isOnline() || player.isDead()) continue;
            double distSq = zombieLoc.distanceSquared(player.getLocation());
            if (distSq < closestDistanceSq) {
                closestPlayer = player;
                closestDistanceSq = distSq;
            }
        }

        if (closestPlayer != null) {
            Player finalTarget = closestPlayer;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (zombie.isValid() && finalTarget.isOnline()) {
                    zombie.setTarget(finalTarget);
                    persistentTargets.put(zombieUUID, finalTarget.getUniqueId());
                }
            });
        }

        lastTargetSearchTime.put(zombieUUID, System.currentTimeMillis());
    }

    private void maintainPersistentTargets() {
        persistentTargets.entrySet().removeIf(entry -> {
            Entity zombieEntity = Bukkit.getEntity(entry.getKey());
            if (!(zombieEntity instanceof Zombie zombie) || !zombie.isValid()) return true;

            Player target = Bukkit.getPlayer(entry.getValue());
            if (target == null || !target.isOnline()) return true;

            if (!isValidGameMode(target.getGameMode())) {
                zombie.setTarget(null);
                return true;
            }
            if (!zombie.getWorld().equals(target.getWorld()) ||
                    zombie.getLocation().distanceSquared(target.getLocation()) > getMaxTargetDistanceSquared()) {
                zombie.setTarget(null);
                return true;
            }

            if (zombie.getTarget() == null) zombie.setTarget(target);
            return false;
        });
    }

    private void processZombieBreakBlock(Zombie zombie) {
        if (activeBreakingTasks.containsKey(zombie.getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> applyZombieBreakBlock(zombie));
    }

    private void applyZombieBreakBlock(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0) return;
        if (activeBreakingTasks.containsKey(zombie.getUniqueId())) return;

        LivingEntity target = getZombieTarget(zombie);
        if (target == null || !target.getWorld().equals(zombie.getWorld())) return;

        ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
        Material toolType = itemInHand.getType();
        if (!isValidTool(toolType)) return;

        Block targetBlock = selectBlockToBreak(zombie, target, toolType);
        if (targetBlock == null) return;

        double breakTimeTicks = Math.max(5, calculateBreakTime(toolType, targetBlock.getType()) * 0.25);
        startBreakingBlock(zombie, target, targetBlock, itemInHand, breakTimeTicks);
    }

    private LivingEntity getZombieTarget(Zombie zombie) {
        UUID persistentTargetUUID = persistentTargets.get(zombie.getUniqueId());
        if (persistentTargetUUID != null) {
            Player persistentPlayer = Bukkit.getPlayer(persistentTargetUUID);
            if (persistentPlayer != null && persistentPlayer.isOnline() &&
                    zombie.getWorld().equals(persistentPlayer.getWorld()) &&
                    zombie.getLocation().distanceSquared(persistentPlayer.getLocation()) <= getMaxTargetDistanceSquared() &&
                    isValidGameMode(persistentPlayer.getGameMode())) {
                return persistentPlayer;
            }
        }
        if (zombie.getTarget() instanceof Player player) return player;
        if (zombie.getTarget() instanceof Villager villager) return villager;
        return null;
    }

    private Block selectBlockToBreak(Zombie zombie, LivingEntity target, Material toolType) {
        Location zombieLoc = zombie.getLocation();
        Location targetLoc = target.getLocation();
        Location zombieEyeLoc = zombie.getEyeLocation();

        if (targetLoc.getY() > zombieLoc.getY() + 1.0) {
            Vector direction = targetLoc.toVector().subtract(zombieLoc.toVector()).setY(0).normalize();
            Block blockInFrontFeet = zombieLoc.clone().add(direction).getBlock();

            if (blockInFrontFeet.getType().isSolid()) {
                Block blockInFrontHead = blockInFrontFeet.getRelative(BlockFace.UP);
                Block blockInFrontAboveHead = blockInFrontHead.getRelative(BlockFace.UP);
                if (blockInFrontHead.getType().isSolid() && canBreakBlock(toolType, blockInFrontHead.getType()))
                    return blockInFrontHead;
                if (blockInFrontAboveHead.getType().isSolid() && canBreakBlock(toolType, blockInFrontAboveHead.getType()))
                    return blockInFrontAboveHead;
                return null;
            }
        }

        Vector direction = targetLoc.toVector().subtract(zombieEyeLoc.toVector());
        if (direction.lengthSquared() < 0.01 || !isValidVector(direction)) return null;
        direction.normalize();

        if (!zombieEyeLoc.isWorldLoaded() || zombieEyeLoc.getWorld() == null) return null;

        BlockIterator iterator;
        try {
            iterator = new BlockIterator(zombie.getWorld(), zombieEyeLoc.toVector(), direction, 0, 3);
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("Failed to create BlockIterator for zombie " + zombie.getUniqueId());
            return null;
        }

        Block targetFloorBlock = targetLoc.getBlock().getRelative(BlockFace.DOWN);
        Block zombieFloorBlock = zombieLoc.getBlock().getRelative(BlockFace.DOWN);
        boolean sameLevel = Math.abs(zombieLoc.getY() - targetLoc.getY()) < 0.8;

        while (iterator.hasNext()) {
            Block block = iterator.next();

            if (sameLevel) {
                if (block.equals(targetFloorBlock) || block.equals(zombieFloorBlock)) continue;
                if (block.getY() < zombieLoc.getBlockY()) continue;
            }

            if (block.getType().isSolid()) {
                if (canBreakBlock(toolType, block.getType()) &&
                        !block.equals(targetFloorBlock) && !block.equals(zombieFloorBlock)) {
                    return block;
                }
                return null;
            }

            if (targetLoc.getY() < zombieLoc.getY() - 1.5) {
                Block blockBelow = block.getRelative(BlockFace.DOWN);
                if (!blockBelow.equals(targetFloorBlock) && !blockBelow.equals(zombieFloorBlock) &&
                        blockBelow.getType().isSolid() && canBreakBlock(toolType, blockBelow.getType())) {
                    return blockBelow;
                }
            }
        }

        return null;
    }

    private boolean isValidVector(Vector vec) {
        if (vec == null) return false;
        double x = vec.getX(), y = vec.getY(), z = vec.getZ();
        return !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z) &&
                !Double.isInfinite(x) && !Double.isInfinite(y) && !Double.isInfinite(z) &&
                vec.lengthSquared() > 0.0001;
    }

    private void startBreakingBlock(Zombie zombie, LivingEntity target, Block block, ItemStack tool, double breakTime) {
        UUID zombieUUID = zombie.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;
            final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            final int entityId = RANDOM.nextInt(Integer.MAX_VALUE);
            final Location breakLocation = zombie.getLocation().clone();

            @Override
            public void run() {
                try {
                    if (!zombie.isValid() || !target.isValid() || !block.getType().isSolid() ||
                            zombie.getLocation().distanceSquared(breakLocation) > 3.0) {
                        cleanup();
                        return;
                    }

                    Location lookAt = block.getLocation().add(0.5, 0.5, 0.5);
                    zombie.setTarget(null);
                    zombie.teleport(zombie.getLocation().setDirection(
                            lookAt.toVector().subtract(zombie.getEyeLocation().toVector())));

                    ticksElapsed++;

                    if (ticksElapsed < breakTime) {
                        int destroyStage = Math.min(9, (int) ((ticksElapsed / breakTime) * 10));
                        sendBreakAnimation(entityId, block, destroyStage);
                        if (ticksElapsed % 2 == 0) zombie.swingMainHand();
                        if (ticksElapsed % 4 == 0) {
                            zombie.getWorld().playSound(block.getLocation(),
                                    block.getType().createBlockData().getSoundGroup().getHitSound(),
                                    1.0f, 1.5f + (float) (Math.random() * 0.5));
                        }
                        return;
                    }

                    zombie.swingMainHand();
                    zombie.getWorld().playSound(block.getLocation(),
                            block.getType().createBlockData().getSoundGroup().getBreakSound(), 1.2f, 1.2f);
                    sendBreakAnimation(entityId, block, -1);
                    zombie.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5),
                            50, 0.4, 0.4, 0.4, 0.1, block.getBlockData());

                    if (Feature.ZOMBIE_BREAK_BLOCK.getBoolean("drop-blocks")) {
                        handleBlockDrop(zombieUUID, block, tool);
                    } else {
                        block.setType(Material.AIR);
                    }

                    zombie.setTarget(target);
                    cleanup();
                    Bukkit.getScheduler().runTask(plugin, () -> applyZombieBreakBlock(zombie));

                } catch (Exception e) {
                    cleanup();
                }
            }

            private void cleanup() {
                activeBreakingTasks.remove(zombieUUID);
                sendBreakAnimation(entityId, block, -1);
                cancel();
            }

            private void sendBreakAnimation(int id, Block b, int stage) {
                try {
                    PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
                    packet.getIntegers().write(0, id);
                    packet.getBlockPositionModifier().write(0, new BlockPosition(b.getX(), b.getY(), b.getZ()));
                    packet.getIntegers().write(1, stage);
                    for (Player nearbyPlayer : b.getWorld().getPlayers()) {
                        if (nearbyPlayer.isOnline() &&
                                nearbyPlayer.getLocation().distanceSquared(b.getLocation()) <= 1024) {
                            protocolManager.sendServerPacket(nearbyPlayer, packet);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeBreakingTasks.put(zombieUUID, task);
    }

    private void handleBlockDrop(UUID zombieUUID, Block block, ItemStack tool) {
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        Set<UUID> existingItems = new HashSet<>();
        for (Entity entity : dropLoc.getWorld().getNearbyEntities(dropLoc, 1.5, 1.5, 1.5)) {
            if (entity instanceof Item) existingItems.add(entity.getUniqueId());
        }

        block.breakNaturally(tool);

        Set<UUID> newDroppedItems = new HashSet<>();
        for (Entity entity : dropLoc.getWorld().getNearbyEntities(dropLoc, 1.5, 1.5, 1.5)) {
            if (entity instanceof Item && !existingItems.contains(entity.getUniqueId())) {
                newDroppedItems.add(entity.getUniqueId());
            }
        }

        if (newDroppedItems.isEmpty()) return;
        zombieDroppedItems.computeIfAbsent(zombieUUID, k -> ConcurrentHashMap.newKeySet()).addAll(newDroppedItems);

        double removeInterval = Feature.ZOMBIE_BREAK_BLOCK.getDouble("drop-remove-interval");
        if (removeInterval <= 0) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Set<UUID> itemsToRemove = zombieDroppedItems.get(zombieUUID);
            if (itemsToRemove == null) return;
            for (UUID itemUUID : new HashSet<>(itemsToRemove)) {
                Entity entity = Bukkit.getEntity(itemUUID);
                if (entity instanceof Item && entity.isValid()) {
                    entity.remove();
                    itemsToRemove.remove(itemUUID);
                }
            }
        }, (long) (removeInterval * 20L));
    }

    private void cleanupOldMemories() {
        long now = System.currentTimeMillis();
        lastTargetSearchTime.entrySet().removeIf(entry -> now - entry.getValue() > TARGET_SEARCH_COOLDOWN * 10);
    }

    private void cleanupZombieBreaking(Zombie zombie) {
        UUID uuid = zombie.getUniqueId();
        BukkitTask task = activeBreakingTasks.remove(uuid);
        if (task != null) task.cancel();
        zombieDroppedItems.remove(uuid);
    }

    private boolean isValidTool(Material material) {
        return isPickaxe(material) || isShovel(material) || isAxe(material);
    }

    private boolean isPickaxe(Material material) {
        return material.name().contains("PICKAXE");
    }

    private boolean isShovel(Material material) {
        return material.name().contains("SHOVEL");
    }

    private boolean isAxe(Material material) {
        return material.name().contains("_AXE");
    }

    private boolean canBreakBlock(Material tool, Material block) {
        if (block.getHardness() < 0) return false;
        String modifierKey;
        if (isPickaxe(tool)) modifierKey = "breakable-pickaxe-blocks";
        else if (isShovel(tool)) modifierKey = "breakable-shovel-blocks";
        else modifierKey = "breakable-axe-blocks";
        String blockListString = Feature.ZOMBIE_BREAK_BLOCK.getString(modifierKey);
        return !blockListString.isEmpty() && blockListString.contains(block.name());
    }

    private double calculateBreakTime(Material tool, Material block) {
        float blockHardness = block.getHardness();
        if (blockHardness < 0) return -1;
        float toolMultiplier = 1.0f;
        if (tool.name().contains("DIAMOND")) toolMultiplier = 8.0f;
        else if (tool.name().contains("IRON")) toolMultiplier = 6.0f;
        else if (tool.name().contains("STONE")) toolMultiplier = 4.0f;
        else if (tool.name().contains("WOODEN")) toolMultiplier = 2.0f;
        else if (tool.name().contains("GOLDEN")) toolMultiplier = 12.0f;
        else if (tool.name().contains("NETHERITE")) toolMultiplier = 9.0f;
        return blockHardness * 1.5f / toolMultiplier * 20.0;
    }
}