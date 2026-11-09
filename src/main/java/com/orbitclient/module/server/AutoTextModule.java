package com.orbitclient.module.server;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.KeybindSetting;
import com.orbitclient.setting.TextSetting;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Bind chat messages or commands to keys. Messages starting with "/" are sent as commands. */
public class AutoTextModule extends Module {
	private static final int SLOTS = 4;
	private static final long COOLDOWN_MS = 1000;

	private final TextSetting[] texts = new TextSetting[SLOTS];
	private final KeybindSetting[] keys = new KeybindSetting[SLOTS];
	private final boolean[] wasDown = new boolean[SLOTS];
	private long lastSent;

	public AutoTextModule() {
		super("Auto Text Hotkey", "Send a chat message or command with one key.", Category.SERVER);
		setIcon(Items.PAPER);
		String[] defaults = {"/l", "gg", "", ""};
		for (int i = 0; i < SLOTS; i++) {
			texts[i] = add(new TextSetting("Message " + (i + 1), defaults[i], 256));
			keys[i] = add(new KeybindSetting("Key " + (i + 1), GLFW.GLFW_KEY_UNKNOWN));
		}
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		for (int i = 0; i < SLOTS; i++) {
			boolean down = mc.screen == null && keys[i].isBound() && keys[i].isDown();
			if (down && !wasDown[i]) send(texts[i].get().trim());
			wasDown[i] = down;
		}
	}

	private void send(String text) {
		// A short cooldown so a stuck key can't spam the server and get you kicked.
		if (text.isEmpty() || System.currentTimeMillis() - lastSent < COOLDOWN_MS) return;
		lastSent = System.currentTimeMillis();
		if (text.startsWith("/")) mc.player.connection.sendCommand(text.substring(1));
		else mc.player.connection.sendChat(text);
	}
}
