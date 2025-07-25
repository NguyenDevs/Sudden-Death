package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.nguyendevs.suddendeath.command.SuddenDeathMobCommand;
import org.nguyendevs.suddendeath.command.SuddenDeathStatusCommand;
import org.nguyendevs.suddendeath.command.completion.SuddenDeathMobCompletion;
import org.nguyendevs.suddendeath.command.completion.SuddenDeathStatusCompletion;
import org.nguyendevs.suddendeath.comp.SuddenDeathPlaceholders;
import org.nguyendevs.suddendeath.comp.worldguard.WGPlugin;
import org.nguyendevs.suddendeath.comp.worldguard.WorldGuardOff;
import org.nguyendevs.suddendeath.comp.worldguard.WorldGuardOn;
import org.nguyendevs.suddendeath.gui.AdminView;
import org.nguyendevs.suddendeath.gui.CrafterInventory;
import org.nguyendevs.suddendeath.gui.listener.GuiListener;
import org.nguyendevs.suddendeath.listener.*;
import org.nguyendevs.suddendeath.manager.EventManager;
import org.nguyendevs.suddendeath.packets.PacketSender;
import org.nguyendevs.suddendeath.packets.v1_17.ProtocolLibImpl;
import org.nguyendevs.suddendeath.player.Modifier;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.*;

