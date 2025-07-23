package org.nguyendevs.suddendeath.world;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;

import java.util.logging.Level;

/**
 * Handles the Thunderstorm world event, increasing lightning damage and frequency.
 */
public class Thunderstorm extends WorldEventHandler {
	private static final double LIGHTNING_PROBABILITY = 0.35;
	private static final int WEATHER_DURATION_TICKS = 200;
	private static final double PARTICLE_OFFSET = 0.5;
	private static final double KNOCKBACK_MULTIPLIER = 3.0;
	private static final double KNOCKBACK_Y = 1.0;
	private static final int PARTICLE_COUNT = 128;
	private static final int SECONDARY_PARTICLE_COUNT = 16;

	/**
	 * Constructs a Thunderstorm event handler for the specified world.
	 *
	 * @param world The world where the thunderstorm event occurs.
	 */
	public Thunderstorm(World world) {
		super(world, 4 * 20, WorldStatus.THUNDER_STORM);
	}

	/**
	 * Modifies damage dealt by lightning to players during the thunderstorm.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING ||
				!event.getEntity().getWorld().equals(getWorld()) ||
				!(event.getEntity() instanceof Player)) {
			return;
		}

		try {
			double damageMultiplier = 1 + (Feature.THUNDERSTORM.getDouble("damage-percent") / 100.0);
			event.setDamage(event.getDamage() * damageMultiplier);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error applying damage multiplier for thunderstorm in world: " + getWorld().getName(), e);
		}
	}

	/**
	 * Adds visual and knockback effects when lightning strikes.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onLightningStrike(LightningStrikeEvent event) {
		if (!event.getWorld().equals(getWorld())) {
			return;
		}

		try {
			LightningStrike strike = event.getLightning();
			Location strikeLocation = strike.getLocation();
			World world = strike.getWorld();

			// Spawn particles at the strike location
			world.spawnParticle(Particle.SMOKE_NORMAL, strikeLocation, PARTICLE_COUNT, 0, 0, 0, 0.6);
			world.spawnParticle(Particle.SMOKE_NORMAL, strikeLocation, SECONDARY_PARTICLE_COUNT, 2, 1, 2, 0);

			// Apply knockback to nearby entities
			for (Entity entity : strike.getNearbyEntities(6, 3, 6)) {
				entity.setVelocity(entity.getLocation().toVector()
						.subtract(strikeLocation.toVector())
						.normalize()
						.multiply(KNOCKBACK_MULTIPLIER)
						.setY(KNOCKBACK_Y));
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error handling lightning strike in world: " + getWorld().getName(), e);
		}
	}

	/**
	 * Runs periodically to maintain the thunderstorm and trigger lightning strikes.
	 */
	@Override
	public void run() {
		try {
			// Maintain storm state
			getWorld().setStorm(true);
			getWorld().setWeatherDuration(WEATHER_DURATION_TICKS);

			// Trigger lightning strikes near players
			for (Player player : getWorld().getPlayers()) {
				if (getRandom().nextDouble() < LIGHTNING_PROBABILITY) {
					continue;
				}

				Location playerLoc = player.getLocation();
				double offsetX = getRandom().nextDouble() * 10 - 5;
				double offsetZ = getRandom().nextDouble() * 10 - 5;
				Location strikeLoc = getWorld().getHighestBlockAt(
						playerLoc.clone().add(offsetX, 0, offsetZ)).getLocation();

				new LightningEffectTask(strikeLoc).runTaskTimer(SuddenDeath.getInstance(), 0, 1);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Error running thunderstorm task in world: " + getWorld().getName(), e);
		}
	}

	/**
	 * Inner class to handle the visual and sound effects before a lightning strike.
	 */
	private static class LightningEffectTask extends BukkitRunnable {
		private final Location strikeLocation;
		private double angle = 0;

		LightningEffectTask(Location strikeLocation) {
			this.strikeLocation = strikeLocation.clone();
		}

		@Override
		public void run() {
			try {
				angle += Math.PI / 16;
				Location effectLoc = strikeLocation.clone()
						.add(PARTICLE_OFFSET + Math.cos(angle), 1, PARTICLE_OFFSET + Math.sin(angle));

				World world = effectLoc.getWorld();
				if (world == null) {
					cancel();
					return;
				}

				world.spawnParticle(Particle.SMOKE_NORMAL, effectLoc, 0, 0, 0, 0, 0);
				world.spawnParticle(Particle.FIREWORKS_SPARK, effectLoc, 0, 0, 0, 0, 0);
				world.playSound(effectLoc, Sound.BLOCK_GLASS_BREAK, 2, 2);

				if (angle > Math.PI * 2) {
					world.strikeLightning(strikeLocation);
					cancel();
				}
			} catch (Exception e) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Error in lightning effect task at location: " + strikeLocation, e);
				cancel();
			}
		}
	}
}