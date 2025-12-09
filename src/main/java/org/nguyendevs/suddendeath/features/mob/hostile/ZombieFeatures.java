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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.listener.Loops;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import java.util.*;
        import java.util.logging.Level;

public class ZombieFeatures extends AbstractFeature {

    private final Map<UUID, BukkitRunnable> activeBreakingTasks = new HashMap<>();

    @Override
    public String getName() {
        return "Zombie Features (Undead Gunners + Rage + Break Block)";
    }

    @Override
    protected void onEnable() {
        // Undead Gunners loop
        BukkitRunnable gunnerLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.UNDEAD_GUNNERS.isEnabled(world)) {
                            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                                if (zombie.getTarget() instanceof Player && isUndeadGunner(zombie)) {
                                    Loops.loop3s_zombie(zombie);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Undead Gunners loop", e);
                }
            }
        };
        gunnerLoop.runTaskTimer(plugin, 20L, 60L);
        registerTask((BukkitTask) gunnerLoop);

        // Zombie Break Block loop
        BukkitRunnable breakLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ZOMBIE_BREAK_BLOCK.isEnabled(world)) {
                            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                                if ((zombie.getTarget() instanceof Player || zombie.getTarget() instanceof Villager) &&
                                        plugin.getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), CustomFlag.SDS_BREAK)) {
                                    applyZombieBreakBlock(zombie);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Zombie Break Block loop", e);
                }
            }
        };
        breakLoop.runTaskTimer(plugin, 0, 20);
        registerTask((BukkitTask) breakLoop);
    }

    @Override
    protected void onDisable() {
        activeBreakingTasks.values().forEach(BukkitRunnable::cancel);
        activeBreakingTasks.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (event.getDamage() <= 0 || zombie.hasMetadata("NPC")) return;
        if (!Feature.UNDEAD_RAGE.isEnabled(zombie)) return;

        try {
            applyUndeadRage(zombie);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in ZombieFeatures.onEntityDamage", e);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            cleanupZombieBreaking(zombie);
        }
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

    private void applyZombieBreakBlock(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0) return;

        UUID zombieUUID = zombie.getUniqueId();
        if (activeBreakingTasks.containsKey(zombieUUID)) return;

        Player target = null;
        if (zombie.getTarget() instanceof Player) {
            target = (Player) zombie.getTarget();
        } else {
            for (Player player : zombie.getWorld().getPlayers()) {
                if (player.isOnline() && zombie.getLocation().distanceSquared(player.getLocation()) <= 1600) {
                    target = player;
                    zombie.setTarget(target);
                    break;
                }
            }
        }
        if (target == null || !target.getWorld().equals(zombie.getWorld()) ||
                zombie.getLocation().distanceSquared(target.getLocation()) > 1600) return;

        ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
        Material toolType = itemInHand != null ? itemInHand.getType() : Material.AIR;
        if (!isValidTool(toolType)) return;

        Location zombieEyeLoc = zombie.getEyeLocation();
        Vector direction = target.getLocation().toVector().subtract(zombieEyeLoc.toVector()).normalize();
        Block targetBlock = null;

        BlockIterator iterator = new BlockIterator(zombie.getWorld(), zombieEyeLoc.toVector(), direction, 0, 3);
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid() && canBreakBlock(toolType, block.getType())) {
                targetBlock = block;
                break;
            }
            if (!block.getType().isSolid()) {
                Block blockBelow = block.getRelative(0, -1, 0);
                if (blockBelow.getType().isSolid() && canBreakBlock(toolType, blockBelow.getType())) {
                    targetBlock = blockBelow;
                    break;
                }
            }
        }
        if (targetBlock == null) {
            zombie.setTarget(target);
            return;
        }

        double breakTimeTicks = calculateBreakTime(toolType, targetBlock.getType());
        if (breakTimeTicks <= 0) return;

        startBreakingBlock(zombie, target, targetBlock, itemInHand, breakTimeTicks);
    }

    private void startBreakingBlock(Zombie zombie, Player target, Block block, ItemStack tool, double breakTime) {
        UUID zombieUUID = zombie.getUniqueId();

        BukkitRunnable breakTask = new BukkitRunnable() {
            int ticksElapsed = 0;
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            int entityId = (int) (Math.random() * Integer.MAX_VALUE);

            @Override
            public void run() {
                try {
                    if (!zombie.isValid() || !target.isOnline() ||
                            zombie.getLocation().distanceSquared(target.getLocation()) > 1600 ||
                            !block.getType().isSolid()) {
                        activeBreakingTasks.remove(zombieUUID);
                        sendBreakAnimation(target, entityId, block, -1);
                        cancel();
                        return;
                    }

                    zombie.setTarget(target);
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

                    // Break the block
                    zombie.swingMainHand();
                    Sound breakSound = block.getType().createBlockData().getSoundGroup().getBreakSound();
                    zombie.getWorld().playSound(block.getLocation(), breakSound, 0.8f, 1.0f);
                    sendBreakAnimation(target, entityId, block, -1);
                    zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                            block.getLocation().add(0.5, 0.5, 0.5),
                            25, 0.3, 0.3, 0.3, 0.1, block.getBlockData());
                    block.breakNaturally(tool);

                    activeBreakingTasks.remove(zombieUUID);
                    cancel();

                    // Schedule next break
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> applyZombieBreakBlock(zombie), 10 + RANDOM.nextInt(10));

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in zombie break task", e);
                    activeBreakingTasks.remove(zombieUUID);
                    cancel();
                }
            }

            private void sendBreakAnimation(Player p, int id, Block b, int stage) {
                try {
                    PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
                    packet.getIntegers().write(0, id);
                    packet.getBlockPositionModifier().write(0, new BlockPosition(b.getX(), b.getY(), b.getZ()));
                    packet.getIntegers().write(1, stage);
                    protocolManager.sendServerPacket(p, packet);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error sending break animation", e);
                }
            }
        };

        activeBreakingTasks.put(zombieUUID, breakTask);
        breakTask.runTaskTimerAsynchronously(plugin, 0, 1);
    }

    public void cleanupZombieBreaking(Zombie zombie) {
        BukkitRunnable task = activeBreakingTasks.remove(zombie.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private boolean isValidTool(Material material) {
        return isPickaxe(material) || isShovel(material) || isAxe(material);
    }

    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE || material == Material.STONE_PICKAXE ||
                material == Material.IRON_PICKAXE || material == Material.GOLDEN_PICKAXE ||
                material == Material.DIAMOND_PICKAXE || material == Material.NETHERITE_PICKAXE;
    }

    private boolean isShovel(Material material) {
        return material == Material.WOODEN_SHOVEL || material == Material.STONE_SHOVEL ||
                material == Material.IRON_SHOVEL || material == Material.GOLDEN_SHOVEL ||
                material == Material.DIAMOND_SHOVEL || material == Material.NETHERITE_SHOVEL;
    }
    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE || material == Material.STONE_AXE ||
                material == Material.IRON_AXE || material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE || material == Material.NETHERITE_AXE;
    }

    private boolean canBreakBlock(Material tool, Material block) {
        if (block.getHardness() < 0) return false;

        String modifierKey;
        if (isPickaxe(tool)) {
            modifierKey = "breakable-pickaxe-blocks";
        } else if (isShovel(tool)) {
            modifierKey = "breakable-shovel-blocks";
        } else if (isAxe(tool)) {
            modifierKey = "breakable-axe-blocks";
        } else {
            return false;
        }

        String blockListString = Feature.ZOMBIE_BREAK_BLOCK.getString(modifierKey);
        if (blockListString == null || blockListString.isEmpty()) return false;

        Set<Material> breakableBlocks = new HashSet<>();
        String[] blockNames = blockListString.split(",");
        for (String blockName : blockNames) {
            Material material = Material.getMaterial(blockName.trim().toUpperCase());
            if (material != null) {
                breakableBlocks.add(material);
            }
        }

        if (isPickaxe(tool) && block == Material.OBSIDIAN &&
                tool != Material.DIAMOND_PICKAXE && tool != Material.NETHERITE_PICKAXE) {
            return false;
        }

        return breakableBlocks.contains(block);
    }

    private double calculateBreakTime(Material tool, Material block) {
        float blockHardness = block.getHardness();
        if (blockHardness < 0) return -1;

        float toolMultiplier = switch (tool) {
            case WOODEN_PICKAXE, WOODEN_SHOVEL, WOODEN_AXE -> 2.0f;
            case GOLDEN_PICKAXE, GOLDEN_SHOVEL, GOLDEN_AXE -> 12.0f;
            case STONE_PICKAXE, STONE_SHOVEL, STONE_AXE -> 4.0f;
            case IRON_PICKAXE, IRON_SHOVEL, IRON_AXE -> 6.0f;
            case DIAMOND_PICKAXE, DIAMOND_SHOVEL, DIAMOND_AXE -> 8.0f;
            case NETHERITE_PICKAXE, NETHERITE_SHOVEL, NETHERITE_AXE -> 9.0f;
            default -> 0.5f;
        };
        return blockHardness * 1.5f / toolMultiplier * 20.0;
    }
}