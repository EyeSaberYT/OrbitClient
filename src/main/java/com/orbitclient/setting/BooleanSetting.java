package com.orbitclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	public boolean isOn() {
		return value;
	}

	public void toggle() {
		set(!value);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element.isJsonPrimitive()) set(element.getAsBoolean());
	}
}
