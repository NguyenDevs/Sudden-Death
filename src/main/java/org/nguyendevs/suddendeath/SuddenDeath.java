package org.nguyendevs.suddendeath;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SuddenDeath extends JavaPlugin {
    // plugin data
    private final Map<Player, Integer> players = new ConcurrentHashMap<>();

    private final PluginConfiguration configuration = new PluginConfiguration((Plugin)this);


    private PacketSender packetSender;

    public static SuddenDeath plugin;
    private WGPlugin wgPlugin;
    private EventManager eventManager;

    public ConfigFile messages, difficulties, items;
    public Difficulty defaultDifficulty;
    private static SuddenDeath instance;
    public static Plugin getInstance() {
        return instance;
    }

    public void onLoad() {
        plugin = this;
        wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard") != null ? new WorldGuardOn() : new WorldGuardOff();
    }
    public Map<Player, Integer> getPlayers() {
        return this.players;
    }

    public PluginConfiguration getConfiguration() {
        return this.configuration;
    }
    private void setPackerSender(ProtocolManager manager) {
        double subversion = Double.parseDouble("21.1");
        if (subversion == 21.1D) {
            this.packetSender = (PacketSender)new ProtocolLibImpl(manager);
        }
    }
    /*
     private void setPackerSender(ProtocolManager manager) {
        String version = getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        double subversion = Double.parseDouble(version.replace("v1_", "").replaceAll("_R", "."));
        if (subversion >= 17.0D) {
            this.packetSender = (PacketSender)new ProtocolLibImpl(manager);
        } else if (subversion >= 8.0D) {
            this.packetSender = (PacketSender)new LegacyProtocolLibImpl(manager);
        } else {
            throw new UnsupportedOperationException("Your Minecraft version is unsupported by this plugin");
        }
    }*/

    public PacketSender getPacketSender() {
        return this.packetSender;
    }
    public void onDisable() {
        PlayerData.getLoaded().forEach(data -> {
            ConfigFile file = new ConfigFile("/userdata", data.getUniqueId().toString());
            data.save(file.getConfig());
            file.save();
        });
    }

    public void onEnable() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            getLogger().warning("ProtocolLib is unavailable, stopping...");
            setEnabled(false);
        }
        setPackerSender(protocolManager);
        this.configuration.load();
        getServer().getPluginManager().registerEvents((Listener)new BloodEffectListener(this), (Plugin)this);
        getServer().getScheduler().runTaskTimer((Plugin)this, (Runnable)new BloodEffectRunnable(this), 0L, 1L);

        instance = this;
        saveDefaultConfig();
        reloadConfig();
        new SpigotPlugin(119526, this).checkForUpdate();

        new Metrics(this);
        eventManager = new EventManager();
        Bukkit.getServer().getPluginManager().registerEvents(new GuiListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new MainListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new CustomMobs(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Listener1(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new Listener2(), this);

        // worldguard flags
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null)
            getLogger().log(Level.INFO, "Hooked onto WorldGuard");

        // placeholderapi
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SuddenDeathPlaceholders().register();
            getLogger().log(Level.INFO, "Hooked onto PlaceholderAPI");
        }

        // initialize config
        saveDefaultConfig();
        reloadConfig();
        for (Feature feature : Feature.values())
            if (!getConfig().contains(feature.getPath())) {
                List<String> list = new ArrayList<>();
                Bukkit.getWorlds().forEach(world -> list.add(world.getName()));
                getConfig().set(feature.getPath(), list);
            }
        for (EntityType type : EntityType.values())
            if (type.isAlive()) {
                if (!getConfig().contains("default-spawn-coef")) {
                    getConfig().set("default-spawn-coef." + type.name(), 20);
                    continue;
                }
                if (!Objects.requireNonNull(getConfig().getConfigurationSection("default-spawn-coef")).contains(type.name()))
                    getConfig().set("default-spawn-coef." + type.name(), 20);
            }
        saveConfig();

        for (EntityType type : EntityType.values())
            if (type.isAlive())
                new ConfigFile("/customMobs", Utils.lowerCaseId(type.name())).setup();

        Bukkit.getOnlinePlayers().forEach(PlayerData::setup);

        // difficulties
        difficulties = new ConfigFile("/language", "difficulties");
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

        // default difficulty
        if (!getConfig().getBoolean("disable-difficulties")) {
            Bukkit.getServer().getPluginManager().registerEvents(new DifficultiesListener(), this);
            try {
                defaultDifficulty = Difficulty.valueOf(getConfig().getString("default-difficulty"));
            } catch (Exception e) {
                defaultDifficulty = Difficulty.SANDBOX;
                getLogger().log(Level.WARNING, "Could not read default difficulty.");
            }
        }

        // messages
        messages = new ConfigFile("/language", "messages");
        for (Message pa : Message.values()) {
            String path = pa.name().toLowerCase().replace("_", "-");
            if (!messages.getConfig().contains(path))
                messages.getConfig().set(path, pa.value);
        }
        messages.save();

        // params
        for (Feature feature : Feature.values()) {
            feature.updateConfig();
            ConfigFile modifiers = feature.getConfigFile();
            for (Modifier mod : feature.getModifiers()) {
                if (modifiers.getConfig().contains(mod.getName()))
                    continue;

                if (mod.getType() == Modifier.Type.NONE)
                    modifiers.getConfig().set(mod.getName(), mod.getDefaultValue());
                if (mod.getType() == Modifier.Type.EACH_MOB)
                    for (EntityType type : Utils.getLivingEntityTypes())
                        modifiers.getConfig().set(mod.getName() + "." + type.name(), mod.getDefaultValue());
            }
            modifiers.save();
        }

        // items initialize
        items = new ConfigFile("/language", "items");
        for (CustomItem i : CustomItem.values()) {
            if (!items.getConfig().contains(i.name())) {
                items.getConfig().set(i.name() + ".name", i.getDefaultName());
                items.getConfig().set(i.name() + ".lore", Arrays.asList(i.lore));
                if (i.craft != null) {
                    items.getConfig().set(i.name() + ".craft-enabled", true);
                    items.getConfig().set(i.name() + ".craft", Arrays.asList(i.craft));
                }
            }

            i.update(Objects.requireNonNull(items.getConfig().getConfigurationSection(i.name())));

            // crafting recipes
            if (items.getConfig().getBoolean(i.name() + ".craft-enabled") && i.craft != null) {
                ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "SuddenDeath_" + i.name()), i.a());
                recipe.shape(new String[]{"ABC", "DEF", "GHI"});
                char[] chars = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
                boolean check = true;
                List<String> list = items.getConfig().getStringList(i.name() + ".craft");
                for (int j = 0; j < 9; j++) {
                    char c = chars[j];
                    if (list.size() != 3) {
                        getLogger().log(Level.WARNING, "Couldn't register recipe of " + i.name() + " (format error)");
                        check = false;
                        break;
                    }

                    List<String> line = Arrays.asList(list.get(j / 3).split(","));
                    if (line.size() < 3) {
                        getLogger().log(Level.WARNING, "Couldn't register recipe of " + i.name() + " (format error)");
                        check = false;
                        break;
                    }

                    String s = line.get(j % 3);
                    Material material = null;
                    try {
                        material = Material.valueOf(s);
                    } catch (Exception e1) {
                        getLogger().log(Level.WARNING, "Couldn't register recipe of " + i.name() + " (" + s + " is not a valid material)");
                        check = false;
                        break;
                    }

                    if (material == Material.AIR)
                        continue;

                    recipe.setIngredient(c, material);
                }
                if (check)
                    getServer().addRecipe(recipe);
            }
        }
        items.save();

        // commands
        Objects.requireNonNull(getCommand("sdstatus")).setExecutor(new SuddenDeathStatusCommand());
        Objects.requireNonNull(getCommand("sdmob")).setExecutor(new SuddenDeathMobCommand());

        Objects.requireNonNull(getCommand("sdstatus")).setTabCompleter(new SuddenDeathStatusCompletion());
        Objects.requireNonNull(getCommand("sdmob")).setTabCompleter(new SuddenDeathMobCompletion());
    }

    public void reloadConfigFiles() {

        messages = new ConfigFile("/language", "messages");
        items = new ConfigFile("/language", "items");
        difficulties = new ConfigFile("/language", "difficulties");
        saveDefaultConfig();
        reloadConfig();
    }
    public WGPlugin getWorldGuard() {
        return wgPlugin;
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}