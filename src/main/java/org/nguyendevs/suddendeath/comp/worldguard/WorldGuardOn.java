package org.nguyendevs.suddendeath.comp.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;

public class WorldGuardOn implements WGPlugin {
	private final WorldGuard worldGuard;
	private final WorldGuardPlugin worldGuardPlugin;
	private final Map<String, StateFlag> customFlags;
	private final Set<String> failedFlags; // Track flags that failed to register
	private volatile boolean flagsRegistered = false;

	public WorldGuardOn() {
		this.customFlags = new HashMap<>();
		this.failedFlags = new HashSet<>();
		this.worldGuard = WorldGuard.getInstance();

		WorldGuardPlugin plugin = (WorldGuardPlugin) SuddenDeath.getInstance().getServer()
				.getPluginManager().getPlugin("WorldGuard");
		if (plugin == null) {
			throw new IllegalStateException("WorldGuard plugin not found. Disabling WorldGuard integration.");
		}
		this.worldGuardPlugin = plugin;
		SuddenDeath.getInstance().getServer().getScheduler().runTaskLater(
				SuddenDeath.getInstance(), this::registerCustomFlags, 1L);
	}

	private void registerCustomFlags() {
		FlagRegistry registry = worldGuard.getFlagRegistry();
		int successCount = 0;
		for (CustomFlag customFlag : CustomFlag.values()) {
			String flagPath = customFlag.getPath();
			try {
				if (registry.get(flagPath) != null) {
					StateFlag existingFlag = (StateFlag) registry.get(flagPath);
					customFlags.put(flagPath, existingFlag);
					successCount++;
				} else {
					failedFlags.add(flagPath);
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Custom flag not found in registry: " + flagPath);
				}
			} catch (Exception e) {
				failedFlags.add(flagPath);
				SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
						"Error loading WorldGuard flag: " + flagPath, e);
			}
		}

		flagsRegistered = true;
	}

	@Override
	public boolean isPvpAllowed(Location location) {
		if (location == null) {
			throw new IllegalArgumentException("Location cannot be null");
		}
		try {
			ApplicableRegionSet regions = getApplicableRegion(location);
			return regions.queryState(null, Flags.PVP) != StateFlag.State.DENY;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking PvP state at location: " + location, e);
			return true;
		}
	}

	@Override
	public boolean isFlagAllowed(Player player, CustomFlag customFlag) {
		if (player == null) {
			throw new IllegalArgumentException("Player cannot be null");
		}
		if (customFlag == null) {
			throw new IllegalArgumentException("CustomFlag cannot be null");
		}
		if (!flagsRegistered) {
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}

		String flagPath = customFlag.getPath();
		if (failedFlags.contains(flagPath)) {
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}
		try {
			ApplicableRegionSet regions = getApplicableRegion(player.getLocation());
			StateFlag flag = customFlags.get(flagPath);

			if (flag == null) {
				if (!failedFlags.contains(flagPath)) {
					failedFlags.add(flagPath);
				}
				return customFlag == CustomFlag.SDS_REMOVE ? false : true;
			}
			StateFlag.State state = regions.queryValue(worldGuardPlugin.wrapPlayer(player), flag);
			return state == null ? (customFlag == CustomFlag.SDS_REMOVE ? false : true) : state == StateFlag.State.ALLOW;

		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking flag " + flagPath + " for player: " + player.getName(), e);
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}
	}

	@Override
	public boolean isFlagAllowedAtLocation(Location location, CustomFlag customFlag) {
		if (location == null) {
			throw new IllegalArgumentException("Location cannot be null");
		}
		if (customFlag == null) {
			throw new IllegalArgumentException("CustomFlag cannot be null");
		}
		if (!flagsRegistered) {
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}
		String flagPath = customFlag.getPath();
		if (failedFlags.contains(flagPath)) {
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}
		try {
			ApplicableRegionSet regions = getApplicableRegion(location);
			StateFlag flag = customFlags.get(flagPath);
			if (flag == null) {
				if (!failedFlags.contains(flagPath)) {
					failedFlags.add(flagPath);
					SuddenDeath.getInstance().getLogger().log(Level.WARNING,
							"Custom flag not found after registration: " + flagPath + " - Using default behavior");
				}
				return customFlag == CustomFlag.SDS_REMOVE ? false : true;
			}
			StateFlag.State state = regions.queryState(null, flag);
			return state == null ? (customFlag == CustomFlag.SDS_REMOVE ? false : true) : state == StateFlag.State.ALLOW;
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error checking flag " + flagPath + " at location: " + location, e);
			return customFlag == CustomFlag.SDS_REMOVE ? false : true;
		}
	}

	private ApplicableRegionSet getApplicableRegion(Location location) {
		try {
			return worldGuard.getPlatform().getRegionContainer()
					.createQuery()
					.getApplicableRegions(BukkitAdapter.adapt(location));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to query WorldGuard regions at location: " + location, e);
		}
	}

	public boolean isReady() {
		return flagsRegistered;
	}

	public Map<String, StateFlag> getRegisteredFlags() {
		return new HashMap<>(customFlags);
	}
}