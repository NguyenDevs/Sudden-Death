package org.nguyendevs.suddendeath.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;

public class AdminView extends PluginInventory {
    private static final String PREFIX = "&6[&cSudden&4Death&6]";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.###");
    private static final int[] AVAILABLE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int ITEMS_PER_PAGE = 21;
    private static final int INVENTORY_SIZE = 45;

    private static final int FILTER_SLOT = 40;
    private static final Material FILTER_MATERIAL = Material.BOOK;
    private int filterIndex = 0;
    private boolean visualMode = false;
    private long lastFilterClickMs = 0L;

    // Feature Categories
    private static final EnumSet<Feature> EVENT_SET = EnumSet.of(
            Feature.BLOOD_MOON, Feature.THUNDERSTORM, Feature.METEOR_RAIN
    );
    private static final EnumSet<Feature> MOB_SET = EnumSet.of(
            Feature.ABYSSAL_VORTEX, Feature.ANGRY_SPIDERS, Feature.BONE_GRENADES, Feature.BONE_WIZARDS,
            Feature.BREEZE_DASH, Feature.CREEPER_REVENGE, Feature.ENDER_POWER, Feature.EVERBURNING_BLAZES,
            Feature.FORCE_OF_THE_UNDEAD, Feature.HOMING_FLAME_BARRAGE, Feature.IMMORTAL_EVOKER,
            Feature.LEAPING_SPIDERS, Feature.MOB_CRITICAL_STRIKES, Feature.NETHER_SHIELD,
            Feature.POISONED_SLIMES, Feature.QUICK_MOBS, Feature.SHOCKING_SKELETON_ARROWS,
            Feature.SILVERFISHES_SUMMON, Feature.STRAY_FROST, Feature.SPIDER_WEB, Feature.TANKY_MONSTERS,
            Feature.THIEF_SLIMES, Feature.TRIDENT_WRATH, Feature.UNDEAD_GUNNERS, Feature.UNDEAD_RAGE,
            Feature.WITCH_SCROLLS, Feature.WITHER_MACHINEGUN, Feature.WITHER_RUSH, Feature.ZOMBIE_BREAK_BLOCK,
            Feature.ZOMBIE_TOOLS
    );
    private static final EnumSet<Feature> SURVIVAL_SET = EnumSet.of(
            Feature.ADVANCED_PLAYER_DROPS, Feature.ARROW_SLOW, Feature.BLEEDING, Feature.BLOOD_SCREEN,
            Feature.DANGEROUS_COAL, Feature.ELECTRICITY_SHOCK, Feature.FALL_STUN, Feature.FREDDY,
            Feature.HUNGER_NAUSEA, Feature.INFECTION, Feature.PHYSIC_ENDER_PEARL, Feature.REALISTIC_PICKUP,
            Feature.SNOW_SLOW, Feature.STONE_STIFFNESS
    );

    // Animation Sets
    private static final EnumSet<Feature> ANIMATED_FEATURES = EnumSet.of(
            Feature.ENDER_POWER, Feature.FORCE_OF_THE_UNDEAD, Feature.QUICK_MOBS, Feature.MOB_CRITICAL_STRIKES,
            Feature.TANKY_MONSTERS, Feature.NETHER_SHIELD, Feature.ANGRY_SPIDERS, Feature.LEAPING_SPIDERS,
            Feature.UNDEAD_RAGE
    );

    // Animation Materials
    private static final Material[] UNDEAD_RAGE_EGGS = {Material.ZOMBIE_SPAWN_EGG, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, Material.ZOMBIE_VILLAGER_SPAWN_EGG, Material.HUSK_SPAWN_EGG, Material.DROWNED_SPAWN_EGG};
    private static final Material[] NETHER_SHIELD_EGGS = {Material.MAGMA_CUBE_SPAWN_EGG, Material.BLAZE_SPAWN_EGG, Material.HOGLIN_SPAWN_EGG, Material.ZOGLIN_SPAWN_EGG, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, Material.STRIDER_SPAWN_EGG, Material.PIGLIN_BRUTE_SPAWN_EGG, Material.PIGLIN_SPAWN_EGG};
    private static final Material[] ANGRY_SPIDERS_EGGS = {Material.SPIDER_SPAWN_EGG, Material.CAVE_SPIDER_SPAWN_EGG};
    private static final Material[] LEAPING_SPIDERS_EGGS = {Material.SPIDER_SPAWN_EGG, Material.CAVE_SPIDER_SPAWN_EGG};
    private static final Material[] ENDER_POWER_EGGS = {Material.SHULKER_SPAWN_EGG, Material.ENDERMAN_SPAWN_EGG, Material.ENDERMITE_SPAWN_EGG, Material.ENDER_DRAGON_SPAWN_EGG};

