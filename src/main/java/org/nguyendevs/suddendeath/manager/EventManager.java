package org.nguyendevs.suddendeath.manager;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Utils;
import org.nguyendevs.suddendeath.world.StatusRetriever;
import org.nguyendevs.suddendeath.world.WorldEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EventManager extends BukkitRunnable {
    private final Map<String, StatusRetriever> status = new HashMap<>();

    private static final Random random = new Random();

    public EventManager() {
        runTaskTimer(SuddenDeath.plugin, 20, 80);
    }

    /*
     * this loop does not have to end events because it is handled in the
     * runnable of worldEventHandlers
     */
    private void checkForEvent(World world) {
        WorldStatus status = getStatus(world);

        /*
         * check if there is an event and if the event can end.
         */
        if (isDay(world) && status != WorldStatus.DAY)
            applyStatus(world, WorldStatus.DAY);

        /*
         * check for a new event if world it is night time and if the status is
         * DAY
         */
        if (isDay(world) || status != WorldStatus.DAY)
            return;

        /*
         * try to find an event which can be applied on this world. if cannot
         * find, set status to NIGHT so no further event is called.
         */
        for (Feature feature : new Feature[]{Feature.THUNDERSTORM, Feature.BLOOD_MOON}) {
            if (!feature.isEnabled(world) || random.nextDouble() > feature.getDouble("chance") / 100)
                continue;

            applyStatus(world, feature.generateWorldEventHandler(world));
            for (Player online : world.getPlayers()) {
                String message = ChatColor.DARK_RED + "" + ChatColor.ITALIC + Utils.msg(feature.name().toLowerCase().replace("_", "-"));
                online.sendMessage(message);
                online.sendTitle("", message, 10, 40, 10);
                online.playSound(online.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0f, 0.0f); // Volume = 1.0, Pitch = 0
            }
            return;
        }

        applyStatus(world, WorldStatus.NIGHT);
    }

    public void applyStatus(World world, WorldStatus status) {
        applyStatus(world, new SimpleStatusRetriever(status));
    }

    public void applyStatus(World world, StatusRetriever retriever) {

        // make sure to close the previous event
        if (status.containsKey(world.getName())) {
            StatusRetriever event = status.get(world.getName());
            if (event instanceof WorldEventHandler)
                ((WorldEventHandler) event).close();
        }

        status.put(world.getName(), retriever);
    }

    public WorldStatus getStatus(World world) {
        return status.containsKey(world.getName()) ? status.get(world.getName()).getStatus() : WorldStatus.DAY;
    }

    public boolean isDay(World world) {
        return world.getTime() < 12300 || world.getTime() > 23850;
    }

    public enum WorldStatus {

        /*
         * when it is a normal night and no event started.
         */
        NIGHT,

        /*
         * event statuses
         */
        BLOOD_MOON,
        THUNDER_STORM,

        /*
         * when an event can potentially start if night time is detected.
         */
        DAY;

        public String getName() {
            return Utils.caseOnWords(name().toLowerCase().replace("_", " "));
        }
    }

    @Override
    public void run() {
        Bukkit.getWorlds().stream().filter(world -> world.getEnvironment() == Environment.NORMAL).forEach(world -> checkForEvent(world));
    }

    public class SimpleStatusRetriever implements StatusRetriever {
        private final WorldStatus status;

        public SimpleStatusRetriever(WorldStatus status) {
            this.status = status;
        }

        @Override
        public WorldStatus getStatus() {
            return status;
        }
    }
}
