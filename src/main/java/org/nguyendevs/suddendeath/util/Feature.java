package org.nguyendevs.suddendeath.util;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.Modifier;
import org.nguyendevs.suddendeath.player.Modifier.Type;
import org.nguyendevs.suddendeath.world.BloodMoon;
import org.nguyendevs.suddendeath.world.StatusRetriever;
import org.nguyendevs.suddendeath.world.Thunderstorm;
import org.nguyendevs.suddendeath.world.WorldEventHandler;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Enum representing various features that can be enabled in the SuddenDeath plugin.
 * Each feature defines gameplay modifiers and optional world events.
 */
public enum Feature {
	BLOOD_SCREEN(
			"Blood Screen",
			new String[]{
					"Player screen turn red when get damage.",
			},
			"blood-screen",
			new Modifier[]{
					new Modifier("mode", "HEALTH"),
					new Modifier("interval", 6),
					new Modifier("coefficient", 0.95),
			}
	),
	QUICK_MOBS(
			"Quick Mobs",
			new String[]{"Monsters have increased movement speed.", "&cConfigurable for each monster."},
			"quick-mobs",
			new Modifier[]{new Modifier("additional-ms-percent", 25.0, Type.EACH_MOB)}
	),
	FORCE_OF_THE_UNDEAD(
			"Force of the Undead",
			new String[]{"Monsters deal increased attack damage.", "&cConfigurable for each monster."},
			"force-of-the-undead",
			new Modifier[]{new Modifier("additional-ad-percent", 25.0, Type.EACH_MOB)}
	),
	TANKY_MONSTERS(
			"Tanky Monsters",
			new String[]{"Monsters take reduced damage.", "&cConfigurable for each monster."},
			"tanky-monsters",
			new Modifier[]{new Modifier("dmg-reduction-percent", 25.0, Type.EACH_MOB)}
	),
	BLEEDING(
			"Bleeding",
			new String[]{
					"Players have a #chance-percent#% chance to bleed when damaged.",
					"Bleeding disables health saturation regen and deals #damage# every 3 seconds.",
					"Can be stopped by using a bandage."
			},
			"bleeding",
			new Modifier[]{
					new Modifier("dps", 0.1),
					new Modifier("chance-percent", 10.0),
					new Modifier("health-min", 0),
					new Modifier("auto-stop-bleed-time", 30),
					new Modifier("tug", true)
			}
	),
	INFECTION(
			"Infection",
			new String[]{
					"Zombies have a #chance-percent#% chance to infect players.",
					"Infection causes nausea and deals #damage# every 3 seconds.",
					"Can be stopped using a Strange Brew.",
					"Infection spreads via bare-hand attacks or player-to-player contact."
			},
			"infection",
			new Modifier[]{
					new Modifier("dps", 0.1),
					new Modifier("chance-percent", 15.0),
					new Modifier("health-min", 0),
					new Modifier("tug", true),
					new Modifier("sound", true)
			}
	),
	ARROW_SLOW(
			"Arrow Slow",
			new String[]{"Players hit by arrows are slowed for #slow-duration# seconds."},
			"arrow-slow",
			new Modifier[]{new Modifier("slow-duration", 3.0)}
	),
	SHOCKING_SKELETON_ARROWS(
			"Shocking Skeleton Arrows",
			new String[]{"Skeleton arrows shock players for #shock-duration# seconds."},
			"shocking-skeleton-arrows",
			new Modifier[]{new Modifier("shock-duration", 1.0)}
	),
	SILVERFISHES_SUMMON(
			"Silverfishes Summon",
			new String[]{"Zombies have #chance-percent#% chance to summon #min# to #max# silverfish on death."},
			"silverfishes-summon",
			new Modifier[]{
					new Modifier("chance-percent", 35.0),
					new Modifier("min", 1),
					new Modifier("max", 2)
			}
	),
	ARMOR_WEIGHT(
			"Armor Weight",
			new String[]{
					"Players move slower when wearing iron, gold, or diamond armor.",
					"Movement Speed Malus: #movement-speed-malus#%"
			},
			"armor-weight",
			new Modifier[]{new Modifier("movement-speed-malus", 3)}
	),
	HUNGER_NAUSEA(
			"Hunger Nausea",
			new String[]{"Players get permanent nausea when hungry."},
			"hunger-nausea",
			new Modifier[]{}
	),
	CREEPER_REVENGE(
			"Creeper Revenge",
			new String[]{"Creepers have #chance-percent#% chance to explode on death."},
			"creeper-revenge",
			new Modifier[]{new Modifier("chance-percent", 15.0)}
	),
	FALL_STUN(
			"Fall Stun",
			new String[]{
					"High falls cause players to be slowed (III) for a duration.",
					"Duration scales with fall height."
			},
			"fall-stun",
			new Modifier[]{new Modifier("duration-amplifier", 1)}
	),
	LEAPING_SPIDERS(
			"Leaping Spiders",
			new String[]{"Spiders can leap powerfully at players."},
			"leaping-spiders",
			new Modifier[]{}
	),
	ADVANCED_PLAYER_DROPS(
			"Advanced Player Drops",
			new String[]{
					"Players drop a skull, bones, and human flesh on death.",
					"Human flesh can be cooked."
			},
			"advanced-player-drops",
			new Modifier[]{
					new Modifier("drop-skull", true),
					new Modifier("player-skull", true),
					new Modifier("dropped-flesh", 2),
					new Modifier("dropped-bones", 2)
			}
	),
	STONE_STIFFNESS(
			"Stone Stiffness",
			new String[]{"Players take #damage# damage when punching stone."},
			"stone-stiffness",
			new Modifier[]{new Modifier("damage", 1.0)}
	),
	WITCH_SCROLLS(
			"Witch Scrolls",
			new String[]{
					"Witches have a #chance-percent#% chance to block damage with a magic shield.",
					"Witches cast runes dealing #damage# damage and slowing players (II) for #slow-duration# seconds."
			},
			"witch-scrolls",
			new Modifier[]{
					new Modifier("chance-percent", 55.0),
					new Modifier("damage", 2.5),
					new Modifier("slow-duration", 2.0)
			}
	),
	EVERBURNING_BLAZES(
			"Everburning Blazes",
			new String[]{"Blazes summon fire beams that ignite players for #burn-duration# seconds."},
			"everburning-blazes",
			new Modifier[]{new Modifier("burn-duration", 3.0)}
	),
	ELECTRICITY_SHOCK(
			"Electricity Shock",
			new String[]{
					"Powered redstone (wires, torches, repeaters, comparators) deals #damage# damage.",
					"Can occur every 3 seconds."
			},
			"electricity-shock",
			new Modifier[]{new Modifier("damage", 6.0)}
	),
	NETHER_SHIELD(
			"Nether Shield",
			new String[]{
					"Magma cubes, pigmen, and blazes have #chance-percent#% chance to block attacks.",
					"Reflects #dmg-reflection-percent#% damage and ignites for #burn-duration# seconds."
			},
			"nether-shield",
			new Modifier[]{
					new Modifier("chance-percent", 37.5),
					new Modifier("dmg-reflection-percent", 75.0),
					new Modifier("burn-duration", 3.0)
			}
	),
	UNDEAD_RAGE(
			"Undead Rage",
			new String[]{
					"Zombies and pigmen gain Strength and Speed II for #rage-duration# seconds when damaged."
			},
			"undead-rage",
			new Modifier[]{new Modifier("rage-duration", 4.0)}
	),
	BONE_GRENADES(
			"Bone Grenades",
			new String[]{
					"Skeletons have #chance-percent#% chance to throw bone grenades.",
					"Grenades explode, dealing #damage# damage and knocking back players."
			},
			"bone-grenades",
			new Modifier[]{
					new Modifier("chance-percent", 35.0),
					new Modifier("damage", 6.0)
			}
	),
	POISONED_SLIMES(
			"Poisoned Slimes",
			new String[]{
					"Slimes have #chance-percent#% chance to poison players for #duration# seconds.",
					"Poison effect amplifier: #amplifier#."
			},
			"poisoned-slimes",
			new Modifier[]{
					new Modifier("amplifier", 1),
					new Modifier("duration", 3.0),
					new Modifier("chance-percent", 65.0)
			}
	),
	ANGRY_SPIDERS(
			"Angry Spiders",
			new String[]{
					"Spiders throw cobwebs, dealing #damage# damage.",
					"Slows players (#amplifier#) for #duration# seconds."
			},
			"angry-spiders",
			new Modifier[]{
					new Modifier("damage", 4),
					new Modifier("duration", 3.0),
					new Modifier("amplifier", 1.0)
			}
	),
	ENDER_POWER(
			"Ender Power",
			new String[]{
					"Players have #chance-percent#% chance to be blinded for #duration# seconds.",
					"Triggered when hitting endermen, endermites, shulkers, or dragons."
			},
			"ender-power",
			new Modifier[]{
					new Modifier("chance-percent", 70),
					new Modifier("duration", 6.0)
			}
	),
	UNDEAD_GUNNERS(
			"Undead Gunners",
			new String[]{
					"Zombies named 'Undead Gunner' cast rockets dealing #damage# damage (AoE)."
			},
			"undead-gunners",
			new Modifier[]{
					new Modifier("damage", 7.0),
					new Modifier("block-damage", 0)
			}
	),
	REALISTIC_PICKUP(
			"Realistic Pickup",
			new String[]{
					"Players must crouch and look down to pick up items.",
					"Briefly slows players on pickup."
			},
			"realistic-pickup",
			new Modifier[]{}
	),
	BONE_WIZARDS(
			"Bone Wizards",
			new String[]{
					"Skeletons named 'Bone Wizards' cast spells:",
					"Fireball: #fireball-damage# damage, #fireball-duration# sec. burn",
					"Frost Curse: #frost-curse-damage# damage, #frost-curse-duration# sec. slow (#frost-curse-amplifier#)"
			},
			"bone-wizards",
			new Modifier[]{
					new Modifier("fireball-damage", 7.0),
					new Modifier("fireball-duration", 3.0),
					new Modifier("frost-curse-damage", 6.0),
					new Modifier("frost-curse-duration", 2.0),
					new Modifier("frost-curse-amplifier", 1)
			}
	),
	FREDDY(
			"Freddy",
			new String[]{"Players have #chance-percent#% chance to summon Freddy upon waking."},
			"freddy",
			new Modifier[]{new Modifier("chance-percent", 5.0)}
	),
	DANGEROUS_COAL(
			"Dangerous Coal",
			new String[]{
					"Mining coal has #chance-percent#% chance to trigger a gas pocket explosion.",
					"Explosion radius: #radius# blocks."
			},
			"dangerous-coal",
			new Modifier[]{
					new Modifier("chance-percent", 5.0),
					new Modifier("radius", 5.0)
			}
	),
	WITHER_RUSH(
			"Wither Rush",
			new String[]{"Wither Skeletons blink to players, dealing #damage# damage."},
			"wither-rush",
			new Modifier[]{new Modifier("damage", 3.0)}
	),
	MOB_CRITICAL_STRIKES(
			"Mob Critical Strikes",
			new String[]{
					"Monsters can deal critical strikes with #damage-percent#% additional damage.",
					"&cCrit Chance configurable for each monster."
			},
			"mob-critical-strikes",
			new Modifier[]{
					new Modifier("crit-chance", 17, Type.EACH_MOB),
					new Modifier("damage-percent", 75.0)
			}
	),
	THIEF_SLIMES(
			"Thief Slimes",
			new String[]{
					"Slimes have #chance-percent#% chance to steal #exp# EXP when hitting players."
			},
			"thief-slimes",
			new Modifier[]{
					new Modifier("chance-percent", 55),
					new Modifier("exp", 12)
			}
	),
	WITHER_MACHINEGUN(
			"Wither Machinegun",
			new String[]{"Wither Skeletons throw coal, each dealing #damage# damage."},
			"wither-machinegun",
			new Modifier[]{new Modifier("damage", 2)}
	),
	SNOW_SLOW(
			"Snow Slow",
			new String[]{"Players without iron, gold, or diamond boots are slowed on snow."},
			"snow-slow",
			new Modifier[]{}
	),
	BLOOD_MOON(
			"Blood Moon",
			new String[]{
					"Night has #chance#% chance to turn red.",
					"Players take #damage-percent#% more damage and are slowed for #slow-duration#s.",
					"Monsters spawn with Speed #speed#, Strength #increase-damage#, and Resistance #damage-resistance#."
			},
			"blood-moon",
			new Modifier[]{
					new Modifier("chance", 2),
					new Modifier("drowned-per-chunk", 3),
					new Modifier("damage-percent", 60),
					new Modifier("slow-duration", 3),
					new Modifier("increase-damage", 2),
					new Modifier("speed", 2),
					new Modifier("damage-resistance", 2)
			},
			BloodMoon::new
	),
	THUNDERSTORM(
			"Thunderstorm",
			new String[]{
					"Storms have #chance#% chance to become thunderstorms.",
					"Lightning deals #damage-percent#% more AoE damage and strikes more frequently."
			},
			"thunderstorm",
			new Modifier[]{
					new Modifier("chance", 25),
					new Modifier("damage-percent", 125)
			},
			Thunderstorm::new
	);

