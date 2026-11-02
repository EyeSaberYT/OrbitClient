package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import net.minecraft.world.item.Items;

/** Blurs the world behind inventories and chests (vanilla only blurs behind menus like Options). */
public class MenuBlurModule extends Module {
	private static MenuBlurModule instance;

	public MenuBlurModule() {
		super("Menu Blur", "Blurs the world behind your inventory and chests.", Category.MECHANIC);
		setIcon(Items.CHEST);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}
}
