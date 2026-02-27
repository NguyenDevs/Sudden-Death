package org.nguyendevs.suddendeath.Player;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.ConfigFile;

import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("deprecation")

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

	private PlayerData(Player player) {
		if (player == null) {
			throw new IllegalArgumentException("Player cannot be null");
		}
		this.offlinePlayer = player;
		this.player = player;
	}

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
		return isInfected;
	}

	public boolean isBleeding() {
		return isBleeding;
	}

	public void setBleeding(boolean value) {
		this.isBleeding = value;
	}

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

	public boolean isOnCooldown(Feature feature) {
		return cooldowns.getOrDefault(feature, 0L) > System.currentTimeMillis();
	}

	public void setBleedingTask(BukkitRunnable task) {
		this.bleedingTask = task;
	}

	public BukkitRunnable getBleedingTask() {
		return bleedingTask;
	}

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

	public static PlayerData get(OfflinePlayer player) {
		return player != null ? playerDataMap.get(player.getUniqueId()) : null;
	}

	public static Collection<PlayerData> getLoaded() {
		return Collections.unmodifiableCollection(playerDataMap.values());
	}

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