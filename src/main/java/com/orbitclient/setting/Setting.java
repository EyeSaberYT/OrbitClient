package com.orbitclient.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
	private final String name;
	private final T defaultValue;
	protected T value;
	private BooleanSupplier visibility = () -> true;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public void reset() {
		set(defaultValue);
	}

	/** Only show this setting in the menu while the condition holds (e.g. a color only when its toggle is on). */
	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
		this.visibility = condition;
		return (S) this;
	}

	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement element);
}
