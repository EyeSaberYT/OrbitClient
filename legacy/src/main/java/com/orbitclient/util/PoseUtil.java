package com.orbitclient.util;

import com.orbitclient.legacy.gl.Axis;
import com.orbitclient.legacy.gl.PoseStack;

/** Turns the pose, the way the shared cosmetic art asks for it. */
public final class PoseUtil {
	private PoseUtil() {
	}

	public static void mulPose(PoseStack pose, Axis.Rotation rotation) {
		pose.mulPose(rotation);
	}
}
