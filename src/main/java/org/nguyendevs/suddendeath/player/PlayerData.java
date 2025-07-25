package org.nguyendevs.suddendeath.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.ConfigFile;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages persistent and transient data for a player in the SuddenDeath plugin.
 */
public class PlayerData {
	private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
	private static final String MODIFIER_PREFIX = "suddenDeath.";
	private static final double COOLDOWN_DURATION_SECONDS = 3.0;
	private BukkitRunnable bleedingTask;
	private boolean isInfected;
	private boolean isBleeding;
	private final Map<Feature, Long> cooldowns = new HashMap<>();
	private final OfflinePlayer offlinePlayer;
	private Player player;

	/**
	 * Constructs a PlayerData instance for the specified player.
	 *
	 * @param player The player associated with this data.
	 * @throws IllegalArgumentException if player is null.
	 */
	private PlayerData(Player player) {
		if (player == null) {
			throw new IllegalArgumentException("Player cannot be null");
		}
		this.offlinePlayer = player;
		this.player = player;
	}

	/**
	 * Loads player data from a configuration file.
	 *
	 * @param config The configuration file containing player data.
	 * @return This PlayerData instance for method chaining.
	 */
	private PlayerData load(FileConfiguration config) {
		if (config == null) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Configuration is null for player: " + offlinePlayer.getUniqueId());
			return this;
		}

