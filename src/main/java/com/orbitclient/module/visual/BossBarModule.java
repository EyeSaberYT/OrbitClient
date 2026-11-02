package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.world.item.Items;

public class BossBarModule extends Module {
	public final BooleanSetting hide;
	public final NumberSetting scale;

	public BossBarModule() {
		super("Boss Bar", "Resize or hide boss health bars.", Category.SERVER);
		setIcon(Items.DRAGON_HEAD);
		hide = add(new BooleanSetting("Hide Boss Bar", false));
		scale = add(new NumberSetting("Scale", 1.0, 0.5, 1.5, 0.05).visibleWhen(() -> !hide.isOn()));
	}
}
