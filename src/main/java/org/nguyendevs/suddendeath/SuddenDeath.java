package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
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
import org.nguyendevs.suddendeath.gui.listener.GuiListener;
import org.nguyendevs.suddendeath.listener.*;
import org.nguyendevs.suddendeath.manager.EventManager;
import org.nguyendevs.suddendeath.packets.PacketSender;
import org.nguyendevs.suddendeath.packets.v1_17.ProtocolLibImpl;
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
    private ConfigFile configuration;
    private PacketSender packetSender;
    private WGPlugin wgPlugin;
    private EventManager eventManager;
    public ConfigFile messages;
    public ConfigFile items;

    public static SuddenDeath getInstance() {
        return instance;
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
        getServer().getScheduler().runTaskTimer(this, new BloodEffectRunnable(this), 0L, 1L);

        new SpigotPlugin(119526, this).checkForUpdate();
    }

    private void initializeConfigFiles() {
        messages = new ConfigFile(this, "/language", "messages");
        items = new ConfigFile(this, "/language", "items");

        initializeDefaultMessages();

        for (Feature feature : Feature.values()) {
            if (!configuration.getConfig().contains(feature.getPath())) {
                List<String> worlds = new ArrayList<>();
                Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
                configuration.getConfig().set(feature.getPath(), worlds);
            }
        }

        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && !configuration.getConfig().contains("default-spawn-coef." + type.name())) {
                configuration.getConfig().set("default-spawn-coef." + type.name(), 20);
            }
        }
        configuration.save();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BloodEffectListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getServer().getPluginManager().registerEvents(new CustomMobs(), this);
        getServer().getPluginManager().registerEvents(new Listener1(), this);
        getServer().getPluginManager().registerEvents(new Listener2(), this);
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

        removeCustomRecipes(); // Gỡ bỏ các công thức cũ trước khi đăng ký lại
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

            // Tải lại các file trong /customMobs
            for (EntityType type : EntityType.values()) {
                if (type.isAlive()) {
                    ConfigFile mobConfig = new ConfigFile(this, "/customMobs", Utils.lowerCaseId(type.name()));
                    mobConfig.reload();
                }
            }

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