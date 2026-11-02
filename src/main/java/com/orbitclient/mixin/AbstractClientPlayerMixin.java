package com.orbitclient.mixin;

import com.orbitclient.module.mechanics.FovChangerModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
	private float orbit$fov(float modifier) {
		return FovChangerModule.modify(modifier);
	}
}
