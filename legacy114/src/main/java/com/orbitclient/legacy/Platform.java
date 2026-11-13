package com.orbitclient.legacy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;

/** Input, plain text, run-time textures and the mod's own assets, for Minecraft 1.14 (LWJGL 3). */
public final class Platform {
	private Platform() {
	}

	/** Right Shift, which opens the wardrobe. */
	public static boolean wardrobeKeyDown() {
		long window = GLFW.glfwGetCurrentContext();
		return window != 0 && GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
	}

	/** Text without a shadow. */
	public static void text(TextRenderer renderer, String text, int x, int y, int color) {
		renderer.draw(text, x, y, color);
	}

	/** A square texture made from ARGB pixels (a cosmetic's picture in the wardrobe). */
	public static Identifier iconTexture(MinecraftClient mc, int[] argb, int size) {
		NativeImageBackedTexture texture = new NativeImageBackedTexture(size, size, true);
		NativeImage image = texture.getImage();
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int c = argb[y * size + x];
				// Native images hold ABGR: red and blue change places.
				image.setPixelRgba(x, y, (c & 0xFF00FF00) | (c >> 16 & 0xFF) | (c & 0xFF) << 16);
			}
		}
		texture.upload();
		return mc.getTextureManager().registerDynamicTexture("orbit_icon", texture);
	}

	/**
	 * Makes sure one of the mod's textures can be bound: they are read straight from the mod's jar and handed
	 * to the texture manager on first use, so this client needs no resource-pack plumbing (or Fabric API).
	 */
	public static void ensureTexture(MinecraftClient mc, Identifier id) {
		if (!OrbitLegacy.MOD_ID.equals(id.getNamespace()) || mc.getTextureManager().getTexture(id) != null) return;
		try (InputStream in = Platform.class.getResourceAsStream("/assets/" + id.getNamespace() + "/" + id.getPath())) {
			if (in == null) return;
			mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(NativeImage.read(in)));
		} catch (Exception e) {
			System.err.println("[Orbit Client] Couldn't load " + id + ": " + e);
		}
	}

	/** For the screenshot audit: no pause menu when the window isn't focused, and no HUD in the pictures. */
	public static void auditSetup(MinecraftClient mc) {
		mc.options.pauseOnLostFocus = false;
		mc.options.hudHidden = true;
	}
}
