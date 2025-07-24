package org.nguyendevs.suddendeath.command;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.gui.MonsterEdition;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.MobStat;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.logging.Level;

/**
 * Command executor for the SuddenDeath plugin's /sdmob command.
 * Handles mob creation, editing, deletion, listing, and killing.
 */
public class SuddenDeathMobCommand implements CommandExecutor {
    private static final String PERMISSION_OP = "suddendeath.op";
    private static final String STRIKETHROUGH = "&8&m---------------";
    private static final String HELP_HEADER = STRIKETHROUGH + "[&d Sudden Death Help Page &8&m]---------------";
    private static final String MOB_LIST_HEADER = STRIKETHROUGH + "[&d Mob List &8&m]---------------------";
    private static final String MOB_TYPES_HEADER = STRIKETHROUGH + "[&d Available Mob Types &8&m]----------------";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cOnly players can use this command."));
            return true;
        }

        if (!player.hasPermission(PERMISSION_OP)) {
            player.sendMessage(translateColors(Utils.msg("prefix") + " " + getMessage("not-enough-perms")));
            playSound(player);
            return true;
        }

        try {
            if (args.length == 0) {
                sendHelpMessage(player);
                playSound(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "kill" -> handleKillCommand(player, args);
                case "edit" -> handleEditCommand(player, args);
                case "create" -> handleCreateCommand(player, args);
                case "remove", "delete" -> handleDeleteCommand(player, args);
                case "list" -> handleListCommand(player, args);
                default -> {
                    player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cUnknown command. Use /sdmob for help."));
                    playSound(player);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error processing command /sdmob for player: " + player.getName(), e);
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while processing your command."));
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
        player.sendMessage(translateColors("&d/sdmob create <type> <name> &fcreates a new monster."));
        player.sendMessage(translateColors("&d/sdmob edit <type> <name> &fedits an existing mob."));
        player.sendMessage(translateColors("&d/sdmob delete <type> <name> &fdeletes an existing monster."));
        player.sendMessage(translateColors("&d/sdmob list type &flists all supported mob types."));
        player.sendMessage(translateColors("&d/sdmob list <type> &flists all mobs from one specific type."));
        player.sendMessage(translateColors("&d/sdmob kill <radius> &fkills every nearby custom mob."));
        playSound(player);
    }

    /**
     * Handles the kill command to remove nearby custom mobs within a specified radius.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleKillCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cUsage: /sdmob kill <radius>"));
            playSound(player);
            return;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[1]);
            if (radius <= 0) {
                player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&c" + args[1] + " is not a valid positive number."));
                playSound(player);
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&c" + args[1] + " is not a valid number."));
            playSound(player);
            return;
        }

        int count = 0;
        try {
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (entity.hasMetadata("SDCustomMob")) {
                    entity.remove();
                    count++;
                }
            }
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&eSuccessfully killed " + count + " custom mob" + (count != 1 ? "s" : "") + "."));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error killing custom mobs for player: " + player.getName(), e);
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while killing custom mobs."));
        }
        playSound(player);
    }

    /**
     * Handles the edit command to open the mob editor GUI for an existing mob.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleEditCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cUsage: /sdmob edit <type> <mob-id>"));
            playSound(player);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null) {
            return;
        }

        String id = args[2].toUpperCase().replace("-", "_");
        ConfigFile mobs = new ConfigFile(type);
        if (!mobs.getConfig().contains(id)) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cCouldn't find the mob called " + id + "."));
            playSound(player);
            return;
        }

        new MonsterEdition(player, type, id).open();
        playSound(player);
    }

    /**
     * Handles the create command to create a new mob configuration.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cUsage: /sdmob create <type> <mob-id>"));
            playSound(player);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null) {
            return;
        }

        String id = args[2].toUpperCase().replace("-", "_");
        ConfigFile mobs = new ConfigFile(type);

        if (!isValidId(player, id, mobs)) {
            return;
        }

        try {
            for (MobStat mobStat : MobStat.values()) {
                mobs.getConfig().set(id + "." + mobStat.getPath(), mobStat.getDefaultValue());
            }
            mobs.save();
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&eYou successfully created a new mob: &f" + id + "&e!"));
            new MonsterEdition(player, type, id).open();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating mob " + id + " for player: " + player.getName(), e);
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while creating the mob."));
        }
        playSound(player);
    }

    /**
     * Handles the delete command to remove a mob configuration.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cUsage: /sdmob delete <type> <mob-id>"));
            playSound(player);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null) {
            return;
        }

        String id = args[2].toUpperCase().replace("-", "_");
        ConfigFile mobs = new ConfigFile(type);

        if (!isValidId(player, id, mobs)) {
            return;
        }

        if (!mobs.getConfig().contains(id)) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cThere is no mob called " + id + "!"));
            playSound(player);
            return;
        }

        try {
            mobs.getConfig().set(id, null);
            mobs.save();
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&eYou successfully removed &f" + id + "&e!"));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error deleting mob " + id + " for player: " + player.getName(), e);
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while deleting the mob."));
        }
        playSound(player);
    }

    /**
     * Handles the list command to display mob types or mobs of a specific type.
     *
     * @param player The player executing the command.
     * @param args   The command arguments.
     */
    private void handleListCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cUsage: /sdmob list <mob-type/'type'>"));
            playSound(player);
            return;
        }

        if (args[1].equalsIgnoreCase("type")) {
            listMobTypes(player);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null) {
            return;
        }

        FileConfiguration mobs = new ConfigFile(type).getConfig();
        player.sendMessage(translateColors(MOB_LIST_HEADER));
        player.sendMessage(translateColors("&8&oFrom " + type.name()));
        player.sendMessage("");

        try {
            if (mobs.getKeys(false).isEmpty()) {
                player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&eNo custom mobs found for " + type.name() + "."));
            } else {
                for (String mobId : mobs.getKeys(false)) {
                    String mobName = mobs.getString(mobId + ".name", mobId);
                    player.spigot().sendMessage(new ComponentBuilder(mobName)
                            .color(net.md_5.bungee.api.ChatColor.WHITE)
                            .append(" (")
                            .color(net.md_5.bungee.api.ChatColor.GRAY)
                            .append(mobId)
                            .color(net.md_5.bungee.api.ChatColor.WHITE)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sdmob edit " + type.name() + " " + mobId))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(translateColors("&fClick to edit " + mobId))))
                            .append(") ")
                            .create());
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error listing mobs for type " + type.name() + " for player: " + player.getName(), e);
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while listing mobs."));
        }
        playSound(player);
    }

    /**
     * Lists all supported mob types with clickable commands.
     *
     * @param player The player to send the list to.
     */
    private void listMobTypes(Player player) {
        player.sendMessage(translateColors(MOB_TYPES_HEADER));
        player.sendMessage("");
        try {
            for (EntityType mobType : EntityType.values()) {
                if (mobType.isAlive()) {
                    player.spigot().sendMessage(new ComponentBuilder(mobType.name())
                            .color(net.md_5.bungee.api.ChatColor.WHITE)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sdmob list " + mobType.name()))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(translateColors("&fClick to see all mobs in " + mobType.name()))))
                            .create());
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error listing mob types for player: " + player.getName(), e);
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cAn error occurred while listing mob types."));
        }
        playSound(player);
    }

    /**
     * Parses the entity type from a string and validates it.
     *
     * @param player The player executing the command.
     * @param typeStr The entity type string.
     * @return The parsed EntityType, or null if invalid.
     */
    private EntityType parseEntityType(Player player, String typeStr) {
        try {
            EntityType type = EntityType.valueOf(typeStr.toUpperCase().replace("-", "_"));
            Validate.isTrue(type.isAlive(), typeStr + " is not a supported mob type.");
            return type;
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&c" + typeStr.toUpperCase().replace("-", "_") + " is not a supported mob type."));
            player.sendMessage(translateColors(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " + "&cUse /sdmob list type to see all available mob types."));
            playSound(player);
            return null;
        }
    }

    /**
     * Validates the mob ID for create or delete commands.
     *
     * @param player The player executing the command.
     * @param id     The mob ID to validate.
     * @param mobs   The configuration file for the mob type.
     * @return true if the ID is valid, false otherwise.
     */
    private boolean isValidId(Player player, String id, ConfigFile mobs) {
        if (Utils.isIDType(id)) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&c" + id + " is not a valid ID."));
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cID Format: USE_THIS_FORMAT"));
            playSound(player);
            return false;
        }

        if (id.equalsIgnoreCase("DEFAULT_KEY")) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cThis ID is forbidden."));
            playSound(player);
            return false;
        }

        if (mobs.getConfig().contains(id)) {
            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED+ "Sudden" + ChatColor.DARK_RED + "Death" + ChatColor.GOLD+ "] " +translateColors("&cThere is already a mob with ID " + id + "."));
            playSound(player);
            return false;
        }

        return true;
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
            return Utils.msg(key);
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