	private final String name;
	private final List<String> lore;
	private final String path;
	private final List<Modifier> modifiers;
	private final Function<World, WorldEventHandler> event;
	private ConfigFile configFile;

	/**
	 * Constructor for features without world events.
	 */
	Feature(String name, String[] lore, String path, Modifier[] modifiers) {
		this(name, lore, path, modifiers, null);
	}

	/**
	 * Constructor for features with world events.
	 */
	Feature(String name, String[] lore, String path, Modifier[] modifiers, Function<World, WorldEventHandler> event) {
		this.name = name;
		this.lore = Collections.unmodifiableList(Arrays.asList(lore));
		this.path = path;
		this.modifiers = Collections.unmodifiableList(Arrays.asList(modifiers));
		this.event = event;
	}

	/**
	 * Gets the display name of the feature.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the lore/description of the feature.
	 */
	public List<String> getLore() {
		return lore;
	}

	/**
	 * Gets the configuration path for the feature.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Initializes or updates the configuration file for this feature.
	 */
	public void updateConfig() {
		try {
			configFile = new ConfigFile("/modifiers", path);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Failed to initialize config for feature: " + name, e);
		}
	}

	/**
	 * Gets the configuration file for this feature.
	 */
	public ConfigFile getConfigFile() {
		if (configFile == null) {
			updateConfig();
		}
		return configFile;
	}

