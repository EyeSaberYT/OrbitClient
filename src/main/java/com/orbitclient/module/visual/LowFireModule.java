package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.world.item.Items;

/** Lowers the fire overlay when you're burning, so it doesn't cover your view. */
public class LowFireModule extends Module {
	private static LowFireModule instance;
	private final NumberSetting height;

	public LowFireModule() {
		super("Low Fire", "Lowers the fire on your screen when you're burning.", Category.MECHANIC);
		setIcon(Items.CAMPFIRE);
		instance = this;
		height = add(new NumberSetting("Lowered By", 40, 0, 80, 5));
	}

	/** How far to move the fire down, in screen-effect units (the overlay spans about 1). */
	public static float offset() {
		return instance != null && instance.isEnabled() ? instance.height.getFloat() / 100f : 0f;
	}
}
