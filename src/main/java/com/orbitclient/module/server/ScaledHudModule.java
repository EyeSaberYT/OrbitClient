package com.orbitclient.module.server;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** A vanilla HUD element you can hide or resize: tab list, titles. */
public class ScaledHudModule extends Module {
	public final BooleanSetting hide;
	public final NumberSetting scale;

	private ScaledHudModule(String name, String description, Item icon, String hideLabel) {
		super(name, description, Category.SERVER);
		setIcon(icon);
		hide = add(new BooleanSetting(hideLabel, false));
		scale = add(new NumberSetting("Scale", 1.0, 0.5, 1.5, 0.05).visibleWhen(() -> !hide.isOn()));
	}

	public static ScaledHudModule tabEditor() {
		return new ScaledHudModule("Tab Editor", "Resize or hide the player list (Tab).", Items.WRITABLE_BOOK, "Hide Tab List");
	}

	public static ScaledHudModule titles() {
		return new ScaledHudModule("Titles", "Resize or hide the big titles servers show.", Items.BOOK, "Hide Titles");
	}
}
