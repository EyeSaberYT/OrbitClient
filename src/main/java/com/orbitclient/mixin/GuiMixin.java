package com.orbitclient.mixin;

import com.orbitclient.util.VanillaAnimations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class GuiMixin {
	/** Slides the hotbar selection highlight between slots. Only the selection sprite is moved. */
	@WrapOperation(method = "renderItemHotbar", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void orbit$smoothHotbar(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h,
	                                  Operation<Void> original) {
		if (sprite.getPath().equals("hud/hotbar_selection")) x = VanillaAnimations.hotbarSelectionX(x);
		original.call(graphics, pipeline, sprite, x, y, w, h);
	}
}
