package com.orbitclient.module.visual;

import com.orbitclient.compat.BorderlessWindow;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import net.minecraft.world.item.Items;

/**
 * Lunar-style borderless fullscreen: when the game goes fullscreen (F11 or Video Settings) it becomes a borderless
 * window covering the screen instead of taking over the display, so Alt+Tab and a second monitor work instantly.
 * The switch itself is done in WindowMixin; this module only says whether it's wanted.
 */
public class BorderlessFullscreenModule extends Module {
	private static BorderlessFullscreenModule instance;

	public BorderlessFullscreenModule() {
		super("Borderless Fullscreen", "Fullscreen as a borderless window: instant Alt+Tab, no black flash.", Category.MECHANIC);
		setIcon(Items.ITEM_FRAME);
		instance = this;
	}

	/** Whether fullscreen should be borderless. macOS keeps its own fullscreen, which already behaves this way. */
	public static boolean active() {
		return instance != null && instance.isEnabled() && !System.getProperty("os.name", "").startsWith("Mac");
	}

	@Override
	public void onTick() {
		sync();
	}

	@Override
	protected void onDisable() {
		sync();
	}

	/** Switches the window if it's fullscreen in the wrong way (e.g. just turned on, or fullscreen from startup). */
	private static void sync() {
		var window = mc.getWindow();
		//? if >=26.3 {
		/*// Vanilla's own borderless fullscreen: Orbit's switch overrides the Exclusive Fullscreen video setting while on.
		window.setExclusiveFullscreen(!active() && mc.options.exclusiveFullscreen().get());
		*///?} else {
		if ((Object) window instanceof BorderlessWindow borderless && window.isFullscreen() && borderless.orbit$isBorderless() != active()) {
			borderless.orbit$refreshMode();
		}
		//?}
	}
}
