package org.nguyendevs.suddendeath.util;

import org.nguyendevs.suddendeath.SuddenDeath;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static boolean isIDType(String str) {
        for (char c : str.toCharArray())
            if (!Character.isLetter(c) && !Character.toString(c).equals("_"))
                return false;
        return true;
    }

    public static boolean hasCreativeGameMode(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    public static boolean isPluginItem(ItemStack item, boolean lore) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && (!lore || item.getItemMeta().hasLore());
    }

    /**
     * @param path Message path
     * @return Translated message taking into account color codes
     */
    public static String msg(String path) {
        return ChatColor.translateAlternateColorCodes('&', SuddenDeath.plugin.messages.getConfig().getString(path));
    }

    /**
     * @param path Message path
     * @return Translated message taking into account color codes
     */
    public static List<String> msgList(String path) {
        List<String> list = SuddenDeath.plugin.messages.getConfig().getStringList(path);
        for (int j = 0; j < list.size(); j++)
            list.set(j, ChatColor.translateAlternateColorCodes('&', list.get(j)));
        return list;
    }

    public static void damage(LivingEntity target, double value, boolean effect) {
        double health = target.getHealth();
        if (health - value <= 1 || effect)
            target.damage(value);
        else
            target.setHealth(target.getHealth() - value);
    }

    public static List<EntityType> getLivingEntityTypes() {
        return Arrays.asList(EntityType.values()).stream().filter(type -> type.isSpawnable() && type.isAlive()).collect(Collectors.toList());
    }

    public static int romanToInt(String number) {
        if (number.equals(""))
            return 0;
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
    }

    public static String intToRoman(int input) {
        if (input < 1 || input > 499)
            return ">499";
        String s = "";
        while (input >= 400) {
            s += "CD";
            input -= 400;
        }
        while (input >= 100) {
            s += "C";
            input -= 100;
        }
        while (input >= 90) {
            s += "XC";
            input -= 90;
        }
        while (input >= 50) {
            s += "L";
            input -= 50;
        }
        while (input >= 40) {
            s += "XL";
            input -= 40;
        }
        while (input >= 10) {
            s += "X";
            input -= 10;
        }
        while (input >= 9) {
            s += "IX";
            input -= 9;
        }
        while (input >= 5) {
            s += "V";
            input -= 5;
        }
        while (input >= 4) {
            s += "IV";
            input -= 4;
        }
        while (input >= 1) {
            s += "I";
            input -= 1;
        }
        return s;
    }

    public static String caseOnWords(String str) {
        StringBuilder builder = new StringBuilder(str);
        boolean isLastSpace = true;
        for (int i = 0; i < builder.length(); i++) {
            char ch = builder.charAt(i);
            if (isLastSpace && ch >= 'a' && ch <= 'z') {
                builder.setCharAt(i, (char) (ch + ('A' - 'a')));
                isLastSpace = false;
            } else if (ch != ' ')
                isLastSpace = false;
            else
                isLastSpace = true;
        }
        return builder.toString();
    }

    public static NamespacedKey nsk(String key) {
        return new NamespacedKey(SuddenDeath.plugin, key);
    }

    public static String displayName(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : caseOnWords(item.getType().name().replace("_", " "));
    }

    public static String lowerCaseId(String str) {
        return str.toLowerCase().replace("_", "-");
    }
}
