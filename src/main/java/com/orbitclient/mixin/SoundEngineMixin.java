package com.orbitclient.mixin;

import com.orbitclient.module.server.SoundChangerModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
	/** Sound Changer: scales the volume of the sound groups it controls. */
	@ModifyReturnValue(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"))
	private float orbit$soundVolume(float volume, @Local(argsOnly = true) SoundInstance sound) {
		return SoundChangerModule.volume(sound, volume);
	}
}