		try {
			isBleeding = config.getBoolean("bleeding", false);
			isInfected = config.getBoolean("infected", false);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error loading player data for: " + offlinePlayer.getUniqueId(), e);
		}
		return this;
	}

	/**
	 * Saves player data to a configuration file.
	 *
	 * @param config The configuration file to save to.
	 */
	public void save(FileConfiguration config) {
		if (config == null) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Configuration is null for saving player data: " + offlinePlayer.getUniqueId());
			return;
		}

		try {
			config.set("bleeding", isBleeding ? true : null);
			config.set("infected", isInfected ? true : null);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error saving player data for: " + offlinePlayer.getUniqueId(), e);
		}
	}

	/**
	 * Gets the associated online player.
	 *
	 * @return The Player instance, or null if offline.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Sets the online player instance.
	 *
	 * @param player The Player to set.
	 * @return This PlayerData instance for method chaining.
	 */
	public PlayerData setPlayer(Player player) {
		this.player = player;
		return this;
	}

	/**
	 * Gets the associated OfflinePlayer.
	 *
	 * @return The OfflinePlayer instance.
	 */
	public OfflinePlayer getOfflinePlayer() {
		return offlinePlayer;
	}

	/**
	 * Gets the player's UUID.
	 *
	 * @return The player's UUID.
	 */
	public UUID getUniqueId() {
		return offlinePlayer.getUniqueId();
	}

	/**
	 * Checks if the player is infected.
	 *
	 * @return True if infected.
	 */
	public boolean isInfected() {
		return isInfected;
	}

	/**
	 * Checks if the player is bleeding.
	 *
	 * @return True if bleeding.
	 */
	public boolean isBleeding() {
		return isBleeding;
	}

	/**
	 * Sets the player's bleeding status.
	 *
	 * @param value The bleeding status.
	 */
	public void setBleeding(boolean value) {
		this.isBleeding = value;
	}

	/**
	 * Sets the player's infection status and removes confusion effect if not infected.
	 *
	 * @param value The infection status.
	 */
	public void setInfected(boolean value) {
		this.isInfected = value;
		if (!value && player != null) {
			try {
				player.removePotionEffect(PotionEffectType.CONFUSION);
			} catch (Exception e) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Error removing confusion effect for player: " + player.getName(), e);
			}
		}
	}

	/**
	 * Checks if the player is on cooldown for a specific feature.
	 *
	 * @param feature The feature to check.
	 * @return True if the player is on cooldown.
	 */
	public boolean isOnCooldown(Feature feature) {
		return cooldowns.getOrDefault(feature, 0L) > System.currentTimeMillis();
	}

	public void setBleedingTask(BukkitRunnable task) {
		this.bleedingTask = task;
	}

	public BukkitRunnable getBleedingTask() {
		return bleedingTask;
	}
	/**
	 * Applies a cooldown for a specific feature.
	 *
	 * @param feature The feature to apply the cooldown to.
	 * @param seconds The cooldown duration in seconds.
	 */
	public void applyCooldown(Feature feature, double seconds) {
		if (feature == null || seconds < 0) {
			return;
		}
		try {
			cooldowns.put(feature, System.currentTimeMillis() + (long) (seconds * 1000));
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error applying cooldown for feature: " + feature, e);
		}
	}

	/**
	 * Removes attribute modifiers prefixed with the plugin's namespace.
	 *
	 * @param attributeInstance The AttributeInstance to clean.
	 */
	/**
	 * Removes specific attribute modifiers by name.
	 *
	 * @param attributeInstance The AttributeInstance to clean.
	 * @param modifierName The specific modifier name to remove.
	 */
	public void removeSpecificModifier(AttributeInstance attributeInstance, String modifierName) {
		if (attributeInstance == null || modifierName == null) {
			return;
		}
		try {
			attributeInstance.getModifiers().stream()
					.filter(modifier -> modifierName.equals(modifier.getName()))
					.collect(java.util.stream.Collectors.toList())
					.forEach(attributeInstance::removeModifier);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error removing specific modifier '" + modifierName + "' for player: " + offlinePlayer.getUniqueId(), e);
		}
	}

	/**
	 * Removes attribute modifiers prefixed with the plugin's namespace.
	 * USE WITH CAUTION - this removes ALL plugin modifiers.
	 *
	 * @param attributeInstance The AttributeInstance to clean.
	 */
	public void cleanAttributeModifiers(AttributeInstance attributeInstance) {
		if (attributeInstance == null) {
			return;
		}
		try {
			attributeInstance.getModifiers().stream()
					.filter(mod -> mod.getName().startsWith(MODIFIER_PREFIX))
					.collect(java.util.stream.Collectors.toList())
					.forEach(attributeInstance::removeModifier);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error cleaning attribute modifiers for player: " + offlinePlayer.getUniqueId(), e);
		}
	}

	/**
	 * Updates the player's movement speed based on equipped armor weight.
	 */
	public void updateMovementSpeed() {
		if (player == null || isOnCooldown(Feature.ARMOR_WEIGHT)) {
			return;
		}

		try {
			applyCooldown(Feature.ARMOR_WEIGHT, COOLDOWN_DURATION_SECONDS);
			double malus = Feature.ARMOR_WEIGHT.getDouble("movement-speed-malus") / 100.0;
			double totalMalus = 0.0;

			for (ItemStack item : new ItemStack[]{
					player.getInventory().getHelmet(),
					player.getInventory().getChestplate(),
					player.getInventory().getLeggings(),
					player.getInventory().getBoots()
			}) {
				if (item != null && isHeavyArmor(item.getType().name())) {
					totalMalus += malus;
				}
			}

			AttributeInstance attributeInstance = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
			if (attributeInstance == null) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Movement speed attribute not found for player: " + player.getName());
				return;
			}

			cleanAttributeModifiers(attributeInstance);
			if (totalMalus > 0) {
				attributeInstance.addModifier(new AttributeModifier(
						UUID.randomUUID(),
						MODIFIER_PREFIX + "armorSlow",
						-totalMalus,
						Operation.ADD_SCALAR
				));
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error updating movement speed for player: " + player.getName(), e);
		}
	}

	/**
	 * Checks if the material is considered heavy armor.
	 *
	 * @param materialName The name of the material.
	 * @return True if the material is heavy armor.
	 */
	private boolean isHeavyArmor(String materialName) {
		return materialName.contains("DIAMOND") || materialName.contains("IRON") ||
				materialName.contains("GOLD") || materialName.contains("NETHERITE");
	}

	/**
	 * Gets the PlayerData for an OfflinePlayer.
	 *
	 * @param player The OfflinePlayer to retrieve data for.
	 * @return The PlayerData, or null if not found.
	 */
	public static PlayerData get(OfflinePlayer player) {
		return player != null ? playerDataMap.get(player.getUniqueId()) : null;
	}

	/**
	 * Gets all loaded PlayerData instances.
	 *
	 * @return A collection of loaded PlayerData.
	 */
	public static Collection<PlayerData> getLoaded() {
		return Collections.unmodifiableCollection(playerDataMap.values());
	}

	/**
	 * Sets up PlayerData for a player, loading from config if not already present.
	 *
	 * @param player The Player to set up data for.
	 */
	public static void setup(Player player) {
		if (player == null) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Attempted to setup PlayerData for null player");
			return;
		}

		try {
			UUID uuid = player.getUniqueId();
			if (!playerDataMap.containsKey(uuid)) {
				ConfigFile config = new ConfigFile("/userdata", uuid.toString());
				config.setup();
				PlayerData data = new PlayerData(player).load(config.getConfig());
				playerDataMap.put(uuid, data);
			} else {
				playerDataMap.get(uuid).setPlayer(player);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error setting up PlayerData for player: " + player.getName(), e);
		}
	}
}