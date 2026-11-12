package com.orbitclient.mixin;

import com.orbitclient.module.visual.NametagsModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	/** Nametags: your own nametag in third person, or hidden nametags. */
	//? if >=1.21.2 {
	@ModifyReturnValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"))
	//?} else
	/*@ModifyReturnValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))*/
	private boolean orbit$nametags(boolean show, @Local(argsOnly = true) LivingEntity entity) {
		return NametagsModule.shouldShow(entity, show);
	}
}
