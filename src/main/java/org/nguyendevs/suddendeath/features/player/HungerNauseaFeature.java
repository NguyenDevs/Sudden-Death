package org.nguyendevs.suddendeath.features.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class HungerNauseaFeature extends AbstractFeature {

    // Danh sách lưu UUID của những người chơi đang bị đói (đã nhận thông báo đói)
    private final Set<UUID> starvingPlayers = new HashSet<>();

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
                        if (player == null || Utils.hasCreativeGameMode(player)) {
                            handleRecovery(player);
                            continue;
                        }

                        if (Feature.HUNGER_NAUSEA.isEnabled(player) && player.getFoodLevel() < 8) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 0));
                            if (!starvingPlayers.contains(player.getUniqueId())) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("hunger-nausea")));
                                starvingPlayers.add(player.getUniqueId());
                            }
                        } else {
                            handleRecovery(player);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Hunger Nausea loop", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L));
    }

    private void handleRecovery(Player player) {
        if (starvingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Utils.msg("prefix") + " " + Utils.msg("no-longer-hunger")));
            starvingPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        starvingPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Override
    protected void onDisable() {
        starvingPlayers.clear();
    }
}