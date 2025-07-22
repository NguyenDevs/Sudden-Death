package org.nguyendevs.suddendeath.player;

import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

/**
 * Represents a customizable modifier for player-related attributes in the SuddenDeath plugin.
 */
public class Modifier {
	private final String name;
	private final Object value;
	private final Type type;

	/**
	 * Constructs a Modifier with a double value and default type NONE.
	 *
	 * @param name  The name of the modifier.
	 * @param value The double value of the modifier.
	 * @throws IllegalArgumentException if name is null or empty.
	 */
	public Modifier(String name, double value) {
		this(name, value, Type.NONE);
	}

	/**
	 * Constructs a Modifier with a boolean value and default type NONE.
	 *
	 * @param name  The name of the modifier.
	 * @param value The boolean value of the modifier.
	 * @throws IllegalArgumentException if name is null or empty.
	 */
	public Modifier(String name, boolean value) {
		this(name, value, Type.NONE);
	}

	/**
	 * Constructs a Modifier with an integer value and default type NONE.
	 *
	 * @param name  The name of the modifier.
	 * @param value The integer value of the modifier.
	 * @throws IllegalArgumentException if name is null or empty.
	 */
	public Modifier(String name, int value) {
		this(name, value, Type.NONE);
	}

	/**
	 * Constructs a Modifier with a String value and default type NONE.
	 *
	 * @param name  The name of the modifier.
	 * @param value The String value of the modifier.
	 * @throws IllegalArgumentException if name is null or empty.
	 */
	public Modifier(String name, String value) {
		this(name, value, Type.NONE);
	}

	/**
	 * Constructs a Modifier with the specified value and type.
	 *
	 * @param name  The name of the modifier.
	 * @param value The value of the modifier (Double, Boolean, Integer, String, or other).
	 * @param type  The type of the modifier.
	 * @throws IllegalArgumentException if name is null or empty, or if type is null.
	 */
	public Modifier(String name, Object value, Type type) {
		if (name == null || name.trim().isEmpty()) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Modifier name cannot be null or empty");
			throw new IllegalArgumentException("Modifier name cannot be null or empty");
		}
		if (type == null) {
			SuddenDeath.getInstance().getLogger().log(Level.SEVERE,
					"Modifier type cannot be null for modifier: " + name);
			throw new IllegalArgumentException("Modifier type cannot be null");
		}
		this.name = name;
		this.value = value;
		this.type = type;
	}

	/**
	 * Gets the name of the modifier.
	 *
	 * @return The modifier's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value of the modifier.
	 *
	 * @return The modifier's value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Gets the type of the modifier.
	 *
	 * @return The modifier's Type.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Enum defining the possible types for a Modifier.
	 */
	public enum Type {
		NONE,
		EACH_MOB
	}
}