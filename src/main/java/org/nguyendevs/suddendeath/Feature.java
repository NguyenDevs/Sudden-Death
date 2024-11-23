package org.nguyendevs.suddendeath;

import org.nguyendevs.suddendeath.player.Modifier;
import org.nguyendevs.suddendeath.player.Modifier.Type;
import org.nguyendevs.suddendeath.util.ConfigFile;
import org.nguyendevs.suddendeath.world.BloodMoon;
import org.nguyendevs.suddendeath.world.Thunderstorm;
import org.nguyendevs.suddendeath.world.WorldEventHandler;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public enum Feature {
	//ZOMBIE_BREAKABLE("Zombie Breakable", new String[]{"Zombie can break or place block to get to player"},"zombie-breakable", new Modifier[]{new Modifier("chance",25)}),
	QUICK_MOBS("Quick Mobs", new String[] { "Monsters have more movement speed.", "&cConfigurable for each monster.", "&cNot supported in 1.8" }, "quick-mobs", new Modifier[] { new Modifier("additional-ms-percent", 25.0, Type.EACH_MOB) }),
	FORCE_OF_THE_UNDEAD("Force of the Undead", new String[] { "Monsters have more attack damage.", "&cConfigurable for each monster.", "&cNot supported in 1.8" }, "force-of-the-undead", new Modifier[] { new Modifier("additional-ad-percent", 25.0, Type.EACH_MOB) }),
	TANKY_MONSTERS("Tanky Monsters", new String[] { "Monsters take less damage.", "&cConfigurable for each monster." }, "tanky-monsters", new Modifier[] { new Modifier("dmg-reduction-percent", 25.0, Type.EACH_MOB) }),
	BLEEDING("Bleeding", new String[] { "Players have a #chance-percent#% chance to bleed when being damaged.", "Bleeding disables health saturation regen.", "Also deals #damage# every 3 seconds.", "Can be stopped by using a bandage." }, "bleeding", new Modifier[] { new Modifier("dps", .1), new Modifier("chance-percent", 10.0), new Modifier("health-min", 0), new Modifier("tug", true),new Modifier("auto-stop-bleed-time", 120.0) }),
	INFECTION("Infection", new String[] { "Zombies have a #chance-percent#% chance to infect players.", "Gives nausea and deals #damage# damage every 3 seconds.", "Can be stoped using a Strange Brew.", "Players also get infected when hitting a zombie bare hands.", "Players can infect other players when hitting them." }, "infection", new Modifier[] { new Modifier("dps", .1), new Modifier("chance-percent", 15.0), new Modifier("health-min", 0), new Modifier("tug", true), new Modifier("sound", true) }),
	ARROW_SLOW("Arrow Slow", new String[] { "Players hit by arrows get", "slowed down for #slow-duration# seconds." }, "arrow-slow", new Modifier[] { new Modifier("slow-duration", 3.0) }),
	SHOCKING_SKELETON_ARROWS("Shocking Skeleton Arrows", new String[] { "Skeleton arrows shock", "players for #shock-duration# seconds." }, "shocking-skeleton-arrows", new Modifier[] { new Modifier("shock-duration", 2.0) }),
	SILVERFISHES_SUMMON("Silverfishes Summon", new String[] { "Zombies have #chance-percent#% chance to summon", "from #min# to #max# silverfishes when dying." }, "silverfishes-summon", new Modifier[] { new Modifier("chance-percent", 35.0), new Modifier("min", 1), new Modifier("max", 2) }),
	ARMOR_WEIGHT("Armor Weight", new String[] { "Players walk slower when wearing", "iron, gold or diamond armor sets.", "Movement Speed Malus: #movement-speed-malus#%", }, "armor-weight", new Modifier[] { new Modifier("movement-speed-malus", 3) }),
	HUNGER_NAUSEA("Hunger Nausea", new String[] { "Players get permanent nausea when hungry." }, "hunger-nausea", new Modifier[] {}),
	CREEPER_REVENGE("Creeper Revenge", new String[] { "Creepers have a #chance-percent#% chance to", "cause an explosion when dying." }, "creeper-revenge", new Modifier[] { new Modifier("chance-percent", 15.0) }),
	FALL_STUN("Fall Stun", new String[] { "High falls cause players to be highly", "slowed (III) for a few seconds. The", "higher the fall was, the longer it will last." }, "fall-stun", new Modifier[] { new Modifier("duration-amplifier", 1) }),
	LEAPING_SPIDERS("Leaping Spiders", new String[] { "Spiders can powerfuly leap on players." }, "leaping-spiders", new Modifier[] {}),
	ADVANCED_PLAYER_DROPS("Advanced Player Drops", new String[] { "Players drop a skull, some bones and human", "flesh when dying. Human flesh can be cooked." }, "advanced-player-drops", new Modifier[] { new Modifier("drop-skull", true), new Modifier("player-skull", true), new Modifier("dropped-flesh", 2), new Modifier("dropped-bones", 2) }),
	STONE_STIFFNESS("Stone Stiffness", new String[] { "Players take #damage# damage when punching stone." }, "stone-stiffness", new Modifier[] { new Modifier("damage", 1.0) }),
	WITCH_SCROLLS("Witch Scrolls", new String[] { "Witches have a #chance-percent#% chance to block an", "incoming damage thanks to a magic shield.", "", "Witches can also cast dark magic runes", "that deal #damage# damage to hit players,", "slowing them down (II) for #slow-duration# seconds." }, "witch-scrolls", new Modifier[] { new Modifier("chance-percent", 55.0), new Modifier("damage", 2.5), new Modifier("slow-duration", 2.0) }),
	EVERBURNING_BLAZES("Everburning Blazes", new String[] { "Blazes summon beams of", "fire that ignite hit players.", "Lasts #burn-duration# seconds." }, "everburning-blazes", new Modifier[] { new Modifier("burn-duration", 3.0) }),
	ELECTRICITY_SHOCK("Electricity Shock", new String[] { "Walking on powered redstone deals #damage# damage.", "Can happen every 3 seconds.", "Works on wires, torches, repeaters and comparators." }, "electricity-shock", new Modifier[] { new Modifier("damage", 6.0) }),
	NETHER_SHIELD("Nether Shield", new String[] { "Magma cubes, pigmen & blazes have a", "#chance-percent#% chance to block player attacks", "thanks to a magical shield.", "It reflects #dmg-reflection-percent#% damage to the", "sender and ignites him for #burn-duration#", "seconds, while knocking him back." }, "nether-shield", new Modifier[] { new Modifier("chance-percent", 37.5), new Modifier("dmg-reflection-percent", 75.0), new Modifier("burn-duration", 3.0) }),
	UNDEAD_RAGE("Undead Rage", new String[] { "Whenever a (pig) zombie takes", "damage, it gets angry and gains", "Strength and Speed II for #rage-duration# seconds." }, "undead-rage", new Modifier[] { new Modifier("rage-duration", 4.0) }),
	BONE_GRENADES("Bone Grenades", new String[] { "Skeletons can throw bone grenades instead of arrows.", "Has a #chance-percent#% chance to occur.", "It explodes when touching the ground,", "dealing #damage# damage to nearby players", "while knocking them back." }, "bone-grenades", new Modifier[] { new Modifier("chance-percent", 35.0), new Modifier("damage", 6.0) }),
	POISONED_SLIMES("Poisoned Slimes", new String[] { "When hitting a slime, players have a #chance-percent#%", "chance to get poisoned for #duration# seconds.", "Potion effect amplifier: #amplifier#." }, "poisoned-slimes", new Modifier[] { new Modifier("amplifier", 1), new Modifier("duration", 3.0), new Modifier("chance-percent", 65.0) }),
	ANGRY_SPIDERS("Angry Spiders", new String[] { "Spiders can throw cobweb at players.", "It deals #damage# damage to hit players,", "slowing (#amplifier#) them down for #duration# seconds." }, "angry-spiders", new Modifier[] { new Modifier("damage", 4), new Modifier("duration", 3.0), new Modifier("amplifier", 1.0) }),
	// TRAPPED_DUNGEON_CHESTS("Trapped Dungeon Chests", new String[] { "Dungeon
	// chests have a #chance-percent#% chance to", "explode when opened for the
	// first time.", "Explosion radius: #radius#.", "&4Still in Beta." },
	// "trapped-dungeon-chests", new Modifier[] { new Modifier("radius", 4), new
	// Modifier("chance-percent", 35.0) }),
	//REALISTIC_SPAWN("Realistic Spawn", new String[] {"All Mob will have animation spawn like growup from underground"}, "realistic-spawn", new Modifier[] {new Modifier("spawn-duration-seconds", 1.5)}),
	ENDER_POWER("Ender Power", new String[] { "Players have a #chance-percent#% chance to get", "blinded for #duration# seconds when hitting", "endermen, endermites, shulkers and dragons." }, "ender-power", new Modifier[] { new Modifier("chance-percent", 70), new Modifier("duration", 6.0) }),
	UNDEAD_GUNNERS("Undead Gunners", new String[] { "Zombies named 'Undead Gunner' become gunners.", "Gunners can cast 1 spell!", "Rocket: #damage# damage, AoE" }, "undead-gunners", new Modifier[] { new Modifier("damage", 7.0), new Modifier("block-damage", 0) }),
	REALISTIC_PICKUP("Realistic Pickup", new String[] { "Players need to crouch near an", "item and look down in order to pick", "it up. Players also get briefly slowed." }, "realistic-pickup", new Modifier[] {}),
	BONE_WIZARDS("Bone Wizards", new String[] { "Skeletons named 'Bone Wizards' become mages.", "Wizards can cast 2 spells:", "Fireball: #fireball-damage# damage, #fireball-duration# sec. burn", "Frost Curse: #frost-curse-damage# damage, #frost-curse-duration# sec. slow (#frost-curse-amplifier#)" }, "bone-wizards", new Modifier[] { new Modifier("fireball-damage", 7.0), new Modifier("fireball-duration", 3.0), new Modifier("frost-curse-damage", 6.0), new Modifier("frost-curse-duration", 2.0), new Modifier("frost-curse-amplifier", 1) }),
	FREDDY("Freddy", new String[] { "Each time a player wakes up, he has", "a #chance-percent#% chance to summon Freddy." }, "freddy", new Modifier[] { new Modifier("chance-percent", 5.0) }),
	DANGEROUS_COAL("Dangerous Coal", new String[] { "When mining coal, players have a #chance-percent#%", "chance to pierce a gas pocket", "trapped in the rocks. It causes a", "explosion which radius is #radius# blocks." }, "dangerous-coal", new Modifier[] { new Modifier("chance-percent", 5.0), new Modifier("radius", 5.0) }),
	WITHER_RUSH("Wither Rush", new String[] { "Wither Skeletons can blink to players,", "dealing them #damage# damage." }, "wither-rush", new Modifier[] { new Modifier("damage", 3.0) }),
	MOB_CRITICAL_STRIKES("Mob Critical Strikes", new String[] { "Monsters can deal critical strikes,", "dealing #damage-percent#% &nadditional&7 damage.", "&cCrit Chance configurable for each monster." }, "mob-critical-strikes", new Modifier[] { new Modifier("crit-chance", 17, Type.EACH_MOB), new Modifier("damage-percent", 75.0) }),
	THIEF_SLIMES("Thief Slimes", new String[] { "Slimes have #chance-percent#% to steal levels", "from players when hitting them.", "Slimes can steal #exp# EXP." }, "thief-slimes", new Modifier[] { new Modifier("chance-percent", 55), new Modifier("exp", 12) }),
	WITHER_MACHINEGUN("Wither Machinegun", new String[] { "Wither Skeletons can throw coal", "at you, each dealing #damage# damage." }, "wither-machinegun", new Modifier[] { new Modifier("damage", 2) }),
	SNOW_SLOW("Snow Slow", new String[] { "Players who are not wearing iron,", "gold or diamond boots are permanently", "slowed when walking on snow." }, "snow-slow", new Modifier[] {}),
	// ENDLESS_DOOM("Endless Doom", new String[] { "", "", "" }, "endless-doom",
	// new Modifier[] {}),
	BLOOD_MOON("Blood Moon", new String[] { "When night comes, it has a #chance#% chance to turn red...", "Players take #damage-percent#% more damage and get", "slowed for #slow-duration#s whenever taking damage.", "", "Monsters spawn with Speed #speed#, Strength", "#increase-damage# and Resistance #damage-resistance#" }, "blood-moon", new Modifier[] { new Modifier("chance", 2),new Modifier("drowned-per-chunk", 3), new Modifier("damage-percent", 60), new Modifier("slow-duration", 3), new Modifier("increase-damage", 2), new Modifier("speed", 2), new Modifier("damage-resistance", 2) }, BloodMoon::new),
	THUNDERSTORM("Thunderstorm", new String[] { "Each storm has a #chance#% chance to become a thunderstorm.", "Lightning strikes deal #damage-percent#% more AoE damage", "and appear way more frequently around players!!" }, "thunderstorm", new Modifier[] { new Modifier("chance", 25), new Modifier("damage-percent", 125) }, Thunderstorm::new),
	// PLAYER_NIGHTMARES("Player Nightmares", new String[] { "Players can
	// trigger nightmares when", "leaving their beds (#chance#% chance).", "",
	// "During nightmares, tiny monsters will spawn", "and target the unlucky
	// player.", "Only the player who has the nightmare", "can damage these
	// monsters.", "", "The nightmare grants Slow I and Blindness.", "Lasts for
	// 1 minute.", "Other players can right-click the player", "to make him wake
	// up faster!" }, "nightmare", new Modifier[] { new Modifier("chance", 10),
	// new Modifier("damage-percent", 125) }, true),
	;

	private final String name, path;
	private final List<String> lore;
	private final List<Modifier> modifiers;
	private final Function<World, WorldEventHandler> event;
	private ConfigFile configFile;

	private Feature(String name, String[] lore, String path, Modifier[] modifiers) {
		this(name, lore, path, modifiers, null);
	}

	private Feature(String name, String[] lore, String path, Modifier[] modifiers, Function<World, WorldEventHandler> event) {
		this.name = name;
		this.lore = Arrays.asList(lore);
		this.path = path;
		this.modifiers = Arrays.asList(modifiers);
		this.event = event;
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
		configFile = new ConfigFile("/modifiers", path);
	}

	public ConfigFile getConfigFile() {
		return configFile;
	}

	public boolean getBoolean(String path) {
		return configFile.getConfig().getBoolean(path);
	}

	public double getDouble(String path) {
		return configFile.getConfig().getDouble(path);
	}

	public String getString(String path) {
		return configFile.getConfig().getString(path);
	}

	public List<Modifier> getModifiers() {
		return modifiers;
	}

	public boolean isEvent() {
		return event != null;
	}
	
	public WorldEventHandler generateWorldEventHandler(World world) {
		return event.apply(world);
	}

	public boolean isEnabled(Entity entity) {
		return isEnabled(entity.getWorld());
	}

	public boolean isEnabled(World world) {
		return SuddenDeath.plugin.getConfig().getStringList(path).contains(world.getName());
	}
}