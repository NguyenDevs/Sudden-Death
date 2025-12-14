package org.nguyendevs.suddendeath.Utils;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Features.world.*;
import org.nguyendevs.suddendeath.Player.Modifier;
import org.nguyendevs.suddendeath.Player.Modifier.Type;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

public enum Feature {

	ABYSSAL_VORTEX(
			"Abyssal Vortex",
			new String[]{
					"&3The Guardian &7have a #chance-percent#% chance create a &fWhirlpool &7that",
					"pulls players toward it, preventing them from escaping."
			},
			"abyssal-vortex",
			new Modifier[]{
					new Modifier("chance-percent", 30),
					new Modifier("duration", 2),
					new Modifier("strength", 0.55)
			}
	),

	ADVANCED_PLAYER_DROPS(
			"Advanced Player Drops",
			new String[]{
					"&fPlayers &7drop a skull, bones and human",
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
					"&8Spiders &7throw cobwebs, dealing #damage# damage.",
					"Slows players (#amplifier#) for #duration# seconds."
			},
			"angry-spiders",
			new Modifier[]{
					new Modifier("damage", 4),
					new Modifier("duration", 3.0),
					new Modifier("amplifier", 1.0)
			}
	),

	ARMOR_PIERCING(
			"Armor Piercing",
			new String[]{
					"&cMonsters &7have a chance to deal &4True Damage&7,",
					"completely ignoring player armor.",
					" ",
					"&cConfigurable chance for each monster."
			},
			"armor-piercing",
			new Modifier[]{
					new Modifier("chance-percent", 15.0, Type.EACH_MOB),
					new Modifier("visual-particles", true),
					new Modifier("visual-sound", true)
			}
	),

	ARROW_SLOW(
			"Arrow Slow",
			new String[]{
					"&fPlayers &7hit by arrows are slowed",
					"for #slow-duration# seconds."
			},
			"arrow-slow",
			new Modifier[]{
					new Modifier("slow-duration", 1.5)
			}
	),

	BLEEDING(
			"Bleeding",
			new String[]{
					"&fPlayers &7have a #chance-percent#% chance",
					"to bleed when damaged.",
					"&cBleeding &7disables health saturation regen",
					"and deals #dps#*3 HP every 3 seconds.",
					"Can be stopped by using a &fBandage &7or",
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
					"&9Night &7has #chance#% chance to turn red.",
					"&fPlayers &7take #damage-percent#% more damage",
					"and are slowed for #slow-duration#s.",
					"&cMonsters &7spawn with &3Speed #speed#&7,",
					"&6Strength #increase-damage# &7and &8Resistance #damage-resistance#&7."
			},
			"blood-moon",
			new Modifier[]{
					new Modifier("chance", 2),
					new Modifier("drowned-per-chunk", 3),
					new Modifier("damage-percent", 60),
					new Modifier("slow-duration", 3),
					new Modifier("increase-damage", 2),
					new Modifier("speed", 2),
					new Modifier("damage-resistance", 2),
					new Modifier("max-mobs-per-chunk", 10)
			},
			BloodMoon::new
	),

	BLOOD_SCREEN(
			"Blood Screen",
			new String[]{
					"&fPlayers &7will have bleeding and red",
					"screen effects when taking damage"
			},
			"blood-screen",
			new Modifier[]{
					new Modifier("mode", "HEALTH"),
					new Modifier("interval", 6),
					new Modifier("coefficient", 0.95)
			}
	),

	BONE_GRENADES(
			"Bone Grenades",
			new String[]{
					"&fSkeletons &7have #chance-percent#% chance",
					"to throw bone grenades.",
					"&8Grenades &7explode, dealing #damage# damage",
					"and knocking back players."
			},
			"bone-grenades",
			new Modifier[]{
					new Modifier("chance-percent", 25.0),
					new Modifier("damage", 6.0)
			}
	),

