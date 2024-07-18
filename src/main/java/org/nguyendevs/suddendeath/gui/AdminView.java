package org.nguyendevs.suddendeath.gui;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AdminView extends PluginInventory {
    private int page;

    private static final DecimalFormat digit = new DecimalFormat("0.###");

    public AdminView(Player player) {
        super(player);
    }

    @Override
    public Inventory getInventory() {
        int maxPage = 1 + Feature.values().length / 21;
        Inventory inv = Bukkit.createInventory(this, 45, ChatColor.UNDERLINE + "SD Admin GUI (" + (page + 1) + "/" + maxPage + ")");

        for (int j = page * 21; j < Math.min(Feature.values().length, (page + 1) * 21); j++) {
            Feature feature = Feature.values()[j];
            List<String> list = SuddenDeath.plugin.getConfig().getStringList(feature.getPath());
            ItemStack item = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + feature.getName());
            List<String> lore = new ArrayList<>();
            lore.add("");
            for (String s : feature.getLore()) {
                s = statsInLore(feature, s, feature.name());
                lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s));
            }
            if (!list.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.GRAY + "This feature is enabled in:");
                for (String s : list)
                    lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + s);
            }
            lore.add("");
            if (list.contains(player.getWorld().getName())) {
                lore.add(ChatColor.GREEN + "This feature is enabled in this world.");
                lore.add(ChatColor.YELLOW + "Click to disable.");
                item.setType(feature.isEvent() ? Material.LIGHT_BLUE_DYE : Material.LIME_DYE);
            } else {
                lore.add(ChatColor.RED + "This feature is disabled in this world.");
                lore.add(ChatColor.YELLOW + "Click to enable.");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(Utils.nsk("featureId"), PersistentDataType.STRING, feature.name());
            item.setItemMeta(meta);

            inv.setItem(getAvailableSlot(inv), item);
        }

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(ChatColor.GREEN + "Next");
        next.setItemMeta(nextMeta);

        ItemStack previous = new ItemStack(Material.ARROW);
        ItemMeta previousMeta = previous.getItemMeta();
        previousMeta.setDisplayName(ChatColor.GREEN + "Previous");
        previous.setItemMeta(previousMeta);

        if (page > 0)
            inv.setItem(18, previous);
        if ((page + 1) * 3 * 7 < Feature.values().length)
            inv.setItem(26, next);

        return inv;
    }

    @Override
    public void whenClicked(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        e.setCancelled(true);
        if (e.getInventory() != e.getClickedInventory() || !Utils.isPluginItem(item, false))
            return;

        if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Next")) {
            page++;
            open();
        }

        if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Previous")) {
            page--;
            open();
        }

        String tag = item.getItemMeta().getPersistentDataContainer().get(Utils.nsk("featureId"), PersistentDataType.STRING);
        if (tag == null || tag.equals(""))
            return;

        Feature feature = Feature.valueOf(tag);
        List<String> list = SuddenDeath.plugin.getConfig().getStringList(feature.getPath());
        if (list.contains(player.getWorld().getName())) {
            list.remove(player.getWorld().getName());
            SuddenDeath.plugin.getConfig().set(feature.getPath(), list);
            SuddenDeath.plugin.saveConfig();
            player.sendMessage(ChatColor.YELLOW + "You disabled " + ChatColor.GOLD + feature.getName() + ChatColor.YELLOW + " in " + ChatColor.GOLD
                    + player.getWorld().getName() + ChatColor.YELLOW + ".");
            open();
            return;
        }
        list.add(player.getWorld().getName());
        SuddenDeath.plugin.getConfig().set(feature.getPath(), list);
        SuddenDeath.plugin.saveConfig();
        player.sendMessage(ChatColor.YELLOW + "You enabled " + ChatColor.GOLD + feature.getName() + ChatColor.YELLOW + " in " + ChatColor.GOLD
                + player.getWorld().getName() + ChatColor.YELLOW + ".");
        open();

    }

    public static String statsInLore(Feature feature, String lore1, String mainName) {
        if (lore1.contains("#")) {
            String stat = lore1.split("#")[1];
            lore1 = lore1.replace("#" + stat + "#", ChatColor.WHITE + "" + digit.format(feature.getDouble(stat)) + ChatColor.GRAY);
            lore1 = statsInLore(feature, lore1, mainName);
        }
        return lore1;
    }

    private int getAvailableSlot(Inventory inv) {
        Integer[] slots = new Integer[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int available : slots)
            if (inv.getItem(available) == null)
                return available;
        return -1;
    }
}