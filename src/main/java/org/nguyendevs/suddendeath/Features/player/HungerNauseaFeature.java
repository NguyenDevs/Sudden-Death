package org.nguyendevs.suddendeath.Features.player;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class HungerNauseaFeature extends AbstractFeature {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
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
                        if (Utils.hasCreativeGameMode(player)) {
                            handleRecovery(player);
                            continue;
                        }
                        if (Feature.HUNGER_NAUSEA.isEnabled(player) && player.getFoodLevel() < 8) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 180, 0));
                            if (starvingPlayers.add(player.getUniqueId())) {
                                sendMsg(player, "hunger-nausea");
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
        if (starvingPlayers.remove(player.getUniqueId())) {
            sendMsg(player, "no-longer-hunger");
        }
    }

    private void sendMsg(Player player, String key) {
        player.sendMessage(LEGACY.deserialize(Utils.msg("prefix") + " " + Utils.msg(key)));
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