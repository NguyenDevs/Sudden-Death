package org.nguyendevs.suddendeath.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.regex.Pattern;

public class ItemUtils {
	public static String serialize(ItemStack i) {
		String format = "material=" + i.getType().name();
		if (i.hasItemMeta()) {
			if (i.getItemMeta() instanceof Damageable)
				format += ",damage=" + ((Damageable) i.getItemMeta()).getDamage();

			if (i.getItemMeta() instanceof LeatherArmorMeta)
				format += ",color=" + ((LeatherArmorMeta) i.getItemMeta()).getColor().asRGB();

			if (i.getItemMeta().hasEnchants()) {
				String enchFormat = "";
				for (Enchantment ench : i.getItemMeta().getEnchants().keySet())
					enchFormat += ";" + ench.getKey().getKey() + ":" + i.getItemMeta().getEnchantLevel(ench);

				enchFormat = enchFormat.substring(1);
				format += ",enchants=" + enchFormat;
			}
		}
		return "[" + format + "]";
	}

	public static String serialize(Material m) {
		return serialize(new ItemStack(m));
	}

	public static ItemStack deserialize(String s) {
		ItemStack i = new ItemStack(Material.AIR);
		if (s == null || s.length() < 3)
			return i;

		ItemMeta meta = i.getItemMeta();
		s = s.substring(1, s.length() - 1);

		for (String arg : s.split(Pattern.quote(","))) {
			if (arg.startsWith("material=")) {
				arg = arg.replaceFirst("material=", "");
				Material material = Material.AIR;
				try {
					material = Material.valueOf(arg);
				} catch (Exception e) {
				}
				i = new ItemStack(material);
				meta = i.getItemMeta();
			}

			if (arg.startsWith("color=")) {
				arg = arg.replaceFirst("color=", "");
				if (meta instanceof LeatherArmorMeta)
					((LeatherArmorMeta) meta).setColor(Color.fromRGB(Integer.parseInt(arg)));
			}

			if (arg.startsWith("damage=")) {
				arg = arg.replaceFirst("damage=", "");
				if (meta instanceof Damageable)
					((Damageable) meta).setDamage(Integer.parseInt(arg));
			}

			if (arg.startsWith("enchants=")) {
				arg = arg.replaceFirst("enchants=", "");
				for (String ench : arg.split(Pattern.quote(";"))) {
					String[] split = ench.split(Pattern.quote(":"));
					Enchantment name = Enchantment.getByKey(NamespacedKey.minecraft(split[0]));
					int level = Integer.parseInt(split[1]);
					if (name != null && level != 0)
						meta.addEnchant(name, level, true);
				}
			}
		}

		i.setItemMeta(meta);
		return i;
	}
}
