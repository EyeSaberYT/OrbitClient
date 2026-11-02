package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.NumberSetting;
import com.orbitclient.util.SmoothOverride;
import net.minecraft.world.item.Items;

public class FullbrightModule extends Module {
	private static FullbrightModule instance;
	public final NumberSetting brightness;
	private final SmoothOverride gamma = new SmoothOverride(5, 0);

	public FullbrightModule() {
		super("Fullbright", "Lets you see in the dark without night vision.", Category.MECHANIC);
		setIcon(Items.TORCH);
		instance = this;
		brightness = add(new NumberSetting("Brightness", 10, 1, 15, 0.5));
	}

	/** Gamma to use given the video setting; fades in and out instead of snapping. */
	public static double modifyGamma(double real) {
		if (instance == null) return real;
		return instance.gamma.apply(real, instance.isEnabled(), instance.brightness.get());
	}
}
