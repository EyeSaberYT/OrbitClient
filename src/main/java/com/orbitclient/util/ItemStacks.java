package com.orbitclient.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Makes example item stacks for menus and the HUD editor.
 *
 * <p>On newer versions (26.2+) an item's components are only bound once the game's registries are loaded
 * for a world, so {@code new ItemStack(item)} throws "Components not bound yet" on the title screen.
 * These helpers return {@link ItemStack#EMPTY} instead, and the caller simply draws nothing until it works.
 */
public final class ItemStacks {
	private ItemStacks() {
	}

	public static ItemStack of(Item item) {
		if (item == null) return ItemStack.EMPTY;
		try {
			return new ItemStack(item);
		} catch (RuntimeException notReady) {
			return ItemStack.EMPTY;
		}
	}
}
