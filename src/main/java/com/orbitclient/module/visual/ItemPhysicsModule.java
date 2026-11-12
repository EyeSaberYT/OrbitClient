package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.orbitclient.util.PoseUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
//? if >=1.21.2
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

	/** Vanilla's ground sizes for a block (a quarter-size cube) and a flat item (a half-size sprite, one pixel thick). */
	private static final AABB BLOCK = new AABB(-0.125, 0.0625, -0.125, 0.125, 0.3125, 0.125);
	private static final AABB FLAT = new AABB(-0.25, -0.125, -1 / 64.0, 0.25, 0.375, 1 / 64.0);

	/** Turns the pose so an item of this size lies on the ground at its own fixed angle (pushed; the caller pops). */
	private static void lay(PoseStack pose, AABB box, int seed) {
		boolean flat = box.getZsize() < 0.2;
		float angle = (seed & 0xFFFF) / 65536f * 360f;
		pose.pushPose();
		PoseUtil.mulPose(pose, Axis.YP.rotationDegrees(angle));
		if (flat) {
			pose.translate(0f, 0.02f, 0f);
			PoseUtil.mulPose(pose, Axis.XP.rotationDegrees(90));
			// Centered on the entity, resting on its face.
			pose.translate(0f, (float) (-(box.minY + box.maxY) / 2), (float) (box.getZsize() / 2));
		} else {
			pose.translate(0f, (float) -box.minY, 0f);
		}
	}

	//? if <1.21.2 {
	/*/^* Draws a dropped item lying on the ground (replaces the floating, spinning version). ^/
	public static void submit(net.minecraft.world.entity.item.ItemEntity entity, PoseStack pose, SubmitNodeCollector collector, int light, RandomSource random) {
		net.minecraft.world.item.ItemStack stack = entity.getItem();
		if (stack.isEmpty()) return;
		var itemRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
		var model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());
		//? if >=1.20.5 {
		lay(pose, model.isGui3d() ? BLOCK : FLAT, ItemEntityRenderer.getSeedForItemStack(stack));
		ItemEntityRenderer.renderMultipleFromCount(itemRenderer, pose, collector.buffers(), light, stack, model, model.isGui3d(), random);
		//?} else {
		/^// Before 1.20.5 there's no shared helper for a stack's copies: the item is drawn once.
		lay(pose, model.isGui3d() ? BLOCK : FLAT, entity.getId() * 31 + net.minecraft.world.item.Item.getId(stack.getItem()));
		itemRenderer.render(stack, net.minecraft.world.item.ItemDisplayContext.GROUND, false, pose, collector.buffers(), light,
			net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, model);
		^///?}
		pose.popPose();
	}
	*///?} else {
	/** Draws a dropped item lying on the ground (replaces the floating, spinning version). */
	public static void submit(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, int light, RandomSource random) {
		if (state.item.isEmpty()) return;
		AABB box = box(state);
		//? if >=1.21.4 {
		int seed = state.seed;
		//?} else
		/*int seed = ItemEntityRenderer.getSeedForItemStack(state.item);*/
		lay(pose, box, seed);
		//? if >=1.21.9 {
		ItemEntityRenderer.submitMultipleFromCount(pose, collector, light, state, random, box);
		//?}
		//? if >=1.21.5 <1.21.9 {
		/*ItemEntityRenderer.renderMultipleFromCount(pose, collector.buffers(), light, state, random, box);
		*///?}
		//? if >=1.21.4 <1.21.5 {
		/*ItemEntityRenderer.renderMultipleFromCount(pose, collector.buffers(), light, state, random);
		*///?}
		//? if <1.21.4 {
		/*ItemEntityRenderer.renderMultipleFromCount(net.minecraft.client.Minecraft.getInstance().getItemRenderer(), pose, collector.buffers(), light,
			state.item, state.itemModel, state.itemModel.isGui3d(), random);
		*///?}
		pose.popPose();
	}

	/** The item's model bounds as drawn on the ground. */
	private static AABB box(ItemEntityRenderState state) {
		//? if >=1.21.6 {
		return state.item.getModelBoundingBox();
		//?}
		//? if >=1.21.5 <1.21.6 {
		/*return com.orbitclient.mixin.ItemEntityRendererAccessor.orbit$modelBoundingBox(state.item);
		*///?}
		//? if <1.21.5 {
		/*// Before 1.21.5 the game doesn't measure the model.
		//? if >=1.21.4 {
		boolean block = state.item.isGui3d();
		//?} else
		/^boolean block = state.itemModel.isGui3d();^/
		return block ? BLOCK : FLAT;
		*///?}
	}
	//?}
}
