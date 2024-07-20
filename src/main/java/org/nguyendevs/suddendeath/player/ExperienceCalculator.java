package org.nguyendevs.suddendeath.player;

import org.bukkit.entity.Player;

/**
 * @author Dev_Richard
 */
public class ExperienceCalculator {
    private final Player player;

    public ExperienceCalculator(Player player) {
        this.player = player;
    }

    public int getTotalExperience() {
        int experience = 0;
        int level = player.getLevel();
        if (level >= 0 && level <= 15) {
            experience = (int) Math.ceil(Math.pow(level, 2) + (6 * level));
            int requiredExperience = 2 * level + 7;
            double currentExp = Double.parseDouble(Float.toString(player.getExp()));
            experience += (int) Math.ceil(currentExp * requiredExperience);
            return experience;
        } else if (level > 15 && level <= 30) {
            experience = (int) Math.ceil((2.5 * Math.pow(level, 2) - (40.5 * level) + 360));
            int requiredExperience = 5 * level - 38;
            double currentExp = Double.parseDouble(Float.toString(player.getExp()));
            experience += (int) Math.ceil(currentExp * requiredExperience);
            return experience;
        } else {
            experience = (int) Math.ceil(((4.5 * Math.pow(level, 2) - (162.5 * level) + 2220)));
            int requiredExperience = 9 * level - 158;
            double currentExp = Double.parseDouble(Float.toString(player.getExp()));
            experience += (int) Math.ceil(currentExp * requiredExperience);
            return experience;
        }
    }

    public void setTotalExperience(int xp) {
        // Levels 0 through 15
        if (xp >= 0 && xp < 351) {
            // Calculate Everything
            int a = 1;
            int b = 6;
            int c = -xp;
            int level = (int) (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            int xpForLevel = (int) (Math.pow(level, 2) + (6 * level));
            int remainder = xp - xpForLevel;
            int experienceNeeded = (2 * level) + 7;
            float experience = (float) remainder / (float) experienceNeeded;
            experience = round(experience, 2);

            // Set Everything
            player.setLevel(level);
            player.setExp(experience);
            // Levels 16 through 30
        } else if (xp >= 352 && xp < 1507) {
            // Calculate Everything
            double a = 2.5;
            double b = -40.5;
            int c = -xp + 360;
            double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            int level = (int) Math.floor(dLevel);
            int xpForLevel = (int) (2.5 * Math.pow(level, 2) - (40.5 * level) + 360);
            int remainder = xp - xpForLevel;
            int experienceNeeded = (5 * level) - 38;
            float experience = (float) remainder / (float) experienceNeeded;
            experience = round(experience, 2);

            // Set Everything
            player.setLevel(level);
            player.setExp(experience);
            // Level 31 and greater
        } else {
            // Calculate Everything
            double a = 4.5;
            double b = -162.5;
            int c = -xp + 2220;
            double dLevel = (-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
            int level = (int) Math.floor(dLevel);
            int xpForLevel = (int) (4.5 * Math.pow(level, 2) - (162.5 * level) + 2220);
            int remainder = xp - xpForLevel;
            int experienceNeeded = (9 * level) - 158;
            float experience = (float) remainder / (float) experienceNeeded;
            experience = round(experience, 2);

            // Set Everything
            player.setLevel(level);
            player.setExp(experience);
        }
    }

    private float round(float d, int decimalPlace) {
        float pow = (float) Math.pow(10, decimalPlace);
        return (float) Math.floor(pow * d) / pow;
    }
}