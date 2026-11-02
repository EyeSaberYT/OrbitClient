package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import com.orbitclient.util.CombatTracker;
import net.minecraft.world.item.Items;

public class ComboCounterModule extends TextHudModule {
	public ComboCounterModule() {
		super("Combo Counter", "Counts hits landed without taking damage.", 0.16, 0.46);
		setIcon(Items.IRON_SWORD);
	}

	@Override
	protected String getText() {
		int combo = CombatTracker.getCombo();
		return combo == 0 ? "No Combo" : combo + " Combo";
	}

	@Override
	protected boolean pulseOnChange() {
		return true;
	}

	@Override
	protected String getPreviewText() {
		return "3 Combo";
	}
}
