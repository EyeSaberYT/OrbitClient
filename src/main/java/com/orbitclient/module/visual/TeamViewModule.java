package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Teammates glow in their team color, so you can see them through walls and in fights. */
public class TeamViewModule extends Module {
	private static TeamViewModule instance;

	public TeamViewModule() {
		super("Team View", "Makes your teammates glow in their team color.", Category.MECHANIC);
		setIcon(Items.WHITE_BANNER);
		instance = this;
	}

	public static boolean glows(Entity entity) {
		if (instance == null || !instance.isEnabled()) return false;
		Minecraft mc = Minecraft.getInstance();
		return entity instanceof Player && entity != mc.player && mc.player != null
			&& mc.player.getTeam() != null && mc.player.isAlliedTo(entity);
	}
}
