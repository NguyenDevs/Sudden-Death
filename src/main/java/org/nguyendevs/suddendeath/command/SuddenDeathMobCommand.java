package org.nguyendevs.suddendeath.command;

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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
        if (!sender.hasPermission("suddendeath.op")) {
            sender.sendMessage(ChatColor.RED + Utils.msg("not-enough-perms"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "---------------[" + ChatColor.LIGHT_PURPLE + " Sudden Death Help Page " + ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "]---------------");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "<>" + ChatColor.GRAY + " = required");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "()" + ChatColor.GRAY + " = optional");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "..." + ChatColor.GRAY + " = multiple args support");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob create <type> <name> " + ChatColor.WHITE + "creates a new monster.");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob edit <type> <name> " + ChatColor.WHITE + "edits an existing mob.");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob delete <type> <name> " + ChatColor.WHITE + "deletes an existing monster.");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob list type " + ChatColor.WHITE + "lists all supported mob types.");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob list <type> " + ChatColor.WHITE + "lists all mobs from one specific type.");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/sdmob kill <radius> " + ChatColor.WHITE + "kills every nearby custom mob.");
            playSound((Player) sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("kill")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (!(sender instanceof Player))
                return true;

            Player p = (Player) sender;
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /sdmob kill <radius>");
                playSound(p);
                return true;
            }

            // radius
            double r = 0;
            try {
                r = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + args[1] + " is not a valid number.");
            }

            // kill
            int n = 0;
            for (Entity t : p.getNearbyEntities(r, r, r))
                if (t.hasMetadata("SDCustomMob")) {
                    n++;
                    t.remove();
                }
            p.sendMessage(ChatColor.YELLOW + "Successfully killed " + n + " custom mob" + (n > 1 ? "s" : "") + ".");
            playSound(p);
        }
        if (args[0].equalsIgnoreCase("edit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (!(sender instanceof Player))
                return true;

            Player player = (Player) sender;
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /sdmob edit <type> <mob-id>");
                playSound(player);
                return true;
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
                return true;
            }

            ConfigFile mobs = new ConfigFile(type);
            if (!mobs.getConfig().contains(id)) {
                player.sendMessage(ChatColor.RED + "Couldn't find the mob called " + id + ".");
                playSound(player);
                return true;
            }
            new MonsterEdition(player, type, id).open();
            playSound(player);
        }
        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /sdmob create <type> <mob-id>");
                playSound((Player) sender);
                return true;
            }
            String typeFormat = args[1].toUpperCase().replace("-", "_");
            String id = args[2].toUpperCase().replace("-", "_");
            EntityType type;
            try {
                type = EntityType.valueOf(typeFormat);
                Validate.isTrue(type.isAlive());
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + typeFormat + " is not a supported mob type.");
                sender.sendMessage(ChatColor.RED + "Use /sdmob list type to see all available mob types.");
                playSound((Player) sender);
                return true;
            }

            ConfigFile mobs = new ConfigFile(type);

            if (!Utils.isIDType(id)) {
                sender.sendMessage(ChatColor.RED + id + " is not a valid ID.");
                sender.sendMessage(ChatColor.RED + "ID Format: USE_THIS_FORMAT");
                playSound((Player) sender);
                return true;
            }
            if (id.equalsIgnoreCase("DEFAULT_KEY")) {
                sender.sendMessage(ChatColor.RED + "This ID is forbidden.");
                playSound((Player) sender);
                return true;
            }

            if (mobs.getConfig().contains(id)) {
                sender.sendMessage(ChatColor.RED + "There is already a mob which ID is " + id + ".");
                playSound((Player) sender);
                return true;
            }

            for (MobStat mb : MobStat.values())
                mobs.getConfig().set(id + "." + mb.getPath(), mb.getDefaultValue());

            mobs.save();
            sender.sendMessage(ChatColor.YELLOW + "You successfully created a new mob: " + ChatColor.WHITE + id + ChatColor.YELLOW + "!");
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2);
                new MonsterEdition(p, type, id).open();
            }
            playSound((Player) sender);
        }
        if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /sdmob delete <type> <mob-id>");
                playSound((Player) sender);
                return true;
            }
            String typeFormat = args[1].toUpperCase().replace("-", "_");
            String id = args[2].toUpperCase().replace("-", "_");
            EntityType type;
            try {
                type = EntityType.valueOf(typeFormat);
                Validate.isTrue(type.isAlive());
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + typeFormat + " is not a supported mob type.");
                sender.sendMessage(ChatColor.RED + "Use /sdmob list type to see all available mob types.");
                playSound((Player) sender);
                return true;
            }

            ConfigFile mobs = new ConfigFile(type);

            if (!Utils.isIDType(id)) {
                sender.sendMessage(ChatColor.RED + id + " is not a valid ID.");
                sender.sendMessage(ChatColor.RED + "ID Format: USE_THIS_FORMAT");
                playSound((Player) sender);
                return true;
            }
            if (id.equalsIgnoreCase("DEFAULT_KEY")) {
                sender.sendMessage(ChatColor.RED + "This ID is forbidden.");
                playSound((Player) sender);
                return true;
            }

            if (!mobs.getConfig().contains(id)) {
                sender.sendMessage(ChatColor.RED + "There is no mob called " + id + "!");
                playSound((Player) sender);
                return true;
            }

            mobs.getConfig().set(id, null);
            mobs.save();
            sender.sendMessage(ChatColor.YELLOW + "You successfully removed " + ChatColor.WHITE + id + ChatColor.YELLOW + "!");
            if (sender instanceof Player)
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 2);
            playSound((Player) sender);
        }
        if (args[0].equalsIgnoreCase("list")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /sdmob list <mob-type/'type'>");
                playSound((Player) sender);
                return true;
            }
            if (args[1].equalsIgnoreCase("type")) {
                sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------[" + ChatColor.LIGHT_PURPLE + " Available Mob Types " + ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "]----------------");
                sender.sendMessage("");
                if (sender instanceof Player) {
                    for (EntityType mb : EntityType.values())
                        if (mb.isAlive())
                            sender.spigot().sendMessage(new ComponentBuilder(mb.name())
                                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sdmob list " + mb.name()))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to see all mobs in " + mb.name())))
                                    .create());
                } else
                    for (EntityType mb : EntityType.values())
                        if (mb.isAlive())
                            sender.sendMessage(" * " + ChatColor.WHITE + mb.name());
                playSound((Player) sender);
                return true;
            }

            String typeFormat = args[1].toUpperCase().replace("-", "_");
            EntityType type;
            try {
                type = EntityType.valueOf(typeFormat);
                Validate.isTrue(type.isAlive());
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + typeFormat + " is not a supported custom mob type.");
                sender.sendMessage(ChatColor.RED + "Use /sdmob list type to see all available mob types.");
                playSound((Player) sender);
                return true;
            }

            FileConfiguration mobs = new ConfigFile(type).getConfig();
            sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------------[" + ChatColor.LIGHT_PURPLE + " Mob List " + ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "]---------------------");
            sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "From " + type.name());
            sender.sendMessage("");
            if (sender instanceof Player)
                for (String s : mobs.getKeys(false))
                    sender.spigot().sendMessage(new ComponentBuilder(mobs.getString(s + ".name"))
                            .color(net.md_5.bungee.api.ChatColor.WHITE)
                            .append(" (")
                            .color(net.md_5.bungee.api.ChatColor.GRAY)
                            .append(s)
                            .color(net.md_5.bungee.api.ChatColor.WHITE)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sdmob edit " + typeFormat + " " + s))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to edit " + s)))
                            .append(") ")
                            .create());
            else
                for (String s : mobs.getKeys(false))
                    sender.sendMessage(" * " + ChatColor.WHITE + mobs.getString(s + ".name") + ChatColor.GRAY + " (" + ChatColor.WHITE + s + ChatColor.GRAY + ")");
            playSound((Player) sender);
        }
        return true;
    }

    private void playSound(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1, 1);
        }
    }
}
