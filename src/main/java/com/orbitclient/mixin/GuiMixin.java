package com.orbitclient.mixin;

import com.orbitclient.util.VanillaAnimations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? if >=1.21.6
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class GuiMixin {
	// Slides the hotbar selection highlight between slots. Only the selection sprite is moved. The sprite is
	// drawn with a pipeline from 1.21.6, a RenderType factory in 1.21.2 to 1.21.5, and by itself before that.

	//? if >=1.21.6 {
	@WrapOperation(method = "renderItemHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void orbit$smoothHotbar(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h,
	                                  Operation<Void> original) {
		original.call(graphics, pipeline, sprite, slid(sprite, x), y, w, h);
	}
	//?}

	//? if >=1.21.2 <1.21.6 {
	/*@WrapOperation(method = "renderItemHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void orbit$smoothHotbar(GuiGraphics graphics, java.util.function.Function<Identifier, net.minecraft.client.renderer.RenderType> pipeline,
	                                  Identifier sprite, int x, int y, int w, int h, Operation<Void> original) {
		original.call(graphics, pipeline, sprite, slid(sprite, x), y, w, h);
	}
	*///?}

	//? if >=1.20.5 <1.21.2 {
	/*@WrapOperation(method = "renderItemHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/Identifier;IIII)V"))
	private void orbit$smoothHotbar(GuiGraphics graphics, Identifier sprite, int x, int y, int w, int h, Operation<Void> original) {
		original.call(graphics, sprite, slid(sprite, x), y, w, h);
	}
	*///?}

	//? if >=1.20.2 <1.20.5 {
	/*@WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/Identifier;IIII)V"))
	private void orbit$smoothHotbar(GuiGraphics graphics, Identifier sprite, int x, int y, int w, int h, Operation<Void> original) {
		original.call(graphics, sprite, slid(sprite, x), y, w, h);
	}
	*///?}

	//? if <1.16 {
	/*// Before 1.16 it takes no pose stack either.
	@WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;blit(IIIIII)V"))
	private void orbit$smoothHotbar(net.minecraft.client.gui.Gui gui, int x, int y, int u, int v, int w, int h, Operation<Void> original) {
		original.call(gui, u == 0 && v == 22 && w == 24 ? VanillaAnimations.hotbarSelectionX(x) : x, y, u, v, w, h);
	}
	*///?}

	//? if >=1.16 <1.19.4 {
	/*// Before 1.19.4 that blit is an instance method (the Gui itself comes first).
	@WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
	private void orbit$smoothHotbar(net.minecraft.client.gui.Gui gui, com.mojang.blaze3d.vertex.PoseStack pose, int x, int y, int u, int v, int w, int h, Operation<Void> original) {
		original.call(gui, pose, u == 0 && v == 22 && w == 24 ? VanillaAnimations.hotbarSelectionX(x) : x, y, u, v, w, h);
	}
	*///?}

	//? if >=1.19.4 <1.20 {
	/*// Before 1.20 the selection is drawn with GuiComponent's static blit on the already-bound widgets texture.
	@WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
	private void orbit$smoothHotbar(com.mojang.blaze3d.vertex.PoseStack pose, int x, int y, int u, int v, int w, int h, Operation<Void> original) {
		original.call(pose, u == 0 && v == 22 && w == 24 ? VanillaAnimations.hotbarSelectionX(x) : x, y, u, v, w, h);
	}
	*///?}

	//? if >=1.20 <1.20.2 {
	/*// Before 1.20.2 there are no GUI sprites: the selection is the 24x22 piece at (0, 22) of the widgets texture.
	@WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/Identifier;IIIIII)V"))
	private void orbit$smoothHotbar(GuiGraphics graphics, Identifier texture, int x, int y, int u, int v, int w, int h, Operation<Void> original) {
		original.call(graphics, texture, u == 0 && v == 22 && w == 24 ? VanillaAnimations.hotbarSelectionX(x) : x, y, u, v, w, h);
	}
	*///?}

	private static int slid(Identifier sprite, int x) {
		return sprite.getPath().equals("hud/hotbar_selection") ? VanillaAnimations.hotbarSelectionX(x) : x;
	}
}
