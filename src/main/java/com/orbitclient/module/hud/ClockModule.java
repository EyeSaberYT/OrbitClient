package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.TextHudModule;
import com.orbitclient.setting.ModeSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockModule extends TextHudModule {
	private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a");
	private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm");
	private final ModeSetting format;

	public ClockModule() {
		super("Clock", "Shows your real-world time.", 0.16, 0.01);
		setIcon(Items.CLOCK);
		format = add(new ModeSetting("Format", "12 Hour", "12 Hour", "24 Hour"));
	}

	@Override
	protected String getText() {
		return LocalTime.now().format(format.is("12 Hour") ? H12 : H24);
	}
}
