package org.nguyendevs.suddendeath.comp.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface WGPlugin {

	boolean isPvpAllowed(Location location);

	boolean isFlagAllowed(Player player, CustomFlag customFlag);
}
