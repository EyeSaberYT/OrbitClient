package com.orbitclient.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Builds a cape from its chain of points: a strip 10 pixels wide and 1 pixel thick that bends at
 * every point, textured from the standard 64x32 cape layout (outside 1..11, inside 12..22, edges and
 * hem around them). UVs are fractions, so HD capes work too.
 *
 * <p>Smooth style gives each row the average normal of the segments on either side, so light rolls
 * across the folds; Blocky gives each segment its own flat normal, like stacked plates.
 */
public final class CapeMesh {
	private static final float HALF_WIDTH = 5f / 16f;
	private static final float THICKNESS = 1f / 16f;

	private CapeMesh() {
	}

	/** @param points {x0, y0, z0, x1, ...} in the body frame, from {@link ClothCape} */
	public static void draw(PoseStack.Pose pose, VertexConsumer vc, float[] points, int light, boolean smooth) {
		int rows = points.length / 3, segs = rows - 1;
		// Each row's normal: perpendicular to the cape in its own y-z plane, pointing away from the back.
		float[] ny = new float[rows], nz = new float[rows];
		for (int i = 0; i < rows; i++) {
			int a = Math.max(0, i - 1), b = Math.min(segs, i + 1);
			float ty = points[b * 3 + 1] - points[a * 3 + 1], tz = points[b * 3 + 2] - points[a * 3 + 2];
			float len = (float) Math.sqrt(ty * ty + tz * tz);
			if (len < 1e-6f) {
				ty = 1;
				len = 1;
			}
			ny[i] = -tz / len;
			nz[i] = ty / len;
		}

		for (int i = 0; i < segs; i++) {
			int j = i + 1;
			float x0 = points[i * 3], y0 = points[i * 3 + 1], z0 = points[i * 3 + 2];
			float x1 = points[j * 3], y1 = points[j * 3 + 1], z1 = points[j * 3 + 2];
			// Normals for the two rows of this segment: shared between segments when smooth.
			float ny0 = ny[i], nz0 = nz[i], ny1 = ny[j], nz1 = nz[j];
			if (!smooth) {
				float ty = y1 - y0, tz = z1 - z0, len = Math.max(1e-6f, (float) Math.sqrt(ty * ty + tz * tz));
				ny0 = ny1 = -tz / len;
				nz0 = nz1 = ty / len;
			}
			// Outer surface sits one pixel out from the inner one, along the normal.
			float oy0 = y0 + ny0 * THICKNESS, oz0 = z0 + nz0 * THICKNESS, oy1 = y1 + ny1 * THICKNESS, oz1 = z1 + nz1 * THICKNESS;
			float v0 = (1 + 16f * i / segs) / 32f, v1 = (1 + 16f * j / segs) / 32f;

			// Outside (seen from behind the player).
			quad(vc, pose, light,
				x0 - HALF_WIDTH, oy0, oz0, 11 / 64f, v0, 0, ny0, nz0,
				x0 + HALF_WIDTH, oy0, oz0, 1 / 64f, v0, 0, ny0, nz0,
				x1 + HALF_WIDTH, oy1, oz1, 1 / 64f, v1, 0, ny1, nz1,
				x1 - HALF_WIDTH, oy1, oz1, 11 / 64f, v1, 0, ny1, nz1);
			// Inside (against the back).
			quad(vc, pose, light,
				x0 + HALF_WIDTH, y0, z0, 22 / 64f, v0, 0, -ny0, -nz0,
				x0 - HALF_WIDTH, y0, z0, 12 / 64f, v0, 0, -ny0, -nz0,
				x1 - HALF_WIDTH, y1, z1, 12 / 64f, v1, 0, -ny1, -nz1,
				x1 + HALF_WIDTH, y1, z1, 22 / 64f, v1, 0, -ny1, -nz1);
			// The two side edges.
			quad(vc, pose, light,
				x0 - HALF_WIDTH, y0, z0, 12 / 64f, v0, -1, 0, 0,
				x0 - HALF_WIDTH, oy0, oz0, 11 / 64f, v0, -1, 0, 0,
				x1 - HALF_WIDTH, oy1, oz1, 11 / 64f, v1, -1, 0, 0,
				x1 - HALF_WIDTH, y1, z1, 12 / 64f, v1, -1, 0, 0);
			quad(vc, pose, light,
				x0 + HALF_WIDTH, oy0, oz0, 1 / 64f, v0, 1, 0, 0,
				x0 + HALF_WIDTH, y0, z0, 0, v0, 1, 0, 0,
				x1 + HALF_WIDTH, y1, z1, 0, v1, 1, 0, 0,
				x1 + HALF_WIDTH, oy1, oz1, 1 / 64f, v1, 1, 0, 0);
		}

		// Top edge at the shoulders and the hem at the bottom.
		float tx = points[0], ty = points[1], tz = points[2];
		quad(vc, pose, light,
			tx - HALF_WIDTH, ty, tz, 11 / 64f, 1 / 32f, 0, -1, 0,
			tx + HALF_WIDTH, ty, tz, 1 / 64f, 1 / 32f, 0, -1, 0,
			tx + HALF_WIDTH, ty + ny[0] * THICKNESS, tz + nz[0] * THICKNESS, 1 / 64f, 0, 0, -1, 0,
			tx - HALF_WIDTH, ty + ny[0] * THICKNESS, tz + nz[0] * THICKNESS, 11 / 64f, 0, 0, -1, 0);
		int e = segs * 3;
		float bx = points[e], by = points[e + 1], bz = points[e + 2];
		float hy = by - points[e - 2], hz = bz - points[e - 1], hl = Math.max(1e-6f, (float) Math.sqrt(hy * hy + hz * hz));
		quad(vc, pose, light,
			bx - HALF_WIDTH, by + ny[segs] * THICKNESS, bz + nz[segs] * THICKNESS, 21 / 64f, 0, 0, hy / hl, hz / hl,
			bx + HALF_WIDTH, by + ny[segs] * THICKNESS, bz + nz[segs] * THICKNESS, 11 / 64f, 0, 0, hy / hl, hz / hl,
			bx + HALF_WIDTH, by, bz, 11 / 64f, 1 / 32f, 0, hy / hl, hz / hl,
			bx - HALF_WIDTH, by, bz, 21 / 64f, 1 / 32f, 0, hy / hl, hz / hl);
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light,
							 float x0, float y0, float z0, float u0, float v0, float nx0, float ny0, float nz0,
							 float x1, float y1, float z1, float u1, float v1, float nx1, float ny1, float nz1,
							 float x2, float y2, float z2, float u2, float v2, float nx2, float ny2, float nz2,
							 float x3, float y3, float z3, float u3, float v3, float nx3, float ny3, float nz3) {
		vertex(vc, pose, light, x0, y0, z0, u0, v0, nx0, ny0, nz0);
		vertex(vc, pose, light, x1, y1, z1, u1, v1, nx1, ny1, nz1);
		vertex(vc, pose, light, x2, y2, z2, u2, v2, nx2, ny2, nz2);
		vertex(vc, pose, light, x3, y3, z3, u3, v3, nx3, ny3, nz3);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int light, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
		vc.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
	}
}
