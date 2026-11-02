package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Show your own nametag in third person, or hide nametags. */
public class NametagsModule extends Module {
	private static NametagsModule instance;
	private final BooleanSetting showOwn;
	private final BooleanSetting hidePlayers;
	private final BooleanSetting hideMobs;

	public NametagsModule() {
		super("Nametags", "Shows your own nametag in third person and can hide others.", Category.MECHANIC);
		setIcon(Items.NAME_TAG);
		instance = this;
		showOwn = add(new BooleanSetting("Show Own Nametag", true));
		hidePlayers = add(new BooleanSetting("Hide Player Nametags", false));
		hideMobs = add(new BooleanSetting("Hide Mob Nametags", false));
	}

	/** Whether a nametag is shown, given vanilla's decision. */
	public static boolean shouldShow(Entity entity, boolean vanilla) {
		NametagsModule m = instance;
		if (m == null || !m.isEnabled()) return vanilla;
		Minecraft mc = Minecraft.getInstance();
		if (entity == mc.player) {
			return vanilla || (m.showOwn.isOn() && !mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui && !entity.isInvisible());
		}
		if (entity instanceof Player) return vanilla && !m.hidePlayers.isOn();
		return vanilla && !m.hideMobs.isOn();
	}
}
