package com.orbitclient.cosmetics;

import com.orbitclient.util.PoseUtil;
import com.orbitclient.cosmetics.VoxelModel.Paint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every cosmetic, registered slot by slot from the art in Capes, Hats, Faces, Backs, Wings, Auras and
 * Pets, plus the helpers they share. Models are voxel pixel art in model pixels (1/16 block). On the
 * head, the origin is the neck pivot and the head spans x/z -4..4, y -8..0 (front is -z). On the body,
 * the origin is the neck and the body spans x -4..4, y 0..12, z -2..2 (back is +z).
 */
public final class Cosmetics {
	private static final Map<String, Cosmetic> BY_ID = new LinkedHashMap<>();

	private Cosmetics() {
	}

	public static List<Cosmetic> all() {
		return List.copyOf(BY_ID.values());
	}

	public static List<Cosmetic> in(CosmeticSlot slot) {
		List<Cosmetic> list = new ArrayList<>();
		for (Cosmetic c : BY_ID.values()) if (c.slot == slot) list.add(c);
		return Collections.unmodifiableList(list);
	}

	public static Cosmetic get(String id) {
		return id == null ? null : BY_ID.get(id);
	}

	static {
		Capes.register();
		Hats.register();
		Faces.register();
		Backs.register();
		Wings.register();
		Auras.register();
		Pets.register();
	}

	// ---------------------------------------------------------------- registering

	static void add(String id, String name, CosmeticSlot slot, int color, boolean colorable, Cosmetic.Renderer r) {
		if (BY_ID.containsKey(id)) throw new IllegalStateException("Duplicate cosmetic id " + id);
		BY_ID.put(id, new Cosmetic(id, name, slot, color, colorable, r, 1));
	}

	/** Sets the model shown as a cosmetic's picture in menus. */
	static void icon(String id, VoxelModel model, boolean pair) {
		Cosmetic c = BY_ID.get(id);
		c.iconModel = model;
		c.iconPair = pair;
	}

	/** Marks a cosmetic that just sits still (for listings). */
	static void still(String id) {
		BY_ID.get(id).animated = false;
	}

	/** A hat's height above the head in model pixels, for the nametag. */
	static void lift(String id, float pixels) {
		BY_ID.get(id).nameLift = pixels / 16f;
	}

	/** Lifts the nametag past the top of a hat model (and a little more for one that moves). */
	static void liftTo(String id, VoxelModel model, float extra) {
		lift(id, Math.max(0, -8 - model.top() + 0.3f + extra));
	}

	static void cape(String id, String name, int frames) {
		if (BY_ID.containsKey(id)) throw new IllegalStateException("Duplicate cosmetic id " + id);
		BY_ID.put(id, new Cosmetic(id, name, CosmeticSlot.CAPE, 0xFFFFFFFF, false, null, frames));
	}

