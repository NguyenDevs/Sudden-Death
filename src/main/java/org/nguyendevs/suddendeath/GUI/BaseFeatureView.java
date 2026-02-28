package org.nguyendevs.suddendeath.GUI;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;

public abstract class BaseFeatureView extends PluginInventory {
    protected static final String PREFIX = "&6[&cSudden&4Death&6]";
    protected static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.###");
    protected static final int[] AVAILABLE_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30,
            31, 32, 33, 34 };
    protected static final int ITEMS_PER_PAGE = 21;
    protected static final int INVENTORY_SIZE = 45;

    protected static final int FILTER_SLOT = 40;
    protected static final Material FILTER_MATERIAL = Material.BOOK;

    protected int filterIndex = 0;
    protected boolean visualMode = false;
    protected long lastFilterClickMs = 0L;
    protected int page;

    protected static final EnumSet<Feature> EVENT_SET = EnumSet.of(Feature.BLOOD_MOON, Feature.THUNDERSTORM,
            Feature.METEOR_RAIN);
    protected static final EnumSet<Feature> MOB_SET = EnumSet.of(Feature.ABYSSAL_VORTEX, Feature.ANGRY_SPIDERS,
            Feature.BONE_GRENADES, Feature.BONE_WIZARDS, Feature.PHANTOM_BLADE, Feature.BREEZE_DASH,
            Feature.CREEPER_REVENGE, Feature.ENDER_POWER, Feature.EVERBURNING_BLAZES, Feature.FORCE_OF_THE_UNDEAD,
            Feature.HOMING_FLAME_BARRAGE, Feature.IMMORTAL_EVOKER, Feature.LEAPING_SPIDERS,
            Feature.MOB_CRITICAL_STRIKES, Feature.NETHER_SHIELD, Feature.POISONED_SLIMES, Feature.QUICK_MOBS,
            Feature.SHOCKING_SKELETON_ARROWS, Feature.SILVERFISHES_SUMMON, Feature.STRAY_FROST, Feature.SPIDER_WEB,
            Feature.SPIDER_NEST, Feature.TANKY_MONSTERS, Feature.THIEF_SLIMES, Feature.TRIDENT_WRATH,
            Feature.UNDEAD_GUNNERS, Feature.UNDEAD_RAGE, Feature.WITCH_SCROLLS, Feature.WITHER_MACHINEGUN,
            Feature.WITHER_RUSH, Feature.ZOMBIE_BREAK_BLOCK, Feature.ZOMBIE_TOOLS, Feature.FIREWORK_ARROWS,
            Feature.ARMOR_PIERCING);
    protected static final EnumSet<Feature> SURVIVAL_SET = EnumSet.of(Feature.ADVANCED_PLAYER_DROPS, Feature.ARROW_SLOW,
            Feature.BLEEDING, Feature.BLOOD_SCREEN, Feature.DANGEROUS_COAL, Feature.ELECTRICITY_SHOCK,
            Feature.FALL_STUN, Feature.FREDDY, Feature.HUNGER_NAUSEA, Feature.INFECTION, Feature.PHYSIC_ENDER_PEARL,
            Feature.REALISTIC_PICKUP, Feature.SNOW_SLOW, Feature.STONE_STIFFNESS, Feature.WHISPERS_OF_THE_DESERT);

    protected static final EnumSet<Feature> ANIMATED_FEATURES = EnumSet.of(Feature.ENDER_POWER,
            Feature.FORCE_OF_THE_UNDEAD, Feature.QUICK_MOBS, Feature.MOB_CRITICAL_STRIKES, Feature.TANKY_MONSTERS,
            Feature.NETHER_SHIELD, Feature.ANGRY_SPIDERS, Feature.LEAPING_SPIDERS, Feature.UNDEAD_RAGE,
            Feature.WHISPERS_OF_THE_DESERT);

    protected static final Material[] UNDEAD_RAGE_EGGS = { Material.ZOMBIE_SPAWN_EGG,
            Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, Material.ZOMBIE_VILLAGER_SPAWN_EGG, Material.HUSK_SPAWN_EGG,
            Material.DROWNED_SPAWN_EGG };
    protected static final Material[] NETHER_SHIELD_EGGS = { Material.MAGMA_CUBE_SPAWN_EGG, Material.BLAZE_SPAWN_EGG,
            Material.HOGLIN_SPAWN_EGG, Material.ZOGLIN_SPAWN_EGG, Material.ZOMBIFIED_PIGLIN_SPAWN_EGG,
            Material.STRIDER_SPAWN_EGG, Material.PIGLIN_BRUTE_SPAWN_EGG, Material.PIGLIN_SPAWN_EGG };
    protected static final Material[] ANGRY_SPIDERS_EGGS = { Material.SPIDER_SPAWN_EGG,
            Material.CAVE_SPIDER_SPAWN_EGG };
    protected static final Material[] LEAPING_SPIDERS_EGGS = { Material.SPIDER_SPAWN_EGG,
            Material.CAVE_SPIDER_SPAWN_EGG };
    protected static final Material[] ENDER_POWER_EGGS = { Material.SHULKER_SPAWN_EGG, Material.ENDERMAN_SPAWN_EGG,
            Material.ENDERMITE_SPAWN_EGG, Material.ENDER_DRAGON_SPAWN_EGG };
    protected static final Material[] WHISPER_OF_THE_DESERT_BLOCK = { Material.CHISELED_SANDSTONE,
            Material.CHISELED_RED_SANDSTONE, Material.HUSK_SPAWN_EGG };

    protected static final int ANIM_PERIOD_TICKS = 15;
    protected BukkitTask visualTask = null;
    protected final Map<Integer, Feature> slotFeatureMap = new HashMap<>();
    protected final Map<Integer, Material> lastSlotMaterial = new HashMap<>();
    protected final Random rng = new Random();

    protected Feature[] cachedSource = null;
    protected int cachedFilterIndex = -1;


    public BaseFeatureView(Player player) {
        super(player);
    }

    @Override
    public void open() {
        stopVisualAnimation();
        super.open();
        if (visualMode)
            startVisualAnimation();
    }

    protected void stopVisualAnimation() {
        if (visualTask != null) {
            try {
                visualTask.cancel();
            } catch (Exception ignored) {
            }
            visualTask = null;
        }
        lastSlotMaterial.clear();
    }

    protected void startVisualAnimation() {
        if (!visualMode || visualTask != null)
            return;

        visualTask = Bukkit.getScheduler().runTaskTimer(SuddenDeath.getInstance(), () -> {
            try {
                if (!player.isOnline()) {
                    stopVisualAnimation();
                    return;
                }
                if (player.getOpenInventory().getTopInventory().getHolder() != this) {
                    stopVisualAnimation();
                    return;
                }

                Inventory inv = player.getOpenInventory().getTopInventory();

                for (Map.Entry<Integer, Feature> e : slotFeatureMap.entrySet()) {
                    int slot = e.getKey();
                    Feature f = e.getValue();
                    if (!ANIMATED_FEATURES.contains(f))
                        continue;

                    ItemStack oldItem = inv.getItem(slot);
                    if (oldItem == null)
                        continue;

                    if (!isFeatureEnabledForAnimation(f))
                        continue;

                    Material newMat = getRandomAnimatedMaterialFor(f);
                    if (newMat == null)
                        continue;

                    if (newMat == oldItem.getType()) {
                        Material retry = getRandomAnimatedMaterialFor(f);
                        if (retry != null)
                            newMat = retry;
                    }
                    Material lastMat = lastSlotMaterial.get(slot);
                    if (lastMat == newMat)
                        continue;

                    ItemMeta oldMeta = oldItem.getItemMeta();
                    ItemStack newItem = new ItemStack(newMat);
                    ItemMeta newMeta = newItem.getItemMeta();
                    if (oldMeta != null && newMeta != null) {
                        Component oldDisplayName = oldMeta.displayName();
                        if (oldDisplayName != null) {
                            newMeta.displayName(oldDisplayName);
                        }
                        List<Component> oldLore = oldMeta.lore();
                        if (oldLore != null) {
                            newMeta.lore(oldLore);
                        }
                        newItem.setItemMeta(newMeta);
                    }
                    inv.setItem(slot, newItem);
                    lastSlotMaterial.put(slot, newMat);
                }
            } catch (Exception t) {
                SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Visual animation tick error", t);
            }
        }, ANIM_PERIOD_TICKS, ANIM_PERIOD_TICKS);
    }

    protected boolean isFeatureEnabledForAnimation(Feature f) {
        List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig()
                .getStringList(f.getPath());
        return enabledWorlds.contains(player.getWorld().getName());
    }

    @Nullable
    protected Material getRandomAnimatedMaterialFor(Feature f) {
        Material[] pool;
        switch (f) {
            case NETHER_SHIELD -> pool = NETHER_SHIELD_EGGS;
            case LEAPING_SPIDERS -> pool = LEAPING_SPIDERS_EGGS;
            case ANGRY_SPIDERS -> pool = ANGRY_SPIDERS_EGGS;
            case UNDEAD_RAGE -> pool = UNDEAD_RAGE_EGGS;
            case ENDER_POWER -> pool = ENDER_POWER_EGGS;
            case WHISPERS_OF_THE_DESERT -> pool = WHISPER_OF_THE_DESERT_BLOCK;
            default -> {
                return getVisualMaterial(f);
            }
        }
        if (pool.length == 0)
            return getVisualMaterial(f);
        return pool[rng.nextInt(pool.length)];
    }

    @Nullable
    protected Material getVisualMaterial(Feature f) {
        return switch (f) {
            case ABYSSAL_VORTEX -> Material.GUARDIAN_SPAWN_EGG;
            case ARMOR_PIERCING -> Material.NETHERITE_CHESTPLATE;
            case ANGRY_SPIDERS, LEAPING_SPIDERS, SPIDER_NEST -> Material.SPIDER_SPAWN_EGG;
            case BLOOD_MOON -> Material.ZOMBIE_HEAD;
            case BONE_GRENADES, BONE_WIZARDS, SHOCKING_SKELETON_ARROWS -> Material.SKELETON_SPAWN_EGG;
            case BREEZE_DASH -> Material.BREEZE_SPAWN_EGG;
            case CREEPER_REVENGE -> Material.CREEPER_SPAWN_EGG;
            case ENDER_POWER -> Material.ENDER_DRAGON_SPAWN_EGG;
            case EVERBURNING_BLAZES, HOMING_FLAME_BARRAGE -> Material.BLAZE_SPAWN_EGG;
            case FORCE_OF_THE_UNDEAD, TANKY_MONSTERS, QUICK_MOBS, MOB_CRITICAL_STRIKES -> Material.SPAWNER;
            case FIREWORK_ARROWS -> Material.PILLAGER_SPAWN_EGG;
            case IMMORTAL_EVOKER -> Material.EVOKER_SPAWN_EGG;
            case METEOR_RAIN -> Material.FIRE_CHARGE;
            case NETHER_SHIELD -> Material.NETHERRACK;
            case POISONED_SLIMES, THIEF_SLIMES -> Material.SLIME_SPAWN_EGG;
            case PHANTOM_BLADE -> Material.PHANTOM_SPAWN_EGG;
            case SILVERFISHES_SUMMON -> Material.SILVERFISH_SPAWN_EGG;
            case STRAY_FROST -> Material.STRAY_SPAWN_EGG;
            case SPIDER_WEB -> Material.CAVE_SPIDER_SPAWN_EGG;
            case TRIDENT_WRATH -> Material.DROWNED_SPAWN_EGG;
            case UNDEAD_GUNNERS, ZOMBIE_BREAK_BLOCK, ZOMBIE_TOOLS, UNDEAD_RAGE -> Material.ZOMBIE_SPAWN_EGG;
            case WITCH_SCROLLS -> Material.WITCH_SPAWN_EGG;
            case WITHER_MACHINEGUN, WITHER_RUSH -> Material.WITHER_SKELETON_SPAWN_EGG;
            case ADVANCED_PLAYER_DROPS -> Material.PLAYER_HEAD;
            case ARROW_SLOW -> Material.TIPPED_ARROW;
            case BLEEDING -> Material.PAPER;
            case BLOOD_SCREEN -> Material.FIRE_CORAL;
            case DANGEROUS_COAL -> Material.COAL;
            case ELECTRICITY_SHOCK -> Material.REDSTONE;
            case FALL_STUN -> Material.RABBIT_FOOT;
            case FREDDY -> Material.ENDER_EYE;
            case HUNGER_NAUSEA -> Material.COOKED_CHICKEN;
            case INFECTION -> Material.SUSPICIOUS_STEW;
            case PHYSIC_ENDER_PEARL -> Material.ENDER_PEARL;
            case REALISTIC_PICKUP -> Material.DIAMOND;
            case SNOW_SLOW -> Material.SNOWBALL;
            case STONE_STIFFNESS -> Material.DEEPSLATE;
            case THUNDERSTORM -> Material.NETHER_STAR;
            default -> null;
        };
    }

    @Override
    public @NotNull Inventory getInventory() {
        Feature[] source = getFilteredFeatures();
        int maxPage = Math.max(1, (source.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (page >= maxPage)
            page = maxPage - 1;
        if (page < 0)
            page = 0;

        Inventory inventory = Bukkit.createInventory(this, INVENTORY_SIZE,
                Utils.color(getInventoryTitle() + " (" + (page + 1) + "/" + maxPage + ")"));
        this.slotFeatureMap.clear();

        try {
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(source.length, (page + 1) * ITEMS_PER_PAGE);

            for (int i = startIndex; i < endIndex; i++) {
                int slot = getAvailableSlot(inventory);
                if (slot != -1) {
                    Feature f = source[i];
                    inventory.setItem(slot, createFeatureItem(f));
                    this.slotFeatureMap.put(slot, f);
                }
            }
            if (page > 0) {
                inventory.setItem(18, createNavigationItem(Utils.msg("gui-previous")));
            }
            if (endIndex < source.length) {
                inventory.setItem(26, createNavigationItem(Utils.msg("gui-next")));
            }

            inventory.setItem(FILTER_SLOT, createFilterItem());
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating inventory for player: " + player.getName(), e);
        }
        return inventory;
    }

    protected abstract String getInventoryTitle();

    protected abstract ItemStack createFeatureItem(Feature feature);

    protected Feature[] getFilteredFeatures() {
        if (cachedSource == null || cachedFilterIndex != filterIndex) {
            Feature[] all = Feature.values();
            List<Feature> list = new ArrayList<>();
            Object filterSet = switch (filterIndex) {
                case 1 -> SURVIVAL_SET;
                case 2 -> MOB_SET;
                case 3 -> EVENT_SET;
                default -> null;
            };

            for (Feature f : all) {
                if (filterSet == null || ((EnumSet<?>) filterSet).contains(f)) {
                    list.add(f);
                }
            }
            list.sort(
                    Comparator.comparing(Feature::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Enum::ordinal));
            cachedSource = list.toArray(new Feature[0]);
            cachedFilterIndex = filterIndex;
        }
        return cachedSource;
    }

    protected ItemStack createNavigationItem(@NotNull String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.displayName(Utils.color(name));
        item.setItemMeta(meta);
        return item;
    }

    protected ItemStack createFilterItem() {
        ItemStack item = new ItemStack(FILTER_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.displayName(Utils.color(Utils.msg("filter-name")));
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.color("&7" + Utils.msg("filter-lore-desc")));
        lore.add(Component.empty());

        String defColor = (filterIndex == 0) ? "&a" : "&f";
        String srvColor = (filterIndex == 1) ? "&a" : "&f";
        String mobColor = (filterIndex == 2) ? "&a" : "&f";
        String eventColor = (filterIndex == 3) ? "&a" : "&f";
        lore.add(Utils.color("&6► " + defColor + Utils.msg("filter-lore-default")));
        lore.add(Utils.color("&6► " + srvColor + Utils.msg("filter-lore-survival")));
        lore.add(Utils.color("&6► " + mobColor + Utils.msg("filter-lore-mob")));
        lore.add(Utils.color("&6► " + eventColor + Utils.msg("filter-lore-event")));

        lore.add(Component.empty());
        String visColor = visualMode ? "&6" : "&f";
        lore.add(Utils.color("&e► " + visColor + Utils.msg("filter-lore-visual")));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    protected boolean canToggleFilter() {
        long now = System.currentTimeMillis();
        if (now - lastFilterClickMs < 250)
            return false;
        lastFilterClickMs = now;
        return true;
    }

    protected void cycleFilterLeft() {
        filterIndex = (filterIndex + 1) % 4;
        page = 0;
        cachedSource = null;
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
        } catch (Exception ignored) {
        }
        open();
    }

    protected void toggleVisualRight() {
        visualMode = !visualMode;
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, visualMode ? 1.2f : 0.9f);
        } catch (Exception ignored) {
        }
        open();
    }

    protected void handleCommonClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta())
            return;

        int slot = event.getSlot();
        Feature[] source = getFilteredFeatures();
        int maxPage = Math.max(1, (source.length + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);

        if (slot == 26) {
            if (page + 1 < maxPage) {
                page++;
                open();
                try {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
                } catch (Exception ignored) {
                }
            }
            return;
        }
        if (slot == 18) {
            if (page > 0) {
                page--;
                open();
                try {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.05f);
                } catch (Exception ignored) {
                }
            }
            return;
        }

        if (slot == FILTER_SLOT && item.getType() == FILTER_MATERIAL) {
            if (!canToggleFilter())
                return;
            if (event.isLeftClick()) {
                cycleFilterLeft();
            } else if (event.isRightClick()) {
                toggleVisualRight();
            }
        }
    }

    public static String statsInLore(Feature feature, String lore) {
        if (lore != null && lore.contains("#")) {
            String[] parts = lore.split("#", 3);
            if (parts.length >= 3) {
                String stat = parts[1];
                return statsInLore(feature,
                        parts[0] + "&a" + DECIMAL_FORMAT.format(feature.getDouble(stat)) + "&7" + parts[2]);
            }
        }
        return lore;
    }

    protected int getAvailableSlot(Inventory inventory) {
        for (int slot : AVAILABLE_SLOTS) {
            if (inventory.getItem(slot) == null) {
                return slot;
            }
        }
        return -1;
    }
}
