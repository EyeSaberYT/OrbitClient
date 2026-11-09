package com.orbitclient.mixin;

import com.orbitclient.module.mechanics.TimeChangerModule;
import com.orbitclient.module.mechanics.WeatherChangerModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Client-side time and weather. Only the client's level is touched, never the server's. */
@Mixin(Level.class)
public class LevelMixin {
	// 26.1 replaced the day time with world clocks: see ClientClockMixin.
	//? if <26.1 {
	@ModifyReturnValue(method = "getDayTime", at = @At("RETURN"))
	private long orbit$time(long time) {
		return (Object) this instanceof ClientLevel ? TimeChangerModule.modify(time) : time;
	}
	//?}

	@ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
	private float orbit$rain(float rain) {
		return (Object) this instanceof ClientLevel ? WeatherChangerModule.modifyRain(rain) : rain;
	}

	@ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
	private float orbit$thunder(float thunder) {
		return (Object) this instanceof ClientLevel ? WeatherChangerModule.modifyThunder(thunder) : thunder;
	}
}
