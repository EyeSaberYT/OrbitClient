package com.orbitclient.compat;

//? if <1.20.5 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;

/^* Before 1.20.5 screens had no renderPanorama: the title screen's spinning panorama, drawn the same way. ^/
public final class Panorama {
	private static PanoramaRenderer renderer;

	private Panorama() {
	}

	public static void render(GuiGraphics graphics, float partialTick) {
		if (renderer == null) renderer = new PanoramaRenderer(TitleScreen.CUBE_MAP);
		renderer.render(partialTick, 1f);
	}
}
*///?}
