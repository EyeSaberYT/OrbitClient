package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Gives potions the enchantment glint, so they're easy to spot in your hotbar and inventory. */
public class ShinyPotsModule extends Module {
	private static ShinyPotsModule instance;

	public ShinyPotsModule() {
		super("Shiny Pots", "Makes potions shine like enchanted items.", Category.MECHANIC);
		setIcon(Items.SPLASH_POTION);
		instance = this;
	}

	public static boolean shiny(ItemStack stack) {
		return instance != null && instance.isEnabled()
			&& (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
	}
}
