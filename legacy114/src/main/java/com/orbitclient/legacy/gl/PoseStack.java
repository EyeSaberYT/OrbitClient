package com.orbitclient.legacy.gl;

import com.mojang.blaze3d.platform.GlStateManager;

/**
 * The pose stack the shared cosmetic art is written against. On the old engine it is OpenGL's own
 * matrix stack: every call goes straight to it, so whatever is drawn next is already in place.
 */
public final class PoseStack {
	/** The current matrix. Nothing reads it here (OpenGL holds it); it only fills the shared signatures. */
	public static final class Pose {
		private Pose() {
		}
	}

	private static final Pose POSE = new Pose();
	public static final PoseStack GL = new PoseStack();

	private PoseStack() {
	}

	public void pushPose() {
		GlStateManager.pushMatrix();
	}

	public void popPose() {
		GlStateManager.popMatrix();
	}

	public void translate(float x, float y, float z) {
		GlStateManager.translatef(x, y, z);
	}

	public void translate(double x, double y, double z) {
		GlStateManager.translated(x, y, z);
	}

	public void scale(float x, float y, float z) {
		GlStateManager.scalef(x, y, z);
	}

	public void mulPose(Axis.Rotation rotation) {
		GlStateManager.rotatef(rotation.degrees(), rotation.x(), rotation.y(), rotation.z());
	}

	public Pose last() {
		return POSE;
	}
}
