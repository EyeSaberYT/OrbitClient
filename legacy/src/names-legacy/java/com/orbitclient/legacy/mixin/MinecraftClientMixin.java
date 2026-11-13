package com.orbitclient.legacy.mixin;

import com.orbitclient.legacy.OrbitLegacy;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	/** Once a game tick: cosmetics sync, key presses, the start-up audit. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void orbit$tick(CallbackInfo ci) {
		OrbitLegacy.tick((MinecraftClient) (Object) this);
	}
}
