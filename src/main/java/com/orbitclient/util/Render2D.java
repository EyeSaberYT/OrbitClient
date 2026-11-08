package com.orbitclient.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Drawing helpers on top of GuiGraphics: rounded rectangles, color math, and a global alpha.
 * Everything drawn through these helpers is multiplied by {@link #alpha}, which is how menus
 * and HUD mods fade in and out (vanilla GuiGraphics has no global opacity).
 */
public final class Render2D {
	/** Opacity applied to everything drawn through this class. Always restore it to 1 after use. */
	public static float alpha = 1f;

	private Render2D() {
	}

	/** Applies the global alpha to a color. */
	public static int a(int color) {
		if (alpha >= 1f) return color;
		int na = Math.round((color >>> 24) * Math.max(0f, alpha));
		return (na << 24) | (color & 0xFFFFFF);
	}

	private static boolean invisible(int color) {
		return (color >>> 24) == 0;
	}

	public static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		color = a(color);
		if (!invisible(color)) g.fill(x1, y1, x2, y2, color);
	}

	public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		color = a(color);
		if (invisible(color) || w <= 0 || h <= 0) return;
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	public static void gradient(GuiGraphics g, int x1, int y1, int x2, int y2, int top, int bottom) {
		g.fillGradient(x1, y1, x2, y2, a(top), a(bottom));
	}

	public static void text(GuiGraphics g, Font font, String text, int x, int y, int color, boolean shadow) {
		color = a(color);
		// Text with almost no alpha is skipped by the font renderer and can flicker; just don't draw it.
		if ((color >>> 24) > 4) g.drawString(font, text, x, y, color, shadow);
	}

	public static void text(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
		color = a(color);
		if ((color >>> 24) > 4) g.drawString(font, text, x, y, color, shadow);
	}

	/** Filled rectangle with rounded corners, built from horizontal strips. */
	public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
		color = a(color);
		if (w <= 0 || h <= 0 || invisible(color)) return;
		int r = Math.min(radius, Math.min(w, h) / 2);
		if (r <= 0) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		g.fill(x, y + r, x + w, y + h - r, color);
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
			g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
		}
	}

	/** Rounded rectangle with a 1px border. */
	public static void roundedRectBordered(GuiGraphics g, int x, int y, int w, int h, int radius, int fill, int border) {
		roundedRect(g, x, y, w, h, radius, border);
		roundedRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
	}

	public static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
		roundedRect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
	}

	public static int lerpColor(int from, int to, float t) {
		t = Math.max(0, Math.min(1, t));
		int a = (int) ((from >>> 24) + ((to >>> 24) - (from >>> 24)) * t);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int gr = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (gr << 8) | b;
	}

	/** Multiply a color's alpha. */
	public static int withAlpha(int color, float alpha) {
		int a = (int) ((color >>> 24) * Math.max(0, Math.min(1, alpha)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	public static boolean hovered(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Frame-rate independent exponential approach toward a target. */
	public static float approach(float current, float target, float speed, float deltaSeconds) {
		return Anim.approach(current, target, speed, deltaSeconds);
	}
}
