package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.world.item.Items;

/** Controls how much sprinting, speed and slowness change your FOV. */
public class FovChangerModule extends Module {
	private static FovChangerModule instance;

	private final BooleanSetting dynamic;
	private final NumberSetting speedFov;
	private final NumberSetting slowFov;

	public FovChangerModule() {
		super("FOV Changer", "Change how much sprinting, speed and slowness affect your FOV.", Category.MECHANIC);
		setIcon(Items.TINTED_GLASS);
		instance = this;
		dynamic = add(new BooleanSetting("Dynamic FOV", true));
		speedFov = add(new NumberSetting("Speed/Sprint Effect", 1.0, 0, 2, 0.05).visibleWhen(dynamic::isOn));
		slowFov = add(new NumberSetting("Slowness Effect", 0.0, 0, 2, 0.05).visibleWhen(dynamic::isOn));
	}

	/** Called with the vanilla FOV multiplier (1.0 = no change). */
	public static float modify(float modifier) {
		if (instance == null || !instance.isEnabled()) return modifier;
		if (!instance.dynamic.isOn()) return 1f;
		float strength = modifier >= 1f ? instance.speedFov.getFloat() : instance.slowFov.getFloat();
		return 1f + (modifier - 1f) * strength;
	}
}
