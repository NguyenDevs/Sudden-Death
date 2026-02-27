package org.nguyendevs.suddendeath.Features.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.logging.Level;

@SuppressWarnings("deprecation")

public class SnowSlowFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Snow Slow";
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !hasMovedBlock(event)) return;

        try {
            if (Feature.SNOW_SLOW.isEnabled(player) && !Utils.hasCreativeGameMode(player) &&
                    player.getLocation().getBlock().getType() == Material.SNOW &&
                    !isWearingLeatherBoots(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in SnowSlowFeature", e);
        }
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
}