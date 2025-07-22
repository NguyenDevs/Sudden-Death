package org.nguyendevs.suddendeath.util;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Utility class providing helper methods for the SuddenDeath plugin.
 */
public final class Utils {
    private Utils() {
        // Prevent instantiation
    }

    /**
     * Checks if a string contains non-letter characters or underscores, indicating an ID-like format.
     *
     * @param str The string to check.
     * @return True if the string contains digits or special characters other than underscore.
     */
    public static boolean isIDType(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isLetter(c) && c != '_') {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a player is in Creative or Spectator mode.
     *
     * @param player The player to check.
     * @return True if the player is in Creative or Spectator mode.
     */
    public static boolean hasCreativeGameMode(Player player) {
        if (player == null) {
            return false;
        }
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    /**
     * Checks if an item is a plugin-specific item based on its metadata.
     *
     * @param item The item to check.
     * @param checkLore If true, also checks for lore presence.
     * @return True if the item has a display name and optionally lore.
     */
    public static boolean isPluginItem(ItemStack item, boolean checkLore) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        var itemMeta = item.getItemMeta();
        return itemMeta != null && itemMeta.hasDisplayName() && (!checkLore || itemMeta.hasLore());
    }

    /**
     * Retrieves and translates a message from the plugin's message configuration.
     *
     * @param path The configuration path of the message.
     * @return The translated message with color codes applied, or an empty string if not found.
     */
    public static String msg(String path) {
        try {
            String message = SuddenDeath.getInstance().messages.getConfig().getString(path);
            return message != null ? ChatColor.translateAlternateColorCodes('&', message) : "";
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error retrieving message for path: " + path, e);
            return "";
        }
    }

    /**
     * Retrieves and translates a list of messages from the plugin's message configuration.
     *
     * @param path The configuration path of the message list.
     * @return A list of translated messages with color codes applied.
     */
    public static List<String> msgList(String path) {
        try {
            return SuddenDeath.getInstance().messages.getConfig().getStringList(path)
                    .stream()
                    .map(text -> ChatColor.translateAlternateColorCodes('&', text))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error retrieving message list for path: " + path, e);
            return List.of();
        }
    }

    /**
     * Applies damage to a living entity, optionally triggering the damage animation.
     *
     * @param target The entity to damage.
     * @param value The amount of damage to apply.
     * @param effect Whether to trigger the damage animation.
     */
    public static void damage(LivingEntity target, double value, boolean effect) {
        if (target == null || value < 0) {
            return;
        }
        try {
            double health = target.getHealth();
            if (health - value <= 1 || effect) {
                target.damage(value);
            } else {
                target.setHealth(Math.max(0, health - value));
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying damage to entity: " + target.getType(), e);
        }
    }

    /**
     * Retrieves a list of all spawnable and living entity types.
     *
     * @return A list of living EntityType values.
     */
    public static List<EntityType> getLivingEntityTypes() {
        return Arrays.stream(EntityType.values())
                .filter(type -> type.isSpawnable() && type.isAlive())
                .collect(Collectors.toList());
    }

    /**
     * Converts a Roman numeral string to an integer.
     *
     * @param number The Roman numeral string.
     * @return The integer value, or -1 if invalid.
     */
    public static int romanToInt(String number) {
        if (number == null || number.isEmpty()) {
            return 0;
        }
        try {
            number = number.toUpperCase();
            if (number.startsWith("CD")) return 400 + romanToInt(number.substring(2));
            if (number.startsWith("C")) return 100 + romanToInt(number.substring(1));
            if (number.startsWith("XC")) return 90 + romanToInt(number.substring(2));
            if (number.startsWith("L")) return 50 + romanToInt(number.substring(1));
            if (number.startsWith("XL")) return 40 + romanToInt(number.substring(2));
            if (number.startsWith("X")) return 10 + romanToInt(number.substring(1));
            if (number.startsWith("IX")) return 9 + romanToInt(number.substring(2));
            if (number.startsWith("V")) return 5 + romanToInt(number.substring(1));
            if (number.startsWith("IV")) return 4 + romanToInt(number.substring(2));
            if (number.startsWith("I")) return 1 + romanToInt(number.substring(1));
            return -1;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error converting Roman numeral: " + number, e);
            return -1;
        }
    }

    /**
     * Converts an integer to a Roman numeral string.
     *
     * @param input The integer to convert (1 to 499).
     * @return The Roman numeral string, or ">499" if out of range.
     */
    public static String intToRoman(int input) {
        if (input < 1 || input > 499) {
            return ">499";
        }
        try {
            StringBuilder result = new StringBuilder();
            while (input >= 400) { result.append("CD"); input -= 400; }
            while (input >= 100) { result.append("C"); input -= 100; }
            while (input >= 90) { result.append("XC"); input -= 90; }
            while (input >= 50) { result.append("L"); input -= 50; }
            while (input >= 40) { result.append("XL"); input -= 40; }
            while (input >= 10) { result.append("X"); input -= 10; }
            while (input >= 9) { result.append("IX"); input -= 9; }
            while (input >= 5) { result.append("V"); input -= 5; }
            while (input >= 4) { result.append("IV"); input -= 4; }
            while (input >= 1) { result.append("I"); input -= 1; }
            return result.toString();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error converting integer to Roman numeral: " + input, e);
            return ">499";
        }
    }

    /**
     * Capitalizes the first letter of each word in a string.
     *
     * @param str The input string.
     * @return The capitalized string.
     */
    public static String caseOnWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        try {
            StringBuilder builder = new StringBuilder(str);
            boolean isLastSpace = true;
            for (int i = 0; i < builder.length(); i++) {
                char ch = builder.charAt(i);
                if (isLastSpace && ch >= 'a' && ch <= 'z') {
                    builder.setCharAt(i, (char) (ch - 32));
                    isLastSpace = false;
                } else if (ch != ' ') {
                    isLastSpace = false;
                } else {
                    isLastSpace = true;
                }
            }
            return builder.toString();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error capitalizing string: " + str, e);
            return str;
        }
    }

    /**
     * Creates a NamespacedKey for the plugin.
     *
     * @param key The key string.
     * @return A NamespacedKey instance.
     */
    public static NamespacedKey nsk(String key) {
        if (key == null || key.isEmpty()) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Invalid NamespacedKey: " + key);
            return null;
        }
        try {
            return new NamespacedKey(SuddenDeath.getInstance(), key);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error creating NamespacedKey: " + key, e);
            return null;
        }
    }

    /**
     * Gets the display name of an item, falling back to a formatted material name if none exists.
     *
     * @param item The item to get the display name for.
     * @return The display name or formatted material name.
     */
    public static String displayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        try {
            var itemMeta = item.getItemMeta();
            if (itemMeta != null && itemMeta.hasDisplayName()) {
                return itemMeta.getDisplayName();
            }
            return caseOnWords(item.getType().name().replace("_", " "));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error getting display name for item: " + item.getType(), e);
            return "";
        }
    }

    /**
     * Converts a string to a lowercase ID format by replacing underscores with hyphens.
     *
     * @param str The input string.
     * @return The formatted lowercase ID.
     */
    public static String lowerCaseId(String str) {
        if (str == null) {
            return "";
        }
        try {
            return str.toLowerCase().replace("_", "-");
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error formatting lowercase ID: " + str, e);
            return str;
        }
    }
}