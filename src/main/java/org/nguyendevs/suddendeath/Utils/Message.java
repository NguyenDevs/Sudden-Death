package org.nguyendevs.suddendeath.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")

public enum Message {
	PREFIX("&6[&cSudden&4Death&6]"),
	NOW_BLEEDING("&7You are now &cbleeding&7. Quickly find a &bBandage&7 or you'll die within seconds."),
	NOW_INFECTED("&7You're starting to feel very &8Weird..."),
	USE_STRANGE_BREW("&7You are no longer infected."),
	USE_BANDAGE("&7You cured all your wounds."),
	NO_LONGER_BLEEDING("&7You are no longer bleeding."),
	HUNGER_NAUSEA("&7It seems like your blood sugar is low, you start to feel dizzy."),
	NO_LONGER_HUNGER("&7Feeling full feels good, doesn't it?"),
	FREDDY_SUMMONED("&7You summoned &0Freddy!"),
	LOST_EXP("You just lost #exp# EXP!"),
	NOT_ENOUGH_PERMS("&cYou don't have enough permissions."),

	BLOOD_MOON("&4&lThe Blood Moon is rising..."),
	THUNDERSTORM("&4&lYou feel the air above you getting colder..."),
	METEOR_RAIN("&4&lSomething just entered the atmosphere!"),

	GUI_STATUS_NAME("&8Your Status"),
	GUI_RECIPE_NAME("&8Recipes"),
	GUI_RECIPE_CLOSE("&cClose"),
	GUI_CRAFTER_NAME("&8Recipe:"),
	GUI_CRAFTER_BACK("&aBack"),
	GUI_BLEEDING_NAME("&cBleeding"),
	GUI_BLEEDING_LORE(Arrays.asList(
			"You are slowly losing life! Quickly",
			"find or craft a bandage to stop bleeding!"
	)),
	GUI_INFECTED_NAME("&5Infected"),
	GUI_INFECTED_LORE(Arrays.asList(
			"You're feeling very weird... Find",
			"a Strange Brew to stop that."
	)),
	GUI_NO_SPECIAL_STATUS_NAME("No special status"),
	GUI_NO_SPECIAL_STATUS_LORE(Arrays.asList(
			"You seem clean... for now."
	)),

	GUI_ADMIN_NAME("&8SD Admin GUI"),
	GUI_PLAYER_NAME("&8SD Player GUI"),

	GUI_NEXT("&aNext"),
	GUI_PREVIOUS("&aPrevious"),
	GUI_FEATURES("&7This feature is enabled in:"),
	GUI_FEATURES_ENABLED("&aThis feature is enabled in this world."),
	GUI_FEATURES_DISABLED("&cThis feature is disabled in this world."),

	FILTER_NAME("&aFilters"),
    FILTER_LORE_DESC("&7Filter options"),
	FILTER_LORE_DEFAULT("Default"),
	FILTER_LORE_MOB("Mob abilities"),
	FILTER_LORE_SURVIVAL("Survival features"),
	FILTER_LORE_EVENT("Event features"),
	FILTER_LORE_VISUAL("Visual filter"),
	GIVE_ITEM("You gave &f#player# #item##amount#&e."),
	RECEIVE_ITEM("You received &f#item##amount#&e."),

	PAPI_INFECTED("Infected"),
	PAPI_NOT_INFECTED("Clean"),
	PAPI_BLEEDING("Bleeding"),
	PAPI_NOT_BLEEDING("Clean");
	private final Object value;

	Message(Object value) {
		if (value == null || !(value instanceof String || value instanceof List)) {
			throw new IllegalArgumentException("Message value must be a String or List<String>");
		}
		if (value instanceof List) {
			this.value = Collections.unmodifiableList(((List<?>) value).stream()
					.map(Object::toString)
					.collect(Collectors.toList()));
		} else {
			this.value = value;
		}
	}

	public Object getValue() {
		return value;
	}

	public String getString() {
		if (!(value instanceof String)) {
			throw new IllegalStateException("Message value is not a String for: " + name());
		}
		return (String) value;
	}

	@SuppressWarnings("unchecked")
	public List<String> getLore() {
		if (!(value instanceof List)) {
			throw new IllegalStateException("Message value is not a List for: " + name());
		}
		return (List<String>) value;
	}

	public boolean isLore() {
		return value instanceof List;
	}
}