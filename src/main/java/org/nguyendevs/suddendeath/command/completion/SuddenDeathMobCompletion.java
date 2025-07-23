package org.nguyendevs.suddendeath.command.completion;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.ConfigFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Tab completion handler for the SuddenDeath plugin's /sdmob command.
 * Provides intelligent auto-completion for command arguments.
 */
public class SuddenDeathMobCompletion implements TabCompleter {
    private static final String PERMISSION_OP = "suddendeath.op";
    private static final List<String> MAIN_COMMANDS = Arrays.asList("create", "edit", "remove", "delete", "list", "kill");
    private static final List<String> KILL_RADIUS_SUGGESTIONS = Arrays.asList("10", "20", "30", "40", "50", "60", "70", "80", "90", "100");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Permission check
        if (!sender.hasPermission(PERMISSION_OP)) {
            return Collections.emptyList();
        }

        try {
            switch (args.length) {
                case 1 -> {
                    return filterCompletions(MAIN_COMMANDS, args[0]);
                }
                case 2 -> {
                    return handleSecondArgument(args);
                }
                case 3 -> {
                    return handleThirdArgument(args);
                }
                default -> {
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error during tab completion for /sdmob command", e);
            return Collections.emptyList();
        }
    }

    /**
     * Handles tab completion for the second argument based on the first argument.
     *
     * @param args The command arguments.
     * @return List of completion suggestions.
     */
    private List<String> handleSecondArgument(String[] args) {
        String firstArg = args[0].toLowerCase();

        switch (firstArg) {
            case "kill" -> {
                return filterCompletions(KILL_RADIUS_SUGGESTIONS, args[1]);
            }
            case "create", "edit", "remove", "delete" -> {
                return filterCompletions(getValidEntityTypes(), args[1]);
            }
            case "list" -> {
                List<String> listOptions = new ArrayList<>(getValidEntityTypes());
                listOptions.add("type");
                return filterCompletions(listOptions, args[1]);
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

        if (!firstArg.equals("edit") && !firstArg.equals("remove") && !firstArg.equals("delete")) {
            return Collections.emptyList();
        }

        EntityType entityType = parseEntityType(args[1]);
        if (entityType == null) {
            return Collections.emptyList();
        }

        return filterCompletions(getMobIdsForType(entityType), args[2]);
    }

    /**
     * Gets all valid living entity types for mob creation.
     *
     * @return List of valid entity type names.
     */
    private List<String> getValidEntityTypes() {
        try {
            return Arrays.stream(EntityType.values())
                    .filter(EntityType::isAlive)
                    .map(Enum::name)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error getting valid entity types", e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets all mob IDs for a specific entity type.
     *
     * @param entityType The entity type to get mob IDs for.
     * @return List of mob IDs for the specified type.
     */
    private List<String> getMobIdsForType(EntityType entityType) {
        try {
            ConfigFile configFile = new ConfigFile(entityType);
            return new ArrayList<>(configFile.getConfig().getKeys(false));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error getting mob IDs for type: " + entityType.name(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses an entity type string and validates it.
     *
     * @param typeStr The entity type string to parse.
     * @return The parsed EntityType, or null if invalid.
     */
    private EntityType parseEntityType(String typeStr) {
        try {
            EntityType type = EntityType.valueOf(typeStr.toUpperCase().replace("-", "_"));
            return type.isAlive() ? type : null;
        } catch (IllegalArgumentException e) {
            return null;
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