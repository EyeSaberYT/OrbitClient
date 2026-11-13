package com.orbitclient.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Menu icons, drawn sharp at any size.
 *
 * <p>Each icon ships pre-rendered at several pixel sizes (textures/gui/icons/SIZE/NAME.png). When
 * drawing, the size closest to the real on-screen pixel size is picked and placed on whole screen
 * pixels, so a 16-pixel icon at GUI scale 3 uses the 48-pixel image 1:1 instead of a stretched 32.
 * In-between sizes (e.g. while a node grows on hover) are scaled slightly with smooth filtering.</p>
 */
public final class Icons {
	/** Must match SIZES in tools/IconGenerator.java. */
	public static final int[] SIZES = {16, 20, 24, 28, 32, 36, 40, 48, 56, 64, 72, 80, 96};
	private static final Map<String, Identifier> LOADED = new HashMap<>();
	private static final Map<String, Boolean> EXISTS = new HashMap<>();

	private Icons() {
	}

	private static Identifier resource(int size, String name) {
		return Identifier.fromNamespaceAndPath("orbitclient", "textures/gui/icons/" + size + "/" + name + ".png");
	}

	/** Whether an icon with this name exists. */
	public static boolean exists(String name) {
		return EXISTS.computeIfAbsent(name, n -> Minecraft.getInstance().getResourceManager().getResource(resource(32, n)).isPresent());
	}

	/**
	 * Draws an icon with its top-left corner at (x, y) and the given size, all in GUI pixels.
	 * The color tints the white icon and is faded by Render2D's global alpha.
	 */
	public static void draw(GuiGraphics g, String name, float x, float y, float size, int color) {
		color = Render2D.a(color);
		if ((color >>> 24) <= 2 || size <= 0) return;
		int guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
		float pixels = size * guiScale;
		int texSize = SIZES[SIZES.length - 1];
		int best = Integer.MAX_VALUE;
		for (int s : SIZES) {
			// Prefer an exact match, otherwise the nearest size (downscaling a little looks better than upscaling).
			int diff = s >= pixels ? Math.round((s - pixels) * 1f) : Math.round((pixels - s) * 2f);
			if (diff < best) {
				best = diff;
				texSize = s;
			}
		}
		Identifier texture = load(texSize, name);
		if (texture == null) return;

		g.pose().pushMatrix();
		g.pose().scale(1f / guiScale, 1f / guiScale);
		g.pose().translate(Math.round(x * guiScale), Math.round(y * guiScale));
		float k = pixels / texSize;
		if (Math.abs(k - 1f) > 0.01f) g.pose().scale(k, k);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, texSize, texSize, texSize, texSize, color);
		g.pose().popMatrix();
	}

	/** Draws an icon centered on (cx, cy). */
	public static void drawCentered(GuiGraphics g, String name, float cx, float cy, float size, int color) {
		draw(g, name, cx - size / 2f, cy - size / 2f, size, color);
	}

	private static Identifier load(int size, String name) {
		String key = size + "/" + name;
		if (LOADED.containsKey(key)) return LOADED.get(key);
		Identifier id = null;
		Minecraft mc = Minecraft.getInstance();
		var resource = mc.getResourceManager().getResource(resource(size, name));
		if (resource.isPresent()) {
			try (InputStream in = resource.get().open()) {
				id = Identifier.fromNamespaceAndPath("orbitclient", "dynamic/icon/" + size + "/" + name);
				mc.getTextureManager().register(id, new SmoothTexture("icon " + key, NativeImage.read(in)));
			} catch (Exception e) {
				id = null;
			}
		}
		LOADED.put(key, id);
		return id;
	}
}