import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SuddenDeath extends JavaPlugin {
    private static SuddenDeath instance;
    private final Map<Player, Integer> players = new ConcurrentHashMap<>();
    private ConfigFile configuration;
    private PacketSender packetSender;
    private WGPlugin wgPlugin;
    private EventManager eventManager;
    public ConfigFile messages;
    public ConfigFile items;

    public static SuddenDeath getInstance() {
        return instance;
    }

    /**
     * Prints the Sudden Death plugin logo to the console.
     */
    public void printLogo() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ███████╗██╗   ██╗██████╗ ██████╗ ███████╗███╗   ██╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ██╔════╝██║   ██║██╔══██╗██╔══██╗██╔════╝████╗  ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ███████╗██║   ██║██║  ██║██║  ██║█████╗  ██╔██╗ ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ╚════██║██║   ██║██║  ██║██║  ██║██╔══╝  ██║╚██╗██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ███████║╚██████╔╝██████╔╝██████╔╝███████╗██║ ╚████║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&c   ╚══════╝ ╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██║  ██║█████╗  ███████║   ██║   ███████║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ██████╔╝███████╗██║  ██║   ██║   ██║  ██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4   ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6              Version " + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b         Development by NguyenDevs"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }

    @Override
    public void onLoad() {
        instance = this;
        wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard") != null ? new WorldGuardOn() : new WorldGuardOff();
        configuration = new ConfigFile(this, "config");
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
            removeCustomRecipes();
            savePlayerData();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while saving player data on disable", e);
        }
    }

    private void initializePlugin() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            getLogger().severe("ProtocolLib is unavailable, stopping...");
            throw new IllegalStateException("ProtocolLib is required but not found");
        }
        packetSender = new ProtocolLibImpl(protocolManager);
        configuration.reload();
        initializeConfigFiles();
        registerListeners();
        hookIntoPlugins();
        initializeFeaturesAndEntities();
        initializeItemsAndRecipes();
        registerCommands();
        printLogo();
        new SpigotPlugin(119526, this).checkForUpdate();
    }

    private void initializeConfigFiles() {
        messages = new ConfigFile(this, "/language", "messages");
        items = new ConfigFile(this, "/language", "items");

        initializeDefaultMessages();

        // Load default config from resources if it exists
        FileConfiguration defaultConfig = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(getResource("config.yml")))) {
            defaultConfig.load(reader);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to load default config.yml from resources", e);
        }

        // Merge default values if they don't exist in config.yml
        ConfigurationSection configSection = configuration.getConfig();
        if (!configSection.contains("update-notify")) {
            configSection.set("update-notify", defaultConfig.getBoolean("update-notify", true));
        }

        // Add feature worlds
        for (Feature feature : Feature.values()) {
            if (!configSection.contains(feature.getPath())) {
                List<String> worlds = new ArrayList<>();
                Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
                configSection.set(feature.getPath(), worlds);
            }
        }

        // Add default spawn coefficients
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && !configSection.contains("default-spawn-coef." + type.name())) {
                configSection.set("default-spawn-coef." + type.name(), 20);
            }
        }

        configuration.save();
    }

    public void refreshFeatures() {
        for (Feature feature : Feature.values()) {
            List<String> enabledWorld = getConfiguration().getConfig().getStringList(feature.getPath());
            feature.updateConfig();
        }
        if (eventManager != null) {
            eventManager.refresh();
            getLogger().info("Refreshed EventManager.");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getServer().getPluginManager().registerEvents(new CustomMobs(), this);
        getServer().getPluginManager().registerEvents(new Listener1(this), this);
        getServer().getPluginManager().registerEvents(new Listener2(), this);
       // getServer().getPluginManager().registerEvents(new CrafterInventory.Listener(), this);
    }

    private void hookIntoPlugins() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            getLogger().info("Hooked onto WorldGuard");
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SuddenDeathPlaceholders().register();
            getLogger().info("Hooked onto PlaceholderAPI");
        }
    }

    private void initializeDefaultMessages() {
        boolean saveNeeded = false;
        for (Message msg : Message.values()) {
            String key = msg.name().toLowerCase().replace("_", "-");
            if (!messages.getConfig().contains(key)) {
                messages.getConfig().set(key, msg.getValue());
                saveNeeded = true;
            }
        }
        if (saveNeeded) {
            messages.save();
        }
    }

    private void initializeFeaturesAndEntities() {
        eventManager = new EventManager();
        for (EntityType type : EntityType.values()) {
            if (type.isAlive()) {
                ConfigFile mobConfig = new ConfigFile(this, "/customMobs", Utils.lowerCaseId(type.name()));
                mobConfig.setup();
            }
        }

        Bukkit.getOnlinePlayers().forEach(PlayerData::setup);
        for (Feature feature : Feature.values()) {
            feature.updateConfig();
            ConfigFile modifiers = feature.getConfigFile();
            boolean saveNeeded = false;
            for (Modifier mod : feature.getModifiers()) {
                if (modifiers.getConfig().contains(mod.getName())) {
                    continue;
                }
                if (mod.getType() == Modifier.Type.NONE) {
                    modifiers.getConfig().set(mod.getName(), mod.getValue());
                    saveNeeded = true;
                } else if (mod.getType() == Modifier.Type.EACH_MOB) {
                    for (EntityType type : Utils.getLivingEntityTypes()) {
                        if (!modifiers.getConfig().contains(mod.getName() + "." + type.name())) {
                            modifiers.getConfig().set(mod.getName() + "." + type.name(), mod.getValue());
                            saveNeeded = true;
                        }
                    }
                }
            }
            if (saveNeeded) {
                modifiers.save();
            }
        }
    }

    private void initializeItemsAndRecipes() {
        boolean saveNeeded = false;
        for (CustomItem item : CustomItem.values()) {
            if (!items.getConfig().contains(item.name())) {
                items.getConfig().set(item.name() + ".name", item.getDefaultName());
                items.getConfig().set(item.name() + ".lore", item.getLore());
                if (item.getCraft() != null) {
                    items.getConfig().set(item.name() + ".craft-enabled", true);
                    items.getConfig().set(item.name() + ".craft", item.getCraft());
                } else {
                    items.getConfig().set(item.name() + ".craft-enabled", false);
                }
                saveNeeded = true;
            }
        }
        if (saveNeeded) {
            items.save();
        }

        removeCustomRecipes(); // Remove old recipes before registering new ones
        for (CustomItem item : CustomItem.values()) {
            ConfigurationSection section = items.getConfig().getConfigurationSection(item.name());
            if (section == null) {
                getLogger().log(Level.WARNING, "Configuration section is null for CustomItem: " + item.name());
                continue;
            }
            item.update(section);
            if (items.getConfig().getBoolean(item.name() + ".craft-enabled") && item.getCraft() != null) {
                try {
                    registerCraftingRecipe(item);
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.WARNING, "Failed to register recipe for " + item.name(), e);
                }
            }
        }
    }

    private void registerCraftingRecipe(CustomItem item) {
        if (item.getCraft() == null || item.getCraft().isEmpty()) {
            getLogger().log(Level.INFO, "No crafting recipe defined for " + item.name());
            return;
        }

        try {
            NamespacedKey recipeKey = new NamespacedKey(this, "suddendeath_" + item.name().toLowerCase());
            ShapedRecipe recipe = new ShapedRecipe(recipeKey, item.a());
            recipe.shape("ABC", "DEF", "GHI");
            char[] chars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};

            List<String> craftLines = item.getCraft();
            getLogger().info("Attempting to register recipe for " + item.name() + ": " + craftLines);

            if (craftLines.size() != 3) {
                throw new IllegalArgumentException("Invalid craft format for " + item.name() +
                        ". Expected 3 lines, got " + craftLines.size());
            }

            for (int i = 0; i < 9; i++) {
                String[] line = craftLines.get(i / 3).split(",");
                if (line.length != 3) {
                    throw new IllegalArgumentException("Invalid craft line format for " + item.name() +
                            ". Line " + (i / 3 + 1) + " has " + line.length + " elements, expected 3");
                }

                String materialName = line[i % 3].trim();
                if (materialName.equalsIgnoreCase("AIR") || materialName.isEmpty()) {
                    continue;
                }

                Material material = Material.getMaterial(materialName.toUpperCase());
                if (material == null) {
                    throw new IllegalArgumentException("Invalid material: " + materialName + " for " + item.name());
                }

                recipe.setIngredient(chars[i], material);
            }

            recipe.setGroup("suddendeath_items");
            getServer().addRecipe(recipe);
            getLogger().log(Level.INFO, "Successfully registered crafting recipe for " + item.name());
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to register recipe for " + item.name(), e);
        }
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
                ConfigFile file = new ConfigFile(this, "/userdata", data.getUniqueId().toString());
                data.save(file.getConfig());
                file.save();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to save player data for " + data.getUniqueId(), e);
            }
        });
    }

    private void removeCustomRecipes() {
        Iterator<Recipe> recipes = getServer().recipeIterator();
        List<NamespacedKey> toRemove = new ArrayList<>();

        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                if (key.getNamespace().equals(this.getName().toLowerCase()) &&
                        key.getKey().startsWith("suddendeath_")) {
                    toRemove.add(key);
                }
            }
        }

        for (NamespacedKey key : toRemove) {
            getServer().removeRecipe(key);
        }

        getLogger().info("Removed " + toRemove.size() + " old custom recipes");
    }

    private void reRegisterRecipes() {
        removeCustomRecipes();
        for (CustomItem item : CustomItem.values()) {
            if (items.getConfig().contains(item.name())) {
                ConfigurationSection section = items.getConfig().getConfigurationSection(item.name());
                if (section == null) {
                    getLogger().log(Level.WARNING, "Configuration section is null for CustomItem: " + item.name());
                    continue;
                }
                item.update(section);
                if (section.getBoolean("craft-enabled") && item.getCraft() != null) {
                    try {
                        registerCraftingRecipe(item);
                    } catch (IllegalArgumentException e) {
                        getLogger().log(Level.WARNING, "Failed to register recipe for " + item.name(), e);
                    }
                }
            }
        }
    }

    public void reloadConfigFiles() {
        try {
            savePlayerData();

            configuration.reload();
            messages.reload();
            items.reload();

            // Reload custom mob configs
            for (EntityType type : EntityType.values()) {
                if (type.isAlive()) {
                    ConfigFile mobConfig = new ConfigFile(this, "/customMobs", Utils.lowerCaseId(type.name()));
                    mobConfig.reload();
                }
            }

            // Reinitialize config to ensure default values
            initializeConfigFiles();
            refreshFeatures();
            reRegisterRecipes();
            Bukkit.getOnlinePlayers().forEach(PlayerData::setup);
            getLogger().info("Reload all configuration successfully.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading configuration file", e);
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
}