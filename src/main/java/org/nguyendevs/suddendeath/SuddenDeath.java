package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.nguyendevs.suddendeath.features.base.IFeature;
import org.nguyendevs.suddendeath.features.combat.ArrowSlowFeature;
import org.nguyendevs.suddendeath.features.combat.MobCriticalStrikesFeature;
import org.nguyendevs.suddendeath.features.combat.SharpKnifeFeature;
import org.nguyendevs.suddendeath.features.items.AdvancedPlayerDropsFeature;
import org.nguyendevs.suddendeath.features.items.PhysicEnderPearlFeature;
import org.nguyendevs.suddendeath.features.items.RealisticPickupFeature;
import org.nguyendevs.suddendeath.features.mob.attributes.ArmorPiercingFeature;
import org.nguyendevs.suddendeath.features.mob.hostile.ZombieToolsFeature;
import org.nguyendevs.suddendeath.features.mob.attributes.ForceOfUndeadFeature;
import org.nguyendevs.suddendeath.features.mob.attributes.QuickMobsFeature;
import org.nguyendevs.suddendeath.features.mob.attributes.TankyMonstersFeature;
import org.nguyendevs.suddendeath.features.mob.hostile.*;
import org.nguyendevs.suddendeath.features.nether.NetherShieldFeature;
import org.nguyendevs.suddendeath.features.player.*;
import org.nguyendevs.suddendeath.features.world.DangerousCoalFeature;
import org.nguyendevs.suddendeath.features.world.FreddyFeature;
import org.nguyendevs.suddendeath.features.world.SnowSlowFeature;
import org.nguyendevs.suddendeath.features.world.WhispersOfTheDesertFeature;
import org.nguyendevs.suddendeath.gui.AdminView;
import org.nguyendevs.suddendeath.gui.PlayerView;
import org.nguyendevs.suddendeath.gui.PluginInventory;
import org.nguyendevs.suddendeath.gui.listener.GuiListener;
import org.nguyendevs.suddendeath.features.CustomMobs;
import org.nguyendevs.suddendeath.manager.EventManager;
import org.nguyendevs.suddendeath.manager.RecipeRegistrationManager;
import org.nguyendevs.suddendeath.player.Modifier;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.*;
import org.nguyendevs.suddendeath.listener.RecipeDiscoveryListener;

