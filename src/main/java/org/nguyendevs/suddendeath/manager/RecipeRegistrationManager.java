package org.nguyendevs.suddendeath.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.CustomItem;

import java.util.*;
import java.util.logging.Level;

public class RecipeRegistrationManager {

    private final SuddenDeath plugin;
    private final Map<NamespacedKey, Integer> recipeCache;
    private final Set<NamespacedKey> registeredRecipes;

    public RecipeRegistrationManager(SuddenDeath plugin) {
        this.plugin = plugin;
        this.recipeCache = new HashMap<>();
        this.registeredRecipes = new HashSet<>();
    }

    public void registerAllRecipes() {
        plugin.getLogger().info("Starting recipe registration...");

        for (CustomItem item : CustomItem.values()) {
            try {
                ConfigurationSection section = plugin.items.getConfig().getConfigurationSection(item.name());
                if (section == null) {
                    plugin.getLogger().warning("Configuration section is null for: " + item.name());
                    continue;
                }

                item.update(section);

                boolean craftEnabled = section.getBoolean("craft-enabled", false);
                List<String> craftPattern = item.getCraft();

                if (craftEnabled && craftPattern != null && !craftPattern.isEmpty()) {
                    registerRecipe(item);
                } else {
                    plugin.getLogger().info("Recipe disabled or invalid for: " + item.name());
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to register recipe for: " + item.name(), e);
            }
        }

        plugin.getLogger().info("Registered " + registeredRecipes.size() + " recipes successfully!");
    }

    private void registerRecipe(CustomItem item) {
        try {
            NamespacedKey recipeKey = new NamespacedKey(plugin, "suddendeath_" + item.name().toLowerCase());

            if (isRecipeRegistered(recipeKey)) {
                plugin.getLogger().info("Recipe already exists, skipping: " + item.name());
                return;
            }

            ItemStack result = item.createItem();
            if (result == null || result.getType() == Material.AIR) {
                plugin.getLogger().warning("Invalid result item for: " + item.name());
                return;
            }

            List<String> craftLines = item.getCraft();
            if (craftLines.size() != 3) {
                plugin.getLogger().warning("Invalid craft pattern size for: " + item.name());
                return;
            }

            Material[][] grid = new Material[3][3];
            for (int row = 0; row < 3; row++) {
                String[] materials = craftLines.get(row).split(",");
                if (materials.length != 3) {
                    plugin.getLogger().warning("Invalid craft line format for: " + item.name());
                    return;
                }

                for (int col = 0; col < 3; col++) {
                    String materialName = materials[col].trim().toUpperCase();
                    if (materialName.equals("AIR") || materialName.isEmpty()) {
                        grid[row][col] = null;
                    } else {
                        try {
                            grid[row][col] = Material.valueOf(materialName);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid material in recipe " + item.name() + ": " + materialName);
                            return;
                        }
                    }
                }
            }

            String[] shape = buildOptimizedShape(grid);
            Map<Character, Material> ingredients = buildIngredients(grid, shape);

            if (shape.length == 0 || ingredients.isEmpty()) {
                plugin.getLogger().warning("Empty recipe pattern for: " + item.name());
                return;
            }

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
            recipe.shape(shape);

            for (Map.Entry<Character, Material> entry : ingredients.entrySet()) {
                recipe.setIngredient(entry.getKey(), entry.getValue());
            }

            //recipe.setGroup("suddendeath_items");
            recipe.setGroup("");
            recipe.setCategory(CraftingBookCategory.MISC);
            boolean added = Bukkit.addRecipe(recipe);

            if (added) {
                registeredRecipes.add(recipeKey);
                int hash = calculateRecipeHash(item);
                recipeCache.put(recipeKey, hash);
                plugin.getLogger().info("Successfully registered recipe: " + item.name() +
                        " [Shape: " + String.join(", ", shape) + "]");
            } else {
                plugin.getLogger().warning("Failed to add recipe to server: " + item.name());
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error registering recipe for: " + item.name(), e);
        }
    }

    private String[] buildOptimizedShape(Material[][] grid) {
        int minRow = 3, maxRow = -1;
        int minCol = 3, maxCol = -1;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (grid[row][col] != null) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        if (maxRow == -1) {
            return new String[0];
        }

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;
        String[] shape = new String[height];

        for (int row = 0; row < height; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < width; col++) {
                Material mat = grid[minRow + row][minCol + col];
                if (mat == null) {
                    line.append(' ');
                } else {
                    char c = (char) ('A' + (row * 3 + col));
                    line.append(c);
                }
            }
            shape[row] = line.toString();
        }

        return shape;
    }

    private Map<Character, Material> buildIngredients(Material[][] grid, String[] shape) {
        Map<Character, Material> ingredients = new HashMap<>();

        int minRow = 3, minCol = 3;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (grid[row][col] != null) {
                    minRow = Math.min(minRow, row);
                    minCol = Math.min(minCol, col);
                }
            }
        }

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length(); col++) {
                char c = shape[row].charAt(col);
                if (c != ' ') {
                    Material mat = grid[minRow + row][minCol + col];
                    if (mat != null) {
                        ingredients.put(c, mat);
                    }
                }
            }
        }

        return ingredients;
    }

    private boolean isRecipeRegistered(NamespacedKey key) {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof org.bukkit.Keyed keyed) {
                if (keyed.getKey().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void unregisterAllRecipes() {
        plugin.getLogger().info("Unregistering all custom recipes...");

        int removed = 0;
        for (NamespacedKey key : new HashSet<>(registeredRecipes)) {
            if (Bukkit.removeRecipe(key)) {
                removed++;
            }
        }

        registeredRecipes.clear();
        recipeCache.clear();

        plugin.getLogger().info("Unregistered " + removed + " recipes successfully!");
    }

    public void reloadRecipes() {
        plugin.getLogger().info("Reloading recipes...");

        for (CustomItem item : CustomItem.values()) {
            try {
                ConfigurationSection section = plugin.items.getConfig().getConfigurationSection(item.name());
                if (section == null) continue;

                item.update(section);

                NamespacedKey recipeKey = new NamespacedKey(plugin, "suddendeath_" + item.name().toLowerCase());
                boolean craftEnabled = section.getBoolean("craft-enabled", false);

                if (craftEnabled && item.getCraft() != null) {
                    int newHash = calculateRecipeHash(item);

                    if (recipeCache.containsKey(recipeKey)) {
                        int oldHash = recipeCache.get(recipeKey);

                        if (newHash != oldHash) {
                            plugin.getLogger().info("Recipe changed, updating: " + item.name());
                            Bukkit.removeRecipe(recipeKey);
                            registeredRecipes.remove(recipeKey);
                            registerRecipe(item);
                        }
                    } else {
                        plugin.getLogger().info("New recipe detected: " + item.name());
                        registerRecipe(item);
                    }
                } else {
                    if (registeredRecipes.contains(recipeKey)) {
                        plugin.getLogger().info("Removing disabled recipe: " + item.name());
                        Bukkit.removeRecipe(recipeKey);
                        registeredRecipes.remove(recipeKey);
                        recipeCache.remove(recipeKey);
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error reloading recipe for: " + item.name(), e);
            }
        }

        plugin.getLogger().info("Recipe reload completed!");
    }

    private int calculateRecipeHash(CustomItem item) {
        ItemStack result = item.createItem();
        if (result == null || result.getItemMeta() == null) {
            return Objects.hash(item.getCraft());
        }

        return Objects.hash(
                item.getCraft(),
                result.getType(),
                result.getItemMeta().getDisplayName(),
                result.getItemMeta().getLore()
        );
    }

    public void discoverRecipeForPlayer(org.bukkit.entity.Player player, CustomItem item) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "suddendeath_" + item.name().toLowerCase());

        if (registeredRecipes.contains(recipeKey)) {
            player.discoverRecipe(recipeKey);
        }
    }

    public void discoverAllRecipesForPlayer(org.bukkit.entity.Player player) {
        int discovered = 0;
        for (NamespacedKey key : registeredRecipes) {
            if (player.discoverRecipe(key)) {
                discovered++;
            }
        }
        plugin.getLogger().info("Player " + player.getName() + " discovered " + discovered + " recipes");
    }

    public Set<NamespacedKey> getRegisteredRecipes() {
        return new HashSet<>(registeredRecipes);
    }

    public boolean isRecipeEnabled(CustomItem item) {
        ConfigurationSection section = plugin.items.getConfig().getConfigurationSection(item.name());
        if (section == null) return false;

        return section.getBoolean("craft-enabled", false) && item.getCraft() != null;
    }
}