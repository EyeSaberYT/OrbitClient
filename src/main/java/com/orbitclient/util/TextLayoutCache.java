package com.orbitclient.util;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Laid-out text (Font.prepareText), kept so the same text isn't laid out again every frame. Minecraft
 * looks up every glyph and builds a new list of them for each line it draws, every frame, even when
 * nothing changed. Holograms (text displays), signs and chat keep the same text object until their
 * text changes, so a layout is found by that object (one hash lookup); comparing the characters
 * instead cost as much as laying them out again. Text that's rebuilt every frame (player nametags,
 * the scoreboard) simply isn't found. Only on the render thread, never for obfuscated text (its
 * glyphs are random each frame), and cleared when the fonts reload (the old glyphs belong to closed
 * textures). Entries go away with their text: the map holds its keys weakly.
 */
public final class TextLayoutCache {
	private static final Map<FormattedCharSequence, Entry> CACHE = new WeakHashMap<>();
	private static Thread renderThread;
	/** The default font's glyphs when the cache was filled; another one means the fonts reloaded. */
	private static Object glyphs;
	/** The text being laid out right now, stored once it's done. */
	private static Entry pending;
	private static FormattedCharSequence pendingText;
	private static boolean enabled = true;
	/** For the performance test: what happened to each layout asked for since the last {@link #stats}. */
	private static int hits, misses, otherThread, uncacheable;

	private TextLayoutCache() {
	}

	/** Called once on the render thread, before any text is drawn. */
	public static void init(Thread thread) {
		renderThread = thread;
	}

	public static void setEnabled(boolean on) {
		enabled = on;
		CACHE.clear();
	}

	/** The cached layout, or null (and then {@link #store} keeps the new one). */
	public static Font.PreparedText find(Font font, Object currentGlyphs, FormattedCharSequence text, float x, float y,
	                                    int color, boolean shadow, boolean includeEmpty, int background) {
		pending = null;
		pendingText = null;
		if (!enabled) return null;
		if (Thread.currentThread() != renderThread) {
			otherThread++;
			return null;
		}
		if (currentGlyphs != glyphs) {
			CACHE.clear();
			glyphs = currentGlyphs;
		}
		Entry entry = CACHE.get(text);
		if (entry != null && entry.matches(font, x, y, color, shadow, includeEmpty, background)) {
			if (!entry.checked) {
				// Checked on the first reuse, not when stored: most text that's never reused (rebuilt every
				// frame) then costs nothing more than the store. Obfuscated glyphs change every frame.
				entry.checked = true;
				if (!text.accept((index, style, codepoint) -> !style.isObfuscated())) entry.prepared = null;
			}
			if (entry.prepared == null) {
				uncacheable++;  // obfuscated: laid out every time, as in vanilla
				return null;
			}
			hits++;
			return entry.prepared;
		}
		misses++;
		pending = new Entry(font, x, y, color, shadow, includeEmpty, background);
		pendingText = text;
		return null;
	}

	public static void store(FormattedCharSequence text, Font.PreparedText prepared) {
		if (pending != null && text == pendingText && Thread.currentThread() == renderThread) {
			pending.prepared = prepared;
			CACHE.put(text, pending);
		}
		pending = null;
		pendingText = null;
	}

	public static int size() {
		return CACHE.size();
	}

	/** Counts since the last call, then starts counting again. */
	public static String stats() {
		String text = "kept " + CACHE.size() + ", reused " + hits + ", laid out " + misses + ", not cacheable " + uncacheable
			+ ", other thread " + otherThread;
		hits = misses = uncacheable = otherThread = 0;
		return text;
	}

	/** How a text was laid out (its position and colors), and the result. */
	private static final class Entry {
		final Font font;
		final float x, y;
		final int color, background;
		final boolean shadow, includeEmpty;
		Font.PreparedText prepared;
		/** Whether the text was checked for obfuscated (random) glyphs, done on its first reuse. */
		boolean checked;

		Entry(Font font, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
			this.font = font;
			this.x = x;
			this.y = y;
			this.color = color;
			this.shadow = shadow;
			this.includeEmpty = includeEmpty;
			this.background = background;
		}

		boolean matches(Font font, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
			return this.font == font && this.x == x && this.y == y && this.color == color && this.shadow == shadow
				&& this.includeEmpty == includeEmpty && this.background == background;
		}
	}
}
