package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import com.orbitclient.setting.KeybindSetting;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Press the key to start, again to stop, and a third time to reset. */
public class StopwatchModule extends TextHudModule {
	private final KeybindSetting key;
	private boolean wasDown;
	private long startTime;
	private long elapsed;
	private boolean running;

	public StopwatchModule() {
		super("Stopwatch", "A stopwatch: press the key to start, stop, then reset.", 0.16, 0.82);
		setIcon(Items.RECOVERY_COMPASS);
		key = add(new KeybindSetting("Start/Stop Key", GLFW.GLFW_KEY_P));
	}

	@Override
	public void onTick() {
		boolean down = mc.screen == null && key.isBound()
			&& key.isDown();
		if (down && !wasDown) {
			if (running) {
				elapsed += System.currentTimeMillis() - startTime;
				running = false;
			} else if (elapsed > 0) {
				elapsed = 0;
			} else {
				startTime = System.currentTimeMillis();
				running = true;
			}
		}
		wasDown = down;
	}

	@Override
	protected String getText() {
		long ms = elapsed + (running ? System.currentTimeMillis() - startTime : 0);
		return String.format("%02d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms / 10) % 100);
	}
}
