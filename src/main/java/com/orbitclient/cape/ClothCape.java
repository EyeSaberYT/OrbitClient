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

/** Lightweight, original Verlet cloth simulation for player capes. */
public final class ClothCape {
	public static final int COLUMNS = 9;
	public static final int ROWS = 17;
	private static final int POINTS = COLUMNS * ROWS;
	private static final float WIDTH = 10f / 16f;
	private static final float HEIGHT = 1f;
	private static final float REST_Z = 0.035f;
	private static final int SOLVER_PASSES = 9;
	private static final int FORGET_AFTER = 60;
	private static final Map<Integer, ClothCape> CAPES = new HashMap<>();

	// Current, previous and previous-tick positions. Each point is x/y/z in body-local blocks.
	private final float[] positions = new float[POINTS * 3];
	private final float[] previous = new float[POINTS * 3];
	private final float[] oldPositions = new float[POINTS * 3];
	private int age;
	private int lastDrawn;
	private double lastPosX, lastPosY, lastPosZ;
	private double lastDy;
	private float lastBodyYaw;
	private boolean hasLast;

	private ClothCape() {
		for (int row = 0; row < ROWS; row++) {
			float y = HEIGHT * row / (ROWS - 1);
			float z = REST_Z + (float) Math.sin(Math.toRadians(6)) * y;
			for (int column = 0; column < COLUMNS; column++) {
				int i = index(column, row);
				positions[i] = previous[i] = (column / (float) (COLUMNS - 1) - 0.5f) * WIDTH;
				positions[i + 1] = previous[i + 1] = y;
				positions[i + 2] = previous[i + 2] = z;
			}
		}
		System.arraycopy(positions, 0, oldPositions, 0, positions.length);
	}

	public static ClothCape of(int entityId) {
		ClothCape cape = CAPES.computeIfAbsent(entityId, id -> new ClothCape());
		cape.lastDrawn = cape.age;
		return cape;
	}

