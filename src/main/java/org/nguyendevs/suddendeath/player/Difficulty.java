package org.nguyendevs.suddendeath.player;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Enum representing difficulty levels in the SuddenDeath plugin.
 */
public enum Difficulty {
	SANDBOX("Sandbox", Material.YELLOW_TERRACOTTA, 2, 0.0, 0.0),
	DIFFICULT("Difficult", Material.ORANGE_TERRACOTTA, 9, 30.0, 4.0),
	HARDCORE("Hardcore", Material.RED_TERRACOTTA, 14, 40.0, 6.0),
	DEATH_WISH("Death Wish", Material.BROWN_TERRACOTTA, 17, 50.0, 8.0),
	SUDDEN_DEATH("Sudden Death", Material.BLACK_TERRACOTTA, 20, 60.0, 10.0);
	private final Material material;

	private static final String MODIFIER_NAME = "suddenDeath.difficultyMalus";
	private final int difficultyIndex;
	private final ItemStack item;
	private String name;
	private List<String> lore;
	private double increasedDamage;

	private double healthMalus;

	/**
	 * Constructs a Difficulty with the specified properties.
	 *
	 * @param name            The default name of the difficulty.
	 * @param material        The material representing the difficulty in the GUI.
	 * @param difficultyIndex The cosmetic difficulty index.
	 * @param increasedDamage The percentage increase in damage.
	 * @param healthMalus     The health reduction amount.
	 * @throws IllegalArgumentException if name or material is null.
	 */
	Difficulty(String name, Material material, int difficultyIndex, double increasedDamage, double healthMalus) {
		if (name == null || material == null) {
			throw new IllegalArgumentException("Name and material cannot be null");
		}
		this.material = material;
		this.name = name;
		this.item = new ItemStack(material);
		this.difficultyIndex = difficultyIndex;
		this.increasedDamage = increasedDamage;
		this.healthMalus = healthMalus;
		this.lore = new ArrayList<>();
	}
	public Material getMaterial() {
		return material;
	}
	/**
	 * Creates a new copy of the representative item.
	 *
	 * @return A cloned ItemStack.
	 */
	public ItemStack getNewItem() {
		try {
			return item.clone();
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error cloning item for difficulty: " + name, e);
			return new ItemStack(Material.AIR);
		}
	}

	/**
	 * Gets the cosmetic difficulty index.
	 *
	 * @return The difficulty index.
	 */
	public int getDifficultyIndex() {
		return difficultyIndex;
	}

	/**
	 * Gets the display name of the difficulty.
	 *
	 * @return The difficulty name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the lore/description of the difficulty.
	 *
	 * @return An unmodifiable list of lore strings.
	 */
	public List<String> getLore() {
		return Collections.unmodifiableList(lore);
	}

	/**
	 * Gets the permission node for this difficulty.
	 *
	 * @return The permission string.
	 */
	public String getPermission() {
		return "suddendeath.difficulty." + Utils.lowerCaseId(name());
	}

	/**
	 * Gets the health reduction amount.
	 *
	 * @return The health malus value.
	 */
	public double getHealthMalus() {
		return healthMalus;
	}

	/**
	 * Gets the damage increase percentage.
	 *
	 * @return The increased damage percentage.
	 */
	public double getIncreasedDamage() {
		return increasedDamage;
	}

	/**
	 * Gets the damage multiplier based on the increased damage percentage.
	 *
	 * @return The damage multiplier (1 + increasedDamage / 100).
	 */
	public double getDamageMultiplier() {
		try {
			return 1.0 + increasedDamage / 100.0;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error calculating damage multiplier for difficulty: " + name, e);
			return 1.0;
		}
	}

	/**
	 * Applies the health malus to the player's max health attribute.
	 *
	 * @param data The PlayerData instance for the player.
	 */
	/**
	 * Applies the health malus to the player's max health attribute.
	 *
	 * @param data The PlayerData instance for the player.
	 */
	public void applyHealthMalus(PlayerData data) {
		if (data == null || data.getPlayer() == null) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Cannot apply health malus: PlayerData or Player is null");
			return;
		}

		try {
			AttributeInstance attributeInstance = data.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (attributeInstance == null) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Max health attribute not found for player: " + data.getPlayer().getName());
				return;
			}

			// Xóa tất cả modifier có tên MODIFIER_NAME
			List<AttributeModifier> toRemove = attributeInstance.getModifiers().stream()
					.filter(mod -> MODIFIER_NAME.equals(mod.getName()))
					.collect(java.util.stream.Collectors.toList());
			toRemove.forEach(attributeInstance::removeModifier);

			// Đặt lại base value
			attributeInstance.setBaseValue(20.0);

			// Áp dụng modifier mới nếu cần
			if (healthMalus > 0) {
				UUID uuid = UUID.nameUUIDFromBytes((MODIFIER_NAME + data.getPlayer().getUniqueId()).getBytes());
				AttributeModifier healthModifier = new AttributeModifier(
						uuid,
						MODIFIER_NAME,
						-healthMalus,
						Operation.ADD_NUMBER
				);
				attributeInstance.addModifier(healthModifier);
			}

			// Cập nhật máu hiện tại
			double newMaxHealth = attributeInstance.getValue();
			if (data.getPlayer().getHealth() > newMaxHealth) {
				data.getPlayer().setHealth(newMaxHealth);
			}

			SuddenDeath.getInstance().getLogger().info(String.format(
					"Applied difficulty %s to %s: MaxHealth=%.1f, CurrentHealth=%.1f, HealthMalus=%.1f",
					this.name(), data.getPlayer().getName(), newMaxHealth,
					data.getPlayer().getHealth(), healthMalus
			));

		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error applying health malus for player: " + data.getPlayer().getName(), e);
		}
	}


	/**
	 * Updates the difficulty properties from a configuration file.
	 *
	 * @param config The configuration file containing difficulty data.
	 */
	public void update(FileConfiguration config) {
		if (config == null) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Configuration is null for updating difficulty: " + name());
			return;
		}

		try {
			String configPath = name();
			String configName = config.getString(configPath + ".name");
			if (configName != null && !configName.trim().isEmpty()) {
				this.name = configName;
			}

			List<String> configLore = config.getStringList(configPath + ".lore");
			if (!configLore.isEmpty()) {
				this.lore = new ArrayList<>(configLore);
			}

			double configIncreasedDamage = config.getDouble(configPath + ".increased-damage", increasedDamage);
			if (configIncreasedDamage >= 0) {
				this.increasedDamage = configIncreasedDamage;
			} else {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Invalid increased-damage value for difficulty: " + name());
			}

			double configHealthMalus = config.getDouble(configPath + ".health-malus", healthMalus);
			if (configHealthMalus >= 0) {
				this.healthMalus = configHealthMalus;
			} else {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Invalid health-malus value for difficulty: " + name());
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error updating difficulty: " + name(), e);
		}
	}

	/**
	 * Gets the formatted main name of the difficulty (first letter capitalized).
	 *
	 * @return The formatted name.
	 */
	public String getMainName() {
		try {
			return Utils.caseOnWords(name().toLowerCase());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error formatting main name for difficulty: " + name(), e);
			return name();
		}
	}
}