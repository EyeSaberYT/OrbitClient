package com.orbitclient.mixin;

import com.orbitclient.module.visual.TeamViewModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	/** Team View: teammates glow in their team color. */
	@ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
	private boolean orbit$teamGlow(boolean glowing, @Local(argsOnly = true) Entity entity) {
		return glowing || TeamViewModule.glows(entity);
	}
}
