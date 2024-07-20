package org.nguyendevs.suddendeath.command.completion;

import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SuddenDeathStatusCompletion implements TabCompleter {
	@Override
	public List<String> onTabComplete(CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (!sender.hasPermission("suddendeath.op"))
			return null;

		List<String> list = new ArrayList<>();

		if (args.length == 1) {
			list.add("admin");
			list.add("help");
			list.add("give");
			list.add("itemlist");
			list.add("reload");
			list.add("clean");
			list.add("start");
		}

		if (args.length == 2) {
			if (args[0].equals("start"))
				for (Feature feature : Feature.values())
					if (feature.isEvent())
						list.add(feature.name().toLowerCase().replace("_", "-"));

			if (args[0].equals("give"))
				for (CustomItem item : CustomItem.values())
					list.add(item.name());

			if (args[0].equals("clean"))
				for (Player online : Bukkit.getOnlinePlayers())
					list.add(online.getName());
		}

		if (args.length == 3)
			if (args[0].equals("give"))
				for (Player online : Bukkit.getOnlinePlayers())
					list.add(online.getName());

		if (args.length == 4)
			if (args[0].equals("give"))
				for (int j = 1; j < 10; j++)
					list.add("" + j);

		return args[args.length - 1].isEmpty() ? list : list.stream().filter(string -> string.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
	}
}
