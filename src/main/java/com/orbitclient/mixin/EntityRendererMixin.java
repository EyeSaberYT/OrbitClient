package com.orbitclient.mixin;

import com.orbitclient.cosmetics.CosmeticManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
//? if >=1.21.2
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	/** Cosmetics: lifts a player's nametag above a tall hat (crown, top hat, halo) instead of through it. */
	//? if >=1.21.2 {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
	private void orbit$liftNameTag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
		if (!(entity instanceof Player) || state.nameTagAttachment == null) return;
		float lift = CosmeticManager.nameTagLift(entity.getUUID());
		if (lift > 0) state.nameTagAttachment = state.nameTagAttachment.add(0, lift, 0);
	}
	//?}
	// Before 1.21.2 there's no render state to move the tag in: the tag is drawn moved up instead.
	//? if >=1.20.5 <1.21.2 {
	/*@com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "renderNameTag")
	private void orbit$liftNameTag(Entity entity, net.minecraft.network.chat.Component name, com.mojang.blaze3d.vertex.PoseStack pose,
	                               net.minecraft.client.renderer.MultiBufferSource buffers, int light, float partialTick,
	                               com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
		boolean lifted = lift(entity, pose);
		original.call(entity, name, pose, buffers, light, partialTick);
		if (lifted) pose.popPose();
	}
	*///?}
	//? if >=1.16 <1.20.5 {
	/*@com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "renderNameTag")
	private void orbit$liftNameTag(Entity entity, net.minecraft.network.chat.Component name, com.mojang.blaze3d.vertex.PoseStack pose,
	                               net.minecraft.client.renderer.MultiBufferSource buffers, int light,
	                               com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
		boolean lifted = lift(entity, pose);
		original.call(entity, name, pose, buffers, light);
		if (lifted) pose.popPose();
	}
	*///?}
	//? if <1.16 {
	/*// Before 1.16 the name is a plain string.
	@com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "renderNameTag")
	private void orbit$liftNameTag(Entity entity, String name, com.mojang.blaze3d.vertex.PoseStack pose,
	                               net.minecraft.client.renderer.MultiBufferSource buffers, int light,
	                               com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
		boolean lifted = lift(entity, pose);
		original.call(entity, name, pose, buffers, light);
		if (lifted) pose.popPose();
	}
	*///?}
	//? if <1.21.2 {
	/*/^* Moves the pose up over a player's tall hat (pushed; true when the caller must pop). ^/
	private static boolean lift(Entity entity, com.mojang.blaze3d.vertex.PoseStack pose) {
		float lift = entity instanceof Player ? CosmeticManager.nameTagLift(entity.getUUID()) : 0;
		if (lift <= 0) return false;
		pose.pushPose();
		pose.translate(0, lift, 0);
		return true;
	}
	*///?}
}
