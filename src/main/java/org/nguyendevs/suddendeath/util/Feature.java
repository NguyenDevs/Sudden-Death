package org.nguyendevs.suddendeath.util;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.player.Modifier;
import org.nguyendevs.suddendeath.player.Modifier.Type;
import org.nguyendevs.suddendeath.world.BloodMoon;
import org.nguyendevs.suddendeath.world.StatusRetriever;
import org.nguyendevs.suddendeath.world.Thunderstorm;
import org.nguyendevs.suddendeath.world.WorldEventHandler;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Enum representing various features that can be enabled in the SuddenDeath plugin.
 * Each feature defines gameplay modifiers and optional world events.
 */
public enum Feature {

	ADVANCED_PLAYER_DROPS(
			"Advanced Player Drops",
			new String[]{
					"Players drop a skull, bones, and human",
					"flesh on death. Human flesh can be cooked."
			},
			"advanced-player-drops",
			new Modifier[]{
					new Modifier("drop-skull", true),
					new Modifier("player-skull", true),
					new Modifier("dropped-flesh", 2),
					new Modifier("dropped-bones", 2)
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
	/*ARMOR_WEIGHT(
			"Armor Weight",
			new String[]{
					"Players move slower when wearing iron, gold, or diamond armor.",
					"Movement Speed Malus: #movement-speed-malus#%"
			},
			"armor-weight",
			new Modifier[]{new Modifier("movement-speed-malus", 3)}
	),
	*/

	ARROW_SLOW(
			"Arrow Slow",
			new String[]{
					"Players hit by arrows are slowed",
					"for #slow-duration# seconds."
			},
			"arrow-slow",
			new Modifier[]{new Modifier("slow-duration", 2.0)}
	),
	/*BLAZE_LASER(
			"Unreadable Fireball",
			new String[]{
					"Fireball shoot by blaze have a #chance-percent#% can't be read or dodge by player!"
			},
			"unreadble-fireball",
			new Modifier[]{
					new Modifier("chance-percent",30),
					new Modifier("shoot-amount",3),
					new Modifier("damage", 15)
					//new Modifier("speed",4)
			}
	),
	 */
	BLEEDING(
			"Bleeding",
			new String[]{
					"Players have a #chance-percent#% chance",
					"to bleed when damaged.",
					"Bleeding disables health saturation regen",
					"and deals #dps#*3 HP every 3 seconds.",
					"Can be stopped by using a Bandage or",
					"it will stop on its own after #auto-stop-bleed-time# seconds."
			},
			"bleeding",
			new Modifier[]{
					new Modifier("dps", 0.3),
					new Modifier("chance-percent", 10.0),
					new Modifier("health-min", 0),
					new Modifier("auto-stop-bleed-time", 30),
					new Modifier("tug", true)
			}
	),
	BLOOD_MOON(
			"Blood Moon",
			new String[]{
					"Night has #chance#% chance to turn red.",
					"Players take #damage-percent#% more damage",
					"and are slowed for #slow-duration#s.",
					"Monsters spawn with Speed #speed#,",
					"Strength #increase-damage#, and Resistance #damage-resistance#."
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
	BLOOD_SCREEN(
			"Blood Screen",
			new String[]{
					"Players will have bleeding and red",
					"screen effects when taking damage"
			},
			"blood-screen",
			new Modifier[]{
					new Modifier("mode", "HEALTH"),
					new Modifier("interval", 6),
					new Modifier("coefficient", 0.95),
			}
	),
	BONE_GRENADES(
			"Bone Grenades",
			new String[]{
					"Skeletons have #chance-percent#% chance",
					"to throw bone grenades.",
					"Grenades explode, dealing #damage# damage",
					"and knocking back players."
			},
			"bone-grenades",
			new Modifier[]{
					new Modifier("chance-percent", 35.0),
					new Modifier("damage", 6.0)
			}
	),

	BONE_WIZARDS(
			"Bone Wizards",
			new String[]{
					"Skeletons named 'Bone Wizards' cast spells:",
					"Fireball: #fireball-damage# damage,",
					"#fireball-duration# sec. burn",
					"Frost Curse: #frost-curse-damage# damage,",
					"#frost-curse-duration# sec. slow (#frost-curse-amplifier#)"
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
	BREEZE_DASH(
			"Breeze Dash",
			new String[]{
					"Breeze has a #chance-percent#% chance to accelerate,",
					"very quickly, continuously firing #shoot-amount#",
					"WindCharges at enemies but in return",
					"the ability to jump is greatly reduced."
			}, "breeze-dash",
			new Modifier[]{
					new Modifier("chance-percent", 20),
					new Modifier("shoot-amount", 5),
					new Modifier("amplifier", 2),
					new Modifier("duration", 10)
			}
	),
	CREEPER_REVENGE(
			"Creeper Revenge",
			new String[]{
					"Creepers have #chance-percent#% chance",
					"to explode on death."
			},
			"creeper-revenge",
			new Modifier[]{new Modifier("chance-percent", 15.0)}
	),
	DANGEROUS_COAL(
			"Dangerous Coal",
			new String[]{
					"Mining coal has #chance-percent#% chance",
					"to trigger a gas pocket explosion.",
					"Explosion radius: #radius# blocks."
			},
			"dangerous-coal",
			new Modifier[]{
					new Modifier("chance-percent", 5.0),
					new Modifier("radius", 5.0)
			}
	),
	ELECTRICITY_SHOCK(
			"Electricity Shock",
			new String[]{
					"Powered redstone (wires, torches,",
					"repeaters, comparators) deals #damage# damage.",
					"Can occur every 3 seconds."
			},
			"electricity-shock",
			new Modifier[]{new Modifier("damage", 6.0)}
	),
	ENDER_POWER(
			"Ender Power",
			new String[]{
					"Players have #chance-percent#% chance",
					"to be blinded for #duration# seconds.",
					"Triggered when hitting endermen, endermites,",
					"shulkers, or dragons."
			},
			"ender-power",
			new Modifier[]{
					new Modifier("chance-percent", 70),
					new Modifier("duration", 6.0)
			}
	),
	EVERBURNING_BLAZES(
			"Everburning Blazes",
			new String[]{
					"Blazes summon fire beams that ignite",
					"players for #burn-duration# seconds."
			},
			"everburning-blazes",
			new Modifier[]{new Modifier("burn-duration", 3.0)}
	),
	FALL_STUN(
			"Fall Stun",
			new String[]{
					"High falls cause players to be slowed (III)",
					"for a duration. Duration scales with fall height."
			},
			"fall-stun",
			new Modifier[]{new Modifier("duration-amplifier", 1)}
	),
	FORCE_OF_THE_UNDEAD(
			"Force of the Undead",
			new String[]{
					"Monsters deal increased attack damage.",
					"&cConfigurable for each monster."
			},
			"force-of-the-undead",
			new Modifier[]{new Modifier("additional-ad-percent", 25.0, Type.EACH_MOB)}
	),
	FREDDY(
			"Freddy",
			new String[]{
					"Players have #chance-percent#% chance",
					"to summon Freddy upon waking."
			},
			"freddy",
			new Modifier[]{new Modifier("chance-percent", 5.0)}
	),
	HOMING_FLAME_BARRAGE(
			"Homing Flame Barrage",
			new String[]{
					"Blaze has a chance to fire a #shoot-amount# beam of fire.",
					"It tracks the player and deals #damage# HP each beam",
					"that hits, with a hit rate of almost 100%."
			},
			"homing-flame-barrage",
			new Modifier[]{
					new Modifier("chance-percent", 50),
					new Modifier("shoot-amount", 3),
			        new Modifier("damage", 1)}
	),
	HUNGER_NAUSEA(
			"Hunger Nausea",
			new String[]{
					"Players get permanent nausea when hungry."
			},
			"hunger-nausea",
			new Modifier[]{}
	),
	IMMORTAL_EVOKER(
			"Immortal Evoker",
			new String[]{
					"Evoker has a #chance-percent#% chance to use Totem to",
					"avoid death and gain level #resistance-amplifier# resistance.",
					"After that, every 5 seconds, summon Fangs ",
					"can pull the player into the ground 3 block."
			},
			"immortal-evoker",
			new Modifier[]{
					new Modifier("chance-percent", 75.0),
					new Modifier("resistance-amplifier", 3),
			}
	),
	INFECTION(
			"Infection",
			new String[]{
					"Zombies have a #chance-percent#% chance",
					"to infect players.",
					"Infection causes nausea and deals #dps#*3 HP every ",
					"3 seconds. Can be stopped using a Strange Brew.",
					"Infection spreads via bare-hand attacks",
					"or player-to-player contact."
			},
			"infection",
			new Modifier[]{
					new Modifier("dps", 0.3),
					new Modifier("chance-percent", 15.0),
					new Modifier("health-min", 0),
					new Modifier("tug", true),
					new Modifier("sound", true)
			}
	),
	LEAPING_SPIDERS(
			"Leaping Spiders",
			new String[]{
					"Spiders can leap powerfully at players."
			},
			"leaping-spiders",
			new Modifier[]{}
	),
	MOB_CRITICAL_STRIKES(
			"Mob Critical Strikes",
			new String[]{
					"Monsters can deal critical strikes",
					"with #damage-percent#% additional damage.",
					"&cCrit Chance configurable for each monster."
			},
			"mob-critical-strikes",
			new Modifier[]{
					new Modifier("crit-chance", 17, Type.EACH_MOB),
					new Modifier("damage-percent", 75.0)
			}
	),
	NETHER_SHIELD(
			"Nether Shield",
			new String[]{
					"Magma cubes, pigmen, and blazes have",
					"#chance-percent#% chance to block attacks.",
					"Reflects #dmg-reflection-percent#% damage",
					"and ignites for #burn-duration# seconds."
			},
			"nether-shield",
			new Modifier[]{
					new Modifier("chance-percent", 37.5),
					new Modifier("dmg-reflection-percent", 75.0),
					new Modifier("burn-duration", 3.0)
			}
	),
	PHYSIC_ENDER_PEARL(
			"Physic EnderPearl",
			new String[]{
					"EnderPearl will have physical properties",
					"like bounce, friction."
			},
			"physic-ender-pearl",
			new Modifier[]{new Modifier("bounciness", 0.85), new Modifier("vertical-bounciness", 0.7),
					new Modifier("max-bounces",5), new Modifier("friction", 0.98), new Modifier("min-velocity-threshold", 0.03)}
	),
	POISONED_SLIMES(
			"Poisoned Slimes",
			new String[]{
					"Slimes have #chance-percent#% chance",
					"to poison players for #duration# seconds.",
					"Poison effect amplifier: #amplifier#."
			},
			"poisoned-slimes",
			new Modifier[]{
					new Modifier("amplifier", 1),
					new Modifier("duration", 3.0),
					new Modifier("chance-percent", 65.0)
			}
	),
	QUICK_MOBS(
			"Quick Mobs",
			new String[]{
					"Monsters have increased movement speed.",
					"&cConfigurable for each monster."
			},
			"quick-mobs",
			new Modifier[]{new Modifier("additional-ms-percent", 25.0, Type.EACH_MOB)}
	),
	REALISTIC_PICKUP(
			"Realistic Pickup",
			new String[]{
					"Players must crouch and look down",
					"to pick up items.",
					"Briefly slows players on pickup."
			},
			"realistic-pickup",
			new Modifier[]{}
	),
	SHOCKING_SKELETON_ARROWS(
			"Shocking Skeleton Arrows",
			new String[]{
					"Skeletons have #chance-percent#% chance",
					"to shoot arrows that shock players",
					"for #shock-duration# seconds."
			},
			"shocking-skeleton-arrows",
			new Modifier[]{
					new Modifier("chance-percent", 40),
					new Modifier("shock-duration", 1.0)}
	),
	SILVERFISHES_SUMMON(
			"Silverfishes Summon",
			new String[]{
					"Zombies have #chance-percent#% chance",
					"to summon #min# to #max# silverfish on death."
			},
			"silverfishes-summon",
			new Modifier[]{
					new Modifier("chance-percent", 35.0),
					new Modifier("min", 1),
					new Modifier("max", 2)
			}
	),
	SNOW_SLOW(
			"Snow Slow",
			new String[]{
					"Players without leather boots",
					"are slowed on snow."
			},
			"snow-slow",
			new Modifier[]{}
	),
	STONE_STIFFNESS(
			"Stone Stiffness",
			new String[]{
					"Players take #damage# damage",
					"when punching stone."
			},
			"stone-stiffness",
			new Modifier[]{new Modifier("damage", 1.0)}
	),
	STRAY_FROST(
			"Stray Frost",
			new String[]{
					"Stray have a #chance-percent#% chance",
					"to shoot a Frost arrow.",
					"The player will freeze in #duration# second."
			}, "stray-frost",
			new Modifier[]{
					new Modifier("chance-percent", 15),
					new Modifier("duration", 5)
			}
	),
	SPIDER_WEB(
			"Spider Web",
			new String[]{
					"Cave spider has a #chance-percent#% chance",
					"to shoot a web that traps the player in place.",
					"It only disappears when the player breaks it."
			}, "spider-web",
			new Modifier[]{
					new Modifier("chance-percent", 15),
					new Modifier("amount-per-shoot", 5)
			}
	),
	TANKY_MONSTERS(
			"Tanky Monsters",
			new String[]{
					"Monsters take reduced damage.",
					"&cConfigurable for each monster."
			},
			"tanky-monsters",
			new Modifier[]{new Modifier("dmg-reduction-percent", 25.0, Type.EACH_MOB)}
	),
	THIEF_SLIMES(
			"Thief Slimes",
			new String[]{
					"Slimes have #chance-percent#% chance",
					"to steal #exp# EXP when hitting players."
			},
			"thief-slimes",
			new Modifier[]{
					new Modifier("chance-percent", 55),
					new Modifier("exp", 12)
			}
	),
	THUNDERSTORM(
			"Thunderstorm",
			new String[]{
					"Storms have #chance#% chance",
					"to become thunderstorms.",
					"Lightning deals #damage-percent#% more AoE damage",
					"and strikes more frequently."
			},
			"thunderstorm",
			new Modifier[]{
					new Modifier("chance", 25),
					new Modifier("damage-percent", 125)
			},
			Thunderstorm::new
	),
	UNDEAD_GUNNERS(
			"Undead Gunners",
			new String[]{
					"Zombies named 'Undead Gunner' cast rockets",
					"dealing #damage# damage (AoE)."
			},
			"undead-gunners",
			new Modifier[]{
					new Modifier("damage", 7.0),
					new Modifier("block-damage", 0)
			}
	),
	UNDEAD_RAGE(
			"Undead Rage",
			new String[]{
					"Zombies and pigmen gain Strength and Speed II",
					"for #rage-duration# seconds when damaged."
			},
			"undead-rage",
			new Modifier[]{new Modifier("rage-duration", 4.0)}
	),

	WITCH_SCROLLS(
			"Witch Scrolls",
			new String[]{
					"Witches have a #chance-percent#% chance",
					"to block damage with a magic shield.",
					"Witches cast runes dealing #damage# damage",
					"and slowing players (II) for #slow-duration# seconds."
			},
			"witch-scrolls",
			new Modifier[]{
					new Modifier("chance-percent", 55.0),
					new Modifier("damage", 2.5),
					new Modifier("slow-duration", 2.0)
			}
	),
	WITHER_MACHINEGUN(
			"Wither Machinegun",
			new String[]{
					"Wither Skeletons throw coal,",
					"each dealing #damage# damage."
			},
			"wither-machinegun",
			new Modifier[]{new Modifier("damage", 2)}
	),
	WITHER_RUSH(
			"Wither Rush",
			new String[]{
					"Wither Skeletons blink to players,",
					"dealing #damage# damage."
			},
			"wither-rush",
			new Modifier[]{new Modifier("damage", 3.0)}
	);

	private String name;
	private List<String> lore;
	private final String path;
	private final List<Modifier> modifiers;
	private final Function<World, WorldEventHandler> event;
	private ConfigFile configFile;
	private static FileConfiguration featureConfig;

	/**
	 * Constructor for features without world events.
	 */
	Feature(String defaultName, String[] defaultLore, String path, Modifier[] modifiers) {
		this(defaultName, defaultLore, path, modifiers, null);
	}

	/**
	 * Constructor for features with world events.
	 */
	Feature(String defaultName, String[] defaultLore, String path, Modifier[] modifiers, Function<World, WorldEventHandler> event) {
		this.path = path;
		this.modifiers = Collections.unmodifiableList(Arrays.asList(modifiers));
		this.event = event;
		loadDescriptions(defaultName, defaultLore); // Tải mô tả từ file YAML
	}

	/**
	 * Tải name và lore từ Feature.yml, sử dụng giá trị mặc định nếu không tìm thấy.
	 */
	private void loadDescriptions(String defaultName, String[] defaultLore) {
		if (featureConfig == null) {
			try {
				File file = new File(SuddenDeath.getInstance().getDataFolder(), "language/feature.yml");
				if (!file.exists()) {
					SuddenDeath.getInstance().saveResource("language/feature.yml", false);
				}
				featureConfig = YamlConfiguration.loadConfiguration(file);
			} catch (Exception e) {
				SuddenDeath.getInstance().getLogger().log(Level.SEVERE, "Không thể tải Feature.yml", e);
			}
		}

		ConfigurationSection section = featureConfig.getConfigurationSection("features." + path);
		if (section != null) {
			this.name = section.getString("name", defaultName);
			this.lore = section.getStringList("lore");
			if (this.lore.isEmpty()) {
				this.lore = Collections.unmodifiableList(Arrays.asList(defaultLore));
			} else {
				this.lore = Collections.unmodifiableList(this.lore);
			}
		} else {
			this.name = defaultName;
			this.lore = Collections.unmodifiableList(Arrays.asList(defaultLore));
		}
	}

	/**
	 * Tải lại mô tả cho tất cả các tính năng.
	 */
	public static void reloadDescriptions() {
		featureConfig = null;
		for (Feature feature : values()) {
			feature.loadDescriptions(feature.name, feature.lore.toArray(new String[0]));
		}
	}

	/**
	 * Lấy tên hiển thị của tính năng.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Lấy mô tả của tính năng.
	 */
	public List<String> getLore() {
		return lore;
	}

	/**
	 * Lấy đường dẫn cấu hình cho tính năng.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Khởi tạo hoặc cập nhật file cấu hình cho tính năng.
	 */
	public void updateConfig() {
		try {
			configFile = new ConfigFile("/modifiers", path);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Không thể khởi tạo cấu hình cho tính năng: " + name, e);
		}
	}

	/**
	 * Lấy file cấu hình cho tính năng.
	 */
	public ConfigFile getConfigFile() {
		if (configFile == null) {
			updateConfig();
		}
		return configFile;
	}

	/**
	 * Lấy giá trị boolean từ cấu hình của tính năng.
	 */
	public boolean getBoolean(String path) {
		return getConfigFile().getConfig().getBoolean(path, false);
	}

	/**
	 * Lấy giá trị double từ cấu hình của tính năng.
	 */
	public double getDouble(String path) {
		return getConfigFile().getConfig().getDouble(path, 0.0);
	}

	/**
	 * Lấy giá trị chuỗi từ cấu hình của tính năng.
	 */
	public String getString(String path) {
		return getConfigFile().getConfig().getString(path, "");
	}

	/**
	 * Lấy danh sách các modifier của tính năng.
	 */
	public List<Modifier> getModifiers() {
		return modifiers;
	}

	/**
	 * Kiểm tra xem tính năng có liên quan đến sự kiện thế giới không.
	 */
	public boolean isEvent() {
		return event != null;
	}

	/**
	 * Tạo WorldEventHandler cho thế giới đã cho, nếu có.
	 */
	public StatusRetriever generateWorldEventHandler(World world) {
		if (isEvent()) {
			WorldEventHandler handler = event.apply(world);
			return handler;
		}
		return null;
	}

	/**
	 * Kiểm tra xem tính năng có được kích hoạt cho entity không.
	 */
	public boolean isEnabled(Entity entity) {
		return isEnabled(entity.getWorld());
	}

	/**
	 * Kiểm tra xem tính năng có được kích hoạt cho thế giới không.
	 */
	public boolean isEnabled(World world) {
		try {
			List<String> enabledWorlds = SuddenDeath.getInstance().getConfig().getStringList(path);
			return enabledWorlds.contains(world.getName());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Lỗi khi kiểm tra tính năng " + name + " cho thế giới " + world.getName(), e);
			return false;
		}
	}
}