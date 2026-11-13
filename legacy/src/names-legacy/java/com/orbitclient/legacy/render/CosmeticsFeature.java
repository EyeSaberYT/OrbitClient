package com.orbitclient.legacy.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.orbitclient.cosmetics.Cosmetic;
import com.orbitclient.cosmetics.CosmeticManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.cosmetics.Cosmetics;
import com.orbitclient.legacy.gl.PoseStack;
import com.orbitclient.legacy.gl.VertexConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;

/** Draws a player's worn cosmetics on the old engine (all but the cape, which the vanilla cape layer draws). */
public final class CosmeticsFeature implements FeatureRenderer<AbstractClientPlayerEntity> {
	private final PlayerEntityRenderer renderer;
	private final VertexConsumer collector = new VertexConsumer();

	public CosmeticsFeature(PlayerEntityRenderer renderer) {
		this.renderer = renderer;
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
		state.bodyRot = player.prevBodyYaw + wrap(player.bodyYaw - player.prevBodyYaw) * tickDelta;
		state.walkAnimationPos = limbAngle;
		state.isFallFlying = com.orbitclient.legacy.Compat.fallFlying(player);
		state.walkAnimationSpeed = Math.min(1f, limbDistance);
		PlayerParts parts = new PlayerParts(renderer.getModel(), player.isSneaking());
		double time = System.nanoTime() / 1e9 + (state.id % 97) * 0.37;

		// Voxels are colored quads on a plain white texture, shaded by the world's light like the model.
		com.orbitclient.legacy.Platform.ensureTexture(MinecraftClient.getInstance(), Cosmetic.WHITE);
		MinecraftClient.getInstance().getTextureManager().bindTexture(Cosmetic.WHITE);
		collector.setLight(com.orbitclient.legacy.Compat.lightmap(player, tickDelta));
		GlStateManager.color(1f, 1f, 1f, 1f);
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
		GlStateManager.color(1f, 1f, 1f, 1f);
		com.orbitclient.legacy.Perf.end(startedAt);
	}

	private static float wrap(float degrees) {
		degrees %= 360f;
		if (degrees >= 180f) degrees -= 360f;
		if (degrees < -180f) degrees += 360f;
		return degrees;
	}

	@Override
	public boolean combineTextures() {
		return false;
	}
}