import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SuddenDeath extends JavaPlugin {
    private static SuddenDeath instance;
    private final Map<Player, Integer> players = new ConcurrentHashMap<>();
    private ConfigFile configuration;
    private ConfigFile features;
    private WGPlugin wgPlugin;
    private EventManager eventManager;
    private boolean worldGuardReady = false;
    public ConfigFile messages;
    public ConfigFile items;
    private final Map<NamespacedKey, Integer> recipeCache = new HashMap<>();

    private RecipeRegistrationManager recipeManager;

    private final List<IFeature> loadedFeatures = new ArrayList<>();
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
            for (IFeature feature : loadedFeatures) {
                feature.shutdown();
            }
            loadedFeatures.clear();

            if (recipeManager != null) {
                recipeManager.unregisterAllRecipes();
            }

            savePlayerData();
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &cSuddenDeath plugin disabled.!"));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while disabling plugin", e);
        }
    }

    private void initializePlugin() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            throw new IllegalStateException("ProtocolLib is required but not found");
        }

        configuration.reload();
        recipeManager = new RecipeRegistrationManager(this);

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
        features = new ConfigFile(this, "/language", "feature");
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

        recipeManager.registerAllRecipes();

        getLogger().info("Items and recipes initialized successfully!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new RecipeDiscoveryListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomMobs(), this);

        // Feature Registration
        registerFeature(new PlayerCoreFeature());

        registerFeature(new BlazeFeatures());
        registerFeature(new BreezeFeature());
        registerFeature(new CreeperFeature());
        registerFeature(new DrownedFeature());
        registerFeature(new EnderFeatures());
        registerFeature(new EvokerFeature());
        registerFeature(new GuardianFeature());
        registerFeature(new PhantomFeature());
        registerFeature(new SilverfishFeature());
        registerFeature(new SkeletonFeatures());
        registerFeature(new SlimeFeatures());
        registerFeature(new SpiderFeatures());
        registerFeature(new StrayFeature());
        registerFeature(new WitchFeature());
        registerFeature(new WitherSkeletonFeature());
        registerFeature(new UndeadRageFeature());
        registerFeature(new UndeadGunnersFeature());
        registerFeature(new ZombieBreakBlockFeature());

        registerFeature(new ForceOfUndeadFeature());
        registerFeature(new QuickMobsFeature());
        registerFeature(new TankyMonstersFeature());
        registerFeature(new ArmorPiercingFeature());

        registerFeature(new ArrowSlowFeature());
        registerFeature(new MobCriticalStrikesFeature());
        registerFeature(new SharpKnifeFeature());

        registerFeature(new AdvancedPlayerDropsFeature());
        registerFeature(new BleedingFeature());
        registerFeature(new BloodScreenFeature());
        registerFeature(new DangerousCoalFeature());
        registerFeature(new ElectricityShockFeature());
        registerFeature(new FallStunFeature());
        registerFeature(new PillagerFireWorkFeature());
        registerFeature(new FreddyFeature());
        registerFeature(new HungerNauseaFeature());
        registerFeature(new InfectionFeature());
        registerFeature(new NetherShieldFeature());
        registerFeature(new PhysicEnderPearlFeature());
        registerFeature(new RealisticPickupFeature());
        registerFeature(new SnowSlowFeature());
        registerFeature(new StoneStiffnessFeature());
        registerFeature(new ZombieToolsFeature());
        registerFeature(new WhispersOfTheDesertFeature());
        //registerFeature(new ZombiePlaceFeature());
    }

    private void registerFeature(IFeature feature) {
        feature.initialize(this);
        loadedFeatures.add(feature);
    }

    private void registerCommands() {
        Optional.ofNullable(getCommand("sds")).ifPresent(cmd -> {
            cmd.setExecutor(new SuddenDeathStatusCommand());
            cmd.setTabCompleter(new SuddenDeathStatusCompletion());
        });
        Optional.ofNullable(getCommand("sdm")).ifPresent(cmd -> {
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
                                    worldGuardReady = false;
                                }
                            }, 40L);
                            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&6[&cSudden&4Death&6] &6WorldGuard integration created, waiting for flags to load..."));
                        }
                    } else {
                        throw new IllegalStateException("WorldGuardOn instance creation failed");
                    }
                } catch (Exception e) {
                    getLogger().severe("Failed to initialize WorldGuardOn - " + e.getMessage());
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
        }
    }

    public boolean isWorldGuardReady() {
        return worldGuardReady && wgPlugin != null;
    }

    public WGPlugin getWorldGuard() {
        if (wgPlugin == null) {
            return new WorldGuardOff();
        }
        return wgPlugin;
    }

    public void refreshFeatures() {
        for (Feature feature : Feature.values()) {
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

            recipeManager.reloadRecipes();

            Bukkit.getOnlinePlayers().forEach(PlayerData::setup);
            Feature.reloadDescriptions();

            Bukkit.getScheduler().runTask(this, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getOpenInventory() != null &&
                            player.getOpenInventory().getTopInventory().getHolder() instanceof PluginInventory pluginInventory) {
                        if (pluginInventory instanceof AdminView || pluginInventory instanceof PlayerView) {
                            player.closeInventory();
                            Bukkit.getScheduler().runTaskLater(this, pluginInventory::open, 10L);
                        }
                    }
                }
            });
            for (Player player : Bukkit.getOnlinePlayers()) {
                getRecipeManager().discoverAllRecipesForPlayer(player);
            }
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[&cSudden&4Death&6] &aConfiguration reload completed successfully."));
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


    public RecipeRegistrationManager getRecipeManager() {
        return recipeManager;
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

    public EventManager getEventManager() {
        return eventManager;
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
}