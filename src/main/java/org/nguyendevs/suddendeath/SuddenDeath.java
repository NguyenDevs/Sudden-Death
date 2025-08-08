package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.*;
import org.bukkit.command.defaults.ReloadCommand;
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
import org.nguyendevs.suddendeath.comp.worldguard.CustomFlag;
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
import org.nguyendevs.suddendeath.util.*;

import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SuddenDeath extends JavaPlugin {
    private static SuddenDeath instance;
    private final Map<Player, Integer> players = new ConcurrentHashMap<>();
    private ConfigFile configuration;
    private ConfigFile features;
    private PacketSender packetSender;
    private WGPlugin wgPlugin;
    private EventManager eventManager;
    private boolean worldGuardReady = false;
    public ConfigFile messages;
    public ConfigFile items;
    private static final Map<String, String> DEFAULT_MATERIAL_NAMES = new HashMap<>();

    static {
        DEFAULT_MATERIAL_NAMES.put("PAPER", "&aPaper");
        DEFAULT_MATERIAL_NAMES.put("STICK", "&aStick");
        DEFAULT_MATERIAL_NAMES.put("GLOW_INK_SAC", "&aGlow Ink Sac");
        DEFAULT_MATERIAL_NAMES.put("BOWL", "&aBowl");
        DEFAULT_MATERIAL_NAMES.put("BROWN_MUSHROOM", "&aBrown Mushroom");
    }

    @Override
    public void onLoad() {
        instance = this;
        configuration = new ConfigFile(this, "config");
        registerWorldGuardFlags();
    }

    @Override
    public void onEnable() {
        try {
            initializePlugin();
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aSuddenDeath plugin enabled successfully!"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SuddenDeath plugin.", e);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        try {
            removeCustomRecipes();
            savePlayerData();
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &cSuddenDeath plugin disabled.!"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while disabling plugin", e);
        }
    }

    private void initializePlugin() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &6ProtocolLib is unavailable, stopping..."));
            throw new IllegalStateException("ProtocolLib is required but not found");
        }
        packetSender = new ProtocolLibImpl(protocolManager);
        configuration.reload();
        initializeWorldGuard();
        initializeConfigFiles();
        registerListeners();
        hookIntoPlaceholderAPI();
        initializeFeaturesAndEntities();
        initializeItemsAndRecipes();
        registerCommands();
        printLogo();
        new SpigotPlugin(119526, this).checkForUpdate();
    }

    private void initializeConfigFiles() {
        messages = new ConfigFile(this, "/language", "messages");
        items = new ConfigFile(this, "/language", "items");
        features = new ConfigFile(this, "/language","feature");
        initializeDefaultMessages();
        initializeDefaultItems();
        initializeDefaultFeatures();
        FileConfiguration defaultConfig = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(getResource("config.yml")))) {
            defaultConfig.load(reader);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to load default config.yml from resources", e);
        }
        ConfigurationSection configSection = configuration.getConfig();
        if (!configSection.contains("update-notify")) {
            configSection.set("update-notify", defaultConfig.getBoolean("update-notify", true));
        }
        for (Feature feature : Feature.values()) {
            if (!configSection.contains(feature.getPath())) {
                List<String> worlds = new ArrayList<>();
                Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
                configSection.set(feature.getPath(), worlds);
            }
        }
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && !configSection.contains("default-spawn-coef." + type.name())) {
                configSection.set("default-spawn-coef." + type.name(), 20);
            }
        }
        configuration.save();
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

    private void initializeDefaultItems() {
        items.setup();
        boolean saveNeeded = false;
        for (CustomItem item : CustomItem.values()) {
            String itemKey = item.name();
            ConfigurationSection section = items.getConfig().getConfigurationSection(itemKey);
            if (section == null) {
                section = items.getConfig().createSection(itemKey);
                section.set("name", item.getDefaultName());
                section.set("lore", item.getLore());
                section.set("craft-enabled", item.getCraft() != null);
                if (item.getCraft() != null) {
                    section.set("craft", item.getCraft());
                    List<String> materials = new ArrayList<>();
                    Set<String> uniqueMaterials = new HashSet<>();
                    for (String row : item.getCraft()) {
                        for (String material : row.split(",")) {
                            String trimmedMaterial = material.trim().toUpperCase();
                            if (!trimmedMaterial.equals("AIR") && !uniqueMaterials.contains(trimmedMaterial)) {
                                String displayName = DEFAULT_MATERIAL_NAMES.getOrDefault(
                                        trimmedMaterial,
                                        "&a" + trimmedMaterial.replace("_", " ")
                                );
                                materials.add(trimmedMaterial + ": " + displayName);
                                uniqueMaterials.add(trimmedMaterial);
                            }
                        }
                    }
                    section.set("materials", materials);
                }
                saveNeeded = true;
            } else {
                if (!section.contains("name")) {
                    section.set("name", item.getDefaultName());
                    saveNeeded = true;
                }
                if (!section.contains("lore")) {
                    section.set("lore", item.getLore());
                    saveNeeded = true;
                }
                if (!section.contains("craft-enabled")) {
                    section.set("craft-enabled", item.getCraft() != null);
                    saveNeeded = true;
                }
                if (item.getCraft() != null && !section.contains("craft")) {
                    section.set("craft", item.getCraft());
                    saveNeeded = true;
                }
                if (item.getCraft() != null && !section.contains("materials")) {
                    List<String> materials = new ArrayList<>();
                    Set<String> uniqueMaterials = new HashSet<>();
                    for (String row : item.getCraft()) {
                        for (String material : row.split(",")) {
                            String trimmedMaterial = material.trim().toUpperCase();
                            if (!trimmedMaterial.equals("AIR") && !uniqueMaterials.contains(trimmedMaterial)) {
                                String displayName = DEFAULT_MATERIAL_NAMES.getOrDefault(
                                        trimmedMaterial,
                                        "&a" + trimmedMaterial.replace("_", " ")
                                );
                                materials.add(trimmedMaterial + ": " + displayName);
                                uniqueMaterials.add(trimmedMaterial);
                            }
                        }
                    }
                    section.set("materials", materials);
                    saveNeeded = true;
                }
            }
        }
        if (saveNeeded) {
            items.save();
        }
    }

    private void initializeDefaultFeatures() {
        features.setup();
        boolean saveNeeded = false;

        for (Feature feature : Feature.values()) {
            String featureKey = feature.getPath();
            ConfigurationSection section = features.getConfig().getConfigurationSection("features." + featureKey);
            if (section == null) {
                section = features.getConfig().createSection("features." + featureKey);
                section.set("name", feature.getName());
                section.set("lore", feature.getLore());
                saveNeeded = true;
            } else {
                if (!section.contains("name")) {
                    section.set("name", feature.getName());
                    saveNeeded = true;
                }
                if (!section.contains("lore")) {
                    section.set("lore", feature.getLore());
                    saveNeeded = true;
                }
                List<String> existingLore = section.getStringList("lore");
                if (existingLore.isEmpty()) {
                    section.set("lore", feature.getLore());
                    saveNeeded = true;
                }
            }
        }
        if (saveNeeded) {
            features.save();
        }
        Feature.reloadDescriptions();
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
        initializeDefaultItems();
        removeCustomRecipes();
        for (CustomItem item : CustomItem.values()) {
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

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getServer().getPluginManager().registerEvents(new CustomMobs(), this);
        getServer().getPluginManager().registerEvents(new Listener1(this), this);
        getServer().getPluginManager().registerEvents(new Listener2(), this);
        getServer().getPluginManager().registerEvents(new Listener3(this), this);
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

    private void hookIntoPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SuddenDeathPlaceholders().register();
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aHooked onto PlaceholderAPI"));
        }
    }

    private void registerWorldGuardFlags() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            return;
        }
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            for (CustomFlag customFlag : CustomFlag.values()) {
                String flagPath = customFlag.getPath();
                try {
                    if (registry.get(flagPath) != null) {
                        continue;
                    }
                    boolean defaultState = customFlag == CustomFlag.SDS_REMOVE ? false : true;
                    StateFlag flag = new StateFlag(flagPath, defaultState);
                    registry.register(flag);
                } catch (FlagConflictException e) {
                    getLogger().warning("Flag conflict while registering: " + flagPath + " - " + e.getMessage());
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Unexpected error while registering WorldGuard flag: " + flagPath, e);
                }
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aWorldGuard flag registration completed."));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register WorldGuard flags during onLoad", e);
        }
    }

    private void initializeWorldGuard() {
        try {
            if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                org.bukkit.plugin.Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aWorldGuard version: " + wgPlugin.getDescription().getVersion()));
                try {
                    this.wgPlugin = new WorldGuardOn();
                    if (this.wgPlugin instanceof WorldGuardOn) {
                        WorldGuardOn wgOn = (WorldGuardOn) this.wgPlugin;
                        boolean isReady = wgOn.isReady();
                        int flagCount = wgOn.getRegisteredFlags().size();
                        if (isReady && flagCount > 0) {
                            worldGuardReady = true;
                            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&6[&cSudden&4Death&6] &aWorldGuard integration ready immediately with " +
                                            flagCount + " custom flags: " + String.join(", ", wgOn.getRegisteredFlags().keySet())));
                        } else {
                            getServer().getScheduler().runTaskLater(this, () -> {
                                boolean delayedReady = wgOn.isReady();
                                int delayedFlagCount = wgOn.getRegisteredFlags().size();
                                if (delayedReady && delayedFlagCount > 0) {
                                    worldGuardReady = true;
                                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            "&6[&cSudden&4Death&6] &aWorldGuard integration read with " +
                                                    delayedFlagCount + " custom flags"));
                                } else {
                                    getLogger().severe("WorldGuard integration failed - flags not loaded properly");
                                    getLogger().severe("Ready: " + delayedReady + ", Flag count: " + delayedFlagCount);
                                    worldGuardReady = false;
                                }
                            }, 40L);
                            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&6[&cSudden&4Death&6] &6WorldGuard integration created, waiting for flags to load..."));
                        }
                    } else {
                        throw new IllegalStateException("WorldGuardOn instance creation failed");
                    }
                } catch (IllegalStateException e) {
                    getLogger().severe("Failed to initialize WorldGuardOn - " + e.getMessage());
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[&cSudden&4Death&6] &cWorldGuardOn failed: " + e.getMessage()));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[&cSudden&4Death&6] &6Falling FIback to WorldGuardOff mode"));
                    this.wgPlugin = new WorldGuardOff();
                    worldGuardReady = true;
                } catch (NoClassDefFoundError e) {
                    getLogger().severe("WorldGuard classes not found - " + e.getMessage());
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[&cSudden&4Death&6] &cMissing WorldGuard dependencies"));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[&cSudden&4Death&6] &6Falling back to WorldGuardOff mode"));
                    this.wgPlugin = new WorldGuardOff();
                    worldGuardReady = true;
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Unexpected error initializing WorldGuardOn", e);
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[&cSudden&4Death&6] &cUnexpected error: " + e.getClass().getSimpleName()));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[&cSudden&4Death&6] &6Falling back to WorldGuardOff mode"));
                    this.wgPlugin = new WorldGuardOff();
                    worldGuardReady = true;
                }
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6[&cSudden&4Death&6] &6WorldGuard not found, using fallback mode"));
                this.wgPlugin = new WorldGuardOff();
                worldGuardReady = true;
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize WorldGuard integration", e);
            this.wgPlugin = new WorldGuardOff();
            worldGuardReady = true;
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &cForced fallback to WorldGuardOff due to initialization error"));
        }
    }

    public boolean isWorldGuardReady() {
        boolean ready = worldGuardReady && wgPlugin != null;
        if (!ready) {
            getLogger().fine("WorldGuard not ready - worldGuardReady: " + worldGuardReady +
                    ", wgPlugin: " + (wgPlugin != null ? wgPlugin.getClass().getSimpleName() : "null"));
        }
        return ready;
    }

    public WGPlugin getWorldGuard() {
        if (wgPlugin == null) {
            getLogger().warning("WorldGuard plugin requested but not initialized, returning fallback");
            return new WorldGuardOff();
        }
        if (getLogger().isLoggable(Level.FINE)) {
            String wgType = wgPlugin instanceof WorldGuardOn ? "WorldGuardOn" : "WorldGuardOff";
            getLogger().fine("WorldGuard requested, returning: " + wgType);
        }
        return wgPlugin;
    }

    public boolean canUseWorldGuardFlags() {
        boolean canUse = isWorldGuardReady() &&
                wgPlugin instanceof WorldGuardOn &&
                ((WorldGuardOn) wgPlugin).isReady();
        if (!canUse && getLogger().isLoggable(Level.FINE)) {
            if (!isWorldGuardReady()) {
                getLogger().fine("Cannot use WG flags - WorldGuard not ready");
            } else if (!(wgPlugin instanceof WorldGuardOn)) {
                getLogger().fine("Cannot use WG flags - Using WorldGuardOff");
            } else if (!((WorldGuardOn) wgPlugin).isReady()) {
                getLogger().fine("Cannot use WG flags - WorldGuardOn not ready");
            }
        }
        return canUse;
    }

    private void registerCraftingRecipe(CustomItem item) {
        if (item.getCraft() == null || item.getCraft().isEmpty()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &6No crafting recipe defined for " + item.name()));
            return;
        }
        try {
            NamespacedKey recipeKey = new NamespacedKey(this, "suddendeath_" + item.name().toLowerCase());
            ShapedRecipe recipe = new ShapedRecipe(recipeKey, item.a());
            recipe.shape("ABC", "DEF", "GHI");
            char[] chars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
            List<String> craftLines = item.getCraft();
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
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to register recipe for " + item.name(), e);
        }
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

    public void refreshFeatures() {
        for (Feature feature : Feature.values()) {
            List<String> enabledWorld = getConfiguration().getConfig().getStringList(feature.getPath());
            feature.updateConfig();
        }
        if (eventManager != null) {
            eventManager.refresh();
        }
    }

    public void reloadConfigFiles() {
        try {
            savePlayerData();
            configuration.reload();
            messages.reload();
            items.reload();
            features.reload();
            for (EntityType type : EntityType.values()) {
                if (type.isAlive()) {
                    ConfigFile mobConfig = new ConfigFile(this, "/customMobs", Utils.lowerCaseId(type.name()));
                    mobConfig.setup();
                }
            }
            initializeConfigFiles();
            refreshFeatures();
            reRegisterRecipes();
            if (wgPlugin instanceof WorldGuardOn) {
                WorldGuardOn wgOn = (WorldGuardOn) wgPlugin;
            }
            Bukkit.getOnlinePlayers().forEach(PlayerData::setup);
            Feature.reloadDescriptions(); // Tải lại mô tả của các tính năng
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aConfiguration reload completed successfully."));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading configuration files", e);
        }
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
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4         Sudden Death"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6         Version " + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b         Development by NguyenDevs"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }

    public static SuddenDeath getInstance() {
        return instance;
    }

    public Map<Player, Integer> getPlayers() {
        return players;
    }

    public ConfigFile getConfiguration() {
        return configuration;
    }

    public ConfigFile getFeaturesConfig() {
        return features;
    }

    public PacketSender getPacketSender() {
        return packetSender;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}