package com.orbitclient.compat;

//? if <1.16.2 {
/*import net.minecraft.client.Options;

// Before 1.16.2 the camera mode was a number on Options (0 first person, 1 behind, 2 in front).
// build.gradle points the CameraType import and the options calls here.
public enum CameraType {
	FIRST_PERSON, THIRD_PERSON_BACK, THIRD_PERSON_FRONT;

	public boolean isFirstPerson() {
		return this == FIRST_PERSON;
	}

	public static CameraType get(Options options) {
		return values()[Math.floorMod(options.thirdPersonView, 3)];
	}

	public static void set(Options options, CameraType type) {
		options.thirdPersonView = type.ordinal();
	}
}
*///?}
