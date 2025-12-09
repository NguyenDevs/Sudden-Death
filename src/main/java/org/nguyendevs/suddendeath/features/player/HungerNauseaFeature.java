package org.nguyendevs.suddendeath.features.player;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.logging.Level;

public class HungerNauseaFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Hunger Nausea";
    }

    @Override
    protected void onEnable() {
        BukkitRunnable playerLoop = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Bukkit.getOnlinePlayers().forEach(player -> loop3s_player(player));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Player loop task", e);
                }
            }
        };
        playerLoop.runTaskTimer(plugin, 0L, 60L);
        registerTask((BukkitTask) playerLoop);
    }

    private void loop3s_player(Player player) {
        if (player == null || Utils.hasCreativeGameMode(player)) return;

        try {
            PlayerData data = PlayerData.get(player);
            if (data == null) return;

            if (Feature.HUNGER_NAUSEA.isEnabled(player) && player.getFoodLevel() < 8) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 0));
            }

            if (Feature.BLEEDING.isEnabled(player) && data.isBleeding() &&
                    plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) &&
                    player.getHealth() >= Feature.BLEEDING.getDouble("health-min")) {
                Utils.damage(player, Feature.BLEEDING.getDouble("dps") * 3, Feature.BLEEDING.getBoolean("tug"));
            }

            if (Feature.INFECTION.isEnabled(player) && data.isInfected() &&
                    plugin.getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EFFECT) &&
                    player.getHealth() >= Feature.INFECTION.getDouble("health-min")) {
                Utils.damage(player, Feature.INFECTION.getDouble("dps") * 3, Feature.INFECTION.getBoolean("tug"));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 600, 0));
                if (Feature.INFECTION.getBoolean("sound")) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 666.0f, 0.1f);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in player loop for: " + player.getName(), e);
        }
    }
}