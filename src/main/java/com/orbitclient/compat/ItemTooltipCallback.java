package com.orbitclient.compat;

//? if <1.16.1 {
/*import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Minecraft 1.16 itself: Orbit's own item tooltip event (see ClientTickEvents), fired by OwnEventsItemMixin.
public interface ItemTooltipCallback {
	void getTooltip(ItemStack stack, TooltipFlag flag, List<Component> lines);

	final class Event {
		private final List<ItemTooltipCallback> listeners = new CopyOnWriteArrayList<>();

		public void register(ItemTooltipCallback listener) {
			listeners.add(listener);
		}

		public void fire(ItemStack stack, TooltipFlag flag, List<Component> lines) {
			for (ItemTooltipCallback listener : listeners) listener.getTooltip(stack, flag, lines);
		}
	}

	Event EVENT = new Event();
}
*///?}
