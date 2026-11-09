package com.orbitclient.mixin;

import com.orbitclient.module.visual.MenuBlurModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// From 26.1 inventories use Screen's own background, which draws a plain tint instead of the blur for in-game screens.
//? if >=26.1 {
/*@Mixin(Screen.class)
*///?} else
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	/** Menu Blur: blur the world behind inventories, like vanilla does behind other menus. */
	//? if >=26.1 {
	/*// Written with 1.21.11's GuiGraphics, which the 26.x replacements turn into GuiGraphicsExtractor.
	@Inject(method = "extractBackground", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/Screen;extractTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V"))
	*///?} else
	@Inject(method = "renderBackground", at = @At("HEAD"))
	private void orbit$menuBlur(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
		if (MenuBlurModule.active() && (Object) this instanceof AbstractContainerScreen<?>) {
			((ScreenInvoker) this).orbit$renderBlurredBackground(g);
		}
	}
}
