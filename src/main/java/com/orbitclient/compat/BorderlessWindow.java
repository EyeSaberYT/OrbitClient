package com.orbitclient.compat;

/** Added to Minecraft's game window by WindowMixin so Borderless Fullscreen can switch it at any time. */
public interface BorderlessWindow {
	/** True while the window is in Orbit's borderless fullscreen. */
	boolean orbit$isBorderless();

	/** Puts the window back into the mode it should be in now (windowed, borderless or exclusive fullscreen). */
	void orbit$refreshMode();
}
