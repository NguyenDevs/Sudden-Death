package org.nguyendevs.suddendeath.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class SpigotPlugin {
	private final JavaPlugin plugin;
	private final int id;
	private String latestVersion;

	public SpigotPlugin(int id, JavaPlugin plugin) {
		this.plugin = plugin;
		this.id = id;
	}

	/**
	 * The request is executed asynchronously as not to block the main thread.
	 */
	public void checkForUpdate() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + id)
						.openConnection();
				connection.setRequestMethod("GET");
				latestVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
			} catch (IOException e) {
				plugin.getLogger().log(Level.WARNING, "Couldn't check the latest plugin version: " + e.getMessage());
				return;
			}

			String currentVersion = plugin.getDescription().getVersion();
			if (latestVersion == null || latestVersion.isEmpty()) {
				plugin.getLogger().log(Level.WARNING, "Received invalid version from SpigotMC API.");
				return;
			}

			if (isVersionNewer(currentVersion, latestVersion)) {
				plugin.getLogger().log(Level.INFO,
						"A new build is available: " + latestVersion + " (you are running " + currentVersion + ")");
				plugin.getLogger().log(Level.INFO, "Download it here: " + getResourceUrl());

				/*
				 * Registers the event to notify op players when they join only if
				 * the corresponding option is enabled
				 */
				if (plugin.getConfig().getBoolean("update-notify")) {
					Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().registerEvents(new Listener() {
						@EventHandler(priority = EventPriority.MONITOR)
						public void onPlayerJoin(PlayerJoinEvent event) {
							Player player = event.getPlayer();
							if (player.hasPermission(plugin.getName().toLowerCase() + ".update-notify")) {
								getOutOfDateMessage().forEach(msg -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg)));
							}
						}
					}, plugin));
				}
			} else {
				plugin.getLogger().log(Level.INFO, "You are running the latest version: " + currentVersion);
			}
		});
	}

	/**
	 * Compare two version strings to determine if the latest version is newer.
	 * Handles numeric version formats like "2.0.6" and "2.0.8".
	 */
	private boolean isVersionNewer(String currentVersion, String latestVersion) {
		if (currentVersion.equals(latestVersion)) {
			return false;
		}

		try {
			// Split version strings into parts
			String[] currentParts = currentVersion.split("\\.");
			String[] latestParts = latestVersion.split("\\.");

			int length = Math.max(currentParts.length, latestParts.length);
			for (int i = 0; i < length; i++) {
				int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
				int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

				if (latestPart > currentPart) {
					return true;
				} else if (latestPart < currentPart) {
					return false;
				}
			}
		} catch (NumberFormatException e) {
			plugin.getLogger().log(Level.WARNING, "Invalid version format detected: Current=" + currentVersion + ", Latest=" + latestVersion);
			return false; // If versions can't be parsed, assume no update
		}

		return false; // If versions are equal or current is newer
	}

	private List<String> getOutOfDateMessage() {
		return Arrays.asList(
				"&8--------------------------------------------",
				"&a" + plugin.getName() + " " + latestVersion + " is available!",
				"&a" + getResourceUrl(),
				"&7&oYou can disable this notification in the config file.",
				"&8--------------------------------------------"
		);
	}

	public String getResourceUrl() {
		return "https://www.spigotmc.org/resources/" + id + "/";
	}
}