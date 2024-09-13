package org.nguyendevs.suddendeath.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public enum CustomItem {
	BANDAGE(Material.PAPER, "&fBandage", new String[] { "Stops Bleeding." }, new String[] { "AIR,AIR,AIR", "PAPER,STICK,PAPER", "AIR,AIR,AIR" }),
	STRANGE_BREW(Material.BEETROOT_SOUP, "&fStrange Brew", new String[] { "Stops Infection." }, new String[] { "AIR,AIR,AIR", "INK_SAC,BOWL,CLAY_BALL", "AIR,AIR,AIR" }),
	RAW_HUMAN_FLESH(Material.BEEF, "Human Flesh", new String[] { "Some fresh human meet.", "I wonder if I can cook it?" }),
	HUMAN_BONE(Material.BONE, "Human Bone"),
	COOKED_HUMAN_FLESH(Material.COOKED_BEEF, "Cooked Human Flesh", new String[] { "Looks tasty!" }),
	SHARP_KNIFE(Material.IRON_SWORD, "Sharp Knife", new String[] { "A super sharp knife.", "Hit someone to make him bleed." }),;

	public final Material material;
	private String name;
	public String[] lore;
	public String[] craft;

	private CustomItem(Material material, String name) {
		this(material, name, new String[0], null);
	}

	private CustomItem(Material material, String name, String[] lore) {
		this(material, name, lore, null);
	}

	private CustomItem(Material material, String name, String[] lore, String[] craft) {
		this.material = material;
		this.name = name;
		this.lore = lore;
		this.craft = craft;
	}

	public void update(ConfigurationSection config) {
		this.name = config.getString("name");
		this.lore = config.getStringList("lore").toArray(new String[0]);
		this.craft = config.getStringList("craft").toArray(new String[0]);
	}

	public String getDefaultName() {
		return name;
	}

	public String getName() {
		return "" + ChatColor.RESET + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', name);
	}

	public ItemStack a() {
		ItemStack i = new ItemStack(material);
		ItemMeta meta = i.getItemMeta();

		if (meta != null) {
			meta.setDisplayName(getName());
			meta.addItemFlags(ItemFlag.values());
			if (lore != null) {
				ArrayList<String> loreList = new ArrayList<>();
				for (String s : this.lore) {
					loreList.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s));
				}
				meta.setLore(loreList);
			}
			i.setItemMeta(meta);
		}
		return i;
	}

}