package com.orbitclient.mixin;

import com.orbitclient.module.mechanics.FreelookModule;
import com.orbitclient.module.mechanics.ZoomModule;
import com.orbitclient.module.server.WaypointsModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class CameraMixin {
	/** Point the camera where the freelook camera faces; third-person distance is computed after this. */
	//? if >=26.1 {
	/*@WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
	*///?} else
	@WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
	private void orbit$freelook(Camera camera, float yaw, float pitch, Operation<Void> original) {
		float[] rotation = FreelookModule.cameraRotation(yaw, pitch);
		if (rotation != null) original.call(camera, rotation[0], rotation[1]);
		else original.call(camera, yaw, pitch);
	}

	//? if >=26.1 {
	/*/^* Zoom from 26.1, where the camera works out the world's field of view (the held item's is calculateHudFov). *^/
	@ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
	private float orbit$zoom(float fov) {
		float result = ZoomModule.modifyFov(fov);
		WaypointsModule.setFov(result);
		return result;
	}
	*///?}
}
