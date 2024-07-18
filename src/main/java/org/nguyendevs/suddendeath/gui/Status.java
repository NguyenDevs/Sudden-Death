package org.nguyendevs.suddendeath.gui;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.Difficulty;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.Utils;
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

import java.util.ArrayList;
import java.util.List;

public class Status extends PluginInventory {
    private PlayerData data;

    public Status(Player player) {
        super(player);
        data = PlayerData.get(player);
    }

    @Override
    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(this, 45, ChatColor.UNDERLINE + Utils.msg("gui-name"));

        if (Feature.BLEEDING.isEnabled(player) && data.isBleeding()) {
            ItemStack item = new ItemStack(Material.RED_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + Utils.msg("gui-bleeding-name"));
            List<String> lore = new ArrayList<String>();
            for (String s : Utils.msgList("gui-bleeding-lore"))
                lore.add(ChatColor.GRAY + s);
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(getAvailableSlot(inv, "status"), item);
        }

        if (Feature.INFECTION.isEnabled(player) && data.isInfected()) {
            ItemStack item = new ItemStack(Material.ROTTEN_FLESH);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + Utils.msg("gui-infected-name"));
            List<String> lore = new ArrayList<String>();
            for (String s : Utils.msgList("gui-infected-lore"))
                lore.add(ChatColor.GRAY + s);
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(getAvailableSlot(inv, "status"), item);
        }

        if (inv.getItem(10) == null) {
            ItemStack item = new ItemStack(Material.RED_STAINED_GLASS);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + Utils.msg("gui-no-special-status-name"));
            List<String> lore = new ArrayList<>();
            for (String s : Utils.msgList("gui-no-special-status-lore"))
                lore.add(ChatColor.GRAY + s);
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(13, item);
        }

        if (!SuddenDeath.plugin.getConfig().getBoolean("disable-difficulties"))
            for (Difficulty difficulty : Difficulty.values()) {
                boolean has = data.hasDifficulty(difficulty);

                ItemStack item = difficulty.getNewItem();
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + (has ? "[" + Utils.msg("current") + "] " : "")
                        + ChatColor.translateAlternateColorCodes('&', difficulty.getName()));

                String difficultyBar = "||||||||||||||||||||";
                difficultyBar = ChatColor.GREEN + difficultyBar.substring(0, difficulty.getDifficultyIndex()) + ChatColor.DARK_GRAY
                        + difficultyBar.substring(difficulty.getDifficultyIndex());

                List<String> lore = new ArrayList<String>();
                lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
                for (String s : Utils.msgList("gui-difficulty-lore"))
                    lore.add(ChatColor.GRAY + s.replace("#health-malus#", "" + difficulty.getHealthMalus())
                            .replace("#increased-damage#", "" + difficulty.getIncreasedDamage()).replace("#difficulty#", difficultyBar));
                lore.add("");
                for (String s : difficulty.getLore())
                    lore.add(ChatColor.BLUE + ChatColor.translateAlternateColorCodes('&', s));
                if (lore.get(lore.size() - 1).equals(""))
                    lore = lore.subList(0, lore.size() - 1);
                lore.add(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------------------");
                if (!has) {
                    item.setType(Material.GRAY_DYE);
                    lore.add("");
                    lore.add(ChatColor.YELLOW + Utils.msg("gui-click-select-diff"));
                }

                meta.setLore(lore);
                meta.getPersistentDataContainer().set(Utils.nsk("difficultyId"), PersistentDataType.STRING, difficulty.name());
                item.setItemMeta(meta);

                inv.setItem(getAvailableSlot(inv, "difficulty"), item);
            }

        return inv;
    }

    private int getAvailableSlot(Inventory inv, String type) {
        int[] slots = type.equalsIgnoreCase("status") ? new int[]{10, 11, 12, 13, 14, 15, 16} : type.equalsIgnoreCase("difficulty") ? new int[]{29, 30, 31, 32, 33} : new int[]{};
        for (int available : slots)
            if (inv.getItem(available) == null)
                return available;
        return -1;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (!Utils.isPluginItem(item, true))
            return;

        String tag = item.getItemMeta().getPersistentDataContainer().get(Utils.nsk("difficultyId"), PersistentDataType.STRING);
        if (tag == null || tag.equals(""))
            return;

        Difficulty difficulty = Difficulty.valueOf(tag);
        if (!player.hasPermission(difficulty.getPermission()))
            return;

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
        data.setDifficulty(difficulty);
        difficulty.applyHealthMalus(data);
        player.sendMessage(ChatColor.YELLOW + Utils.msg("chose-diff").replace("#difficulty#", difficulty.getName()));
        open();
    }
}