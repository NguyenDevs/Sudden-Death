package org.nguyendevs.suddendeath.gui;

import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.MobStat;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.regex.Pattern;

public class StatEditor implements Listener {
    private final String path;
    private final EntityType type;
    private final MobStat stat;
    private final ConfigFile config;

    private boolean open = true;

    public StatEditor(String path, EntityType type, MobStat stat, ConfigFile config) {
        this.path = path;
        this.type = type;
        this.stat = stat;
        this.config = config;

        Bukkit.getPluginManager().registerEvents(this, SuddenDeath.plugin);
    }

    public void close() {
        Validate.isTrue(open, "Already closed");
        open = false;

        AsyncPlayerChatEvent.getHandlerList().unregister(this);
        InventoryOpenEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void doThings(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        if (event.getMessage().equals("cancel")) {
            close();
            player.sendMessage(ChatColor.YELLOW + "Mob editing canceled.");
            new MonsterEdition(player, type, path).open();
            return;
        }

        ConfigFile config = new ConfigFile(type);
        MobStat.Type valueType = stat.getType();
        // ==================================================================================================================================
        if (valueType == MobStat.Type.STRING) {
            String msg = event.getMessage();
            close();
            config.getConfig().set(path + "." + stat.getPath(), msg);

            // remove if set to "null"
            if (msg.equalsIgnoreCase("null"))
                config.getConfig().set(path + "." + stat.getPath(), null);

            config.save();
            new MonsterEdition(player, type, path).open();
            player.sendMessage(ChatColor.YELLOW + stat.getName() + " succesfully changed to " + msg + "�7.");
            return;
        }
        // ==================================================================================================================================
        if (valueType == MobStat.Type.DOUBLE) {
            double value = 0;
            try {
                value = Double.parseDouble(event.getMessage());
            } catch (Exception e1) {
                player.sendMessage(ChatColor.RED + event.getMessage() + " is not a valid number.");
                return;
            }
            close();
            config.getConfig().set(path + "." + stat.getPath(), value == 0 ? null : value);
            config.save();
            new MonsterEdition(player, type, path).open();
            player.sendMessage(ChatColor.YELLOW + stat.getName() + " succesfully changed to " + value + ".");
            return;
        }
        // ==================================================================================================================================
        if (valueType == MobStat.Type.POTION_EFFECTS) {
            String[] split = event.getMessage().split(Pattern.quote(" "));
            if (split.length != 2) {
                player.sendMessage(ChatColor.RED + event.getMessage() + " is not a valid [POTION_EFFECT] [AMPLIFIER].");
                player.sendMessage(ChatColor.RED + "Example: 'INCREASE_DAMAGE 4' stands for Strength 4.");
                return;
            }

            // effect
            PotionEffectType effect = null;
            for (PotionEffectType effect1 : PotionEffectType.values())
                if (effect1 != null && effect1.getName().equalsIgnoreCase(split[0].replace("-", "_"))) {
                    effect = effect1;
                    break;
                }

            if (effect == null) {
                player.sendMessage(ChatColor.RED + split[0] + " is not a valid potion effect!");
                player.sendMessage(ChatColor.RED + "All potion effects can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html");
                return;
            }

            // amplifier
            int amplifier = 0;
            try {
                amplifier = (int) Double.parseDouble(split[1]);
            } catch (Exception e1) {
                player.sendMessage(ChatColor.RED + split[1] + " is not a valid number!");
                return;
            }

            close();
            config.getConfig().set(path + "." + stat.getPath() + "." + effect.getName(), amplifier);
            config.save();
            new MonsterEdition(player, type, path).open();
            player.sendMessage(ChatColor.YELLOW + effect.getName() + " " + amplifier + " succesfully added.");
            return;
        }
    }

    @EventHandler
    public void closeOnInventoryOpen(InventoryOpenEvent event) {
        if (stat.getType() != MobStat.Type.ITEMSTACK)
            close();
    }
}
