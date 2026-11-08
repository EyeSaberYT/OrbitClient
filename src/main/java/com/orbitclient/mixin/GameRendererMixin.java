package com.orbitclient.mixin;

import com.orbitclient.module.mechanics.ZoomModule;
import com.orbitclient.module.server.WaypointsModule;
import com.orbitclient.module.visual.MotionBlurModule;
import com.orbitclient.module.visual.NoHurtCamModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	// From 26.1 the camera works out the field of view: see CameraMixin.
	//? if <26.1 {
	/** useFovSetting is false for the held-item pass, which shouldn't zoom. */
	@ModifyReturnValue(method = "getFov", at = @At("RETURN"))
	private float orbit$zoom(float fov, @Local(argsOnly = true) boolean useFovSetting) {
		if (!useFovSetting) return fov;
		float result = ZoomModule.modifyFov(fov);
		WaypointsModule.setFov(result);
		return result;
	}
	//?}

	/** No hand or held item in the way while zoomed in. */
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void orbit$hideHandWhileZooming(CallbackInfo ci) {
		if (ZoomModule.hidesHand()) ci.cancel();
	}

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void orbit$noHurtCam(CallbackInfo ci) {
		if (NoHurtCamModule.active()) ci.cancel();
	}

	@Shadow
	@Final
	private CrossFrameResourcePool resourcePool;

	/** Motion blur runs on the finished world image, before the HUD is drawn on top. */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
	private void orbit$motionBlur(CallbackInfo ci) {
		MotionBlurModule.apply(resourcePool);
	}
}