package com.orbitclient.cosmetics;

/** Where a cosmetic goes; one cosmetic per slot can be worn. */
public enum CosmeticSlot {
	CAPE("Capes"),
	HAT("Hats"),
	FACE("Face"),
	BACK("Back"),
	WINGS("Wings"),
	AURA("Auras"),
	PET("Pets");

	public final String title;

	CosmeticSlot(String title) {
		this.title = title;
	}
}
