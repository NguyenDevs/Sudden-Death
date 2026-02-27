package org.nguyendevs.suddendeath.Commands.completion;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.ConfigFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")

public class SuddenDeathMobCompletion implements TabCompleter {
    private static final String PERMISSION_OP = "suddendeath.admin";
    private static final List<String> MAIN_COMMANDS = Arrays.asList("create", "delete", "edit", "help", "kill", "list",
            "spawn");
    private static final List<String> KILL_RADIUS_SUGGESTIONS = Arrays.asList("10", "20", "30", "40", "50", "60", "70",
            "80", "90", "100", "1000");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
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
                case 4 -> {
                    if (args[0].equalsIgnoreCase("spawn")) {
                        return filterCompletions(Arrays.asList("1", "2", "3", "5", "10"), args[3]);
                    }
                    return Collections.emptyList();
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

    private List<String> handleSecondArgument(String[] args) {
        String firstArg = args[0].toLowerCase();

        switch (firstArg) {
            case "kill" -> {
                return filterCompletions(KILL_RADIUS_SUGGESTIONS, args[1]);
            }
            case "create", "edit", "delete", "spawn" -> {
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

    private List<String> handleThirdArgument(String[] args) {
        String firstArg = args[0].toLowerCase();

        if (!firstArg.equals("edit") && !firstArg.equals("delete") && !firstArg.equals("spawn")) {
            return Collections.emptyList();
        }

        EntityType entityType = parseEntityType(args[1]);
        if (entityType == null) {
            return Collections.emptyList();
        }

        return filterCompletions(getMobIdsForType(entityType), args[2]);
    }

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

    private List<String> getMobIdsForType(EntityType entityType) {
        try {
            ConfigFile configFile = SuddenDeath.getInstance().getConfigManager().getMobConfig(entityType);
            if (configFile == null)
                return Collections.emptyList();
            return new ArrayList<>(configFile.getConfig().getKeys(false));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error getting mob IDs for type: " + entityType.name(), e);
            return Collections.emptyList();
        }
    }

    private EntityType parseEntityType(String typeStr) {
        try {
            EntityType type = EntityType.valueOf(typeStr.toUpperCase().replace("-", "_"));
            return type.isAlive() ? type : null;
        } catch (IllegalArgumentException e) {
            return null;
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