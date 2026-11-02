package com.orbitclient.module.visual;

import net.minecraft.world.item.Items;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;

public class NoHurtCamModule extends Module {
	private static NoHurtCamModule instance;

	public NoHurtCamModule() {
		super("Hurt Cam", "Stops your screen from shaking when you take damage.", Category.MECHANIC);
		setIcon(Items.SHIELD);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
