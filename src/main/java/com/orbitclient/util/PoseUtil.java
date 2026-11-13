package com.orbitclient.util;

import com.mojang.blaze3d.vertex.PoseStack;

/** Adapts cosmetic rotations to the pose-stack signatures used by supported game versions. */
public final class PoseUtil {
	private PoseUtil() {
	}

	//? if >=1.19.3 {
	public static void mulPose(PoseStack pose, org.joml.Quaternionf rotation) {
		pose.mulPose(rotation);
	}
	//?} else {
	/*public static void mulPose(PoseStack pose, com.mojang.math.Quaternion rotation) {
		pose.mulPose(rotation);
	}
	*///?}
}
