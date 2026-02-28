package org.nguyendevs.suddendeath.Utils;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public final class Utils {
    private Utils() {
    }

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

    public static boolean hasCreativeGameMode(Player player) {
        if (player == null) {
            return false;
        }
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    public static boolean isPluginItem(ItemStack item, boolean checkLore) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        var itemMeta = item.getItemMeta();
        return itemMeta != null && itemMeta.hasDisplayName() && (!checkLore || itemMeta.hasLore());
    }

    public static String msg(String path) {
        try {
            String message = SuddenDeath.getInstance().getConfigManager().messages.getConfig().getString(path);
            return message != null ? message : "";
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error retrieving message for path: " + path, e);
            return "";
        }
    }

    public static List<String> msgList(String path) {
        try {
            return SuddenDeath.getInstance().getConfigManager().messages.getConfig().getStringList(path)
                    .stream()
                    .map(text -> text)
                    .toList();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error retrieving message list for path: " + path,
                    e);
            return List.of();
        }
    }

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

    public static List<EntityType> getLivingEntityTypes() {
        return Arrays.stream(EntityType.values())
                .filter(type -> type.isSpawnable() && type.isAlive())
                .toList();
    }

    public static int romanToInt(String number) {
        if (number == null || number.isEmpty()) {
            return 0;
        }
        try {
            number = number.toUpperCase();
            if (number.startsWith("CD"))
                return 400 + romanToInt(number.substring(2));
            if (number.startsWith("C"))
                return 100 + romanToInt(number.substring(1));
            if (number.startsWith("XC"))
                return 90 + romanToInt(number.substring(2));
            if (number.startsWith("L"))
                return 50 + romanToInt(number.substring(1));
            if (number.startsWith("XL"))
                return 40 + romanToInt(number.substring(2));
            if (number.startsWith("X"))
                return 10 + romanToInt(number.substring(1));
            if (number.startsWith("IX"))
                return 9 + romanToInt(number.substring(2));
            if (number.startsWith("V"))
                return 5 + romanToInt(number.substring(1));
            if (number.startsWith("IV"))
                return 4 + romanToInt(number.substring(2));
            if (number.startsWith("I"))
                return 1 + romanToInt(number.substring(1));
            return -1;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error converting Roman numeral: " + number, e);
            return -1;
        }
    }

    public static String intToRoman(int input) {
        if (input < 1 || input > 499) {
            return ">499";
        }
        try {
            StringBuilder result = new StringBuilder();
            while (input >= 400) {
                result.append("CD");
                input -= 400;
            }
            while (input >= 100) {
                result.append("C");
                input -= 100;
            }
            while (input >= 90) {
                result.append("XC");
                input -= 90;
            }
            while (input >= 50) {
                result.append("L");
                input -= 50;
            }
            while (input >= 40) {
                result.append("XL");
                input -= 40;
            }
            while (input >= 10) {
                result.append("X");
                input -= 10;
            }
            while (input >= 9) {
                result.append("IX");
                input -= 9;
            }
            while (input >= 5) {
                result.append("V");
                input -= 5;
            }
            while (input >= 4) {
                result.append("IV");
                input -= 4;
            }
            while (input >= 1) {
                result.append("I");
                input -= 1;
            }
            return result.toString();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error converting integer to Roman numeral: " + input, e);
            return ">499";
        }
    }

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

    public static String displayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        try {
            var itemMeta = item.getItemMeta();
            if (itemMeta != null && itemMeta.hasDisplayName()) {
                net.kyori.adventure.text.Component component = itemMeta.displayName();
                if (component != null) {
                    return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                            .serialize(component);
                }
            }
            return caseOnWords(item.getType().name().replace("_", " "));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error getting display name for item: " + item.getType(), e);
            return "";
        }
    }

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