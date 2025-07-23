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
	NOW_BLEEDING("You are now bleeding. Quickly find a bandage or you'll die within seconds."),
	NOW_INFECTED("You're starting to feel very weird..."),
	PAPI_INFECTED("Infected"),
	PAPI_NOT_INFECTED("Clean"),
	PAPI_BLEEDING("Bleeding"),
	PAPI_NOT_BLEEDING("Clean"),
	USE_STRANGE_BREW("You are no longer infected."),
	USE_BANDAGE("You cured all your wounds."),
	FREDDY_SUMMONED("You summoned Freddy!"),
	LOST_EXP("You just lost #exp# EXP!"),
	NOT_ENOUGH_PERMS("You don't have enough permissions."),

	BLOOD_MOON("The Blood Moon is rising..."),
	THUNDERSTORM("You feel the air above you getting colder..."),

	GUI_NAME("Your Status"),
	GUI_BLEEDING_NAME("Bleeding"),
	GUI_BLEEDING_LORE(Arrays.asList(
			"You are slowly losing life! Quickly",
			"find or craft a bandage to stop bleeding!"
	)),
	GUI_INFECTED_NAME("Infected"),
	GUI_INFECTED_LORE(Arrays.asList(
			"You're feeling very weird... Find",
			"a Strange Brew to stop that."
	)),
	GUI_NO_SPECIAL_STATUS_NAME("No special status"),
	GUI_NO_SPECIAL_STATUS_LORE(Arrays.asList(
			"You seem clean... for now."
	)),

	GIVE_ITEM("You gave &f#player# #item##amount#&e."),
	RECEIVE_ITEM("You received &f#item##amount#&e.");

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