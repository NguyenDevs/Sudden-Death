package org.nguyendevs.suddendeath.command;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.gui.AdminView;
import org.nguyendevs.suddendeath.gui.Status;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.logging.Level;

/**
 * Command executor for the SuddenDeath plugin's /sds command.
 * Handles status viewing, admin GUI, item management, and more.
 */
public class SuddenDeathStatusCommand implements CommandExecutor {
    private static final String PERMISSION_STATUS = "suddendeath.status";
    private static final String PERMISSION_ADMIN_RECIPE = "suddendeath.admin.recipe";
    private static final String STRIKETHROUGH = "&8&m---------------";
    private static final String HELP_HEADER = STRIKETHROUGH + "[&d Sudden Death Help Page &8&m]---------------";
    private static final String ITEM_LIST_HEADER = STRIKETHROUGH + "[&d Sudden Death Items &8&m]-----------------";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(translateColors("&cOnly players can use this command."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("recipe")) {
            if (args.length < 2) {
                sender.sendMessage(translateColors("&cUsage: /sds recipe <unlock|lock|reload> [player] [item]"));
                return true;
            }

            String action = args[1].toLowerCase();

            switch (action) {
                case "unlock":
                    // Cho phép người chơi tự mở khóa công thức nếu có quyền suddendeath.recipe.*
                    if (args.length == 2 && player.hasPermission("suddendeath.recipe.*")) {
                        SuddenDeath.getInstance().unlockRecipesForPlayer(player);
                        sender.sendMessage(translateColors("&aUnlocked all recipes for yourself."));
                        playSound(player);
                        return true;
                    }

                    // Kiểm tra quyền admin để mở khóa công thức cho người khác
                    if (!sender.hasPermission(PERMISSION_ADMIN_RECIPE)) {
                        sender.sendMessage(translateColors("&cYou don't have permission to manage recipes."));
                        return true;
                    }

                    if (args.length >= 3) {
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            sender.sendMessage(translateColors("&cPlayer not found!"));
                            return true;
                        }

                        if (args.length >= 4) {
                            // Unlock specific item recipe
                            try {
                                CustomItem item = CustomItem.valueOf(args[3].toUpperCase());
                                if (!target.hasPermission("suddendeath.recipe." + item.name().toLowerCase()) &&
                                        !target.hasPermission("suddendeath.recipe.*")) {
                                    sender.sendMessage(translateColors("&c" + target.getName() + " does not have permission for " + item.name() + " recipe."));
                                    return true;
                                }
                                NamespacedKey key = new NamespacedKey(SuddenDeath.getInstance(),
                                        "suddendeath_" + item.name().toLowerCase());
                                target.discoverRecipe(key);
                                sender.sendMessage(translateColors("&aUnlocked " + item.name() + " recipe for " + target.getName()));
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(translateColors("&cInvalid item name!"));
                            }
                        } else {
                            // Unlock all recipes
                            SuddenDeath.getInstance().unlockRecipesForPlayer(target);
                            sender.sendMessage(translateColors("&aUnlocked all recipes for " + target.getName()));
                        }
                    } else {
                        sender.sendMessage(translateColors("&cUsage: /sds recipe unlock [player] [item]"));
                    }
                    break;

                case "lock":
                    if (!sender.hasPermission(PERMISSION_ADMIN_RECIPE)) {
                        sender.sendMessage(translateColors("&cYou don't have permission to manage recipes."));
                        return true;
                    }

                    if (args.length >= 4) {
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            sender.sendMessage(translateColors("&cPlayer not found!"));
                            return true;
                        }

                        try {
                            CustomItem item = CustomItem.valueOf(args[3].toUpperCase());
                            SuddenDeath.getInstance().revokeRecipeFromPlayer(target, item);
                            sender.sendMessage(translateColors("&aLocked " + item.name() + " recipe for " + target.getName()));
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(translateColors("&cInvalid item name!"));
                        }
                    } else {
                        sender.sendMessage(translateColors("&cUsage: /sds recipe lock <player> <item>"));
                    }
                    break;

                case "reload":
                    if (!sender.hasPermission(PERMISSION_ADMIN_RECIPE)) {
                        sender.sendMessage(translateColors("&cYou don't have permission to manage recipes."));
                        return true;
                    }

                    try {
                        SuddenDeath.getInstance().reloadConfigFiles();
                        sender.sendMessage(translateColors("&aRecipes reloaded and updated in Recipe Book!"));
                    } catch (Exception e) {
                        sender.sendMessage(translateColors("&cError reloading recipes: " + e.getMessage()));
                    }
                    break;

                default:
                    sender.sendMessage(translateColors("&cUsage: /sds recipe <unlock|lock|reload> [player] [item]"));
                    break;
            }
            playSound(player);
            return true;
        }

        if (!player.hasPermission(PERMISSION_STATUS)) {
            player.sendMessage(translateColors("&c" + getMessage("not-enough-perms")));
            playSound(player);
            return true;
        }

        try {
            if (args.length == 0) {
                new Status(player).open();
                playSound(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help" -> sendHelpMessage(player);
                case "clean" -> handleCleanCommand(player, args);
                case "start" -> handleStartCommand(player, args);
                case "admin" -> {
                    new AdminView(player).open();
                    playSound(player);
                }
                case "reload" -> handleReloadCommand(player);
                case "itemlist" -> sendItemList(player);
                case "give" -> handleGiveCommand(player, args);
                default -> {
                    player.sendMessage(translateColors("&cUnknown command. Use /sds help for a list of commands."));
                    playSound(player);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error processing command /sds for player: " + player.getName(), e);
            player.sendMessage(translateColors("&cAn error occurred while processing your command."));
        }
        return true;
    }

    /**
     * Sends the help message with available commands to the player.
     *
     * @param player The player to send the message to.
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(translateColors(HELP_HEADER));
        player.sendMessage(translateColors("&d<> &7= required"));
        player.sendMessage(translateColors("&d() &7= optional"));
        player.sendMessage(translateColors("&d... &7= multiple args support"));
        player.sendMessage("");
        player.sendMessage(translateColors("&d/sds &fshows your status."));
        player.sendMessage(translateColors("&d/sds admin &fopens the admin GUI."));
        player.sendMessage(translateColors("&d/sds help &fdisplays the help page."));
        player.sendMessage(translateColors("&d/sds give <item> (player) (amount) &fgives a player an item."));
        player.sendMessage(translateColors("&d/sds itemlist &fdisplays the item list."));
        player.sendMessage(translateColors("&d/sds reload &freloads the config file."));
        player.sendMessage(translateColors("&d/sds clean &fremoves all negative effects (Bleeding...)."));
        player.sendMessage(translateColors("&d/sds start <event> &fstarts an event."));
        player.sendMessage(translateColors("&d/sds recipe unlock (player) (item) &funlocks all or specific recipes."));
        player.sendMessage(translateColors("&d/sds recipe lock <player> <item> &flocks a specific recipe."));
        player.sendMessage(translateColors("&d/sds recipe reload &freloads all recipes."));
        playSound(player);
    }

    /**
     * Handles the clean command to remove negative effects from a player.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleCleanCommand(Player player, String[] args) {
        Player target = args.length < 2 ? player : Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(translateColors("&cCouldn't find player called " + args[1] + "."));
            playSound(player);
            return;
        }

        PlayerData data = PlayerData.get(target);
        if (data.isInfected()) {
            data.setInfected(false);
        }
        if (data.isBleeding()) {
            data.setBleeding(false);
        }
        player.sendMessage(translateColors("&aRemoved bad status for " + target.getName() + "."));
        playSound(player);
    }

    /**
     * Handles the start command to initiate an event in the player's world.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(translateColors("&cPlease specify an event to start."));
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
            player.sendMessage(translateColors("&cCould not find event called " + args[1].toUpperCase().replace("-", "_") + "."));
            playSound(player);
        }
    }

    /**
     * Handles the reload command to reload plugin configurations.
     *
     * @param player The player executing the command.
     */
    private void handleReloadCommand(Player player) {
        try {
            SuddenDeath plugin = SuddenDeath.getInstance();
            plugin.reloadConfigFiles();
            for (Feature feature : Feature.values()) {
                feature.updateConfig();
            }
            player.sendMessage(translateColors("&e" + plugin.getName() + " " + plugin.getDescription().getVersion() + " reloaded."));
            playSound(player);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error reloading plugin for player: " + player.getName(), e);
            player.sendMessage(translateColors("&cAn error occurred while reloading the plugin."));
        }
    }

    /**
     * Sends the list of custom items to the player with clickable commands.
     *
     * @param player The player to send the list to.
     */
    private void sendItemList(Player player) {
        player.sendMessage(translateColors(ITEM_LIST_HEADER));
        player.sendMessage("");
        for (CustomItem item : CustomItem.values()) {
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
        }
        playSound(player);
    }

    /**
     * Handles the give command to provide a custom item to a player.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleGiveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(translateColors("&cPlease specify an item to give."));
            playSound(player);
            return;
        }

        ItemStack itemStack;
        try {
            itemStack = CustomItem.valueOf(args[1].toUpperCase().replace("-", "_")).a();
        } catch (IllegalArgumentException e) {
            player.sendMessage(translateColors("&cCouldn't find the item called " + args[1].toUpperCase().replace("-", "_") + "."));
            playSound(player);
            return;
        }

        if (args.length > 3) {
            try {
                int amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    player.sendMessage(translateColors("&c" + args[3] + " is not a valid positive number."));
                    playSound(player);
                    return;
                }
                itemStack.setAmount(amount);
            } catch (NumberFormatException e) {
                player.sendMessage(translateColors("&c" + args[3] + " is not a valid number."));
                playSound(player);
                return;
            }
        }

        Player target = args.length > 2 ? Bukkit.getPlayer(args[2]) : player;
        if (target == null) {
            player.sendMessage(translateColors("&cCouldn't find player called " + args[2] + "."));
            playSound(player);
            return;
        }

        String displayName = Utils.displayName(itemStack);
        String amountStr = itemStack.getAmount() > 1 ? " x" + itemStack.getAmount() : "";
        if (player != target) {
            player.sendMessage(translateColors("&e" + getMessage("give-item")
                    .replace("#item#", displayName)
                    .replace("#player#", target.getName())
                    .replace("#amount#", amountStr)));
        }
        target.sendMessage(translateColors("&e" + getMessage("receive-item")
                .replace("#item#", displayName)
                .replace("#amount#", amountStr)));

        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), itemStack);
        } else {
            target.getInventory().addItem(itemStack);
        }
        playSound(player);
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