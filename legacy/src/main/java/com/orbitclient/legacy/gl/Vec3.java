package com.orbitclient.legacy.gl;

/** A point or direction in blocks (net.minecraft.world.phys.Vec3 on the new engine). */
public final class Vec3 {
	public final double x, y, z;

	public Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3 add(Vec3 o) {
		return new Vec3(x + o.x, y + o.y, z + o.z);
	}

	public Vec3 subtract(Vec3 o) {
		return new Vec3(x - o.x, y - o.y, z - o.z);
	}

	public Vec3 scale(double k) {
		return new Vec3(x * k, y * k, z * k);
	}

	public double distanceToSqr(Vec3 o) {
		double dx = x - o.x, dy = y - o.y, dz = z - o.z;
		return dx * dx + dy * dy + dz * dz;
	}
}
