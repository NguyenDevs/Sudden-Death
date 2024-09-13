package org.nguyendevs.suddendeath.command;

import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.gui.MonsterEdition;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.MobStat;
import org.nguyendevs.suddendeath.util.Utils;
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

public class SuddenDeathMobCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("suddendeath.op")) {
            player.sendMessage(ChatColor.RED + Utils.msg("not-enough-perms"));
            playSound(player);
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            playSound(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "kill":
                handleKillCommand(player, args);
                break;
            case "edit":
                handleEditCommand(player, args);
                break;
            case "create":
                handleCreateCommand(player, args);
                break;
            case "remove":
            case "delete":
                handleDeleteCommand(player, args);
                break;
            case "list":
                handleListCommand(player, args);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown command. Use /sdmob for help.");
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
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob create <type> <name> " + ChatColor.WHITE + "creates a new monster.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob edit <type> <name> " + ChatColor.WHITE + "edits an existing mob.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob delete <type> <name> " + ChatColor.WHITE + "deletes an existing monster.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob list type " + ChatColor.WHITE + "lists all supported mob types.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob list <type> " + ChatColor.WHITE + "lists all mobs from one specific type.");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob kill <radius> " + ChatColor.WHITE + "kills every nearby custom mob.");
    }

    private void handleKillCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /sdmob kill <radius>");
            playSound(player);
            return;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + args[1] + " is not a valid number.");
            playSound(player);
            return;
        }

        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity.hasMetadata("SDCustomMob")) {
                count++;
                entity.remove();
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Successfully killed " + count + " custom mob" + (count > 1 ? "s" : "") + ".");
        playSound(player);
    }

    private void handleEditCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /sdmob edit <type> <mob-id>");
            playSound(player);
            return;
        }

        String typeFormat = args[1].toUpperCase().replace("-", "_");
        String id = args[2].toUpperCase().replace("-", "_");
        EntityType type;

        try {
            type = EntityType.valueOf(typeFormat);
            Validate.isTrue(type.isAlive());
        } catch (IllegalArgumentException exception) {
            player.sendMessage(ChatColor.RED + typeFormat + " is not a supported mob type.");
            player.sendMessage(ChatColor.RED + "Type /sdmob list to see all available mob types.");
            playSound(player);
            return;
        }

        ConfigFile mobs = new ConfigFile(type);
        if (!mobs.getConfig().contains(id)) {
            player.sendMessage(ChatColor.RED + "Couldn't find the mob called " + id + ".");
            playSound(player);
            return;
        }

        new MonsterEdition(player, type, id).open();
        playSound(player);
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /sdmob create <type> <mob-id>");
            playSound(player);
            return;
        }

        String typeFormat = args[1].toUpperCase().replace("-", "_");
        String id = args[2].toUpperCase().replace("-", "_");
        EntityType type;

        try {
            type = EntityType.valueOf(typeFormat);
            Validate.isTrue(type.isAlive());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + typeFormat + " is not a supported mob type.");
            player.sendMessage(ChatColor.RED + "Use /sdmob list type to see all available mob types.");
            playSound(player);
            return;
        }

        ConfigFile mobs = new ConfigFile(type);

        if (Utils.isIDType(id)) {
            player.sendMessage(ChatColor.RED + id + " is not a valid ID.");
            player.sendMessage(ChatColor.RED + "ID Format: USE_THIS_FORMAT");
            playSound(player);
            return;
        }

        if (id.equalsIgnoreCase("DEFAULT_KEY")) {
            player.sendMessage(ChatColor.RED + "This ID is forbidden.");
            playSound(player);
            return;
        }

        if (mobs.getConfig().contains(id)) {
            player.sendMessage(ChatColor.RED + "There is already a mob with ID " + id + ".");
            playSound(player);
            return;
        }

        for (MobStat mobStat : MobStat.values()) {
            mobs.getConfig().set(id + "." + mobStat.getPath(), mobStat.getDefaultValue());
        }

        mobs.save();
        player.sendMessage(ChatColor.YELLOW + "You successfully created a new mob: " + ChatColor.WHITE + id + ChatColor.YELLOW + "!");
        playSound(player);
        new MonsterEdition(player, type, id).open();
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /sdmob delete <type> <mob-id>");
            playSound(player);
            return;
        }

        String typeFormat = args[1].toUpperCase().replace("-", "_");
        String id = args[2].toUpperCase().replace("-", "_");
        EntityType type;

        try {
            type = EntityType.valueOf(typeFormat);
            Validate.isTrue(type.isAlive());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + typeFormat + " is not a supported mob type.");
            player.sendMessage(ChatColor.RED + "Use /sdmob list type to see all available mob types.");
            playSound(player);
            return;
        }

        ConfigFile mobs = new ConfigFile(type);

        if (Utils.isIDType(id)) {
            player.sendMessage(ChatColor.RED + id + " is not a valid ID.");
            player.sendMessage(ChatColor.RED + "ID Format: USE_THIS_FORMAT");
            playSound(player);
            return;
        }

        if (id.equalsIgnoreCase("DEFAULT_KEY")) {
            player.sendMessage(ChatColor.RED + "This ID is forbidden.");
            playSound(player);
            return;
        }

        if (!mobs.getConfig().contains(id)) {
            player.sendMessage(ChatColor.RED + "There is no mob called " + id + "!");
            playSound(player);
            return;
        }

        mobs.getConfig().set(id, null);
        mobs.save();
        player.sendMessage(ChatColor.YELLOW + "You successfully removed " + ChatColor.WHITE + id + ChatColor.YELLOW + "!");
        playSound(player);
    }

    private void handleListCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /sdmob list <mob-type/'type'>");
            playSound(player);
            return;
        }

        if (args[1].equalsIgnoreCase("type")) {
            player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------[" + ChatColor.LIGHT_PURPLE + " Available Mob Types " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "]----------------");
            player.sendMessage("");
            for (EntityType mobType : EntityType.values()) {
                if (mobType.isAlive()) {
                    player.spigot().sendMessage(new ComponentBuilder(mobType.name())
                            .color(net.md_5.bungee.api.ChatColor.WHITE)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sdmob list " + mobType.name()))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to see all mobs in " + mobType.name())))
                            .create());
                }
            }
            playSound(player);
            return;
        }

        String typeFormat = args[1].toUpperCase().replace("-", "_");
        EntityType type;

        try {
            type = EntityType.valueOf(typeFormat);
            Validate.isTrue(type.isAlive());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + typeFormat + " is not a supported custom mob type.");
            player.sendMessage(ChatColor.RED + "Use /sdmob list type to see all available mob types.");
            playSound(player);
            return;
        }

        FileConfiguration mobs = new ConfigFile(type).getConfig();
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------------[" + ChatColor.LIGHT_PURPLE + " Mob List " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "]---------------------");
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "From " + type.name());
        player.sendMessage("");

        for (String mobId : mobs.getKeys(false)) {
            String mobName = mobs.getString(mobId + ".name");
            player.spigot().sendMessage(new ComponentBuilder(mobName)
                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                    .append(" (")
                    .color(net.md_5.bungee.api.ChatColor.GRAY)
                    .append(mobId)
                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sdmob edit " + typeFormat + " " + mobId))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to edit " + mobId)))
                    .append(") ")
                    .create());
        }

        playSound(player);
    }

    private void playSound(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1, 1);
        }
    }
}
