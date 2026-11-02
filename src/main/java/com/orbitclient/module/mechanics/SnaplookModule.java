package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.KeybindSetting;
import com.orbitclient.setting.ModeSetting;
import net.minecraft.client.CameraType;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Hold a key to switch perspective, and snap back when you let go. */
public class SnaplookModule extends Module {
	private final KeybindSetting key;
	private final ModeSetting view;
	private boolean active;
	private CameraType previousCamera;

	public SnaplookModule() {
		super("Snaplook", "Hold the key to see yourself from the front or back.", Category.MECHANIC);
		setIcon(Items.ARMOR_STAND);
		key = add(new KeybindSetting("Snaplook Key", GLFW.GLFW_KEY_V));
		view = add(new ModeSetting("View", "Front", "Front", "Back"));
	}

	@Override
	public void onTick() {
		boolean down = mc.screen == null && key.isBound()
			&& key.isDown();
		if (down && !active) {
			active = true;
			previousCamera = mc.options.getCameraType();
			mc.options.setCameraType(view.is("Front") ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
		} else if (!down && active) {
			release();
		}
	}

	@Override
	protected void onDisable() {
		if (active) release();
	}

	private void release() {
		active = false;
		if (previousCamera != null) mc.options.setCameraType(previousCamera);
	}
}
