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

/**
 * Tab completion handler for the SuddenDeath plugin's /sds command.
 * Provides intelligent auto-completion for admin commands, recipe management, and features.
 */
public class SuddenDeathStatusCompletion implements TabCompleter {
	private static final String PERMISSION_STATUS = "suddendeath.admin";
	private static final String PERMISSION_RECIPE = "suddendeath.recipe";
	private static final String PERMISSION_STATUS_VIEW = "suddendeath.status";
	private static final List<String> STATUS_COMMAND = Arrays.asList("status");
	private static final List<String> MAIN_COMMANDS = Arrays.asList("admin", "help", "give", "itemlist", "recipe", "reload", "clean", "start");
	private static final List<String> RECIPE_COMMAND = Arrays.asList("recipe");
	private static final List<String> QUANTITY_SUGGESTIONS = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "16", "32", "64");

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		try {
			switch (args.length) {
				case 1 -> {
					if (sender.hasPermission(PERMISSION_STATUS)) {
						return filterCompletions(MAIN_COMMANDS, args[0]);
					} else if (sender.hasPermission(PERMISSION_RECIPE)) {
						return filterCompletions(RECIPE_COMMAND, args[0]);
					} else if (sender.hasPermission(PERMISSION_STATUS_VIEW)) {
						return filterCompletions(STATUS_COMMAND, args[0]);
					}
					return Collections.emptyList();
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

	/**
	 * Handles tab completion for the second argument based on the first argument.
	 *
	 * @param sender The command sender.
	 * @param args   The command arguments.
	 * @return List of completion suggestions.
	 */
	private List<String> handleSecondArgument(CommandSender sender, String[] args) {
		if (!sender.hasPermission(PERMISSION_STATUS)) {
			return Collections.emptyList();
		}
		String firstArg = args[0].toLowerCase();

		switch (firstArg) {
			case "start" -> {
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

	/**
	 * Handles tab completion for the third argument based on the first two arguments.
	 *
	 * @param args The command arguments.
	 * @return List of completion suggestions.
	 */
	private List<String> handleThirdArgument(String[] args) {
		String firstArg = args[0].toLowerCase();
		if (firstArg.equals("give")) {
			return filterCompletions(getOnlinePlayerNames(), args[2]);
		}
		return Collections.emptyList();
	}

	/**
	 * Handles tab completion for the fourth argument based on the first three arguments.
	 *
	 * @param args The command arguments.
	 * @return List of completion suggestions.
	 */
	private List<String> handleFourthArgument(String[] args) {
		String firstArg = args[0].toLowerCase();
		if (firstArg.equals("give")) {
			return filterCompletions(QUANTITY_SUGGESTIONS, args[3]);
		}
		return Collections.emptyList();
	}

	/**
	 * Gets all available event features that can be started.
	 *
	 * @return List of event feature names.
	 */
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

	/**
	 * Gets all custom item names that can be given to players.
	 *
	 * @return List of custom item names.
	 */
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

	/**
	 * Gets all online player names for player targeting commands.
	 *
	 * @return List of online player names.
	 */
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

	/**
	 * Filters completion suggestions based on what the user has already typed.
	 *
	 * @param completions All available completions.
	 * @param partial     What the user has typed so far.
	 * @return Filtered list of completions that start with the partial input.
	 */
	private List<String> filterCompletions(List<String> completions, String partial) {
		if (partial.isEmpty()) {
			return completions;
		}

		return completions.stream()
				.filter(completion -> completion.toLowerCase().startsWith(partial.toLowerCase()))
				.collect(Collectors.toList());
	}
}