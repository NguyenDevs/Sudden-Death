package org.nguyendevs.suddendeath.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.nguyendevs.suddendeath.features.player.PlayerCoreFeature;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.util.ItemUtils;
import org.nguyendevs.suddendeath.util.MobStat;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class MonsterEdition extends PluginInventory {
    // ... (Giữ nguyên các biến static và constructor)
    private static final String PREFIX = "&6[&cSudden&4Death&6]";
    private static final int[] AVAILABLE_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final String TITLE_PREFIX = "Mob Editor: ";
    private final EntityType type;
    private final String id;

    public MonsterEdition(Player player, EntityType type, String id) {
        super(player);
        if (type == null || id == null) throw new IllegalArgumentException("EntityType and ID cannot be null");
        this.type = type;
        this.id = id;
    }

    // ... (Giữ nguyên getInventory, createMobStatItem, createMobStatLore)
    @Override
    public @NotNull Inventory getInventory() {
        FileConfiguration config = new ConfigFile(type).getConfig();
        Inventory inventory = Bukkit.createInventory(this, 54, translateColors(TITLE_PREFIX + id));
        try {
            for (MobStat stat : MobStat.values()) {
                inventory.setItem(getAvailableSlot(inventory), createMobStatItem(stat, config));
            }
            inventory.setItem(4, createMobEggItem(config));
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error creating inventory", e);
        }
        return inventory;
    }

    private ItemStack createMobStatItem(MobStat stat, FileConfiguration config) {
        ItemStack item = stat.getNewItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(translateColors("&a" + stat.getName()));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(Utils.nsk("mobStatId"), PersistentDataType.STRING, stat.name());
        meta.setLore(createMobStatLore(stat, config));
        item.setItemMeta(meta);
        return item;
    }

    private List<String> createMobStatLore(MobStat stat, FileConfiguration config) {
        List<String> lore = new ArrayList<>();
        for (String line : stat.getLore()) lore.add(translateColors("&7" + line));
        lore.add("");
        switch (stat.getType()) {
            case DOUBLE:
                lore.add(translateColors("&7Current Value: &f" + config.getDouble(id + "." + stat.getPath(), 0.0)));
                lore.add("");
                lore.add(translateColors("&e► Left click to change this value."));
                break;
            case ITEMSTACK:
                addItemStackLore(lore, config, stat);
                break;
            case STRING:
                lore.add(translateColors("&7Current Value: &f" + config.getString(id + "." + stat.getPath(), "")));
                lore.add("");
                lore.add(translateColors("&e► Left click to change this value."));
                break;
            case POTION_EFFECTS:
                addPotionEffectsLore(lore, config, stat);
                break;
        }
        return lore;
    }

    private void addItemStackLore(List<String> lore, FileConfiguration config, MobStat stat) {
        lore.add(translateColors("&7Current Value:"));
        ItemStack deserialized = ItemUtils.deserialize(config.getString(id + "." + stat.getPath()));
        String format = Utils.caseOnWords(deserialized.getType().name().toLowerCase().replace("_", " "));
        format += (deserialized.getAmount() > 0 ? " x" + deserialized.getAmount() : "");
        lore.add(translateColors("&b" + format));
        if (deserialized.hasItemMeta() && deserialized.getItemMeta() != null) {
            if (deserialized.getType().name().startsWith("LEATHER_")) {
                LeatherArmorMeta leatherMeta = (LeatherArmorMeta) deserialized.getItemMeta();
                if (leatherMeta.getColor() != null) lore.add(translateColors("&b* Dye color: " + leatherMeta.getColor().asRGB()));
            }
            if (deserialized.getItemMeta().hasEnchants()) {
                for (Enchantment ench : deserialized.getItemMeta().getEnchants().keySet()) {
                    lore.add(translateColors("&b* " + Utils.caseOnWords(ench.getKey().getKey().replace("_", " ")) + " " + deserialized.getItemMeta().getEnchantLevel(ench)));
                }
            }
        }
        lore.add("");
        lore.add(translateColors("&e► Drag & drop an item to change this value."));
        lore.add(translateColors("&e► Right click to remove this value."));
    }

    private void addPotionEffectsLore(List<String> lore, FileConfiguration config, MobStat stat) {
        lore.add(translateColors("&7Current Value:"));
        ConfigurationSection section = config.getConfigurationSection(id + "." + stat.getPath());
        if (section == null || section.getKeys(false).isEmpty()) {
            lore.add(translateColors("&cNo permanent effect."));
        } else {
            for (String effect : section.getKeys(false)) {
                String formattedEffect = Utils.caseOnWords(effect.replace("_", " ").toLowerCase());
                String level = Utils.intToRoman(section.getInt(effect));
                lore.add(translateColors("&b* " + formattedEffect + " " + level));
            }
        }
        lore.add("");
        lore.add(translateColors("&e► Left click to add an effect."));
        lore.add(translateColors("&e► Right click to remove the last effect."));
    }

    private ItemStack createMobEggItem(FileConfiguration config) {
        ItemStack egg = new ItemStack(Material.CREEPER_SPAWN_EGG);
        ItemMeta meta = egg.getItemMeta();
        if (meta == null) return egg;
        String name = config.getString(id + ".name", id);
        meta.setDisplayName(translateColors("&a" + (name.isEmpty() ? id : name)));
        List<String> lore = new ArrayList<>();
        lore.add(translateColors("&7" + type.name()));
        meta.setLore(lore);
        egg.setItemMeta(meta);
        return egg;
    }

    // ... (Giữ nguyên whenClicked, handleDoubleOrStringStat)
    @Override
    public void whenClicked(InventoryClickEvent event) {
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();
        if (clickedInv == null || topInv == null) return;
        if (clickedInv.equals(topInv)) {
            if (!isAvailableSlot(event.getSlot()) && event.getSlot() != 4) {
                event.setCancelled(true);
                return;
            }
            if (event.getSlot() == 4) {
                event.setCancelled(true);
                return;
            }
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !Utils.isPluginItem(item, false)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getDisplayName().isEmpty()) return;
        String tag = meta.getPersistentDataContainer().get(Utils.nsk("mobStatId"), PersistentDataType.STRING);
        if (tag == null || tag.isEmpty()) return;
        event.setCancelled(true);
        try {
            MobStat stat = MobStat.valueOf(tag);
            ConfigFile config = new ConfigFile(type);
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
            player.sendMessage(translateColors(PREFIX + " " + "&cAn error occurred."));
        }
    }

    private void handleDoubleOrStringStat(MobStat stat, ConfigFile config) {
        new StatEditor(id, type, stat, config);
        player.closeInventory();
        promptChatInput();
        player.sendMessage(translateColors(PREFIX + " " + "&eWrite in the chat the value you want!"));
    }

    private void handleItemStackStat(InventoryClickEvent event, MobStat stat, ConfigFile config) {
        if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR || event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_SOME) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                String serialized = ItemUtils.serialize(cursorItem);
                // CHANGE HERE: Use PlayerCoreFeature instead of MainListener
                PlayerCoreFeature.cancelNextDrop(player);
                config.getConfig().set(id + "." + stat.getPath(), serialized);
                config.save();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(translateColors(PREFIX + " " + "&e" + stat.getName() + " successfully updated."));
                event.setCancelled(true);
                open();
            } else {
                player.sendMessage(translateColors(PREFIX + " " + "&cNo item on cursor to place."));
            }
        } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
            ConfigurationSection section = config.getConfig().getConfigurationSection(id);
            if (section != null && section.contains(stat.getPath()) &&
                    !"[material=AIR:0]".equals(config.getConfig().getString(id + "." + stat.getPath()))) {
                config.getConfig().set(id + "." + stat.getPath(), "[material=AIR:0]");
                config.save();
                player.sendMessage(translateColors(PREFIX + " " + "&eSuccessfully removed " + stat.getName() + "."));
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
            player.sendMessage(translateColors(PREFIX + " " + "&eWrite in the chat the permanent potion effect you want to add."));
            player.sendMessage(translateColors("&f► &bFormat: [POTION_EFFECT] [AMPLIFIER]"));
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
                player.sendMessage(translateColors(PREFIX + " " + "&eSuccessfully removed " + Utils.caseOnWords(lastEffect.toLowerCase()) + "."));
                open();
            }
        }
    }

    private void promptChatInput() {
        player.sendMessage(translateColors("&8&m-----------------------------------------------------"));
        player.sendTitle(translateColors("&6&lMob Edition"), translateColors("&fSee chat."), 10, 40, 10);
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    player.sendMessage(translateColors("&f► &eType 'cancel' to abort editing the mob."));
                } catch (Exception e) {}
            }
        }.runTaskLater(SuddenDeath.getInstance(), 0);
    }

    private int getAvailableSlot(Inventory inventory) {
        for (int slot : AVAILABLE_SLOTS) {
            if (inventory.getItem(slot) == null) return slot;
        }
        return -1;
    }
    private boolean isAvailableSlot(int slot) { return Arrays.stream(AVAILABLE_SLOTS).anyMatch(s -> s == slot); }
    private String translateColors(String message) { return ChatColor.translateAlternateColorCodes('&', message); }
}