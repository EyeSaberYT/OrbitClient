package com.orbitclient.mixin;

import com.orbitclient.module.visual.TeamViewModule;
import com.orbitclient.gui.OrbitHomeScreen;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	/** Replace the vanilla title screen with Orbit's in-game home, leaving all other screens alone. */
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen orbit$homeScreen(Screen screen) {
		return screen instanceof TitleScreen && !(screen instanceof OrbitHomeScreen) ? new OrbitHomeScreen() : screen;
	}

	/** Team View: teammates glow in their team color. */
	@ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
	private boolean orbit$teamGlow(boolean glowing, @Local(argsOnly = true) Entity entity) {
		return glowing || TeamViewModule.glows(entity);
	}
}
