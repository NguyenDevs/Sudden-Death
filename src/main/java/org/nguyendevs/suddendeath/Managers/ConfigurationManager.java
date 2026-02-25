package org.nguyendevs.suddendeath.Managers;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.Material;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.*;

import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;

public class ConfigurationManager {
    private final SuddenDeath plugin;
    public ConfigFile messages;
    public ConfigFile items;
    public ConfigFile features;
    private final ConfigFile mainConfig;
    private final Map<NamespacedKey, Integer> recipeCache = new HashMap<>();
    private final Map<EntityType, ConfigFile> mobConfigs = new HashMap<>();

    private static final Map<String, String> DEFAULT_MATERIAL_NAMES = new HashMap<>();

    static {
        DEFAULT_MATERIAL_NAMES.put("PAPER", "&aPaper");
        DEFAULT_MATERIAL_NAMES.put("STICK", "&aStick");
        DEFAULT_MATERIAL_NAMES.put("GLOW_INK_SAC", "&aGlow Ink Sac");
        DEFAULT_MATERIAL_NAMES.put("BOWL", "&aBowl");
        DEFAULT_MATERIAL_NAMES.put("BROWN_MUSHROOM", "&aBrown Mushroom");
    }

    public ConfigurationManager(SuddenDeath plugin) {
        this.plugin = plugin;
        this.mainConfig = new ConfigFile(plugin, "config");
    }

    public void initialize() {
        mainConfig.reload();
        messages = new ConfigFile(plugin, "/language", "messages");
        items = new ConfigFile(plugin, "/language", "items");
        features = new ConfigFile(plugin, "/language", "feature");

        initializeDefaultMessages();
        initializeDefaultItems();
        initializeDefaultFeatures();

        loadDefaultConfig();
        loadMobConfigs();

        plugin.items = this.items;
        plugin.messages = this.messages;

        initializeItemsAndRecipes();
    }

    public ConfigFile getMainConfig() {
        return mainConfig;
    }

    public ConfigFile getMobConfig(EntityType type) {
        return mobConfigs.get(type);
    }

    public void reloadAll() {
        mainConfig.reload();
        messages.reload();
        items.reload();
        features.reload();

        loadMobConfigs();

        initializeDefaultMessages();
        initializeDefaultItems();
        initializeDefaultFeatures();

        initializeItemsAndRecipes();
        Feature.reloadDescriptions();
    }

    private void loadMobConfigs() {
        mobConfigs.clear();
        for (EntityType type : EntityType.values()) {
            if (type.isAlive()) {
                ConfigFile mobConfig = new ConfigFile(plugin, "/customMobs", Utils.lowerCaseId(type.name()));
                mobConfig.setup();
                mobConfigs.put(type, mobConfig);
            }
        }
    }

    private void loadDefaultConfig() {
        FileConfiguration defaultConfig = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(plugin.getResource("config.yml")))) {
            defaultConfig.load(reader);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load default config.yml", e);
        }
        ConfigurationSection configSection = mainConfig.getConfig();
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
        mainConfig.save();
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
        if (saveNeeded)
            messages.save();
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
                    writeMaterials(section, item);
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
                    writeMaterials(section, item);
                    saveNeeded = true;
                }
            }
        }
        if (saveNeeded)
            items.save();
    }

    private void writeMaterials(ConfigurationSection section, CustomItem item) {
        List<String> materials = new ArrayList<>();
        Set<String> uniqueMaterials = new HashSet<>();
        for (String row : item.getCraft()) {
            for (String material : row.split(",")) {
                String trimmed = material.trim().toUpperCase();
                if (!trimmed.equals("AIR") && !uniqueMaterials.contains(trimmed)) {
                    String displayName = DEFAULT_MATERIAL_NAMES.getOrDefault(
                            trimmed,
                            "&a" + trimmed.replace("_", " "));
                    materials.add(trimmed + ": " + displayName);
                    uniqueMaterials.add(trimmed);
                }
            }
        }
        section.set("materials", materials);
    }

    private void initializeDefaultFeatures() {
        features.setup();
        boolean saveNeeded = false;
        for (Feature feature : Feature.values()) {
            String featureKey = feature.getPath();
            if (!features.getConfig().contains("features." + featureKey)) {
                ConfigurationSection section = features.getConfig().createSection("features." + featureKey);
                section.set("name", feature.getName());
                section.set("lore", feature.getLore());
                saveNeeded = true;
            }
        }
        if (saveNeeded)
            features.save();
        Feature.reloadDescriptions();
    }

    private void initializeItemsAndRecipes() {
        for (CustomItem item : CustomItem.values()) {
            ConfigurationSection section = items.getConfig().getConfigurationSection(item.name());
            if (section == null) {
                plugin.getLogger().log(Level.WARNING, "Configuration section is null for CustomItem: " + item.name());
                continue;
            }
            item.update(section);

            NamespacedKey key = new NamespacedKey(plugin, "suddendeath_" + item.name().toLowerCase());
            boolean craftEnabled = section.getBoolean("craft-enabled") && item.getCraft() != null;

            if (craftEnabled) {
                int newHash = calculateRecipeHash(item);
                Integer cachedHash = recipeCache.get(key);
                if (cachedHash != null && cachedHash == newHash) {
                    // Recipe unchanged — skip remove/re-register
                    continue;
                }
                // Remove old recipe if it exists, then register the new one
                if (cachedHash != null) {
                    plugin.getServer().removeRecipe(key);
                }
                try {
                    registerCraftingRecipe(item);
                    recipeCache.put(key, newHash);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to register recipe for " + item.name(), e);
                }
            } else {
                // Recipe was disabled — remove if it was registered before
                if (recipeCache.containsKey(key)) {
                    plugin.getServer().removeRecipe(key);
                    recipeCache.remove(key);
                }
            }
        }
    }

    private void registerCraftingRecipe(CustomItem item) {
        if (item.getCraft() == null || item.getCraft().isEmpty())
            return;
        try {
            NamespacedKey recipeKey = new NamespacedKey(plugin, "suddendeath_" + item.name().toLowerCase());
            ShapedRecipe recipe = new ShapedRecipe(recipeKey, item.a());
            recipe.shape("ABC", "DEF", "GHI");
            char[] chars = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I' };
            List<String> craftLines = item.getCraft();
            for (int i = 0; i < 9; i++) {
                String materialName = craftLines.get(i / 3).split(",")[i % 3].trim();
                if (materialName.equalsIgnoreCase("AIR") || materialName.isEmpty())
                    continue;
                recipe.setIngredient(chars[i], Material.valueOf(materialName.toUpperCase()));
            }
            recipe.setGroup("suddendeath_items");
            plugin.getServer().addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to register recipe for " + item.name(), e);
        }
    }

    private int calculateRecipeHash(CustomItem item) {
        return Objects.hash(item.getCraft(), item.a().getType(), item.a().getItemMeta().getDisplayName(),
                item.a().getItemMeta().getLore());
    }

    private void removeAllCustomRecipes() {
        Iterator<Recipe> recipes = plugin.getServer().recipeIterator();
        List<NamespacedKey> toRemove = new ArrayList<>();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe instanceof org.bukkit.Keyed) {
                NamespacedKey key = ((org.bukkit.Keyed) recipe).getKey();
                if (key.getNamespace().equals(plugin.getName().toLowerCase())
                        && key.getKey().startsWith("suddendeath_")) {
                    toRemove.add(key);
                }
            }
        }
        for (NamespacedKey key : toRemove) {
            plugin.getServer().removeRecipe(key);
        }
    }
}