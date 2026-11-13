package com.orbitclient.util;

import com.orbitclient.legacy.gl.PoseStack;
import com.orbitclient.legacy.gl.VertexConsumer;

/** Where the shared voxel code sends its vertices. On the old engine OpenGL already holds the pose. */
public final class Vertices {
	private Vertices() {
	}

	public static void add(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int color,
	                       float u, float v, int overlay, int light, float nx, float ny, float nz) {
		consumer.add(x, y, z, color, u, v, light, nx, ny, nz);
	}

	private static final float[][] DIRECTIONS = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};

	/** One model being drawn (see the main client's Vertices.Batch): here the points only need scaling. */
	public static final class Batch {
		private VertexConsumer consumer;
		private float[] points;
		private float scale;

		private Batch() {
		}

		public void quad(int a, int b, int c, int d, int color, int light, int dir) {
			vertex(a, color, light, dir);
			vertex(b, color, light, dir);
			vertex(c, color, light, dir);
			vertex(d, color, light, dir);
		}

		private void vertex(int index, int color, int light, int dir) {
			int o = index * 3;
			float[] n = DIRECTIONS[dir];
			consumer.add(points[o] * scale, points[o + 1] * scale, points[o + 2] * scale, color, 0.5f, 0.5f, light, n[0], n[1], n[2]);
		}
	}

	private static final Batch BATCH = new Batch();

	public static Batch begin(VertexConsumer consumer, PoseStack.Pose pose, float[] points, int count, float scale) {
		BATCH.consumer = consumer;
		BATCH.points = points;
		BATCH.scale = scale;
		return BATCH;
	}
}
