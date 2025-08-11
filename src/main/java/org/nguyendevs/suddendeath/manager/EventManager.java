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

public class EventManager extends BukkitRunnable {
    private static final Map<String, StatusRetriever> statusMap = new HashMap<>();
    private static final Random random = new Random();
    private static final long TICK_INTERVAL = 80L;
    private static final long INITIAL_DELAY = 20L;
    private static final Feature[] EVENT_FEATURES = {Feature.THUNDERSTORM, Feature.BLOOD_MOON, Feature.METEOR_RAIN};

    public EventManager() {
        runTaskTimer(SuddenDeath.getInstance(), INITIAL_DELAY, TICK_INTERVAL);
    }

    private void checkForEvent(World world) {
        if (world == null) {
            return;
        }
        try {
            WorldStatus currentStatus = getStatus(world);
            if (isDay(world) && currentStatus != WorldStatus.DAY) {
                applyStatus(world, WorldStatus.DAY);
                return;
            }
            if (isDay(world) || currentStatus != WorldStatus.DAY) {
                return;
            }
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
                            if (feature != Feature.BLOOD_MOON) {
                                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 0.1f);
                            } else if (feature != Feature.THUNDERSTORM) {
                                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.1f);
                            } else {
                                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.1f);
                            }
                        } catch (Exception e) {
                            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                                    "Error sending event notification to player: " + player.getName(), e);
                        }
                    }

                return;
            }
            applyStatus(world, WorldStatus.NIGHT);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error checking for event in world: " + world.getName(), e);
        }
    }

    public void applyStatus(World world, StatusRetriever retriever) {
        if (world == null || retriever == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Cannot apply status: world or retriever is null");
            return;
        }
        try {
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

    public void refresh(){
        try{
            for(World world : Bukkit.getWorlds()){
                if(world.getEnvironment() != Environment.NORMAL){
                    continue;
                }
                StatusRetriever currentRetriever = statusMap.get(world.getName());
                WorldStatus currentStatus = currentRetriever != null ? currentRetriever.getStatus() : WorldStatus.DAY;
                if(currentRetriever instanceof WorldEventHandler){
                    Feature feature = null;
                    if(currentStatus == WorldStatus.BLOOD_MOON){
                        feature = Feature.BLOOD_MOON;
                    } else if(currentStatus == WorldStatus.THUNDER_STORM){
                        feature = Feature.THUNDERSTORM;
                    } else if(currentStatus == WorldStatus.METEOR_RAIN){
                        feature = Feature.METEOR_RAIN;
                    }
                    if (feature != null && !feature.isEnabled(world)) {
                        ((WorldEventHandler) currentRetriever).close();
                        applyStatus(world, WorldStatus.DAY);
                    }
                }
                if(isDay(world)){
                    applyStatus(world, WorldStatus.DAY);

                } else if(currentStatus == WorldStatus.DAY){
                    for(Feature feature : EVENT_FEATURES){
                        if(feature.isEnabled(world) && random.nextDouble() <= feature.getDouble("chance")/100.0) {
                            applyStatus(world, feature.generateWorldEventHandler(world));
                            SuddenDeath.getInstance().getLogger().info("Started event " + feature.getName() + " in world " + world.getName() + " after config refresh");
                            break;
                        }
                    }
                    if (getStatus(world) == WorldStatus.DAY) {
                        applyStatus(world, WorldStatus.NIGHT);
                    }
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.SEVERE,"Error refreshing EventManager", e);
        }
    }

    public void applyStatus(World world, WorldStatus status) {
        applyStatus(world, new SimpleStatusRetriever(status));
    }

    public static WorldStatus getStatus(World world) {
        if (world == null) {
            return WorldStatus.DAY;
        }
        StatusRetriever retriever = statusMap.get(world.getName());
        return retriever != null ? retriever.getStatus() : WorldStatus.DAY;
    }

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

    public enum WorldStatus {
        NIGHT,
        BLOOD_MOON,
        THUNDER_STORM,
        METEOR_RAIN,
        DAY;

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

    public static class SimpleStatusRetriever implements StatusRetriever {
        private final WorldStatus status;

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