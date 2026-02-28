package org.nguyendevs.suddendeath.Player;

import org.nguyendevs.suddendeath.SuddenDeath;

import java.util.logging.Level;

public record Modifier(String name, Object value, Type type) {
	public Modifier(String name, double value) {
		this(name, value, Type.NONE);
	}

	public Modifier(String name, boolean value) {
		this(name, value, Type.NONE);
	}

	public Modifier(String name, int value) {
		this(name, value, Type.NONE);
	}

	public Modifier(String name, String value) {
		this(name, value, Type.NONE);
	}

	public Modifier {
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
	}


	public enum Type {
		NONE,
		EACH_MOB
	}
}