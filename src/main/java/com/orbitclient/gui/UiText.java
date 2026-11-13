package com.orbitclient.gui;

import com.orbitclient.util.Render2D;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Text helpers shared by the menus. Everything goes through Render2D so it fades with the screen. */
public final class UiText {
	private UiText() {
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	/** The width of styled text. */
	public static int width(Component c) {
		//? if >=1.16 {
		return font().width(c);
		//?} else {
		/*// Before 1.16 the font measures text with formatting codes.
		return font().width(c.getColoredString());
		*///?}
	}

	public static Component styled(String s, boolean bold) {
		return bold ? Component.literal(s).withStyle(ChatFormatting.BOLD) : Component.literal(s);
	}

	/** Width of letter-spaced text (used for headings and buttons). */
	public static int spacedWidth(String s, boolean bold) {
		int w = 0;
		for (int i = 0; i < s.length(); i++) w += width(styled(String.valueOf(s.charAt(i)), bold)) + 1;
		return Math.max(0, w - 1);
	}

	public static void spaced(GuiGraphics g, String s, int x, int y, int color, boolean bold) {
		for (int i = 0; i < s.length(); i++) {
			Component c = styled(String.valueOf(s.charAt(i)), bold);
			Render2D.text(g, font(), c, x, y, color, false);
			x += width(c) + 1;
		}
	}

	/** Letter-spaced text at a smaller scale, centered on cx. */
	public static void spacedSmall(GuiGraphics g, String s, float cx, float y, int color, boolean bold, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(cx - spacedWidth(s, bold) * scale / 2f, y);
		g.pose().scale(scale, scale);
		spaced(g, s, 0, 0, color, bold);
		g.pose().popMatrix();
	}

	public static void text(GuiGraphics g, String s, int x, int y, int color) {
		Render2D.text(g, font(), s, x, y, color, false);
	}

	public static void centered(GuiGraphics g, String s, int cx, int y, int color) {
		text(g, s, cx - font().width(s) / 2, y, color);
	}

	/** Draws text within maxW, shrinking long text (down to 70%) before truncating. Returns the drawn width. */
	public static int fit(GuiGraphics g, String s, int x, int y, int maxW, int color) {
		int w = font().width(s);
		if (w <= maxW) {
			text(g, s, x, y, color);
			return w;
		}
		float scale = Math.max(0.7f, maxW / (float) w);
		String shown = scale * w > maxW ? font().plainSubstrByWidth(s, (int) (maxW / scale)) : s;
		g.pose().pushMatrix();
		g.pose().translate(x, y + (1 - scale) * 4);
		g.pose().scale(scale, scale);
		text(g, shown, 0, 0, color);
		g.pose().popMatrix();
		return Math.round(font().width(shown) * scale);
	}

	/** Word-wraps text to maxW, at most maxLines lines (the last one is truncated if needed). */
	public static java.util.List<String> wrap(String s, int maxW, int maxLines) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : s.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (font().width(next) <= maxW || line.isEmpty()) {
				line = new StringBuilder(next);
			} else {
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
		}
		if (!line.isEmpty()) lines.add(line.toString());
		if (lines.size() > maxLines) {
			java.util.List<String> cut = new java.util.ArrayList<>(lines.subList(0, maxLines));
			String last = String.join(" ", lines.subList(maxLines - 1, lines.size()));
			cut.set(maxLines - 1, font().plainSubstrByWidth(last, maxW - font().width("...")) + "...");
			return cut;
		}
		return lines;
	}

	/** A softly blinking caret color. */
	public static int caretColor() {
		double t = System.currentTimeMillis() / 1000.0 * Math.PI * 2 / 1.1;
		return Render2D.withAlpha(0xFFFFFFFF, (float) (0.5 + 0.5 * Math.cos(t)));
	}
}
