package com.orbitclient.module.mechanics;

import net.minecraft.world.item.Items;
import com.orbitclient.module.Category;
import com.orbitclient.module.TextHudModule;
import com.orbitclient.setting.BooleanSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ToggleSprintModule extends TextHudModule {
	private final BooleanSetting toggleSneak;
	private final BooleanSetting showText;

	private boolean sprinting;
	private boolean sneaking;
	private boolean sprintWasDown;
	private boolean sneakWasDown;

	public ToggleSprintModule() {
		super("Toggle Sneak/Sprint", "Press sprint (or sneak) once to keep it held.", Category.MECHANIC, 0.30, 0.01);
		setIcon(Items.FEATHER);
		toggleSneak = add(new BooleanSetting("Toggle Sneak", false));
		showText = add(new BooleanSetting("Show HUD Text", true));
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		boolean sprintDown = isPhysicallyDown(mc.options.keySprint);
		if (sprintDown && !sprintWasDown && mc.screen == null) sprinting = !sprinting;
		sprintWasDown = sprintDown;
		if (sprinting) mc.options.keySprint.setDown(true);

		if (toggleSneak.isOn()) {
			boolean sneakDown = isPhysicallyDown(mc.options.keyShift);
			if (sneakDown && !sneakWasDown && mc.screen == null) sneaking = !sneaking;
			sneakWasDown = sneakDown;
			if (sneaking) mc.options.keyShift.setDown(true);
		} else if (sneaking) {
			sneaking = false;
			mc.options.keyShift.setDown(isPhysicallyDown(mc.options.keyShift));
		}
	}

	@Override
	protected void onDisable() {
		sprinting = false;
		sneaking = false;
		// Settings load before the game's options exist; nothing to release then.
		if (mc.options == null) return;
		mc.options.keySprint.setDown(isPhysicallyDown(mc.options.keySprint));
		mc.options.keyShift.setDown(isPhysicallyDown(mc.options.keyShift));
	}

	private static boolean isPhysicallyDown(KeyMapping mapping) {
		InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mapping);
		if (key.getType() == InputConstants.Type.MOUSE) {
			return com.orbitclient.util.InputUtil.isMouseDown(key.getValue());
		}
		return key.getValue() != GLFW.GLFW_KEY_UNKNOWN && com.orbitclient.util.InputUtil.isKeyDown(key.getValue());
	}

	@Override
	public boolean shouldRender() {
		return showText.isOn() && !getText().isEmpty();
	}

	@Override
	protected String getText() {
		if (sneaking) return "Sneaking (Toggled)";
		if (sprinting) return "Sprinting (Toggled)";
		if (mc.player != null && mc.player.isSprinting()) return "Sprinting (Vanilla)";
		return "";
	}

	@Override
	protected String getPreviewText() {
		return "Sprinting (Toggled)";
	}
}
