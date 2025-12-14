package org.nguyendevs.suddendeath.GUI;

import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.ConfigFile;
import org.nguyendevs.suddendeath.Utils.MobStat;

import java.util.logging.Level;
import java.util.regex.Pattern;

public class StatEditor implements Listener {
    private static final String PREFIX = "&6[&cSudden&4Death&6]";
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private static final String CANCEL_COMMAND = "cancel";
    private static final String NULL_VALUE = "null";

    private final String path;
    private final EntityType type;
    private final MobStat stat;
    private final ConfigFile config;
    private boolean open;

    public StatEditor(String path, EntityType type, MobStat stat, ConfigFile config) {
        Validate.notNull(path, "Path cannot be null");
        Validate.notNull(type, "EntityType cannot be null");
        Validate.notNull(stat, "MobStat cannot be null");
        Validate.notNull(config, "ConfigFile cannot be null");

        this.path = path;
        this.type = type;
        this.stat = stat;
        this.config = config;
        this.open = true;

        Bukkit.getPluginManager().registerEvents(this, SuddenDeath.getInstance());
    }

    public void close() {
        Validate.isTrue(open, "StatEditor is already closed");
        open = false;
        AsyncPlayerChatEvent.getHandlerList().unregister(this);
        InventoryOpenEvent.getHandlerList().unregister(this);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!open) {
            return;
        }
        Player player = event.getPlayer();
        event.setCancelled(true);
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase(CANCEL_COMMAND)) {
                handleCancel(player);
                return;
            }
            switch (stat.getType()) {
                case STRING:
                    handleStringInput(player, message);
                    break;
                case DOUBLE:
                    handleDoubleInput(player, message);
                    break;
                case POTION_EFFECTS:
                    handlePotionEffectInput(player, message);
                    break;
                default:
                    player.sendMessage(translateColors(PREFIX + " " + "&cUnsupported stat type: " + stat.getType()));
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling AsyncPlayerChatEvent for player: " + player.getName(), e);
            player.sendMessage(translateColors(PREFIX + " " + "&cAn error occurred while processing your input."));
        }
    }

    private void handleCancel(Player player) {
        close();
        player.sendMessage(translateColors(PREFIX + " " + "&7Mob editing canceled."));
        new MonsterEdition(player, type, path).open();
    }

    private void handleStringInput(Player player, String message) {
        close();
        config.getConfig().set(path + "." + stat.getPath(), message.equalsIgnoreCase(NULL_VALUE) ? null : message);
        config.save();
        new MonsterEdition(player, type, path).open();
        player.sendMessage(translateColors(PREFIX + " " + "&e" + stat.getName() + " &7successfully changed to &6" + message + "&7."));
    }

    private void handleDoubleInput(Player player, String message) {
        try {
            double value = Double.parseDouble(message);
            close();
            config.getConfig().set(path + "." + stat.getPath(), value == 0 ? null : value);
            config.save();
            new MonsterEdition(player, type, path).open();
            player.sendMessage(translateColors(PREFIX + " " + "&e" + stat.getName() + " &7successfully changed to &6" + value + "&7."));
        } catch (NumberFormatException e) {
            player.sendMessage(translateColors(PREFIX + " " + "&c" + message + " is not a valid number."));
        }
    }

    private void handlePotionEffectInput(Player player, String message) {
        String[] split = SPACE_PATTERN.split(message);
        if (split.length != 2) {
            player.sendMessage(translateColors(PREFIX + " " + "&c" + message + " is not a valid [POTION_EFFECT] [AMPLIFIER]."));
            player.sendMessage(translateColors("&7" + "► Example: &e'INCREASE_DAMAGE 4'&7 stands for &6Strength 4."));
            return;
        }

        String effectName = split[0].replace("-", "_").toUpperCase();
        PotionEffectType effect = PotionEffectType.getByName(effectName);
        if (effect == null) {
            player.sendMessage(translateColors(PREFIX + " " + "&c" + split[0] + " is not a valid potion effect!"));
            player.sendMessage(translateColors("&c" + "► All potion effects can be found here: &ehttps://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html"));
            return;
        }

        try {
            int amplifier = Integer.parseInt(split[1]);
            close();
            config.getConfig().set(path + "." + stat.getPath() + "." + effect.getName(), amplifier);
            config.save();
            new MonsterEdition(player, type, path).open();
            player.sendMessage(translateColors(PREFIX + " " + "&e" + effect.getName() + " " + amplifier + " &7successfully added."));
        } catch (NumberFormatException e) {
            player.sendMessage(translateColors(PREFIX + " " + "&c" + split[1] + " is not a valid number!"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!open || stat.getType() == MobStat.Type.ITEMSTACK) {
            return;
        }
        close();
    }
    private String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}