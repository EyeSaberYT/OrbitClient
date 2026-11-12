package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import com.orbitclient.setting.BooleanSetting;
//? if >=1.20.5
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.20.5
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class PotionCounterModule extends TextHudModule {
	private final BooleanSetting onlyHealing;

	public PotionCounterModule() {
		super("Potion Counter", "Counts the splash potions left in your inventory.", 0.16, 0.64);
		setIcon(Items.SPLASH_POTION);
		onlyHealing = add(new BooleanSetting("Only Instant Health", true));
	}

	@Override
	protected String getText() {
		if (mc.player == null) return "0 Pots";
		int count = 0;
		var inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.is(Items.SPLASH_POTION)) continue;
			if (onlyHealing.isOn()) {
				//? if >=1.20.5 {
				PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
				if (contents == null || !(contents.is(Potions.HEALING) || contents.is(Potions.STRONG_HEALING))) continue;
				//?} else {
				/*var potion = net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack);
				if (potion != Potions.HEALING && potion != Potions.STRONG_HEALING) continue;
				*///?}
			}
			count += stack.getCount();
		}
		return count + (count == 1 ? " Pot" : " Pots");
	}

	@Override
	protected String getPreviewText() {
		return "28 Pots";
	}
}
