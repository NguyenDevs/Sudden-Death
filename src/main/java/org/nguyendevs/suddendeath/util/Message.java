package org.nguyendevs.suddendeath.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing predefined messages used in the SuddenDeath plugin.
 * Messages can be either a single string or a list of strings for lore.
 */
public enum Message {
	PREFIX("&6[&cSudden&4Death&6]"),
	NOW_BLEEDING("&7You are now &cbleeding&7. Quickly find a &bBandage&7 or you'll die within seconds."),
	NOW_INFECTED("&7You're starting to feel very &8Weird..."),
	USE_STRANGE_BREW("&7You are no longer infected."),
	USE_BANDAGE("&7You cured all your wounds."),
	NO_LONGER_BLEEDING("&7You are no longer bleeding."),
	FREDDY_SUMMONED("&7You summoned &0Freddy!"),
	LOST_EXP("You just lost #exp# EXP!"),
	NOT_ENOUGH_PERMS("&cYou don't have enough permissions."),

	BLOOD_MOON("The Blood Moon is rising..."),
	THUNDERSTORM("You feel the air above you getting colder..."),

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

	GUI_PLAYER_NAME("&8SD Player GUI"),
	GUI_PLAYER_NEXT("&aNext"),
	GUI_PLAYER_PREVIOUS("&aPrevious"),
	GUI_PLAYER_FEATURES("&7This feature is enabled in:"),
	GUI_PLAYER_FEATURES_ENABLED("&aThis feature is enabled in this world."),
	GUI_PLAYER_FEATURES_DISABLED("&cThis feature is disabled in this world."),

	GIVE_ITEM("You gave &f#player# #item##amount#&e."),
	RECEIVE_ITEM("You received &f#item##amount#&e."),

	PAPI_INFECTED("Infected"),
	PAPI_NOT_INFECTED("Clean"),
	PAPI_BLEEDING("Bleeding"),
	PAPI_NOT_BLEEDING("Clean");
	private final Object value;

	/**
	 * Constructs a Message with the specified value.
	 *
	 * @param value The message value, either a String or a List of Strings.
	 * @throws IllegalArgumentException if value is null or not a String/List.
	 */
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

	/**
	 * Gets the message value.
	 *
	 * @return The message as a String or List of Strings.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Gets the message as a String, if applicable.
	 *
	 * @return The message as a String.
	 * @throws IllegalStateException if the value is not a String.
	 */
	public String getString() {
		if (!(value instanceof String)) {
			throw new IllegalStateException("Message value is not a String for: " + name());
		}
		return (String) value;
	}

	/**
	 * Gets the message as a List of Strings, if applicable.
	 *
	 * @return The message as a List of Strings.
	 * @throws IllegalStateException if the value is not a List.
	 */
	@SuppressWarnings("unchecked")
	public List<String> getLore() {
		if (!(value instanceof List)) {
			throw new IllegalStateException("Message value is not a List for: " + name());
		}
		return (List<String>) value;
	}

	/**
	 * Checks if the message is a lore (List of Strings).
	 *
	 * @return True if the value is a List.
	 */
	public boolean isLore() {
		return value instanceof List;
	}
}