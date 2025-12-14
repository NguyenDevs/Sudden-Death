package org.nguyendevs.suddendeath.Features.player;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.*;
import org.nguyendevs.suddendeath.Hook.CustomFlag;
import java.util.logging.Level;

public class BleedingFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Bleeding";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player == null || Utils.hasCreativeGameMode(player)) continue;
                        PlayerData data = PlayerData.get(player);
                        if (data == null) continue;

                        if (Feature.BLEEDING.isEnabled(player) && data.isBleeding() &&
                                plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) &&
                                player.getHealth() >= Feature.BLEEDING.getDouble("health-min")) {
                            Utils.damage(player, Feature.BLEEDING.getDouble("dps") * 3, Feature.BLEEDING.getBoolean("tug"));
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Bleeding loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getDamage() <= 0 || player.hasMetadata("NPC")) return;
        if (!Feature.BLEEDING.isEnabled(player)) return;
        if (isExcludedDamageCause(event.getCause())) return;
        if (!plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT)) return;

        try {
            applyBleeding(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in BleedingFeature.onEntityDamage", e);
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.hasMetadata("NPC")) return;
        if (!Feature.BLEEDING.isEnabled(player)) return;
        try {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.isBleeding()) {
                event.setCancelled(true);
            }
        } catch (Exception e) {}
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !hasMovedBlock(event)) return;
        if (!Feature.BLEEDING.isEnabled(player)) return;
        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) return;
            if (plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_REMOVE)) {
                if (data.isBleeding()) {
                    data.setBleeding(false);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("no-longer-bleeding")));
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.0f);
                }
            }
        } catch (Exception e) {}
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasItem()) return;
        if (!Feature.BLEEDING.isEnabled(player)) return;
        try {
            var item = player.getInventory().getItemInMainHand();
            if (!Utils.isPluginItem(item, false)) return;
            if (!item.isSimilar(CustomItem.BANDAGE.a())) return;
            PlayerData data = PlayerData.get(player);
            if (data == null || !data.isBleeding()) return;
            event.setCancelled(true);
            consumeItem(player);
            data.setBleeding(false);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("use-bandage")));
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.0f);
        } catch (Exception e) {}
    }

    private void applyBleeding(Player player) {
        double chance = Feature.BLEEDING.getDouble("chance-percent") / 100.0;
        PlayerData data = PlayerData.get(player);
        if (data == null) return;

        if (RANDOM.nextDouble() <= chance && !data.isBleeding()) {
            if (data.getBleedingTask() != null) data.getBleedingTask().cancel();
            data.setBleeding(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("now-bleeding")));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1.0f, 2.0f);

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (data.isBleeding()) {
                        data.setBleeding(false);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("no-longer-bleeding")));
                        data.setBleedingTask(null);
                    }
                }
            };
            data.setBleedingTask(task);
            spawnBleedingParticles(player);
            task.runTaskLater(plugin, (long)(Feature.BLEEDING.getDouble("auto-stop-bleed-time") * 20));
        }
    }

    private void spawnBleedingParticles(Player player) {
        double offsetX = (Math.random() - 0.5D) * 0.4D;
        double offsetY = 1.0 + ((Math.random() - 0.5D) * 0.5D);
        double offsetZ = (Math.random() - 0.5D) * 2.0D;
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(offsetX, offsetY, offsetZ), 30, Material.REDSTONE_BLOCK.createBlockData());
    }

    private void consumeItem(Player player) {
        var item = player.getInventory().getItemInMainHand();
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItemInMainHand(item.getAmount() < 1 ? null : item);
    }

    private boolean hasMovedBlock(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ());
    }

    private boolean isExcludedDamageCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.STARVATION || cause == EntityDamageEvent.DamageCause.DROWNING ||
                cause == EntityDamageEvent.DamageCause.SUICIDE || cause == EntityDamageEvent.DamageCause.MELTING ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.VOID ||
                cause == EntityDamageEvent.DamageCause.SUFFOCATION || cause == EntityDamageEvent.DamageCause.POISON;
    }
}