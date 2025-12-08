package org.nguyendevs.suddendeath.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.NoInteractItemEntity;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.*;
import java.util.logging.Level;

public class UndeadFeaturesListener implements Listener {
    private static final Random RANDOM = new Random();
    private final Map<UUID, BukkitRunnable> activeBreakingTasks = new HashMap<>();

    public UndeadFeaturesListener() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.WITHER_RUSH.isEnabled(world)) {
                            for (WitherSkeleton ws : world.getEntitiesByClass(WitherSkeleton.class)) {
                                if (ws.getTarget() instanceof Player) Loops.loop6s_wither_skeleton(ws);
                            }
                        }
                        if (Feature.UNDEAD_GUNNERS.isEnabled(world)) {
                            for (Zombie z : world.getEntitiesByClass(Zombie.class)) {
                                if (z.getTarget() instanceof Player && isUndeadGunner(z)) Loops.loop3s_zombie(z);
                            }
                        }
                        if (Feature.BONE_WIZARDS.isEnabled(world)) {
                            for (Skeleton s : world.getEntitiesByClass(Skeleton.class)) {
                                if (s.getTarget() instanceof Player && isBoneWizard(s)) Loops.loop3s_skeleton(s);
                            }
                        }
                        if (Feature.TRIDENT_WRATH.isEnabled(world)) {
                            for (Drowned d : world.getEntitiesByClass(Drowned.class)) {
                                if (d.getTarget() instanceof Player) applyTridentWrath(d);
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Undead loops", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 20L, 100L);

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ZOMBIE_BREAK_BLOCK.isEnabled(world)) {
                            for (Zombie z : world.getEntitiesByClass(Zombie.class)) {
                                if ((z.getTarget() instanceof Player || z.getTarget() instanceof Villager) && SuddenDeath.getInstance().getWorldGuard().isFlagAllowedAtLocation(z.getLocation(), CustomFlag.SDS_BREAK)) {
                                    applyZombieBreakBlock(z);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Zombie Break Block loop", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;
        if (event.getEntity() instanceof Zombie zombie && Feature.UNDEAD_GUNNERS.isEnabled(zombie)) {
            applyUndeadRage(zombie);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC") || event.getDamager().hasMetadata("NPC")) return;
        if (event.getEntity() instanceof Player player) {
            if (event.getDamager() instanceof Arrow arrow) {
                if (Feature.STRAY_FROST.isEnabled(player) && arrow.getShooter() instanceof Stray) applyStrayFrost(player);
                if (Feature.SHOCKING_SKELETON_ARROWS.isEnabled(player) && arrow.getShooter() instanceof Skeleton) applyShockingSkeletonArrows(player);
            }
            if (isZombieEntity(event.getDamager()) && Feature.INFECTION.isEnabled(player) && SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT)) {
                applyInfection(player);
            }
        }
        if (isZombieEntity(event.getEntity()) && event.getDamager() instanceof Player player && Feature.INFECTION.isEnabled(event.getEntity()) && player.getInventory().getItemInMainHand().getType() == Material.AIR && !Utils.hasCreativeGameMode(player)) {
            applyInfection(player);
        }
    }

    @EventHandler
    public void onSkeletonShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Skeleton skeleton) || !(event.getProjectile() instanceof Arrow) || !Feature.BONE_GRENADES.isEnabled(skeleton) || !(skeleton.getTarget() instanceof Player target)) return;
        if (RANDOM.nextDouble() >= Feature.BONE_GRENADES.getDouble("chance-percent") / 100.0) return;
        event.setCancelled(true);
        double damage = Feature.BONE_GRENADES.getDouble("damage");
        skeleton.getWorld().playSound(skeleton.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
        NoInteractItemEntity grenade = new NoInteractItemEntity(skeleton.getEyeLocation(), new ItemStack(Material.SKELETON_SKULL));
        grenade.getEntity().setVelocity(target.getLocation().subtract(skeleton.getLocation()).toVector().multiply(0.05).setY(0.6));
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {
                ticks++;
                Item grenadeEntity = grenade.getEntity();
                if (ticks > 40 || grenadeEntity.isDead()) { grenade.close(); cancel(); return; }
                grenadeEntity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, grenadeEntity.getLocation(), 0);
                if (grenadeEntity.isOnGround()) {
                    grenade.close();
                    grenadeEntity.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, grenadeEntity.getLocation(), 24, 3, 3, 3, 0);
                    grenadeEntity.getWorld().playSound(grenadeEntity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    for (Entity nearby : grenadeEntity.getNearbyEntities(6, 6, 6)) {
                        if (nearby instanceof Player player) Utils.damage(player, damage, true);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            cleanupZombieBreaking(zombie);
            if (Feature.SILVERFISHES_SUMMON.isEnabled(zombie) && RANDOM.nextDouble() <= Feature.SILVERFISHES_SUMMON.getDouble("chance-percent") / 100.0) {
                int min = (int) Feature.SILVERFISHES_SUMMON.getDouble("min");
                int max = (int) Feature.SILVERFISHES_SUMMON.getDouble("max");
                int count = min + RANDOM.nextInt(max);
                for (int j = 0; j < count; j++) {
                    Vector velocity = new Vector(RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5);
                    zombie.getWorld().spawnParticle(Particle.SMOKE_LARGE, zombie.getLocation(), 0);
                    zombie.getWorld().spawnEntity(zombie.getLocation(), EntityType.SILVERFISH).setVelocity(velocity);
                }
            }
        }
    }

    private void applyUndeadRage(Zombie zombie) {
        int duration = (int) (Feature.UNDEAD_RAGE.getDouble("rage-duration") * 20);
        zombie.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, zombie.getLocation().add(0, 1.7, 0), 6, 0.35, 0.35, 0.35, 0);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 1));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
    }

    private void applyZombieBreakBlock(Zombie zombie) {
        if (zombie == null || zombie.getHealth() <= 0) return;
        UUID zombieUUID = zombie.getUniqueId();
        if (activeBreakingTasks.containsKey(zombieUUID)) return;
        LivingEntity target = zombie.getTarget();
        if (target == null) {
            for (Player p : zombie.getWorld().getPlayers()) {
                if (p.isOnline() && zombie.getLocation().distanceSquared(p.getLocation()) <= 1600) { target = p; zombie.setTarget(target); break; }
            }
        }
        if (target == null || !target.getWorld().equals(zombie.getWorld()) || zombie.getLocation().distanceSquared(target.getLocation()) > 1600) return;
        ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
        Material toolType = itemInHand != null ? itemInHand.getType() : Material.AIR;
        if (!isValidTool(toolType)) return;
        Block targetBlock = null;
        BlockIterator iterator = new BlockIterator(zombie.getWorld(), zombie.getEyeLocation().toVector(), target.getLocation().toVector().subtract(zombie.getEyeLocation().toVector()).normalize(), 0, 3);
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (block.getType().isSolid() && canBreakBlock(toolType, block.getType())) { targetBlock = block; break; }
            Block blockBelow = block.getRelative(0, -1, 0);
            if (!block.getType().isSolid() && blockBelow.getType().isSolid() && canBreakBlock(toolType, blockBelow.getType())) { targetBlock = blockBelow; break; }
        }
        if (targetBlock == null) { zombie.setTarget(target); return; }
        double breakTimeTicks = calculateBreakTime(toolType, targetBlock.getType());
        if (breakTimeTicks <= 0) return;
        Block finalTargetBlock = targetBlock;
        Player finalTarget = target instanceof Player ? (Player) target : null;
        BukkitRunnable breakTask = new BukkitRunnable() {
            int ticksElapsed = 0;
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            int entityId = (int) (Math.random() * Integer.MAX_VALUE);
            @Override
            public void run() {
                if (!zombie.isValid() || (finalTarget != null && !finalTarget.isOnline()) || !finalTargetBlock.getType().isSolid() || !canBreakBlock(toolType, finalTargetBlock.getType())) {
                    activeBreakingTasks.remove(zombieUUID);
                    sendBreakAnimationPacket(protocolManager, entityId, finalTargetBlock, -1);
                    cancel();
                    return;
                }
                ticksElapsed++;
                if (ticksElapsed < breakTimeTicks) {
                    int destroyStage = Math.min(9, (int) ((ticksElapsed / breakTimeTicks) * 10));
                    sendBreakAnimationPacket(protocolManager, entityId, finalTargetBlock, destroyStage);
                    if (ticksElapsed % 10 == 0) zombie.swingMainHand();
                    if (ticksElapsed % 15 == 0) zombie.getWorld().playSound(finalTargetBlock.getLocation(), finalTargetBlock.getBlockData().getSoundGroup().getHitSound(), 0.4f, 1.0f);
                    return;
                }
                if (finalTargetBlock.getType().isSolid()) {
                    zombie.swingMainHand();
                    zombie.getWorld().playSound(finalTargetBlock.getLocation(), finalTargetBlock.getBlockData().getSoundGroup().getBreakSound(), 0.8f, 1.0f);
                    sendBreakAnimationPacket(protocolManager, entityId, finalTargetBlock, -1);
                    zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK, finalTargetBlock.getLocation().add(0.5, 0.5, 0.5), 25, 0.3, 0.3, 0.3, 0.1, finalTargetBlock.getBlockData());
                    finalTargetBlock.breakNaturally(itemInHand);
                }
                activeBreakingTasks.remove(zombieUUID);
                new BukkitRunnable() { @Override public void run() { applyZombieBreakBlock(zombie); } }.runTaskLater(SuddenDeath.getInstance(), 15L);
            }
        };
        activeBreakingTasks.put(zombieUUID, breakTask);
        breakTask.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }

    private void cleanupZombieBreaking(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();
        BukkitRunnable task = activeBreakingTasks.remove(zombieUUID);
        if (task != null && !task.isCancelled()) task.cancel();
    }

    private void applyTridentWrath(Drowned drowned) {
        if (drowned == null || drowned.getHealth() <= 0 || !(drowned.getTarget() instanceof Player target) || !target.getWorld().equals(drowned.getWorld()) || drowned.getEquipment().getItemInMainHand().getType() != Material.TRIDENT) return;
        if (RANDOM.nextDouble() > Feature.TRIDENT_WRATH.getDouble("chance-percent") / 100.0) return;
        int duration = (int) (Feature.TRIDENT_WRATH.getDouble("duration") * 20);
        double speed = Feature.TRIDENT_WRATH.getDouble("speed");
        drowned.getWorld().playSound(drowned.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        new BukkitRunnable() {
            int ticks = 0;
            final Location startLoc = drowned.getLocation().clone();
            final Vector direction = target.getLocation().add(0, 1, 0).subtract(startLoc).toVector().normalize();
            @Override
            public void run() {
                if (ticks >= duration || !drowned.isValid() || !target.isOnline()) { cancel(); return; }
                drowned.setVelocity(direction.clone().multiply(speed));
                drowned.setRotation(drowned.getLocation().getYaw(), (float) ((drowned.getLocation().getPitch() + 30.0) % 360));
                Location loc = drowned.getLocation().add(0, 1, 0);
                for (double height = -0.8; height <= 0.8; height += 0.2) {
                    double angle = (height * Math.PI * 2) + (ticks * 0.3);
                    loc.getWorld().spawnParticle(Particle.WATER_SPLASH, loc.clone().add(Math.cos(angle) * 0.6, height, Math.sin(angle) * 0.6), 1, 0, 0, 0, 0);
                }
                if (loc.distanceSquared(target.getLocation()) < 2.0) {
                    if (!Utils.hasCreativeGameMode(target)) {
                        Utils.damage(target, Feature.TRIDENT_WRATH.getDouble("damage"), true);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 1.0f);
                    }
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }

    private void applyStrayFrost(Player player) {
        if (RANDOM.nextDouble() > Feature.STRAY_FROST.getDouble("chance-percent") / 100.0) return;
        player.setFreezeTicks((int) (Feature.STRAY_FROST.getDouble("duration") * 20 + 140));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 1.0f);
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {
                if (ticks < 10) {
                    for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
                        Location pl = player.getLocation().clone().add(Math.cos(i) * 0.5, 1.0 + (Math.sin(ticks * 0.1) * 0.2), Math.sin(i) * 0.5);
                        pl.getWorld().spawnParticle(Particle.SNOWFLAKE, pl, 1, 0, 0, 0, 0);
                    }
                    ticks++;
                } else cancel();
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);
    }

    private void applyShockingSkeletonArrows(Player player) {
        if (RANDOM.nextDouble() > Feature.SHOCKING_SKELETON_ARROWS.getDouble("chance-percent") / 100.0) return;
        double duration = Feature.SHOCKING_SKELETON_ARROWS.getDouble("shock-duration");
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {
                for (int j = 0; j < 3; j++) {
                    ticks += Math.PI / 15;
                    player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().add(Math.cos(ticks), 1, Math.sin(ticks)), 0);
                }
                if (ticks >= Math.PI * 2) cancel();
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
        new BukkitRunnable() {
            int ticksPassed = 0;
            @Override
            public void run() {
                ticksPassed++;
                if (ticksPassed > duration * 20) { cancel(); return; }
                player.playHurtAnimation(0.004f);
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);
        new BukkitRunnable() {
            int playCount = 0;
            @Override
            public void run() {
                if (playCount >= duration * 20) { cancel(); return; }
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
                playCount++;
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);
    }

    private void applyInfection(Player player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) return;
        if (RANDOM.nextDouble() <= Feature.INFECTION.getDouble("chance-percent") / 100.0 && !data.isInfected()) {
            data.setInfected(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("now-infected")));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1.0f, 2.0f);
        }
    }

    private void sendBreakAnimationPacket(ProtocolManager pm, int entityId, Block block, int stage) {
        PacketContainer packet = pm.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        packet.getIntegers().write(0, entityId);
        packet.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
        packet.getIntegers().write(1, stage);
        block.getWorld().getPlayers().forEach(p -> {
            try { if (p.getLocation().distanceSquared(block.getLocation()) <= 1024) pm.sendServerPacket(p, packet); } catch (Exception ignored) {}
        });
    }

    private boolean isZombieEntity(Entity entity) {
        return entity instanceof Zombie || entity instanceof PigZombie || entity instanceof ZombieVillager || entity instanceof Husk || entity instanceof Drowned;
    }

    private boolean isUndeadGunner(Zombie zombie) { return zombie.getCustomName() != null && zombie.getCustomName().equalsIgnoreCase("Undead Gunner"); }
    private boolean isBoneWizard(Skeleton skeleton) { return skeleton.getCustomName() != null && skeleton.getCustomName().equalsIgnoreCase("Bone Wizard"); }
    private boolean isValidTool(Material m) { return isPickaxe(m) || isShovel(m) || isAxe(m); }
    private boolean isPickaxe(Material m) { return m.name().contains("_PICKAXE"); }
    private boolean isShovel(Material m) { return m.name().contains("_SHOVEL"); }
    private boolean isAxe(Material m) { return m.name().contains("_AXE"); }

    private boolean canBreakBlock(Material tool, Material block) {
        if (block.getHardness() < 0) return false;
        String key = isPickaxe(tool) ? "breakable-pickaxe-blocks" : isShovel(tool) ? "breakable-shovel-blocks" : isAxe(tool) ? "breakable-axe-blocks" : null;
        if (key == null) return false;
        String list = Feature.ZOMBIE_BREAK_BLOCK.getString(key);
        if (list == null || list.isEmpty()) return false;
        if (isPickaxe(tool) && block == Material.OBSIDIAN && tool != Material.DIAMOND_PICKAXE && tool != Material.NETHERITE_PICKAXE) return false;
        return Arrays.stream(list.split(",")).anyMatch(s -> s.trim().equalsIgnoreCase(block.name()));
    }

    private double calculateBreakTime(Material tool, Material block) {
        float hardness = block.getHardness();
        if (hardness < 0) return -1;
        float multiplier = switch (tool) {
            case WOODEN_PICKAXE, WOODEN_SHOVEL, WOODEN_AXE -> 2.0f;
            case STONE_PICKAXE, STONE_SHOVEL, STONE_AXE -> 4.0f;
            case IRON_PICKAXE, IRON_SHOVEL, IRON_AXE -> 6.0f;
            case DIAMOND_PICKAXE, DIAMOND_SHOVEL, DIAMOND_AXE -> 8.0f;
            case NETHERITE_PICKAXE, NETHERITE_SHOVEL, NETHERITE_AXE -> 9.0f;
            case GOLDEN_PICKAXE, GOLDEN_SHOVEL, GOLDEN_AXE -> 12.0f;
            default -> 0.5f;
        };
        return hardness * 1.5f / multiplier * 20.0;
    }
}