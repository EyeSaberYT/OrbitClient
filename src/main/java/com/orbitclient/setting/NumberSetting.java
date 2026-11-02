package com.orbitclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;

	public NumberSetting(String name, double defaultValue, double min, double max, double step) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
	}

	@Override
	public void set(Double value) {
		double clamped = Math.max(min, Math.min(max, value));
		this.value = Math.round(clamped / step) * step;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public float getFloat() {
		return value.floatValue();
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	/** 0..1 position of the current value inside the range, for drawing sliders. */
	public double getPercent() {
		return (value - min) / (max - min);
	}

	public void setPercent(double percent) {
		set(min + (max - min) * Math.max(0, Math.min(1, percent)));
	}

	public String format() {
		if (step >= 1) return String.valueOf(getInt());
		if (step >= 0.1) return String.format("%.1f", value);
		return String.format("%.2f", value);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element.isJsonPrimitive()) set(element.getAsDouble());
	}
}
