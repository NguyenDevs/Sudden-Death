package org.nguyendevs.suddendeath.player;

import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Utility class for calculating and setting player experience in the SuddenDeath plugin.
 */
public class ExperienceCalculator {
    private static final int LEVEL_THRESHOLD_1 = 15;
    private static final int LEVEL_THRESHOLD_2 = 30;
    private static final int XP_THRESHOLD_1 = 351;
    private static final int XP_THRESHOLD_2 = 1507;

    private final Player player;

    /**
     * Constructs an ExperienceCalculator for the specified player.
     *
     * @param player The player whose experience is to be calculated or set.
     * @throws IllegalArgumentException if player is null.
     */
    public ExperienceCalculator(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        this.player = player;
    }

    /**
     * Calculates the total experience points based on the player's level and experience progress.
     *
     * @return The total experience points.
     */
    public int getTotalExperience() {
        try {
            int level = player.getLevel();
            float expProgress = player.getExp();
            int experience = 0;
            int requiredExperience;

            if (level >= 0 && level <= LEVEL_THRESHOLD_1) {
                experience = (int) Math.ceil(level * level + 6 * level);
                requiredExperience = 2 * level + 7;
            } else if (level <= LEVEL_THRESHOLD_2) {
                experience = (int) Math.ceil(2.5 * level * level - 40.5 * level + 360);
                requiredExperience = 5 * level - 38;
            } else {
                experience = (int) Math.ceil(4.5 * level * level - 162.5 * level + 2220);
                requiredExperience = 9 * level - 158;
            }

            experience += (int) Math.ceil(expProgress * requiredExperience);
            return Math.max(0, experience);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error calculating total experience for player: " + player.getName(), e);
            return 0;
        }
    }

    /**
     * Sets the player's total experience, updating their level and experience progress.
     *
     * @param xp The total experience points to set.
     */
    public void setTotalExperience(int xp) {
        if (xp < 0) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Attempted to set negative experience for player: " + player.getName());
            return;
        }

        try {
            int level;
            int xpForLevel;
            int experienceNeeded;
            float experience;

            if (xp <= XP_THRESHOLD_1) {
                // Levels 0 through 15
                double a = 1.0;
                double b = 6.0;
                double c = -xp;
                level = (int) ((-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
                xpForLevel = level * level + 6 * level;
                experienceNeeded = 2 * level + 7;
            } else if (xp <= XP_THRESHOLD_2) {
                // Levels 16 through 30
                double a = 2.5;
                double b = -40.5;
                double c = -xp + 360;
                level = (int) Math.floor((-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
                xpForLevel = (int) (2.5 * level * level - 40.5 * level + 360);
                experienceNeeded = 5 * level - 38;
            } else {
                // Level 31 and above
                double a = 4.5;
                double b = -162.5;
                double c = -xp + 2220;
                level = (int) Math.floor((-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a));
                xpForLevel = (int) (4.5 * level * level - 162.5 * level + 2220);
                experienceNeeded = 9 * level - 158;
            }

            int remainder = xp - xpForLevel;
            experience = (float) remainder / experienceNeeded;
            experience = round(experience, 2);

            player.setLevel(level);
            player.setExp(experience);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error setting total experience for player: " + player.getName(), e);
        }
    }

    /**
     * Rounds a float value to the specified number of decimal places.
     *
     * @param value       The float value to round.
     * @param decimalPlaces The number of decimal places.
     * @return The rounded float value.
     */
    private float round(float value, int decimalPlaces) {
        try {
            float pow = (float) Math.pow(10, decimalPlaces);
            return Math.round(value * pow) / pow;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error rounding experience value: " + value, e);
            return value;
        }
    }
}