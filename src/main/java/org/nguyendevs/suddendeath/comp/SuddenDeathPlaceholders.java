package org.nguyendevs.suddendeath.comp;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.nguyendevs.suddendeath.player.PlayerData;
import org.nguyendevs.suddendeath.util.Utils;
import org.bukkit.entity.Player;

public class SuddenDeathPlaceholders extends PlaceholderExpansion {
	@Override
	public String getAuthor() {
		return "NguyenDevs";
	}

	@Override
	public String getIdentifier() {
		return "suddendeath";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	public String onPlaceholderRequest(Player player, String identifier) {
		if (identifier.equalsIgnoreCase("bleeding"))
			return Utils.msg(PlayerData.get(player).isBleeding() ? "papi-bleeding" : "papi-not-bleeding");
		if (identifier.equalsIgnoreCase("infection"))
			return Utils.msg(PlayerData.get(player).isInfected() ? "papi-infected" : "papi-not-infected");
		return null;
	}
}
