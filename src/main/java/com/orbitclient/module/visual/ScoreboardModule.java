package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.world.item.Items;

public class ScoreboardModule extends Module {
	public final BooleanSetting hide;
	public final NumberSetting scale;

	public ScoreboardModule() {
		super("Scoreboard", "Resize or hide the sidebar scoreboard.", Category.SERVER);
		setIcon(Items.OAK_SIGN);
		hide = add(new BooleanSetting("Hide Scoreboard", false));
		scale = add(new NumberSetting("Scale", 1.0, 0.5, 1.5, 0.05).visibleWhen(() -> !hide.isOn()));
	}
}
