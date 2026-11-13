package com.orbitclient.compat;

//? if <1.17 {
/*import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Before 1.17 there was no ItemStack.is(Item); build.gradle sends stack.is(...) here.
public final class Stacks {
	private Stacks() {
	}

	public static boolean is(ItemStack stack, Item item) {
		return stack.getItem() == item;
	}
}
*///?}
