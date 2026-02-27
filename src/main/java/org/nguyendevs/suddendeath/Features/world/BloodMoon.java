package org.nguyendevs.suddendeath.Features.world;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nguyendevs.suddendeath.Hook.CustomFlag;
import org.nguyendevs.suddendeath.Managers.EventManager.WorldStatus;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BloodMoon extends WorldEventHandler {

	private static final Random RANDOM = new Random();

	// Players currently carrying a skeleton mark (immune to re-marking until
	// resolved)
	private final Set<UUID> markedPlayers = ConcurrentHashMap.newKeySet();

	// Materials used for creeper crater decoration
	private static final Material[] CRATER_BLOCKS = {
			Material.MAGMA_BLOCK, Material.BLACKSTONE, Material.COBBLED_DEEPSLATE,
			Material.BASALT, Material.DEEPSLATE
	};

	// -------------------------------------------------------------------------
	// Tool tier / type helpers for zombie bonus tools
	// -------------------------------------------------------------------------
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
		super(world, 180, WorldStatus.BLOOD_MOON); // run() every 180 ticks = 9s (6s nausea + 3s gap)
	}

	// =========================================================================
	// Repeating Task — player debuffs every 9 seconds
	// =========================================================================
	@Override
	public void run() {
		try {
			for (Player player : getWorld().getPlayers()) {
				if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
					continue;
				if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EVENT))
					continue;

				// Stun — Nausea 6s (120 ticks); task period 180t → natural 3s gap
				player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,
						120, 0, false, false, false));
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Error running Blood Moon debuff task in world: " + getWorld().getName(), e);
		}
	}

	// =========================================================================
	// Creature Spawn — buff all monsters; Creeper gets extra speed
	// =========================================================================
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		LivingEntity entity = event.getEntity();
		if (!(entity instanceof Monster))
			return;
		if (!entity.getWorld().equals(getWorld()))
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard()
					.isFlagAllowedAtLocation(entity.getLocation(), CustomFlag.SDS_EVENT))
				return;

			int baseSpeed = (int) Feature.BLOOD_MOON.getDouble("speed") - 1;
			int strength = (int) Feature.BLOOD_MOON.getDouble("increase-damage") - 1;
			int resistance = (int) Feature.BLOOD_MOON.getDouble("damage-resistance") - 1;

			// Creeper gets one extra speed level
			int entitySpeed = (entity instanceof Creeper) ? baseSpeed + 1 : baseSpeed;

			entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
					Integer.MAX_VALUE, entitySpeed, false, false, false));
			entity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,
					Integer.MAX_VALUE, strength, false, false, false));
			entity.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
					Integer.MAX_VALUE, resistance, false, false, false));

			if (entity instanceof Zombie zombie) {
				applyBonusZombieTool(zombie);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error buffing creature in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	// =========================================================================
	// Player Deals Damage — reduce outgoing damage by weakness-percent%
	// =========================================================================
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerDealDamage(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player player))
			return;
		if (!player.getWorld().equals(getWorld()))
			return;
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EVENT))
				return;
			double reduction = Feature.BLOOD_MOON.getDouble("weakness-percent") / 100.0;
			event.setDamage(event.getDamage() * (1.0 - reduction));
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error reducing player outgoing damage in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!event.getEntity().getWorld().equals(getWorld()))
			return;
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EVENT))
				return;
			double multiplier = 1.0 + (Feature.BLOOD_MOON.getDouble("damage-percent") / 100.0);
			event.setDamage(event.getDamage() * multiplier);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error amplifying player damage in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	// =========================================================================
	// Skeleton Arrow — Shadow Mark
	// =========================================================================
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSkeletonArrowHit(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (!player.getWorld().equals(getWorld()))
			return;
		if (!(event.getDamager() instanceof Arrow arrow))
			return;
		if (!(arrow.getShooter() instanceof AbstractSkeleton))
			return;
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard().isFlagAllowed(player, CustomFlag.SDS_EVENT))
				return;

			// Only mark if not already marked
			if (!markedPlayers.add(player.getUniqueId()))
				return;

			// Schedule detonation after 3 seconds
			Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
				markedPlayers.remove(player.getUniqueId());
				if (!player.isOnline() || !player.getWorld().equals(getWorld()))
					return;
				if (player.isDead())
					return;

				double chance = Feature.BLOOD_MOON.getDouble("skeleton-mark-chance") / 100.0;
				if (RANDOM.nextDouble() < chance) {
					// Instant Damage II (amplifier 1 = level 2)
					player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 1, false, false, true));
				}
			}, 60L);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error in Blood Moon skeleton mark for world: " + getWorld().getName(), e);
		}
	}

	// =========================================================================
	// Creeper Fuse — Expand explosion radius
	// =========================================================================
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCreeperFuse(ExplosionPrimeEvent event) {
		if (!(event.getEntity() instanceof Creeper))
			return;
		if (!event.getEntity().getWorld().equals(getWorld()))
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard()
					.isFlagAllowedAtLocation(event.getEntity().getLocation(), CustomFlag.SDS_EVENT))
				return;

			double multiplier = Feature.BLOOD_MOON.getDouble("creeper-explosion-multiplier");
			event.setRadius((float) (event.getRadius() * multiplier));
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error expanding creeper explosion in Blood Moon for world: " + getWorld().getName(), e);
		}
	}

	// =========================================================================
	// Creeper Explosion — Crater decoration (Void Bomb)
	// =========================================================================
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onCreeperExplosion(EntityExplodeEvent event) {
		if (!(event.getEntity() instanceof Creeper))
			return;
		if (!event.getEntity().getWorld().equals(getWorld()))
			return;

		try {
			if (!SuddenDeath.getInstance().getWorldGuard()
					.isFlagAllowedAtLocation(event.getLocation(), CustomFlag.SDS_EVENT))
				return;

			// Compute bounding box of the explosion from blockList
			final List<Block> blocks = new ArrayList<>(event.blockList());
			if (blocks.isEmpty())
				return;

			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
			for (Block b : blocks) {
				minX = Math.min(minX, b.getX());
				maxX = Math.max(maxX, b.getX());
				minY = Math.min(minY, b.getY());
				maxY = Math.max(maxY, b.getY());
				minZ = Math.min(minZ, b.getZ());
				maxZ = Math.max(maxZ, b.getZ());
			}
			// Expand by 1 to catch wall blocks just outside the carved zone
			final int fx0 = minX - 1, fy0 = minY - 1, fz0 = minZ - 1;
			final int fx1 = maxX + 1, fy1 = maxY + 1, fz1 = maxZ + 1;
			final World world = event.getEntity().getWorld();

			// Let explosion carve normally — decorate 1 tick later
			Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
				for (int x = fx0; x <= fx1; x++) {
					for (int y = fy0; y <= fy1; y++) {
						for (int z = fz0; z <= fz1; z++) {
							Block block = world.getBlockAt(x, y, z);
							Material type = block.getType();

							if (isAir(type)) {
								// --- Lava at crater bottom ---
								// Condition: upper face = AIR, all other 5 faces = solid (not air)
								Block above = block.getRelative(0, 1, 0);
								Block below = block.getRelative(0, -1, 0);
								Block north = block.getRelative(-1, 0, 0);
								Block south = block.getRelative(1, 0, 0);
								Block east = block.getRelative(0, 0, 1);
								Block west = block.getRelative(0, 0, -1);
								if (isAir(above.getType())
										&& !isAir(below.getType())
										&& !isAir(north.getType())
										&& !isAir(south.getType())
										&& !isAir(east.getType())
										&& !isAir(west.getType())) {
									block.setType(Material.LAVA);
								}
							} else {
								// --- Crater wall decoration ---
								// Solid block adjacent to at least one air block → 30% chance replace
								if (RANDOM.nextDouble() < 0.30 && hasAdjacentAir(block)) {
									block.setType(CRATER_BLOCKS[RANDOM.nextInt(CRATER_BLOCKS.length)]);
								}
							}
						}
					}
				}
			}, 1L);

		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error decorating creeper crater in Blood Moon for world: " + getWorld().getName(), e);
		}

	}

	// =========================================================================
	// Close — cleanup
	// =========================================================================
	@Override
	public void close() {
		super.close();
		try {
			markedPlayers.clear();
			for (Player player : getWorld().getPlayers()) {
				player.removePotionEffect(PotionEffectType.CONFUSION);
			}
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error cleaning up Blood Moon effects in world: " + getWorld().getName(), e);
		}
	}

	// =========================================================================
	// Zombie bonus tool helpers
	// =========================================================================
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
		if (RANDOM.nextDouble() < 0.40)
			applyRandomEnchant(tool);

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
		if (item.getType().name().contains("_AXE"))
			options.add(Enchantment.DAMAGE_UNDEAD);
		Enchantment pick = options.get(RANDOM.nextInt(options.size()));
		int level = RANDOM.nextInt(pick.getMaxLevel()) + 1;
		try {
			item.addUnsafeEnchantment(pick, level);
		} catch (Exception ignored) {
		}
	}

	// =========================================================================
	// Block helpers
	// =========================================================================
	private boolean isAir(Material mat) {
		return mat == Material.AIR || mat == Material.CAVE_AIR;
	}

	private boolean hasAdjacentAir(Block block) {
		return isAir(block.getRelative(0, 1, 0).getType())
				|| isAir(block.getRelative(0, -1, 0).getType())
				|| isAir(block.getRelative(1, 0, 0).getType())
				|| isAir(block.getRelative(-1, 0, 0).getType())
				|| isAir(block.getRelative(0, 0, 1).getType())
				|| isAir(block.getRelative(0, 0, -1).getType());
	}
}