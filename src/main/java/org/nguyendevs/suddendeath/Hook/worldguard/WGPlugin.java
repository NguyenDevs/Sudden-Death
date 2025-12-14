package org.nguyendevs.suddendeath.Hook.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface WGPlugin {

	boolean isPvpAllowed(Location location);

	boolean isFlagAllowed(Player player, CustomFlag customFlag);

	boolean isFlagAllowedAtLocation(Location location, CustomFlag customFlag);
}