	BONE_WIZARDS(
			"Bone Wizards",
			new String[]{
					"&fSkeletons &7named &f'Bone Wizard' &7cast spells:",
					"&c► Fireball: #fireball-damage# &7damage, #fireball-duration# sec. burn",
					"&3► Frost Curse: #frost-curse-damage# &7damage, #frost-curse-duration# sec. slow (#frost-curse-amplifier#)",
					" ",
					"&e&oYou can create custom Skeleton with name by using",
					"&6/sdmob &ecommand."
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
					"&3Breeze &7has a #chance-percent#% chance to accelerate,",
					"very quickly, continuously firing #shoot-amount#",
					"&fWindCharges &7at enemies but in return",
					"the ability to jump is greatly reduced."
			},
			"breeze-dash",
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
					"&aCreepers &7have #chance-percent#% chance",
					"to &cexplode &7on death."
			},
			"creeper-revenge",
			new Modifier[]{
					new Modifier("chance-percent", 15.0)
			}
	),

	DANGEROUS_COAL(
			"Dangerous Coal",
			new String[]{
					"Mining &8Coal &7has #chance-percent#% chance",
					"to trigger a gas pocket &cexplosion.",
					"&7&oExplosion radius: #radius# &7&oblocks."
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
					"&cPowered Redstone &7&o(wires, torches,",
					"&7&orepeaters, comparators) &7deals #damage# damage.",
					"Can occur every 3 seconds."
			},
			"electricity-shock",
			new Modifier[]{
					new Modifier("damage", 6.0)
			}
	),

	ENDER_POWER(
			"Ender Power",
			new String[]{
					"&fPlayers &7have #chance-percent#% chance to be blinded for #duration# seconds.",
					"Triggered when hitting &5Endermen&7, &5Endermites&7,",
					"&5Shulkers &7or &5Dragons."
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
					"&eBlazes &7summon fire beams that ignite",
					"players for #burn-duration# seconds."
			},
			"everburning-blazes",
			new Modifier[]{
					new Modifier("burn-duration", 2.0)
			}
	),

	FALL_STUN(
			"Fall Stun",
			new String[]{
					"High falls cause players to be slowed (III)",
					"for a duration. Duration scales with fall height."
			},
			"fall-stun",
			new Modifier[]{
					new Modifier("duration-amplifier", 1)
			}
	),

	FIREWORK_ARROWS(
			"Firework Arrows",
			new String[]{
					"&6Pillager &7have #chance-percent#% chance to shoot a &fFirework &fArrow",
					"instead of regular arrow. Deal #damage# damage per shot in",
					"in #area#x#area# area. Apply shock effect for #duration#s."
			},
			"firework-arrows",
			new Modifier[]{
					new Modifier("chance-percent", 55.0),
					new Modifier("damage", 3),
					new Modifier("area", 3),
					new Modifier("duration", 8)
			}
	),

	FORCE_OF_THE_UNDEAD(
			"Force of the Undead",
			new String[]{
					"&cMonsters &7deal increased attack damage.",
					" ",
					"&cConfigurable for each monster."
			},
			"force-of-the-undead",
			new Modifier[]{
					new Modifier("additional-ad-percent", 25.0, Type.EACH_MOB)
			}
	),

	FREDDY(
			"Freddy",
			new String[]{
					"&fPlayers &7have #chance-percent#% chance",
					"to summon &8Freddy &7upon waking."
			},
			"freddy",
			new Modifier[]{
					new Modifier("chance-percent", 5.0)
			}
	),

	HOMING_FLAME_BARRAGE(
			"Homing Flame Barrage",
			new String[]{
					"&eBlaze &7has a #chance-percent#% chance to fire a #shoot-amount# beam of fire.",
					"It tracks the player and deals #damage# HP each beam",
					"that hits, with a hit rate of almost &c100%&7."
			},
			"homing-flame-barrage",
			new Modifier[]{
					new Modifier("chance-percent", 45),
					new Modifier("shoot-amount", 3),
					new Modifier("damage", 1)
			}
	),

	HUNGER_NAUSEA(
			"Hunger Nausea",
			new String[]{
					"&fPlayers &7get permanent nausea when hungry."
			},
			"hunger-nausea",
			new Modifier[]{}
	),

	IMMORTAL_EVOKER(
			"Immortal Evoker",
			new String[]{
					"&8Evoker &7has a #chance-percent#% chance to use &eTotem &7to",
					"avoid death and gain level #resistance-amplifier# resistance.",
					"After that, every 5 seconds, summon &fFangs",
					"can pull the player into the ground 3 block."
			},
			"immortal-evoker",
			new Modifier[]{
					new Modifier("chance-percent", 75.0),
					new Modifier("resistance-amplifier", 3)
			}
	),

	INFECTION(
			"Infection",
			new String[]{
					"&2Zombies &7have a #chance-percent#% chance to infect players.",
					"&8Infection &7causes nausea and deals #dps#*3 HP every",
					"3 seconds. Can be stopped using a &fStrange Brew&7.",
					"&8Infection &7spreads via bare-hand attacks",
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
					"&8Spiders &7can leap powerfully at players."
			},
			"leaping-spiders",
			new Modifier[]{}
	),

	METEOR_RAIN(
			"Meteor Rain",
			new String[]{
					"&9At night&7, there is a #chance-percent#% chance you will see a &6Meteor &7streaking",
					"rapidly as it enters the atmosphere. It will devastate the terrain,",
					"creating a crater at the impact point. At the center of the crater,",
					"you can find the meteorite remains along with rare minerals."
			},
			"meteor-rain",
			new Modifier[]{

					new Modifier("chance-percent", 1.0),
					new Modifier("coal-ore-rate", 10),
					new Modifier("iron-ore-rate", 20),
					new Modifier("gold-ore-rate", 30),
					new Modifier("diamond-ore-rate", 70),
					new Modifier("emerald-ore-rate", 60),
					new Modifier("ancient-debris-rate", 80),
					new Modifier("meteor-crater", true),
					new Modifier("crater-recovery", true)
			},
			MeteorRain::new
	),

	MOB_CRITICAL_STRIKES(
			"Mob Critical Strikes",
			new String[]{
					"&cMonsters &7can deal critical strikes",
					"with #damage-percent#% additional damage.",
					" ",
					"&cConfigurable for each monster."
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
					"&7All nether mobs &cexcept &7Wither Skeleton",
					"&7have#chance-percent#% chance to block attacks.",
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

	PHANTOM_BLADE(
			"Phantom Blade",
			new String[]{
					"&bPhantoms &7enter invisible state every #invisibility-interval#s for #invisibility-duration#s.",
					"While invisible, they have #shoot-chance#% chance to shoot",
					"&fWind Blades &7dealing #damage# damage and applying",
					"&7weakness for #weakness-duration#s."
			},
			"phantom-blade",
			new Modifier[]{
					new Modifier("invisibility-interval", 5.0),
					new Modifier("invisibility-duration", 10.0),
					new Modifier("shoot-chance", 40.0),
					new Modifier("damage", 1.0),
					new Modifier("weakness-duration", 10.0),
					new Modifier("weakness-amplifier", 1)
			}
	),

	PHYSIC_ENDER_PEARL(
			"Physic EnderPearl",
			new String[]{
					"&2EnderPearl &7will have physical properties",
					"like bounce, friction."
			},
			"physic-ender-pearl",
			new Modifier[]{
					new Modifier("bounciness", 0.85),
					new Modifier("vertical-bounciness", 0.7),
					new Modifier("max-bounces", 5),
					new Modifier("friction", 0.98),
					new Modifier("min-velocity-threshold", 0.03)
			}
	),

	POISONED_SLIMES(
			"Poisoned Slimes",
			new String[]{
					"&aSlimes &7have #chance-percent#% chance",
					"to &apoison &7players for #duration# seconds.",
					"&7&oPoison effect amplifier: #amplifier#&7&o."
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
					"&cMonsters &7have increased movement speed.",
					" ",
					"&cConfigurable for each monster."
			},
			"quick-mobs",
			new Modifier[]{
					new Modifier("additional-ms-percent", 25.0, Type.EACH_MOB)
			}
	),

	REALISTIC_PICKUP(
			"Realistic Pickup",
			new String[]{
					"&fPlayers &7must crouch and look down",
					"to pick up items.",
					"&7&oBriefly slows players on pickup."
			},
			"realistic-pickup",
			new Modifier[]{}
	),

	SHOCKING_SKELETON_ARROWS(
			"Shocking Skeleton Arrows",
			new String[]{
					"&fSkeletons &7have #chance-percent#% chance",
					"to shoot arrows that shock players",
					"for #shock-duration# seconds."
			},
			"shocking-skeleton-arrows",
			new Modifier[]{
					new Modifier("chance-percent", 35),
					new Modifier("shock-duration", 0.75)
			}
	),

	SILVERFISHES_SUMMON(
			"Silverfishes Summon",
			new String[]{
					"&2Zombies &7have #chance-percent#% chance",
					"to summon #min# to #max# &fSilverfish &7on death."
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
					"&fPlayers &7without leather boots",
					"are slowed on snow."
			},
			"snow-slow",
			new Modifier[]{}
	),

	STONE_STIFFNESS(
			"Stone Stiffness",
			new String[]{
					"&fPlayers &7take #damage# damage",
					"when punching stone."
			},
			"stone-stiffness",
			new Modifier[]{
					new Modifier("damage", 1.0)
			}
	),

	STRAY_FROST(
			"Stray Frost",
			new String[]{
					"&3Stray &7have a #chance-percent#% chance",
					"to shoot a &bFrost &7arrow.",
					"The player will freeze in #duration# second."
			},
			"stray-frost",
			new Modifier[]{
					new Modifier("chance-percent", 15),
					new Modifier("duration", 5)
			}
	),

	SPIDER_WEB(
			"Spider Web",
			new String[]{
					"&2Cave spider &7has a #chance-percent#% chance",
					"to shoot a web that traps the player in place.",
					"It only disappears when the player breaks it."
			},
			"spider-web",
			new Modifier[]{
					new Modifier("chance-percent", 15),
					new Modifier("amount-per-shoot", 5)
			}
	),

	TANKY_MONSTERS(
			"Tanky Monsters",
			new String[]{
					"&cMonsters&7 take reduced damage.",
					" ",
					"&cConfigurable for each monster."
			},
			"tanky-monsters",
			new Modifier[]{
					new Modifier("dmg-reduction-percent", 25.0, Type.EACH_MOB)
			}
	),

	THIEF_SLIMES(
			"Thief Slimes",
			new String[]{
					"&aSlimes &7have #chance-percent#% chance",
					"to steal #exp# &aEXP &7when hitting players."
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
					"&8Storms &7have #chance#% chance",
					"to become &8thunderstorms.",
					"&eLightning &7deals #damage-percent#% more &fAoE&7 damage",
					"and strikes more frequently."
			},
			"thunderstorm",
			new Modifier[]{
					new Modifier("chance", 5),
					new Modifier("damage-percent", 125)
			},
			Thunderstorm::new
	),

	TRIDENT_WRATH(
			"Trident's Wrath",
			new String[]{
					"&bDrowned Tridents &7have a #chance-percent#% chance to use their &3Trident",
					"to launch at a player. Deals #damage# HP on hit."
			},
			"trident-wrath",
			new Modifier[]{
					new Modifier("chance-percent", 25),
					new Modifier("duration", 1),
					new Modifier("damage", 4),
					new Modifier("speed", 0.5)
			}
	),

	UNDEAD_GUNNERS(
			"Undead Gunners",
			new String[]{
					"&2Zombies &7named &f'Undead Gunner'&7 cast rockets",
					"dealing #damage# damage (AoE).",
					" ",
					"&e&oYou can create custom Zombie with name by using",
					"&6/sdmob &ecommand."
			},
			"undead-gunners",
			new Modifier[]{
					new Modifier("damage", 7.0),
					new Modifier("block-damage", 3)
			}
	),

	UNDEAD_RAGE(
			"Undead Rage",
			new String[]{
					"&2Zombies&7, &3Drowned&7, &6Husk &7and &2Zombified Piglin &7gain &6Strength",
					"&7and &3Speed II &7for #rage-duration# seconds when damaged."
			},
			"undead-rage",
			new Modifier[]{
					new Modifier("rage-duration", 4.0)
			}
	),

	WITCH_SCROLLS(
			"Witch Scrolls",
			new String[]{
					"&5Witches &7have a #chance-percent#% chance",
					"to block damage with a &fMagic shield.",
					"Witches cast runes dealing #damage# damage,",
					"slowing players (II) for #slow-duration# seconds.",
					"And apply Weakness for #weak-duration$ seconds"
			},
			"witch-scrolls",
			new Modifier[]{
					new Modifier("chance-percent", 40.0),
					new Modifier("damage", 2.5),
					new Modifier("slow-duration", 2.0),
					new Modifier("weak-duration", 4.0)
			}
	),

	WITHER_MACHINEGUN(
			"Wither Machinegun",
			new String[]{
					"&8Wither Skeletons &7throw coal,",
					"each dealing #damage# damage."
			},
			"wither-machinegun",
			new Modifier[]{
					new Modifier("damage", 2)
			}
	),

	WITHER_RUSH(
			"Wither Rush",
			new String[]{
					"&8Wither Skeletons &7blink to players,",
					"dealing #damage# damage."
			},
			"wither-rush",
			new Modifier[]{
					new Modifier("damage", 3.0)
			}
	),

	WHISPERS_OF_THE_DESERT(
			"Whispers of the Desert",
			new String[]{
					"&eIn arid biomes like &6Desert &eand &6Badlands&e,",
					"there is a #chance-percent#% chance for &6Husks",
					"to rise from the sands in waves.",
					"&cWarning: &7Ambushers (#ambush-chance#%) can pull",
					"you down and trap you in the sand."
			},
			"whispers-of-the-desert",
			new Modifier[]{
					new Modifier("chance-percent", 45.0),
					new Modifier("max-mobs", 3),
					new Modifier("ambush-chance", 20.0),
					new Modifier("spawn-particles", true),
					new Modifier("use-block-under-entity-for-particles", true)
			}
	),

	ZOMBIE_TOOLS(
			"Zombie Tools",
			new String[]{
					"&2Zombies &7have a #chance-percent#% chance to spawn with",
					"random tools (Axe, Shovel, Pickaxe).",
					"Tools have varied materials, enchantments,",
					"and a #drop-chance-percent#% chance to drop with random durability.",
					"&5&o(Optional) Recommended to enable together with the",
					"&cZombie Break Block &7feature."
			},
			"zombie-tools",
			new Modifier[]{
					new Modifier("chance-percent", 89.0),
					new Modifier("drop-chance-percent", 15.0),
					new Modifier("netherite-chance", 5.0),
					new Modifier("diamond-chance", 2.0),
					new Modifier("gold-chance", 35.0),
					new Modifier("iron-chance", 50.0),
					new Modifier("wood-chance", 70.0),
					new Modifier("enchantment-chance", 40.0),
					new Modifier("max-enchantments", 3)
			}
	),

	ZOMBIE_BREAK_BLOCK(
			"Zombie Break Block",
			new String[]{
					"&2Zombies &7with an &bAxe&7, &bPickaxe &7or &bShovel&7 in hand",
					"can &cdestroy &7the corresponding block type.",
					"&7&oExample: Zombie Pickaxe can break Stone and",
					"&7&orelated blocks.",
					" ",
					"&e&oYou can create custom Zombie with tools by using",
					"&6/sdmob &ecommand."
			},
			"zombie-break-block",
			new Modifier[]{
					new Modifier("max-target-distance", 150.0),
					new Modifier("drop-blocks", true),
					new  Modifier("drop-remove-interval", 5.0),
					new Modifier("breakable-pickaxe-blocks", String.join(",", new String[]{
							"STONE", "COBBLESTONE", "ANDESITE", "DIORITE", "GRANITE", "TUFF", "DEEPSLATE",
							"COBBLED_DEEPSLATE", "COAL_ORE", "IRON_ORE", "GOLD_ORE", "DIAMOND_ORE", "EMERALD_ORE",
							"LAPIS_ORE", "REDSTONE_ORE", "COPPER_ORE", "NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE",
							"DEEPSLATE_COAL_ORE", "DEEPSLATE_IRON_ORE", "DEEPSLATE_GOLD_ORE", "DEEPSLATE_DIAMOND_ORE",
							"DEEPSLATE_EMERALD_ORE", "DEEPSLATE_LAPIS_ORE", "DEEPSLATE_REDSTONE_ORE",
							"DEEPSLATE_COPPER_ORE", "OBSIDIAN", "STONE_BRICKS", "MOSSY_STONE_BRICKS",
							"CRACKED_STONE_BRICKS", "CHISELED_STONE_BRICKS", "DEEPSLATE_BRICKS",
							"CRACKED_DEEPSLATE_BRICKS", "CHISELED_DEEPSLATE", "POLISHED_DEEPSLATE", "SMOOTH_STONE",
							"SANDSTONE", "RED_SANDSTONE", "CHISELED_SANDSTONE", "SMOOTH_SANDSTONE",
							"CHISELED_RED_SANDSTONE", "SMOOTH_RED_SANDSTONE"
					})),
					new Modifier("breakable-shovel-blocks", String.join(",", new String[]{
							"DIRT", "GRASS_BLOCK", "PODZOL", "MYCELIUM", "DIRT_PATH", "COARSE_DIRT", "ROOTED_DIRT",
							"SAND", "RED_SAND", "GRAVEL", "CLAY", "SOUL_SAND", "SOUL_SOIL", "SNOW", "SNOW_BLOCK",
							"FARMLAND", "MUD", "MUDDY_MANGROVE_ROOTS"
					})),
					new Modifier("breakable-axe-blocks", String.join(",", new String[]{
							"OAK_LOG", "BIRCH_LOG", "SPRUCE_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG",
							"MANGROVE_LOG", "CHERRY_LOG", "STRIPPED_OAK_LOG", "STRIPPED_BIRCH_LOG", "STRIPPED_SPRUCE_LOG",
							"STRIPPED_JUNGLE_LOG", "STRIPPED_ACACIA_LOG", "STRIPPED_DARK_OAK_LOG", "STRIPPED_MANGROVE_LOG",
							"STRIPPED_CHERRY_LOG", "OAK_WOOD", "BIRCH_WOOD", "SPRUCE_WOOD", "JUNGLE_WOOD", "ACACIA_WOOD",
							"DARK_OAK_WOOD", "MANGROVE_WOOD", "CHERRY_WOOD", "STRIPPED_OAK_WOOD", "STRIPPED_BIRCH_WOOD",
							"STRIPPED_SPRUCE_WOOD", "STRIPPED_JUNGLE_WOOD", "STRIPPED_ACACIA_WOOD", "STRIPPED_DARK_OAK_WOOD",
							"STRIPPED_MANGROVE_WOOD", "STRIPPED_CHERRY_WOOD", "OAK_PLANKS", "BIRCH_PLANKS", "SPRUCE_PLANKS",
							"JUNGLE_PLANKS", "ACACIA_PLANKS", "DARK_OAK_PLANKS", "MANGROVE_PLANKS", "CHERRY_PLANKS",
							"CRIMSON_STEM", "WARPED_STEM", "STRIPPED_CRIMSON_STEM", "STRIPPED_WARPED_STEM",
							"CRIMSON_HYPHAE", "WARPED_HYPHAE", "STRIPPED_CRIMSON_HYPHAE", "STRIPPED_WARPED_HYPHAE",
							"CRIMSON_PLANKS", "WARPED_PLANKS"
					}))
			}
	);

	private String name;
	private List<String> lore;
	private final String path;
	private final List<Modifier> modifiers;
	private final Function<World, WorldEventHandler> event;
	private ConfigFile configFile;

	Feature(String defaultName, String[] defaultLore, String path, Modifier[] modifiers) {
		this(defaultName, defaultLore, path, modifiers, null);
	}

	Feature(String defaultName, String[] defaultLore, String path, Modifier[] modifiers, Function<World, WorldEventHandler> event) {
		this.path = path;
		this.modifiers = Collections.unmodifiableList(Arrays.asList(modifiers));
		this.event = event;
		loadDescriptions(defaultName, defaultLore);
	}

	private void loadDescriptions(String defaultName, String[] defaultLore) {
		try {
			if (SuddenDeath.getInstance() != null && SuddenDeath.getInstance().getConfigManager() != null) {
				FileConfiguration featureConfig = SuddenDeath.getInstance().getConfigManager().features.getConfig();
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
			} else {
				this.name = defaultName;
				this.lore = Collections.unmodifiableList(Arrays.asList(defaultLore));
			}
		} catch (Exception e) {
			if (SuddenDeath.getInstance() != null) {
				SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Could not load descriptions for feature " + path, e);
			}
			this.name = defaultName;
			this.lore = Collections.unmodifiableList(Arrays.asList(defaultLore));
		}
	}

	public static void reloadDescriptions() {
		for (Feature feature : values()) {
			feature.loadDescriptions(feature.name, feature.lore != null ? feature.lore.toArray(new String[0]) : new String[0]);
		}
	}

	public String getName() {
		return name;
	}

	public List<String> getLore() {
		return lore;
	}

	public String getPath() {
		return path;
	}

	public void updateConfig() {
		try {
			configFile = new ConfigFile("/modifiers", path);
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Unable to initialize configuration for feature: " + name, e);
		}
	}

	public ConfigFile getConfigFile() {
		if (configFile == null) {
			updateConfig();
		}
		return configFile;
	}

	public boolean getBoolean(String path) {
		return getConfigFile().getConfig().getBoolean(path, false);
	}

	public double getDouble(String path) {
		return getConfigFile().getConfig().getDouble(path, 0.0);
	}

	public String getString(String path) {
		return getConfigFile().getConfig().getString(path, "");
	}

	public List<Modifier> getModifiers() {
		return modifiers;
	}

	public boolean isEvent() {
		return event != null;
	}

	public StatusRetriever generateWorldEventHandler(World world) {
		if (isEvent()) {
			WorldEventHandler handler = event.apply(world);
			return handler;
		}
		return null;
	}

	public boolean isEnabled(Entity entity) {
		return isEnabled(entity.getWorld());
	}

	public boolean isEnabled(World world) {
		try {
			List<String> enabledWorlds = SuddenDeath.getInstance().getConfigManager().getMainConfig().getConfig().getStringList(path);
			return enabledWorlds.contains(world.getName());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING, "Error while checking feature " + name + " the world " + world.getName(), e);
			return false;
		}
	}
}