package org.nguyendevs.suddendeath.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Enum representing custom items in the SuddenDeath plugin.
 */
public enum CustomItem {
	BANDAGE(
			Material.PAPER,
			"&fBandage",
			new String[]{"Stops Bleeding."},
			new String[]{"AIR,AIR,AIR", "PAPER,STICK,PAPER", "AIR,AIR,AIR"}
	),
	STRANGE_BREW(
			Material.SUSPICIOUS_STEW,
			"&fStrange Brew",
			new String[]{"Stops Infection."},
			new String[]{"AIR,AIR,AIR", "GLOW_INK_SAC,BOWL,TURTLE_SCUTE", "AIR,AIR,AIR"}
	),
	RAW_HUMAN_FLESH(
			Material.BEEF,
			"Human Flesh",
			new String[]{"Some fresh human meat.", "I wonder if I can cook it?"},
			null
	),
	HUMAN_BONE(
			Material.BONE,
			"Human Bone",
			new String[]{},
			null
	),
	COOKED_HUMAN_FLESH(
			Material.COOKED_BEEF,
			"Cooked Human Flesh",
			new String[]{"Looks tasty!"},
			null
	),
	SHARP_KNIFE(
			Material.IRON_SWORD,
			"Sharp Knife",
			new String[]{"A super sharp knife.", "Hit someone to make them bleed."},
			null
	);

	private static final NamespacedKey CUSTOM_ITEM_KEY = new NamespacedKey(SuddenDeath.getInstance(), "custom_item_id");

	private final Material material;
	private String name;
	private List<String> lore;
	private List<String> craft;

	/**
	 * Constructs a CustomItem with the specified properties.
	 *
	 * @param material The material of the item.
	 * @param name The default display name with color codes.
	 * @param lore The default lore of the item.
	 * @param craft The crafting recipe, or null if not craftable.
	 */
	CustomItem(Material material, String name, String[] lore, String[] craft) {
		if (material == null || name == null || lore == null) {
			throw new IllegalArgumentException("Material, name, and lore cannot be null");
		}
		this.material = material;
		this.name = name;
		this.lore = Collections.unmodifiableList(Arrays.stream(lore)
				.map(line -> line == null ? "" : line)
				.collect(Collectors.toList()));
		this.craft = craft == null ? null : Collections.unmodifiableList(Arrays.asList(craft));
	}

	/**
	 * Updates the item properties from a configuration section.
	 *
	 * @param config The configuration section containing item data.
	 */
	public void update(ConfigurationSection config) {
		if (config == null) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Configuration section is null for CustomItem: " + name());
			return;
		}
		try {
			String configName = config.getString("name");
			if (configName != null) {
				this.name = configName;
			}

			List<String> configLore = config.getStringList("lore");
			if (!configLore.isEmpty()) {
				this.lore = Collections.unmodifiableList(configLore);
			}

			List<String> configCraft = config.getStringList("craft");
			if (!configCraft.isEmpty()) {
				if (configCraft.size() == 3 && configCraft.stream().allMatch(line -> line.split(",").length == 3)) {
					this.craft = Collections.unmodifiableList(configCraft);
				} else {
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Invalid craft format for " + name() + ": Expected 3 lines with 3 materials each, got " + configCraft);
				}
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error updating CustomItem: " + name(), e);
		}
	}

	/**
	 * Gets the material of the item.
	 *
	 * @return The Material.
	 */
	public Material getMaterial() {
		return material;
	}

	/**
	 * Gets the default or configured name of the item.
	 *
	 * @return The name with color codes.
	 */
	public String getDefaultName() {
		return name;
	}

	/**
	 * Gets the formatted display name of the item.
	 *
	 * @return The translated name with color codes applied.
	 */
	public String getName() {
		try {
			return ChatColor.RESET + "" + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', name);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error translating name for CustomItem: " + name(), e);
			return name;
		}
	}

	/**
	 * Gets the lore of the item.
	 *
	 * @return An unmodifiable list of lore strings.
	 */
	public List<String> getLore() {
		return lore;
	}

	/**
	 * Gets the crafting recipe of the item, if applicable.
	 *
	 * @return An unmodifiable list of crafting recipe strings, or null if not craftable.
	 */
	public List<String> getCraft() {
		return craft;
	}

	/**
	 * Creates a new ItemStack for this custom item with applied metadata.
	 *
	 * @return The configured ItemStack.
	 */
	public ItemStack createItem() {
		try {
			ItemStack item = new ItemStack(material);
			ItemMeta meta = item.getItemMeta();
			if (meta == null) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"ItemMeta is null for CustomItem: " + name());
				return item;
			}

			meta.setDisplayName(getName());
			meta.addItemFlags(ItemFlag.values());
			if (!lore.isEmpty()) {
				List<String> formattedLore = lore.stream()
						.map(line -> ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', line))
						.collect(Collectors.toList());
				meta.setLore(formattedLore);
			}
			// Store the CustomItem enum name in PersistentDataContainer
			meta.getPersistentDataContainer().set(CUSTOM_ITEM_KEY, PersistentDataType.STRING, this.name());
			item.setItemMeta(meta);
			return item;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error creating ItemStack for CustomItem: " + name(), e);
			return new ItemStack(Material.AIR);
		}
	}

	/**
	 * Alias method to create the custom item. Used for compatibility.
	 *
	 * @return The ItemStack of this CustomItem.
	 */
	public ItemStack a() {
		return createItem();
	}

	/**
	 * Retrieves the CustomItem corresponding to the provided ItemStack.
	 *
	 * @param item The ItemStack to check.
	 * @return The matching CustomItem, or null if no match is found.
	 */
	public static CustomItem fromItemStack(ItemStack item) {
		if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
			return null;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return null;
		}
		String customItemId = meta.getPersistentDataContainer().get(CUSTOM_ITEM_KEY, PersistentDataType.STRING);
		if (customItemId != null) {
			try {
				return CustomItem.valueOf(customItemId);
			} catch (IllegalArgumentException e) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Invalid CustomItem ID in PersistentDataContainer: " + customItemId, e);
			}
		}
		return null;
	}
}