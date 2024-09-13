package org.nguyendevs.suddendeath.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Objects;
import java.util.regex.Pattern;

public class ItemUtils {

	public static String serialize(ItemStack i) {
		if (i == null || i.getType() == Material.AIR) {
			return "[material=AIR]";
		}

		StringBuilder format = new StringBuilder("material=" + i.getType().name());

		if (i.hasItemMeta() && i.getItemMeta() != null) {
			ItemMeta meta = i.getItemMeta();

			// Damage
			if (meta instanceof Damageable) {
				format.append(",damage=").append(((Damageable) meta).getDamage());
			}

			// Color (for leather armor)
			if (meta instanceof LeatherArmorMeta) {
				format.append(",color=").append(((LeatherArmorMeta) meta).getColor().asRGB());
			}

			// Enchantments
			if (meta.hasEnchants()) {
				StringBuilder enchFormat = new StringBuilder();
				meta.getEnchants().forEach((enchantment, level) ->
						enchFormat.append(";")
								.append(enchantment.getKey().getKey())
								.append(":")
								.append(level)
				);

				if (!enchFormat.isEmpty()) {
					format.append(",enchants=").append(enchFormat.substring(1)); // Remove leading semicolon
				}
			}
		}

		return "[" + format + "]";
	}

	public static String serialize(Material m) {
		return serialize(new ItemStack(m));
	}

	public static ItemStack deserialize(String s) {
		if (s == null || s.length() < 3) {
			return new ItemStack(Material.AIR);
		}

		// Strip square brackets
		s = s.substring(1, s.length() - 1);

		ItemStack i = new ItemStack(Material.AIR);
		ItemMeta meta = i.getItemMeta();

		for (String arg : s.split(Pattern.quote(","))) {
			// Material
			if (arg.startsWith("material=")) {
				arg = arg.replaceFirst("material=", "");
				Material material;
				try {
					material = Material.valueOf(arg);
				} catch (IllegalArgumentException e) {
					material = Material.AIR; // Fallback to AIR if material is not found
				}
				i = new ItemStack(material);
				meta = i.getItemMeta();
			}

			// Color for leather armor
			if (arg.startsWith("color=")) {
				arg = arg.replaceFirst("color=", "");
				if (meta instanceof LeatherArmorMeta) {
					((LeatherArmorMeta) meta).setColor(Color.fromRGB(Integer.parseInt(arg)));
				}
			}

			// Damage for items
			if (arg.startsWith("damage=")) {
				arg = arg.replaceFirst("damage=", "");
				if (meta instanceof Damageable) {
					((Damageable) meta).setDamage(Integer.parseInt(arg));
				}
			}

			// Enchantments
			if (arg.startsWith("enchants=")) {
				arg = arg.replaceFirst("enchants=", "");
				for (String ench : arg.split(Pattern.quote(";"))) {
					String[] split = ench.split(Pattern.quote(":"));
					if (split.length == 2) {
						Enchantment name = Enchantment.getByKey(NamespacedKey.minecraft(split[0]));
						int level;
						try {
							level = Integer.parseInt(split[1]);
						} catch (NumberFormatException e) {
							level = 0; // Default level to 0 if parsing fails
						}
						if (name != null && level > 0 && meta != null) {
							meta.addEnchant(name, level, true);
						}
					}
				}
			}
		}

		if (meta != null) {
			i.setItemMeta(meta);
		}

		return i;
	}
}
