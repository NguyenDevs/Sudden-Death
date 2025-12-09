package org.nguyendevs.suddendeath.features.player;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.*;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.bukkit.entity.*;
import java.util.logging.Level;

public class InfectionFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Infection";
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;

        try {
            // Zombie to Player infection
            if (event.getEntity() instanceof Player player && isZombieEntity(event.getDamager()) &&
                    Feature.INFECTION.isEnabled(player) &&
                    plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT)) {
                applyInfection(player);
            }

            // Player to Zombie infection (hand attack)
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
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in InfectionFeature.onEntityRegainHealth", e);
        }
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
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            Utils.msg("prefix") + " " + Utils.msg("no-longer-infected")));
                    player.removePotionEffect(PotionEffectType.CONFUSION);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
                }
                return;
            }

            if (plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) &&
                    player.isOnGround() && !Utils.hasCreativeGameMode(player) && data.isInfected()) {
                Color infectionColor = Color.fromRGB(34, 139, 34);
                player.getWorld().spawnParticle(Particle.SPELL_MOB,
                        player.getLocation().add(0, 1, 0), 5, 0.3, 0, 0.3, 0, infectionColor);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in InfectionFeature.onPlayerMove", e);
        }
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
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Utils.msg("prefix") + " " + Utils.msg("use-strange-brew")));
            player.removePotionEffect(PotionEffectType.CONFUSION);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.0f);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in InfectionFeature.onPlayerInteract", e);
        }
    }

    private void applyInfection(Player player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) return;

        double chance = Feature.INFECTION.getDouble("chance-percent") / 100.0;
        if (RANDOM.nextDouble() <= chance && !data.isInfected()) {
            data.setInfected(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Utils.msg("prefix") + " " + Utils.msg("now-infected")));
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
        return to != null &&
                (from.getBlockX() != to.getBlockX() ||
                        from.getBlockY() != to.getBlockY() ||
                        from.getBlockZ() != to.getBlockZ());
    }
}