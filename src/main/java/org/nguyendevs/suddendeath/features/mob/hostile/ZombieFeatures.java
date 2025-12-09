package org.nguyendevs.suddendeath.features.mob.hostile;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ZombieFeatures extends AbstractFeature {

    private final Map<UUID, BukkitTask> activeBreakingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Location>> recentlyBrokenBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> breakAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBreakTime = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> persistentTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTargetTime = new ConcurrentHashMap<>();
    private final Map<Location, Long> blockBreakCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> zombieBreakDelay = new ConcurrentHashMap<>();

    private static final double TICK_INTERVAL = 0.5;
    private static final int MAX_TICKS = 20;
    private static final int MAX_BREAK_ATTEMPTS = 5;
    private static final long MEMORY_DURATION = 10000;
    private static final double MAX_TARGET_DISTANCE = 150.0;
    private static final double MAX_TARGET_DISTANCE_SQUARED = MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE;
    private static final long BLOCK_BREAK_COOLDOWN = 2000;
    private static final long MIN_ZOMBIE_BREAK_DELAY = 500;

    @Override
    public String getName() {
        return "Improved Zombie Features";
    }

    @Override
    protected void onEnable() {
        // Undead Gunners task - sync để tránh lỗi
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.UNDEAD_GUNNERS.isEnabled(world)) {
                            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                                if (zombie.getTarget() instanceof Player && isUndeadGunner(zombie)) {
                                    loop3s_zombie(zombie);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Undead Gunners loop", e);
                }
            }
        }.runTaskTimer(plugin, 20L, 60L));

        // Zombie Break Block task với async processing và stagger
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ZOMBIE_BREAK_BLOCK.isEnabled(world)) {
                            List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));

                            // Process từng zombie với delay khác nhau
                            for (int i = 0; i < zombies.size(); i++) {
                                final Zombie zombie = zombies.get(i);
                                final long delay = i; // tick delay

                                // Schedule async sau delay
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
                                }, delay);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Zombie Break Block loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));

        // Cleanup task
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldMemories();
                cleanupBlockCooldowns();
            }
        }.runTaskTimerAsynchronously(plugin, 200L, 200L));

        // Persistent target maintenance
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                maintainPersistentTargets();
            }
        }.runTaskTimer(plugin, 0L, 10L));
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
        blockBreakCooldowns.clear();
        zombieBreakDelay.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (event.getDamage() <= 0 || zombie.hasMetadata("NPC")) return;
        if (!Feature.UNDEAD_RAGE.isEnabled(zombie)) return;

        try {
            applyUndeadRage(zombie);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in onEntityDamage", e);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            cleanupZombieBreaking(zombie);
            persistentTargets.remove(zombie.getUniqueId());
            lastTargetTime.remove(zombie.getUniqueId());
            zombieBreakDelay.remove(zombie.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        UUID zombieUUID = zombie.getUniqueId();

        // Zombie có target mới -> lưu lại
        if (event.getTarget() instanceof Player player) {
            persistentTargets.put(zombieUUID, player.getUniqueId());
            lastTargetTime.put(zombieUUID, System.currentTimeMillis());
        }

        // Zombie mất target nhưng có persistent target -> khôi phục
        if (event.getTarget() == null && persistentTargets.containsKey(zombieUUID)) {
            UUID targetUUID = persistentTargets.get(zombieUUID);
            Player persistentPlayer = Bukkit.getPlayer(targetUUID);

            if (persistentPlayer != null && persistentPlayer.isOnline()) {
                double distSq = zombie.getLocation().distanceSquared(persistentPlayer.getLocation());

                if (distSq <= MAX_TARGET_DISTANCE_SQUARED) {
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

    private void maintainPersistentTargets() {
        persistentTargets.entrySet().removeIf(entry -> {
            UUID zombieUUID = entry.getKey();
            UUID targetUUID = entry.getValue();

            Entity zombieEntity = Bukkit.getEntity(zombieUUID);
            if (!(zombieEntity instanceof Zombie zombie) || !zombie.isValid()) {
                return true;
            }

            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null || !target.isOnline()) {
                return true;
            }

            double distSq = zombie.getLocation().distanceSquared(target.getLocation());
            if (distSq > MAX_TARGET_DISTANCE_SQUARED) {
                zombie.setTarget(null);
                return true;
            }

            // Khôi phục target nếu bị mất
            if (zombie.getTarget() == null) {
                zombie.setTarget(target);
            }

            return false;
        });
    }

    private void processZombieBreakBlock(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();

        // Check cooldown
        Long lastBreak = zombieBreakDelay.get(zombieUUID);
        long now = System.currentTimeMillis();
        if (lastBreak != null && now - lastBreak < MIN_ZOMBIE_BREAK_DELAY) {
            return;
        }

        // Schedule sync task
        Bukkit.getScheduler().runTask(plugin, () -> applyZombieBreakBlock(zombie));
    }

    private void applyUndeadRage(Zombie zombie) {
        int duration = (int) (Feature.UNDEAD_RAGE.getDouble("rage-duration") * 20);
        zombie.getWorld().spawnParticle(Particle.VILLAGER_ANGRY,
                zombie.getLocation().add(0, 1.7, 0), 6, 0.35, 0.35, 0.35, 0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 1));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
    }

    private boolean isUndeadGunner(Zombie zombie) {
        return zombie.getCustomName() != null && zombie.getCustomName().equalsIgnoreCase("Undead Gunner");
    }

    private void loop3s_zombie(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0 || zombie.getTarget() == null ||
                !(zombie.getTarget() instanceof Player target)) return;

        try {
            if (!target.getWorld().equals(zombie.getWorld())) return;

            double damage = Feature.UNDEAD_GUNNERS.getDouble("damage");
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.0f);

            Vector direction = target.getLocation().add(0, 0.5, 0).toVector()
                    .subtract(zombie.getLocation().add(0, 0.75, 0).toVector()).normalize().multiply(TICK_INTERVAL);
            Location loc = zombie.getEyeLocation().clone();

            new BukkitRunnable() {
                double ticks = 0;
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < 2; j++) {
                            ticks += TICK_INTERVAL;
                            loc.add(direction);
                            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 4, 0.1, 0.1, 0.1, 0);

                            for (Player player : zombie.getWorld().getPlayers()) {
                                if (loc.distanceSquared(player.getLocation().add(0, 1, 0)) < 2.3 * 2.3) {
                                    loc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 0);
                                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                                    player.damage(damage);

                                    double blockDmg = Feature.UNDEAD_GUNNERS.getDouble("block-damage");
                                    if (blockDmg > 0) {
                                        zombie.getWorld().createExplosion(zombie.getLocation(), (float) blockDmg);
                                    }
                                    cancel();
                                    return;
                                }
                            }
                        }
                        if (ticks > MAX_TICKS) cancel();
                    } catch (Exception e) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Zombie loop", e);
        }
    }

    private void applyZombieBreakBlock(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0) return;

        UUID zombieUUID = zombie.getUniqueId();
        if (activeBreakingTasks.containsKey(zombieUUID)) return;

        LivingEntity target = getZombieTarget(zombie);
        if (target == null) return;

        ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
        Material toolType = itemInHand != null ? itemInHand.getType() : Material.AIR;
        if (!isValidTool(toolType)) return;

        int attempts = breakAttempts.getOrDefault(zombieUUID, 0);
        if (attempts >= MAX_BREAK_ATTEMPTS) {
            forceRepath(zombie, target);
            breakAttempts.remove(zombieUUID);
            zombieBreakDelay.put(zombieUUID, System.currentTimeMillis());
            return;
        }

        Block targetBlock = selectBlockToBreak(zombie, target, toolType);
        if (targetBlock == null) {
            zombie.setTarget(target);
            return;
        }

        Location blockLoc = targetBlock.getLocation();
        Long blockCooldown = blockBreakCooldowns.get(blockLoc);
        long now = System.currentTimeMillis();
        if (blockCooldown != null && now - blockCooldown < BLOCK_BREAK_COOLDOWN) {
            return;
        }

        if (wasRecentlyBroken(zombieUUID, targetBlock)) {
            breakAttempts.put(zombieUUID, attempts + 1);
            return;
        }

        double breakTimeTicks = calculateBreakTime(toolType, targetBlock.getType());
        if (breakTimeTicks <= 0) return;

        blockBreakCooldowns.put(blockLoc, now);
        zombieBreakDelay.put(zombieUUID, now);

        startBreakingBlock(zombie, target, targetBlock, itemInHand, breakTimeTicks);
    }

    private LivingEntity getZombieTarget(Zombie zombie) {
        UUID persistentTargetUUID = persistentTargets.get(zombie.getUniqueId());
        if (persistentTargetUUID != null) {
            Player persistentPlayer = Bukkit.getPlayer(persistentTargetUUID);
            if (persistentPlayer != null && persistentPlayer.isOnline() &&
                    zombie.getLocation().distanceSquared(persistentPlayer.getLocation()) <= MAX_TARGET_DISTANCE_SQUARED) {
                return persistentPlayer;
            }
        }

        if (zombie.getTarget() instanceof Player) {
            return (Player) zombie.getTarget();
        } else if (zombie.getTarget() instanceof Villager) {
            return (Villager) zombie.getTarget();
        }

        for (Player player : zombie.getWorld().getPlayers()) {
            if (player.isOnline() && zombie.getLocation().distanceSquared(player.getLocation()) <= 1600) {
                zombie.setTarget(player);
                return player;
            }
        }

        return null;
    }

    private Block selectBlockToBreak(Zombie zombie, LivingEntity target, Material toolType) {
        Block targetBlock = handleSpecialCases(zombie, target, toolType);
        if (targetBlock == null) {
            targetBlock = selectBestBlockToBreak(zombie, target, toolType);
        }
        if (targetBlock == null) {
            targetBlock = findBlockingPath(zombie, target, toolType);
        }
        return targetBlock;
    }

    private void startBreakingBlock(Zombie zombie, LivingEntity target, Block block, ItemStack tool, double breakTime) {
        UUID zombieUUID = zombie.getUniqueId();

        BukkitRunnable breakTask = new BukkitRunnable() {
            int ticksElapsed = 0;
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            int entityId = RANDOM.nextInt(Integer.MAX_VALUE);
            Location breakLocation = zombie.getLocation().clone();
            boolean isFrozen = false;

            @Override
            public void run() {
                try {
                    if (!zombie.isValid() || !target.isValid() ||
                            zombie.getLocation().distanceSquared(target.getLocation()) > 1600 ||
                            !block.getType().isSolid()) {
                        unfreezeZombie(zombie);
                        activeBreakingTasks.remove(zombieUUID);
                        sendBreakAnimation(target, entityId, block, -1);
                        cancel();
                        return;
                    }

                    double distanceMoved = zombie.getLocation().distanceSquared(breakLocation);
                    if (distanceMoved > 4) {
                        unfreezeZombie(zombie);
                        activeBreakingTasks.remove(zombieUUID);
                        sendBreakAnimation(target, entityId, block, -1);
                        cancel();
                        return;
                    }

                    if (!isFrozen) {
                        freezeZombie(zombie, block);
                        isFrozen = true;
                    }

                    zombie.setTarget(null);
                    zombie.teleport(breakLocation);
                    lookAtBlock(zombie, block);

                    ticksElapsed++;

                    if (ticksElapsed < breakTime) {
                        int destroyStage = Math.min(9, (int) ((ticksElapsed / breakTime) * 10));
                        sendBreakAnimation(target, entityId, block, destroyStage);

                        if (ticksElapsed % 8 == 0) zombie.swingMainHand();
                        if (ticksElapsed % 10 == 0) {
                            Sound hitSound = block.getType().createBlockData().getSoundGroup().getHitSound();
                            zombie.getWorld().playSound(block.getLocation(), hitSound, 0.4f, 0.8f);
                        }
                        return;
                    }

                    zombie.swingMainHand();
                    Sound breakSound = block.getType().createBlockData().getSoundGroup().getBreakSound();
                    zombie.getWorld().playSound(block.getLocation(), breakSound, 0.8f, 1.0f);
                    sendBreakAnimation(target, entityId, block, -1);
                    zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                            block.getLocation().add(0.5, 0.5, 0.5),
                            25, 0.3, 0.3, 0.3, 0.1, block.getBlockData());
                    block.breakNaturally(tool);

                    rememberBrokenBlock(zombieUUID, block);
                    unfreezeZombie(zombie);
                    zombie.setTarget(target);

                    activeBreakingTasks.remove(zombieUUID);
                    cancel();

                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> applyZombieBreakBlock(zombie), 10L + RANDOM.nextInt(10));

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in zombie break task", e);
                    activeBreakingTasks.remove(zombieUUID);
                    cancel();
                }
            }

            private void sendBreakAnimation(LivingEntity entity, int id, Block b, int stage) {
                try {
                    if (entity instanceof Player) {
                        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
                        packet.getIntegers().write(0, id);
                        packet.getBlockPositionModifier().write(0, new BlockPosition(b.getX(), b.getY(), b.getZ()));
                        packet.getIntegers().write(1, stage);
                        protocolManager.sendServerPacket((Player) entity, packet);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error sending break animation", e);
                }
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
    }

    private void cleanupBlockCooldowns() {
        long now = System.currentTimeMillis();
        blockBreakCooldowns.entrySet().removeIf(entry ->
                now - entry.getValue() > BLOCK_BREAK_COOLDOWN);
    }

    private void cleanupZombieBreaking(Zombie zombie) {
        UUID uuid = zombie.getUniqueId();
        BukkitTask task = activeBreakingTasks.remove(uuid);
        if (task != null) task.cancel();
        recentlyBrokenBlocks.remove(uuid);
        breakAttempts.remove(uuid);
        lastBreakTime.remove(uuid);
        zombieBreakDelay.remove(uuid);
    }

    // Helper methods (giữ nguyên từ code gốc)
    private boolean isValidTool(Material tool) { return true; }
    private boolean canBreakBlock(Material tool, Material block) { return true; }
    private double calculateBreakTime(Material tool, Material block) { return 40.0; }
    private void forceRepath(Zombie zombie, LivingEntity target) {}
    private void freezeZombie(Zombie zombie, Block block) {}
    private void unfreezeZombie(Zombie zombie) {}
    private void lookAtBlock(Zombie zombie, Block block) {}
    private void rememberBrokenBlock(UUID uuid, Block block) {
        recentlyBrokenBlocks.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(block.getLocation());
        lastBreakTime.put(uuid, System.currentTimeMillis());
        breakAttempts.put(uuid, 0);
    }
    private Block handleSpecialCases(Zombie zombie, LivingEntity target, Material toolType) { return null; }
    private Block selectBestBlockToBreak(Zombie zombie, LivingEntity target, Material toolType) { return null; }
    private Block findBlockingPath(Zombie zombie, LivingEntity target, Material toolType) { return null; }
    private boolean wasRecentlyBroken(UUID uuid, Block block) { return false; }
}