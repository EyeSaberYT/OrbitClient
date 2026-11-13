package com.orbitclient.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Writes one lit, textured vertex, in whichever way this Minecraft version's vertex consumers take it. */
public final class Vertices {
	private Vertices() {
	}

	public static void add(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int color,
	                       float u, float v, int overlay, int light, float nx, float ny, float nz) {
		//? if >=1.21 {
		consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
		//?}
		//? if >=1.20.5 <1.21 {
		/*consumer.vertex(pose.pose(), x, y, z).color(color).uv(u, v).overlayCoords(overlay).uv2(light).normal(pose, nx, ny, nz).endVertex();
		*///?}
		//? if <1.20.5 {
		/*consumer.vertex(pose.pose(), x, y, z).color(color).uv(u, v).overlayCoords(overlay).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
		*///?}
	}

	/**
	 * One model being drawn: its corners already placed by the pose, and the six face directions already
	 * turned by it. Faces are then written a whole vertex at a time, which the game's buffers do in one go.
	 * One instance, reused: drawing happens on the render thread only.
	 */
	public static final class Batch {
		private VertexConsumer consumer;
		private float[] placed = new float[3 * 512];
		/** The six directions (-x, +x, -y, +y, -z, +z) as the pose turns them. */
		private final float[] normals = new float[18];

		private Batch() {
		}

		public void quad(int a, int b, int c, int d, int color, int light, int dir) {
			vertex(a, color, light, dir);
			vertex(b, color, light, dir);
			vertex(c, color, light, dir);
			vertex(d, color, light, dir);
		}

		private void vertex(int index, int color, int light, int dir) {
			int o = index * 3, n = dir * 3;
			//? if >=1.21 {
			consumer.addVertex(placed[o], placed[o + 1], placed[o + 2], color, 0.5f, 0.5f,
				net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, light, normals[n], normals[n + 1], normals[n + 2]);
			//?} else {
			/*consumer.vertex(placed[o], placed[o + 1], placed[o + 2], (color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f,
				(color >>> 24) / 255f, 0.5f, 0.5f, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, light, normals[n], normals[n + 1], normals[n + 2]);
			*///?}
		}
	}

	private static final Batch BATCH = new Batch();

	/**
	 * Starts drawing a model: places its count points (x y z each, times scale) with the pose, and turns the
	 * six face directions with it.
	 */
	public static Batch begin(VertexConsumer consumer, PoseStack.Pose pose, float[] points, int count, float scale) {
		Batch batch = BATCH;
		batch.consumer = consumer;
		if (batch.placed.length < count * 3) batch.placed = new float[count * 3];
		float[] placed = batch.placed, normals = batch.normals;
		// The pose as plain numbers: where the origin goes (t), and where one step along x, y and z goes.
		float xx, xy, xz, yx, yy, yz, zx, zy, zz, tx, ty, tz;
		float nxx, nxy, nxz, nyx, nyy, nyz, nzx, nzy, nzz;
		//? if >=1.19.3 {
		org.joml.Matrix4f m = pose.pose();
		xx = m.m00();
		xy = m.m01();
		xz = m.m02();
		yx = m.m10();
		yy = m.m11();
		yz = m.m12();
		zx = m.m20();
		zy = m.m21();
		zz = m.m22();
		tx = m.m30();
		ty = m.m31();
		tz = m.m32();
		org.joml.Matrix3f r = pose.normal();
		nxx = r.m00();
		nxy = r.m01();
		nxz = r.m02();
		nyx = r.m10();
		nyy = r.m11();
		nyz = r.m12();
		nzx = r.m20();
		nzy = r.m21();
		nzz = r.m22();
		//?} else {
		/*// Before 1.19.3 the matrices don't hand out their numbers: they are read by sending the axes through.
		com.mojang.math.Vector4f v = new com.mojang.math.Vector4f(0, 0, 0, 1);
		v.transform(pose.pose());
		tx = v.x();
		ty = v.y();
		tz = v.z();
		v = new com.mojang.math.Vector4f(1, 0, 0, 0);
		v.transform(pose.pose());
		xx = v.x();
		xy = v.y();
		xz = v.z();
		v = new com.mojang.math.Vector4f(0, 1, 0, 0);
		v.transform(pose.pose());
		yx = v.x();
		yy = v.y();
		yz = v.z();
		v = new com.mojang.math.Vector4f(0, 0, 1, 0);
		v.transform(pose.pose());
		zx = v.x();
		zy = v.y();
		zz = v.z();
		com.mojang.math.Vector3f d = new com.mojang.math.Vector3f(1, 0, 0);
		d.transform(pose.normal());
		nxx = d.x();
		nxy = d.y();
		nxz = d.z();
		d = new com.mojang.math.Vector3f(0, 1, 0);
		d.transform(pose.normal());
		nyx = d.x();
		nyy = d.y();
		nyz = d.z();
		d = new com.mojang.math.Vector3f(0, 0, 1);
		d.transform(pose.normal());
		nzx = d.x();
		nzy = d.y();
		nzz = d.z();
		*///?}
		for (int i = 0, o = 0; i < count; i++, o += 3) {
			float x = points[o] * scale, y = points[o + 1] * scale, z = points[o + 2] * scale;
			placed[o] = xx * x + yx * y + zx * z + tx;
			placed[o + 1] = xy * x + yy * y + zy * z + ty;
			placed[o + 2] = xz * x + yz * y + zz * z + tz;
		}
		direction(normals, 0, -nxx, -nxy, -nxz);
		direction(normals, 3, nxx, nxy, nxz);
		direction(normals, 6, -nyx, -nyy, -nyz);
		direction(normals, 9, nyx, nyy, nyz);
		direction(normals, 12, -nzx, -nzy, -nzz);
		direction(normals, 15, nzx, nzy, nzz);
		return batch;
	}

	/** Stores a direction at unit length (a scaled pose stretches it). */
	private static void direction(float[] into, int at, float x, float y, float z) {
		float length = (float) Math.sqrt(x * x + y * y + z * z);
		if (length > 1e-6f) {
			x /= length;
			y /= length;
			z /= length;
		}
		into[at] = x;
		into[at + 1] = y;
		into[at + 2] = z;
	}
}
