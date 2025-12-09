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
    // Loại bỏ map cooldown block để zombie đập dồn dập

    private static final double TICK_INTERVAL = 0.5;
    private static final int MAX_TICKS = 20;
    private static final int MAX_BREAK_ATTEMPTS = 50; // Tăng lên để zombie kiên trì đập
    private static final long MEMORY_DURATION = 2000;
    private static final double MAX_TARGET_DISTANCE_SQUARED = 150.0 * 150.0;

    @Override
    public String getName() {
        return "Improved Zombie Features";
    }

    @Override
    protected void onEnable() {
        // Undead Gunners
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

        // Zombie Break Block - Chạy cực nhanh (5 tick)
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ZOMBIE_BREAK_BLOCK.isEnabled(world)) {
                            List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));
                            for (Zombie zombie : zombies) {
                                // Xử lý song song không delay
                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    try {
                                        if (zombie.isValid() && (zombie.getTarget() instanceof Player || zombie.getTarget() instanceof Villager)) {
                                            if (plugin.getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), CustomFlag.SDS_BREAK)) {
                                                processZombieBreakBlock(zombie);
                                            }
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().log(Level.WARNING, "Error processing zombie break", e);
                                    }
                                });
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
                if (zombie.getLocation().distanceSquared(persistentPlayer.getLocation()) <= MAX_TARGET_DISTANCE_SQUARED) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (zombie.isValid() && zombie.getTarget() == null) zombie.setTarget(persistentPlayer);
                    });
                    event.setCancelled(true);
                }
            }
        }
    }

    private void maintainPersistentTargets() {
        persistentTargets.entrySet().removeIf(entry -> {
            Entity zombieEntity = Bukkit.getEntity(entry.getKey());
            if (!(zombieEntity instanceof Zombie zombie) || !zombie.isValid()) return true;
            Player target = Bukkit.getPlayer(entry.getValue());
            if (target == null || !target.isOnline()) return true;
            if (zombie.getLocation().distanceSquared(target.getLocation()) > MAX_TARGET_DISTANCE_SQUARED) {
                zombie.setTarget(null);
                return true;
            }
            if (zombie.getTarget() == null) zombie.setTarget(target);
            return false;
        });
    }

    private void processZombieBreakBlock(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();
        if (activeBreakingTasks.containsKey(zombieUUID)) return;
        Bukkit.getScheduler().runTask(plugin, () -> applyZombieBreakBlock(zombie));
    }

    private void applyUndeadRage(Zombie zombie) {
        int duration = (int) (Feature.UNDEAD_RAGE.getDouble("rage-duration") * 20);
        zombie.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, zombie.getLocation().add(0, 1.7, 0), 6, 0.35, 0.35, 0.35, 0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 1));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
    }

    private boolean isUndeadGunner(Zombie zombie) {
        return zombie.getCustomName() != null && zombie.getCustomName().equalsIgnoreCase("Undead Gunner");
    }

    private void loop3s_zombie(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0 || zombie.getTarget() == null || !(zombie.getTarget() instanceof Player target)) return;
        try {
            if (!target.getWorld().equals(zombie.getWorld())) return;
            double damage = Feature.UNDEAD_GUNNERS.getDouble("damage");
            zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.0f);
            Vector direction = target.getLocation().add(0, 0.5, 0).toVector().subtract(zombie.getLocation().add(0, 0.75, 0).toVector()).normalize().multiply(TICK_INTERVAL);
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
                                    if (blockDmg > 0) zombie.getWorld().createExplosion(zombie.getLocation(), (float) blockDmg);
                                    cancel();
                                    return;
                                }
                            }
                        }
                        if (ticks > MAX_TICKS) cancel();
                    } catch (Exception e) { cancel(); }
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

        Block targetBlock = selectBlockToBreak(zombie, target, toolType);
        if (targetBlock == null) return;

        // Giảm thời gian phá hủy xuống cực thấp (1/4 thời gian bình thường) để tạo cảm giác "điên cuồng"
        double breakTimeTicks = calculateBreakTime(toolType, targetBlock.getType()) * 0.25;
        if (breakTimeTicks < 5) breakTimeTicks = 5; // Tối thiểu 0.25s để có animation

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

                    zombie.setTarget(null); // Tạm dừng target để tập trung đập

                    // Look at block
                    Location lookAt = block.getLocation().add(0.5, 0.5, 0.5);
                    Vector dir = lookAt.toVector().subtract(zombie.getEyeLocation().toVector());
                    Location newLoc = zombie.getLocation().setDirection(dir);
                    zombie.teleport(newLoc);

                    ticksElapsed++;

                    if (ticksElapsed < breakTime) {
                        int destroyStage = Math.min(9, (int) ((ticksElapsed / breakTime) * 10));
                        sendBreakAnimation(target, entityId, block, destroyStage);

                        // Swing liên tục
                        if (ticksElapsed % 2 == 0) zombie.swingMainHand();

                        // Âm thanh dồn dập
                        if (ticksElapsed % 4 == 0) {
                            Sound hitSound = block.getType().createBlockData().getSoundGroup().getHitSound();
                            zombie.getWorld().playSound(block.getLocation(), hitSound, 1.0f, 1.5f + (float)(Math.random() * 0.5));
                        }
                        return;
                    }

                    // BREAK!
                    zombie.swingMainHand();
                    Sound breakSound = block.getType().createBlockData().getSoundGroup().getBreakSound();
                    zombie.getWorld().playSound(block.getLocation(), breakSound, 1.2f, 1.2f);
                    sendBreakAnimation(target, entityId, block, -1);
                    zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 50, 0.4, 0.4, 0.4, 0.1, block.getBlockData());

                    block.breakNaturally(tool);

                    zombie.setTarget(target);
                    cleanup();

                    // Schedule next break NGAY LẬP TỨC
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
    }

    private void cleanupZombieBreaking(Zombie zombie) {
        UUID uuid = zombie.getUniqueId();
        BukkitTask task = activeBreakingTasks.remove(uuid);
        if (task != null) task.cancel();
        recentlyBrokenBlocks.remove(uuid);
        breakAttempts.remove(uuid);
        lastBreakTime.remove(uuid);
        //zombieBreakDelay.remove(uuid);
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