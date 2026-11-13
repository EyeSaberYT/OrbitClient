package com.orbitclient.mixin;

//? if <1.20.2 {
/*import com.orbitclient.compat.LegacyBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^* Before 1.20.2: draws Orbit screens' backgrounds first, like Screen.render does from 1.20.2 (see LegacyBackground). ^/
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
	//? if >=1.20 {
	@Inject(method = "renderWithTooltip", at = @At("HEAD"))
	private void orbit$background(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
		if ((Object) this instanceof LegacyBackground screen) screen.orbit$background(g, mx, my, delta);
	}
	//?} else {
	/^// Before 1.20 vanilla hands screens a PoseStack.
	@Inject(method = "renderWithTooltip", at = @At("HEAD"))
	private void orbit$background(com.mojang.blaze3d.vertex.PoseStack pose, int mx, int my, float delta, CallbackInfo ci) {
		if ((Object) this instanceof LegacyBackground screen) screen.orbit$background(GuiGraphics.of(pose), mx, my, delta);
	}
	^///?}
}
*///?}
