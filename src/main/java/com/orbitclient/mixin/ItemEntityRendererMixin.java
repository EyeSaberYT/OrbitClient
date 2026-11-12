package com.orbitclient.mixin;

import com.orbitclient.module.visual.ItemPhysicsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
//? if >=1.21.2
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
//? if >=1.21.9
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
	@Shadow
	@Final
	private RandomSource random;

	/** Item Physics: draw dropped items lying on the ground instead of floating and spinning. */
	//? if >=1.21.9 {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
		at = @At("HEAD"), cancellable = true)
	private void orbit$itemPhysics(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (!ItemPhysicsModule.active()) return;
		ItemPhysicsModule.submit(state, pose, collector, state.lightCoords, random);
	//?}
	//? if >=1.21.2 <1.21.9 {
	/*@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("HEAD"), cancellable = true)
	private void orbit$itemPhysics(ItemEntityRenderState state, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
		if (!ItemPhysicsModule.active()) return;
		ItemPhysicsModule.submit(state, pose, new SubmitNodeCollector(buffers), light, random);
	*///?}
	//? if <1.21.2 {
	/*@Inject(method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("HEAD"), cancellable = true)
	private void orbit$itemPhysics(net.minecraft.world.entity.item.ItemEntity entity, float yaw, float partialTick, PoseStack pose,
	                               net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
		if (!ItemPhysicsModule.active()) return;
		ItemPhysicsModule.submit(entity, pose, new SubmitNodeCollector(buffers), light, random);
	*///?}
		ci.cancel();
	}
}
