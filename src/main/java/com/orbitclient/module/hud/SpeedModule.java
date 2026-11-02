package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.TextHudModule;

public class SpeedModule extends TextHudModule {
	private double lastX;
	private double lastZ;
	private double speed;

	public SpeedModule() {
		super("Speedometer", "Shows your horizontal speed in blocks per second.", 0.16, 0.28);
		setIcon(Items.SUGAR);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		double dx = mc.player.getX() - lastX;
		double dz = mc.player.getZ() - lastZ;
		lastX = mc.player.getX();
		lastZ = mc.player.getZ();
		// Ignore teleports so the number doesn't spike to thousands.
		double perTick = Math.sqrt(dx * dx + dz * dz);
		speed = perTick > 10 ? 0 : perTick * 20;
	}

	@Override
	protected String getText() {
		return String.format("%.2f m/s", speed);
	}
}