	/** A still hat: drawn on the head, pictured by its model, with the nametag lifted past it. */
	static void hat(String id, String name, int color, boolean colorable, VoxelModel model) {
		add(id, name, CosmeticSlot.HAT, color, colorable, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(model);
			ctx.pose().popPose();
		});
		icon(id, model, false);
		liftTo(id, model, 0);
		still(id);
	}

	/** A still model on the head (face) or the body (back). */
	static void worn(String id, String name, CosmeticSlot slot, int color, boolean colorable, VoxelModel model) {
		add(id, name, slot, color, colorable, ctx -> {
			ctx.attach(slot == CosmeticSlot.BACK ? ctx.model().body : ctx.model().head);
			ctx.draw(model);
			ctx.pose().popPose();
		});
		icon(id, model, false);
		still(id);
	}

	/** A pair of wings from one (right) wing model. speed scales how fast they beat. */
	static void wingPair(String id, String name, int color, boolean colorable, VoxelModel wing, double flap, double speed) {
		add(id, name, CosmeticSlot.WINGS, color, colorable, ctx -> wings(ctx, wing, flap, speed));
		icon(id, wing, true);
	}

	static Map<Character, Paint> palette(Object... pairs) {
		Map<Character, Paint> m = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) m.put((Character) pairs[i], (Paint) pairs[i + 1]);
		return m;
	}

	/** A flat, upright picture one voxel thick (rows top to bottom), centered on the origin. */
	static VoxelModel sprite(float size, Map<Character, Paint> palette, String... rows) {
		int w = 0;
		for (String r : rows) w = Math.max(w, r.length());
		return VoxelModel.build(size, -w * size / 2, -rows.length * size / 2, -size / 2, palette, rows);
	}

	// ---------------------------------------------------------------- shape helpers

	/** Is (x, y) within the ring between radii r0 and r1 around (c, c)? */
	static boolean ring(double x, double y, double c, double r0, double r1) {
		double d = Math.hypot(x - c, y - c);
		return d >= r0 && d <= r1;
	}

	/** Distance from (px, py) to the segment (ax, ay)-(bx, by). */
	static double seg(double px, double py, double ax, double ay, double bx, double by) {
		double dx = bx - ax, dy = by - ay;
		double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
		return Math.hypot(px - ax - t * dx, py - ay - t * dy);
	}

	/** Is (x, y) inside the polygon given as x0, y0, x1, y1, ...? */
	static boolean inPolygon(double x, double y, double... p) {
		boolean in = false;
		for (int i = 0, j = p.length - 2; i < p.length; j = i, i += 2) {
			if ((p[i + 1] > y) != (p[j + 1] > y) && x < (p[j] - p[i]) * (y - p[i + 1]) / (p[j + 1] - p[i + 1]) + p[i]) in = !in;
		}
		return in;
	}

	/** Distance from (x, y) to the nearest edge of the polygon. */
	static double polygonEdge(double x, double y, double... p) {
		double best = Double.MAX_VALUE;
		for (int i = 0, j = p.length - 2; i < p.length; j = i, i += 2) best = Math.min(best, seg(x, y, p[j], p[j + 1], p[i], p[i + 1]));
		return best;
	}

	/** A repeatable pseudo-random number in 0..1 from a seed and a channel. */
	static double rnd(long seed, int k) {
		long h = seed * 0x9E3779B97F4A7C15L + k * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h >>> 11) * 0x1.0p-53;
	}

	/** The same shape seen from behind (front and back swapped), for things that face away from you. */
	static VoxelModel.Shape flipZ(VoxelModel.Shape s, int nz) {
		return (x, y, z) -> s.at(x, y, nz - 1 - z);
	}

	/** True for a moment every few seconds, for blinking eyes. */
	static boolean blink(Cosmetic.Context ctx, double period) {
		return ctx.time() % period < 0.14;
	}

	// ---------------------------------------------------------------- animation helpers

	/** Both wings on the back, flapping: faster and wider while sprinting, flying or falling. */
	static void wings(Cosmetic.Context ctx, VoxelModel wing, double flapScale) {
		wings(ctx, wing, flapScale, 1);
	}

	static void wings(Cosmetic.Context ctx, VoxelModel wing, double flapScale, double speedScale) {
		var s = ctx.state();
		boolean active = s.isFallFlying || s.walkAnimationSpeed > 0.15f;
		double speed = (active ? 7.5 : 1.8) * speedScale;
		float flap = (float) (Math.sin(ctx.time() * speed) * (active ? 19 : 5) * flapScale);
		ctx.attach(ctx.model().body);
		ctx.pose().translate(0, 3f / 16f, 2.6f / 16f);
		for (int side = -1; side <= 1; side += 2) {
			ctx.pose().pushPose();
			ctx.pose().translate(side * 1f / 16f, 0, 0);
			// Swept back and slightly up, flapping around the hinge.
			float phase = side < 0 ? 0.22f : 0f;
			float localFlap = (float) (Math.sin(ctx.time() * speed + phase) * (active ? 3.5 : 1.2) * flapScale);
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees(side * (-28f - flap)));
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees(side * (-12f + localFlap)));
			if (side < 0) ctx.pose().scale(-1, 1, 1);
			ctx.draw(wing);
			ctx.pose().popPose();
		}
		ctx.pose().popPose();
	}

	/** A pet floats beside the player's shoulder, trailing a little behind as they move, and bobs. */
	static void pet(Cosmetic.Context ctx, Runnable draw) {
		var s = ctx.state();
		Vec3 offset = CosmeticManager.petOffset(s.id, s.x, s.y, s.z, s.bodyRot, ctx.time());
		ctx.pose().pushPose();
		ctx.model().root().translateAndRotate(ctx.pose());
		ctx.pose().translate((float) offset.x, (float) offset.y, (float) offset.z);
		ctx.pose().translate(0, (float) Math.sin(ctx.time() * 2.4) * 0.06f, 0);
		PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) Math.sin(ctx.time() * 0.9) * 25f));
		draw.run();
		ctx.pose().popPose();
	}

	/** Places one mote for this moment of its life (0..1) at a spot from a, b, c (random, 0..1); returns what to draw. */
	interface Mote {
		VoxelModel place(PoseStack pose, double life, double a, double b, double c, int i);
	}

	/**
	 * Little models drifting around the player (in model pixels from the neck, upright). Each mote
	 * lives for one cycle at a fresh random spot, then starts again somewhere else.
	 */
	static void motes(Cosmetic.Context ctx, int count, double rate, Mote mote) {
		ctx.pose().pushPose();
		ctx.model().root().translateAndRotate(ctx.pose());
		for (int i = 0; i < count; i++) {
			double cycle = ctx.time() * rate + i / (double) count;
			long n = (long) Math.floor(cycle);
			long seed = n * 7919 + i * 104729L;
			ctx.pose().pushPose();
			VoxelModel m = mote.place(ctx.pose(), cycle - n, rnd(seed, 1), rnd(seed, 2), rnd(seed, 3), i);
			if (m != null) ctx.draw(m);
			ctx.pose().popPose();
		}
		ctx.pose().popPose();
	}

	/** Moves the pose to (x, y, z) model pixels. */
	static void at(PoseStack pose, double x, double y, double z) {
		pose.translate((float) x / 16f, (float) y / 16f, (float) z / 16f);
	}

	/** Fades a mote in at the start of its life and out at the end, as a scale. */
	static float fade(double life, double edge) {
		return (float) Math.max(0, Math.min(1, Math.min(life, 1 - life) / edge));
	}
}
