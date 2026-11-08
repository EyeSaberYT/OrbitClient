package com.orbitclient.mixin;

import com.orbitclient.module.visual.BlockOutlineModule;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	/** Block Outline: the outline's color. */
	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int orbit$outlineColor(int color) {
		return BlockOutlineModule.color(color);
	}

	// The outline only takes a width from 1.21.11 on; before that its thickness can't be changed here.
	//? if >=1.21.11 {
	/** Block Outline: the outline's thickness (7.0 is high-contrast mode's black backing line, left alone). */
	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float orbit$outlineWidth(float width) {
		return width == 7.0f ? width : BlockOutlineModule.width(width, 0);
	}
	//?}
}
