package org.nguyendevs.suddendeath.Features.player;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import org.bukkit.entity.*;
import java.util.logging.Level;

@SuppressWarnings("deprecation")

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
                        if (player == null || Utils.hasCreativeGameMode(player)) continue;
                        PlayerData data = PlayerData.get(player);
                        if (data == null) continue;

                        if (Feature.INFECTION.isEnabled(player) && data.isInfected() &&
                                plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) &&
                                player.getHealth() >= Feature.INFECTION.getDouble("health-min")) {
                            Utils.damage(player, Feature.INFECTION.getDouble("dps") * 3, Feature.INFECTION.getBoolean("tug"));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 600, 0));
                            if (Feature.INFECTION.getBoolean("sound")) {
                                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 666.0f, 0.1f);
                            }
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
            if (event.getEntity() instanceof Player player && isZombieEntity(event.getDamager()) &&
                    Feature.INFECTION.isEnabled(player) &&
                    plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT)) {
                applyInfection(player);
            }
            if (isZombieEntity(event.getEntity()) && event.getDamager() instanceof Player player &&
                    Feature.INFECTION.isEnabled(event.getEntity()) &&
                    player.getInventory().getItemInMainHand().getType() == Material.AIR &&
                    !Utils.hasCreativeGameMode(player)) {
                applyInfection(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in InfectionFeature.onEntityDamageByEntity", e);
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.hasMetadata("NPC")) return;
        if (!Feature.INFECTION.isEnabled(player)) return;
        try {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.isInfected()) {
                event.setCancelled(true);
            }
        } catch (Exception e) {}
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !hasMovedBlock(event)) return;
        if (!Feature.INFECTION.isEnabled(player)) return;
        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) return;
            if (plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_REMOVE)) {
                if (data.isInfected()) {
                    data.setInfected(false);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("use-strange-brew")));
                    player.removePotionEffect(PotionEffectType.CONFUSION);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
                }
                return;
            }
            if (plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) &&
                    player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isInfected()) {
                Color infectionColor = Color.fromRGB(34, 139, 34);
                player.getWorld().spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1, 0), 5, 0.3, 0, 0.3, 0, infectionColor);
            }
        } catch (Exception e) {}
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.hasItem()) return;
        if (!Feature.INFECTION.isEnabled(player)) return;
        try {
            var item = player.getInventory().getItemInMainHand();
            if (!Utils.isPluginItem(item, false)) return;
            if (!item.isSimilar(CustomItem.STRANGE_BREW.a())) return;
            PlayerData data = PlayerData.get(player);
            if (data == null || !data.isInfected()) return;
            event.setCancelled(true);
            consumeItem(player);
            data.setInfected(false);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("use-strange-brew")));
            player.removePotionEffect(PotionEffectType.CONFUSION);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
        } catch (Exception e) {}
    }

    private void applyInfection(Player player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) return;
        double chance = Feature.INFECTION.getDouble("chance-percent") / 100.0;
        if (RANDOM.nextDouble() <= chance && !data.isInfected()) {
            data.setInfected(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("now-infected")));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, 1.0f, 2.0f);
        }
    }

    private boolean isZombieEntity(Entity entity) {
        return entity instanceof Zombie || entity instanceof PigZombie ||
                entity instanceof ZombieVillager || entity instanceof Husk ||
                entity instanceof Drowned;
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
}