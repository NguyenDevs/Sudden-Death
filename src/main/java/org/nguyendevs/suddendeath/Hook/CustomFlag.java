package org.nguyendevs.suddendeath.Hook;

public enum CustomFlag {
	SDS_EFFECT,
	SDS_REMOVE,
	SDS_EVENT,
    SDS_BREAK;
	//SDS_PLACE;

	public String getPath() {
		return name().toLowerCase().replace("_", "-");
	}
}