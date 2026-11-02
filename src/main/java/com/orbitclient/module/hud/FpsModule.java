package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.TextHudModule;

public class FpsModule extends TextHudModule {
	public FpsModule() {
		super("FPS", "Shows your frames per second.", 0.005, 0.01);
		setIcon(Items.REPEATER);
	}

	@Override
	protected String getText() {
		return mc.getFps() + " FPS";
	}
}
