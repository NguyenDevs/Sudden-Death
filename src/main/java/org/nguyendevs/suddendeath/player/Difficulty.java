package org.nguyendevs.suddendeath.player;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public enum Difficulty {
	SANDBOX("Sandbox", Material.YELLOW_TERRACOTTA, 2, 0, 0),
	DIFFICULT("Difficult", Material.ORANGE_TERRACOTTA, 9, 30, 4),
	HARDCORE("Hardcore", Material.RED_TERRACOTTA, 14, 40, 6),
	DEATH_WISH("Death Wish", Material.BROWN_TERRACOTTA, 17, 50, 8),
	SUDDEN_DEATH("Sudden Death", Material.BLACK_TERRACOTTA, 20, 60, 10);

	// this difficulty index is 100% cosmetic
	private int difficulty;

	// item in the gui
	private ItemStack item;

	private String name;
	private List<String> lore;
	private double increasedDamage, healthMalus;

	private Difficulty(String name, Material material, int difficulty, double increasedDamage, double healthMalus) {
		this.name = name;
		this.item = new ItemStack(material);
		this.difficulty = difficulty;

		this.increasedDamage = increasedDamage;
		this.healthMalus = healthMalus;
	}

	public ItemStack getNewItem() {
		return item.clone();
	}

	public int getDifficultyIndex() {
		return difficulty;
	}

	public String getName() {
		return name;
	}

	public List<String> getLore() {
		return lore == null ? new ArrayList<>() : lore;
	}

	public String getPermission() {
		return "suddendeath.difficulty." + name().toLowerCase().replace("_", "-");
	}

	public double getHealthMalus() {
		return healthMalus;
	}

	public double getIncreasedDamage() {
		return increasedDamage;
	}

	public double getDamageMultiplier() {
		return 1 + increasedDamage / 100;
	}

	public void applyHealthMalus(PlayerData data) {
		AttributeInstance ins = data.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
		data.cleanAttributeModifiers(ins);
		ins.addModifier(new AttributeModifier("suddenDeath.difficultyMalus", -healthMalus, Operation.ADD_NUMBER));
	}

	public void update(FileConfiguration config) {
		name = config.getString(name() + ".name");
		lore = config.getStringList(name() + ".lore");
		increasedDamage = config.getDouble(name() + ".increased-damage");
		healthMalus = config.getDouble(name() + ".health-malus");
	}

	public String getMainName() {
		return name().substring(0, 1) + name().toLowerCase().substring(1);
	}
}
