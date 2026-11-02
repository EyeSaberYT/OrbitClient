package com.orbitclient.mixin;

import com.orbitclient.module.visual.ParticleChangerModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	/** Particle Changer: crit/sharpness particles on every hit. */
	@Inject(method = "attack", at = @At("HEAD"))
	private void orbit$hitParticles(Player player, Entity target, CallbackInfo ci) {
		ParticleChangerModule.onAttack(target);
	}
}
