package com.orbitclient.mixin;

import com.orbitclient.cosmetics.CosmeticManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	/** Cosmetics: lifts a player's nametag above a tall hat (crown, top hat, halo) instead of through it. */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
	private void orbit$liftNameTag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
		if (!(entity instanceof Player) || state.nameTagAttachment == null) return;
		float lift = CosmeticManager.nameTagLift(entity.getUUID());
		if (lift > 0) state.nameTagAttachment = state.nameTagAttachment.add(0, lift, 0);
	}
}
