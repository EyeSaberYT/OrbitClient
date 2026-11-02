package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.NumberSetting;
import com.orbitclient.util.SmoothOverride;
import net.minecraft.world.item.Items;

/** Client-side time of day; only you see it. The sky sweeps to the new time instead of jumping. */
public class TimeChangerModule extends Module {
	private static final long DAY = 24000;
	private static TimeChangerModule instance;
	private final NumberSetting time;
	private final SmoothOverride smooth = new SmoothOverride(3, DAY);

	public TimeChangerModule() {
		super("Time Changer", "Set your own time of day. Only you see it.", Category.MECHANIC);
		setIcon(Items.DAYLIGHT_DETECTOR);
		instance = this;
		time = add(new NumberSetting("Time (ticks)", 6000, 0, 23999, 100));
	}

	public static long modify(long real) {
		if (instance == null) return real;
		long days = Math.floorDiv(real, DAY);
		double timeOfDay = instance.smooth.apply(Math.floorMod(real, DAY), instance.isEnabled(), instance.time.get());
		return days * DAY + Math.round(timeOfDay);
	}
}
