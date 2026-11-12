package com.orbitclient.mixin;

import com.orbitclient.module.visual.LowFireModule;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	// renderFire takes the fire sprite from 1.21.9, the buffers from 1.21.4, and the game before that.

	/** Low Fire: move the burning overlay down, then back so nothing after it is affected. */
	@Inject(method = "renderFire", at = @At("HEAD"))
	//? if >=1.21.9 {
	private static void orbit$lowFireStart(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
	//?}
	//? if >=1.21.4 <1.21.9 {
	/*private static void orbit$lowFireStart(PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
	*///?}
	//? if <1.21.4 {
	/*private static void orbit$lowFireStart(net.minecraft.client.Minecraft mc, PoseStack pose, CallbackInfo ci) {
	*///?}
		float offset = LowFireModule.offset();
		if (offset > 0) pose.translate(0f, -offset, 0f);
	}

	@Inject(method = "renderFire", at = @At("TAIL"))
	//? if >=1.21.9 {
	private static void orbit$lowFireEnd(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
	//?}
	//? if >=1.21.4 <1.21.9 {
	/*private static void orbit$lowFireEnd(PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
	*///?}
	//? if <1.21.4 {
	/*private static void orbit$lowFireEnd(net.minecraft.client.Minecraft mc, PoseStack pose, CallbackInfo ci) {
	*///?}
		float offset = LowFireModule.offset();
		if (offset > 0) pose.translate(0f, offset, 0f);
	}
}
