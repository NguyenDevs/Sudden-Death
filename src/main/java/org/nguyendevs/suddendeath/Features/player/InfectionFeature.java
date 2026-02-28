package org.nguyendevs.suddendeath.Features.player;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Hook.CustomFlag;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.*;

import java.util.logging.Level;

public class InfectionFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Infection";
    }

    @Override
    protected void onEnable() {
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (Utils.hasCreativeGameMode(player)) continue;
                        PlayerData data = PlayerData.get(player);
                        if (data == null || !data.isInfected()) continue;
                        if (!Feature.INFECTION.isEnabled(player)) continue;
                        if (!plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT)) continue;
                        if (player.getHealth() < Feature.INFECTION.getDouble("health-min")) continue;

                        Utils.damage(player, Feature.INFECTION.getDouble("dps") * 3, Feature.INFECTION.getBoolean("tug"));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 600, 0));
                        if (Feature.INFECTION.getBoolean("sound")) {
                            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 666.0f, 0.1f);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Infection loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L));
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;
        try {
            if (event.getEntity() instanceof Player player &&
                    isZombieEntity(event.getDamager()) &&
                    Feature.INFECTION.isEnabled(player) &&
                    plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT)) {
                applyInfection(player);
            }

            if (isZombieEntity(event.getEntity()) &&
                    event.getDamager() instanceof Player player &&
                    !Utils.hasCreativeGameMode(player) &&
                    Feature.INFECTION.isEnabled(event.getEntity()) &&
                    player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                applyInfection(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in InfectionFeature.onEntityDamageByEntity", e);
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.hasMetadata("NPC")) return;
        if (!Feature.INFECTION.isEnabled(player)) return;
        PlayerData data = PlayerData.get(player);
        if (data != null && data.isInfected()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !hasMovedBlock(event)) return;
        if (!Feature.INFECTION.isEnabled(player)) return;

        PlayerData data = PlayerData.get(player);
        if (data == null) return;

        if (plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_REMOVE)) {
            if (data.isInfected()) clearInfection(player, data);
            return;
        }

        if (plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) &&
                isOnGround(event) && !Utils.hasCreativeGameMode(player) && data.isInfected()) {
            player.getWorld().spawnParticle(
                    Particle.ENTITY_EFFECT,
                    player.getLocation().add(0, 1, 0),
                    5, 0.3, 0, 0.3, 0,
                    Color.fromRGB(34, 139, 34)
            );
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasItem() || !Feature.INFECTION.isEnabled(player)) return;

        var item = player.getInventory().getItemInMainHand();
        if (!Utils.isPluginItem(item, false) || !item.isSimilar(CustomItem.STRANGE_BREW.a())) return;

        PlayerData data = PlayerData.get(player);
        if (data == null || !data.isInfected()) return;

        event.setCancelled(true);
        consumeItem(player);
        clearInfection(player, data);
    }

    private void clearInfection(Player player, PlayerData data) {
        data.setInfected(false);
        player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(Utils.msg("prefix") + " " + Utils.msg("use-strange-brew")));
        player.removePotionEffect(PotionEffectType.NAUSEA);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
    }

    private void applyInfection(Player player) {
        PlayerData data = PlayerData.get(player);
        if (data == null || data.isInfected()) return;
        double chance = Feature.INFECTION.getDouble("chance-percent") / 100.0;
        if (RANDOM.nextDouble() <= chance) {
            data.setInfected(true);
            player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacyAmpersand().deserialize(Utils.msg("prefix") + " " + Utils.msg("now-infected")));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1.0f, 2.0f);
        }
    }

    private boolean isZombieEntity(Entity entity) {
        return entity instanceof Zombie || entity.getType() == EntityType.ZOMBIFIED_PIGLIN;
    }

    private void consumeItem(Player player) {
        var item = player.getInventory().getItemInMainHand();
        int newAmount = item.getAmount() - 1;
        player.getInventory().setItemInMainHand(newAmount < 1 ? null : item);
        if (newAmount >= 1) item.setAmount(newAmount);
    }

    private boolean hasMovedBlock(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        return from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ();
    }

    private boolean isOnGround(PlayerMoveEvent event) {
        Location to = event.getTo();
        return to.getBlock().getRelative(0, -1, 0).getType().isSolid();
    }
}