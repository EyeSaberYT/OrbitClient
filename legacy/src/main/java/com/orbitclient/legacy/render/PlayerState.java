package com.orbitclient.legacy.render;

/** What the shared cosmetic art needs to know about the player being drawn this frame. */
public final class PlayerState {
	public int id;
	public double x, y, z;
	public float bodyRot;
	/** How far through the walk cycle the legs are. */
	public float walkAnimationPos;
	/** How fast the legs are swinging (0 standing still, about 1 sprinting). */
	public float walkAnimationSpeed;
	/** Gliding on an elytra. Always false here: there are none before 1.9. */
	public boolean isFallFlying;
	public boolean isInvisible;
}
