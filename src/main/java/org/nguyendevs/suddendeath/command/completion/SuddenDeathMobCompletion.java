package org.nguyendevs.suddendeath.command.completion;

import org.nguyendevs.suddendeath.util.ConfigFile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SuddenDeathMobCompletion implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("suddendeath.op"))
            return null;

        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("create");
            list.add("edit");
            list.add("remove");
            list.add("list");
            list.add("kill");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("kill"))
                for (int j = 10; j < 101; j += 10)
                    list.add("" + j);

            if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("list"))
                for (EntityType type : EntityType.values())
                    if (type.isAlive())
                        list.add(type.name());

            if (args[0].equalsIgnoreCase("list"))
                list.add("type");
        }

        if (args.length == 3)
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("remove"))
                for (EntityType type1 : EntityType.values())
                    if (type1.isAlive() && type1.name().equalsIgnoreCase(args[1].replace("-", "_")))
                        new ConfigFile(type1).getConfig().getKeys(false).forEach(key -> list.add(key.toUpperCase().replace("-", "_")));

        // if last arg is not empty, only complete word
        return args[args.length - 1].isEmpty() ? list : list.stream().filter(string -> string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}
