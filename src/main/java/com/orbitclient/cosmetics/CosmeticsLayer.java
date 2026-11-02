package com.orbitclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;

/** Draws a player's worn cosmetics (all but the cape, which the cape layer draws). */
public class CosmeticsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public CosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Entity entity = mc.level.getEntity(state.id);
		if (entity == null) return;
		CosmeticManager.Loadout loadout = CosmeticManager.of(entity.getUUID());
		if (loadout == null || loadout.isEmpty()) return;
		double time = System.nanoTime() / 1e9 + (state.id % 97) * 0.37;
		float delta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (CosmeticSlot slot : CosmeticSlot.values()) {
			if (slot == CosmeticSlot.CAPE) continue;
			CosmeticManager.Worn worn = loadout.get(slot);
			Cosmetic c = worn == null ? null : Cosmetics.get(worn.id());
			if (c == null || c.renderer == null) continue;
			c.renderer.render(new Cosmetic.Context(pose, collector, light, getParentModel(), state, worn.color(), time, delta));
		}
	}

	/** The cosmetic cape texture this player wears right now, or null. */
	public static net.minecraft.resources.Identifier capeFor(Entity entity, int id) {
		CosmeticManager.Loadout loadout = CosmeticManager.of(entity.getUUID());
		CosmeticManager.Worn worn = loadout == null ? null : loadout.get(CosmeticSlot.CAPE);
		Cosmetic c = worn == null ? null : Cosmetics.get(worn.id());
		return c == null ? null : c.capeTexture(System.nanoTime() / 1e9 + (id % 97) * 0.37);
	}
}
