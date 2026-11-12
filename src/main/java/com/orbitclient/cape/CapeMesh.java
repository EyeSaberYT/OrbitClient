package com.orbitclient.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders the simulated cloth grid with smooth normals and the standard cape texture layout. */
public final class CapeMesh {
	private static final float THICKNESS = 0.0015f;

	private CapeMesh() {
	}

	/** @param points body-local x/y/z positions, row-major, from {@link ClothCape} */
	public static void draw(PoseStack.Pose pose, VertexConsumer vc, float[] points, int light, boolean smooth) {
		int columns = ClothCape.COLUMNS;
		int rows = ClothCape.ROWS;
		float[] normals = new float[points.length];
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				int left = point(points, Math.max(0, column - 1), row, columns);
				int right = point(points, Math.min(columns - 1, column + 1), row, columns);
				int up = point(points, column, Math.max(0, row - 1), columns);
				int down = point(points, column, Math.min(rows - 1, row + 1), columns);
				float ax = points[right] - points[left], ay = points[right + 1] - points[left + 1], az = points[right + 2] - points[left + 2];
				float bx = points[down] - points[up], by = points[down + 1] - points[up + 1], bz = points[down + 2] - points[up + 2];
				float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
				float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (length < 1e-7f) {
					nx = 0; ny = 0; nz = 1; length = 1;
				}
				int n = point(normals, column, row, columns);
				normals[n] = nx / length;
				normals[n + 1] = ny / length;
				normals[n + 2] = nz / length;
			}
		}

		for (int row = 0; row < rows - 1; row++) {
			float v0 = (1f + 16f * row / (rows - 1)) / 32f;
			float v1 = (1f + 16f * (row + 1) / (rows - 1)) / 32f;
			for (int column = 0; column < columns - 1; column++) {
				float uLeft = (11f - 10f * column / (columns - 1)) / 64f;
				float uRight = (11f - 10f * (column + 1) / (columns - 1)) / 64f;
				int tl = point(points, column, row, columns);
				int tr = point(points, column + 1, row, columns);
				int br = point(points, column + 1, row + 1, columns);
				int bl = point(points, column, row + 1, columns);
				float faceX = normals[tl], faceY = normals[tl + 1], faceZ = normals[tl + 2];
				if (!smooth) {
					float ax = points[tr] - points[tl], ay = points[tr + 1] - points[tl + 1], az = points[tr + 2] - points[tl + 2];
					float bx = points[bl] - points[tl], by = points[bl + 1] - points[tl + 1], bz = points[bl + 2] - points[tl + 2];
					faceX = ay * bz - az * by;
					faceY = az * bx - ax * bz;
					faceZ = ax * by - ay * bx;
					float length = Math.max(1e-7f, (float) Math.sqrt(faceX * faceX + faceY * faceY + faceZ * faceZ));
					faceX /= length; faceY /= length; faceZ /= length;
				}
				emitFace(vc, pose, light, points, normals, tl, tr, br, bl, uLeft, uRight, v0, v1,
					faceX, faceY, faceZ, smooth, 1f);
				// The cape's inner texture panel uses the adjacent 12..22 UV region.
				emitFace(vc, pose, light, points, normals, tl, tr, br, bl,
					(22f - 10f * column / (columns - 1)) / 64f,
					(22f - 10f * (column + 1) / (columns - 1)) / 64f,
					v0, v1, faceX, faceY, faceZ, smooth, -1f);
			}
		}
	}

	private static int point(float[] points, int column, int row, int columns) {
		return (row * columns + column) * 3;
	}

	private static void emitFace(VertexConsumer vc, PoseStack.Pose pose, int light,
							  float[] points, float[] normals, int tl, int tr, int br, int bl,
							  float uLeft, float uRight, float v0, float v1,
							  float faceX, float faceY, float faceZ, boolean smooth, float side) {
		emitVertex(vc, pose, light, points, normals, tl, uLeft, v0, faceX, faceY, faceZ, smooth, side);
		emitVertex(vc, pose, light, points, normals, tr, uRight, v0, faceX, faceY, faceZ, smooth, side);
		emitVertex(vc, pose, light, points, normals, br, uRight, v1, faceX, faceY, faceZ, smooth, side);
		emitVertex(vc, pose, light, points, normals, bl, uLeft, v1, faceX, faceY, faceZ, smooth, side);
	}

	private static void emitVertex(VertexConsumer vc, PoseStack.Pose pose, int light,
								   float[] points, float[] normals, int point, float u, float v,
								   float faceX, float faceY, float faceZ, boolean smooth, float side) {
		float nx = (smooth ? normals[point] : faceX) * side;
		float ny = (smooth ? normals[point + 1] : faceY) * side;
		float nz = (smooth ? normals[point + 2] : faceZ) * side;
		vertex(vc, pose, light,
			points[point] + nx * THICKNESS,
			points[point + 1] + ny * THICKNESS,
			points[point + 2] + nz * THICKNESS,
			u, v, nx, ny, nz);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int light,
								float x, float y, float z, float u, float v, float nx, float ny, float nz) {
		com.orbitclient.util.Vertices.add(vc, pose, x, y, z, 0xFFFFFFFF, u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
	}
}
