package org.nguyendevs.suddendeath.player;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

public class PlayerData {

	/*
	 * data which gets saved when the server turns off
	 */
	private boolean infected, bleeding;
	private Difficulty difficulty;

	/*
	 * data that gets reset when the server switches off
	 */
	private final Map<Feature, Long> cooldowns = new HashMap<>();

	/*
	 * player data
	 */
	private final OfflinePlayer offlinePlayer;
	private Player player;

	private static Map<UUID, PlayerData> map = new HashMap<>();

	private PlayerData(Player player) {
		offlinePlayer = player;
		this.player = player;
	}

	private PlayerData load(FileConfiguration config) {
		bleeding = config.getBoolean("bleeding");
		infected = config.getBoolean("infected");

		if (config.contains("difficulty"))
			try {
				difficulty = Difficulty.valueOf(config.getString("difficulty").toUpperCase());
			} catch (IllegalArgumentException e) {
				SuddenDeath.plugin.getLogger().log(Level.WARNING, "Couldn't read difficulty (userdata) from " + config.getString("difficulty").toUpperCase());
			}

		return this;
	}

	public void save(FileConfiguration config) {
		config.set("bleeding", bleeding ? true : null);
		config.set("infected", infected ? true : null);
		config.set("difficulty", difficulty != null ? difficulty.name() : null);
	}

	public Player getPlayer() {
		return player;
	}

	public PlayerData setPlayer(Player player) {
		this.player = player;
		return this;
	}

	public OfflinePlayer getOfflinePlayer() {
		return offlinePlayer;
	}

	public UUID getUniqueId() {
		return offlinePlayer.getUniqueId();
	}

	public boolean isInfected() {
		return infected;
	}

	public boolean isBleeding() {
		return bleeding;
	}

	public void setBleeding(boolean value) {
		bleeding = value;
	}

	public void setInfected(boolean value) {
		infected = value;
		if (player != null)
			player.removePotionEffect(PotionEffectType.CONFUSION);
	}

	public Difficulty getDifficulty() {
		return hasDifficulty() ? difficulty : SuddenDeath.plugin.defaultDifficulty;
	}

	public boolean hasDifficulty() {
		return difficulty != null;
	}

	public boolean hasDifficulty(Difficulty difficulty) {
		return difficulty.equals(getDifficulty());
	}

	public void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	public boolean isOnCooldown(Feature feature) {
		return cooldowns.containsKey(feature) && cooldowns.get(feature) > System.currentTimeMillis();
	}

	public void applyCooldown(Feature type, double value) {
		cooldowns.put(type, (long) (System.currentTimeMillis() + value * 1000));
	}

	public void cleanAttributeModifiers(AttributeInstance ins) {
		for (Iterator<AttributeModifier> iterator = ins.getModifiers().iterator(); iterator.hasNext();) {
			AttributeModifier mod = iterator.next();
			if (mod.getName().startsWith("suddenDeath."))
				ins.removeModifier(mod);
		}
	}

	public void updateMovementSpeed() {
		if (isOnCooldown(Feature.ARMOR_WEIGHT))
			return;

		applyCooldown(Feature.ARMOR_WEIGHT, 3);
		double malus = Feature.ARMOR_WEIGHT.getDouble("movement-speed-malus") / 100, t = 0;

		for (ItemStack item : new ItemStack[] { player.getInventory().getHelmet(), player.getInventory().getChestplate(), player.getInventory().getLeggings(), player.getInventory().getBoots() })
			if (item != null)
				if (item.getType().name().contains("DIAMOND") || item.getType().name().contains("IRON") || item.getType().name().contains("GOLD")|| item.getType().name().contains("NETHERITE"))
					t += malus;

		AttributeInstance ins = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		cleanAttributeModifiers(ins);
		if (t > 0)
			ins.addModifier(new AttributeModifier(UUID.randomUUID(), "suddenDeath.armorSlow", -t, Operation.ADD_SCALAR));
	}

	public static PlayerData get(OfflinePlayer player) {
		return map.get(player.getUniqueId());
	}

	public static Collection<PlayerData> getLoaded() {
		return map.values();
	}

	public static PlayerData setup(Player player) {
		if (!map.containsKey(player.getUniqueId())) {
			PlayerData data = new PlayerData(player).load(new ConfigFile("/userdata", player.getUniqueId().toString()).getConfig());
			map.put(player.getUniqueId(), data);
			return data;
		}
		return get(player).setPlayer(player);
	}
}
