package com.orbitclient.compat;

/**
 * Typing into Orbit's own screens. From 26.3 the game reads the keyboard through SDL, which only sends
 * typed characters while something has asked for text input (Minecraft's text boxes do it when focused).
 * Orbit's menu and wardrobe read characters themselves, so they ask for it while they are open.
 * Before 26.3 characters always arrive and these do nothing.
 */
public final class TextInput {
	private TextInput() {
	}

	/** Text input on, owned by {@code owner}. Cheap to call every tick: it keeps it on if a text box turned it off. */
	public static void keepOn(Object owner) {
		//? if >=26.3 {
		/*net.minecraft.client.Minecraft.getInstance().textInputManager().startTextInput(owner);
		*///?}
	}

	/** Text input off, if {@code owner} still has it (a text box that took it over is left alone). */
	public static void off(Object owner) {
		//? if >=26.3 {
		/*net.minecraft.client.Minecraft.getInstance().textInputManager().stopTextInput(owner);
		*///?}
	}

	/** Whether the game will receive typed characters now: always before 26.3. For the development audit. */
	public static boolean active() {
		//? if >=26.3 {
		/*return org.lwjgl.sdl.SDLKeyboard.SDL_TextInputActive(net.minecraft.client.Minecraft.getInstance().getWindow().handle());
		*///?} else
		return true;
	}
}
