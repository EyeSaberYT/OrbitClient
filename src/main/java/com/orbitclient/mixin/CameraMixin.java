package com.orbitclient.mixin;

import com.orbitclient.module.mechanics.FreelookModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class CameraMixin {
	/** Point the camera where the freelook camera faces; third-person distance is computed after this. */
	@WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
	private void orbit$freelook(Camera camera, float yaw, float pitch, Operation<Void> original) {
		float[] rotation = FreelookModule.cameraRotation(yaw, pitch);
		if (rotation != null) original.call(camera, rotation[0], rotation[1]);
		else original.call(camera, yaw, pitch);
	}
}
