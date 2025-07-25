package org.nguyendevs.suddendeath.listener;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nguyendevs.suddendeath.util.FadingType;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * Event listener for handling various entity, player, and block interactions in the SuddenDeath plugin.
 */
public class Listener1 implements Listener {
    private static final Random RANDOM = new Random();
    private static final long WITCH_LOOP_INTERVAL = 80L;
    private static final long BLAZE_PLAYER_LOOP_INTERVAL = 60L;
    private static final long SPIDER_LOOP_INTERVAL = 40L;
    private static final long BLOOD_EFFECT_INTERVAL = 0L; // Adjust interval as needed
    private static final long INITIAL_DELAY = 0L;
    private static final Set<Material> STIFFNESS_MATERIALS = Set.of(
            Material.STONE, Material.COAL_ORE, Material.IRON_ORE, Material.NETHER_QUARTZ_ORE,
            Material.GOLD_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE,
            Material.EMERALD_ORE, Material.COBBLESTONE, Material.STONE_SLAB, Material.COBBLESTONE_SLAB,
            Material.BRICK_STAIRS, Material.BRICK, Material.MOSSY_COBBLESTONE);
    private final SuddenDeath plugin;

    /**
     * Initializes periodic tasks for Witch, Blaze, Player, Spider, and Blood Effect.
     */
    public Listener1(SuddenDeath plugin) {
        this.plugin = plugin;
        // Witch loop
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

        // Blaze and Player loop
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.EVERBURNING_BLAZES.isEnabled(world)) {
                            world.getEntitiesByClass(Blaze.class).forEach(Loops::loop3s_blaze);
                        }
                    }
                    Bukkit.getOnlinePlayers().forEach(Loops::loop3s_player);
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Blaze/Player loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, BLAZE_PLAYER_LOOP_INTERVAL);

        // Spider loop
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : Bukkit.getWorlds()) {
                        if (Feature.ANGRY_SPIDERS.isEnabled(world) || Feature.LEAPING_SPIDERS.isEnabled(world)) {
                            for (Spider spider : world.getEntitiesByClass(Spider.class)) {
                                if (spider.getTarget() instanceof Player) {
                                    Loops.loop3s_spider(spider);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error in Spider loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, SPIDER_LOOP_INTERVAL);

        // Blood Effect loop
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Map<Player, Integer> players = plugin.getPlayers();

                    for (Map.Entry<Player, Integer> entry : players.entrySet()) {
                        Player player = entry.getKey();
                        if (!player.isOnline() || player.isDead()) {
                            players.remove(player);
                            continue;
                        }

                        try {
                            WorldBorder border = player.getWorld().getWorldBorder();
                            double distanceToBorder = border.getSize() / 2.0 - player.getLocation().distance(border.getCenter());
                            int currentDistance = entry.getValue();

                            // Apply fading effect
                            plugin.getPacketSender().fading(player, currentDistance);

                            // Update distance with coefficient
                            double coefficient = Feature.BLOOD_SCREEN.getDouble("coefficient");
                            int newDistance = (int) (currentDistance * coefficient);
                            entry.setValue(newDistance);

                            // Remove player if they are too close to the border
                            if (distanceToBorder >= currentDistance) {
                                players.remove(player);
                                plugin.getPacketSender().fading(player, border.getWarningDistance());
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING,
                                    "Error processing blood effect for player: " + player.getName(), e);
                            players.remove(player);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error running BloodEffect task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, BLOOD_EFFECT_INTERVAL);
    }

    /**
     * Handles general entity damage events (fall stun, bleeding, tanky monsters, undead rage, witch scrolls).
     *
     * @param event The EntityDamageEvent.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) {
            return;
        }

        try {
            // Blood Screen
            if (event.getEntity() instanceof Player player && Feature.BLOOD_SCREEN.isEnabled(player)) {
                try {
                    // Calculate fake distance based on world border
                    double distance = player.getWorld().getWorldBorder().getSize() / 2.0 - player.getLocation().distance(player.getWorld().getWorldBorder().getCenter());
                    int fakeDistance = (int) (distance * Feature.BLOOD_SCREEN.getDouble("interval"));

                    // Adjust fake distance based on fading mode
                    FadingType mode = FadingType.valueOf(Feature.BLOOD_SCREEN.getString("mode"));
                    if (mode == FadingType.DAMAGE) {
                        fakeDistance = (int) (fakeDistance * event.getDamage());
                    } else if (mode == FadingType.HEALTH) {
                        int health = (int) (player.getMaxHealth() - player.getHealth());
                        health = Math.max(health, 1); // Ensure health is at least 1
                        fakeDistance *= health;
                    }

                    // Update player's blood effect distance
                    plugin.getPlayers().put(player, Math.abs(fakeDistance));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Error handling EntityDamageEvent for player: " + player.getName(), e);
                }
            }
            // Fall Stun
            if (event.getEntity() instanceof Player player && Feature.FALL_STUN.isEnabled(player) &&
                    event.getCause() == DamageCause.FALL) {
                applyFallStun(player, event.getDamage());
            }

            // Bleeding
            if (event.getEntity() instanceof Player player && Feature.BLEEDING.isEnabled(player) &&
                    !isExcludedDamageCause(event.getCause())) {
                applyBleeding(player);
            }

            // Tanky Monsters
            if (event.getEntity() instanceof LivingEntity entity && !(event.getEntity() instanceof Player) &&
                    Feature.TANKY_MONSTERS.isEnabled(entity)) {
                double reduction = Feature.TANKY_MONSTERS.getDouble("dmg-reduction-percent." + entity.getType().name()) / 100.0;
                event.setDamage(event.getDamage() * (1 - reduction));
            }

            // Undead Rage
            if (event.getEntity() instanceof Zombie zombie && Feature.UNDEAD_GUNNERS.isEnabled(zombie)) {
                applyUndeadRage(zombie);
            }

            // Witch Scrolls
            if (event.getEntity() instanceof Witch witch && Feature.WITCH_SCROLLS.isEnabled(witch)) {
                double chance = Feature.WITCH_SCROLLS.getDouble("chance-percent") / 100.0;
                if (RANDOM.nextDouble() <= chance) {
                    event.setCancelled(true);
                    applyWitchScrollsEffect(witch);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling EntityDamageEvent for entity: " + event.getEntity().getType(), e);
        }
    }

    /**
     * Checks if the damage cause should be excluded from triggering bleeding.
     *
     * @param cause The damage cause.
     * @return True if the cause is excluded.
     */
    private boolean isExcludedDamageCause(DamageCause cause) {
        return cause == DamageCause.STARVATION || cause == DamageCause.DROWNING ||
                cause == DamageCause.SUICIDE || cause == DamageCause.MELTING ||
                cause == DamageCause.FIRE_TICK || cause == DamageCause.VOID ||
                cause == DamageCause.SUFFOCATION || cause == DamageCause.POISON;
    }

    /**
     * Applies the fall stun effect to a player.
     *
     * @param player The player to apply the effect to.
     * @param damage The damage taken from the fall.
     */
    private void applyFallStun(Player player, double damage) {
        try {
            player.removePotionEffect(PotionEffectType.SLOW);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
                    (int) (damage * 10 * Feature.FALL_STUN.getDouble("duration-amplifier")), 2));

            Location loc = player.getLocation().clone();
            new BukkitRunnable() {
                double ticks = 0;

                @Override
                public void run() {
                    try {
                        ticks += 0.25;
                        for (double j = 0; j < Math.PI * 2; j += Math.PI / 16) {
                            Location particleLoc = loc.clone().add(Math.cos(j) * ticks, 0.1, Math.sin(j) * ticks);
                            particleLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLoc, 0, Material.DIRT.createBlockData());
                        }
                        loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 2.0f, 2.0f);
                        if (ticks >= 2) {
                            cancel();
                        }
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Fall Stun particle task for player: " + player.getName(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Fall Stun for player: " + player.getName(), e);
        }
    }

    /**
     * Applies the bleeding effect to a player.
     *
     * @param player The player to apply the effect to.
     */
    private void applyBleeding(Player player) {
        try {
            double chance = Feature.BLEEDING.getDouble("chance-percent") / 100.0;
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "PlayerData not found for player: " + player.getName());
                return;
            }
            if (RANDOM.nextDouble() <= chance && !data.isBleeding()) {
                data.setBleeding(true);
                player.sendMessage(translateColors(Utils.msg("prefix") + " " +  Utils.msg("now-bleeding")));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1.0f, 2.0f);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (data.isBleeding()) {
                        data.setBleeding(false);
                        player.sendMessage(ChatColor.GREEN + Utils.msg("no-longer-bleeding"));
                    }
                }
            }.runTaskLater(SuddenDeath.getInstance(), (long) (Feature.BLEEDING.getDouble("auto-stop-bleed-time") * 20));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Bleeding for player: " + player.getName(), e);
        }
    }

    /**
     * Applies the undead rage effect to a zombie.
     *
     * @param zombie The zombie to apply the effect to.
     */
    private void applyUndeadRage(Zombie zombie) {
        try {
            int duration = (int) (Feature.UNDEAD_RAGE.getDouble("rage-duration") * 20);
            zombie.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, zombie.getLocation().add(0, 1.7, 0), 6, 0.35, 0.35, 0.35, 0);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 1));
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Undead Rage for zombie: " + zombie.getUniqueId(), e);
        }
    }

    /**
     * Applies the witch scrolls effect.
     *
     * @param witch The witch to apply the effect to.
     */
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

    /**
     * Handles entity damage by entity events (infection, arrow slow, shocking skeleton arrows, nether shield, sharp knife).
     *
     * @param event The EntityDamageByEntityEvent.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC") || event.getDamager().hasMetadata("NPC")) {
            return;
        }

        try {
            // Infection from Zombie to Player
            if (event.getEntity() instanceof Player player && isZombieEntity(event.getDamager()) &&
                    Feature.INFECTION.isEnabled(player)) {
                applyInfection(player);
            }

            // Arrow Slow
            if (event.getEntity() instanceof Player player && event.getDamager() instanceof Arrow &&
                    Feature.ARROW_SLOW.isEnabled(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
                        (int) (Feature.ARROW_SLOW.getDouble("slow-duration") * 20), 2));
            }

            // Shocking Skeleton Arrows
            if (event.getEntity() instanceof Player player && event.getDamager() instanceof Arrow arrow &&
                    Feature.SHOCKING_SKELETON_ARROWS.isEnabled(player) && arrow.getShooter() instanceof Skeleton) {
                applyShockingSkeletonArrows(player);
            }

            // Nether Shield
            if (isNetherEntity(event.getEntity()) && event.getDamager() instanceof Player player &&
                    Feature.NETHER_SHIELD.isEnabled(event.getEntity())) {
                applyNetherShield(event, (LivingEntity) event.getEntity(), player);
            }

            // Sharp Knife Bleeding
            if (event.getEntity() instanceof Player player && event.getDamager() instanceof Player damager &&
                    Feature.INFECTION.isEnabled(player)) {
                ItemStack item = damager.getInventory().getItemInMainHand();
                if (Utils.isPluginItem(item, false) && item.isSimilar(CustomItem.SHARP_KNIFE.a())) {
                    applyBleeding(player);
                }
            }

            // Infection from Zombie to Player (hand attack)
            if (isZombieEntity(event.getEntity()) && event.getDamager() instanceof Player player &&
                    Feature.INFECTION.isEnabled(event.getEntity()) && player.getInventory().getItemInMainHand().getType() == Material.AIR &&
                    !Utils.hasCreativeGameMode(player)) {
                applyInfection(player);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling EntityDamageByEntityEvent for entity: " + event.getEntity().getType(), e);
        }
    }

    /**
     * Checks if the entity is a zombie-related entity.
     *
     * @param entity The entity to check.
     * @return True if the entity is a Zombie, PigZombie, or ZombieVillager.
     */
    private boolean isZombieEntity(Entity entity) {
        return entity instanceof Zombie || entity instanceof PigZombie || entity instanceof ZombieVillager;
    }

    /**
     * Checks if the entity is a nether-related entity.
     *
     * @param entity The entity to check.
     * @return True if the entity is a PigZombie, MagmaCube, or Blaze.
     */
    private boolean isNetherEntity(Entity entity) {
        return entity instanceof PigZombie || entity instanceof MagmaCube || entity instanceof Blaze;
    }

    /**
     * Applies the infection effect to a player.
     *
     * @param player The player to apply the effect to.
     */
    private void applyInfection(Player player) {
        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "PlayerData not found for player: " + player.getName());
                return;
            }
            double chance = Feature.INFECTION.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() <= chance && !data.isInfected()) {
                data.setInfected(true);
                player.sendMessage(translateColors(Utils.msg("prefix") + " " +  Utils.msg("now-infected")));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1.0f, 2.0f);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Infection for player: " + player.getName(), e);
        }
    }

    /**
     * Applies the shocking skeleton arrows effect to a player.
     *
     * @param player The player to apply the effect to.
     */
    private void applyShockingSkeletonArrows(Player player) {
        try {
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

    /**
     * Applies the nether shield effect to a nether entity.
     *
     * @param event  The damage event.
     * @param entity The nether entity.
     * @param player The attacking player.
     */
    private void applyNetherShield(EntityDamageByEntityEvent event, LivingEntity entity, Player player) {
        try {
            double chance = Feature.NETHER_SHIELD.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() <= chance) {
                event.setCancelled(true);
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
                int radius = entity instanceof MagmaCube && ((MagmaCube) entity).getSize() == 4 ? 2 : 1;

                for (double j = 0; j < Math.PI * 2; j += 0.3) {
                    double x = Math.cos(j) * radius;
                    double z = Math.sin(j) * radius;
                    for (double y = 0; y < 2; y += 0.2) {
                        if (RANDOM.nextDouble() < 0.3) {
                            continue;
                        }
                        Location loc = entity.getLocation().clone().add(x, y, z);
                        if (loc.getBlock().getType().isSolid()) {
                            continue;
                        }
                        loc.getWorld().spawnParticle(Particle.FLAME, loc, 0);
                        if (RANDOM.nextDouble() < 0.45) {
                            loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 0);
                        }
                    }
                }
                player.setVelocity(player.getEyeLocation().getDirection().multiply(-0.6).setY(0.3));
                player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation().add(0, 1, 0), 0);
                player.setFireTicks((int) Feature.NETHER_SHIELD.getDouble("burn-duration"));
                Utils.damage(player, event.getDamage() * Feature.NETHER_SHIELD.getDouble("dmg-reflection-percent") / 100.0, true);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Nether Shield for entity: " + entity.getType(), e);
        }
    }

    /**
     * Prevents health regeneration for infected or bleeding players.
     *
     * @param event The EntityRegainHealthEvent.
     */
    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getEntity().hasMetadata("NPC") ||
                (event.getRegainReason() != RegainReason.SATIATED && event.getRegainReason() != RegainReason.REGEN)) {
            return;
        }

        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "PlayerData not found for player: " + player.getName());
                return;
            }
            if ((Feature.INFECTION.isEnabled(player) && data.isInfected()) ||
                    (Feature.BLEEDING.isEnabled(player) && data.isBleeding())) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling EntityRegainHealthEvent for player: " + player.getName(), e);
        }
    }

    /**
     * Handles player death to reset bleeding/infection and drop custom items.
     *
     * @param event The PlayerDeathEvent.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().hasMetadata("NPC")) {
            return;
        }

        try {
            Player player = event.getEntity();
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "PlayerData not found for player: " + player.getName());
                return;
            }
            data.setBleeding(false);
            data.setInfected(false);

            if (Feature.ADVANCED_PLAYER_DROPS.isEnabled(player)) {
                FileConfiguration config = Feature.ADVANCED_PLAYER_DROPS.getConfigFile().getConfig();
                if (config.getBoolean("drop-skull", false)) {
                    ItemStack skull = new ItemStack(config.getBoolean("player-skull", false) ? Material.PLAYER_HEAD : Material.SKELETON_SKULL);
                    if (skull.getType() == Material.PLAYER_HEAD) {
                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        if (skullMeta != null) {
                            skullMeta.setOwningPlayer(player);
                            skull.setItemMeta(skullMeta);
                        } else {
                            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                    "SkullMeta is null for player skull drop");
                        }
                    }
                    player.getWorld().dropItemNaturally(player.getLocation(), skull);
                }

                int boneAmount = config.getInt("dropped-bones", 0);
                if (boneAmount > 0) {
                    ItemStack bone = CustomItem.HUMAN_BONE.a().clone();
                    bone.setAmount(boneAmount);
                    player.getWorld().dropItemNaturally(player.getLocation(), bone);
                }

                int fleshAmount = config.getInt("dropped-flesh", 0);
                if (fleshAmount > 0) {
                    ItemStack flesh = CustomItem.RAW_HUMAN_FLESH.a().clone();
                    flesh.setAmount(fleshAmount);
                    player.getWorld().dropItemNaturally(player.getLocation(), flesh);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling PlayerDeathEvent for player: " + event.getEntity().getName(), e);
        }
    }

    /**
     * Handles player movement to apply electricity shock, bleeding/infection effects, and armor weight.
     *
     * @param event The PlayerMoveEvent.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !hasMovedBlock(event)) {
            return;
        }

        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "PlayerData not found for player: " + player.getName());
                return;
            }

            // Electricity Shock
            if (Feature.ELECTRICITY_SHOCK.isEnabled(player) && isPoweredRedstoneBlock(player.getLocation().getBlock()) &&
                    !Utils.hasCreativeGameMode(player) && !data.isOnCooldown(Feature.ELECTRICITY_SHOCK)) {
                data.applyCooldown(Feature.ELECTRICITY_SHOCK, 3);
                applyElectricityShock(player);
            }

            // Bleeding Effect
            if (Feature.BLEEDING.isEnabled(player) && SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT) &&
                    player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isBleeding()) {
                player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, Material.REDSTONE_WIRE.createBlockData());
            }

            // Infection Effect
            if (Feature.INFECTION.isEnabled(player) && SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SD_EFFECT) &&
                    player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isInfected()) {

                // Option 1: Use SPELL_MOB with Color data (recommended for infection effect)
                Color infectionColor = Color.fromRGB(34, 139, 34); // Dark green color for infection
                player.getWorld().spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1, 0), 5, 0.3, 0, 0.3, 0, infectionColor);

                // Option 2: Alternative - Use a different particle that doesn't require Color data
                // player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 5, 0.3, 0, 0.3, 0);

                // Option 3: Use DUST particle with custom color (another good option)
                // Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(34, 139, 34), 1.0f);
                // player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 5, 0.3, 0, 0.3, 0, dustOptions);
            }

            // Armor Weight
            if (Feature.ARMOR_WEIGHT.isEnabled(player)) {
                data.updateMovementSpeed();
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling PlayerMoveEvent for player: " + player.getName(), e);
        }
    }

    /**
     * Checks if the player has moved to a different block.
     *
     * @param event The PlayerMoveEvent.
     * @return True if the player moved to a new block.
     */
    private boolean hasMovedBlock(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null &&
                (from.getBlockX() != to.getBlockX() ||
                        from.getBlockY() != to.getBlockY() ||
                        from.getBlockZ() != to.getBlockZ());
    }

    /**
     * Applies the electricity shock effect to a player.
     *
     * @param player The player to apply the effect to.
     */
    private void applyElectricityShock(Player player) {
        try {
            player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 16, 0, 0, 0, 0.15);
            player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 24, 0, 0, 0, 0.15);
            Utils.damage(player, Feature.ELECTRICITY_SHOCK.getDouble("damage"), true);

            new BukkitRunnable() {
                int ticksPassed = 0;

                @Override
                public void run() {
                    try {
                        ticksPassed++;
                        if (ticksPassed > 15) {
                            cancel();
                            return;
                        }
                        player.playHurtAnimation(0.005f);
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error in Electricity Shock task for player: " + player.getName(), e);
                        cancel();
                    }
                }
            }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying Electricity Shock for player: " + player.getName(), e);
        }
    }

    /**
     * Handles entity death to trigger creeper revenge and silverfish summon.
     *
     * @param event The EntityDeathEvent.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("NPC")) {
            return;
        }

        try {
            Entity entity = event.getEntity();

            // Creeper Revenge
            if (Feature.CREEPER_REVENGE.isEnabled(entity) && entity instanceof Creeper creeper) {
                double chance = Feature.CREEPER_REVENGE.getDouble("chance-percent") / 100.0;
                if (RANDOM.nextDouble() <= chance) {
                    float power = creeper.isPowered() ? 6.0f : 3.0f;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                entity.getWorld().createExplosion(entity.getLocation(), power);
                            } catch (Exception e) {
                                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                        "Error in Creeper Revenge explosion for entity: " + entity.getType(), e);
                            }
                        }
                    }.runTaskLater(SuddenDeath.getInstance(), 15);
                }
            }

            // Silverfish Summon
            if (Feature.SILVERFISHES_SUMMON.isEnabled(entity) && entity instanceof Zombie) {
                double chance = Feature.SILVERFISHES_SUMMON.getDouble("chance-percent") / 100.0;
                int min = (int) Feature.SILVERFISHES_SUMMON.getDouble("min");
                int max = (int) Feature.SILVERFISHES_SUMMON.getDouble("max");
                if (RANDOM.nextDouble() <= chance) {
                    int count = min + RANDOM.nextInt(max);
                    for (int j = 0; j < count; j++) {
                        Vector velocity = new Vector(RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5);
                        entity.getWorld().spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 0);
                        entity.getWorld().spawnEntity(entity.getLocation(), EntityType.SILVERFISH).setVelocity(velocity);
                    }
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling EntityDeathEvent for entity: " + event.getEntity().getType(), e);
        }
    }

    /**
     * Handles entity spawn to apply quick mobs and force of the undead effects.
     *
     * @param event The EntitySpawnEvent.
     */
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity().hasMetadata("NPC") || !(event.getEntity() instanceof Monster monster)) {
            return;
        }

        try {
            // Quick Mobs
            if (Feature.QUICK_MOBS.isEnabled(monster)) {
                AttributeInstance movementSpeed = monster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (movementSpeed != null) {
                    double multiplier = 1 + Feature.QUICK_MOBS.getDouble("additional-ms-percent." + monster.getType().name()) / 100.0;
                    movementSpeed.setBaseValue(movementSpeed.getBaseValue() * multiplier);
                } else {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                            "Movement speed attribute not found for monster: " + monster.getType());
                }
            }

            // Force of the Undead
            if (Feature.FORCE_OF_THE_UNDEAD.isEnabled(monster)) {
                AttributeInstance attackDamage = monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (attackDamage != null) {
                    double multiplier = 1 + Feature.FORCE_OF_THE_UNDEAD.getDouble("additional-ad-percent." + monster.getType().name()) / 100.0;
                    attackDamage.setBaseValue(attackDamage.getBaseValue() * multiplier);
                } else {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                            "Attack damage attribute not found for monster: " + monster.getType());
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling EntitySpawnEvent for entity: " + event.getEntity().getType(), e);
        }
    }

    /**
     * Handles player interactions to apply stone stiffness and cure effects.
     *
     * @param event The PlayerInteractEvent.
     */
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

    /**
     * Consumes one item from the player's main hand.
     *
     * @param player The player consuming the item.
     */
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

    /**
     * Handles furnace smelting to convert raw human flesh to cooked human flesh.
     *
     * @param event The FurnaceSmeltEvent.
     */
    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        try {
            ItemStack item = event.getSource();
            if (Utils.isPluginItem(item, false) && item.isSimilar(CustomItem.RAW_HUMAN_FLESH.a())) {
                event.setResult(CustomItem.COOKED_HUMAN_FLESH.a());
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling FurnaceSmeltEvent", e);
        }
    }

    /**
     * Checks if a block is a powered redstone block.
     *
     * @param block The block to check.
     * @return True if the block is powered and is a redstone wire, comparator, repeater, or torch.
     */
    public boolean isPoweredRedstoneBlock(Block block) {
        try {
            if (!block.isBlockPowered()) {
                return false;
            }
            Material type = block.getType();
            return type == Material.REDSTONE_WIRE || type == Material.COMPARATOR ||
                    type == Material.REPEATER || type == Material.REDSTONE_TORCH;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking powered redstone block", e);
            return false;
        }
    }
    private String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}