package org.nguyendevs.suddendeath.comp.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardOff implements WGPlugin {

	@Override
	public boolean isPvpAllowed(Location location) {
		if (location == null) {
			throw new IllegalArgumentException("Location cannot be null");
		}
		return true;
	}

	@Override
	public boolean isFlagAllowed(Player player, CustomFlag customFlag) {
		if (player == null) {
			throw new IllegalArgumentException("Player cannot be null");
		}
		if (customFlag == null) {
			throw new IllegalArgumentException("CustomFlag cannot be null");
		}
		return true;
	}
}