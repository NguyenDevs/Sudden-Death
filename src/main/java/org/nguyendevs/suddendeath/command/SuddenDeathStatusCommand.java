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
import org.nguyendevs.suddendeath.gui.CrafterInventory;
import org.nguyendevs.suddendeath.gui.PlayerView;
import org.nguyendevs.suddendeath.gui.Status;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.logging.Level;

public class SuddenDeathStatusCommand implements CommandExecutor {
    private static final String PREFIX = "&6[&cSudden&4Death&6]";
    private static final String PERMISSION_STATUS = "suddendeath.admin";
    private static final String PERMISSION_RECIPE = "suddendeath.recipe";
    private static final String PERMISSION_STATUS_VIEW = "suddendeath.status";
    private static final String STRIKETHROUGH = "&8&m---------------";
    private static final String HELP_HEADER = STRIKETHROUGH + "[&d Sudden Death Help Page &8&m]---------------";
    private static final String ITEM_LIST_HEADER = STRIKETHROUGH + "[&d Sudden Death Items &8&m]-----------------";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        try {
            if (args.length == 0) {
                if (!sender.hasPermission(PERMISSION_STATUS)) {
                    sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                    if (sender instanceof Player) {
                        playErrorSound((Player) sender);
                    }
                    return true;
                }
                if (sender instanceof Player player) {
                    sendHelpMessage(sender);
                } else {
                    sender.sendMessage(PREFIX + " " + translateColors("&cThis command is only available for players."));
                }
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "status" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS_VIEW)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    if (sender instanceof Player player) {
                        new Status(player).open();
                        playSound(player);
                    } else {
                        sender.sendMessage(PREFIX + " " + translateColors("&cThis command is only available for players."));
                    }
                }
                case "menu" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS_VIEW)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    if (sender instanceof Player player) {
                        new PlayerView(player).open();
                        playSound(player);
                    } else {
                        sender.sendMessage(PREFIX + " " + translateColors("&cThis command is only available for players."));
                    }
                }
                case "recipe" -> {
                    if (!sender.hasPermission(PERMISSION_RECIPE)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    if (sender instanceof Player player) {
                        new CrafterInventory(player);
                        playSound(player);
                    } else {
                        sender.sendMessage(PREFIX + " " + translateColors("&cThis command is only available for players."));
                    }
                }
                case "help" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    sendHelpMessage(sender);
                }
                case "clean" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    handleCleanCommand(sender, args);
                }
                case "start" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    if (sender instanceof Player player) {
                        handleStartCommand(player, args);
                    } else {
                        sender.sendMessage(PREFIX + " " + translateColors("&cThis command is only available for players."));
                    }
                }
                case "stop" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    if (sender instanceof Player player) {
                        handleStopCommand(player, args);
                        String displayName = args.length > 1 ? Utils.caseOnWords(args[1].toLowerCase().replace("-", " ")) : "event";
                        player.sendMessage(PREFIX + " " + translateColors("&aSuccessfully stopped event " + displayName + "."));
                    } else {
                        sender.sendMessage(PREFIX + " " + translateColors("&cThis command is only available for players."));
                    }
                }
                case "admin" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    if (sender instanceof Player player) {
                        new AdminView(player).open();
                        playSound(player);
                    } else {
                        sender.sendMessage(PREFIX + " " + translateColors("&cThis command is only available for players."));
                    }
                }
                case "reload" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    handleReloadCommand(sender);
                }
                case "itemlist" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    sendItemList(sender);
                }
                case "give" -> {
                    if (!sender.hasPermission(PERMISSION_STATUS)) {
                        sender.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
                        if (sender instanceof Player) {
                            playErrorSound((Player) sender);
                        }
                        return true;
                    }
                    handleGiveCommand(sender, args);
                }
                default -> {
                    sender.sendMessage(PREFIX + " " + translateColors("&cUnknown command. Use /sds help for a list of commands."));
                    if (sender instanceof Player) {
                        playErrorSound((Player) sender);
                    }
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error processing command /sds for sender: " + sender.getName(), e);
            sender.sendMessage(PREFIX + " " + translateColors("&cAn error occurred while processing your command."));
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(translateColors(HELP_HEADER));
        sender.sendMessage(translateColors("&d<> &7= required"));
        sender.sendMessage(translateColors("&d() &7= optional"));
        sender.sendMessage(translateColors("&d... &7= multiple args support"));
        sender.sendMessage("");
        sender.sendMessage(translateColors("&d/sds &fshows your status."));
        if (sender.hasPermission(PERMISSION_STATUS)) {
            sender.sendMessage(translateColors("&d/sds admin &fopens the admin GUI."));
            sender.sendMessage(translateColors("&d/sds clean &fremoves all negative effects (Bleeding...)."));
            sender.sendMessage(translateColors("&d/sds give <item> (player) (amount) &fgives a player an item."));
            sender.sendMessage(translateColors("&d/sds help &fdisplays the help page."));
            sender.sendMessage(translateColors("&d/sds itemlist &fdisplays the item list."));
            sender.sendMessage(translateColors("&d/sds reload &freloads the config file."));
            sender.sendMessage(translateColors("&d/sds start <event> &fstarts an event."));
            sender.sendMessage(translateColors("&d/sds stop <event> &fstop an event."));
        }
        if (sender.hasPermission(PERMISSION_STATUS_VIEW)) {
            sender.sendMessage(translateColors("&d/sds menu &fopens the feature view GUI."));
            sender.sendMessage(translateColors("&d/sds status &fshows your status."));
        }
        if (sender.hasPermission(PERMISSION_RECIPE)) {
            sender.sendMessage(translateColors("&d/sds recipe &fopens the crafting recipe GUI."));
        }
        if (sender instanceof Player) {
            playSound((Player) sender);
        }
    }

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
                sender.sendMessage(PREFIX + " " + translateColors("&aRemoved bad status for " + player.getName() + "."));
                playSound(player);
            } else {
                sender.sendMessage(PREFIX + " " + translateColors("&cPlease specify a player to clean."));
            }
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + " " + translateColors("&cCouldn't find player called " + args[1] + "."));
            if (sender instanceof Player) {
                playErrorSound((Player) sender);
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
        sender.sendMessage(PREFIX + " " + translateColors("&aRemoved bad status for " + target.getName() + "."));
        if (sender instanceof Player) {
            playSound((Player) sender);
        }
    }

    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + " " + translateColors("&cPlease specify an event to start."));
            playErrorSound(player);
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
            player.sendMessage(PREFIX + " " + translateColors("&cCould not find event called " + args[1].toUpperCase().replace("-", "_") + "."));
            playErrorSound(player);
        }
    }

    private void handleStopCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + " " + translateColors("&cPlease specify an event to stop."));
            playErrorSound(player);
            return;
        }
        try {
            Feature feature = Feature.valueOf(args[1].toUpperCase().replace("-", "_"));
            Validate.isTrue(feature.isEvent(), "Specified feature is not an event.");
            player.getWorld().setTime(6000);
            player.getWorld().setStorm(false);
            player.getWorld().setThundering(false);
        } catch (IllegalArgumentException e) {
            player.sendMessage(PREFIX + " " + translateColors("&cCould not find event called " + args[1].toUpperCase().replace("-", "_") + "."));
            playErrorSound(player);
        }
    }
    private void handleReloadCommand(CommandSender sender) {
        try {
            SuddenDeath plugin = SuddenDeath.getInstance();
            plugin.reloadConfigFiles();
            for (Feature feature : Feature.values()) {
                feature.updateConfig();
            }
            sender.sendMessage(PREFIX + " " + translateColors("&e" + plugin.getName() + " " + plugin.getDescription().getVersion() + " reloaded."));
            if (sender instanceof Player) {
                playSound((Player) sender);
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error reloading plugin for sender: " + sender.getName(), e);
            sender.sendMessage(PREFIX + " " + translateColors("&cAn error occurred while reloading the plugin."));
        }
    }

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
                sender.sendMessage(PREFIX + " " + translateColors("&f" + item.getName() + " (" + item.name() + ")"));
            }
        }
        if (sender instanceof Player) {
            playSound((Player) sender);
        }
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(translateColors(Utils.msg("prefix") + " " + "&cPlease specify an item to give."));
            if (sender instanceof Player) {
                playErrorSound((Player) sender);
            }
            return;
        }

        ItemStack itemStack;
        try {
            itemStack = CustomItem.valueOf(args[1].toUpperCase().replace("-", "_")).a();
        } catch (IllegalArgumentException e) {
            sender.sendMessage(translateColors(Utils.msg("prefix") + " " + "&cCouldn't find the item called " + args[1].toUpperCase().replace("-", "_") + "."));
            if (sender instanceof Player) {
                playErrorSound((Player) sender);
            }
            return;
        }

        if (args.length > 3) {
            try {
                int amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    sender.sendMessage(translateColors(Utils.msg("prefix") + " " + "&c" + args[3] + " is not a valid positive number."));
                    if (sender instanceof Player) {
                        playErrorSound((Player) sender);
                    }
                    return;
                }
                itemStack.setAmount(amount);
            } catch (NumberFormatException e) {
                sender.sendMessage(translateColors(Utils.msg("prefix") + " " + args[3] + " is not a valid number."));
                if (sender instanceof Player) {
                    playErrorSound((Player) sender);
                }
                return;
            }
        }

        Player target;
        if (args.length > 2) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(translateColors(Utils.msg("prefix") + " " + "&cCouldn't find player called " + args[2] + "."));
                if (sender instanceof Player) {
                    playErrorSound((Player) sender);
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

    private void playSound(Player player) {
        if (player != null) {
            try {
                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.5f);
            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Error playing sound for player: " + player.getName(), e);
            }
        }
    }
    private void playErrorSound(Player player){
        if (player != null) {
            try {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Error playing sound for player: " + player.getName(), e);
            }
        }
    }

    private String getMessage(String key) {
        try {
            return SuddenDeath.getInstance().messages.getConfig().getString(key, "Message not found: " + key);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error retrieving message for key: " + key, e);
            return "Error retrieving message: " + key;
        }
    }

    private String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}