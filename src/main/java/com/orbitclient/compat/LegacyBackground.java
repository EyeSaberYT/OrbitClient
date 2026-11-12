package com.orbitclient.compat;

//? if <1.20.2 {
/*import net.minecraft.client.gui.GuiGraphics;

/^*
 * Before 1.20.2 Screen.render didn't draw a background, so Orbit's screens' renderBackground(g, mx, my, delta)
 * becomes this, drawn by ScreenBackgroundMixin before the screen (build.gradle renames it and adds the interface).
 ^/
public interface LegacyBackground {
	void orbit$background(GuiGraphics g, int mx, int my, float delta);
}
*///?}
