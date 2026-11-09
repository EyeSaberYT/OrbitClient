package com.orbitclient.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Version-neutral input polling used by modules and keybind settings. */
public final class InputUtil {
	public static final int KEY_LEFT_ALT = GLFW.GLFW_KEY_LEFT_ALT;

	private InputUtil() {
	}

	public static boolean isKeyDown(int key) {
		return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), key) == GLFW.GLFW_PRESS;
	}

	public static boolean isMouseDown(int button) {
		return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), button) == GLFW.GLFW_PRESS;
	}
}
