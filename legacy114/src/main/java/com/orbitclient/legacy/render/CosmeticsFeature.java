package com.orbitclient.legacy.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.orbitclient.cosmetics.Cosmetic;
import com.orbitclient.cosmetics.CosmeticManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.cosmetics.Cosmetics;
import com.orbitclient.legacy.Compat;
import com.orbitclient.legacy.Platform;
import com.orbitclient.legacy.gl.PoseStack;
import com.orbitclient.legacy.gl.VertexConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/** Draws a player's worn cosmetics on 1.14 (all but the cape, which the vanilla cape layer draws). */
public final class CosmeticsFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private final VertexConsumer collector = new VertexConsumer();

	public CosmeticsFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
		super(context);
	}

	@Override
	public void render(AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float age,
	                   float headYaw, float headPitch, float scale) {
		if (player.isInvisible()) return;
		CosmeticManager.Loadout loadout = CosmeticManager.of(player.getUuid());
		if (loadout == null || loadout.isEmpty()) return;
		long startedAt = com.orbitclient.legacy.Perf.start();

		PlayerState state = new PlayerState();
		state.id = player.getEntityId();
		state.x = player.prevX + (player.x - player.prevX) * tickDelta;
		state.y = player.prevY + (player.y - player.prevY) * tickDelta;
		state.z = player.prevZ + (player.z - player.prevZ) * tickDelta;
		// The body's turn, now and a tick ago (these two have no names in 1.14's mappings).
		state.bodyRot = player.field_6220 + wrap(player.field_6283 - player.field_6220) * tickDelta;
		state.walkAnimationPos = limbAngle;
		state.isFallFlying = Compat.fallFlying(player);
		state.walkAnimationSpeed = Math.min(1f, limbDistance);
		PlayerParts parts = new PlayerParts(getContextModel(), player.isSneaking());
		double time = System.nanoTime() / 1e9 + (state.id % 97) * 0.37;

		// Voxels are colored quads on a plain white texture, shaded by the world's light like the model.
		MinecraftClient mc = MinecraftClient.getInstance();
		Platform.ensureTexture(mc, Cosmetic.WHITE);
		mc.getTextureManager().bindTexture(Cosmetic.WHITE);
		collector.setLight(Compat.lightmap(player, tickDelta));
		GlStateManager.color4f(1f, 1f, 1f, 1f);
		GlStateManager.enableRescaleNormal();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		for (CosmeticSlot slot : CosmeticSlot.values()) {
			if (slot == CosmeticSlot.CAPE) continue;
			CosmeticManager.Worn worn = loadout.get(slot);
			Cosmetic c = worn == null ? null : Cosmetics.get(worn.id());
			if (c == null || c.renderer() == null) continue;
			c.renderer().render(new Cosmetic.Context(PoseStack.GL, collector, 0, parts, state, worn.color(), time, tickDelta));
		}
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.disableRescaleNormal();
		GlStateManager.color4f(1f, 1f, 1f, 1f);
		com.orbitclient.legacy.Perf.end(startedAt);
	}

	private static float wrap(float degrees) {
		degrees %= 360f;
		if (degrees >= 180f) degrees -= 360f;
		if (degrees < -180f) degrees += 360f;
		return degrees;
	}

	@Override
	public boolean hasHurtOverlay() {
		return false;
	}
}
