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
import java.util.stream.Collectors;
import java.util.logging.Level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

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

    public static String intToRoman(int input) {
        if (input < 1 || input > 499) {
            return ">499";
        }

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
                } else
                    isLastSpace = ch == ' ';
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

    public static Component color(String str) {
        if (str == null || str.isEmpty()) {
            return Component.empty();
        }

        char[] chars = str.toCharArray();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                String tag = switch (code) {
                    case '0' -> "<black>";
                    case '1' -> "<dark_blue>";
                    case '2' -> "<dark_green>";
                    case '3' -> "<dark_aqua>";
                    case '4' -> "<dark_red>";
                    case '5' -> "<dark_purple>";
                    case '6' -> "<gold>";
                    case '7' -> "<gray>";
                    case '8' -> "<dark_gray>";
                    case '9' -> "<blue>";
                    case 'a' -> "<green>";
                    case 'b' -> "<aqua>";
                    case 'c' -> "<red>";
                    case 'd' -> "<light_purple>";
                    case 'e' -> "<yellow>";
                    case 'f' -> "<white>";
                    case 'k' -> "<obfuscated>";
                    case 'l' -> "<bold>";
                    case 'm' -> "<strikethrough>";
                    case 'n' -> "<underlined>";
                    case 'o' -> "<italic>";
                    case 'r' -> "<reset>";
                    default -> null;
                };
                if (tag != null) {
                    builder.append(tag);
                    i++;
                    continue;
                }
            }
            builder.append(chars[i]);
        }

        String parsed = builder.toString();

        parsed = parsed.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");
        parsed = parsed.replaceAll("</#[A-Fa-f0-9]{6}>", "</color>");

        parsed = parsed.replaceAll("<#([A-Fa-f0-9]{6})>", "<color:#$1>");
        parsed = parsed.replaceAll("</#([A-Fa-f0-9]{6})>", "</color>");

        return MiniMessage.miniMessage().deserialize(parsed)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC,
                        net.kyori.adventure.text.format.TextDecoration.State.FALSE);
    }

    public static List<Component> color(List<String> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(Utils::color).collect(Collectors.toList());
    }
}