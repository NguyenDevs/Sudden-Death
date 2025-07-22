package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.nguyendevs.suddendeath.command.SuddenDeathMobCommand;
import org.nguyendevs.suddendeath.command.SuddenDeathStatusCommand;
import org.nguyendevs.suddendeath.command.completion.SuddenDeathMobCompletion;
import org.nguyendevs.suddendeath.command.completion.SuddenDeathStatusCompletion;
import org.nguyendevs.suddendeath.comp.Metrics;
import org.nguyendevs.suddendeath.comp.SuddenDeathPlaceholders;
import org.nguyendevs.suddendeath.comp.worldguard.WGPlugin;
import org.nguyendevs.suddendeath.comp.worldguard.WorldGuardOff;
import org.nguyendevs.suddendeath.comp.worldguard.WorldGuardOn;
import org.nguyendevs.suddendeath.config.PluginConfiguration;
import org.nguyendevs.suddendeath.gui.listener.GuiListener;
import org.nguyendevs.suddendeath.listener.*;
import org.nguyendevs.suddendeath.manager.EventManager;
import org.nguyendevs.suddendeath.packets.PacketSender;
import org.nguyendevs.suddendeath.packets.v1_17.ProtocolLibImpl;
import org.nguyendevs.suddendeath.player.Difficulty;
import org.nguyendevs.suddendeath.player.Modifier;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.scheduler.BloodEffectRunnable;
import org.nguyendevs.suddendeath.util.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SuddenDeath extends JavaPlugin {
    private static SuddenDeath instance;
    private final Map<Player, Integer> players = new ConcurrentHashMap<>();
    private PluginConfiguration configuration;
    private PacketSender packetSender;
    private WGPlugin wgPlugin;
    private EventManager eventManager;
    public ConfigFile messages;
    private ConfigFile difficulties;
    private ConfigFile items;
    public Difficulty defaultDifficulty;

    public static SuddenDeath getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard") != null ? new WorldGuardOn() : new WorldGuardOff();
        configuration = new PluginConfiguration(this);
    }

    @Override
    public void onEnable() {
        try {
            initializePlugin();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SuddenDeath plugin", e);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        try {
            savePlayerData();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while saving player data on disable", e);
        }
    }

    private void initializePlugin() {
        // Initialize ProtocolLib
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            getLogger().severe("ProtocolLib is unavailable, stopping...");
            throw new IllegalStateException("ProtocolLib is required but not found");
        }
        packetSender = new ProtocolLibImpl(protocolManager);

        // Load configuration
        configuration.load();
        saveDefaultConfig();
        reloadConfig();

        // Initialize config files
        initializeConfigFiles();

        // Register listeners
        registerListeners();

        // Hook into other plugins
        hookIntoPlugins();

        // Initialize features and entities
        initializeFeaturesAndEntities();

        // Initialize difficulties
        initializeDifficulties();

        // Initialize items and recipes
        initializeItemsAndRecipes();

        // Register commands
        registerCommands();

        // Start schedulers
        getServer().getScheduler().runTaskTimer(this, new BloodEffectRunnable(this), 0L, 1L);

        // Check for updates
        new SpigotPlugin(119526, this).checkForUpdate();
    }

    private void initializeConfigFiles() {
        messages = new ConfigFile("/language", "messages");
        difficulties = new ConfigFile("/language", "difficulties");
        items = new ConfigFile("/language", "items");

        // Initialize default config
        for (Feature feature : Feature.values()) {
            if (!getConfig().contains(feature.getPath())) {
                List<String> worlds = new ArrayList<>();
                Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
                getConfig().set(feature.getPath(), worlds);
            }
        }

        // Initialize spawn coefficients
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && !getConfig().contains("default-spawn-coef." + type.name())) {
                getConfig().set("default-spawn-coef." + type.name(), 20);
            }
        }
        saveConfig();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BloodEffectListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getServer().getPluginManager().registerEvents(new CustomMobs(), this);
        getServer().getPluginManager().registerEvents(new Listener1(), this);
        getServer().getPluginManager().registerEvents(new Listener2(), this);

        if (!getConfig().getBoolean("disable-difficulties")) {
            getServer().getPluginManager().registerEvents(new DifficultiesListener(), this);
        }
    }

    private void hookIntoPlugins() {
        // WorldGuard hook
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            getLogger().info("Hooked onto WorldGuard");
        }

        // PlaceholderAPI hook
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SuddenDeathPlaceholders().register();
            getLogger().info("Hooked onto PlaceholderAPI");
        }

        // Initialize Metrics
        new Metrics(this);
    }

    private void initializeFeaturesAndEntities() {
        eventManager = new EventManager();

        // Initialize custom mobs
        for (EntityType type : EntityType.values()) {
            if (type.isAlive()) {
                new ConfigFile("/customMobs", Utils.lowerCaseId(type.name())).setup();
            }
        }

        // Load player data
        Bukkit.getOnlinePlayers().forEach(PlayerData::setup);

        // Initialize feature modifiers
        for (Feature feature : Feature.values()) {
            feature.updateConfig();
            ConfigFile modifiers = feature.getConfigFile();
            for (Modifier mod : feature.getModifiers()) {
                if (modifiers.getConfig().contains(mod.getName())) {
                    continue;
                }
                if (mod.getType() == Modifier.Type.NONE) {
                    modifiers.getConfig().set(mod.getName(), mod.getDefaultValue());
                } else if (mod.getType() == Modifier.Type.EACH_MOB) {
                    for (EntityType type : Utils.getLivingEntityTypes()) {
                        modifiers.getConfig(). set(mod.getName() + "." + type.name(), mod.getDefaultValue());
                    }
                }
            }
            modifiers.save();
        }
    }

    private void initializeDifficulties() {
        for (Difficulty difficulty : Difficulty.values()) {
            if (!difficulties.getConfig().contains(difficulty.name())) {
                difficulties.getConfig().set(difficulty.name() + ".name", difficulty.getName());
                difficulties.getConfig().set(difficulty.name() + ".lore", new ArrayList<>());
                difficulties.getConfig().set(difficulty.name() + ".health-malus", difficulty.getHealthMalus());
                difficulties.getConfig().set(difficulty.name() + ".increased-damage", difficulty.getIncreasedDamage());
            }
            difficulty.update(difficulties.getConfig());
        }
        difficulties.save();

        if (!getConfig().getBoolean("disable-difficulties")) {
            try {
                defaultDifficulty = Difficulty.valueOf(getConfig().getString("default-difficulty", "SANDBOX"));
            } catch (IllegalArgumentException e) {
                defaultDifficulty = Difficulty.SANDBOX;
                getLogger().warning("Invalid default difficulty in config, defaulting to SANDBOX");
            }
        }
    }

    private void initializeItemsAndRecipes() {
        for (CustomItem item : CustomItem.values()) {
            if (!items.getConfig().contains(item.name())) {
                items.getConfig().set(item.name() + ".name", item.getDefaultName());
                items.getConfig().set(item.name() + ".lore", Arrays.asList(item.lore));
                if (item.craft != null) {
                    items.getConfig().set(item.name() + ".craft-enabled", true);
                    items.getConfig().set(item.name() + ".craft", Arrays.asList(item.craft));
                }
            }

            item.update(Objects.requireNonNull(items.getConfig().getConfigurationSection(item.name())));

            if (items.getConfig().getBoolean(item.name() + ".craft-enabled") && item.craft != null) {
                try {
                    registerCraftingRecipe(item);
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.WARNING, "Failed to register recipe for " + item.name(), e);
                }
            }
        }
        items.save();
    }

    private void registerCraftingRecipe(CustomItem item) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "SuddenDeath_" + item.name()), item.a());
        recipe.shape("ABC", "DEF", "GHI");
        char[] chars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
        List<String> craftLines = items.getConfig().getStringList(item.name() + ".craft");

        if (craftLines.size() != 3) {
            throw new IllegalArgumentException("Invalid craft format for " + item.name());
        }

        for (int i = 0; i < 9; i++) {
            String[] line = craftLines.get(i / 3).split(",");
            if (line.length < 3) {
                throw new IllegalArgumentException("Invalid craft line format for " + item.name());
            }

            String materialName = line[i % 3].trim();
            if (materialName.equalsIgnoreCase("AIR")) {
                continue;
            }

            Material material = Material.getMaterial(materialName);
            if (material == null) {
                throw new IllegalArgumentException("Invalid material: " + materialName + " for " + item.name());
            }

            recipe.setIngredient(chars[i], material);
        }

        getServer().addRecipe(recipe);
    }

    private void registerCommands() {
        Optional.ofNullable(getCommand("sdstatus")).ifPresent(cmd -> {
            cmd.setExecutor(new SuddenDeathStatusCommand());
            cmd.setTabCompleter(new SuddenDeathStatusCompletion());
        });
        Optional.ofNullable(getCommand("sdmob")).ifPresent(cmd -> {
            cmd.setExecutor(new SuddenDeathMobCommand());
            cmd.setTabCompleter(new SuddenDeathMobCompletion());
        });
    }

    private void savePlayerData() {
        PlayerData.getLoaded().forEach(data -> {
            try {
                ConfigFile file = new ConfigFile("/userdata", data.getUniqueId().toString());
                data.save(file.getConfig());
                file.save();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to save player data for " + data.getUniqueId(), e);
            }
        });
    }

    public void reloadConfigFiles() {
        try {
            messages = new ConfigFile("/language", "messages");
            items = new ConfigFile("/language", "items");
            difficulties = new ConfigFile("/language", "difficulties");
            saveDefaultConfig();
            reloadConfig();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading configuration files", e);
        }
    }

    public Map<Player, Integer> getPlayers() {
        return players;
    }

    public ConfigFile getConfiguration() {
        return configuration;
    }

    public PacketSender getPacketSender() {
        return packetSender;
    }

    public WGPlugin getWorldGuard() {
        return wgPlugin;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public Difficulty getDefaultDifficulty() {
        return defaultDifficulty;
    }
}