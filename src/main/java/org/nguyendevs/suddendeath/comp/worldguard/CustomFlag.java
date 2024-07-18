package org.nguyendevs.suddendeath.comp.worldguard;

public enum CustomFlag {
	SD_EFFECT;

	public String getPath() {
		return name().toLowerCase().replace("_", "-");
	}
}
