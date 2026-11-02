package com.orbitclient.mixin;

import com.orbitclient.module.mechanics.FreelookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
	/** While freelooking, mouse movement turns the camera instead of the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void orbit$freelook(double yRot, double xRot, CallbackInfo ci) {
		if (FreelookModule.isActive() && (Object) this == Minecraft.getInstance().player) {
			FreelookModule.turn(yRot, xRot);
			ci.cancel();
		}
	}
}
