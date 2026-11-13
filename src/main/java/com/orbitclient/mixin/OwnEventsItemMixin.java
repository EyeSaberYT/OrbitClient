package com.orbitclient.mixin;

//? if <1.16.1 {
/*import com.orbitclient.compat.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// Minecraft 1.16 itself: fires Orbit's own item tooltip event (see ItemTooltipCallback).
@Mixin(ItemStack.class)
public class OwnEventsItemMixin {
	@Inject(method = "getTooltipLines", at = @At("RETURN"))
	private void orbit$tooltip(Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
		ItemTooltipCallback.EVENT.fire((ItemStack) (Object) this, flag, cir.getReturnValue());
	}
}
*///?}
