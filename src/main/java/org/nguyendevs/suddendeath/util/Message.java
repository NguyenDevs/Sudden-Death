package org.nguyendevs.suddendeath.util;

import java.util.Arrays;

public enum Message {
	NOW_BLEEDING("You are now bleeding. Quickly find a bandage or you'll die within seconds."),
	NOW_INFECTED("You're starting to feel very wieird..."),
	PAPI_INFECTED("Infected"),
	PAPI_NOT_INFECTED("Clean"),
	PAPI_BLEEDING("Bleeding"),
	PAPI_NOT_BLEEDING("Clean"),
	USE_STRANGE_BREW("You are no longer infected."),
	USE_BANDAGE("You cured all your wounds."),
	// OPEN_TRAPPED_DUNGEON_CHEST("open-trapped-dungeon-chest", "The chest you
	// opened happened to be trapped!"),
	FREDDY_SUMMONED("You summoned Freddy!"),
	LOST_EXP("You just lost #exp# EXP!"),
	NOT_ENOUGH_PERMS("You don't have enough permissions."),
	CHOSE_DIFF("You successfully selected &f#difficulty#&e."),

	BLOOD_MOON("The Bloodmoon is rising..."),
	THUNDERSTORM("You feel the air above you getting colder..."),

	GUI_NAME("Your Status"),
	GUI_BLEEDING_NAME("Bleeding"),
	GUI_BLEEDING_LORE(Arrays.asList(new String[] { "You are slowly losing life! Quickly", "find or craft a bandage to stop bleeding!" })),
	GUI_INFECTED_NAME("Infected"),
	GUI_INFECTED_LORE(Arrays.asList(new String[] { "You're feeling very wieird.. Find", "a Strange Brew to stop that.", })),
	GUI_NO_SPECIAL_STATUS_NAME("No special status"),
	GUI_NO_SPECIAL_STATUS_LORE(Arrays.asList(new String[] { "You seem clean.. for now." })),
	GUI_DIFFICULTY_LORE(Arrays.asList(new String[] { "Increased Damage: &f#increased-damage#&7%", "Health Malus: &f#health-malus#", "Difficulty: #difficulty#" })),
	GUI_CLICK_SELECT_DIFF("Click to select your difficulty."),
	GUI_CURRENT_DIFF("This is your current difficulty."),
	CURRENT("Current"),

	GIVE_ITEM("You gave &f#player# #item##amount#&e."),
	RECEIVE_ITEM("You received &f#item##amount#&e."),;

	public final Object value;

	private Message(Object value) {
		this.value = value;
	}
}