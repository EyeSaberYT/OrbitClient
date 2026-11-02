package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.util.CpsTracker;
import net.minecraft.world.item.Items;

public class CpsModule extends TextHudModule {
	private final BooleanSetting showRight;

	public CpsModule() {
		super("CPS", "Shows your clicks per second.", 0.005, 0.10);
		setIcon(Items.STONE_BUTTON);
		showRight = add(new BooleanSetting("Show Right Click", false));
	}

	@Override
	protected String getText() {
		return showRight.isOn()
			? CpsTracker.left() + " | " + CpsTracker.right() + " CPS"
			: CpsTracker.left() + " CPS";
	}
}
