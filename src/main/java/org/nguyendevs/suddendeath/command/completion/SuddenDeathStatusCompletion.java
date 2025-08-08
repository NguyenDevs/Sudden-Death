package org.nguyendevs.suddendeath.command.completion;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.CustomItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SuddenDeathStatusCompletion implements TabCompleter {
	private static final String PERMISSION_STATUS = "suddendeath.admin";
	private static final String PERMISSION_RECIPE = "suddendeath.recipe";
	private static final String PERMISSION_STATUS_VIEW = "suddendeath.status";
	private static final List<String> STATUS_COMMAND = Arrays.asList("menu", "status");
	private static final List<String> MAIN_COMMANDS = Arrays.asList("admin", "clean", "give", "help", "menu" ,"itemlist", "recipe", "reload", "start", "stop");
	private static final List<String> RECIPE_COMMAND = Arrays.asList("recipe");
	private static final List<String> QUANTITY_SUGGESTIONS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "16", "32", "64");

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		try {
			switch (args.length) {
				case 1 -> {
					List<String> availableCommands = MAIN_COMMANDS;

					if (sender.hasPermission(PERMISSION_STATUS)) {
						availableCommands = MAIN_COMMANDS;
					} else {
						availableCommands = new java.util.ArrayList<>();

						if (sender.hasPermission(PERMISSION_RECIPE)) {
							availableCommands.addAll(RECIPE_COMMAND);
						}
						if (sender.hasPermission(PERMISSION_STATUS_VIEW)) {
							availableCommands.addAll(STATUS_COMMAND);
						}
					}

					return filterCompletions(availableCommands, args[0]);
				}

				case 2 -> {
					return handleSecondArgument(sender, args);
				}
				case 3 -> {
					return handleThirdArgument(args);
				}
				case 4 -> {
					return handleFourthArgument(args);
				}
				default -> {
					return Collections.emptyList();
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error during tab completion for /sds command", e);
			return Collections.emptyList();
		}
	}

	private List<String> handleSecondArgument(CommandSender sender, String[] args) {
		if (!sender.hasPermission(PERMISSION_STATUS)) {
			return Collections.emptyList();
		}
		String firstArg = args[0].toLowerCase();

		switch (firstArg) {
			case "start", "stop" -> {
				return filterCompletions(getAvailableEvents(), args[1]);
			}
            case "give" -> {
				return filterCompletions(getCustomItemNames(), args[1]);
			}
			case "clean" -> {
				return filterCompletions(getOnlinePlayerNames(), args[1]);
			}
			default -> {
				return Collections.emptyList();
			}
		}
	}

	private List<String> handleThirdArgument(String[] args) {
		String firstArg = args[0].toLowerCase();
		if (firstArg.equals("give")) {
			return filterCompletions(getOnlinePlayerNames(), args[2]);
		}
		return Collections.emptyList();
	}

	private List<String> handleFourthArgument(String[] args) {
		String firstArg = args[0].toLowerCase();
		if (firstArg.equals("give")) {
			return filterCompletions(QUANTITY_SUGGESTIONS, args[3]);
		}
		return Collections.emptyList();
	}

	private List<String> getAvailableEvents() {
		try {
			return Arrays.stream(Feature.values())
					.filter(Feature::isEvent)
					.map(feature -> feature.name().toLowerCase().replace("_", "-"))
					.collect(Collectors.toList());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error getting available events", e);
			return Collections.emptyList();
		}
	}

	private List<String> getCustomItemNames() {
		try {
			return Arrays.stream(CustomItem.values())
					.map(Enum::name)
					.collect(Collectors.toList());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error getting custom item names", e);
			return Collections.emptyList();
		}
	}

	private List<String> getOnlinePlayerNames() {
		try {
			return Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.collect(Collectors.toList());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error getting online player names", e);
			return Collections.emptyList();
		}
	}

	private List<String> filterCompletions(List<String> completions, String partial) {
		if (partial.isEmpty()) {
			return completions;
		}

		return completions.stream()
				.filter(completion -> completion.toLowerCase().startsWith(partial.toLowerCase()))
				.collect(Collectors.toList());
	}
}