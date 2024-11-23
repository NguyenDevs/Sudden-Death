package org.nguyendevs.suddendeath.command;

import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.gui.AdminView;
import org.nguyendevs.suddendeath.gui.Status;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.nguyendevs.suddendeath.util.Utils;
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

import java.util.Objects;

import static org.nguyendevs.suddendeath.SuddenDeath.plugin;

public class SuddenDeathStatusCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            // Console hoặc Command Block
            if (args.length == 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Invalid command. Use /sds help for a list of commands.");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    handleReloadCommand(sender);
                    break;
                case "give":
                    handleGiveCommand(sender, args);
                    break;
                case "clean":
                    handleCleanCommand(sender, args);
                    break;
                case "itemlist":
                    handleCleanCommand(sender, args);
                    break;
                default:
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix")))+" " +ChatColor.RED + "Only players can use this command.");
                    break;
            }
            return true;
        }

        // Player
        Player player = (Player) sender;
        if (!player.hasPermission("suddendeath.status")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + plugin.messages.getConfig().getString("not-enough-perms"));
            playSound(player);
            return true;
        }

        if (args.length == 0) {
            new Status(player).open();
            playSound(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(player);
                break;
            case "clean":
                handleCleanCommand(player, args);
                break;
            case "start":
                handleStartCommand(player, args);
                break;
            case "admin":
                new AdminView(player).open();
                playSound(player);
                break;
            case "reload":
                handleReloadCommand(player);
                break;
            case "itemlist":
                sendItemList(player);
                break;
            case "give":
                handleGiveCommand(player, args);
                break;
            default:
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Unknown command. Use /sds help for a list of commands.");
                playSound(player);
                break;
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "---------------[" + ChatColor.LIGHT_PURPLE + " Sudden Death Help Page " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "]---------------");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "<>" + ChatColor.GRAY + " = required");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "()" + ChatColor.GRAY + " = optional");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "..." + ChatColor.GRAY + " = multiple args support");
        player.sendMessage("");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds " + ChatColor.WHITE + "shows your status.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds admin " + ChatColor.WHITE + "opens the admin GUI.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds help " + ChatColor.WHITE + "displays the help page.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds give <item> (player) (amount) " + ChatColor.WHITE + "gives a player an item.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds itemlist " + ChatColor.WHITE + "displays the item list.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds reload " + ChatColor.WHITE + "reloads the config file.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds clean " + ChatColor.WHITE + "removes all negative effects (Bleeding...).");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sds start <event> " + ChatColor.WHITE + "starts an event.");
        playSound(player);
    }

    private void handleCleanCommand(CommandSender sender, String[] args) {
        Player target = args.length < 2 ? (sender instanceof Player ? (Player) sender : null) : Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Couldn't find player called " + args[1] + ".");
            if (sender instanceof Player) playSound((Player) sender);
            return;
        }

        PlayerData data = PlayerData.get(target);
        if (data.isInfected()) data.setInfected(false);
        if (data.isBleeding()) data.setBleeding(false);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ ChatColor.GREEN + "Removed bad status for " + target.getName() + ".");
        if (sender instanceof Player) playSound((Player) sender);
    }

    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ ChatColor.RED + "Please specify an event to start.");
            playSound(player);
            return;
        }

        Feature feature;
        try {
            feature = Feature.valueOf(args[1].toUpperCase().replace("-", "_"));
            Validate.isTrue(feature.isEvent());
        } catch (IllegalArgumentException exception) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Could not find event called " + args[1].toUpperCase().replace("-", "_") + ".");
            playSound(player);
            return;
        }

        player.getWorld().setTime(14000);
        plugin.getEventManager().applyStatus(player.getWorld(), feature.generateWorldEventHandler(player.getWorld()));
        String message = ChatColor.DARK_RED + "" + ChatColor.ITALIC + Utils.msg(feature.getPath());
        for (Player online : player.getWorld().getPlayers()) {
            online.sendMessage(message);
            online.sendTitle("", message, 10, 40, 10);
            online.playSound(online.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.0f);
            online.playSound(online.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0f, 0.0f);
        }
        playSound(player);
    }

    private void handleReloadCommand(CommandSender sender) {
        plugin.saveDefaultConfig();
        plugin.reloadConfigFiles();
        plugin.reloadConfig();
        for (Feature feature : Feature.values())
            feature.updateConfig();
        for (CustomItem item : CustomItem.values())
            item.update(Objects.requireNonNull(plugin.items.getConfig().getConfigurationSection(item.name())));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" " + plugin.getName() + " " + plugin.getDescription().getVersion() + " reloaded.");
        if (sender instanceof Player) playSound((Player) sender);
    }

    private void sendItemList(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------[" + ChatColor.LIGHT_PURPLE + " Sudden Death Items " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "]-----------------");
        player.sendMessage("");
        for (CustomItem item : CustomItem.values()) {
            player.spigot().sendMessage(new ComponentBuilder(item.getName())
                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                    .append(" (")
                    .color(net.md_5.bungee.api.ChatColor.GRAY)
                    .append(item.name())
                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sds give " + item.name()))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to obtain " + item.getName())))
                    .append(") ")
                    .color(net.md_5.bungee.api.ChatColor.GRAY)
                    .create());
        }
        playSound(player);
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Please specify an item to give.");
            if (sender instanceof Player) playSound((Player) sender);
            return;
        }

        ItemStack itemStack;
        try {
            itemStack = CustomItem.valueOf(args[1].replace("-", "_").toUpperCase()).a();
        } catch (Exception e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Couldn't find the item called " + args[1].replace("-", "_").toUpperCase() + ".");
            if (sender instanceof Player) playSound((Player) sender);
            return;
        }

        if (args.length > 3) {
            try {
                itemStack.setAmount((int) Double.parseDouble(args[3]));
            } catch (Exception e) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix")))+" "+ChatColor.RED + args[3] + " is not a valid number.");
                if (sender instanceof Player) playSound((Player) sender);
                return;
            }
        }

        Player target = args.length > 2 ? Bukkit.getPlayer(args[2]) : (sender instanceof Player ? (Player) sender : null);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.RED + "Couldn't find player called " + args[2] + ".");
            if (sender instanceof Player) playSound((Player) sender);
            return;
        }

        if (!Objects.equals(sender, target)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("prefix"))) +" "+ChatColor.YELLOW + Utils.msg("give-item")
                    .replace("#item#", Utils.displayName(itemStack))
                    .replace("#player#", target.getName())
                    .replace("#amount#", itemStack.getAmount() > 1 ? " x" + itemStack.getAmount() : ""));
        }
        target.sendMessage(ChatColor.YELLOW + Utils.msg("receive-item")
                .replace("#item#", Utils.displayName(itemStack))
                .replace("#amount#", itemStack.getAmount() > 1 ? " x" + itemStack.getAmount() : ""));

        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), itemStack);
        } else {
            target.getInventory().addItem(itemStack);
        }
        if (sender instanceof Player) playSound((Player) sender);
    }

    private void playSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }
}
