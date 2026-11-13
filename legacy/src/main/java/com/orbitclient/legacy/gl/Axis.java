package com.orbitclient.legacy.gl;

/** The rotation axes the shared cosmetic art uses (com.mojang.math.Axis on the new engine). */
public enum Axis {
	XP(1, 0, 0), YP(0, 1, 0), ZP(0, 0, 1), XN(-1, 0, 0), YN(0, -1, 0), ZN(0, 0, -1);

	/** A turn around an axis, as OpenGL's rotate call takes it. */
	public record Rotation(float degrees, float x, float y, float z) {
	}

	private final float x, y, z;

	Axis(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Rotation rotationDegrees(float degrees) {
		return new Rotation(degrees, x, y, z);
	}

	public Rotation rotation(float radians) {
		return new Rotation((float) Math.toDegrees(radians), x, y, z);
	}
}
