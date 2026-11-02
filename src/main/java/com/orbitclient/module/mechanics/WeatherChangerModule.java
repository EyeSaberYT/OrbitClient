package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.util.SmoothOverride;
import net.minecraft.world.item.Items;

/** Client-side weather; rain fades in and out. Snow or rain follows the biome like vanilla. */
public class WeatherChangerModule extends Module {
	private static WeatherChangerModule instance;
	private final ModeSetting weather;
	private final SmoothOverride rain = new SmoothOverride(1.5, 0);
	private final SmoothOverride thunder = new SmoothOverride(1.5, 0);

	public WeatherChangerModule() {
		super("Weather Changer", "Set your own weather. Only you see it.", Category.MECHANIC);
		setIcon(Items.WATER_BUCKET);
		instance = this;
		weather = add(new ModeSetting("Weather", "Clear", "Clear", "Rain", "Thunder"));
	}

	public static float modifyRain(float real) {
		if (instance == null) return real;
		return (float) instance.rain.apply(real, instance.isEnabled(), instance.weather.is("Clear") ? 0 : 1);
	}

	public static float modifyThunder(float real) {
		if (instance == null) return real;
		return (float) instance.thunder.apply(real, instance.isEnabled(), instance.weather.is("Thunder") ? 1 : 0);
	}
}
