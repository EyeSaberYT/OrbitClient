package com.orbitclient.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {
	// Vanilla's menu blur: drawn through the GUI from 1.21.6, by itself in 1.21.2 to 1.21.5, with the frame's
	// partial tick in 1.20.5 to 1.21.1, and not in the game before 1.20.5.
	//? if >=1.21.6 {
	@Invoker("renderBlurredBackground")
	void orbit$renderBlurredBackground(GuiGraphics g);
	//?}
	//? if >=1.21.2 <1.21.6 {
	/*@Invoker("renderBlurredBackground")
	void orbit$renderBlurredBackground();
	*///?}
	//? if >=1.20.5 <1.21.2 {
	/*@Invoker("renderBlurredBackground")
	void orbit$renderBlurredBackground(float delta);
	*///?}
}
