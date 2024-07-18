package org.nguyendevs.suddendeath.gui;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MonsterEdition extends PluginInventory {
    private final EntityType type;
    private final String id;

    public MonsterEdition(Player player, EntityType type, String id) {
        super(player);

        this.type = type;
        this.id = id;
    }

    @Override
    public Inventory getInventory() {
        FileConfiguration config = new ConfigFile(type).getConfig();
        Inventory inv = Bukkit.createInventory(this, 54, ChatColor.UNDERLINE + "Mob Editor: " + id);

        for (MobStat stat : MobStat.values()) {
            ItemStack item = stat.getNewItem().clone();
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + stat.getName());
            meta.addItemFlags(ItemFlag.values());
            List<String> lore = new ArrayList<String>();
            for (String s : stat.getLore())
                lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s));

            lore.add("");
            if (stat.getType() == MobStat.Type.DOUBLE) {
                lore.add(ChatColor.GRAY + "Current Value: " + ChatColor.WHITE + config.getDouble(id + "." + stat.getPath()));
                lore.add("");
                lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Left click to change this value.");
            }
            if (stat.getType() == MobStat.Type.ITEMSTACK) {
                lore.add(ChatColor.GRAY + "Current Value:");

                ItemStack deserialized = ItemUtils.deserialize(config.getString(id + "." + stat.getPath()));
                String format = Utils.caseOnWords(deserialized.getType().name().toLowerCase().replace("_", " "));
                format += (deserialized.getAmount() > 0 ? " x" + deserialized.getAmount() : "");
                lore.add(ChatColor.AQUA + format);
                if (deserialized.hasItemMeta()) {
                    if (deserialized.getType().name().startsWith("LEATHER_"))
                        if (((LeatherArmorMeta) deserialized.getItemMeta()).getColor() != null)
                            lore.add(ChatColor.AQUA + "* Dye color: " + ((LeatherArmorMeta) deserialized.getItemMeta()).getColor().asRGB());
                    if (deserialized.getItemMeta().hasEnchants())
                        for (Enchantment ench : deserialized.getItemMeta().getEnchants().keySet())
                            lore.add(ChatColor.AQUA + "* " + Utils.caseOnWords(ench.getKey().getKey().replace("_", " ")) + " "
                                    + deserialized.getItemMeta().getEnchantLevel(ench));
                }
                lore.add("");
                lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Drag & drop an item to change this value.");
                lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Right click to remove this value.");
            }
            if (stat.getType() == MobStat.Type.STRING) {
                lore.add(ChatColor.GRAY + "Current Value: " + ChatColor.WHITE + config.getString(id + "." + stat.getPath()));
                lore.add("");
                lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Left click to change this value.");
            }
            if (stat.getType() == MobStat.Type.POTION_EFFECTS) {
                lore.add(ChatColor.GRAY + "Current Value:");
                if (!config.getConfigurationSection(id).contains(stat.getPath()))
                    lore.add(ChatColor.RED + "No permanent effect.");
                else if (config.getConfigurationSection(id + "." + stat.getPath()).getKeys(false).isEmpty())
                    lore.add(ChatColor.RED + "No permanent effect.");
                else
                    for (String s1 : config.getConfigurationSection(id + "." + stat.getPath()).getKeys(false)) {
                        String effect = s1;
                        effect = effect.replace("-", " ").replace("_", " ");
                        effect = effect.substring(0, 1).toUpperCase() + effect.substring(1).toLowerCase();
                        String level = Utils.intToRoman(config.getInt(id + "." + stat.getPath() + "." + s1));
                        lore.add("�b* " + effect + " " + level);
                    }
                lore.add("");
                lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Left click to add an effect.");
                lore.add(ChatColor.YELLOW + SpecialChar.listDash + " Right click to remove the last effect.");
            }
            meta.getPersistentDataContainer().set(Utils.nsk("mobStatId"), PersistentDataType.STRING, stat.name());
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(getAvailableSlot(inv), item);
        }

        ItemStack egg = new ItemStack(Material.CREEPER_SPAWN_EGG);
        ItemMeta eggMeta = egg.getItemMeta();
        eggMeta.setDisplayName(ChatColor.GREEN
                + (config.getString(id + ".name").equals("") ? id : ChatColor.translateAlternateColorCodes('&', config.getString(id + ".name"))));
        List<String> eggLore = new ArrayList<String>();
        eggLore.add(ChatColor.GRAY + type.name());
        eggMeta.setLore(eggLore);
        egg.setItemMeta(eggMeta);
        inv.setItem(4, egg);

        return inv;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (event.getClickedInventory() != event.getInventory())
            return;
        if (event.getSlot() == 4) {
            event.setCancelled(true);
            return;
        }
        if (!Utils.isPluginItem(item, false))
            return;
        if (item.getItemMeta().getDisplayName().length() < 2)
            return;

        String tag = item.getItemMeta().getPersistentDataContainer().get(Utils.nsk("mobStatId"), PersistentDataType.STRING);
        if (tag == null || tag.equals(""))
            return;

        MobStat stat = MobStat.valueOf(tag);
        event.setCancelled(true);
        ConfigFile config = new ConfigFile(type);

        if (stat.getType() == MobStat.Type.DOUBLE || stat.getType() == MobStat.Type.STRING) {
            new StatEditor(id, type, stat, config);
            player.closeInventory();
            seeChat(player);
            player.sendMessage(ChatColor.YELLOW + "Write in the chat the value you want!");
        }
        if (stat.getType() == MobStat.Type.ITEMSTACK) {
            if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                ItemStack c = event.getCursor();
                String serialized = ItemUtils.serialize(c);
                MainListener.cancelNextDrop(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                config.getConfig().set(id + "." + stat.getPath(), serialized);
                config.save();
                open();
                player.sendMessage(ChatColor.YELLOW + stat.getName() + " succesfully updated.");
            }
            if (event.getAction() == InventoryAction.PICKUP_HALF)
                if (config.getConfig().contains(id))
                    if (config.getConfig().getConfigurationSection(id).contains(stat.getPath()))
                        if (!config.getConfig().getString(id + "." + stat.getPath()).equals("[material=AIR:0]")) {
                            config.getConfig().set(id + "." + stat.getPath(), "[material=AIR:0]");
                            config.save();
                            player.sendMessage(ChatColor.YELLOW + "Succesfully removed " + stat.getName() + ".");
                            open();
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                        }
        }
        if (stat.getType() == MobStat.Type.POTION_EFFECTS) {
            if (event.getAction() == InventoryAction.PICKUP_ALL) {
                new StatEditor(id, type, stat, config);
                player.closeInventory();
                seeChat(player);
                player.sendMessage(ChatColor.YELLOW + "Write in the chat the permanent potion effect you want to add.");
                player.sendMessage(ChatColor.AQUA + "Format: [POTION_EFFECT] [AMPLIFIER]");
            }
            if (event.getAction() == InventoryAction.PICKUP_HALF)
                if (config.getConfig().getConfigurationSection(id).getKeys(false).contains(stat.getPath())) {
                    Set<String> set = config.getConfig().getConfigurationSection(id + "." + stat.getPath()).getKeys(false);
                    // get last element of array
                    String last = Arrays.asList(set.toArray(new String[0])).get(set.size() - 1);
                    config.getConfig().set(id + "." + stat.getPath() + "." + last, null);
                    if (set.size() <= 1)
                        config.getConfig().set(id + "." + stat.getPath(), null);
                    config.save();
                    open();
                    player.sendMessage(ChatColor.YELLOW + "Succesfully removed " + last.substring(0, 1).toUpperCase()
                            + last.substring(1).toLowerCase() + ChatColor.GRAY + ".");
                }
        }
    }

    private void seeChat(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------------------------------------------");
        player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Mob Edition", "See chat.", 10, 40, 10);
        new BukkitRunnable() {
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