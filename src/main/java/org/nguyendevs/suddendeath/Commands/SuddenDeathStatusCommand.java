package org.nguyendevs.suddendeath.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.GUI.AdminView;
import org.nguyendevs.suddendeath.GUI.CrafterInventory;
import org.nguyendevs.suddendeath.GUI.PlayerView;
import org.nguyendevs.suddendeath.GUI.Status;
import org.nguyendevs.suddendeath.Managers.EventManager;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.CustomItem;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class SuddenDeathStatusCommand implements CommandExecutor {

    private static final String PERMISSION_ADMIN = "suddendeath.admin";
    private static final String PERMISSION_RECIPE = "suddendeath.recipe";
    private static final String PERMISSION_VIEW = "suddendeath.status";

    private static final String DIVIDER = "&8&m---------------[&r";
    private static final String HEADER_HELP = DIVIDER + "&d Sudden Death Help Page &8&m]---------------";
    private static final String HEADER_ITEM_LIST = DIVIDER + "&d Sudden Death Items &8&m]-----------------";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        try {
            if (args.length == 0) {
                if (!requirePermission(sender, PERMISSION_ADMIN))
                    return true;
                if (requirePlayer(sender))
                    sendHelpMessage(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    sendHelpMessage(sender);
                }
                case "status" -> {
                    if (!requirePermission(sender, PERMISSION_VIEW))
                        return true;
                    if (!requirePlayer(sender))
                        return true;
                    new Status((Player) sender).open();
                    sound((Player) sender, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
                }
                case "menu" -> {
                    if (!requirePermission(sender, PERMISSION_VIEW))
                        return true;
                    if (!requirePlayer(sender))
                        return true;
                    new PlayerView((Player) sender).open();
                    sound((Player) sender, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
                }
                case "recipe" -> {
                    if (!requirePermission(sender, PERMISSION_RECIPE))
                        return true;
                    if (!requirePlayer(sender))
                        return true;
                    new CrafterInventory((Player) sender);
                    sound((Player) sender, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
                }
                case "admin" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    if (!requirePlayer(sender))
                        return true;
                    new AdminView((Player) sender).open();
                    sound((Player) sender, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
                }
                case "clean" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    handleClean(sender, args);
                }
                case "start" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    if (requirePlayer(sender))
                        handleStart((Player) sender, args);
                }
                case "stop" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    if (requirePlayer(sender))
                        handleStop((Player) sender, args);
                }
                case "reload" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    handleReload(sender);
                }
                case "itemlist" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    sendItemList(sender);
                }
                case "give" -> {
                    if (!requirePermission(sender, PERMISSION_ADMIN))
                        return true;
                    handleGive(sender, args);
                }
                default -> {
                    msg(sender, Utils.msg("prefix") + " " + "&cUnknown command. Use /sds help for a list of commands.");
                    if (sender instanceof Player p)
                        sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error processing /sds for: " + sender.getName(), e);
            msg(sender, Utils.msg("prefix") + " " + "&cAn error occurred while processing your command.");
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(color(HEADER_HELP));
        sender.sendMessage(color("&d<> &7= required"));
        sender.sendMessage(color("&d() &7= optional"));
        sender.sendMessage(color("&d... &7= multiple args support"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(color("&d/sds &fshows your status."));
        if (sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(color("&d/sds admin &fopens the admin GUI."));
            sender.sendMessage(color("&d/sds clean &fremoves all negative effects (Bleeding...)."));
            sender.sendMessage(color("&d/sds give <item> (player) (amount) &fgives a player an item."));
            sender.sendMessage(color("&d/sds help &fdisplays the help page."));
            sender.sendMessage(color("&d/sds itemlist &fdisplays the item list."));
            sender.sendMessage(color("&d/sds reload &freloads the config file."));
            sender.sendMessage(color("&d/sds start <event> &fstarts an event."));
            sender.sendMessage(color("&d/sds stop <event> &fstops an event."));
        }
        if (sender.hasPermission(PERMISSION_VIEW)) {
            sender.sendMessage(color("&d/sds menu &fopens the feature view GUI."));
            sender.sendMessage(color("&d/sds status &fshows your status."));
        }
        if (sender.hasPermission(PERMISSION_RECIPE)) {
            sender.sendMessage(color("&d/sds recipe &fopens the crafting recipe GUI."));
        }
        if (sender instanceof Player p)
            sound(p, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void handleClean(CommandSender sender, String[] args) {
        Player target;
        if (args.length < 2) {
            if (!(sender instanceof Player p)) {
                msg(sender, Utils.msg("prefix") + " " + "&cPlease specify a player to clean.");
                return;
            }
            target = p;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                msg(sender, Utils.msg("prefix") + " " + "&cCouldn't find player called " + args[1] + ".");
                if (sender instanceof Player p)
                    sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
                return;
            }
        }

        PlayerData data = PlayerData.get(target);
        if (data.isInfected())
            data.setInfected(false);
        if (data.isBleeding())
            data.setBleeding(false);

        msg(sender, Utils.msg("prefix") + " " + "&aRemoved bad status for " + target.getName() + ".");
        if (sender instanceof Player p)
            sound(p, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void handleStart(Player player, String[] args) {
        if (args.length < 2) {
            msg(player, Utils.msg("prefix") + " " + "&cPlease specify an event to start.");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        String featureKey = args[1].toUpperCase().replace("-", "_");
        try {
            Feature feature = Feature.valueOf(featureKey);
            Validate.isTrue(feature.isEvent(), "Specified feature is not an event.");

            player.getWorld().setTime(14000);
            SuddenDeath.getInstance().getEventManager()
                    .applyStatus(player.getWorld(), feature.generateWorldEventHandler(player.getWorld()));

            String message = Utils.msg(feature.getPath());
            Sound startSound = switch (feature) {
                case BLOOD_MOON -> Sound.ENTITY_ZOMBIE_INFECT;
                case THUNDERSTORM -> Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
                default -> Sound.BLOCK_RESPAWN_ANCHOR_CHARGE;
            };

            for (Player online : player.getWorld().getPlayers()) {
                online.sendMessage(color(Utils.msg("prefix") + " " + message));
                online.showTitle(net.kyori.adventure.title.Title.title(
                        Component.empty(),
                        color(message),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(500),
                                java.time.Duration.ofSeconds(2),
                                java.time.Duration.ofMillis(500))));
                online.playSound(online.getLocation(), startSound, 1.0f, 0.1f);
            }
        } catch (IllegalArgumentException e) {
            msg(player, Utils.msg("prefix") + " " + "&cCould not find event called " + featureKey + ".");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
        }
    }

    private void handleStop(Player player, String[] args) {
        if (args.length < 2) {
            msg(player, Utils.msg("prefix") + " " + "&cPlease specify an event to stop.");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        String featureKey = args[1].toUpperCase().replace("-", "_");
        try {
            Feature feature = Feature.valueOf(featureKey);
            Validate.isTrue(feature.isEvent(), "Specified feature is not an event.");

            player.getWorld().setTime(6000);
            player.getWorld().setStorm(false);
            player.getWorld().setThundering(false);
            SuddenDeath.getInstance().getEventManager().applyStatus(player.getWorld(), EventManager.WorldStatus.DAY);

            String displayName = Utils.caseOnWords(args[1].toLowerCase().replace("-", " "));
            msg(player, Utils.msg("prefix") + " " + "&aSuccessfully stopped event " + displayName + ".");
        } catch (IllegalArgumentException e) {
            msg(player, Utils.msg("prefix") + " " + "&cCould not find event called " + featureKey + ".");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
        }
    }

    private void handleReload(CommandSender sender) {
        try {
            SuddenDeath plugin = SuddenDeath.getInstance();
            plugin.reloadConfigFiles();
            for (Feature feature : Feature.values())
                feature.updateConfig();
            msg(sender, Utils.msg("prefix") + " " + "&e" + plugin.getName() + " " + plugin.getDescription().getVersion()
                    + " reloaded.");
            if (sender instanceof Player p)
                sound(p, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error reloading plugin for: " + sender.getName(), e);
            msg(sender, Utils.msg("prefix") + " " + "&cAn error occurred while reloading the plugin.");
        }
    }

    private void sendItemList(CommandSender sender) {
        sender.sendMessage(color(HEADER_ITEM_LIST));
        sender.sendMessage(Component.empty());
        for (CustomItem item : CustomItem.values()) {
            if (sender instanceof Player player) {
                Component entry = color(item.getName())
                        .append(Component.text(" (", NamedTextColor.GRAY))
                        .append(Component.text(item.name(), NamedTextColor.WHITE)
                                .clickEvent(ClickEvent.runCommand("/sds give " + item.name()))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text("Click to obtain ").color(NamedTextColor.GRAY)
                                                .append(color(item.getName())))))
                        .append(Component.text(")", NamedTextColor.GRAY));
                player.sendMessage(entry);
            } else {
                sender.sendMessage(color(item.getName() + " &7(" + item.name() + ")"));
            }
        }
        if (sender instanceof Player p)
            sound(p, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, Utils.msg("prefix") + " " + "&cPlease specify an item to give.");
            if (sender instanceof Player p)
                sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        String itemKey = args[1].toUpperCase().replace("-", "_");
        ItemStack itemStack;
        try {
            itemStack = CustomItem.valueOf(itemKey).a();
        } catch (IllegalArgumentException e) {
            msg(sender, Utils.msg("prefix") + " " + "&cCouldn't find the item called " + itemKey + ".");
            if (sender instanceof Player p)
                sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        if (args.length > 3) {
            try {
                int amount = Integer.parseInt(args[3]);
                if (amount <= 0)
                    throw new NumberFormatException();
                itemStack.setAmount(amount);
            } catch (NumberFormatException e) {
                msg(sender, Utils.msg("prefix") + " " + "&c" + args[3] + " is not a valid positive number.");
                if (sender instanceof Player p)
                    sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
                return;
            }
        }

        Player target;
        if (args.length > 2) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                msg(sender, Utils.msg("prefix") + " " + "&cCouldn't find player called " + args[2] + ".");
                if (sender instanceof Player p)
                    sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            msg(sender, Utils.msg("prefix") + " " + "&cPlease specify a player to give the item to.");
            return;
        }

        String displayName = Utils.displayName(itemStack);
        String amountStr = itemStack.getAmount() > 1 ? " x" + itemStack.getAmount() : "";

        if (sender != target) {
            msg(sender, Utils.msg("prefix") + " " + Utils.msg("give-item")
                    .replace("#item#", displayName)
                    .replace("#player#", target.getName())
                    .replace("#amount#", amountStr));
        }
        msg(target, Utils.msg("prefix") + " " + Utils.msg("receive-item")
                .replace("#item#", displayName)
                .replace("#amount#", amountStr));

        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), itemStack);
        } else {
            target.getInventory().addItem(itemStack);
        }
        if (sender instanceof Player p)
            sound(p, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission))
            return true;
        msg(sender, Utils.msg("prefix") + " " + Utils.msg("not-enough-perms"));
        if (sender instanceof Player p)
            sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
        return false;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player)
            return true;
        msg(sender, Utils.msg("prefix") + " " + "&cThis command is only available for players.");
        return false;
    }

    private void msg(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    private void sound(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 1.0f, pitch);
    }

    private Component color(String message) {
        return Utils.color(message);
    }
}