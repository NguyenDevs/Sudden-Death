package org.nguyendevs.suddendeath.features.mob.hostile;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class ZombieBreakBlockFeature extends AbstractFeature {

    private final Map<UUID, BukkitTask> activeBreakingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Location>> recentlyBrokenBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> breakAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBreakTime = new ConcurrentHashMap<>();

    // Hệ thống Target thông minh được chuyển vào đây vì nó phục vụ chính cho tính năng Break Block
    private final Map<UUID, UUID> persistentTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTargetTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTargetSearchTime = new ConcurrentHashMap<>();

    private static final long MEMORY_DURATION = 2000;
    private static final double MAX_TARGET_DISTANCE_SQUARED = 150.0 * 150.0;
    private static final long TARGET_SEARCH_COOLDOWN = 2000;

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
                        if (Feature.ZOMBIE_BREAK_BLOCK.isEnabled(world)) {
                            List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));
                            for (Zombie zombie : zombies) {
                                long randomDelay = ThreadLocalRandom.current().nextLong(0, 5);
                                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                                    try {
                                        if (zombie.isValid() && (zombie.getTarget() instanceof Player || zombie.getTarget() instanceof Villager)) {
                                            if (plugin.getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), CustomFlag.SDS_BREAK)) {
                                                processZombieBreakBlock(zombie);
                                            }
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().log(Level.WARNING, "Error processing zombie break", e);
                                    }
                                }, randomDelay);
                            }
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
                        List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));
                        for (Zombie zombie : zombies) {
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
        recentlyBrokenBlocks.clear();
        breakAttempts.clear();
        lastBreakTime.clear();
        persistentTargets.clear();
        lastTargetTime.clear();
        lastTargetSearchTime.clear();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            cleanupZombieBreaking(zombie);
            persistentTargets.remove(zombie.getUniqueId());
            lastTargetTime.remove(zombie.getUniqueId());
            lastTargetSearchTime.remove(zombie.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        UUID zombieUUID = zombie.getUniqueId();

        if (event.getTarget() instanceof Player player) {
            persistentTargets.put(zombieUUID, player.getUniqueId());
            lastTargetTime.put(zombieUUID, System.currentTimeMillis());
        }

        if (event.getTarget() == null && persistentTargets.containsKey(zombieUUID)) {
            UUID targetUUID = persistentTargets.get(zombieUUID);
            Player persistentPlayer = Bukkit.getPlayer(targetUUID);
            if (persistentPlayer != null && persistentPlayer.isOnline()) {
                if (zombie.getWorld().equals(persistentPlayer.getWorld()) &&
                        zombie.getLocation().distanceSquared(persistentPlayer.getLocation()) <= MAX_TARGET_DISTANCE_SQUARED) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (zombie.isValid() && zombie.getTarget() == null) {
                            zombie.setTarget(persistentPlayer);
                        }
                    });
                    event.setCancelled(true);
                }
            }
        }
    }

    private void searchForNearbyPlayers(Zombie zombie) {
        if (!zombie.isValid()) return;

        UUID zombieUUID = zombie.getUniqueId();
        Long lastSearch = lastTargetSearchTime.get(zombieUUID);
        if (lastSearch != null && System.currentTimeMillis() - lastSearch < TARGET_SEARCH_COOLDOWN) {
            return;
        }

        if (zombie.getTarget() instanceof Player) {
            lastTargetSearchTime.put(zombieUUID, System.currentTimeMillis());
            return;
        }

        Player closestPlayer = null;
        double closestDistanceSq = MAX_TARGET_DISTANCE_SQUARED;

        Location zombieLoc = zombie.getLocation();
        for (Player player : zombie.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE) continue;
            if (!player.isOnline() || player.isDead()) continue;

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
                    lastTargetTime.put(zombieUUID, System.currentTimeMillis());
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

            if (!zombie.getWorld().equals(target.getWorld())) {
                zombie.setTarget(null);
                return true;
            }

            if (zombie.getLocation().distanceSquared(target.getLocation()) > MAX_TARGET_DISTANCE_SQUARED) {
                zombie.setTarget(null);
                return true;
            }

            if (zombie.getTarget() == null) zombie.setTarget(target);
            return false;
        });
    }

    // --- Logic Phá Block ---

    private void processZombieBreakBlock(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();
        if (activeBreakingTasks.containsKey(zombieUUID)) return;
        Bukkit.getScheduler().runTask(plugin, () -> applyZombieBreakBlock(zombie));
    }

    private void applyZombieBreakBlock(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0) return;
        UUID zombieUUID = zombie.getUniqueId();
        if (activeBreakingTasks.containsKey(zombieUUID)) return;

        LivingEntity target = getZombieTarget(zombie);
        if (target == null) return;

        if (!target.getWorld().equals(zombie.getWorld())) return;

        ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
        Material toolType = itemInHand != null ? itemInHand.getType() : Material.AIR;
        if (!isValidTool(toolType)) return;

        Block targetBlock = selectBlockToBreak(zombie, target, toolType);
        if (targetBlock == null) return;

        double breakTimeTicks = calculateBreakTime(toolType, targetBlock.getType()) * 0.25;
        if (breakTimeTicks < 5) breakTimeTicks = 5;

        startBreakingBlock(zombie, target, targetBlock, itemInHand, breakTimeTicks);
    }

    private LivingEntity getZombieTarget(Zombie zombie) {
        UUID persistentTargetUUID = persistentTargets.get(zombie.getUniqueId());
        if (persistentTargetUUID != null) {
            Player persistentPlayer = Bukkit.getPlayer(persistentTargetUUID);
            if (persistentPlayer != null && persistentPlayer.isOnline() &&
                    zombie.getWorld().equals(persistentPlayer.getWorld()) &&
                    zombie.getLocation().distanceSquared(persistentPlayer.getLocation()) <= MAX_TARGET_DISTANCE_SQUARED) {
                return persistentPlayer;
            }
        }
        if (zombie.getTarget() instanceof Player) return (Player) zombie.getTarget();
        if (zombie.getTarget() instanceof Villager) return (Villager) zombie.getTarget();
        return null;
    }

    private Block selectBlockToBreak(Zombie zombie, LivingEntity target, Material toolType) {
        Location zombieEyeLoc = zombie.getEyeLocation();
        Vector direction = target.getLocation().toVector().subtract(zombieEyeLoc.toVector()).normalize();

        BlockIterator iterator = new BlockIterator(zombie.getWorld(), zombieEyeLoc.toVector(), direction, 0, 3);
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid() && canBreakBlock(toolType, block.getType())) return block;
            if (!block.getType().isSolid()) {
                Block blockBelow = block.getRelative(0, -1, 0);
                if (blockBelow.getType().isSolid() && canBreakBlock(toolType, blockBelow.getType())) {
                    if (target.getLocation().getY() < zombie.getLocation().getY()) return blockBelow;
                }
                Block blockAbove = block.getRelative(0, 1, 0);
                if (blockAbove.getType().isSolid() && canBreakBlock(toolType, blockAbove.getType())) return blockAbove;
            }
        }
        return null;
    }

    private void startBreakingBlock(Zombie zombie, LivingEntity target, Block block, ItemStack tool, double breakTime) {
        UUID zombieUUID = zombie.getUniqueId();

        BukkitRunnable breakTask = new BukkitRunnable() {
            int ticksElapsed = 0;
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            int entityId = RANDOM.nextInt(Integer.MAX_VALUE);
            Location breakLocation = zombie.getLocation().clone();

            @Override
            public void run() {
                try {
                    if (!zombie.isValid() || !target.isValid() || !block.getType().isSolid()) {
                        cleanup();
                        return;
                    }

                    if (zombie.getLocation().distanceSquared(breakLocation) > 3.0) {
                        cleanup();
                        return;
                    }

                    zombie.setTarget(null);

                    Location lookAt = block.getLocation().add(0.5, 0.5, 0.5);
                    Vector dir = lookAt.toVector().subtract(zombie.getEyeLocation().toVector());
                    Location newLoc = zombie.getLocation().setDirection(dir);
                    zombie.teleport(newLoc);

                    ticksElapsed++;

                    if (ticksElapsed < breakTime) {
                        int destroyStage = Math.min(9, (int) ((ticksElapsed / breakTime) * 10));
                        sendBreakAnimation(target, entityId, block, destroyStage);

                        if (ticksElapsed % 2 == 0) zombie.swingMainHand();

                        if (ticksElapsed % 4 == 0) {
                            Sound hitSound = block.getType().createBlockData().getSoundGroup().getHitSound();
                            zombie.getWorld().playSound(block.getLocation(), hitSound, 1.0f, 1.5f + (float)(Math.random() * 0.5));
                        }
                        return;
                    }

                    zombie.swingMainHand();
                    Sound breakSound = block.getType().createBlockData().getSoundGroup().getBreakSound();
                    zombie.getWorld().playSound(block.getLocation(), breakSound, 1.2f, 1.2f);
                    sendBreakAnimation(target, entityId, block, -1);
                    zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 50, 0.4, 0.4, 0.4, 0.1, block.getBlockData());

                    block.breakNaturally(tool);

                    zombie.setTarget(target);
                    cleanup();

                    Bukkit.getScheduler().runTask(plugin, () -> applyZombieBreakBlock(zombie));

                } catch (Exception e) {
                    cleanup();
                }
            }

            private void cleanup() {
                activeBreakingTasks.remove(zombieUUID);
                sendBreakAnimation(target, entityId, block, -1);
                cancel();
            }

            private void sendBreakAnimation(LivingEntity entity, int id, Block b, int stage) {
                try {
                    PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
                    packet.getIntegers().write(0, id);
                    packet.getBlockPositionModifier().write(0, new BlockPosition(b.getX(), b.getY(), b.getZ()));
                    packet.getIntegers().write(1, stage);
                    for (Player nearbyPlayer : b.getWorld().getPlayers()) {
                        if (nearbyPlayer.isOnline() && nearbyPlayer.getLocation().distanceSquared(b.getLocation()) <= 1024) {
                            protocolManager.sendServerPacket(nearbyPlayer, packet);
                        }
                    }
                } catch (Exception ignored) {}
            }
        };

        BukkitTask task = breakTask.runTaskTimer(plugin, 0L, 1L);
        activeBreakingTasks.put(zombieUUID, task);
    }

    private void cleanupOldMemories() {
        long now = System.currentTimeMillis();
        lastBreakTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > MEMORY_DURATION) {
                recentlyBrokenBlocks.remove(entry.getKey());
                return true;
            }
            return false;
        });
        lastTargetSearchTime.entrySet().removeIf(entry -> now - entry.getValue() > TARGET_SEARCH_COOLDOWN * 10);
    }

    private void cleanupZombieBreaking(Zombie zombie) {
        UUID uuid = zombie.getUniqueId();
        BukkitTask task = activeBreakingTasks.remove(uuid);
        if (task != null) task.cancel();
        recentlyBrokenBlocks.remove(uuid);
        breakAttempts.remove(uuid);
        lastBreakTime.remove(uuid);
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
        String modifierKey = isPickaxe(tool) ? "breakable-pickaxe-blocks" : isShovel(tool) ? "breakable-shovel-blocks" : "breakable-axe-blocks";
        String blockListString = Feature.ZOMBIE_BREAK_BLOCK.getString(modifierKey);
        if (blockListString == null || blockListString.isEmpty()) return false;
        return blockListString.contains(block.name());
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

    private void rememberBrokenBlock(UUID uuid, Block block) {
        recentlyBrokenBlocks.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(block.getLocation());
        lastBreakTime.put(uuid, System.currentTimeMillis());
        breakAttempts.put(uuid, 0);
    }

    private boolean wasRecentlyBroken(UUID uuid, Block block) {
        Set<Location> broken = recentlyBrokenBlocks.get(uuid);
        return broken != null && broken.contains(block.getLocation());
    }
}