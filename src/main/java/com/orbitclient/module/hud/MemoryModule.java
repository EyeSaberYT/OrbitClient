package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.TextHudModule;
import com.orbitclient.setting.ModeSetting;

public class MemoryModule extends TextHudModule {
	private final ModeSetting display;

	public MemoryModule() {
		super("Memory Usage", "Shows how much RAM the game is using.", 0.16, 0.10);
		setIcon(Items.COMPARATOR);
		display = add(new ModeSetting("Display", "Percent", "Percent", "Megabytes"));
	}

	@Override
	protected String getText() {
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory();
		long max = rt.maxMemory();
		if (display.is("Percent")) return "Mem: " + (used * 100 / max) + "%";
		return "Mem: " + (used >> 20) + "/" + (max >> 20) + " MB";
	}
}
