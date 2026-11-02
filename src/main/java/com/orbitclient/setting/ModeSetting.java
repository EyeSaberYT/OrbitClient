package com.orbitclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<String> {
	private final List<String> modes;

	public ModeSetting(String name, String defaultValue, String... modes) {
		super(name, defaultValue);
		this.modes = List.of(modes);
	}

	public List<String> getModes() {
		return modes;
	}

	public boolean is(String mode) {
		return value.equals(mode);
	}

	public void cycle(int direction) {
		int index = modes.indexOf(value);
		set(modes.get(Math.floorMod(index + direction, modes.size())));
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element.isJsonPrimitive() && modes.contains(element.getAsString())) set(element.getAsString());
	}
}
