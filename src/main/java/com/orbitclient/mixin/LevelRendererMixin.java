package com.orbitclient.mixin;

import com.orbitclient.module.visual.BlockOutlineModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	//? if >=1.21.2 {
	/** Block Outline: the outline's color. */
	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int orbit$outlineColor(int color) {
		return BlockOutlineModule.color(color);
	}
	//?} else {
	/*/^* Block Outline: the outline's color. Before 1.21.2 the outline takes none, so it's put into the shape drawn. ^/
	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "renderHitOutline", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/LevelRenderer;renderShape(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/shapes/VoxelShape;DDDFFFF)V"))
	private void orbit$outlineColor(com.mojang.blaze3d.vertex.PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer consumer,
	                                net.minecraft.world.phys.shapes.VoxelShape shape, double x, double y, double z, float r, float g, float b, float a,
	                                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
		int vanilla = Math.round(a * 255) << 24 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
		int argb = BlockOutlineModule.color(vanilla);
		original.call(pose, consumer, shape, x, y, z, (argb >> 16 & 0xFF) / 255f, (argb >> 8 & 0xFF) / 255f, (argb & 0xFF) / 255f, (argb >>> 24) / 255f);
	}
	*///?}

	// The outline only takes a width from 1.21.11 on; before that its thickness can't be changed here.
	//? if >=1.21.11 {
	/** Block Outline: the outline's thickness (7.0 is high-contrast mode's black backing line, left alone). */
	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float orbit$outlineWidth(float width) {
		return width == 7.0f ? width : BlockOutlineModule.width(width, 0);
	}
	//?}
}
