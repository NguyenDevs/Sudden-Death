package org.nguyendevs.suddendeath.player;

public class Modifier {
	private final String name;
	private final Object value;
	private final Type type;

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

	public Modifier(String name, Object value, Type type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Object getDefaultValue() {
		return value;
	}

	public Type getType() {
		return type;
	}

	public enum Type {
		NONE,
		EACH_MOB;
	}
}