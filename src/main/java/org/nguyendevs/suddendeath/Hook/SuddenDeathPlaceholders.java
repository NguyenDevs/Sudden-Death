package org.nguyendevs.suddendeath.Hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.Utils;
import org.bukkit.entity.Player;

public class SuddenDeathPlaceholders extends PlaceholderExpansion {
	@Override
	public @NotNull String getAuthor() {
		return "NguyenDevs";
	}

	@Override
	public @NotNull String getIdentifier() {
		return "suddendeath";
	}

	@Override
	public @NotNull String getVersion() {
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
