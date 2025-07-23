package org.nguyendevs.suddendeath.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.PlayerData;

import java.util.logging.Level;

/**
 * Event listener for adjusting damage based on player difficulty settings in the SuddenDeath plugin.
 */
public class DifficultiesListener implements Listener {

	/**
	 * Adjusts damage dealt to players based on their difficulty multiplier.
	 *
	 * @param event The EntityDamageEvent.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player) || event.getEntity().hasMetadata("NPC")) {
			return;
		}

		try {
			PlayerData data = PlayerData.get(player);
			if (data == null) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"PlayerData not found for player: " + player.getName());
				return;
			}

			double multiplier = data.getDifficulty().getDamageMultiplier();
			event.setDamage(event.getDamage() * multiplier);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling EntityDamageEvent for player: " + player.getName(), e);
		}
	}
}