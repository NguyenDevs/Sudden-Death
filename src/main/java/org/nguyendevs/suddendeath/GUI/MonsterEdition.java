package org.nguyendevs.suddendeath.GUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Features.player.PlayerCoreFeature;
import org.nguyendevs.suddendeath.Utils.ConfigFile;
import org.nguyendevs.suddendeath.Utils.ItemUtils;
import org.nguyendevs.suddendeath.Utils.MobStat;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

public class MonsterEdition extends PluginInventory {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String PREFIX = "&6[&cSudden&4Death&6]";
    private static final int[] AVAILABLE_SLOTS = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39,
            40, 41, 42, 43 };
    private static final int SPAWN_TYPE_SLOT = 49;
    private static final String[] SPAWN_TYPES = { "All", "Spawners", "Natural Only", "Regular Spawner Only",
            "Trial Spawner Only" };
    private long lastSpawnTypeClickMs = 0L;
    private static final String TITLE_PREFIX = "Mob Editor: ";
    private final EntityType type;
    private final String id;

    public MonsterEdition(Player player, EntityType type, String id) {
        super(player);
        if (type == null || id == null)
            throw new IllegalArgumentException("EntityType and ID cannot be null");
        this.type = type;
        this.id = id;
    }

    @Override
    public @NotNull Inventory getInventory() {
        FileConfiguration config = SuddenDeath.getInstance().getConfigManager().getMobConfig(type).getConfig();
        Inventory inventory = Bukkit.createInventory(this, 54, color(TITLE_PREFIX + id));
        try {
            for (MobStat stat : MobStat.values()) {
                inventory.setItem(getAvailableSlot(inventory), createMobStatItem(stat, config));
            }
            inventory.setItem(4, createMobEggItem(config));
            inventory.setItem(SPAWN_TYPE_SLOT, createSpawnTypeItem(config));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error creating inventory", e);
        }
        return inventory;
    }

    private ItemStack createMobStatItem(MobStat stat, FileConfiguration config) {
        ItemStack item = stat.getNewItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.displayName(color("&a" + stat.getName()));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(Objects.requireNonNull(Utils.nsk("mobStatId")), PersistentDataType.STRING, stat.name());
        meta.lore(createMobStatLore(stat, config));
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> createMobStatLore(MobStat stat, FileConfiguration config) {
        List<Component> lore = new ArrayList<>();
        for (String line : stat.getLore())
            lore.add(color("&7" + line));
        lore.add(Component.empty());
        switch (stat.getType()) {
            case DOUBLE:
                double value = config.getDouble(id + "." + stat.getPath(), 0.0);
                lore.add(color("&7Current Value: &a" + value));
                if (stat == MobStat.SPAWN_COEFFICIENT) {
                    double actualSpawnRate = calculateActualSpawnRate(value, config);
                    lore.add(color("&7Actual Spawn Rate: &b" + Math.round(actualSpawnRate) + "%"));
                }
                lore.add(Component.empty());
                lore.add(color("&e► Left click to change this value."));
                break;
            case ITEMSTACK:
                addItemStackLore(lore, config, stat);
                break;
            case STRING:
                lore.add(color("&7Current Value: &a" + config.getString(id + "." + stat.getPath(), "")));
                lore.add(Component.empty());
                lore.add(color("&e► Left click to change this value."));
                break;
            case POTION_EFFECTS:
                addPotionEffectsLore(lore, config, stat);
                break;
        }
        return lore;
    }

    private double calculateActualSpawnRate(double customMobWeight, FileConfiguration config) {

        double totalWeight = SuddenDeath.getInstance().getConfigManager().getMainConfig().getConfig()
                .getDouble("default-spawn-coef." + type.name(), 0.0);
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null || !section.contains("spawn-coef"))
                continue;
            totalWeight += section.getDouble("spawn-coef", 0.0);
        }

        if (totalWeight <= 0)
            return 0.0;
        return (customMobWeight / totalWeight) * 100.0;
    }

    private void addItemStackLore(List<Component> lore, FileConfiguration config, MobStat stat) {
        lore.add(color("&7Current Value:"));
        ItemStack deserialized = ItemUtils.deserialize(config.getString(id + "." + stat.getPath()));
        String format = Utils.caseOnWords(deserialized.getType().name().toLowerCase().replace("_", " "));
        format += (deserialized.getAmount() > 0 ? " x" + deserialized.getAmount() : "");
        lore.add(color("&b" + format));
        if (deserialized.hasItemMeta() && deserialized.getItemMeta() != null) {
            if (deserialized.getType().name().startsWith("LEATHER_")) {
                LeatherArmorMeta leatherMeta = (LeatherArmorMeta) deserialized.getItemMeta();
                leatherMeta.getColor();
                lore.add(color("&b* Dye color: " + leatherMeta.getColor().asRGB()));
            }
            if (deserialized.getItemMeta().hasEnchants()) {
                for (Enchantment ench : deserialized.getItemMeta().getEnchants().keySet()) {
                    lore.add(color("&b* " + Utils.caseOnWords(ench.getKey().getKey().replace("_", " ")) + " "
                            + deserialized.getItemMeta().getEnchantLevel(ench)));
                }
            }
        }
        lore.add(Component.empty());
        lore.add(color("&e► Drag & drop an item to change this value."));
        lore.add(color("&e► Right click to remove this value."));
    }

    private void addPotionEffectsLore(List<Component> lore, FileConfiguration config, MobStat stat) {
        lore.add(color("&7Current Value:"));
        ConfigurationSection section = config.getConfigurationSection(id + "." + stat.getPath());
        if (section == null || section.getKeys(false).isEmpty()) {
            lore.add(color("&cNo permanent effect."));
        } else {
            for (String effect : section.getKeys(false)) {
                String formattedEffect = Utils.caseOnWords(effect.replace("_", " ").toLowerCase());
                String level = Utils.intToRoman(section.getInt(effect));
                lore.add(color("&b* " + formattedEffect + " " + level));
            }
        }
        lore.add(Component.empty());
        lore.add(color("&e► Left click to add an effect."));
        lore.add(color("&e► Right click to remove the last effect."));
    }

    private ItemStack createSpawnTypeItem(FileConfiguration config) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.displayName(color("&aSPAWN TYPE"));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(Objects.requireNonNull(Utils.nsk("spawnTypeItem")), PersistentDataType.STRING, "SPAWN_TYPE");
        String currentType = config.getString(id + ".spawn-type", "All");
        List<Component> lore = new ArrayList<>();
        lore.add(color("&7Spawn type for this mob"));
        lore.add(Component.empty());
        for (String spawnType : SPAWN_TYPES) {
            if (spawnType.equals(currentType)) {
                lore.add(color("&6► &a" + spawnType));
            } else {
                lore.add(color("&6► &f" + spawnType));
            }
        }
        lore.add(Component.empty());
        lore.add(color("&e► Click to change spawn type."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMobEggItem(FileConfiguration config) {
        Material eggMaterial = getSpawnEggMaterial(type);

        ItemStack egg = new ItemStack(eggMaterial);
        ItemMeta meta = egg.getItemMeta();
        if (meta == null)
            return egg;

        String name = config.getString(id + ".name", id);
        meta.displayName(color("&a" + (name.isEmpty() ? id : name)));

        List<Component> lore = new ArrayList<>();
        lore.add(color("&7" + type.name()));
        meta.lore(lore);

        egg.setItemMeta(meta);
        return egg;
    }

    private Material getSpawnEggMaterial(EntityType type) {
        try {
            String eggName = type.name() + "_SPAWN_EGG";
            Material eggMaterial = Material.matchMaterial(eggName);

            if (eggMaterial != null && eggMaterial.isItem()) {
                return eggMaterial;
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Could not find spawn egg for " + type.name(), e);
        }

        return Material.PIG_SPAWN_EGG;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();
        if (clickedInv == null)
            return;
        if (clickedInv.equals(topInv)) {
            if (!isAvailableSlot(event.getSlot()) && event.getSlot() != 4 && event.getSlot() != SPAWN_TYPE_SLOT) {
                event.setCancelled(true);
                return;
            }
            if (event.getSlot() == 4) {
                event.setCancelled(true);
                return;
            }
            if (event.getSlot() == SPAWN_TYPE_SLOT) {
                event.setCancelled(true);
                if (!event.isLeftClick())
                    return;
                long now = System.currentTimeMillis();
                if (now - lastSpawnTypeClickMs < 250)
                    return;
                lastSpawnTypeClickMs = now;
                ConfigFile config = SuddenDeath.getInstance().getConfigManager().getMobConfig(type);
                String current = config.getConfig().getString(id + ".spawn-type", "All");
                int idx = 0;
                for (int i = 0; i < SPAWN_TYPES.length; i++) {
                    if (SPAWN_TYPES[i].equals(current)) {
                        idx = i;
                        break;
                    }
                }
                String next = SPAWN_TYPES[(idx + 1) % SPAWN_TYPES.length];
                config.getConfig().set(id + ".spawn-type", next);
                config.save();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.05f);
                player.sendMessage(color(PREFIX + " &eSpawn type set to &6" + next + "&e."));
                open();
                return;
            }
        }
        ItemStack item = event.getCurrentItem();
        if (!Utils.isPluginItem(item, false))
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null)
            return;
        String tag = meta.getPersistentDataContainer().get(Objects.requireNonNull(Utils.nsk("mobStatId")), PersistentDataType.STRING);
        if (tag == null || tag.isEmpty())
            return;
        event.setCancelled(true);
        try {
            MobStat stat = MobStat.valueOf(tag);
            ConfigFile config = SuddenDeath.getInstance().getConfigManager().getMobConfig(type);
            switch (stat.getType()) {
                case DOUBLE:
                case STRING:
                    handleDoubleOrStringStat(stat, config);
                    break;
                case ITEMSTACK:
                    handleItemStackStat(event, stat, config);
                    break;
                case POTION_EFFECTS:
                    handlePotionEffectsStat(event, stat, config);
                    break;
            }
        } catch (Exception e) {
            player.sendMessage(color(PREFIX + " &cAn error occurred."));
        }
    }

    private void handleDoubleOrStringStat(MobStat stat, ConfigFile config) {
        new StatEditor(id, type, stat, config);
        player.closeInventory();
        promptChatInput();
        player.sendMessage(color(PREFIX + " &eWrite in the chat the value you want!"));
    }

    private void handleItemStackStat(InventoryClickEvent event, MobStat stat, ConfigFile config) {
        if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR || event.getAction() == InventoryAction.PLACE_ALL
                || event.getAction() == InventoryAction.PLACE_SOME) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem.getType() != Material.AIR) {
                String serialized = ItemUtils.serialize(cursorItem);
                PlayerCoreFeature.cancelNextDrop(player);
                config.getConfig().set(id + "." + stat.getPath(), serialized);
                config.save();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(color(PREFIX + " &e" + stat.getName() + " successfully updated."));
                event.setCancelled(true);
                open();
            } else {
                player.sendMessage(color(PREFIX + " &cNo item on cursor to place."));
            }
        } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
            ConfigurationSection section = config.getConfig().getConfigurationSection(id);
            if (section != null && section.contains(stat.getPath()) &&
                    !"[material=AIR:0]".equals(config.getConfig().getString(id + "." + stat.getPath()))) {
                config.getConfig().set(id + "." + stat.getPath(), "[material=AIR:0]");
                config.save();
                player.sendMessage(color(PREFIX + " &eSuccessfully removed " + stat.getName() + "."));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                open();
            }
        }
    }

    private void handlePotionEffectsStat(InventoryClickEvent event, MobStat stat, ConfigFile config) {
        if (event.getAction() == InventoryAction.PICKUP_ALL) {
            new StatEditor(id, type, stat, config);
            player.closeInventory();
            promptChatInput();
            player.sendMessage(color(PREFIX + " &eWrite in the chat the permanent potion effect you want to add."));
            player.sendMessage(color("&f► &bFormat: [POTION_EFFECT] [AMPLIFIER]"));
        } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
            ConfigurationSection section = config.getConfig().getConfigurationSection(id + "." + stat.getPath());
            if (section != null && !section.getKeys(false).isEmpty()) {
                Set<String> effects = section.getKeys(false);
                String lastEffect = new ArrayList<>(effects).get(effects.size() - 1);
                config.getConfig().set(id + "." + stat.getPath() + "." + lastEffect, null);
                if (effects.size() <= 1) {
                    config.getConfig().set(id + "." + stat.getPath(), null);
                }
                config.save();
                player.sendMessage(
                        color(PREFIX + " &eSuccessfully removed " + Utils.caseOnWords(lastEffect.toLowerCase()) + "."));
                open();
            }
        }
    }

    private void promptChatInput() {
        player.sendMessage(color("&8&m-----------------------------------------------------"));
        player.showTitle(Title.title(
                color("&6&lMob Edition"),
                color("&fSee chat."),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))));
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    player.sendMessage(color("&f► &eType 'cancel' to abort editing the mob."));
                } catch (Exception ignored) {
                }
            }
        }.runTaskLater(SuddenDeath.getInstance(), 0);
    }

    private int getAvailableSlot(Inventory inventory) {
        for (int slot : AVAILABLE_SLOTS) {
            if (inventory.getItem(slot) == null)
                return slot;
        }
        return -1;
    }

    private boolean isAvailableSlot(int slot) {
        return Arrays.stream(AVAILABLE_SLOTS).anyMatch(s -> s == slot);
    }

    private static Component color(String message) {
        return LEGACY.deserialize(message);
    }
}