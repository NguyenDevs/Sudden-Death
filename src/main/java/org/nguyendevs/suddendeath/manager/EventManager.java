package org.nguyendevs.suddendeath.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Utils;
import org.nguyendevs.suddendeath.world.StatusRetriever;
import org.nguyendevs.suddendeath.world.WorldEventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 * Manages world events such as Blood Moon and Thunderstorm in the SuddenDeath plugin.
 */
public class EventManager extends BukkitRunnable {
    private static final Map<String, StatusRetriever> statusMap = new HashMap<>();
    private static final Random random = new Random();
    private static final long TICK_INTERVAL = 80L;
    private static final long INITIAL_DELAY = 20L;
    private static final Feature[] EVENT_FEATURES = {Feature.THUNDERSTORM, Feature.BLOOD_MOON};

    /**
     * Constructs an EventManager and schedules it to run periodically.
     */
    public EventManager() {
        runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, TICK_INTERVAL);
    }

    /**
     * Checks and applies world events for a specific world.
     *
     * @param world The world to check for events.
     */
    private void checkForEvent(World world) {
        if (world == null) {
            return;
        }

        try {
            WorldStatus currentStatus = getStatus(world);

            // Transition to DAY if it's daytime and not already DAY
            if (isDay(world) && currentStatus != WorldStatus.DAY) {
                applyStatus(world, WorldStatus.DAY);
                return;
            }

            // Only check for new events at night and if current status is DAY
            if (isDay(world) || currentStatus != WorldStatus.DAY) {
                return;
            }

            // Try to trigger a random event
            for (Feature feature : EVENT_FEATURES) {
                if (!feature.isEnabled(world) || random.nextDouble() > feature.getDouble("chance") / 100.0) {
                    continue;
                }

                applyStatus(world, feature.generateWorldEventHandler(world));
                String messageKey = feature.name().toLowerCase().replace("_", "-");
                String message = ChatColor.DARK_RED + "" + ChatColor.ITALIC + Utils.msg(messageKey);

                for (Player player : world.getPlayers()) {
                    try {
                        player.sendMessage(message);
                        player.sendTitle("", message, 10, 40, 10);
                        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0f, 0.0f);
                    } catch (Exception e) {
                        SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                "Error sending event notification to player: " + player.getName(), e);
                    }
                }
                return;
            }

            // If no event is triggered, set to NIGHT
            applyStatus(world, WorldStatus.NIGHT);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking for event in world: " + world.getName(), e);
        }
    }

    /**
     * Applies a status to a world using a StatusRetriever.
     *
     * @param world     The world to apply the status to.
     * @param retriever The StatusRetriever providing the status.
     */
    public void applyStatus(World world, StatusRetriever retriever) {
        if (world == null || retriever == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Cannot apply status: world or retriever is null");
            return;
        }

        try {
            // Close the previous event if it exists
            StatusRetriever existing = statusMap.get(world.getName());
            if (existing instanceof WorldEventHandler) {
                try {
                    ((WorldEventHandler) existing).close();
                } catch (Exception e) {
                    SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                            "Error closing previous event in world: " + world.getName(), e);
                }
            }

            statusMap.put(world.getName(), retriever);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error applying status to world: " + world.getName(), e);
        }
    }

    /**
     * Applies a simple WorldStatus to a world.
     *
     * @param world  The world to apply the status to.
     * @param status The WorldStatus to apply.
     */
    public void applyStatus(World world, WorldStatus status) {
        applyStatus(world, new SimpleStatusRetriever(status));
    }

    /**
     * Gets the current status of a world.
     *
     * @param world The world to check.
     * @return The WorldStatus, or DAY if not set.
     */
    public static WorldStatus getStatus(World world) {
        if (world == null) {
            return WorldStatus.DAY;
        }
        StatusRetriever retriever = statusMap.get(world.getName());
        return retriever != null ? retriever.getStatus() : WorldStatus.DAY;
    }

    /**
     * Checks if it is daytime in the specified world.
     *
     * @param world The world to check.
     * @return True if it is daytime (time < 12300 or time > 23850).
     */
    public boolean isDay(World world) {
        if (world == null) {
            return true;
        }
        try {
            long time = world.getTime();
            return time < 12300 || time > 23850;
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking daytime for world: " + world.getName(), e);
            return true;
        }
    }

    @Override
    public void run() {
        try {
            Bukkit.getWorlds().stream()
                    .filter(world -> world.getEnvironment() == Environment.NORMAL)
                    .forEach(this::checkForEvent);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
                    "Error running EventManager task", e);
        }
    }

    /**
     * Enum representing possible world statuses in the SuddenDeath plugin.
     */
    public enum WorldStatus {
        NIGHT,
        BLOOD_MOON,
        THUNDER_STORM,
        DAY;

        /**
         * Gets the formatted name of the status.
         *
         * @return The formatted status name.
         */
        public String getName() {
            try {
                return Utils.caseOnWords(name().toLowerCase().replace("_", " "));
            } catch (Exception e) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                        "Error formatting name for WorldStatus: " + name(), e);
                return name();
            }
        }
    }

    /**
     * Simple implementation of StatusRetriever for static world statuses.
     */
    public static class SimpleStatusRetriever implements StatusRetriever {
        private final WorldStatus status;

        /**
         * Constructs a SimpleStatusRetriever with the specified status.
         *
         * @param status The WorldStatus to retrieve.
         * @throws IllegalArgumentException if status is null.
         */
        public SimpleStatusRetriever(WorldStatus status) {
            if (status == null) {
                throw new IllegalArgumentException("WorldStatus cannot be null");
            }
            this.status = status;
        }

        @Override
        public WorldStatus getStatus() {
            return status;
        }
    }
}