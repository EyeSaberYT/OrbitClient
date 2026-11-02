package com.orbitclient.module;

/** The filter pills in the mod menu, same as Lunar's: HUD, SERVER, MECHANIC. */
public enum Category {
	HUD("HUD"),
	SERVER("Server"),
	MECHANIC("Mechanic");

	public final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}
}
