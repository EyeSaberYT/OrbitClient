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
}
