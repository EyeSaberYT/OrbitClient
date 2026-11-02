package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.world.item.Items;

/** Custom color and thickness for the outline of the block you're looking at. */
public class BlockOutlineModule extends Module {
	/** The black backing line vanilla draws under the outline in high-contrast mode. */
	private static final int HIGH_CONTRAST_BACKING = 0xFF000000;
	private static BlockOutlineModule instance;

	private final ColorSetting color;
	private final NumberSetting thickness;

	public BlockOutlineModule() {
		super("Block Outline", "Changes the color and thickness of the block outline.", Category.MECHANIC);
		setIcon(Items.GLASS);
		instance = this;
		color = add(new ColorSetting("Color", 0xCC55AAFF));
		thickness = add(new NumberSetting("Thickness", 3, 1, 10, 0.5));
	}

	public static int color(int vanilla) {
		if (instance == null || !instance.isEnabled() || vanilla == HIGH_CONTRAST_BACKING) return vanilla;
		return instance.color.getColor();
	}

	public static float width(float vanilla, int color) {
		if (instance == null || !instance.isEnabled() || color == HIGH_CONTRAST_BACKING) return vanilla;
		return instance.thickness.getFloat();
	}
}
