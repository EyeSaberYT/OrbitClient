package com.orbitclient.compat;

//? if <1.21.2 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.Identifier;

/^*
 * Before 1.21.2 GUI textures took no color: the 1.21.2+ calls Orbit makes, tinted with the shader color
 * around the old call. build.gradle turns g.blit(...) and g.blitSprite(...) into these.
 ^/
public final class GuiBlit {
	private GuiBlit() {
	}

	public static void blit(GuiGraphics g, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
		tint(g, color);
		g.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
		untint(g);
	}

	public static void blit(GuiGraphics g, Identifier texture, int x, int y, float u, float v, int width, int height,
	                        int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
		tint(g, color);
		g.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
		untint(g);
	}

	public static void blitSprite(GuiGraphics g, Identifier sprite, int x, int y, int width, int height, int color) {
		tint(g, color);
		//? if >=1.20.2 {
		g.blitSprite(sprite, x, y, width, height);
		//?} else {
		/^// Before 1.20.2 there are no GUI sprites: the HUD ones Orbit draws are pieces of the HUD icons.
		int u = switch (sprite.getPath()) {
			case "hud/food_full" -> 52;
			case "hud/food_half" -> 61;
			default -> -1;
		};
		if (u >= 0) g.blit(new Identifier("textures/gui/icons.png"), x, y, u, 27, width, height);
		^///?}
		untint(g);
	}

	public static void blitSprite(GuiGraphics g, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
		tint(g, color);
		g.blit(x, y, 0, width, height, sprite);
		untint(g);
	}

	public static void face(GuiGraphics graphics, PlayerSkin skin, int x, int y, int size, int color) {
		tint(graphics, color);
		//? if >=1.20.2 {
		PlayerFaceRenderer.draw(graphics, skin, x, y, size);
		//?}
		//? if >=1.20 <1.20.2 {
		/^PlayerFaceRenderer.draw(graphics, skin.texture(), x, y, size);
		^///?}
		//? if <1.20 {
		/^// The face, then the hat layer over it (64x64 skin texture).
		graphics.blit(skin.texture(), x, y, size, size, 8f, 8f, 8, 8, 64, 64);
		graphics.blit(skin.texture(), x, y, size, size, 40f, 8f, 8, 8, 64, 64);
		^///?}
		untint(graphics);
	}

	private static void tint(GuiGraphics g, int argb) {
		g.setColor((argb >> 16 & 0xFF) / 255f, (argb >> 8 & 0xFF) / 255f, (argb & 0xFF) / 255f, (argb >>> 24) / 255f);
	}

	private static void untint(GuiGraphics g) {
		g.setColor(1f, 1f, 1f, 1f);
	}
}
*///?}
