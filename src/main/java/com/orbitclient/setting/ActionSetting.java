package com.orbitclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A button on a settings page, e.g. "Open Editor" or "Move Here". Nothing is saved. */
public class ActionSetting extends Setting<String> {
	private final Runnable action;
	private final boolean closesMenu;

	/** A button that closes the menu first (e.g. to open another screen). */
	public ActionSetting(String name, String buttonLabel, Runnable action) {
		this(name, buttonLabel, action, true);
	}

	public ActionSetting(String name, String buttonLabel, Runnable action, boolean closesMenu) {
		super(name, buttonLabel);
		this.action = action;
		this.closesMenu = closesMenu;
	}

	public boolean closesMenu() {
		return closesMenu;
	}

	public void run() {
		action.run();
	}

	@Override
	public JsonElement save() {
		return JsonNull.INSTANCE;
	}

	@Override
	public void load(JsonElement element) {
	}
}
