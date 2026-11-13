package com.orbitclient.legacy;

import net.minecraft.class_4277;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;

/**
 * What differs between engine generations: input, plain text, textures made at run time and how the mod's
 * own assets reach the game. This one is for Minecraft 1.13.2 (LWJGL 3). Its mappings leave much unnamed:
 * class_4277 is the game's native image.
 */
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
		renderer.method_18355(text, x, y, color);
	}

	/** A square texture made from ARGB pixels (a cosmetic's picture in the wardrobe). */
	public static Identifier iconTexture(MinecraftClient mc, int[] argb, int size) {
		NativeImageBackedTexture texture = new NativeImageBackedTexture(size, size, true);
		class_4277 image = texture.method_19449();
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int c = argb[y * size + x];
				// Native images hold ABGR: red and blue change places.
				image.method_19460(x, y, (c & 0xFF00FF00) | (c >> 16 & 0xFF) | (c & 0xFF) << 16);
			}
		}
		texture.upload();
		return mc.getTextureManager().registerDynamicTexture("orbit_icon", texture);
	}

	/**
	 * Makes sure one of the mod's textures can be bound. 1.13 has a different resource-pack system, so the
	 * mod's textures are read straight from its jar and handed to the texture manager on first use.
	 */
	public static void ensureTexture(MinecraftClient mc, Identifier id) {
		if (!OrbitLegacy.MOD_ID.equals(id.getNamespace()) || mc.getTextureManager().getTexture(id) != null) return;
		try (InputStream in = Platform.class.getResourceAsStream("/assets/" + id.getNamespace() + "/" + id.getPath())) {
			if (in == null) return;
			mc.getTextureManager().loadTexture(id, new NativeImageBackedTexture(class_4277.method_19472(in)));
		} catch (Exception e) {
			System.err.println("[Orbit Client] Couldn't load " + id + ": " + e);
		}
	}

	/** For the screenshot audit: no pause menu when the window isn't focused. */
	public static void auditSetup(MinecraftClient mc) {
		mc.options.field_19973 = false; // pauseOnLostFocus
	}
}
