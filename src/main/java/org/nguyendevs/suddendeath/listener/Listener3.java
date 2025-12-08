package org.nguyendevs.suddendeath.listener;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.util.*;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.player.PlayerData;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.IntStream;

public class Listener3 implements Listener {

    private final Map<UUID, BukkitRunnable> activeBreakingTasks = new HashMap<>();

    private final NamespacedKey totemUsed;
    private static final Random RANDOM = new Random();
    private static final long DROWNED_LOOP_INTERVAL = 100L;
    private static final long WITCH_LOOP_INTERVAL= 80L;
    private static final long GUARDIAN_LOOP_INTERVAL = 80L;
    private static final long BLAZE_SHOT_INTERVAL = 100L;
    private static final long FANG_INTERVAL = 100L;
    private static final long BREEZE_PLAYER_LOOP_INTERVAL = 60L;
    private static final long SPIDER_LOOP_INTERVAL = 40L;
    private static final long INITIAL_DELAY = 0L;

    private static final Set<Material> STIFFNESS_MATERIALS = Set.of(
            Material.STONE, Material.COAL_ORE, Material.IRON_ORE, Material.NETHER_QUARTZ_ORE,
            Material.GOLD_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE,
            Material.EMERALD_ORE, Material.COBBLESTONE, Material.STONE_SLAB, Material.COBBLESTONE_SLAB,
            Material.BRICK_STAIRS, Material.BRICK, Material.MOSSY_COBBLESTONE);



