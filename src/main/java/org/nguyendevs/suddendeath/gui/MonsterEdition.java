package org.nguyendevs.suddendeath.gui;

import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.listener.MainListener;
import org.nguyendevs.suddendeath.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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

import java.util.*;

public class MonsterEdition extends PluginInventory {
    private final EntityType type;
    private final String id;

    public MonsterEdition(Player player, EntityType type, String id) {
        super(player);
        this.type = type;
        this.id = id;
    }

    @Override
    public @NotNull Inventory getInventory() {
        FileConfiguration config = new ConfigFile(type).getConfig();
        Inventory inv = Bukkit.createInventory(this, 54, ChatColor.UNDERLINE + "Mob Editor: " + id);

        for (MobStat stat : MobStat.values()) {
            ItemStack item = stat.getNewItem().clone();
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.GREEN + stat.getName());
            meta.addItemFlags(ItemFlag.values());
            List<String> lore = new ArrayList<>();
            for (String s : stat.getLore())
                lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s));

            lore.add("");
            switch (stat.getType()) {
                case DOUBLE:
                    lore.add(ChatColor.GRAY + "Current Value: " + ChatColor.WHITE + config.getDouble(id + "." + stat.getPath()));
                    lore.add("");
                    lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Left click to change this value.");
                    break;
                case ITEMSTACK:
                    lore.add(ChatColor.GRAY + "Current Value:");
                    ItemStack deserialized = ItemUtils.deserialize(config.getString(id + "." + stat.getPath()));
                    String format = Utils.caseOnWords(deserialized.getType().name().toLowerCase().replace("_", " "));
                    format += (deserialized.getAmount() > 0 ? " x" + deserialized.getAmount() : "");
                    lore.add(ChatColor.AQUA + format);
                    if (deserialized.hasItemMeta()) {
                        if (deserialized.getType().name().startsWith("LEATHER_") && ((LeatherArmorMeta) deserialized.getItemMeta()).getColor() != null)
                            lore.add(ChatColor.AQUA + "* Dye color: " + ((LeatherArmorMeta) deserialized.getItemMeta()).getColor().asRGB());
                        if (Objects.requireNonNull(deserialized.getItemMeta()).hasEnchants())
                            for (Enchantment ench : deserialized.getItemMeta().getEnchants().keySet())
                                lore.add(ChatColor.AQUA + "* " + Utils.caseOnWords(ench.getKey().getKey().replace("_", " ")) + " " + deserialized.getItemMeta().getEnchantLevel(ench));
                    }
                    lore.add("");
                    lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Drag & drop an item to change this value.");
                    lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Right click to remove this value.");
                    break;
                case STRING:
                    lore.add(ChatColor.GRAY + "Current Value: " + ChatColor.WHITE + config.getString(id + "." + stat.getPath()));
                    lore.add("");
                    lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Left click to change this value.");
                    break;
                case POTION_EFFECTS:
                    lore.add(ChatColor.GRAY + "Current Value:");
                    if (!Objects.requireNonNull(config.getConfigurationSection(id)).contains(stat.getPath()))
                        lore.add(ChatColor.RED + "No permanent effect.");
                    else if (Objects.requireNonNull(config.getConfigurationSection(id + "." + stat.getPath())).getKeys(false).isEmpty())
                        lore.add(ChatColor.RED + "No permanent effect.");
                    else
                        for (String s1 : Objects.requireNonNull(config.getConfigurationSection(id + "." + stat.getPath())).getKeys(false)) {
                            String effect = s1.replace("-", " ").replace("_", " ").toLowerCase();
                            effect = effect.substring(0, 1).toUpperCase() + effect.substring(1);
                            String level = Utils.intToRoman(config.getInt(id + "." + stat.getPath() + "." + s1));
                            lore.add(ChatColor.AQUA + "* " + effect + " " + level);
                        }
                    lore.add("");
                    lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Left click to add an effect.");
                    lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Right click to remove the last effect.");
                    break;
            }
            meta.getPersistentDataContainer().set(Utils.nsk("mobStatId"), PersistentDataType.STRING, stat.name());
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(getAvailableSlot(inv), item);
        }

        ItemStack egg = new ItemStack(Material.CREEPER_SPAWN_EGG);
        ItemMeta eggMeta = egg.getItemMeta();
        assert eggMeta != null;
        eggMeta.setDisplayName(ChatColor.GREEN + (Objects.equals(config.getString(id + ".name"), "") ? id : ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(config.getString(id + ".name")))));
        List<String> eggLore = new ArrayList<>();
        eggLore.add(ChatColor.GRAY + type.name());
        eggMeta.setLore(eggLore);
        egg.setItemMeta(eggMeta);
        inv.setItem(4, egg);

        return inv;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() != event.getInventory()) return;
        if (event.getSlot() == 4) {
            event.setCancelled(true);
            return;
        }
        if (!Utils.isPluginItem(item, false)) return;
        if (Objects.requireNonNull(item.getItemMeta()).getDisplayName().length() < 2) return;

        String tag = item.getItemMeta().getPersistentDataContainer().get(Utils.nsk("mobStatId"), PersistentDataType.STRING);
        if (tag == null || tag.isEmpty()) return;

        MobStat stat = MobStat.valueOf(tag);
        event.setCancelled(true);
        ConfigFile config = new ConfigFile(type);

        switch (stat.getType()) {
            case DOUBLE:
            case STRING:
                new StatEditor(id, type, stat, config);
                player.closeInventory();
                seeChat(player);
                player.sendMessage(ChatColor.YELLOW + "Write in the chat the value you want!");
                break;
            case ITEMSTACK:
                if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                    ItemStack cursorItem = event.getCursor();
                    assert cursorItem != null;
                    String serialized = ItemUtils.serialize(cursorItem);
                    MainListener.cancelNextDrop(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    config.getConfig().set(id + "." + stat.getPath(), serialized);
                    config.save();
                    open();
                    player.sendMessage(ChatColor.YELLOW + stat.getName() + " successfully updated.");
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (config.getConfig().contains(id) && Objects.requireNonNull(config.getConfig().getConfigurationSection(id)).contains(stat.getPath())
                            && !Objects.equals(config.getConfig().getString(id + "." + stat.getPath()), "[material=AIR:0]")) {
                        config.getConfig().set(id + "." + stat.getPath(), "[material=AIR:0]");
                        config.save();
                        player.sendMessage(ChatColor.YELLOW + "Successfully removed " + stat.getName() + ".");
                        open();
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    }
                }
                break;
            case POTION_EFFECTS:
                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    new StatEditor(id, type, stat, config);
                    player.closeInventory();
                    seeChat(player);
                    player.sendMessage(ChatColor.YELLOW + "Write in the chat the permanent potion effect you want to add.");
                    player.sendMessage(ChatColor.AQUA + "Format: [POTION_EFFECT] [AMPLIFIER]");
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (Objects.requireNonNull(config.getConfig().getConfigurationSection(id)).getKeys(false).contains(stat.getPath())) {
                        Set<String> effects = Objects.requireNonNull(config.getConfig().getConfigurationSection(id + "." + stat.getPath())).getKeys(false);
                        String lastEffect = new ArrayList<>(effects).get(effects.size() - 1);
                        config.getConfig().set(id + "." + stat.getPath() + "." + lastEffect, null);
                        if (effects.size() <= 1)
                            config.getConfig().set(id + "." + stat.getPath(), null);
                        config.save();
                        open();
                        player.sendMessage(ChatColor.YELLOW + "Successfully removed " + lastEffect.substring(0, 1).toUpperCase() + lastEffect.substring(1).toLowerCase() + ChatColor.GRAY + ".");
                    }
                }
                break;
        }
    }

    private void seeChat(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------------------------------------------");
        player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Mob Edition", "See chat.", 10, 40, 10);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(ChatColor.YELLOW + "Type 'cancel' to abort editing the mob.");
            }
        }.runTaskLater(SuddenDeath.plugin, 0);
    }

    private int getAvailableSlot(Inventory inv) {
        Integer[] slots = new Integer[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int available : slots)
            if (inv.getItem(available) == null)
                return available;
        return -1;
    }
}
