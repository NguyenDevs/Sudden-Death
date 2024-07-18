package org.nguyendevs.suddendeath.world;

import org.nguyendevs.suddendeath.Feature;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.manager.EventManager.WorldStatus;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Thunderstorm extends WorldEventHandler {
	public Thunderstorm(World world) {
		super(world, 4 * 20, WorldStatus.THUNDER_STORM);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.LIGHTNING && event.getEntity().getWorld().equals(getWorld()) && event.getEntity() instanceof Player) {
			event.setDamage(event.getDamage() * (1 + Feature.THUNDERSTORM.getDouble("damage-percent") / 100));
		}
	}

	@EventHandler
	public void onLightningStrike(LightningStrikeEvent event) {
		LightningStrike strike = event.getLightning();
		if (event.getWorld().equals(getWorld())) {
			strike.getWorld().spawnParticle(Particle.SMOKE_NORMAL, strike.getLocation(), 128, 0, 0, 0, .6);
			strike.getWorld().spawnParticle(Particle.SMOKE_NORMAL, strike.getLocation(), 16, 2, 1, 2, 0);
			for (Entity entity : strike.getNearbyEntities(6, 3, 6)) {
				entity.setVelocity(entity.getLocation().toVector().subtract(strike.getLocation().toVector()).normalize().multiply(3).setY(1));
			}
		}
	}

	@Override
	public void run() {
		getWorld().setStorm(true);
		getWorld().setWeatherDuration(200);

		for (Player player : getWorld().getPlayers()) {
			if (random.nextDouble() < .35)
				continue;

			Location loc = player.getLocation().clone().add(random.nextDouble() * 10 - 5, 0, random.nextDouble() * 10 - 5);
			Location randomLoc = getWorld().getHighestBlockAt(loc).getLocation();
			new BukkitRunnable() {
				double ti = 0;

				public void run() {
					ti += Math.PI / 16;
					Location loc1 = randomLoc.clone().add(.5 + Math.cos(ti), 1, .5 + Math.sin(ti)); // Thêm 1 vào trục Y để cách mặt đất 1 block
					loc1.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc1, 0, 0, 0, 0, 0);
					loc1.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc1, 0, 0, 0, 0, 0);
					loc1.getWorld().playSound(loc1, Sound.BLOCK_GLASS_BREAK, 2, 2);
					if (ti > Math.PI * 2) {
						getWorld().strikeLightning(randomLoc);
						cancel();
					}
				}
			}.runTaskTimer(SuddenDeath.plugin, 0, 1);
		}
	}
}
