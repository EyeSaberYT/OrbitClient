package com.orbitclient.compat;

//? if <1.16 {
/*import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.resources.language.I18n;

// Small text helpers for versions before 1.16.
public final class Texts {
	private Texts() {
	}

	// What a key is called: the letter on it, or its translated name.
	public static String keyName(int key) {
		String printed = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, -1);
		if (printed != null && !printed.isEmpty()) return printed.toUpperCase(java.util.Locale.ROOT);
		return I18n.get(InputConstants.Type.KEYSYM.getOrCreate(key).getName());
	}
}
*///?}
