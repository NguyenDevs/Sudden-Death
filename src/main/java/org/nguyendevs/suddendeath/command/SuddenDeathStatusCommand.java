package org.nguyendevs.suddendeath.command;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.gui.AdminView;
import org.nguyendevs.suddendeath.gui.Status;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.logging.Level;

/**
 * Command executor for the SuddenDeath plugin's /sds command.
 * Handles status viewing, admin GUI, item management, and more.
 */
public class SuddenDeathStatusCommand implements CommandExecutor {
    private static final String PERMISSION_STATUS = "suddendeath.status";
    private static final String STRIKETHROUGH = "&8&m---------------";
    private static final String HELP_HEADER = STRIKETHROUGH + "[&d Sudden Death Help Page &8&m]---------------";
    private static final String ITEM_LIST_HEADER = STRIKETHROUGH + "[&d Sudden Death Items &8&m]-----------------";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_STATUS)) {
            sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
            if (sender instanceof Player) {
                playSound((Player) sender);
            }
            return true;
        }

        try {
            if (args.length == 0) {
                if (sender instanceof Player player) {
                    new Status(player).open();
                    playSound(player);
                } else {
                    sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " + translateColors("&cThis command is only available for players."));
                }
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help" -> sendHelpMessage(sender);
                case "clean" -> handleCleanCommand(sender, args);
                case "start" -> {
                    if (sender instanceof Player player) {
                        handleStartCommand(player, args);
                    } else {
                        sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " + translateColors("&cThis command is only available for players."));
                    }
                }
                case "admin" -> {
                    if (sender instanceof Player player) {
                        new AdminView(player).open();
                        playSound(player);
                    } else {
                        sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cThis command is only available for players."));
                    }
                }
                case "reload" -> handleReloadCommand(sender);
                case "itemlist" -> sendItemList(sender);
                case "give" -> handleGiveCommand(sender, args);
                default -> {
                    sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cUnknown command. Use /sds help for a list of commands."));
                    if (sender instanceof Player) {
                        playSound((Player) sender);
                    }
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error processing command /sds for sender: " + sender.getName(), e);
            sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while processing your command."));
        }
        return true;
    }

    /**
     * Sends the help message with available commands to the sender.
     *
     * @param sender The sender to send the message to.
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(translateColors(HELP_HEADER));
        sender.sendMessage(translateColors("&d<> &7= required"));
        sender.sendMessage(translateColors("&d() &7= optional"));
        sender.sendMessage(translateColors("&d... &7= multiple args support"));
        sender.sendMessage("");
        sender.sendMessage(translateColors("&d/sds &fshows your status."));
        sender.sendMessage(translateColors("&d/sds admin &fopens the admin GUI."));
        sender.sendMessage(translateColors("&d/sds help &fdisplays the help page."));
        sender.sendMessage(translateColors("&d/sds give <item> (player) (amount) &fgives a player an item."));
        sender.sendMessage(translateColors("&d/sds itemlist &fdisplays the item list."));
        sender.sendMessage(translateColors("&d/sds reload &freloads the config file."));
        sender.sendMessage(translateColors("&d/sds clean &fremoves all negative effects (Bleeding...)."));
        sender.sendMessage(translateColors("&d/sds start <event> &fstarts an event."));
        if (sender instanceof Player) {
            playSound((Player) sender);
        }
    }

    /**
     * Handles the clean command to remove negative effects from a player.
     *
     * @param sender The sender executing the command.
     * @param args   The command arguments.
     */
    private void handleCleanCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player player) {
                PlayerData data = PlayerData.get(player);
                if (data.isInfected()) {
                    data.setInfected(false);
                }
                if (data.isBleeding()) {
                    data.setBleeding(false);
                }
                sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&aRemoved bad status for " + player.getName() + "."));
                playSound(player);
            } else {
                sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cPlease specify a player to clean."));
            }
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cCouldn't find player called " + args[1] + "."));
            if (sender instanceof Player) {
                playSound((Player) sender);
            }
            return;
        }

        PlayerData data = PlayerData.get(target);
        if (data.isInfected()) {
            data.setInfected(false);
        }
        if (data.isBleeding()) {
            data.setBleeding(false);
        }
        sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " + translateColors("&aRemoved bad status for " + target.getName() + "."));
        if (sender instanceof Player) {
            playSound((Player) sender);
        }
    }

    /**
     * Handles the start command to initiate an event in the player's world.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cPlease specify an event to start."));
            playSound(player);
            return;
        }

        try {
            Feature feature = Feature.valueOf(args[1].toUpperCase().replace("-", "_"));
            Validate.isTrue(feature.isEvent(), "Specified feature is not an event.");
            player.getWorld().setTime(14000);
            SuddenDeath.getInstance().getEventManager().applyStatus(player.getWorld(), feature.generateWorldEventHandler(player.getWorld()));
            String message = translateColors("&4&l" + Utils.msg(feature.getPath()));
            for (Player online : player.getWorld().getPlayers()) {
                online.sendMessage(message);
                online.sendTitle("", message, 10, 40, 10);
                online.playSound(online.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.0f);
                online.playSound(online.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0f, 0.0f);
            }
            playSound(player);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cCould not find event called " + args[1].toUpperCase().replace("-", "_") + "."));
            playSound(player);
        }
    }

    /**
     * Handles the reload command to reload plugin configurations.
     *
     * @param sender The sender executing the command.
     */
    private void handleReloadCommand(CommandSender sender) {
        try {
            SuddenDeath plugin = SuddenDeath.getInstance();
            plugin.reloadConfigFiles();
            for (Feature feature : Feature.values()) {
                feature.updateConfig();
            }
            sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " + translateColors("&e" + plugin.getName() + " " + plugin.getDescription().getVersion() + " reloaded."));
            if (sender instanceof Player) {
                playSound((Player) sender);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error reloading plugin for sender: " + sender.getName(), e);
            sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while reloading the plugin."));
        }
    }

    /**
     * Sends the list of custom items to the sender with clickable commands for players.
     *
     * @param sender The sender to send the list to.
     */
    private void sendItemList(CommandSender sender) {
        sender.sendMessage(translateColors(ITEM_LIST_HEADER));
        sender.sendMessage("");
        for (CustomItem item : CustomItem.values()) {
            if (sender instanceof Player player) {
                player.spigot().sendMessage(new ComponentBuilder(item.getName())
                        .color(net.md_5.bungee.api.ChatColor.WHITE)
                        .append(" (")
                        .color(net.md_5.bungee.api.ChatColor.GRAY)
                        .append(item.name())
                        .color(net.md_5.bungee.api.ChatColor.WHITE)
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sds give " + item.name()))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(translateColors("&fClick to obtain " + item.getName()))))
                        .append(") ")
                        .color(net.md_5.bungee.api.ChatColor.GRAY)
                        .create());
            } else {
                sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " + translateColors("&f" + item.getName() + " (" + item.name() + ")"));
            }
        }
        if (sender instanceof Player) {
            playSound((Player) sender);
        }
    }

    /**
     * Handles the give command to provide a custom item to a player.
     *
     * @param sender The sender executing the command.
     * @param args   The command arguments.
     */
    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(translateColors(Utils.msg("prefix") + " " + "&cPlease specify an item to give."));
            if (sender instanceof Player) {
                playSound((Player) sender);
            }
            return;
        }

        ItemStack itemStack;
        try {
            itemStack = CustomItem.valueOf(args[1].toUpperCase().replace("-", "_")).a();
        } catch (IllegalArgumentException e) {
            sender.sendMessage(translateColors(Utils.msg("prefix") + " " +"&cCouldn't find the item called " + args[1].toUpperCase().replace("-", "_") + "."));
            if (sender instanceof Player) {
                playSound((Player) sender);
            }
            return;
        }

        if (args.length > 3) {
            try {
                int amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    sender.sendMessage(translateColors(Utils.msg("prefix") + " " +"&c" + args[3] + " is not a valid positive number."));
                    if (sender instanceof Player) {
                        playSound((Player) sender);
                    }
                    return;
                }
                itemStack.setAmount(amount);
            } catch (NumberFormatException e) {
                sender.sendMessage(translateColors(Utils.msg("prefix") + " "  + args[3] + " is not a valid number."));
                if (sender instanceof Player) {
                    playSound((Player) sender);
                }
                return;
            }
        }

        Player target;
        if (args.length > 2) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(translateColors(Utils.msg("prefix") + " " +"&cCouldn't find player called " + args[2] + "."));
                if (sender instanceof Player) {
                    playSound((Player) sender);
                }
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(translateColors(Utils.msg("prefix") + " " + "&cPlease specify a player to give the item to."));
            return;
        }

        String displayName = Utils.displayName(itemStack);
        String amountStr = itemStack.getAmount() > 1 ? " x" + itemStack.getAmount() : "";
        if (sender != target) {
            sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("give-item")
                    .replace("#item#", displayName)
                    .replace("#player#", target.getName())
                    .replace("#amount#", amountStr)));
        }
        target.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("receive-item")
                .replace("#item#", displayName)
                .replace("#amount#", amountStr)));

        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), itemStack);
        } else {
            target.getInventory().addItem(itemStack);
        }
        if (sender instanceof Player) {
            playSound((Player) sender);
        }
    }

    /**
     * Plays a sound effect for the player.
     *
     * @param player The player to play the sound for.
     */
    private void playSound(Player player) {
        if (player != null) {
            try {
                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Error playing sound for player: " + player.getName(), e);
            }
        }
    }

    /**
     * Retrieves a message from the plugin's messages configuration.
     *
     * @param key The message key.
     * @return The message, or a default message if not found.
     */
    private String getMessage(String key) {
        try {
            return SuddenDeath.getInstance().messages.getConfig().getString(key, "Message not found: " + key);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error retrieving message for key: " + key, e);
            return "Error retrieving message: " + key;
        }
    }

    /**
     * Translates color codes in a message.
     *
     * @param message The message containing color codes.
     * @return The translated message with applied colors.
     */
    private String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}