package com.orbitclient.cape;

import com.orbitclient.module.visual.WaveyCapesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * One player's cape as a stiff piece of cloth on a hinge between the shoulders, stepped once per game
 * tick and drawn interpolated between ticks.
 *
 * <p>Three numbers describe it: how far it swings out from the back (a damped pendulum: air from moving
 * forward pushes it out, gravity pulls it back, and gravity feels stronger while rising and weaker while
 * falling), how far it swings to the side (strafing and turning), and how much the hem lags behind the
 * swing, which bends the cloth when the swing speeds up or slows down. {@link #points} lays the cape out
 * from those as a chain of points in the body's frame: in blocks, x across the back, y down, z away from
 * the back.
 */
public final class ClothCape {
	public static final int SEGMENTS = 16;
	/** The cape is 16 texture pixels tall, one block. */
	static final float SEGMENT = 1f / SEGMENTS;
	/** It rests a little off the back, like vanilla's cape, and never swings into it. */
	private static final float REST = (float) Math.toRadians(6);
	private static final float MAX_SWING = (float) Math.toRadians(150);
	/** Pendulum response and damping: settles from a sprint in about 20 ticks, a little under critically damped. */
	private static final float RESPONSE = 0.04f;
	private static final float DAMPING = 0.6f;
	/** Ticks a cape survives without being drawn (off screen, out of range) before it's dropped. */
	private static final int FORGET_AFTER = 60;
	private static final Map<Integer, ClothCape> CAPES = new HashMap<>();

	// Now (end of the last tick) and a tick before, for interpolation.
	private float swing = REST, side, bend;
	private float oldSwing = REST, oldSide, oldBend;
	private float swingSpeed, sideSpeed;
	private int age;
	private int lastDrawn;
	// Where the player was last tick, tracked here rather than read from xo/yo/zo: a teleport resets those,
	// and servers move players by small teleports often.
	private double lastPosX, lastPosY, lastPosZ;
	private float lastBodyYaw;
	private double lastDy;
	private boolean hasLast;

	private ClothCape() {
	}

	/** The cape of the entity with this id, created hanging still the first time. Marks it as drawn. */
	public static ClothCape of(int entityId) {
		ClothCape cape = CAPES.computeIfAbsent(entityId, id -> new ClothCape());
		cape.lastDrawn = cape.age;
		return cape;
	}

	/** Steps every cape that was drawn lately; called once per client tick. */
	public static void tickAll(Minecraft mc) {
		if (mc.level == null) {
			CAPES.clear();
			return;
		}
		if (mc.isPaused()) return;
		Iterator<Map.Entry<Integer, ClothCape>> it = CAPES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, ClothCape> e = it.next();
			ClothCape cape = e.getValue();
			Entity entity = mc.level.getEntity(e.getKey());
			if (!(entity instanceof Player player) || cape.age - cape.lastDrawn > FORGET_AFTER) {
				it.remove();
				continue;
			}
			cape.step(player);
		}
	}

	private void step(Player p) {
		age++;
		oldSwing = swing;
		oldSide = side;
		oldBend = bend;

		// The player's movement this tick, turned into the body's frame.
		double dx = hasLast ? p.getX() - lastPosX : 0, dy = hasLast ? p.getY() - lastPosY : 0, dz = hasLast ? p.getZ() - lastPosZ : 0;
		float turn = hasLast ? (float) Math.toRadians(Mth.wrapDegrees(p.yBodyRot - lastBodyYaw)) : 0;
		lastPosX = p.getX();
		lastPosY = p.getY();
		lastPosZ = p.getZ();
		lastBodyYaw = p.yBodyRot;
		hasLast = true;
		if (dx * dx + dy * dy + dz * dz > 4) { // teleported: don't whip the cape across the map
			dx = dy = dz = 0;
			turn = 0;
		}
		double yaw = Math.toRadians(p.yBodyRot);
		float forward = (float) (-dx * Math.sin(yaw) + dz * Math.cos(yaw));
		float sideways = (float) (dx * Math.cos(yaw) + dz * Math.sin(yaw));

		// The module's settings, scaled so their defaults (gravity 25, height 6, strafe 2) are 1.
		float gravity = WaveyCapesModule.gravity() / 25f;
		float height = WaveyCapesModule.heightMultiplier() / 6f;
		float strafe = WaveyCapesModule.strafeMultiplier() / 2f;
		if (p.isInWater()) gravity *= 0.3f;

		// Out from the back. Air grows a bit faster than speed: walking lifts it about 38 degrees, sprinting 50.
		float air = 10f * (float) Math.pow(Math.max(0, forward), 1.6);
		if (!p.onGround()) air *= 0.75f; // retain some airflow while airborne instead of collapsing to the back
		// Thrown up high (a fall), it drops back quickly instead of floating down.
		float weight = gravity * (1 + Math.max(0, swing - 0.9f) * 1.5f);
		// Keep the same restoring force in the air. A large airborne multiplier pins the cape to
		// the back precisely when jumping, which looks like it flies into the player.
		// Leaning forward (crouching) tilts world-down toward the back, so it drapes along it.
		float lean = p.isCrouching() ? 0.5f : 0f;
		float torque = air * (float) Math.cos(swing) - weight * (float) Math.sin(swing + lean);
		swingSpeed = swingSpeed * DAMPING + RESPONSE * torque;
		// On takeoff the body accelerates upward beneath the cloth, so the hem lags outward.
		// A fast fall also catches air and lifts the cape; a slow fall doesn't.
		if (dy > 0) swingSpeed += (float) dy * 2f * height;
		else swingSpeed += 12f * (float) Math.pow(Math.max(0, -dy - 0.2), 1.5) * height;
		// Landing (the fall stopping short) carries the cloth on down against the back too.
		if (dy > lastDy && lastDy < 0) swingSpeed -= (float) (Math.min(dy, 0) - lastDy) * 3f * height;
		lastDy = dy;
		swing += swingSpeed;
		if (swing < REST) {
			swing = REST;
			swingSpeed = Math.max(0, swingSpeed);
		} else if (swing > MAX_SWING) {
			swing = MAX_SWING;
			swingSpeed = Math.min(0, swingSpeed);
		}

		// To the side: strafing trails it the other way, turning swings it out; a spring brings it back.
		// The angle it settles at: about 15 degrees for a walking-speed strafe.
		float push = -sideways * 1.3f * strafe - turn * 1.7f;
		sideSpeed = sideSpeed * DAMPING + RESPONSE * (push - side);
		side = Mth.clamp(side + sideSpeed, -1.2f, 1.2f);

		// The hem lags behind the swing, so the cloth bends while the swing changes.
		float targetBend = -swingSpeed * 2.5f;
		if (WaveyCapesModule.windWaves()) {
			float speed = (float) Math.sqrt(dx * dx + dz * dz);
			targetBend += (float) Math.sin((age + p.getId() * 7.3f) * 0.6f) * (0.08f + Math.min(1f, speed * 4f) * 0.2f);
		}
		bend += (targetBend - bend) * 0.5f;
	}

	/** The points this frame, interpolated between the last two ticks: {x0, y0, z0, x1, ...}. */
	public float[] points(float partialTick) {
		return layOut(Mth.lerp(partialTick, oldSwing, swing), Mth.lerp(partialTick, oldSide, side), Mth.lerp(partialTick, oldBend, bend));
	}

	/** A cape without physics: straight, tilted by the same lean vanilla uses for its rigid cape. */
	public static float[] rigid(AvatarRenderState state) {
		float swing = (float) Math.toRadians(Mth.clamp(6f + state.capeLean / 2f + state.capeFlap, 0f, 150f));
		return layOut(swing, (float) Math.toRadians(state.capeLean2 / 2f), 0);
	}

	/** The chain for a swing out, a swing to the side and a bend toward the hem (all radians). */
	private static float[] layOut(float swing, float side, float bend) {
		float[] out = new float[(SEGMENTS + 1) * 3];
		float x = 0, y = 0, z = 0;
		float sinSide = (float) Math.sin(side), cosSide = (float) Math.cos(side);
		for (int i = 0; i < SEGMENTS; i++) {
			// Each segment's angle out from the back: the swing, plus the bend spread along the cape.
			float a = Math.max(REST * 0.5f, swing + bend * (i + 0.5f) / SEGMENTS);
			float down = (float) Math.cos(a), outward = (float) Math.sin(a);
			x += sinSide * down * SEGMENT;
			y += cosSide * down * SEGMENT;
			z += outward * SEGMENT;
			out[(i + 1) * 3] = x;
			out[(i + 1) * 3 + 1] = y;
			out[(i + 1) * 3 + 2] = z;
		}
		return out;
	}
}
