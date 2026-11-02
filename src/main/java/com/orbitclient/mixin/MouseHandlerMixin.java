package com.orbitclient.mixin;

import com.orbitclient.module.mechanics.ZoomModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	/** While zooming, the scroll wheel changes the zoom instead of the hotbar slot. */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void orbit$scrollZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (ZoomModule.onScroll(vertical)) ci.cancel();
	}
}
