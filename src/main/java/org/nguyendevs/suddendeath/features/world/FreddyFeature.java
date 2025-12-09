package org.nguyendevs.suddendeath.features.world;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import org.bukkit.ChatColor;
import java.util.logging.Level;

public class FreddyFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Freddy";
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        if (!Feature.FREDDY.isEnabled(player) || Utils.hasCreativeGameMode(player)) {
            return;
        }

        try {
            double chance = Feature.FREDDY.getDouble("chance-percent") / 100.0;
            if (RANDOM.nextDouble() >= chance) {
                return;
            }

            Enderman freddy = (Enderman) player.getWorld().spawnEntity(player.getLocation(), EntityType.ENDERMAN);
            freddy.setCustomName("Freddy");
            freddy.setCustomNameVisible(true);

            setAttribute(freddy, Attribute.GENERIC_MAX_HEALTH, 1.75);
            setAttribute(freddy, Attribute.GENERIC_MOVEMENT_SPEED, 1.35);
            setAttribute(freddy, Attribute.GENERIC_ATTACK_DAMAGE, 1.35);

            freddy.setTarget(player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("freddy-summoned")));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 2.0f, 0.0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in Freddy feature", e);
        }
    }

    private void setAttribute(Enderman entity, Attribute attr, double multiplier) {
        AttributeInstance instance = entity.getAttribute(attr);
        if (instance != null) {
            instance.setBaseValue(instance.getBaseValue() * multiplier);
        }
    }
}