	public static void tickAll(Minecraft mc) {
		if (mc.level == null) {
			CAPES.clear();
			return;
		}
		if (mc.isPaused()) return;
		Iterator<Map.Entry<Integer, ClothCape>> it = CAPES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, ClothCape> entry = it.next();
			ClothCape cape = entry.getValue();
			Entity entity = mc.level.getEntity(entry.getKey());
			if (!(entity instanceof Player player) || cape.age - cape.lastDrawn > FORGET_AFTER) {
				it.remove();
				continue;
			}
			cape.step(player);
		}
	}

	private void step(Player player) {
		age++;
		System.arraycopy(positions, 0, oldPositions, 0, positions.length);

		double dx = hasLast ? player.getX() - lastPosX : 0;
		double dy = hasLast ? player.getY() - lastPosY : 0;
		double dz = hasLast ? player.getZ() - lastPosZ : 0;
		float turn = hasLast ? (float) Math.toRadians(Mth.wrapDegrees(player.yBodyRot - lastBodyYaw)) : 0;
		lastPosX = player.getX();
		lastPosY = player.getY();
		lastPosZ = player.getZ();
		lastBodyYaw = player.yBodyRot;
		boolean firstStep = !hasLast;
		hasLast = true;
		if (dx * dx + dy * dy + dz * dz > 4) {
			dx = dy = dz = 0;
			turn = 0;
			lastDy = 0;
			System.arraycopy(positions, 0, previous, 0, positions.length);
			firstStep = true;
		}

		if (firstStep) return;
		double yaw = Math.toRadians(player.yBodyRot);
		float forward = (float) (-dx * Math.sin(yaw) + dz * Math.cos(yaw));
		float sideways = (float) (dx * Math.cos(yaw) + dz * Math.sin(yaw));
		float gravity = WaveyCapesModule.gravity() / 25f;
		float height = WaveyCapesModule.heightMultiplier() / 6f;
		float strafe = WaveyCapesModule.strafeMultiplier() / 2f;
		float motionScale = WaveyCapesModule.motionScale();
		float lateralScale = WaveyCapesModule.lateralScale();
		float damping = WaveyCapesModule.damping();
		if (player.isInWater()) gravity *= 0.3f;
		float windZ = Mth.clamp(forward * 0.14f, -0.10f, 0.16f) * motionScale;
		float windX = Mth.clamp(-sideways * 0.12f * strafe - turn * 0.16f, -0.10f, 0.10f) * lateralScale;
		// Relative vertical drift makes the hem trail the player through the jump arc; the
		// acceleration term gives the launch and landing a sharper response without teleporting
		// the cloth or letting individual rows collapse together.
		float verticalInertia = (float) Mth.clamp(dy * 0.16 + (dy - lastDy) * 0.06, -0.05, 0.05)
			* height * motionScale;
		lastDy = dy;
		float speed = (float) Math.sqrt(dx * dx + dz * dz);
		float wave = ((WaveyCapesModule.windWaves() ? 0.008f : 0.002f) + Math.min(speed, 0.35f) * 0.018f)
			* WaveyCapesModule.rippleScale();

		for (int row = 1; row < ROWS; row++) {
			float depth = row / (float) (ROWS - 1);
			for (int column = 0; column < COLUMNS; column++) {
				int i = index(column, row);
				float x = positions[i], y = positions[i + 1], z = positions[i + 2];
				float vx = (x - previous[i]) * damping;
				float vy = (y - previous[i + 1]) * damping;
				float vz = (z - previous[i + 2]) * damping;
				previous[i] = x;
				previous[i + 1] = y;
				previous[i + 2] = z;
				float phase = age * 0.27f + row * 0.48f + column * 0.72f + player.getId() * 7.3f;
				float edgeWave = wave * (float) Math.sin(phase) * depth;
				positions[i] = x + vx + windX * depth;
				positions[i + 1] = y + vy + (0.018f * gravity * WaveyCapesModule.gravityScale() + verticalInertia) * depth;
				float arcTarget = WaveyCapesModule.dungeonArc() ? 0.12f * depth * depth : 0f;
				float arcRestoring = WaveyCapesModule.dungeonArc() ? (REST_Z + arcTarget - z) * 0.045f : 0f;
				positions[i + 2] = z + vz + (windZ + edgeWave) * depth + arcRestoring;
			}
		}

		float dxRest = WIDTH / (COLUMNS - 1);
		float dyRest = HEIGHT / (ROWS - 1);
		float diagonal = (float) Math.sqrt(dxRest * dxRest + dyRest * dyRest);
		float verticalBend = dyRest * 2f;
		for (int pass = 0; pass < SOLVER_PASSES; pass++) {
			pinTop();
			for (int row = 0; row < ROWS - 1; row++) {
				for (int column = 0; column < COLUMNS; column++) {
					constrain(index(column, row), index(column, row + 1), dyRest);
					if (column < COLUMNS - 1) {
						constrain(index(column, row), index(column + 1, row), dxRest);
						constrain(index(column, row), index(column + 1, row + 1), diagonal);
						constrain(index(column + 1, row), index(column, row + 1), diagonal);
					}
				}
			}
			for (int row = 1; row < ROWS; row++) {
				for (int column = 1; column < COLUMNS - 1; column++) {
					constrain(index(column - 1, row), index(column + 1, row), dxRest * 2);
				}
			}
			for (int row = 1; row < ROWS - 1; row++) {
				for (int column = 0; column < COLUMNS; column++) {
					constrain(index(column, row - 1), index(column, row + 1), verticalBend);
				}
			}
			collideWithBack();
		}
		pinTop();
	}

	private void constrain(int a, int b, float restLength) {
		float dx = positions[b] - positions[a];
		float dy = positions[b + 1] - positions[a + 1];
		float dz = positions[b + 2] - positions[a + 2];
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1e-7f) return;
		float correction = (length - restLength) / length;
		boolean aPinned = a / 3 < COLUMNS;
		boolean bPinned = b / 3 < COLUMNS;
		if (aPinned && bPinned) return;
		float share = aPinned ? 0f : bPinned ? 1f : 0.5f;
		float otherShare = bPinned ? 0f : aPinned ? 1f : 0.5f;
		positions[a] += dx * correction * share;
		positions[a + 1] += dy * correction * share;
		positions[a + 2] += dz * correction * share;
		positions[b] -= dx * correction * otherShare;
		positions[b + 1] -= dy * correction * otherShare;
		positions[b + 2] -= dz * correction * otherShare;
	}

	private void pinTop() {
		for (int column = 0; column < COLUMNS; column++) {
			int i = index(column, 0);
			positions[i] = previous[i] = (column / (float) (COLUMNS - 1) - 0.5f) * WIDTH;
			positions[i + 1] = previous[i + 1] = 0;
			positions[i + 2] = previous[i + 2] = REST_Z;
		}
	}

	private void collideWithBack() {
		for (int row = 1; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				int i = index(column, row);
				// Keep the fabric behind the avatar without imposing a preferred left-to-right or
				// top-to-bottom ordering: either ordering distorts folds and makes jumps look torn.
				if (positions[i + 2] < 0.018f) positions[i + 2] = 0.018f;
			}
		}
	}

	private static int index(int column, int row) {
		return (row * COLUMNS + column) * 3;
	}

	/** Returns the full cloth grid, interpolated between ticks. */
	public float[] points(float partialTick) {
		float[] result = new float[positions.length];
		for (int i = 0; i < result.length; i++) result[i] = Mth.lerp(partialTick, oldPositions[i], positions[i]);
		return result;
	}

	/** Flat fallback for vanilla capes when cloth physics is disabled. */
	public static float[] rigid(AvatarRenderState state) {
		float angle = (float) Math.toRadians(Mth.clamp(6f + state.capeLean / 2f + state.capeFlap, 0f, 150f));
		float[] grid = new float[POINTS * 3];
		for (int row = 0; row < ROWS; row++) {
			float y = HEIGHT * row / (ROWS - 1);
			for (int column = 0; column < COLUMNS; column++) {
				int i = index(column, row);
				grid[i] = (column / (float) (COLUMNS - 1) - 0.5f) * WIDTH;
				grid[i + 1] = y * (float) Math.cos(angle);
				grid[i + 2] = REST_Z + y * (float) Math.sin(angle);
			}
		}
		return grid;
	}
}
