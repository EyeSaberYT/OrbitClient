package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.TextHudModule;

public class DayCounterModule extends TextHudModule {
	public DayCounterModule() {
		super("Day Counter", "Shows the current in-game day.", 0.16, 0.55);
		setIcon(Items.SUNFLOWER);
	}

	@Override
	protected String getText() {
		if (mc.level == null) return "Day 0";
		return "Day " + (mc.level.getDayTime() / 24000L);
	}
}
