package com.orbitclient.compat;

//? if <1.19.3 {
/*import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

// Before 1.19.3 (which brought JOML and com.mojang.math.Axis): the same rotations from the older Vector3f axes.
// build.gradle points the Axis import here.
public enum Axis {
	XP(Vector3f.XP), YP(Vector3f.YP), ZP(Vector3f.ZP), XN(Vector3f.XN), YN(Vector3f.YN), ZN(Vector3f.ZN);

	private final Vector3f axis;

	Axis(Vector3f axis) {
		this.axis = axis;
	}

	public Quaternion rotation(float radians) {
		return axis.rotation(radians);
	}

	public Quaternion rotationDegrees(float degrees) {
		return axis.rotationDegrees(degrees);
	}
}
*///?}
