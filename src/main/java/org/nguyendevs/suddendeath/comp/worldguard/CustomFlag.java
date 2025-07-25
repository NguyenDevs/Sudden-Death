package org.nguyendevs.suddendeath.comp.worldguard;

/**
 * Enum representing custom WorldGuard flags for the SuddenDeath plugin.
 */
public enum CustomFlag {
	/**
	 * Custom flag for SuddenDeath effects.
	 */
	SD_EFFECT;

	/**
	 * Gets the configuration path for the custom flag.
	 * Converts the enum name to lowercase and replaces underscores with hyphens.
	 *
	 * @return The configuration path for the flag.
	 */
	public String getPath() {
		return name().toLowerCase().replace("_", "-");
	}
}