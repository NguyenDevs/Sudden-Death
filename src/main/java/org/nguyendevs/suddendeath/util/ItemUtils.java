package org.nguyendevs.suddendeath.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Utility class for serializing and deserializing ItemStacks in the SuddenDeath plugin.
 */
public final class ItemUtils {
	private static final Pattern ARGUMENT_SPLITTER = Pattern.compile(",");
	private static final Pattern ENCHANTMENT_SPLITTER = Pattern.compile(";");
	private static final Pattern LEVEL_SPLITTER = Pattern.compile(":");

	private ItemUtils() {
		// Prevent instantiation
	}

	/**
	 * Serializes an ItemStack into a string representation.
	 *
	 * @param item The ItemStack to serialize.
	 * @return A string representation of the ItemStack, or "[material=AIR]" if null or AIR.
	 */
	public static String serialize(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return "[material=AIR]";
		}

		try {
			StringBuilder format = new StringBuilder("material=" + item.getType().name());

			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				// Damage
				if (meta instanceof Damageable damageable) {
					format.append(",damage=").append(damageable.getDamage());
				}

				// Color (for leather armor)
				if (meta instanceof LeatherArmorMeta leatherMeta) {
					format.append(",color=").append(leatherMeta.getColor().asRGB());
				}

				// Enchantments
				if (meta.hasEnchants()) {
					StringBuilder enchFormat = new StringBuilder();
					meta.getEnchants().forEach((enchantment, level) ->
							enchFormat.append(";")
									.append(enchantment.getKey().getKey())
									.append(":")
									.append(level));
					format.append(",enchants=").append(enchFormat.substring(1));
				}
			}

			return "[" + format + "]";
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error serializing ItemStack: " + item.getType(), e);
			return "[material=AIR]";
		}
	}

	/**
	 * Serializes a Material into a string representation.
	 *
	 * @param material The Material to serialize.
	 * @return A string representation of the Material.
	 */
	public static String serialize(Material material) {
		if (material == null) {
			return "[material=AIR]";
		}
		try {
			return serialize(new ItemStack(material));
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error serializing Material: " + material, e);
			return "[material=AIR]";
		}
	}

	/**
	 * Deserializes a string into an ItemStack.
	 *
	 * @param input The string to deserialize.
	 * @return The deserialized ItemStack, or an AIR ItemStack if invalid.
	 */
	public static ItemStack deserialize(String input) {
		if (input == null || input.length() < 3 || !input.startsWith("[") || !input.endsWith("]")) {
			return new ItemStack(Material.AIR);
		}

		try {
			String cleanInput = input.substring(1, input.length() - 1);
			ItemStack item = new ItemStack(Material.AIR);
			ItemMeta meta = item.getItemMeta();

			for (String arg : ARGUMENT_SPLITTER.split(cleanInput)) {
				// Material
				if (arg.startsWith("material=")) {
					String materialName = arg.replace("material=", "");
					try {
						Material material = Material.valueOf(materialName);
						item = new ItemStack(material);
						meta = item.getItemMeta();
					} catch (IllegalArgumentException e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Invalid material name: " + materialName);
						return new ItemStack(Material.AIR);
					}
				}

				// Color for leather armor
				if (arg.startsWith("color=") && meta instanceof LeatherArmorMeta leatherMeta) {
					String colorValue = arg.replace("color=", "");
					try {
						leatherMeta.setColor(Color.fromRGB(Integer.parseInt(colorValue)));
					} catch (NumberFormatException e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Invalid color value: " + colorValue);
					}
				}

				// Damage for items
				if (arg.startsWith("damage=") && meta instanceof Damageable damageable) {
					String damageValue = arg.replace("damage=", "");
					try {
						damageable.setDamage(Integer.parseInt(damageValue));
					} catch (NumberFormatException e) {
						SuddenDeath.getInstance().getLogger().log(Level.WARNING,
								"Invalid damage value: " + damageValue);
					}
				}

				// Enchantments
				if (arg.startsWith("enchants=")) {
					String enchants = arg.replace("enchants=", "");
					for (String ench : ENCHANTMENT_SPLITTER.split(enchants)) {
						String[] split = LEVEL_SPLITTER.split(ench);
						if (split.length == 2) {
							Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(split[0]));
							int level;
							try {
								level = Integer.parseInt(split[1]);
							} catch (NumberFormatException e) {
								SuddenDeath.getInstance().getLogger().log(Level.WARNING,
										"Invalid enchantment level: " + split[1]);
								continue;
							}
							if (enchantment != null && level > 0 && meta != null) {
								meta.addEnchant(enchantment, level, true);
							}
						}
					}
				}
			}

			if (meta != null) {
				item.setItemMeta(meta);
			}
			return item;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error deserializing ItemStack: " + input, e);
			return new ItemStack(Material.AIR);
		}
	}
}