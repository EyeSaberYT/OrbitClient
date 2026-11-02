package com.orbitclient.mixin;

import com.orbitclient.module.visual.MenuBlurModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	/** Menu Blur: blur the world behind inventories, like vanilla does behind other menus. */
	@Inject(method = "renderBackground", at = @At("HEAD"))
	private void orbit$menuBlur(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
		if (MenuBlurModule.active()) ((ScreenInvoker) this).orbit$renderBlurredBackground(g);
	}
}
