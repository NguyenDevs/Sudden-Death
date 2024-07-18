package org.nguyendevs.suddendeath.comp.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface WGPlugin {
	public boolean isPvpAllowed(Location loc);

	public boolean isFlagAllowed(Player player, CustomFlag customFlag);
}
