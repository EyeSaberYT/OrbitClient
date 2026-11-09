package com.orbitclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import com.orbitclient.util.InputUtil;

/**
 * A key or mouse button. Keyboard keys are stored as their GLFW key code; mouse buttons are
 * stored as {@code MOUSE_OFFSET - button} (so they're negative and can't clash with keys).
 * GLFW_KEY_UNKNOWN (-1) means unbound.
 */
public class KeybindSetting extends Setting<Integer> {
	private static final int MOUSE_OFFSET = -100;

	public KeybindSetting(String name, int defaultKey) {
		super(name, defaultKey);
	}

	public static int mouse(int button) {
		return MOUSE_OFFSET - button;
	}

	public boolean isBound() {
		return value != GLFW.GLFW_KEY_UNKNOWN;
	}

	public boolean isMouse() {
		return value <= MOUSE_OFFSET;
	}

	public int mouseButton() {
		return MOUSE_OFFSET - value;
	}

	/** Whether the key or mouse button is held right now. */
	public boolean isDown() {
		if (!isBound()) return false;
		if (isMouse()) return InputUtil.isMouseDown(mouseButton());
		return InputUtil.isKeyDown(value);
	}

	public String getKeyName() {
		if (!isBound()) return "None";
		if (isMouse()) {
			return switch (mouseButton()) {
				case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "Left Click";
				case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "Right Click";
				case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Middle Click";
				default -> "Mouse " + (mouseButton() + 1);
			};
		}
		return InputConstants.Type.KEYSYM.getOrCreate(value).getDisplayName().getString();
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element.isJsonPrimitive()) set(element.getAsInt());
	}
}
