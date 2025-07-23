package org.nguyendevs.suddendeath.gui;

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
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.MobStat;

import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Listener for handling stat editing of custom mobs via chat input in the SuddenDeath plugin.
 */
public class StatEditor implements Listener {
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private static final String CANCEL_COMMAND = "cancel";
    private static final String NULL_VALUE = "null";

    private final String path;
    private final EntityType type;
    private final MobStat stat;
    private final ConfigFile config;
    private boolean open;

    /**
     * Constructs a StatEditor for editing mob stats via chat input.
     *
     * @param path   The configuration path for the mob stat.
     * @param type   The entity type of the mob.
     * @param stat   The mob stat to edit.
     * @param config The configuration file to save changes to.
     */
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

    /**
     * Closes the stat editor and unregisters event listeners.
     *
     * @throws IllegalStateException if the editor is already closed.
     */
    public void close() {
        Validate.isTrue(open, "StatEditor is already closed");
        open = false;
        AsyncPlayerChatEvent.getHandlerList().unregister(this);
        InventoryOpenEvent.getHandlerList().unregister(this);
    }

    /**
     * Handles chat input to update mob stats based on the player's message.
     *
     * @param event The AsyncPlayerChatEvent.
     */
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
                    player.sendMessage(ChatColor.RED + "Unsupported stat type: " + stat.getType());
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling AsyncPlayerChatEvent for player: " + player.getName(), e);
            player.sendMessage(ChatColor.RED + "An error occurred while processing your input.");
        }
    }

    /**
     * Handles cancellation of the stat editing process.
     *
     * @param player The player who canceled the operation.
     */
    private void handleCancel(Player player) {
        close();
        player.sendMessage(ChatColor.YELLOW + "Mob editing canceled.");
        new MonsterEdition(player, type, path).open();
    }

    /**
     * Handles string input for mob stats.
     *
     * @param player  The player providing the input.
     * @param message The input message.
     */
    private void handleStringInput(Player player, String message) {
        close();
        config.getConfig().set(path + "." + stat.getPath(), message.equalsIgnoreCase(NULL_VALUE) ? null : message);
        config.save();
        new MonsterEdition(player, type, path).open();
        player.sendMessage(ChatColor.YELLOW + stat.getName() + " successfully changed to " + message + ".");
    }

    /**
     * Handles double input for mob stats.
     *
     * @param player  The player providing the input.
     * @param message The input message.
     */
    private void handleDoubleInput(Player player, String message) {
        try {
            double value = Double.parseDouble(message);
            close();
            config.getConfig().set(path + "." + stat.getPath(), value == 0 ? null : value);
            config.save();
            new MonsterEdition(player, type, path).open();
            player.sendMessage(ChatColor.YELLOW + stat.getName() + " successfully changed to " + value + ".");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + message + " is not a valid number.");
        }
    }

    /**
     * Handles potion effect input for mob stats.
     *
     * @param player  The player providing the input.
     * @param message The input message.
     */
    private void handlePotionEffectInput(Player player, String message) {
        String[] split = SPACE_PATTERN.split(message);
        if (split.length != 2) {
            player.sendMessage(ChatColor.RED + message + " is not a valid [POTION_EFFECT] [AMPLIFIER].");
            player.sendMessage(ChatColor.RED + "Example: 'INCREASE_DAMAGE 4' stands for Strength 4.");
            return;
        }

        String effectName = split[0].replace("-", "_").toUpperCase();
        PotionEffectType effect = PotionEffectType.getByName(effectName);
        if (effect == null) {
            player.sendMessage(ChatColor.RED + split[0] + " is not a valid potion effect!");
            player.sendMessage(ChatColor.RED + "All potion effects can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html");
            return;
        }

        try {
            int amplifier = Integer.parseInt(split[1]);
            close();
            config.getConfig().set(path + "." + stat.getPath() + "." + effect.getName(), amplifier);
            config.save();
            new MonsterEdition(player, type, path).open();
            player.sendMessage(ChatColor.YELLOW + effect.getName() + " " + amplifier + " successfully added.");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + split[1] + " is not a valid number!");
        }
    }

    /**
     * Closes the stat editor when an inventory is opened, unless editing an ItemStack stat.
     *
     * @param event The InventoryOpenEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!open || stat.getType() == MobStat.Type.ITEMSTACK) {
            return;
        }
        close();
    }
}