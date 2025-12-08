package org.nguyendevs.suddendeath.listener;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.*;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

public class PlayerFeaturesListener implements Listener {
    private static final Random RANDOM = new Random();
    private static final Set<Material> STIFFNESS_MATERIALS = Set.of(
            Material.STONE, Material.COAL_ORE, Material.IRON_ORE, Material.NETHER_QUARTZ_ORE,
            Material.GOLD_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE,
            Material.EMERALD_ORE, Material.COBBLESTONE, Material.STONE_SLAB, Material.COBBLESTONE_SLAB,
            Material.BRICK_STAIRS, Material.BRICK, Material.MOSSY_COBBLESTONE);

    public PlayerFeaturesListener() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Map<Player, Integer> players = SuddenDeath.getInstance().getPlayers();
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
                            SuddenDeath.getInstance().getPacketSender().fading(player, currentDistance);
                            double coefficient = Feature.BLOOD_SCREEN.getDouble("coefficient");
                            int newDistance = (int) (currentDistance * coefficient);
                            entry.setValue(newDistance);
                            if (distanceToBorder >= currentDistance) {
                                players.remove(player);
                                SuddenDeath.getInstance().getPacketSender().fading(player, border.getWarningDistance());
                            }
                        } catch (Exception e) {
                            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error processing blood effect", e);
                            players.remove(player);
                        }
                    }
                    Bukkit.getOnlinePlayers().forEach(Loops::loop3s_player);
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.SEVERE, "Error running Player loop task", e);
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0L, 60L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;
        if (event.getEntity() instanceof Player player) {
            if (Feature.BLOOD_SCREEN.isEnabled(player)) {
                try {
                    double distance = player.getWorld().getWorldBorder().getSize() / 2.0 - player.getLocation().distance(player.getWorld().getWorldBorder().getCenter());
                    int fakeDistance = (int) (distance * Feature.BLOOD_SCREEN.getDouble("interval"));
                    FadingType mode = FadingType.valueOf(Feature.BLOOD_SCREEN.getString("mode"));
                    if (mode == FadingType.DAMAGE) {
                        fakeDistance = (int) (fakeDistance * event.getDamage());
                    } else if (mode == FadingType.HEALTH) {
                        int health = (int) (player.getMaxHealth() - player.getHealth());
                        health = Math.max(health, 1);
                        fakeDistance *= health;
                    }
                    if (player.isOnGround() && !Utils.hasCreativeGameMode(player)) {
                        double offsetX = (Math.random() - 0.5D) * 0.4D;
                        double offsetY = 1.0 + ((Math.random() - 0.5D) * 0.5D);
                        double offsetZ = (Math.random() - 0.5D) * 2.0D;
                        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(offsetX, offsetY, offsetZ), 30, Material.REDSTONE_BLOCK.createBlockData());
                    }
                    SuddenDeath.getInstance().getPlayers().put(player, Math.abs(fakeDistance));
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error handling BloodScreen", e);
                }
            }
            if (Feature.FALL_STUN.isEnabled(player) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                applyFallStun(player, event.getDamage());
            }
            if (Feature.BLEEDING.isEnabled(player) && !isExcludedDamageCause(event.getCause()) && SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT)) {
                applyBleeding(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;
        if (event.getEntity() instanceof Player player) {
            if (event.getDamager() instanceof Arrow && Feature.ARROW_SLOW.isEnabled(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (Feature.ARROW_SLOW.getDouble("slow-duration") * 20), 2));
            }
            if (event.getDamager() instanceof Player damager && Feature.INFECTION.isEnabled(player)) {
                ItemStack item = damager.getInventory().getItemInMainHand();
                if (Utils.isPluginItem(item, false) && item.isSimilar(CustomItem.SHARP_KNIFE.a())) {
                    applyBleeding(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !hasMovedBlock(event)) return;
        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) return;
            if (SuddenDeath.getInstance().isWorldGuardReady() && SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_REMOVE)) {
                if (data.isBleeding()) {
                    data.setBleeding(false);
                    player.sendMessage(translateColors(Utils.msg("prefix") + " " + Utils.msg("no-longer-bleeding")));
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.0f);
                }
                if (data.isInfected()) {
                    data.setInfected(false);
                    player.sendMessage(translateColors(Utils.msg("prefix") + " " + Utils.msg("no-longer-infected")));
                    player.removePotionEffect(PotionEffectType.CONFUSION);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
                }
            }
            if (Feature.ELECTRICITY_SHOCK.isEnabled(player) && isPoweredRedstoneBlock(player.getLocation().getBlock()) && !Utils.hasCreativeGameMode(player) && !data.isOnCooldown(Feature.ELECTRICITY_SHOCK)) {
                data.applyCooldown(Feature.ELECTRICITY_SHOCK, 3);
                applyElectricityShock(player);
            }
            if (Feature.BLEEDING.isEnabled(player) && SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) && player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isBleeding()) {
                double offsetX = (Math.random() - 0.5D) * 0.4D;
                double offsetY = 1.0 + ((Math.random() - 0.5D) * 0.5D);
                double offsetZ = (Math.random() - 0.5D) * 2.0D;
                player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(offsetX, offsetY, offsetZ), 30, Material.REDSTONE_BLOCK.createBlockData());
            }
            if (Feature.INFECTION.isEnabled(player) && SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) && player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isInfected()) {
                player.getWorld().spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1, 0), 5, 0.3, 0, 0.3, 0, Color.fromRGB(34, 139, 34));
            }
            if (Feature.SNOW_SLOW.isEnabled(player) && !Utils.hasCreativeGameMode(player) && player.getLocation().getBlock().getType() == Material.SNOW && isWearingLeatherBoots(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error handling PlayerMoveEvent", e);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().hasMetadata("NPC")) return;
        Player player = event.getEntity();
        PlayerData data = PlayerData.get(player);
        if (data != null) {
            data.setBleeding(false);
            data.setInfected(false);
        }
        if (Feature.ADVANCED_PLAYER_DROPS.isEnabled(player)) {
            FileConfiguration config = Feature.ADVANCED_PLAYER_DROPS.getConfigFile().getConfig();
            if (config.getBoolean("drop-skull", false)) {
                ItemStack skull = new ItemStack(config.getBoolean("player-skull", false) ? Material.PLAYER_HEAD : Material.SKELETON_SKULL);
                if (skull.getType() == Material.PLAYER_HEAD) {
                    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    if (skullMeta != null) {
                        skullMeta.setOwningPlayer(player);
                        skull.setItemMeta(skullMeta);
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
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getEntity().hasMetadata("NPC") || (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED && event.getRegainReason() != EntityRegainHealthEvent.RegainReason.REGEN)) return;
        PlayerData data = PlayerData.get(player);
        if (data != null && ((Feature.INFECTION.isEnabled(player) && data.isInfected()) || (Feature.BLEEDING.isEnabled(player) && data.isBleeding()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (Feature.STONE_STIFFNESS.isEnabled(player) && event.hasBlock() && event.getAction() == Action.LEFT_CLICK_BLOCK && !Utils.hasCreativeGameMode(player) && !event.hasItem()) {
            Block block = event.getClickedBlock();
            if (block != null && STIFFNESS_MATERIALS.contains(block.getType())) {
                Utils.damage(player, Feature.STONE_STIFFNESS.getDouble("damage"), true);
            }
        }
        if (!event.hasItem()) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!Utils.isPluginItem(item, false)) return;
        PlayerData data = PlayerData.get(player);
        if (data == null) return;
        if (Feature.BLEEDING.isEnabled(player) && item.isSimilar(CustomItem.BANDAGE.a()) && data.isBleeding()) {
            event.setCancelled(true);
            consumeItem(player);
            data.setBleeding(false);
            player.sendMessage(translateColors(Utils.msg("prefix") + " " + Utils.msg("use-bandage")));
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.0f);
        }
        if (Feature.INFECTION.isEnabled(player) && item.isSimilar(CustomItem.STRANGE_BREW.a()) && data.isInfected()) {
            event.setCancelled(true);
            consumeItem(player);
            data.setInfected(false);
            player.sendMessage(translateColors(Utils.msg("prefix") + " " + Utils.msg("use-strange-brew")));
            player.removePotionEffect(PotionEffectType.CONFUSION);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Feature.REALISTIC_PICKUP.isEnabled(player) || Utils.hasCreativeGameMode(player)) return;
        try {
            Item item = event.getItem();
            if ((player.getEyeLocation().getPitch() > 70 || item.getLocation().getY() >= player.getLocation().getY() + 1) && player.isSneaking()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1));
            } else {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error handling EntityPickupItemEvent", e);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack item = event.getSource();
        if (Utils.isPluginItem(item, false) && item.isSimilar(CustomItem.RAW_HUMAN_FLESH.a())) {
            event.setResult(CustomItem.COOKED_HUMAN_FLESH.a());
        }
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        if (!Feature.FREDDY.isEnabled(player) || Utils.hasCreativeGameMode(player)) return;
        if (RANDOM.nextDouble() >= Feature.FREDDY.getDouble("chance-percent") / 100.0) return;
        org.bukkit.entity.Enderman freddy = (org.bukkit.entity.Enderman) player.getWorld().spawnEntity(player.getLocation(), org.bukkit.entity.EntityType.ENDERMAN);
        freddy.setCustomName("Freddy");
        freddy.setCustomNameVisible(true);
        var maxHealth = freddy.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(maxHealth.getBaseValue() * 1.75);
        var moveSpeed = freddy.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
        if (moveSpeed != null) moveSpeed.setBaseValue(moveSpeed.getBaseValue() * 1.35);
        var attackDamage = freddy.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(attackDamage.getBaseValue() * 1.35);
        freddy.setTarget(player);
        player.sendMessage(translateColors(Utils.msg("prefix") + " "+ Utils.msg("freddy-summoned")));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.0f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!Feature.DANGEROUS_COAL.isEnabled(player) || Utils.hasCreativeGameMode(player) || block.getType() != Material.COAL_ORE) return;
        if (RANDOM.nextDouble() >= Feature.DANGEROUS_COAL.getDouble("chance-percent") / 100.0) return;
        double radius = Feature.DANGEROUS_COAL.getDouble("radius");
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_TNT_PRIMED, 2.0f, 1.0f);
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {
                ticks++;
                block.getWorld().spawnParticle(Particle.SMOKE_LARGE, block.getLocation().add(0.5, 0, 0.5), 0);
                if (ticks > 39) {
                    block.getWorld().createExplosion(block.getLocation(), (float) radius);
                    cancel();
                }
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }

    private void applyFallStun(Player player, double damage) {
        player.removePotionEffect(PotionEffectType.SLOW);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (damage * 10 * Feature.FALL_STUN.getDouble("duration-amplifier")), 2));
        Location loc = player.getLocation().clone();
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {
                ticks += 0.25;
                for (double j = 0; j < Math.PI * 2; j += Math.PI / 16) {
                    Location particleLoc = loc.clone().add(Math.cos(j) * ticks, 0.1, Math.sin(j) * ticks);
                    particleLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLoc, 0, Material.DIRT.createBlockData());
                }
                loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 2.0f, 2.0f);
                if (ticks >= 2) cancel();
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 1);
    }

    private void applyBleeding(Player player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) return;
        if (RANDOM.nextDouble() <= Feature.BLEEDING.getDouble("chance-percent") / 100.0 && !data.isBleeding()) {
            if (data.getBleedingTask() != null) data.getBleedingTask().cancel();
            data.setBleeding(true);
            player.sendMessage(translateColors(Utils.msg("prefix") + " " + Utils.msg("now-bleeding")));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1.0f, 2.0f);
            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (data.isBleeding()) {
                        data.setBleeding(false);
                        player.sendMessage(translateColors(Utils.msg("prefix") + " " + Utils.msg("no-longer-bleeding")));
                        data.setBleedingTask(null);
                    }
                }
            };
            data.setBleedingTask(task);
            task.runTaskLater(SuddenDeath.getInstance(), (long) (Feature.BLEEDING.getDouble("auto-stop-bleed-time") * 20));
        }
    }

    private void applyElectricityShock(Player player) {
        player.getWorld().spawnParticle(Particle.SNOW_SHOVEL, player.getLocation(), 16, 0, 0, 0, 0.15);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 24, 0, 0, 0, 0.15);
        Utils.damage(player, Feature.ELECTRICITY_SHOCK.getDouble("damage"), true);
        new BukkitRunnable() {
            int ticksPassed = 0;
            @Override
            public void run() {
                ticksPassed++;
                if (ticksPassed > 15) { cancel(); return; }
                player.playHurtAnimation(0.005f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.2f);
            }
        }.runTaskTimer(SuddenDeath.getInstance(), 0, 2);
    }

    private boolean isExcludedDamageCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.STARVATION || cause == EntityDamageEvent.DamageCause.DROWNING ||
                cause == EntityDamageEvent.DamageCause.SUICIDE || cause == EntityDamageEvent.DamageCause.MELTING ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.VOID ||
                cause == EntityDamageEvent.DamageCause.SUFFOCATION || cause == EntityDamageEvent.DamageCause.POISON;
    }

    private boolean isPoweredRedstoneBlock(Block block) {
        if (!block.isBlockPowered()) return false;
        Material type = block.getType();
        return type == Material.REDSTONE_WIRE || type == Material.COMPARATOR || type == Material.REPEATER || type == Material.REDSTONE_TORCH;
    }

    private void consumeItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItemInMainHand(item.getAmount() < 1 ? null : item);
    }

    private boolean hasMovedBlock(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ());
    }

    private boolean isWearingLeatherBoots(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        return boots != null && boots.getType() == Material.LEATHER_BOOTS;
    }

    private String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}