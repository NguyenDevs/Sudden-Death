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
			new String[] {
					"&8◆ &3The Guardian &7has a &f#chance-percent#% &7chance to create a &fWhirlpool",
					"&8◆ &7Pulls players toward it, preventing escape"
			},
			"abyssal-vortex",
			new Modifier[] {
					new Modifier("chance-percent", 30),
					new Modifier("duration", 2),
					new Modifier("strength", 0.55)
			}),

	ADVANCED_PLAYER_DROPS(
			"Advanced Player Drops",
			new String[] {
					"&8◆ &fPlayers &7drop a &fSkull&7, &fBones &7and &fHuman Flesh &7on death",
					"&8◆ &7Human Flesh can be &ecooked"
			},
			"advanced-player-drops",
			new Modifier[] {
					new Modifier("drop-skull", true),
					new Modifier("player-skull", true),
					new Modifier("dropped-flesh", 2),
					new Modifier("dropped-bones", 2)
			}),

	ANGRY_SPIDERS(
			"Angry Spiders",
			new String[] {
					"&8◆ &8Spiders &7throw cobwebs, dealing &c#damage# &7damage",
					"&8◆ &7Slows players &8(#amplifier#) &7for &e#duration# &7seconds"
			},
			"angry-spiders",
			new Modifier[] {
					new Modifier("damage", 4),
					new Modifier("duration", 3.0),
					new Modifier("amplifier", 1.0)
			}),

	ARMOR_PIERCING(
			"Armor Piercing",
			new String[] {
					"&8◆ &cMonsters &7have a chance to deal &4True Damage",
					"&8◆ &7Completely ignores player armor",
					"&8◆ &7Chance is configurable per monster"
			},
			"armor-piercing",
			new Modifier[] {
					new Modifier("chance-percent", 15.0, Type.EACH_MOB),
					new Modifier("visual-particles", true),
					new Modifier("visual-sound", true)
			}),

	ARROW_SLOW(
			"Arrow Slow",
			new String[] {
					"&8◆ &fPlayers &7hit by arrows are &9Slowed",
					"&8◆ &7Effect lasts &e#slow-duration# &7seconds"
			},
			"arrow-slow",
			new Modifier[] {
					new Modifier("slow-duration", 1.5)
			}),

	BLEEDING(
			"Bleeding",
			new String[] {
					"&8◆ &fPlayers &7have a &f#chance-percent#% &7chance to &cBleed &7when damaged",
					"&8◆ &7Disables health saturation regeneration",
					"&8◆ &7Deals &c#dps#×3 HP &7every 3 seconds",
					"&8◆ &7Stop with a &fBandage&7, or auto-stops after &e#auto-stop-bleed-time# &7seconds"
			},
			"bleeding",
			new Modifier[] {
					new Modifier("dps", 0.3),
					new Modifier("chance-percent", 10.0),
					new Modifier("health-min", 0),
					new Modifier("auto-stop-bleed-time", 30),
					new Modifier("tug", true)
			}),

	BLOOD_MOON(
			"Blood Moon",
			new String[] {
					"&8◆ &9Night &7has &f#chance#% &7chance to turn &cred",
					"&8◆ &fPlayers &7take &c#damage-percent#% &7more damage and deal &c#weakness-percent#% &7less damage",
					"&8◆ &fPlayers &7are stunned every &e3 seconds &7for &e6 seconds",
					"&8◆ &cMonsters &7spawn with &3Speed #speed#&7, &6Strength #increase-damage# &7and &8Resistance #damage-resistance#",
					"&8◆ &8Skeleton &7arrows mark players, dealing &cInstant Damage II &7after 3s &8(#skeleton-mark-chance#%)",
					"&8◆ &aCreepers &7gain extra speed, &cx#creeper-explosion-multiplier# explosion&7, leaving a crater of magma & fire",
					"&8◆ &2Zombies &7have &f#zombie-tool-bonus-percent#% &7extra tool-spawn rate"
			},
			"blood-moon",
			new Modifier[] {
					new Modifier("chance", 2),
					new Modifier("damage-percent", 60),
					new Modifier("weakness-percent", 40),
					new Modifier("speed", 2),
					new Modifier("increase-damage", 2),
					new Modifier("damage-resistance", 2),
					new Modifier("zombie-tool-bonus-percent", 30),
					new Modifier("skeleton-mark-chance", 50),
					new Modifier("creeper-explosion-multiplier", 1.5)
			},
			BloodMoon::new),

	BLOOD_SCREEN(
			"Blood Screen",
			new String[] {
					"&8◆ &fPlayers &7show a &cbleeding &7visual effect when taking damage",
					"&8◆ &7Red screen overlay appears on hit"
			},
			"blood-screen",
			new Modifier[] {
					new Modifier("mode", "HEALTH"),
					new Modifier("interval", 6),
					new Modifier("coefficient", 0.95)
			}),

	BONE_GRENADES(
			"Bone Grenades",
			new String[] {
					"&8◆ &fSkeletons &7have a &f#chance-percent#% &7chance to throw &8Bone Grenades",
					"&8◆ &7Grenades explode, dealing &c#damage# &7damage",
					"&8◆ &7Knocks back nearby players"
			},
			"bone-grenades",
			new Modifier[] {
					new Modifier("chance-percent", 25.0),
					new Modifier("damage", 6.0)
			}),

	BONE_WIZARDS(
			"Bone Wizards",
			new String[] {
					"&8◆ &fSkeletons &7named &f'Bone Wizard' &7cast the following spells:",
					"&8◆ &c[Fireball] &7Deals #fireball-damage# damage, burns for #fireball-duration#s",
					"&8◆ &3[Frost Curse] &7Deals #frost-curse-damage# damage, slows (#frost-curse-amplifier#) for #frost-curse-duration#s",
					"&8◆ &e&oCreate custom Skeletons via &6/sdmob"
			},
			"bone-wizards",
			new Modifier[] {
					new Modifier("fireball-damage", 7.0),
					new Modifier("fireball-duration", 3.0),
					new Modifier("frost-curse-damage", 6.0),
					new Modifier("frost-curse-duration", 2.0),
					new Modifier("frost-curse-amplifier", 1)
			}),

	BREEZE_DASH(
			"Breeze Dash",
			new String[] {
					"&8◆ &3Breeze &7has a &f#chance-percent#% &7chance to dash at high speed",
					"&8◆ &7Continuously fires &f#shoot-amount# Wind Charges &7during the dash",
					"&8◆ &7Jump ability is greatly reduced while dashing"
			},
			"breeze-dash",
			new Modifier[] {
					new Modifier("chance-percent", 20),
					new Modifier("shoot-amount", 5),
					new Modifier("amplifier", 2),
					new Modifier("duration", 10)
			}),

	CREEPER_REVENGE(
			"Creeper Revenge",
			new String[] {
					"&8◆ &aCreepers &7have a &f#chance-percent#% &7chance to &cexplode &7on death"
			},
			"creeper-revenge",
			new Modifier[] {
					new Modifier("chance-percent", 15.0)
			}),

	DANGEROUS_COAL(
			"Dangerous Coal",
			new String[] {
					"&8◆ &7Mining &8Coal &7has a &f#chance-percent#% &7chance to trigger a gas pocket &cexplosion",
					"&8◆ &7Explosion radius: &c#radius# &7blocks"
			},
			"dangerous-coal",
			new Modifier[] {
					new Modifier("chance-percent", 5.0),
					new Modifier("radius", 5.0)
			}),

	ELECTRICITY_SHOCK(
			"Electricity Shock",
			new String[] {
					"&8◆ &cPowered Redstone &7(wires, torches, repeaters, comparators) deals &c#damage# &7damage",
					"&8◆ &7Can trigger once every &e3 seconds"
			},
			"electricity-shock",
			new Modifier[] {
					new Modifier("damage", 6.0)
			}),

	ENDER_POWER(
			"Ender Power",
			new String[] {
					"&8◆ &fPlayers &7have a &f#chance-percent#% &7chance to be &8Blinded &7for &e#duration# &7seconds",
					"&8◆ &7Triggered when hitting &5Endermen&7, &5Endermites&7, &5Shulkers &7or &5Dragons"
			},
			"ender-power",
			new Modifier[] {
					new Modifier("chance-percent", 70),
					new Modifier("duration", 6.0)
			}),

	EVERBURNING_BLAZES(
			"Everburning Blazes",
			new String[] {
					"&8◆ &eBlazes &7summon fire beams targeting players",
					"&8◆ &7Ignites target for &c#burn-duration# &7seconds"
			},
			"everburning-blazes",
			new Modifier[] {
					new Modifier("burn-duration", 2.0)
			}),

	FALL_STUN(
			"Fall Stun",
			new String[] {
					"&8◆ &fPlayers &7who fall from great heights are &9Slowed III",
					"&8◆ &7Slow duration scales with fall height"
			},
			"fall-stun",
			new Modifier[] {
					new Modifier("duration-amplifier", 1)
			}),

	FIREWORK_ARROWS(
			"Firework Arrows",
			new String[] {
					"&8◆ &6Pillagers &7have a &f#chance-percent#% &7chance to shoot a &cFirework Arrow",
					"&8◆ &7Deals &c#damage# &7damage per shot in a &f#area#x#area# &7area",
					"&8◆ &7Applies shock effect for &e#duration# &7seconds"
			},
			"firework-arrows",
			new Modifier[] {
					new Modifier("chance-percent", 55.0),
					new Modifier("damage", 3),
					new Modifier("area", 3),
					new Modifier("duration", 8)
			}),

	FORCE_OF_THE_UNDEAD(
			"Force of the Undead",
			new String[] {
					"&8◆ &cMonsters &7deal increased attack damage",
					"&8◆ &7Damage bonus is configurable per monster"
			},
			"force-of-the-undead",
			new Modifier[] {
					new Modifier("additional-ad-percent", 25.0, Type.EACH_MOB)
			}),

	FREDDY(
			"Freddy",
			new String[] {
					"&8◆ &fPlayers &7have a &f#chance-percent#% &7chance to summon &8Freddy &7upon waking"
			},
			"freddy",
			new Modifier[] {
					new Modifier("chance-percent", 5.0)
			}),

	HOMING_FLAME_BARRAGE(
			"Homing Flame Barrage",
			new String[] {
					"&8◆ &eBlaze &7has a &f#chance-percent#% &7chance to fire &f#shoot-amount# &7homing fire beams",
					"&8◆ &7Each beam that hits deals &c#damage# HP",
					"&8◆ &7Tracking accuracy is nearly &c100%"
			},
			"homing-flame-barrage",
			new Modifier[] {
					new Modifier("chance-percent", 45),
					new Modifier("shoot-amount", 3),
					new Modifier("damage", 1)
			}),

	HUNGER_NAUSEA(
			"Hunger Nausea",
			new String[] {
					"&8◆ &fPlayers &7suffer permanent &5Nausea &7while hungry"
			},
			"hunger-nausea",
			new Modifier[] {}),

	IMMORTAL_EVOKER(
			"Immortal Evoker",
			new String[] {
					"&8◆ &8Evoker &7has a &f#chance-percent#% &7chance to use a &eTotem &7to avoid death",
					"&8◆ &7Gains level &f#resistance-amplifier# Resistance &7after revival",
					"&8◆ &7Summons &fFangs &7every 5 seconds that pull players 3 blocks underground"
			},
			"immortal-evoker",
			new Modifier[] {
					new Modifier("chance-percent", 75.0),
					new Modifier("resistance-amplifier", 3)
			}),

	INFECTION(
			"Infection",
			new String[] {
					"&8◆ &2Zombies &7have a &f#chance-percent#% &7chance to infect players",
					"&8◆ &8Infection &7causes nausea and deals &c#dps#×3 HP &7every 3 seconds",
					"&8◆ &7Cured by using a &fStrange Brew",
					"&8◆ &7Spreads via bare-hand attacks or player-to-player contact"
			},
			"infection",
			new Modifier[] {
					new Modifier("dps", 0.3),
					new Modifier("chance-percent", 15.0),
					new Modifier("health-min", 0),
					new Modifier("tug", true),
					new Modifier("sound", true)
			}),

	LEAPING_SPIDERS(
			"Leaping Spiders",
			new String[] {
					"&8◆ &8Spiders &7can leap powerfully at players"
			},
			"leaping-spiders",
			new Modifier[] {}),

	METEOR_RAIN(
			"Meteor Rain",
			new String[] {
					"&8◆ &9At night&7, there is a &f#chance-percent#% &7chance for a &6Meteor &7to streak across the sky",
					"&8◆ &7Impact devastates the terrain, creating a crater",
					"&8◆ &7Meteorite remains and rare minerals can be found at the crater center"
			},
			"meteor-rain",
			new Modifier[] {
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
			MeteorRain::new),

	MOB_CRITICAL_STRIKES(
			"Mob Critical Strikes",
			new String[] {
					"&8◆ &cMonsters &7can deal critical strikes with &f#damage-percent#% &7bonus damage",
					"&8◆ &7Crit chance is configurable per monster"
			},
			"mob-critical-strikes",
			new Modifier[] {
					new Modifier("crit-chance", 17, Type.EACH_MOB),
					new Modifier("damage-percent", 75.0)
			}),

	NETHER_SHIELD(
			"Nether Shield",
			new String[] {
					"&8◆ &7All Nether mobs &c(except Wither Skeletons) &7have a &f#chance-percent#% &7chance to block attacks",
					"&8◆ &7Reflects &c#dmg-reflection-percent#% &7of damage back to attacker",
					"&8◆ &7Ignites attacker for &c#burn-duration# &7seconds"
			},
			"nether-shield",
			new Modifier[] {
					new Modifier("chance-percent", 37.5),
					new Modifier("dmg-reflection-percent", 75.0),
					new Modifier("burn-duration", 3.0)
			}),

	PHANTOM_BLADE(
			"Phantom Blade",
			new String[] {
					"&8◆ &bPhantoms &7enter an invisible state every &e#invisibility-interval#s &7for &e#invisibility-duration#s",
					"&8◆ &7While invisible, &f#shoot-chance#% &7chance to shoot &fWind Blades",
					"&8◆ &7Each blade deals &c#damage# &7damage and applies &7Weakness for &e#weakness-duration#s"
			},
			"phantom-blade",
			new Modifier[] {
					new Modifier("invisibility-interval", 5.0),
					new Modifier("invisibility-duration", 10.0),
					new Modifier("shoot-chance", 40.0),
					new Modifier("damage", 1.0),
					new Modifier("weakness-duration", 10.0),
					new Modifier("weakness-amplifier", 1)
			}),

	PHYSIC_ENDER_PEARL(
			"Physic EnderPearl",
			new String[] {
					"&8◆ &2EnderPearls &7have physical properties like bounce and friction"
			},
			"physic-ender-pearl",
			new Modifier[] {
					new Modifier("bounciness", 0.85),
					new Modifier("vertical-bounciness", 0.7),
					new Modifier("max-bounces", 5),
					new Modifier("friction", 0.98),
					new Modifier("min-velocity-threshold", 0.03)
			}),

	POISONED_SLIMES(
			"Poisoned Slimes",
			new String[] {
					"&8◆ &aSlimes &7have a &f#chance-percent#% &7chance to &apoison &7players",
					"&8◆ &7Poison lasts &e#duration# &7seconds at amplifier &f#amplifier#"
			},
			"poisoned-slimes",
			new Modifier[] {
					new Modifier("amplifier", 1),
					new Modifier("duration", 3.0),
					new Modifier("chance-percent", 65.0)
			}),

	QUICK_MOBS(
			"Quick Mobs",
			new String[] {
					"&8◆ &cMonsters &7have increased movement speed",
					"&8◆ &7Speed bonus is configurable per monster"
			},
			"quick-mobs",
			new Modifier[] {
					new Modifier("additional-ms-percent", 25.0, Type.EACH_MOB)
			}),

	REALISTIC_PICKUP(
			"Realistic Pickup",
			new String[] {
					"&8◆ &fPlayers &7must crouch and look down to pick up items",
					"&8◆ &7Briefly slows players on item pickup"
			},
			"realistic-pickup",
			new Modifier[] {}),

	SHOCKING_SKELETON_ARROWS(
			"Shocking Skeleton Arrows",
			new String[] {
					"&8◆ &fSkeletons &7have a &f#chance-percent#% &7chance to shoot shocking arrows",
					"&8◆ &7Shock effect lasts &e#shock-duration# &7seconds"
			},
			"shocking-skeleton-arrows",
			new Modifier[] {
					new Modifier("chance-percent", 35),
					new Modifier("shock-duration", 0.75)
			}),

	SILVERFISHES_SUMMON(
			"Silverfishes Summon",
			new String[] {
					"&8◆ &2Zombies &7have a &f#chance-percent#% &7chance to summon &fSilverfish &7on death",
					"&8◆ &7Spawns &f#min# &7to &f#max# &7Silverfish"
			},
			"silverfishes-summon",
			new Modifier[] {
					new Modifier("chance-percent", 35.0),
					new Modifier("min", 1),
					new Modifier("max", 2)
			}),

	SNOW_SLOW(
			"Snow Slow",
			new String[] {
					"&8◆ &fPlayers &7without leather boots are &9Slowed &7when walking on snow"
			},
			"snow-slow",
			new Modifier[] {}),

	STONE_STIFFNESS(
			"Stone Stiffness",
			new String[] {
					"&8◆ &fPlayers &7take &c#damage# &7damage when punching stone"
			},
			"stone-stiffness",
			new Modifier[] {
					new Modifier("damage", 1.0)
			}),

	STRAY_FROST(
			"Stray Frost",
			new String[] {
					"&8◆ &3Stray &7has a &f#chance-percent#% &7chance to shoot a &bFrost Arrow",
					"&8◆ &7Freezes the player for &e#duration# &7seconds"
			},
			"stray-frost",
			new Modifier[] {
					new Modifier("chance-percent", 15),
					new Modifier("duration", 5)
			}),

	SPIDER_WEB(
			"Spider Web",
			new String[] {
					"&8◆ &2Cave Spiders &7have a &f#chance-percent#% &7chance to shoot a web",
					"&8◆ &7Web traps the player in place until broken"
			},
			"spider-web",
			new Modifier[] {
					new Modifier("chance-percent", 15),
					new Modifier("amount-per-shoot", 5)
			}),

	SPIDER_NEST(
			"Spider Nest",
			new String[] {
					"&8◆ &8Breaking &7a &fCobweb &7has a &f#chance-percent#% &7chance to spawn spiders",
					"&8◆ &7Spawns &f#min# &7to &f#max# &8Spiders &7or &8Cave Spiders &7at the broken block"
			},
			"spider-nest",
			new Modifier[] {
					new Modifier("chance-percent", 35.0),
					new Modifier("min", 1),
					new Modifier("max", 3),
					new Modifier("cave-spider-chance", 50.0)
			}),

	TANKY_MONSTERS(
			"Tanky Monsters",
			new String[] {
					"&8◆ &cMonsters &7take reduced damage from all sources",
					"&8◆ &7Damage reduction is configurable per monster"
			},
			"tanky-monsters",
			new Modifier[] {
					new Modifier("dmg-reduction-percent", 25.0, Type.EACH_MOB)
			}),

	THIEF_SLIMES(
			"Thief Slimes",
			new String[] {
					"&8◆ &aSlimes &7have a &f#chance-percent#% &7chance to steal &f#exp# &aEXP &7on hit"
			},
			"thief-slimes",
			new Modifier[] {
					new Modifier("chance-percent", 55),
					new Modifier("exp", 12)
			}),

	THUNDERSTORM(
			"Thunderstorm",
			new String[] {
					"&8◆ &8Storms &7have a &f#chance#% &7chance to become &8Thunderstorms",
					"&8◆ &eLightning &7deals &f#damage-percent#% &7more AoE damage",
					"&8◆ &7Lightning strikes more frequently than normal"
			},
			"thunderstorm",
			new Modifier[] {
					new Modifier("chance", 5),
					new Modifier("damage-percent", 125)
			},
			Thunderstorm::new),

	TRIDENT_WRATH(
			"Trident's Wrath",
			new String[] {
					"&8◆ &bDrowned &7have a &f#chance-percent#% &7chance to launch their &3Trident &7at players",
					"&8◆ &7Deals &c#damage# HP &7on direct hit"
			},
			"trident-wrath",
			new Modifier[] {
					new Modifier("chance-percent", 25),
					new Modifier("duration", 1),
					new Modifier("damage", 4),
					new Modifier("speed", 0.5)
			}),

	UNDEAD_GUNNERS(
			"Undead Gunners",
			new String[] {
					"&8◆ &2Zombies &7named &f'Undead Gunner' &7fire rockets dealing &c#damage# &7AoE damage",
					"&8◆ &e&oCreate custom Zombies via &6/sdmob"
			},
			"undead-gunners",
			new Modifier[] {
					new Modifier("damage", 7.0),
					new Modifier("block-damage", 3)
			}),

	UNDEAD_RAGE(
			"Undead Rage",
			new String[] {
					"&8◆ &2Zombies&7, &3Drowned&7, &6Husks &7and &2Zombified Piglins &7enter a rage when damaged",
					"&8◆ &7Gain &6Strength &7and &3Speed II &7for &e#rage-duration# &7seconds"
			},
			"undead-rage",
			new Modifier[] {
					new Modifier("rage-duration", 4.0)
			}),

	WITCH_SCROLLS(
			"Witch Scrolls",
			new String[] {
					"&8◆ &5Witches &7have a &f#chance-percent#% &7chance to block damage with a &fMagic Shield",
					"&8◆ &7Cast runes dealing &c#damage# &7damage and applying &9Slow II &7for &e#slow-duration#s",
					"&8◆ &7Also applies Weakness for &e#weak-duration# &7seconds"
			},
			"witch-scrolls",
			new Modifier[] {
					new Modifier("chance-percent", 40.0),
					new Modifier("damage", 2.5),
					new Modifier("slow-duration", 2.0),
					new Modifier("weak-duration", 4.0)
			}),

	WITHER_MACHINEGUN(
			"Wither Machinegun",
			new String[] {
					"&8◆ &8Wither Skeletons &7rapidly throw coal at players",
					"&8◆ &7Each piece of coal deals &c#damage# &7damage"
			},
			"wither-machinegun",
			new Modifier[] {
					new Modifier("damage", 2)
			}),

	WITHER_RUSH(
			"Wither Rush",
			new String[] {
					"&8◆ &8Wither Skeletons &7blink to the player's location",
					"&8◆ &7Deals &c#damage# &7damage on arrival"
			},
			"wither-rush",
			new Modifier[] {
					new Modifier("damage", 3.0)
			}),

	WHISPERS_OF_THE_DESERT(
			"Whispers of the Desert",
			new String[] {
					"&8◆ &7In &6Desert &7and &6Badlands &7biomes, &6Husks &7rise from the sands in waves &f(#chance-percent#%)",
					"&8◆ &cAmbushers &7(#ambush-chance#%) can pull players underground and trap them in sand"
			},
			"whispers-of-the-desert",
			new Modifier[] {
					new Modifier("chance-percent", 45.0),
					new Modifier("max-mobs", 3),
					new Modifier("ambush-chance", 20.0),
					new Modifier("spawn-particles", true),
					new Modifier("use-block-under-entity-for-particles", true)
			}),

	ZOMBIE_TOOLS(
			"Zombie Tools",
			new String[] {
					"&8◆ &2Zombies &7have a &f#chance-percent#% &7chance to spawn with a random tool (Axe, Shovel, Pickaxe)",
					"&8◆ &7Tools have varied materials, enchantments and random durability",
					"&8◆ &7Tools have a &f#drop-chance-percent#% &7chance to drop on death",
					"&8◆ &5&oRecommended to enable alongside &cZombie Break Block"
			},
			"zombie-tools",
			new Modifier[] {
					new Modifier("chance-percent", 89.0),
					new Modifier("drop-chance-percent", 15.0),
					new Modifier("netherite-chance", 5.0),
					new Modifier("diamond-chance", 2.0),
					new Modifier("gold-chance", 35.0),
					new Modifier("iron-chance", 50.0),
					new Modifier("wood-chance", 70.0),
					new Modifier("enchantment-chance", 40.0),
					new Modifier("max-enchantments", 3)
			}),

	ZOMBIE_BREAK_BLOCK(
			"Zombie Break Block",
			new String[] {
					"&8◆ &2Zombies &7holding an &bAxe&7, &bPickaxe &7or &bShovel &7can &cdestroy &7corresponding block types",
					"&8◆ &7Example: Zombie with Pickaxe can break Stone and related blocks",
					"&8◆ &e&oCreate custom tool-Zombies via &6/sdmob"
			},
			"zombie-break-block",
			new Modifier[] {
					new Modifier("max-target-distance", 150.0),
					new Modifier("drop-blocks", true),
					new Modifier("drop-remove-interval", 15.0),
					new Modifier("breakable-pickaxe-blocks", String.join(",", new String[] {
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
					new Modifier("breakable-shovel-blocks", String.join(",", new String[] {
							"DIRT", "GRASS_BLOCK", "PODZOL", "MYCELIUM", "DIRT_PATH", "COARSE_DIRT", "ROOTED_DIRT",
							"SAND", "RED_SAND", "GRAVEL", "CLAY", "SOUL_SAND", "SOUL_SOIL", "SNOW", "SNOW_BLOCK",
							"FARMLAND", "MUD", "MUDDY_MANGROVE_ROOTS"
					})),
					new Modifier("breakable-axe-blocks", String.join(",", new String[] {
							"OAK_LOG", "BIRCH_LOG", "SPRUCE_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG",
							"MANGROVE_LOG", "CHERRY_LOG", "STRIPPED_OAK_LOG", "STRIPPED_BIRCH_LOG",
							"STRIPPED_SPRUCE_LOG",
							"STRIPPED_JUNGLE_LOG", "STRIPPED_ACACIA_LOG", "STRIPPED_DARK_OAK_LOG",
							"STRIPPED_MANGROVE_LOG",
							"STRIPPED_CHERRY_LOG", "OAK_WOOD", "BIRCH_WOOD", "SPRUCE_WOOD", "JUNGLE_WOOD",
							"ACACIA_WOOD",
							"DARK_OAK_WOOD", "MANGROVE_WOOD", "CHERRY_WOOD", "STRIPPED_OAK_WOOD", "STRIPPED_BIRCH_WOOD",
							"STRIPPED_SPRUCE_WOOD", "STRIPPED_JUNGLE_WOOD", "STRIPPED_ACACIA_WOOD",
							"STRIPPED_DARK_OAK_WOOD",
							"STRIPPED_MANGROVE_WOOD", "STRIPPED_CHERRY_WOOD", "OAK_PLANKS", "BIRCH_PLANKS",
							"SPRUCE_PLANKS",
							"JUNGLE_PLANKS", "ACACIA_PLANKS", "DARK_OAK_PLANKS", "MANGROVE_PLANKS", "CHERRY_PLANKS",
							"CRIMSON_STEM", "WARPED_STEM", "STRIPPED_CRIMSON_STEM", "STRIPPED_WARPED_STEM",
							"CRIMSON_HYPHAE", "WARPED_HYPHAE", "STRIPPED_CRIMSON_HYPHAE", "STRIPPED_WARPED_HYPHAE",
							"CRIMSON_PLANKS", "WARPED_PLANKS"
					}))
			});

	private String name;
	private List<String> lore;
	private final String path;
	private final List<Modifier> modifiers;
	private final Function<World, WorldEventHandler> event;
	private ConfigFile configFile;

	Feature(String defaultName, String[] defaultLore, String path, Modifier[] modifiers) {
		this(defaultName, defaultLore, path, modifiers, null);
	}

	Feature(String defaultName, String[] defaultLore, String path, Modifier[] modifiers,
			Function<World, WorldEventHandler> event) {
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
				SuddenDeath.getInstance().getLogger().log(Level.WARNING,
						"Could not load descriptions for feature " + path, e);
			}
			this.name = defaultName;
			this.lore = Collections.unmodifiableList(Arrays.asList(defaultLore));
		}
	}

	public static void reloadDescriptions() {
		for (Feature feature : values()) {
			feature.loadDescriptions(feature.name,
					feature.lore != null ? feature.lore.toArray(new String[0]) : new String[0]);
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
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Unable to initialize configuration for feature: " + name, e);
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
			List<String> enabledWorlds = SuddenDeath.getInstance().getConfigManager().getMainConfig().getConfig()
					.getStringList(path);
			return enabledWorlds.contains(world.getName());
		} catch (Exception e) {
			SuddenDeath.getInstance().getLogger().log(Level.WARNING,
					"Error while checking feature " + name + " the world " + world.getName(), e);
			return false;
		}
	}
}