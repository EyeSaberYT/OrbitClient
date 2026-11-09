package com.orbitclient.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

/** Adapts cosmetic rotations to the pose-stack signatures used by supported game versions. */
public final class PoseUtil {
	private PoseUtil() {
	}

	public static void mulPose(PoseStack pose, Quaternionf rotation) {
		pose.mulPose(rotation);
	}
}
