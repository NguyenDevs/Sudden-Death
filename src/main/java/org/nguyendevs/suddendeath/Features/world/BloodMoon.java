package org.nguyendevs.suddendeath.Features.world;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.Hook.CustomFlag;
import org.nguyendevs.suddendeath.Managers.EventManager.WorldStatus;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class BloodMoon extends WorldEventHandler {

	private static final Random RANDOM = new Random();

	// Potion effect duration in ticks (large value = "permanent" for the night)
	private static final int BUFF_DURATION_TICKS = 1_200; // 60 seconds, refreshed every run() cycle

	private enum ToolTier {
		NETHERITE("NETHERITE", 5),
		DIAMOND("DIAMOND", 10),
		GOLDEN("GOLDEN", 25),
		IRON("IRON", 40),
		WOODEN("WOODEN", 20);

		final String prefix;
		final int weight;

		ToolTier(String prefix, int weight) {
			this.prefix = prefix;
			this.weight = weight;
		}
	}

	private enum ToolType {
		AXE("_AXE"), PICKAXE("_PICKAXE"), SHOVEL("_SHOVEL");

		final String suffix;

		ToolType(String suffix) {
			this.suffix = suffix;
		}
	}

	public BloodMoon(World world) {
		super(world, 40, WorldStatus.BLOOD_MOON); // run() every 40 ticks = 2 seconds
	}

	// -------------------------------------------------------------------------
	// Repeating Task: apply player debuffs + stun every 2 seconds
	// -------------------------------------------------------------------------
	@Override
	public void run() {
		try {
			for (Player player : getWorld().getPlayers()) {
				if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
					continue;
				}
				if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EVENT)) {
					continue;
				}

				int weaknessAmplifier = (int) Feature.BLOOD_MOON.getDouble("weakness-amplifier") - 1;
				int stunDuration = (int) Feature.BLOOD_MOON.getDouble("stun-duration");

				// Weakness (continuous, refreshed every cycle)
				player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
						BUFF_DURATION_TICKS, weaknessAmplifier, false, false, true));

				// Stun: Nausea for 1 second (20 ticks)
				player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,
						stunDuration, 0, false, false, false));
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Error running Blood Moon debuff task in world: " + getWorld().getName(), e);
		}
	}

	// -------------------------------------------------------------------------
	// On creature spawn: buff all monsters (no spawn replacement)
	// -------------------------------------------------------------------------
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		LivingEntity entity = event.getEntity();
		if (!(entity instanceof Monster))
			return;
		if (!entity.getWorld().equals(getWorld()))
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard()
					.isFlagAllowedAtLocation(entity.getLocation(), CustomFlag.SDS_EVENT)) {
				return;
			}

			int speed = (int) Feature.BLOOD_MOON.getDouble("speed") - 1;
			int strength = (int) Feature.BLOOD_MOON.getDouble("increase-damage") - 1;
			int resistance = (int) Feature.BLOOD_MOON.getDouble("damage-resistance") - 1;

			entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
					Integer.MAX_VALUE, speed, false, false, false));
			entity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,
					Integer.MAX_VALUE, strength, false, false, false));
			entity.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
					Integer.MAX_VALUE, resistance, false, false, false));

			// If it's a zombie, apply bonus tool spawn rate
			if (entity instanceof Zombie zombie) {
				applyBonusZombieTool(zombie);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error buffing creature in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	// -------------------------------------------------------------------------
	// On entity damage: amplify damage dealt to players
	// -------------------------------------------------------------------------
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!event.getEntity().getWorld().equals(getWorld()))
			return;
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EVENT)) {
				return;
			}
			double damageMultiplier = 1.0 + (Feature.BLOOD_MOON.getDouble("damage-percent") / 100.0);
			event.setDamage(event.getDamage() * damageMultiplier);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error amplifying player damage in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	// -------------------------------------------------------------------------
	// Close: remove debuffs from all players in the world
	// -------------------------------------------------------------------------
	@Override
	public void close() {
		super.close();
		try {
			for (Player player : getWorld().getPlayers()) {
				player.removePotionEffect(PotionEffectType.WEAKNESS);
				player.removePotionEffect(PotionEffectType.CONFUSION);
				player.removePotionEffect(PotionEffectType.BLINDNESS);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error cleaning up Blood Moon effects in world: " + getWorld().getName(), e);
		}
	}

	// -------------------------------------------------------------------------
	// Helper: give zombie a random tool with Blood Moon bonus rate
	// -------------------------------------------------------------------------
	private void applyBonusZombieTool(Zombie zombie) {
		if (zombie.hasMetadata("SDCustomMob"))
			return;

		double bonusChance = Feature.BLOOD_MOON.getDouble("zombie-tool-bonus-percent") / 100.0;
		if (RANDOM.nextDouble() > bonusChance)
			return;

		ToolTier tier = selectTier();
		if (tier == null)
			return;

		ToolType type = ToolType.values()[RANDOM.nextInt(ToolType.values().length)];
		Material mat = Material.getMaterial(tier.prefix + type.suffix);
		if (mat == null)
			return;

		ItemStack tool = new ItemStack(mat);
		// 40% chance for a random enchantment
		if (RANDOM.nextDouble() < 0.40) {
			applyRandomEnchant(tool);
		}

		if (zombie.getEquipment() != null) {
			zombie.getEquipment().setItemInMainHand(tool);
			zombie.getEquipment().setItemInMainHandDropChance(0.0f);
			zombie.setCanPickupItems(false);
		}
	}

	private ToolTier selectTier() {
		int total = 0;
		for (ToolTier t : ToolTier.values())
			total += t.weight;
		int roll = RANDOM.nextInt(total);
		int cumulative = 0;
		for (ToolTier t : ToolTier.values()) {
			cumulative += t.weight;
			if (roll < cumulative)
				return t;
		}
		return ToolTier.WOODEN;
	}

	private void applyRandomEnchant(ItemStack item) {
		List<Enchantment> options = new ArrayList<>();
		options.add(Enchantment.DAMAGE_ALL);
		options.add(Enchantment.DIG_SPEED);
		options.add(Enchantment.DURABILITY);
		if (item.getType().name().contains("_AXE")) {
			options.add(Enchantment.DAMAGE_UNDEAD);
		}
		Enchantment pick = options.get(RANDOM.nextInt(options.size()));
		int level = RANDOM.nextInt(pick.getMaxLevel()) + 1;
		try {
			item.addUnsafeEnchantment(pick, level);
		} catch (Exception ignored) {
		}
	}
}