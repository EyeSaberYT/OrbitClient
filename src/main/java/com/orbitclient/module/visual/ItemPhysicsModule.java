package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Dropped items lie flat on the ground instead of floating and spinning. Flat items (tools, ingots)
 * lie on their side; blocks sit on the ground. Each item gets its own fixed angle.
 */
public class ItemPhysicsModule extends Module {
	private static ItemPhysicsModule instance;

	public ItemPhysicsModule() {
		super("Item Physics", "Dropped items lie flat on the ground instead of spinning.", Category.MECHANIC);
		setIcon(Items.IRON_INGOT);
		instance = this;
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Draws a dropped item lying on the ground (replaces the floating, spinning version). */
	public static void submit(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, RandomSource random) {
		if (state.item.isEmpty()) return;
		AABB box = state.item.getModelBoundingBox();
		boolean flat = box.getZsize() < 0.2;
		float angle = (state.seed & 0xFFFF) / 65536f * 360f;
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(angle));
		if (flat) {
			pose.translate(0f, 0.02f, 0f);
			pose.mulPose(Axis.XP.rotationDegrees(90));
			// Centered on the entity, resting on its face.
			pose.translate(0f, (float) (-(box.minY + box.maxY) / 2), (float) (box.getZsize() / 2));
		} else {
			pose.translate(0f, (float) -box.minY, 0f);
		}
		ItemEntityRenderer.submitMultipleFromCount(pose, collector, state.lightCoords, state, random, box);
		pose.popPose();
	}
}
