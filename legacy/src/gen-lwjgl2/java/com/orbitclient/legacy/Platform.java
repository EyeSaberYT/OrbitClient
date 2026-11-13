package com.orbitclient.legacy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;

/**
 * What differs between engine generations: input, plain text, textures made at run time and how the mod's
 * own assets reach the game. This one is for Minecraft 1.8.9 to 1.12.2 (LWJGL 2).
 */
public final class Platform {
	private Platform() {
	}

	/** Right Shift, which opens the wardrobe. */
	public static boolean wardrobeKeyDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	/** Text without a shadow. */
	public static void text(TextRenderer renderer, String text, int x, int y, int color) {
		renderer.draw(text, x, y, color);
	}

	/** A square texture made from ARGB pixels (a cosmetic's picture in the wardrobe). */
	public static Identifier iconTexture(MinecraftClient mc, int[] argb, int size) {
		NativeImageBackedTexture texture = new NativeImageBackedTexture(size, size);
		System.arraycopy(argb, 0, texture.getPixels(), 0, Math.min(argb.length, size * size));
		texture.upload();
		return mc.getTextureManager().registerDynamicTexture("orbit_icon", texture);
	}

	/** Makes sure one of the mod's textures can be bound. Here the mod's resource pack already serves it. */
	public static void ensureTexture(MinecraftClient mc, Identifier id) {
	}

	/** For the screenshot audit: no pause menu when the window isn't focused, and no HUD in the pictures. */
	public static void auditSetup(MinecraftClient mc) {
		mc.options.pauseOnLostFocus = false;
		mc.options.hudHidden = true;
	}
}
