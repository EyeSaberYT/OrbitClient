package com.orbitclient.mixin;

import com.orbitclient.gui.OrbitHomeScreen;
import com.orbitclient.module.visual.TeamViewModule;
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
	/**
	 * Orbit's home screen replaces the vanilla title screen; every other screen is left alone. Closing a
	 * screen outside a world is setScreen(null), which makes its own TitleScreen further down, so that
	 * case is the home screen too.
	 */
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen orbit$homeScreen(Screen screen) {
		if (screen == null && ((Minecraft) (Object) this).level == null) return new OrbitHomeScreen();
		return screen instanceof TitleScreen && !(screen instanceof OrbitHomeScreen) ? new OrbitHomeScreen() : screen;
	}

	/** Team View: teammates glow in their team color. */
	@ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
	private boolean orbit$teamGlow(boolean glowing, @Local(argsOnly = true) Entity entity) {
		return glowing || TeamViewModule.glows(entity);
	}
}
