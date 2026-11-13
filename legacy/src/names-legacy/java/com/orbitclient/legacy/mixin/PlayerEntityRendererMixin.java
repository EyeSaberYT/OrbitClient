package com.orbitclient.legacy.mixin;

import com.orbitclient.legacy.render.CosmeticsFeature;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the cosmetics layer to every player renderer (the wide-armed and the slim one) as it is made. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity> {
	private PlayerEntityRendererMixin() {
		super(null, null, 0);
	}

	@Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRenderDispatcher;Z)V", at = @At("RETURN"))
	private void orbit$cosmetics(EntityRenderDispatcher dispatcher, boolean slim, CallbackInfo ci) {
		addFeature(new CosmeticsFeature((PlayerEntityRenderer) (Object) this));
	}
}
