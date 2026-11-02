package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import com.orbitclient.util.CombatTracker;
import net.minecraft.world.item.Items;

public class ReachDisplayModule extends TextHudModule {
	public ReachDisplayModule() {
		super("Reach Display", "Shows the distance of your last hit.", 0.16, 0.37);
		setIcon(Items.STICK);
	}

	@Override
	protected String getText() {
		double reach = CombatTracker.getReach();
		return reach < 0 ? "Hasn't attacked" : String.format("%.2f blocks", reach);
	}

	@Override
	protected boolean pulseOnChange() {
		return true;
	}

	@Override
	protected String getPreviewText() {
		return "2.94 blocks";
	}
}
