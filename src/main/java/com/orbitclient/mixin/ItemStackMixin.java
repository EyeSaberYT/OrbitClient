package com.orbitclient.mixin;

import com.orbitclient.module.visual.GlintColorizerModule;
import com.orbitclient.module.visual.ShinyPotsModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	/** Shiny Pots: potions get the enchantment glint. Glint Colorizer's All Items: every item does. */
	@ModifyReturnValue(method = "hasFoil", at = @At("RETURN"))
	private boolean orbit$shinyPots(boolean foil) {
		ItemStack stack = (ItemStack) (Object) this;
		return foil || ShinyPotsModule.shiny(stack) || GlintColorizerModule.glintAll(stack);
	}
}
