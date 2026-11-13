package com.orbitclient.mixin;

//? if <1.16.1 {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Before 1.16.1 Fabric API has no callback for adding layers to entity renderers (see OrbitClient):
// the cosmetics layer is added where each player renderer is made.
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererLayerMixin extends LivingEntityRenderer<AbstractClientPlayer, net.minecraft.client.model.player.PlayerModel<AbstractClientPlayer>> {
	private PlayerRendererLayerMixin() {
		super(null, null, 0);
	}

	@SuppressWarnings("unchecked")
	@Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;Z)V", at = @At("RETURN"))
	private void orbit$cosmetics(EntityRenderDispatcher dispatcher, boolean slim, CallbackInfo ci) {
		var parent = (RenderLayerParent<AbstractClientPlayer, net.minecraft.client.model.player.PlayerModel<AbstractClientPlayer>>) (Object) this;
		addLayer(new com.orbitclient.cosmetics.CosmeticsLayer(parent));
	}
}
*///?}
