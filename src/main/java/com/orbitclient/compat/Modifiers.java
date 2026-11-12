package com.orbitclient.compat;

//? if <1.21.9 {
/*import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/^* Before 1.21.9 mouse clicks don't carry the held modifier keys; they're read from the keyboard instead. ^/
final class Modifiers {
	private Modifiers() {
	}

	static int current() {
		long window = Minecraft.getInstance().getWindow().getWindow();
		int modifiers = 0;
		if (down(window, GLFW.GLFW_KEY_LEFT_SHIFT) || down(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) modifiers |= GLFW.GLFW_MOD_SHIFT;
		if (down(window, GLFW.GLFW_KEY_LEFT_CONTROL) || down(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) modifiers |= GLFW.GLFW_MOD_CONTROL;
		if (down(window, GLFW.GLFW_KEY_LEFT_ALT) || down(window, GLFW.GLFW_KEY_RIGHT_ALT)) modifiers |= GLFW.GLFW_MOD_ALT;
		return modifiers;
	}

	private static boolean down(long window, int key) {
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}
}
*///?}
