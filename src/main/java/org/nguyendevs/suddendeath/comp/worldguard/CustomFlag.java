package org.nguyendevs.suddendeath.comp.worldguard;

/**
 * Enum representing custom WorldGuard flags for the SuddenDeath plugin.
 */
public enum CustomFlag {
	/**
	 * Custom flag for SuddenDeath effects (Bleeding, Infected).
	 * When ALLOW: Plugin can apply effects in this region
	 * When DENY: Plugin cannot apply effects in this region
	 */
	SDS_EFFECT,

	/**
	 * Custom flag for SuddenDeath status removal.
	 * When ALLOW: Entering this region will remove all SuddenDeath effects
	 * When DENY: Entering this region will not remove effects
	 */
	SDS_REMOVE;

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