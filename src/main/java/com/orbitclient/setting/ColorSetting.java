package com.orbitclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.Color;

/** An ARGB color with an optional "chroma" (rainbow) mode, like Lunar's color picker. */
public class ColorSetting extends Setting<Integer> {
	private boolean chroma;

	public ColorSetting(String name, int defaultArgb) {
		super(name, defaultArgb);
	}

	@Override
	public void reset() {
		super.reset();
		chroma = false;
	}

	public boolean isChroma() {
		return chroma;
	}

	public void setChroma(boolean chroma) {
		this.chroma = chroma;
	}

	/** The color to draw with right now; chroma colors shift over time and along the x axis. */
	public int getColor(int offset) {
		if (!chroma) return value;
		float hue = ((System.currentTimeMillis() + offset * 10L) % 4000L) / 4000f;
		int rgb = Color.HSBtoRGB(hue, 0.8f, 1f) & 0xFFFFFF;
		return (value & 0xFF000000) | rgb;
	}

	public int getColor() {
		return getColor(0);
	}

	public int getAlpha() {
		return value >>> 24;
	}

	public float[] getHsb() {
		return Color.RGBtoHSB((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, null);
	}

	public void setHsb(float hue, float saturation, float brightness) {
		int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
		set((value & 0xFF000000) | rgb);
	}

	public void setAlpha(int alpha) {
		set((Math.max(0, Math.min(255, alpha)) << 24) | (value & 0xFFFFFF));
	}

	@Override
	public JsonElement save() {
		JsonObject obj = new JsonObject();
		obj.addProperty("argb", value);
		obj.addProperty("chroma", chroma);
		return obj;
	}

	@Override
	public void load(JsonElement element) {
		if (!element.isJsonObject()) return;
		JsonObject obj = element.getAsJsonObject();
		if (obj.has("argb")) set(obj.get("argb").getAsInt());
		if (obj.has("chroma")) chroma = obj.get("chroma").getAsBoolean();
	}
}
