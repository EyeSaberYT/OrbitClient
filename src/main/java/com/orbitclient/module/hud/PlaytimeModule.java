package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import net.minecraft.world.item.Items;

public class PlaytimeModule extends TextHudModule {
	private long sessionStart;

	public PlaytimeModule() {
		super("Playtime", "Shows how long you've been in this world or server.", 0.16, 0.73);
		setIcon(Items.BELL);
	}

	@Override
	public void onTick() {
		if (mc.level == null) sessionStart = 0;
		else if (sessionStart == 0) sessionStart = System.currentTimeMillis();
	}

	@Override
	protected String getText() {
		long seconds = sessionStart == 0 ? 0 : (System.currentTimeMillis() - sessionStart) / 1000;
		long h = seconds / 3600, m = (seconds / 60) % 60, s = seconds % 60;
		return h > 0 ? String.format("%dh %02dm", h, m) : String.format("%dm %02ds", m, s);
	}
}