    private static final int ANIM_PERIOD_TICKS = 15;
    private BukkitTask visualTask = null;
    private final Map<Integer, Feature> slotFeatureMap = new HashMap<>();
    private final Map<Integer, Material> lastSlotMaterial = new HashMap<>();
    private final Random rng = new Random();
    private Feature[] cachedSource = null;
    private int cachedFilterIndex = -1;
    private int page;

    public AdminView(Player player) {
        super(player);
    }

    private static String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public void open() {
        stopVisualAnimation();
        super.open();
        if (visualMode) startVisualAnimation();
    }

    private void stopVisualAnimation() {
        if (visualTask != null) {
            try { visualTask.cancel(); } catch (Throwable ignored) {}
            visualTask = null;
        }
        lastSlotMaterial.clear();
    }

    private void startVisualAnimation() {
        if (!visualMode) return;
        if (visualTask != null) return;

        visualTask = Bukkit.getScheduler().runTaskTimer(SuddenDeath.getInstance(), () -> {
            try {
                if (player == null || !player.isOnline()) { stopVisualAnimation(); return; }
                if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
                    stopVisualAnimation();
                    return;
                }

                Inventory inv = player.getOpenInventory().getTopInventory();

                for (Map.Entry<Integer, Feature> e : slotFeatureMap.entrySet()) {
                    int slot = e.getKey();
                    Feature f = e.getValue();
                    if (!ANIMATED_FEATURES.contains(f)) continue;

                    ItemStack oldItem = inv.getItem(slot);
                    if (oldItem == null) continue;

                    List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig().getStringList(f.getPath());
                    boolean isEnabledInWorld = enabledWorlds.contains(player.getWorld().getName());
                    if (!isEnabledInWorld) continue; // Don't animate disabled features in Admin View, keep them Gray

                    Material newMat = getRandomAnimatedMaterialFor(f);
                    if (newMat == null) continue;

                    if (newMat == oldItem.getType()) {
                        Material retry = getRandomAnimatedMaterialFor(f);
                        if (retry != null) newMat = retry;
                    }
                    Material lastMat = lastSlotMaterial.get(slot);
                    if (lastMat != null && lastMat == newMat) continue;

                    ItemMeta oldMeta = oldItem.getItemMeta();
                    ItemStack newItem = new ItemStack(newMat);
                    if (oldMeta != null) newItem.setItemMeta(oldMeta);
                    inv.setItem(slot, newItem);
                    lastSlotMaterial.put(slot, newMat);
                }
            } catch (Throwable t) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Visual animation tick error", t);
            }
        }, ANIM_PERIOD_TICKS, ANIM_PERIOD_TICKS);
    }

    private Material getRandomAnimatedMaterialFor(Feature f) {
        Material[] pool;
        switch (f) {
            case NETHER_SHIELD: pool = NETHER_SHIELD_EGGS; break;
            case LEAPING_SPIDERS: pool = LEAPING_SPIDERS_EGGS; break;
            case ANGRY_SPIDERS: pool = ANGRY_SPIDERS_EGGS; break;
            case UNDEAD_RAGE: pool = UNDEAD_RAGE_EGGS; break;
            case ENDER_POWER: pool = ENDER_POWER_EGGS; break;
            default: return getVisualMaterial(f);
        }
        if (pool.length == 0) return getVisualMaterial(f);
        return pool[rng.nextInt(pool.length)];
    }

    private Material getVisualMaterial(Feature f) {
        switch (f) {
            case ABYSSAL_VORTEX: return Material.GUARDIAN_SPAWN_EGG;
            case ANGRY_SPIDERS: return Material.SPIDER_SPAWN_EGG;
            case BLOOD_MOON: return Material.ZOMBIE_HEAD;
            case BONE_GRENADES: return Material.SKELETON_SPAWN_EGG;
            case BONE_WIZARDS: return Material.SKELETON_SPAWN_EGG;
            case BREEZE_DASH: return Material.BREEZE_SPAWN_EGG;
            case CREEPER_REVENGE: return Material.CREEPER_SPAWN_EGG;
            case ENDER_POWER: return Material.ENDER_DRAGON_SPAWN_EGG;
            case EVERBURNING_BLAZES: return Material.BLAZE_SPAWN_EGG;
            case FORCE_OF_THE_UNDEAD: return Material.SPAWNER;
            case HOMING_FLAME_BARRAGE: return Material.BLAZE_SPAWN_EGG;
            case IMMORTAL_EVOKER: return Material.EVOKER_SPAWN_EGG;
            case LEAPING_SPIDERS: return Material.SPIDER_SPAWN_EGG;
            case METEOR_RAIN: return Material.FIRE_CHARGE;
            case MOB_CRITICAL_STRIKES: return Material.SPAWNER;
            case NETHER_SHIELD: return Material.NETHERRACK;
            case POISONED_SLIMES: return Material.SLIME_SPAWN_EGG;
            case QUICK_MOBS: return Material.SPAWNER;
            case SHOCKING_SKELETON_ARROWS: return Material.SKELETON_SPAWN_EGG;
            case SILVERFISHES_SUMMON: return Material.SILVERFISH_SPAWN_EGG;
            case STRAY_FROST: return Material.STRAY_SPAWN_EGG;
            case SPIDER_WEB: return Material.CAVE_SPIDER_SPAWN_EGG;
            case TANKY_MONSTERS: return Material.SPAWNER;
            case THIEF_SLIMES: return Material.SLIME_SPAWN_EGG;
            case TRIDENT_WRATH: return Material.DROWNED_SPAWN_EGG;
            case UNDEAD_GUNNERS: return Material.ZOMBIE_SPAWN_EGG;
            case UNDEAD_RAGE: return Material.ZOMBIE_SPAWN_EGG;
            case WITCH_SCROLLS: return Material.WITCH_SPAWN_EGG;
            case WITHER_MACHINEGUN: return Material.WITHER_SKELETON_SPAWN_EGG;
            case WITHER_RUSH: return Material.WITHER_SKELETON_SPAWN_EGG;
            case ZOMBIE_BREAK_BLOCK: return Material.ZOMBIE_SPAWN_EGG;
            case ZOMBIE_TOOLS: return Material.IRON_SHOVEL;
            case ADVANCED_PLAYER_DROPS: return Material.PLAYER_HEAD;
            case ARROW_SLOW: return Material.TIPPED_ARROW;
            case BLEEDING: return Material.PAPER;
            case BLOOD_SCREEN: return Material.FIRE_CORAL;
            case DANGEROUS_COAL: return Material.COAL;
            case ELECTRICITY_SHOCK: return Material.REDSTONE;
            case FALL_STUN: return Material.RABBIT_FOOT;
            case FREDDY: return Material.ENDER_EYE;
            case HUNGER_NAUSEA: return Material.COOKED_CHICKEN;
            case INFECTION: return Material.SUSPICIOUS_STEW;
            case PHYSIC_ENDER_PEARL: return Material.ENDER_PEARL;
            case REALISTIC_PICKUP: return Material.BUNDLE;
            case SNOW_SLOW: return Material.SNOWBALL;
            case STONE_STIFFNESS: return Material.DEEPSLATE;
            case THUNDERSTORM: return Material.NETHER_STAR;
            default: return null;
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        Feature[] source = getFilteredFeatures();
        int maxPage = Math.max(1, (source.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (page >= maxPage) page = maxPage - 1;
        if (page < 0) page = 0;

        Inventory inventory = Bukkit.createInventory(this, INVENTORY_SIZE, translateColors(Utils.msg("gui-admin-name")) + " (" + (page + 1) + "/" + maxPage + ")");
        this.slotFeatureMap.clear();

        try {
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(source.length, (page + 1) * ITEMS_PER_PAGE);

            for (int i = startIndex; i < endIndex; i++) {
                int slot = getAvailableSlot(inventory);
                Feature f = source[i];
                inventory.setItem(slot, createFeatureItem(f));
                this.slotFeatureMap.put(slot, f);
            }
            if (page > 0) {
                inventory.setItem(18, createNavigationItem(Material.ARROW, translateColors(Utils.msg("gui-previous"))));
            }
            if (endIndex < source.length) {
                inventory.setItem(26, createNavigationItem(Material.ARROW, translateColors(Utils.msg("gui-next"))));
            }

            inventory.setItem(FILTER_SLOT, createFilterItem());

        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating AdminView inventory for player: " + player.getName(), e);
        }
        return inventory;
    }

    private Feature[] getFilteredFeatures() {
        if (cachedSource == null || cachedFilterIndex != filterIndex) {
            Feature[] all = Feature.values();
            List<Feature> list;
            if (filterIndex == 1) {
                list = new ArrayList<>();
                for (Feature f : all) if (SURVIVAL_SET.contains(f)) list.add(f);
            } else if (filterIndex == 2) {
                list = new ArrayList<>();
                for (Feature f : all) if (MOB_SET.contains(f)) list.add(f);
            } else if (filterIndex == 3) {
                list = new ArrayList<>();
                for (Feature f : all) if (EVENT_SET.contains(f)) list.add(f);
            } else {
                list = new ArrayList<>(Arrays.asList(all));
            }
            list.sort(Comparator.comparing(Feature::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Enum::ordinal));
            cachedSource = list.toArray(new Feature[0]);
            cachedFilterIndex = filterIndex;
        }
        return cachedSource;
    }

    private ItemStack createFeatureItem(Feature feature) {
        List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig().getStringList(feature.getPath());
        boolean isEnabledInWorld = enabledWorlds.contains(player.getWorld().getName());

        Material material;
        if (visualMode && isEnabledInWorld) {
            Material vis = getRandomAnimatedMaterialFor(feature);
            material = (vis != null) ? vis : Material.LIME_DYE;
        } else {
            material = isEnabledInWorld ? (feature.isEvent() ? Material.LIGHT_BLUE_DYE : Material.LIME_DYE) : Material.GRAY_DYE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "ItemMeta is null for feature: " + feature.getName());
            return item;
        }
        meta.setDisplayName(ChatColor.GOLD + feature.getName());
        meta.getPersistentDataContainer().set(Utils.nsk("featureId"), PersistentDataType.STRING, feature.name());
        meta.setLore(createFeatureLore(feature, enabledWorlds, isEnabledInWorld));
        item.setItemMeta(meta);
        return item;
    }

    private List<String> createFeatureLore(Feature feature, List<String> enabledWorlds, boolean isEnabledInWorld) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : feature.getLore()) {
            lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', statsInLore(feature, line)));
        }

        if (!enabledWorlds.isEmpty()) {
            lore.add("");
            lore.add(translateColors(Utils.msg("gui-features")));
            for (String world : enabledWorlds) {
                lore.add(ChatColor.WHITE + "► " + ChatColor.DARK_GREEN + world);
            }
        }
        lore.add("");
        lore.add(isEnabledInWorld ? translateColors(Utils.msg("gui-features-enabled")) : translateColors(Utils.msg("gui-features-disabled")));
        lore.add(ChatColor.YELLOW + "Click to " + (isEnabledInWorld ? "disable." : "enable."));
        return lore;
    }

    private ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "ItemMeta is null for navigation item: " + name);
            return item;
        }
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFilterItem() {
        ItemStack item = new ItemStack(FILTER_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(translateColors(Utils.msg("filter-name")));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + translateColors(Utils.msg("filter-lore-desc")));
        lore.add("");

        String defColor = (filterIndex == 0) ? "&a" : "&f";
        String srvColor = (filterIndex == 1) ? "&a" : "&f";
        String mobColor = (filterIndex == 2) ? "&a" : "&f";
        String eventColor = (filterIndex == 3) ? "&a" : "&f";
        lore.add(translateColors("&6► " + defColor + Utils.msg("filter-lore-default")));
        lore.add(translateColors("&6► " + srvColor + Utils.msg("filter-lore-survival")));
        lore.add(translateColors("&6► " + mobColor + Utils.msg("filter-lore-mob")));
        lore.add(translateColors("&6► " + eventColor + Utils.msg("filter-lore-event")));

        lore.add("");
        String visColor = visualMode ? "&6" : "&f";
        lore.add(translateColors("&e► " + visColor + Utils.msg("filter-lore-visual")));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean canToggleFilter() {
        long now = System.currentTimeMillis();
        if (now - lastFilterClickMs < 250) return false;
        lastFilterClickMs = now;
        return true;
    }

    private void cycleFilterLeft() {
        filterIndex = (filterIndex + 1) % 4;
        page = 0;
        cachedSource = null;
        try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f); } catch (Throwable ignored) {}
        open();
    }

    private void toggleVisualRight() {
        visualMode = !visualMode;
        try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, visualMode ? 1.2f : 0.9f); } catch (Throwable ignored) {}
        open();
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !Utils.isPluginItem(item, false)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "ItemMeta is null for clicked item in AdminView for player: " + player.getName());
            return;
        }
        try {
            String displayName = meta.getDisplayName();
            Feature[] source = getFilteredFeatures();
            int maxPage = Math.max(1, (source.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);

            if (translateColors(Utils.msg("gui-next")).equals(displayName)) {
                if (page + 1 < maxPage) {
                    page++;
                    open();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
                }
                return;
            }
            if (translateColors(Utils.msg("gui-previous")).equals(displayName)) {
                if (page > 0) {
                    page--;
                    open();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
                }
                return;
            }

            if (event.getSlot() == FILTER_SLOT && item.getType() == FILTER_MATERIAL) {
                if (!canToggleFilter()) return;
                if (event.isLeftClick()) {
                    cycleFilterLeft();
                } else if (event.isRightClick()) {
                    toggleVisualRight();
                }
                return;
            }

            String featureId = meta.getPersistentDataContainer().get(Utils.nsk("featureId"), PersistentDataType.STRING);
            if (featureId == null || featureId.isEmpty()) {
                return;
            }
            Feature feature = Feature.valueOf(featureId);
            List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig().getStringList(feature.getPath());
            String worldName = player.getWorld().getName();

            if (enabledWorlds.contains(worldName)) {
                enabledWorlds.remove(worldName);
                player.sendMessage(translateColors(PREFIX + " " + "&eYou disabled &6" + feature.getName() + " &ein &6" + worldName + "&e."));
            } else {
                enabledWorlds.add(worldName);
                player.sendMessage(translateColors(PREFIX + " " + "&eYou enabled &6" + feature.getName() + " &ein &6" + worldName + "&e."));
            }

            SuddenDeath.getInstance().getConfiguration().getConfig().set(feature.getPath(), enabledWorlds);
            SuddenDeath.getInstance().getConfiguration().save();
            SuddenDeath.getInstance().getConfiguration().reload();
            SuddenDeath.getInstance().getLogger().info("Updated config for " + feature.getName() + ": " + enabledWorlds);
            open();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling InventoryClickEvent for player: " + player.getName(), e);
            player.sendMessage(translateColors(PREFIX + " " + "An error occurred while processing your action."));
        }
    }

    public static String statsInLore(Feature feature, String lore) {
        if (lore.contains("#")) {
            String[] parts = lore.split("#", 3);
            if (parts.length >= 2) {
                String stat = parts[1];
                return statsInLore(feature, parts[0] + ChatColor.GREEN + DECIMAL_FORMAT.format(feature.getDouble(stat)) + ChatColor.GRAY + parts[2]);
            }
        }
        return lore;
    }

    private int getAvailableSlot(Inventory inventory) {
        for (int slot : AVAILABLE_SLOTS) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return -1;
    }
}