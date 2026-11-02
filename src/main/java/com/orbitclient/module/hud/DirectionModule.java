package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import com.orbitclient.setting.BooleanSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

public class DirectionModule extends TextHudModule {
	private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private final BooleanSetting showYaw;

	public DirectionModule() {
		super("Direction HUD", "Shows the direction you are facing.", 0.005, 0.28);
		setIcon(Items.COMPASS);
		showYaw = add(new BooleanSetting("Show Yaw", true));
	}

	@Override
	protected String getText() {
		if (mc.player == null) return "N";
		float yaw = Mth.wrapDegrees(mc.player.getYRot());
		String dir = COMPASS[Math.floorMod(Math.round(yaw / 45f), 8)];
		return showYaw.isOn() ? dir + " (" + String.format("%.1f", yaw) + ")" : dir;
	}

	@Override
	protected String getPreviewText() {
		return showYaw.isOn() ? "N (180.0)" : "N";
	}
}
