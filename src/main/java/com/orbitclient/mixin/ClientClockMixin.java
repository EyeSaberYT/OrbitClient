package com.orbitclient.mixin;

//? if >=26.1 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.orbitclient.module.mechanics.TimeChangerModule;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^* Time Changer from 26.1 on, where the sky follows the client's world clocks instead of the day time. *^/
//? if >=26.3 {
@Mixin(ClientClockManager.ClientClockInstance.class)
//?} else
/^@Mixin(ClientClockManager.class)^/
public class ClientClockMixin {
	//? if >=26.3 {
	@ModifyReturnValue(method = "totalTicks", at = @At("RETURN"))
	//?} else
	/^@ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"))^/
	private long orbit$time(long ticks) {
		return TimeChangerModule.modify(ticks);
	}
}
*///?}
