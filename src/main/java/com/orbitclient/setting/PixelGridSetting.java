package com.orbitclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Base64;

/**
 * A square grid of on/off pixels: the custom crosshair. Always 17x17, so there is a center pixel;
 * designs of other sizes (older saves, imported codes) are centered into it. Saved as rows of "." and "#".
 *
 * <p>Share codes use Lunar Client's format, LCCH-SIZE-DATA, where DATA is the pixels row by row as
 * bits (lowest bit first in each byte), base64 encoded. Codes from Lunar import here and vice versa.</p>
 */
public class PixelGridSetting extends Setting<boolean[]> {
	public static final int SIZE = 17;
	private static final String CODE_PREFIX = "LCCH";

	public PixelGridSetting(String name, boolean[] defaultPixels) {
		super(name, defaultPixels.clone());
		// Separate copy from the default, so painting never changes what Reset goes back to.
		this.value = this.value.clone();
	}

	public int size() {
		return SIZE;
	}

	public boolean get(int x, int y) {
		return x >= 0 && y >= 0 && x < SIZE && y < SIZE && value[y * SIZE + x];
	}

	public void setPixel(int x, int y, boolean on) {
		if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) value[y * SIZE + x] = on;
	}

	public boolean isEmpty() {
		for (boolean b : value) if (b) return false;
		return true;
	}

	public void clear() {
		value = new boolean[SIZE * SIZE];
	}

	public boolean[] copy() {
		return value.clone();
	}

	public void restore(boolean[] pixels) {
		if (pixels.length == SIZE * SIZE) value = pixels.clone();
	}

	/** Places an n x n design in the middle of the grid (cropping if it's bigger). */
	private static boolean[] centered(boolean[] pixels, int n) {
		boolean[] out = new boolean[SIZE * SIZE];
		int offset = (SIZE - n) / 2;
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				int nx = x + offset, ny = y + offset;
				if (pixels[y * n + x] && nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE) out[ny * SIZE + nx] = true;
			}
		}
		return out;
	}

	@Override
	public void reset() {
		super.reset();
		value = value.clone();
	}

	// ---------------------------------------------------------------- share codes

	/** This design as a share code (Lunar-compatible). */
	public String toCode() {
		byte[] data = new byte[(SIZE * SIZE + 7) / 8];
		for (int i = 0; i < SIZE * SIZE; i++) if (value[i]) data[i / 8] |= (byte) (1 << (i % 8));
		return CODE_PREFIX + "-" + SIZE + "-" + Base64.getEncoder().encodeToString(data);
	}

	/** Loads a share code; returns false if it isn't a valid code. */
	public boolean fromCode(String code) {
		try {
			String[] parts = code.trim().split("-", 3);
			if (parts.length != 3 || !parts[0].equalsIgnoreCase(CODE_PREFIX)) return false;
			int n = Integer.parseInt(parts[1]);
			if (n < 1 || n > 64) return false;
			byte[] data = Base64.getDecoder().decode(parts[2]);
			boolean[] pixels = new boolean[n * n];
			for (int i = 0; i < n * n && i / 8 < data.length; i++) pixels[i] = ((data[i / 8] >> (i % 8)) & 1) == 1;
			value = centered(pixels, n);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	// ---------------------------------------------------------------- saving

	@Override
	public JsonElement save() {
		JsonArray rows = new JsonArray();
		for (int y = 0; y < SIZE; y++) {
			StringBuilder row = new StringBuilder();
			for (int x = 0; x < SIZE; x++) row.append(get(x, y) ? '#' : '.');
			rows.add(new JsonPrimitive(row.toString()));
		}
		return rows;
	}

	@Override
	public void load(JsonElement element) {
		if (!element.isJsonArray()) return;
		JsonArray rows = element.getAsJsonArray();
		int n = rows.size();
		if (n < 1) return;
		boolean[] pixels = new boolean[n * n];
		for (int y = 0; y < n; y++) {
			String row = rows.get(y).getAsString();
			for (int x = 0; x < Math.min(n, row.length()); x++) pixels[y * n + x] = row.charAt(x) == '#';
		}
		value = centered(pixels, n);
	}
}
