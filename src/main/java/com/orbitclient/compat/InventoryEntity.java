package com.orbitclient.compat;

//? if <1.20 {
/*import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;

// Before 1.20: draws an entity in a menu at a given pose, like 1.20's InventoryScreen.renderEntityInInventory
// (the older game only had the look-at-the-mouse form). The wardrobe's preview uses it.
public final class InventoryEntity {
	private InventoryEntity() {
	}

	//? if >=1.19.3 {
	public static void render(PoseStack poseStack, int x, int y, int scale, org.joml.Quaternionf pose, org.joml.Quaternionf camera, LivingEntity entity) {
		poseStack.pushPose();
		poseStack.translate(x, y, 50.0);
		poseStack.mulPoseMatrix(new org.joml.Matrix4f().scaling(scale, scale, -scale));
		poseStack.mulPose(pose);
	//?} else {
	/^public static void render(PoseStack poseStack, int x, int y, int scale, com.mojang.math.Quaternion pose, com.mojang.math.Quaternion camera, LivingEntity entity) {
		poseStack.pushPose();
		poseStack.translate(x, y, 50.0);
		// On the matrix alone, like the JOML form above: the normals stay as they are.
		poseStack.last().pose().multiply(com.mojang.math.Matrix4f.createScaleMatrix(scale, scale, -scale));
		poseStack.mulPose(pose);
	^///?}
		//? if >=1.17
		Lighting.setupForEntityInInventory();
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		if (camera != null) {
			//? if >=1.19.3 {
			camera.conjugate();
			//?} else {
			/^camera.conj();
			^///?}
			dispatcher.overrideCameraOrientation(camera);
		}
		dispatcher.setRenderShadow(false);
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		//? if >=1.16 {
		RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0f, 1f, poseStack, buffers, 15728880));
		//?} else {
		/^dispatcher.render(entity, 0, 0, 0, 0f, 1f, poseStack, buffers, 15728880);
		^///?}
		buffers.endBatch();
		dispatcher.setRenderShadow(true);
		poseStack.popPose();
		Lighting.setupFor3DItems();
	}
}
*///?}
