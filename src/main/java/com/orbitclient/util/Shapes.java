package com.orbitclient.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Smooth circles, rings and arcs.
 *
 * <p>Each shape is drawn once into a small anti-aliased white texture (cached by size, thickness and
 * sweep), then drawn every frame as a single tinted, scaled and rotated quad with smooth filtering.
 * Drawing round shapes from thousands of tiny rectangles every frame is what made menus lag.</p>
 */
public final class Shapes {
	private static final float TAU = (float) (Math.PI * 2);
	private static final int[] SIZES = {32, 64, 128, 256};
	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private static long generationNs;

	/** How many shape textures exist and the total time spent creating them (for performance checks). */
	public static String stats() {
		return CACHE.size() + " shape textures, " + generationNs / 1_000_000 + " ms total to generate";
	}

	private Shapes() {
	}

	public static void disc(GuiGraphics g, float cx, float cy, float r, int color) {
		arc(g, cx, cy, 0, r, 0, TAU, color);
	}

	public static void ring(GuiGraphics g, float cx, float cy, float rIn, float rOut, int color) {
		arc(g, cx, cy, rIn, rOut, 0, TAU, color);
	}

	/** Part of a ring between two angles (radians; 0 = right, increasing clockwise on screen). Coordinates in GUI pixels. */
	public static void arc(GuiGraphics g, float cx, float cy, float rIn, float rOut, float start, float end, int color) {
		color = Render2D.a(color);
		if ((color >>> 24) == 0 || rOut <= 0.3f) return;
		float sweep = Math.min(TAU, end - start);
		boolean full = sweep >= TAU - 1e-3f;

		int guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
		int size = SIZES[SIZES.length - 1];
		for (int s : SIZES) {
			if (s >= rOut * 2 * guiScale) {
				size = s;
				break;
			}
		}
		// Quantize so a handful of textures cover every animated variation.
		int inner = Math.round(Math.max(0, rIn) / rOut * 100);
		int sweepDeg = full ? 360 : Math.max(1, Math.round((float) Math.toDegrees(sweep)));
		Identifier texture = texture(size, inner, sweepDeg);

		float radiusPx = size / 2f - 1;
		float k = rOut / radiusPx;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		if (!full) g.pose().rotate(start);
		g.pose().scale(k, k);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, -size / 2, -size / 2, 0f, 0f, size, size, size, size, color);
		g.pose().popMatrix();
	}

	private static Identifier texture(int size, int innerPercent, int sweepDeg) {
		String key = size + "_" + innerPercent + "_" + sweepDeg;
		Identifier id = CACHE.get(key);
		if (id != null) return id;

		long startNs = System.nanoTime();
		NativeImage image = new NativeImage(size, size, false);
		float c = size / 2f, outer = size / 2f - 1, innerR = outer * innerPercent / 100f;
		double sweep = Math.toRadians(sweepDeg);
		boolean full = sweepDeg >= 360;
		// Anti-aliasing from the distance to each edge (about one pixel of soft edge), computed directly
		// per pixel instead of supersampling, so even large shapes take well under a millisecond.
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				float px = x + 0.5f - c, py = y + 0.5f - c;
				float d = (float) Math.sqrt(px * px + py * py);
				float cover = clamp01(outer - d + 0.5f);
				if (innerR > 0) cover *= clamp01(d - innerR + 0.5f);
				if (!full && cover > 0) {
					double a = Math.atan2(py, px);
					if (a < 0) a += Math.PI * 2;
					// Pixel distance to the nearest end of the arc (radius x angle is exact enough at the edges),
					// positive inside the arc and negative outside.
					double inside = a <= sweep ? Math.min(a, sweep - a) : -Math.min(a - sweep, Math.PI * 2 - a);
					cover *= clamp01((float) (inside * d) + 0.5f);
				}
				image.setPixel(x, y, (Math.round(cover * 255) << 24) | 0xFFFFFF);
			}
		}
		id = Identifier.fromNamespaceAndPath("orbitclient", "dynamic/shape_" + key);
		Minecraft.getInstance().getTextureManager().register(id, new SmoothTexture("shape " + key, image));
		CACHE.put(key, id);
		generationNs += System.nanoTime() - startNs;
		return id;
	}

	private static float clamp01(float v) {
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}

	/** Small square dots in screen pixels, e.g. stars. Each entry of xs/ys is in GUI pixels. */
	public static void dots(GuiGraphics g, float[] xs, float[] ys, int[] sizes, int[] colors) {
		int s = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		for (int i = 0; i < xs.length; i++) {
			int c = Render2D.a(colors[i]);
			if ((c >>> 24) == 0) continue;
			int x = Math.round(xs[i] * s), y = Math.round(ys[i] * s);
			g.fill(x, y, x + sizes[i], y + sizes[i], c);
		}
		g.pose().popMatrix();
	}
}
