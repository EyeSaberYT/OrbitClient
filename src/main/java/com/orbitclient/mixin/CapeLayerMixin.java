package com.orbitclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.orbitclient.cape.CapeMesh;
import com.orbitclient.cape.ClothCape;
import com.orbitclient.cosmetics.CosmeticsLayer;
import com.orbitclient.module.visual.WaveyCapesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
//? if >=1.21.2 {
public abstract class CapeLayerMixin extends RenderLayer<AvatarRenderState, PlayerModel> {
	@Shadow
	private boolean hasLayer(ItemStack stack, EquipmentClientInfo.LayerType type) {
		throw new AssertionError();
	}

	private CapeLayerMixin(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}
//?} else {
/*public abstract class CapeLayerMixin extends RenderLayer<net.minecraft.client.player.AbstractClientPlayer, PlayerModel<net.minecraft.client.player.AbstractClientPlayer>> {
	private CapeLayerMixin(RenderLayerParent<net.minecraft.client.player.AbstractClientPlayer, PlayerModel<net.minecraft.client.player.AbstractClientPlayer>> parent) {
		super(parent);
	}

	/^* Before 1.21.2 vanilla only checks for an elytra (wings), with no nudge over armor. ^/
	private boolean hasLayer(ItemStack stack, boolean wings) {
		return wings && stack.is(net.minecraft.world.item.Items.ELYTRA);
	}
*///?}

	/**
	 * Draws capes ourselves when a cosmetic cape is worn or Wavey Capes is on: as cloth with Wavey Capes,
	 * otherwise straight with vanilla's lean. Same rules as vanilla for who shows one (not invisible,
	 * cape shown in skin options, no elytra) and the same nudge out over chest armor.
	 */
	//? if >=1.21.9 {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
		at = @At("HEAD"), cancellable = true)
	private void orbit$cape(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
	//?}
	//? if >=1.21.2 <1.21.9 {
	/*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
		at = @At("HEAD"), cancellable = true)
	private void orbit$cape(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
		SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
	*///?}
	//? if <1.21.2 {
	/*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
		at = @At("HEAD"), cancellable = true)
	private void orbit$cape(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, net.minecraft.client.player.AbstractClientPlayer player,
	                        float limbSwing, float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch, CallbackInfo ci) {
		SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
		AvatarRenderState state = AvatarRenderState.of(player, partialTick);
	*///?}
		Minecraft mc = Minecraft.getInstance();
		Entity entity = mc.level == null ? null : mc.level.getEntity(state.id);
		Identifier cosmetic = entity == null ? null : CosmeticsLayer.capeFor(entity, state.id);
		boolean cloth = WaveyCapesModule.active();
		if (cosmetic == null && !cloth) return; // vanilla's cape
		ci.cancel();
		if (state.isInvisible || !state.showCape) return;
		Identifier texture = cosmetic != null ? cosmetic
			: state.skin.cape() != null ? state.skin.cape().texturePath() : WaveyCapesModule.testCape;
		if (texture == null || hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) return;

		float[] points = cloth && entity != null
			? ClothCape.of(entity.getId()).points(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false))
			: ClothCape.rigid(state);
		boolean smooth = WaveyCapesModule.smooth();

		pose.pushPose();
		if (hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) pose.translate(0, -0.053125f, 0.06875f);
		getParentModel().root().translateAndRotate(pose);
		getParentModel().body.translateAndRotate(pose);
		pose.translate(0, 0, 2f / 16f); // from the neck pivot out to the back
		collector.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutNoCull(texture),
			(entry, consumer) -> CapeMesh.draw(entry, consumer, points, light, smooth));
		pose.popPose();
	}
}