	/**
	 * Gets a boolean value from the feature's configuration.
	 */
	public boolean getBoolean(String path) {
		return getConfigFile().getConfig().getBoolean(path, false);
	}

	/**
	 * Gets a double value from the feature's configuration.
	 */
	public double getDouble(String path) {
		return getConfigFile().getConfig().getDouble(path, 0.0);
	}

	/**
	 * Gets a string value from the feature's configuration.
	 */
	public String getString(String path) {
		return getConfigFile().getConfig().getString(path, "");
	}

	/**
	 * Gets the list of modifiers for this feature.
	 */
	public List<Modifier> getModifiers() {
		return modifiers;
	}

	/**
	 * Checks if this feature is associated with a world event.
	 */
	public boolean isEvent() {
		return event != null;
	}

	/**
     * Generates a WorldEventHandler for the given world, if applicable.
     */
	public StatusRetriever generateWorldEventHandler(World world) {
		if (isEvent()) {
			WorldEventHandler handler = event.apply(world);
			return handler;
		}
		return null;
	}

	/**
	 * Checks if the feature is enabled for the given entity.
	 */
	public boolean isEnabled(Entity entity) {
		return isEnabled(entity.getWorld());
	}

	/**
	 * Checks if the feature is enabled for the given world.
	 */
	public boolean isEnabled(World world) {
		try {
			List<String> enabledWorlds = SuddenDeath.getInstance().getConfig().getStringList(path);
			return enabledWorlds.contains(world.getName());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error checking if feature " + name + " is enabled for world " + world.getName(), e);
			return false;
		}
	}
}