package org.nguyendevs.suddendeath.features.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
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
        registerTask(new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player == null || Utils.hasCreativeGameMode(player)) continue;

                        if (Feature.HUNGER_NAUSEA.isEnabled(player) && player.getFoodLevel() < 8) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 0));
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Hunger Nausea loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L));
    }
}