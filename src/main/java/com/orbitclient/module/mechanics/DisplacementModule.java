package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.KeybindSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Instantly snaps the player's view through a chosen yaw angle while the game is focused. */
public final class DisplacementModule extends Module {
	private final KeybindSetting displacementKey;
	private final NumberSetting degrees;
	private boolean wasDown;

	public DisplacementModule() {
		super("Displacement", "Snap your view by a chosen angle when you press its key. Camera only; never attacks.", Category.MECHANIC);
		setIcon(Items.COMPASS);
		displacementKey = add(new KeybindSetting("Displacement Key", GLFW.GLFW_KEY_G));
		degrees = add(new NumberSetting("Turn Degrees", 180, -360, 360, 1));
	}

	@Override
	public void onTick() {
		boolean down = mc.screen == null && mc.player != null && displacementKey.isBound() && displacementKey.isDown();
		if (down && !wasDown) {
			float yaw = Mth.wrapDegrees(mc.player.getYRot() + degrees.getFloat());
			mc.player.setYRot(yaw);
			mc.player.setYHeadRot(yaw);
		}
		wasDown = down;
	}

	@Override
	protected void onDisable() {
		wasDown = false;
	}
}