    public Listener3(SuddenDeath plugin) {
        this.totemUsed = new NamespacedKey(plugin, "totem_used");

        //Trident Wrath
        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.TRIDENT_WRATH.isEnabled(world)) {
                            for (Drowned drowned : world.getEntitiesByClass(Drowned.class)) {
                                if (drowned.getTarget() instanceof Player)
                                    applyTridentWrath(drowned);
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Drowned loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, DROWNED_LOOP_INTERVAL);

        //Zombie break block
        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ZOMBIE_BREAK_BLOCK.isEnabled(world)) {
                            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                                if (zombie.getTarget() instanceof Player || zombie.getTarget() instanceof Villager)
                                    if (SuddenDeath.getInstance().getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), CustomFlag.SDS_BREAK)){
                                    applyZombieBreakBlock(zombie);
                                    }
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Zombie Place Block loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 20);

        //Witch Scroll
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.WITCH_SCROLLS.isEnabled(world)) {
                            world.getEntitiesByClass(Witch.class).forEach(Loops::loop4s_witch);
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Witch loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, WITCH_LOOP_INTERVAL);

        //Homing Flame Barrgae
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.HOMING_FLAME_BARRAGE.isEnabled(world)) {
                            for(Blaze blaze : world.getEntitiesByClass(Blaze.class)) {
                                if(blaze.getTarget() instanceof Player)
                                    applyHomingFlameBarrage(blaze);
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Unreadable Fireball task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, BLAZE_SHOT_INTERVAL);

        //Abyssal Vortex
        new BukkitRunnable(){
            @Override
            public void run(){
                try{
                    for(World world: Bukkit.getWorlds()) {
                        if(Feature.ABYSSAL_VORTEX.isEnabled(world)) {
                            for(Guardian guardian : world.getEntitiesByClass(Guardian.class)){
                                if(guardian.getTarget() instanceof Player)
                                    applyAbyssalVortex(guardian);
                            }
                        }
                    }
                } catch (Exception e){
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Abyssal Vortext task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, GUARDIAN_LOOP_INTERVAL);

        //Breeze Dash
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.BREEZE_DASH.isEnabled(world)) {
                            for(Breeze breeze : world.getEntitiesByClass(Breeze.class)) {
                                if(breeze.getTarget() instanceof Player)
                                    applyBreezeDash(breeze);
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Breeze Dash task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, BREEZE_PLAYER_LOOP_INTERVAL);

        //Spider Web
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.SPIDER_WEB.isEnabled(world)) {
                            for (CaveSpider caveSpider : world.getEntitiesByClass(CaveSpider.class)) {
                                if (caveSpider.getTarget() instanceof Player) {
                                    applyCaveSpiderWeb(caveSpider);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in CaveSpider loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, SPIDER_LOOP_INTERVAL);

        //Immortal Evoker
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.IMMORTAL_EVOKER.isEnabled(world)) {
                            for (Evoker evoker : world.getEntitiesByClass(Evoker.class)) {
                                if (evoker.getTarget() instanceof Player &&
                                        evoker.getPersistentDataContainer().has(totemUsed, PersistentDataType.BYTE) &&
                                        evoker.getPersistentDataContainer().get(totemUsed, PersistentDataType.BYTE) == 1) {
                                    applyImmortalEvokerFangs(evoker);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Evoker Fangs loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, FANG_INTERVAL);
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) {
            return;
        }
        try {
            // Witch Scrolls
            if (event.getEntity() instanceof Witch witch && Feature.WITCH_SCROLLS.isEnabled(witch)) {
                double chance = Feature.WITCH_SCROLLS.getDouble("chance-percent") / 100.0;
                if (RANDOM.nextDouble() <= chance) {
                    event.setCancelled(true);
                    applyWitchScrollsEffect(witch);
                }
            }

            // Immortal Evoker
            if (event.getEntity() instanceof Evoker evoker && Feature.IMMORTAL_EVOKER.isEnabled(evoker)) {
                if (evoker.getHealth() - event.getFinalDamage() <= 0) {
                    double chance = Feature.IMMORTAL_EVOKER.getDouble("chance-percent") / 100.0;
                    if (RANDOM.nextDouble() <= chance) {
                        event.setCancelled(true);
                        evoker.setHealth(evoker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
                        evoker.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, (int) Feature.IMMORTAL_EVOKER.getDouble("resistance-amplifier") - 1));
                        evoker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,60,2));
                        evoker.getPersistentDataContainer().set(totemUsed, PersistentDataType.BYTE, (byte) 1);

                        evoker.getWorld().playSound(evoker.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                        evoker.getWorld().spawnParticle(Particle.TOTEM, evoker.getLocation().add(0, 1, 0), 100, 0.7, 0.7, 0.7, 0.3);
                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {
                                try {
                                    if (ticks >= 40 || !evoker.isValid()) {
                                        cancel();
                                        return;
                                    }
                                    // Tạo hiệu ứng hạt Totem xoay quanh Evoker
                                    for (int i = 0; i < 8; i++) {
                                        double angle = (ticks + i * Math.PI / 4) % (Math.PI * 2);
                                        double x = Math.cos(angle) * 0.8;
                                        double z = Math.sin(angle) * 0.8;
                                        evoker.getWorld().spawnParticle(Particle.TOTEM,
                                                evoker.getLocation().add(x, 1.2 + Math.sin(ticks * 0.2) * 0.3, z),
                                                1, 0, 0, 0, 0.2);
                                    }
                                    ticks++;
                                } catch (Exception e) {
                                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                            "Error in Immortal Evoker Totem particle task for evoker: " + evoker.getUniqueId(), e);
                                    cancel();
                                }
                            }
                        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
                    }
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling EntityDamageEvent for entity: " + event.getEntity().getType(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC") || event.getDamager().hasMetadata("NPC")) {
            return;
        }

        try {
            // Stray Frost
            if (event.getEntity() instanceof Player player && event.getDamager() instanceof Arrow arrow &&
                    Feature.STRAY_FROST.isEnabled(player) && arrow.getShooter() instanceof Stray) {
                applyStrayFrost(player);
            }
            // Shocking Skeleton Arrows
            if (event.getEntity() instanceof Player player && event.getDamager() instanceof Arrow arrow &&
                    Feature.SHOCKING_SKELETON_ARROWS.isEnabled(player) && arrow.getShooter() instanceof Skeleton) {
                applyShockingSkeletonArrows(player);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling EntityDamageByEntityEvent for entity: " + event.getEntity().getType(), e);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        try {
            // Stone Stiffness
            if (Feature.STONE_STIFFNESS.isEnabled(player) && event.hasBlock() &&
                    event.getAction() == Action.LEFT_CLICK_BLOCK && !Utils.hasCreativeGameMode(player) && !event.hasItem()) {
                Block block = event.getClickedBlock();
                if (block != null && STIFFNESS_MATERIALS.contains(block.getType())) {
                    Utils.damage(player, Feature.STONE_STIFFNESS.getDouble("damage"), true);
                }
            }

            if (!event.hasItem()) {
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (!Utils.isPluginItem(item, false)) {
                return;
            }

            PlayerData data = PlayerData.get(player);
            if (data == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "PlayerData not found for player: " + player.getName());
                return;
            }

            // Bleeding Cure
            if (Feature.BLEEDING.isEnabled(player) && item.isSimilar(CustomItem.BANDAGE.a()) && data.isBleeding()) {
                event.setCancelled(true);
                consumeItem(player);
                data.setBleeding(false);
                player.sendMessage(translateColors(Utils.msg("prefix") + " " +  Utils.msg("use-bandage")));
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.0f);
            }

            // Infection Cure
            if (Feature.INFECTION.isEnabled(player) && item.isSimilar(CustomItem.STRANGE_BREW.a()) && data.isInfected()) {
                event.setCancelled(true);
                consumeItem(player);
                data.setInfected(false);
                player.sendMessage(translateColors(Utils.msg("prefix") + " " + Utils.msg("use-strange-brew")));
                player.removePotionEffect(PotionEffectType.CONFUSION);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling PlayerInteractEvent for player: " + player.getName(), e);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie zombie) {
            cleanupZombieBreaking(zombie);
        }
    }

    // ======== ZOMBIE ========
    private void applyZombieBreakBlock(Zombie zombie) {
        try {
            if (zombie == null || zombie.getHealth() <= 0) {
                return;
            }

            UUID zombieUUID = zombie.getUniqueId();
            if (activeBreakingTasks.containsKey(zombieUUID)) {
                return;
            }

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
                    zombie.getLocation().distanceSquared(target.getLocation()) > 1600) {
                return;
            }

            ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
            Material toolType = itemInHand != null ? itemInHand.getType() : Material.AIR;
            if (!isValidTool(toolType)) {
                return;
            }

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
            if (breakTimeTicks <= 0) {
                return;
            }

            Block finalTargetBlock = targetBlock;
            Player finalTarget = target;

            int randomSwingOffset = (int) (Math.random() * 10);
            int randomSoundOffset = (int) (Math.random() * 8);
            int randomParticleOffset = (int) (Math.random() * 12);

            BukkitRunnable breakTask = new BukkitRunnable() {
                int ticksElapsed = 0;
                ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
                int entityId = (int) (Math.random() * Integer.MAX_VALUE);

                int swingInterval = Math.max(8, (int) (breakTimeTicks / 8)) + (int) (Math.random() * 4 - 2);
                int hitSoundInterval = Math.max(10, (int) (breakTimeTicks / 6)) + (int) (Math.random() * 4 - 2);
                int particleInterval = Math.max(15, (int) (breakTimeTicks / 5)) + (int) (Math.random() * 6 - 3);

                @Override
                public void run() {
                    try {
                        if (!zombie.isValid() || !finalTarget.isOnline() ||
                                zombie.getLocation().distanceSquared(finalTarget.getLocation()) > 1600 ||
                                !finalTargetBlock.getType().isSolid() || !canBreakBlock(toolType, finalTargetBlock.getType())) {

                            activeBreakingTasks.remove(zombieUUID);
                            sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, -1);
                            cancel();
                            return;
                        }
                        zombie.setTarget(finalTarget);

                        ticksElapsed++;
                        if (ticksElapsed < breakTimeTicks) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        int destroyStage = Math.min(9, (int) ((ticksElapsed / breakTimeTicks) * 10));
                                        sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, destroyStage);

                                        if ((ticksElapsed + randomSwingOffset) % swingInterval == 0) {
                                            zombie.swingMainHand();
                                        }

                                        if ((ticksElapsed + randomSoundOffset) % hitSoundInterval == 0) {
                                            Sound hitSound = finalTargetBlock.getType().createBlockData().getSoundGroup().getHitSound();
                                            zombie.getWorld().playSound(finalTargetBlock.getLocation(), hitSound, 0.4f,
                                                    0.8f + (float)(Math.random() * 0.4));
                                        }

                                        if ((ticksElapsed + randomParticleOffset) % particleInterval == 0 && destroyStage >= 2) {
                                            zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                                                    finalTargetBlock.getLocation().add(0.5, 0.5, 0.5),
                                                    2, 0.1, 0.1, 0.1, 0.02, finalTargetBlock.getBlockData());
                                        }

                                    } catch (Exception e) {
                                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                                "Error in block break animation for zombie: " + zombie.getUniqueId(), e);
                                    }
                                }
                            }.runTask(SuddenDeath.getInstance());
                            return;
                        }

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    if (finalTargetBlock.getType().isSolid()) {
                                        zombie.swingMainHand();

                                        Sound breakSound = finalTargetBlock.getType().createBlockData().getSoundGroup().getBreakSound();
                                        zombie.getWorld().playSound(finalTargetBlock.getLocation(), breakSound, 0.8f, 1.0f);

                                        sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, -1);

                                        zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                                                finalTargetBlock.getLocation().add(0.5, 0.5, 0.5),
                                                25, 0.3, 0.3, 0.3, 0.1, finalTargetBlock.getBlockData());

                                        finalTargetBlock.breakNaturally(itemInHand);
                                    }

                                    activeBreakingTasks.remove(zombieUUID);

                                } catch (Exception e) {
                                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                            "Error breaking block for zombie: " + zombie.getUniqueId(), e);
                                    activeBreakingTasks.remove(zombieUUID);
                                }
                            }
                        }.runTaskLater(SuddenDeath.getInstance(), 0);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                applyZombieBreakBlock(zombie);
                            }
                        }.runTaskLater(SuddenDeath.getInstance(), 10 + (int) (Math.random() * 10)); // Random delay 10-20 ticks

                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Zombie Break Block task for zombie: " + zombie.getUniqueId(), e);
                        activeBreakingTasks.remove(zombieUUID);
                        sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, -1);
                        cancel();
                    }
                }

                private void sendBreakAnimationPacket(Player targetPlayer, int entityId, Block block, int destroyStage) {
                    try {
                        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
                        packet.getIntegers().write(0, entityId);
                        packet.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
                        packet.getIntegers().write(1, destroyStage);
                        for (Player nearbyPlayer : block.getWorld().getPlayers()) {
                            if (nearbyPlayer.isOnline() && nearbyPlayer.getLocation().distanceSquared(block.getLocation()) <= 1024) {
                                protocolManager.sendServerPacket(nearbyPlayer, packet);
                            }
                        }
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error sending block break animation packet for zombie: " + zombie.getUniqueId(), e);
                    }
                }
            };
            activeBreakingTasks.put(zombieUUID, breakTask);
            breakTask.runTaskTimerAsynchronously(SuddenDeath.getInstance(), 0, 1);

        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error when applying Zombie Break Block to zombie: " + zombie.getUniqueId(), e);
        }
    }

    public void cleanupZombieBreaking(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();
        BukkitRunnable task = activeBreakingTasks.remove(zombieUUID);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    // ======== CAVE SPIDER ========
    private void applyCaveSpiderWeb(CaveSpider spider) {
        if (spider == null || spider.getHealth() <= 0 || spider.getTarget() == null || !(spider.getTarget() instanceof Player target)) {
            return;
        }
        try {
            if (!target.getWorld().equals(spider.getWorld())) {
                return;
            }
            double chance = Feature.SPIDER_WEB.getDouble("chance-percent") / 100.0;
            int shootAmount = (int) Feature.SPIDER_WEB.getDouble("amount-per-shoot");

            if (RANDOM.nextDouble() <= chance) {
                if (shootAmount <= 2) {
                    shootSingleWeb(spider, target, shootAmount);
                } else {
                    shootContinuousWebs(spider, target, shootAmount);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying SpiderWeb for spider: " + spider.getUniqueId(), e);
        }
    } 

    private void shootSingleWeb(CaveSpider spider, Player target, int amount) {
        try {
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
            NoInteractItemEntity item = new NoInteractItemEntity(
                    spider.getLocation().add(0, 1, 0),
                    new ItemStack(Material.COBWEB)
            );
            item.getEntity().setVelocity(
                    target.getLocation().add(0, 1, 0)
                            .subtract(spider.getLocation().add(0, 1, 0))
                            .toVector().normalize().multiply(1.0)
            );
            new BukkitRunnable() {
                int ti = 0;
                @Override
                public void run() {
                    try {
                        ti++;
                        if (ti > 20 || item.getEntity().isDead()) {
                            item.close();
                            cancel();
                            return;
                        }
                        item.getEntity().getWorld().spawnParticle(Particle.CRIT, item.getEntity().getLocation(), 0);
                        for (Entity entity : item.getEntity().getNearbyEntities(1, 1, 1)) {
                            if (entity instanceof Player hitPlayer) {
                                item.close();
                                placeCobwebsAroundPlayer(hitPlayer, amount);
                                cancel();
                                return;
                            }
                        }
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in SpiderWebSingle projectile task for spider: " + spider.getUniqueId(), e);
                        item.close();
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error in shooting single web for spider: " + spider.getUniqueId(), e);
        }
    } 

    private void shootContinuousWebs(CaveSpider spider, Player target, int totalAmount) {
        try {
            new BukkitRunnable() {
                int shotsFired = 0;

                @Override
                public void run() {
                    try {
                        if (shotsFired >= totalAmount || spider.isDead() || spider.getTarget() != target || !target.isOnline()) {
                            cancel();
                            return;
                        }

                        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1, 0);
                        NoInteractItemEntity item = new NoInteractItemEntity(
                                spider.getLocation().add(0, 1, 0),
                                new ItemStack(Material.COBWEB)
                        );
                        item.getEntity().setVelocity(
                                target.getLocation().add(0, 1, 0)
                                        .subtract(spider.getLocation().add(0, 1, 0))
                                        .toVector().normalize().multiply(1.0)
                        );

                        new BukkitRunnable() {
                            int ti = 0;

                            @Override
                            public void run() {
                                try {
                                    ti++;
                                    if (ti > 20 || item.getEntity().isDead()) {
                                        item.close();
                                        cancel();
                                        return;
                                    }

                                    item.getEntity().getWorld().spawnParticle(Particle.CRIT, item.getEntity().getLocation(), 0);
                                    for (Entity entity : item.getEntity().getNearbyEntities(1, 1, 1)) {
                                        if (entity instanceof Player hitPlayer) {
                                            item.close();
                                            placeCobwebsAroundPlayer(hitPlayer, 1);
                                            shotsFired++;
                                            cancel();
                                            return;
                                        }
                                    }
                                } catch (Exception e) {
                                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                            "Error in SpiderWebContinuous projectile task for spider: " + spider.getUniqueId(), e);
                                    item.close();
                                    cancel();
                                }
                            }
                        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in shooting continuous web for spider: " + spider.getUniqueId(), e);
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 3); // 3-tick delay (0.15 seconds) between shots
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error initiating continuous web shooting for spider: " + spider.getUniqueId(), e);
        }
    } 

    private void placeCobwebsAroundPlayer(Player player, int amount) {
        try {
            Location center = player.getLocation();
            int placed = 0;

            int[][] offsets = {
                    {-1, -1, -1}, {-1, -1, 0}, {-1, -1, 1},
                    {-1, 0, -1},  {-1, 0, 0},  {-1, 0, 1},
                    {-1, 1, -1},  {-1, 1, 0},  {-1, 1, 1},
                    {0, -1, -1},  {0, -1, 0},  {0, -1, 1},
                    {0, 0, -1},              {0, 0, 1},
                    {0, 1, -1},   {0, 1, 0},   {0, 1, 1},
                    {1, -1, -1},  {1, -1, 0},  {1, -1, 1},
                    {1, 0, -1},   {1, 0, 0},   {1, 0, 1},
                    {1, 1, -1},   {1, 1, 0},   {1, 1, 1}
            };

            for (int i = offsets.length - 1; i > 0; i--) {
                int index = RANDOM.nextInt(i + 1);
                int[] temp = offsets[index];
                offsets[index] = offsets[i];
                offsets[i] = temp;
            }

            for (int[] offset : offsets) {
                if (placed >= amount) {
                    break;
                }
                Location loc = center.clone().add(offset[0], offset[1], offset[2]);
                if (loc.getBlock().getType() == Material.AIR) {
                    loc.getBlock().setType(Material.COBWEB);
                    loc.getWorld().playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 2.0f);
                    placed++;
                }
            }

            player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0, Material.COBWEB.createBlockData());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 2.0f);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error placing cobwebs around player: " + player.getName(), e);
        }
    } 

    // ======== GUARDIAN ========
    private void applyAbyssalVortex(Guardian guardian) {
        try {
            if (guardian == null || guardian.getHealth() <= 0 || guardian.getTarget() == null || !(guardian.getTarget() instanceof Player target)) {
                return;
            }
            if (!target.getWorld().equals(guardian.getWorld())) {
                return;
            }

            double chance = Feature.ABYSSAL_VORTEX.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) {
                return;
            }

            int duration = (int) (Feature.ABYSSAL_VORTEX.getDouble("duration") * 20);
            double strength = Feature.ABYSSAL_VORTEX.getDouble("strength");

            final Location fixedGuardianLoc = guardian.getEyeLocation().clone();
            final Location fixedTargetLoc = target.getLocation().clone();
            final Vector fixedDirection = fixedTargetLoc.toVector().subtract(fixedGuardianLoc.toVector()).normalize();
            final double vortexLength = 20;
            final World world = guardian.getWorld();

            guardian.setAI(false);
            guardian.setInvulnerable(true);

            world.playSound(fixedGuardianLoc, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;
                final double maxRadius = 1.5;
                final double baseRadius = 0.2;

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !guardian.isValid()) {
                            guardian.setAI(true);
                            guardian.setInvulnerable(false);
                            cancel();
                            return;
                        }

                        Vector axis1, axis2;
                        if (Math.abs(fixedDirection.getY()) < 0.9) {
                            axis1 = new Vector(0, 1, 0).subtract(fixedDirection.clone().multiply(fixedDirection.getY())).normalize();
                        } else {
                            axis1 = new Vector(1, 0, 0).subtract(fixedDirection.clone().multiply(fixedDirection.getX())).normalize();
                        }
                        axis2 = fixedDirection.clone().crossProduct(axis1).normalize();

                        if (ticks % 3 == 0) {
                            for (int spiralIndex = 0; spiralIndex < 3; spiralIndex++) {
                                double phaseOffset = spiralIndex * (Math.PI * 2 / 3);

                                for (double distance = 0; distance <= vortexLength; distance += 0.7) {
                                    double progress = distance / vortexLength;
                                    double currentRadius = baseRadius + (maxRadius - baseRadius) * progress;
                                    double spiralAngle = phaseOffset + (distance * 1.2) + (ticks * 0.40);

                                    double x = Math.cos(spiralAngle) * currentRadius;
                                    double y = Math.sin(spiralAngle) * currentRadius;

                                    Vector spiralOffset = axis1.clone().multiply(x).add(axis2.clone().multiply(y));
                                    Location particleLoc = fixedGuardianLoc.clone()
                                            .add(fixedDirection.clone().multiply(distance))
                                            .add(spiralOffset);

                                    world.spawnParticle(Particle.WATER_BUBBLE, particleLoc, 1, 0, 0, 0, 0);
                                }
                            }
                        }

                        double searchRadius = Math.max(maxRadius + 2, vortexLength);
                        for (Entity entity : world.getNearbyEntities(fixedGuardianLoc, searchRadius, searchRadius, searchRadius)) {
                            if (!(entity instanceof Player player) || Utils.hasCreativeGameMode(player)) {
                                continue;
                            }

                            Location playerLoc = player.getLocation();
                            Vector toPlayer = playerLoc.toVector().subtract(fixedGuardianLoc.toVector());

                            if (toPlayer.length() < 0.1) continue;

                            double distanceAlongLaser = toPlayer.dot(fixedDirection);
                            Vector projectionOnLaser = fixedDirection.clone().multiply(distanceAlongLaser);
                            Vector offsetFromLaser = toPlayer.clone().subtract(projectionOnLaser);
                            double distanceFromLaser = offsetFromLaser.length();

                            double progress = Math.max(0, Math.min(1, distanceAlongLaser / vortexLength));
                            double radiusAtPlayer = baseRadius + (maxRadius - baseRadius) * progress;

                            if (distanceFromLaser <= radiusAtPlayer && distanceAlongLaser >= 0 && distanceAlongLaser <= vortexLength) {
                                Vector pullForce = fixedDirection.clone().multiply(-strength / 20.0);
                                Vector spiralForce = new Vector(0, 0, 0);
                                if (distanceFromLaser > 0.1) {
                                    spiralForce = offsetFromLaser.clone().normalize().multiply(-strength / 30.0);
                                }

                                Vector totalForce = pullForce.add(spiralForce);

                                if (isValidVector(totalForce)) {
                                    Vector currentVelocity = player.getVelocity();
                                    Vector newVelocity = currentVelocity.add(totalForce);

                                    if (isValidVector(newVelocity)) {
                                        player.setVelocity(newVelocity);
                                    }
                                }

                                if (ticks % 40 == 0) {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (RANDOM.nextDouble() * 40 + 60), 1));
                                    world.playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 1.2f);
                                }

                                double distance = playerLoc.distance(fixedGuardianLoc);
                                if (distance <= 2.5) {
                                    Utils.damage(player, 2.0, true);
                                    world.spawnParticle(Particle.WATER_SPLASH, playerLoc.add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
                                    world.playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 0.9f);
                                }
                            }
                        }
                        ticks++;
                    } catch (Exception e) {
                        guardian.setAI(true);
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error on task Abyssal Vortex: " + guardian.getUniqueId(), e);
                        cancel();
                    }
                }
                private boolean isValidVector(Vector vector) {
                    return vector != null &&
                            Double.isFinite(vector.getX()) &&
                            Double.isFinite(vector.getY()) &&
                            Double.isFinite(vector.getZ()) &&
                            Math.abs(vector.getX()) < 50 &&
                            Math.abs(vector.getY()) < 50 &&
                            Math.abs(vector.getZ()) < 50;
                }

            }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error on apply Abyssal Vortex to guardian: " + guardian.getUniqueId(), e);
        }
    } 

    // ======== EVOKER ========
    private void applyImmortalEvokerFangs(Evoker evoker) {
        try {
            if (evoker == null || evoker.getHealth() <= 0 || evoker.getTarget() == null || !(evoker.getTarget() instanceof Player player)) {
                return;
            }
            Location initialLoc = player.getLocation().clone();
            World world = evoker.getWorld();
            new BukkitRunnable() {
                int ticks = 0;
                final int duration = 20;

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !player.isValid() || player.isDead()) {
                            cancel();
                            return;
                        }
                        Location fangLoc = initialLoc.clone().add(RANDOM.nextDouble() * 1 - 0.5, -1 + (ticks * 0.2), RANDOM.nextDouble() * 1 - 0.5);
                        world.spawnEntity(fangLoc, EntityType.EVOKER_FANGS);
                        world.spawnParticle(Particle.SPELL_WITCH, fangLoc.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.05);
                        world.spawnParticle(Particle.SMOKE_NORMAL, fangLoc, 10, 0.3, 0.3, 0.3, 0.05);
                        world.playSound(fangLoc, Sound.ENTITY_EVOKER_FANGS_ATTACK, 0.8f, 1.2f);
                        ticks++;
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Evoker Fangs rising task for evoker: " + evoker.getUniqueId(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);

            if (!Utils.hasCreativeGameMode(player)) {
                new BukkitRunnable() {
                    int ticks = 0;
                    final int duration = 30;
                    final double maxDistance = 2.0;
                    @Override
                    public void run() {
                        try {
                            if (ticks >= duration || !player.isValid() || player.isDead()) {
                                cancel();
                                return;
                            }
                            if (player.getLocation().distanceSquared(initialLoc) > maxDistance * maxDistance) {
                                cancel();
                                return;
                            }
                            double progress = (double) ticks / duration;
                            Location pullLoc = initialLoc.clone().subtract(0, 3 * progress, 0);
                            player.teleport(pullLoc);
                            world.spawnParticle(Particle.BLOCK_CRACK, pullLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0,
                                    pullLoc.getBlock().getType().createBlockData());
                            world.spawnParticle(Particle.SPELL_MOB, pullLoc, 15, 0.5, 0.5, 0.5, 0, Color.fromRGB(75, 0, 130)); // Màu tím đậm
                            world.playSound(pullLoc, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.9f);
                            ticks++;
                        } catch (Exception e) {
                            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                    "Error in Evoker pull down task for evoker: " + evoker.getUniqueId(), e);
                            cancel();
                        }
                    }
                }.runTaskTimer(SuddenDeath.getInstance(), 20, 1);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Immortal Evoker Fangs for evoker: " + evoker.getUniqueId(), e);
        }
    } 

    // ======== BLAZE ========
    private void applyHomingFlameBarrage(Blaze blaze) {
        if (blaze == null || blaze.getHealth() <= 0 || blaze.getTarget() == null || !(blaze.getTarget() instanceof Player target)) {
            return;
        }
        try {
            if (!target.getWorld().equals(blaze.getWorld())) {
                return;
            }
            double chance = Feature.HOMING_FLAME_BARRAGE.getDouble("chance-percent") / 100.0;
            if (new Random().nextDouble() > chance) {
                return;
            }
            int amount = (int) Feature.HOMING_FLAME_BARRAGE.getDouble("shoot-amount");
            double damage = Feature.HOMING_FLAME_BARRAGE.getDouble("damage");

            blaze.getWorld().playSound(blaze.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

            Location blazeEyeLoc = blaze.getLocation().add(0, 1.2, 0);
            Vector initialDirection = target.getLocation().add(0, 1, 0).subtract(blazeEyeLoc).toVector().normalize();

            double[] angles = new double[amount];
            for (int i = 0; i < amount; i++) {
                angles[i] = (2 * Math.PI / amount) * i;
            }

            new BukkitRunnable() {
                int beamCount = 0;

                @Override
                public void run() {
                    try {
                        if (beamCount >= amount || blaze.isDead() || !target.isOnline()) {
                            cancel();
                            return;
                        }
                       // target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f);

                        double angle = angles[beamCount];
                        Vector beamDirection = initialDirection.clone();
                        double cosAngle = Math.cos(angle);
                        double sinAngle = Math.sin(angle);
                        double y = beamDirection.getY() * cosAngle - beamDirection.getZ() * sinAngle;
                        double z = beamDirection.getY() * sinAngle + beamDirection.getZ() * cosAngle;
                        beamDirection.setY(y).setZ(z);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR, 1.0f, 1.5f);

                        new BukkitRunnable() {
                            int ticks = 0;
                            Location currentLoc = blazeEyeLoc.clone();
                            final int maxTicks = 80;
                            Vector initialVelocity = beamDirection.clone().multiply(0.5);

                            @Override
                            public void run() {
                                try {
                                    if (ticks >= maxTicks || blaze.isDead() || !target.isOnline()) {
                                        cancel();
                                        return;
                                    }

                                    if (ticks < 10) {
                                        currentLoc.add(initialVelocity);
                                    } else {
                                        Location targetLoc = target.getLocation().add(0, 1, 0);
                                        Vector direction = targetLoc.subtract(currentLoc).toVector().normalize();
                                        double speed = 0.8;
                                        currentLoc.add(direction.multiply(speed));
                                    }

                                    blaze.getWorld().spawnParticle(Particle.FLAME, currentLoc, 1, 0, 0, 0, 0);

                                    Location targetLoc = target.getLocation().add(0, 1, 0);
                                    if (currentLoc.distanceSquared(targetLoc) < 1.0) {
                                        if (!Utils.hasCreativeGameMode(target)) {
                                            Utils.damage(target, damage, true);
                                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 0.5f);
                                            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                                        }
                                        cancel();
                                        return;
                                    }

                                    ticks++;
                                } catch (Exception e) {
                                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                            "Error in Homing Flame Barrage beam task for blaze: " + blaze.getUniqueId(), e);
                                    cancel();
                                }
                            }
                        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);

                        beamCount++;
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Homing Flame Barrage beam sequence for blaze: " + blaze.getUniqueId(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 5);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Homing Flame Barrage for blaze: " + blaze.getUniqueId(), e);
        }
    } 

    // ======== DROWNED ========
    private void applyTridentWrath(Drowned drowned) {
        try {
            if (drowned == null || drowned.getHealth() <= 0 || drowned.getTarget() == null || !(drowned.getTarget() instanceof Player target)) {
                return;
            }
            if (!target.getWorld().equals(drowned.getWorld()) || drowned.getEquipment().getItemInMainHand().getType() != Material.TRIDENT) {
                return;
            }

            double chance = Feature.TRIDENT_WRATH.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() > chance) {
                return;
            }

            int duration = (int) (Feature.TRIDENT_WRATH.getDouble("duration") * 20);
            double speed = Feature.TRIDENT_WRATH.getDouble("speed");
            double spinSpeed = 30.0; // Vertical spin speed (degrees per tick)

            drowned.getWorld().playSound(drowned.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);

            new BukkitRunnable() {
                int ticks = 0;
                final Location startLoc = drowned.getLocation().clone();
                final Vector direction = target.getLocation().add(0, 1, 0).subtract(startLoc).toVector().normalize();

                @Override
                public void run() {
                    try {
                        if (ticks >= duration || !drowned.isValid() || !target.isOnline()) {
                            cancel();
                            return;
                        }

                        // Move Drowned in a straight line
                        Vector velocity = direction.clone().multiply(speed);
                        drowned.setVelocity(velocity);

                        // Rotate Drowned vertically (around body axis by adjusting pitch)
                        float pitch = (float) ((drowned.getLocation().getPitch() + spinSpeed) % 360);
                        drowned.setRotation(drowned.getLocation().getYaw(), pitch);

                        // Create vertically oriented helical particle spiral around Drowned
                        Location loc = drowned.getLocation().clone().add(0, 1, 0); // Center at Drowned's mid-body
                        double radius = 0.6; // Radius of the spiral
                        double particleSpinSpeed = 0.3; // Particle rotation speed (radians per tick)
                        for (double height = -0.8; height <= 0.8; height += 0.2) { // Vertical range to cover body
                            double spiralAngle = (height * Math.PI * 2) + (ticks * particleSpinSpeed); // Helical angle
                            double x = Math.cos(spiralAngle) * radius;
                            double z = Math.sin(spiralAngle) * radius;
                            loc.getWorld().spawnParticle(Particle.WATER_SPLASH, loc.clone().add(x, height, z), 1, 0, 0, 0, 0);
                            loc.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc.clone().add(x, height, z), 1, 0, 0, 0, 0);
                        }

                        // Check collision with player
                        if (loc.distanceSquared(target.getLocation()) < 2.0) {
                            if (!Utils.hasCreativeGameMode(target)) {
                                Utils.damage(target, Feature.TRIDENT_WRATH.getDouble("damage"), true);
                                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 1.0f);
                                target.getWorld().spawnParticle(Particle.WATER_SPLASH, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0);
                            }
                            cancel();
                        }

                        ticks++;
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Trident Wrath task for drowned: " + drowned.getUniqueId(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error when applying Trident Wrath to drowned: " + drowned.getUniqueId(), e);
        }
    }

    // ======== BREEZE ========
    private void applyBreezeDash(Breeze breeze) {
        if (breeze == null || breeze.getHealth() <= 0 || breeze.getTarget() == null || !(breeze.getTarget() instanceof Player target)) {
            return;
        }
        try {
            if (!target.getWorld().equals(breeze.getWorld())) {
                return;
            }
            double chance = Feature.BREEZE_DASH.getDouble("chance-percent") / 100.0;

            if (RANDOM.nextDouble() <= chance) {
                double duration = Feature.BREEZE_DASH.getDouble("duration");
                int coefficient = (int) Feature.BREEZE_DASH.getDouble("amplifier");

                breeze.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) (duration * 20), coefficient - 1));
                breeze.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration * 20), coefficient - 1));
                breeze.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) (duration * 20), coefficient - 1));
                breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR, 1.0f, 0.5f);
                breeze.getWorld().playSound(breeze.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.0f, 0.1f);

                breeze.setVelocity(breeze.getVelocity().setY(0));
                breeze.setGravity(true);

                final double[] previousY = {breeze.getLocation().getY()};

                new BukkitRunnable() {
                    int shots = 0;

                    @Override
                    public void run() {
                        try {
                            if (shots < Feature.BREEZE_DASH.getDouble("shoot-amount")) {
                                Location breezeLoc = breeze.getLocation();
                                Vector direction = target.getLocation().subtract(breezeLoc).toVector().normalize();
                                WindCharge windCharge = (WindCharge) breeze.getWorld().spawnEntity(breezeLoc.add(0, 1, 0), EntityType.WIND_CHARGE);
                                windCharge.setVelocity(direction.multiply(1.5));
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (windCharge.isValid()) {
                                                Location chargeLoc = windCharge.getLocation();
                                                Vector velocity = windCharge.getVelocity().normalize();
                                                Location particleLoc = chargeLoc.clone().subtract(velocity.multiply(0.5));
                                                breeze.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, particleLoc, 1, 0, 0, 0, 0);
                                            } else {
                                                cancel();
                                            }
                                        } catch (Exception e) {
                                            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                                    "Error in WindCharge trail task for breeze: " + breeze.getUniqueId(), e);
                                            cancel();
                                        }
                                    }
                                }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);

                                shots++;
                            } else {
                                cancel();
                            }
                        } catch (Exception e) {
                            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                    "Error in Wind Charge shooting task for breeze: " + breeze.getUniqueId(), e);
                            cancel();
                        }
                    }
                }.runTaskTimer(SuddenDeath.getInstance(), 0, 10);
                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        try {
                            if (breeze.isValid() && breeze.getHealth() > 0) {
                                Location loc = breeze.getLocation().add(0, 0.5, 0);
                                Vector velocity = breeze.getVelocity().normalize();
                                double currentY = breeze.getLocation().getY();

                                if (currentY > previousY[0] + 0.1) {
                                    breeze.setVelocity(breeze.getVelocity().setY(-0.2));
                                }
                                previousY[0] = currentY;

                                if (velocity.lengthSquared() > 0.1 && ticks < duration * 20) {
                                    Location trailLoc = loc.subtract(velocity.multiply(0.5));
                                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(189, 235, 255), 1.0f);
                                    breeze.getWorld().spawnParticle(Particle.REDSTONE, trailLoc, 2, 0.1, 0.1, 0.1, 0, dustOptions);
                                }

                                ticks++;
                                if (ticks >= duration * 20) {
                                    breeze.setVelocity(breeze.getVelocity().setY(0.1));
                                    breeze.setGravity(true);
                                    cancel();
                                }
                            } else {
                                cancel();
                            }
                        } catch (Exception e) {
                            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                    "Error in TRON trail particle task for breeze: " + breeze.getUniqueId(), e);
                            cancel();
                        }
                    }
                }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Breeze dash effect for breeze: " + breeze.getUniqueId(), e);
        }
    } 

    // ======== STRAY ========
    private void applyStrayFrost(Player player) {
        try {
            double duration = Feature.STRAY_FROST.getDouble("duration");
            double chance = Feature.STRAY_FROST.getDouble("chance-percent") / 100.0;
            Location loc = player.getLocation();

            if (RANDOM.nextDouble() <= chance) {
                player.setFreezeTicks((int) (duration * 20 + 140));
                player.getWorld().playSound(loc, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 1.0f);

                new BukkitRunnable() {
                    double ticks = 0;

                    @Override
                    public void run() {
                        try {
                            if (ticks < 10) {
                                for (double i = 0; i < Math.PI * 2; i += Math.PI / 8) {
                                    Location particleLoc = loc.clone().add(
                                            Math.cos(i) * 0.5,
                                            1.0 + (Math.sin(ticks * 0.1) * 0.2),
                                            Math.sin(i) * 0.5);
                                    particleLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
                                }
                                ticks++;
                            } else {
                                cancel();
                            }
                        } catch (Exception e) {
                            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                    "Error in Stray Frost particle task for player: " + player.getName(), e);
                            cancel();
                        }
                    }
                }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Stray Frost for player: " + player.getName(), e);
        }
    } 

    // ======== WITCH ========
    private void applyWitchScrollsEffect(Witch witch) {
        try {
            witch.getWorld().playSound(witch.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
            Location loc = witch.getLocation();
            double radius = 1.5;

            new BukkitRunnable() {
                double step = 0;

                @Override
                public void run() {
                    try {
                        for (double j = 0; j < 3; j++) {
                            step += Math.PI / 20;
                            for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                                Location particleLoc = loc.clone().add(
                                        radius * Math.cos(i) * Math.sin(step),
                                        radius * (1 + Math.cos(step)),
                                        radius * Math.sin(i) * Math.sin(step));
                                particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 0,
                                        new Particle.DustOptions(Color.WHITE, 1));
                            }
                        }
                        if (step >= Math.PI * 2) {
                            cancel();
                        }
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Witch Scrolls particle task for witch: " + witch.getUniqueId(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Witch Scrolls for witch: " + witch.getUniqueId(), e);
        }
    } 

    // ======== SKELETON ========
    private void applyShockingSkeletonArrows(Player player) {
        try {
            // Get chance percentage from config
            double chancePercent = Feature.SHOCKING_SKELETON_ARROWS.getDouble("chance-percent");

            // Roll for chance - if fails, don't apply effect
            if (Math.random() * 100 > chancePercent) {
                return;
            }

            double duration = Feature.SHOCKING_SKELETON_ARROWS.getDouble("shock-duration");
            Location loc = player.getLocation();

            // Smoke particles
            new BukkitRunnable() {
                double ticks = 0;

                @Override
                public void run() {
                    try {
                        for (int j = 0; j < 3; j++) {
                            ticks += Math.PI / 15;
                            Location particleLoc = loc.clone().add(Math.cos(ticks), 1, Math.sin(ticks));
                            particleLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 0);
                        }
                        if (ticks >= Math.PI * 2) {
                            cancel();
                        }
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Shocking Skeleton Arrows particle task for player: " + player.getName(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);

            // Shaking effect
            new BukkitRunnable() {
                int ticksPassed = 0;

                @Override
                public void run() {
                    try {
                        ticksPassed++;
                        if (ticksPassed > duration * 20) {
                            cancel();
                            return;
                        }
                        player.playHurtAnimation(0.004f);
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Shocking Skeleton Arrows shaking task for player: " + player.getName(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);

            // Hurt sound
            new BukkitRunnable() {
                int playCount = 0;

                @Override
                public void run() {
                    try {
                        if (playCount >= duration * 20) {
                            cancel();
                            return;
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
                        playCount++;
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Shocking Skeleton Arrows sound task for player: " + player.getName(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);

        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Shocking Skeleton Arrows for player: " + player.getName(), e);
        }
    } 

    // ======== UTILS ========
    private void consumeItem(Player player) {
        try {
            ItemStack item = player.getInventory().getItemInMainHand();
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item.getAmount() < 1 ? null : item);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error consuming item for player: " + player.getName(), e);
        }
    }

    private String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private boolean isValidTool(Material material) {
        return isPickaxe(material) || isShovel(material) || isAxe(material);
    }

    private boolean isPickaxe(Material material) {
        return material == Material.WOODEN_PICKAXE ||
                material == Material.STONE_PICKAXE ||
                material == Material.IRON_PICKAXE ||
                material == Material.GOLDEN_PICKAXE ||
                material == Material.DIAMOND_PICKAXE ||
                material == Material.NETHERITE_PICKAXE;
    }

    private boolean isShovel(Material material) {
        return material == Material.WOODEN_SHOVEL ||
                material == Material.STONE_SHOVEL ||
                material == Material.IRON_SHOVEL ||
                material == Material.GOLDEN_SHOVEL ||
                material == Material.DIAMOND_SHOVEL ||
                material == Material.NETHERITE_SHOVEL;
    }

    private boolean isAxe(Material material) {
        return material == Material.WOODEN_AXE ||
                material == Material.STONE_AXE ||
                material == Material.IRON_AXE ||
                material == Material.GOLDEN_AXE ||
                material == Material.DIAMOND_AXE ||
                material == Material.NETHERITE_AXE;
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
        if (blockListString == null || blockListString.isEmpty()) {
            return false;
        }

        Set<Material> breakableBlocks = new HashSet<>();
        try {
            String[] blockNames = blockListString.split(",");
            for (String blockName : blockNames) {
                Material material = Material.getMaterial(blockName.trim().toUpperCase());
                if (material != null) {
                    breakableBlocks.add(material);
                } else {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                            "Invalid block material in " + modifierKey + ": " + blockName);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error parsing block list for " + modifierKey, e);
            return false;
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