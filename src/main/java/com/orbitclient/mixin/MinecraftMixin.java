package com.orbitclient.mixin;

import com.orbitclient.gui.OrbitHomeScreen;
import com.orbitclient.module.visual.TeamViewModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	//? if <26.2 {
	/** Orbit's home screen replaces the vanilla title screen (OrbitHomeScreen.replacing). 26.2 moved setScreen to Gui: GuiScreenMixin. */
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen orbit$homeScreen(Screen screen) {
		return OrbitHomeScreen.replacing(screen, (Minecraft) (Object) this);
	}
	//?}

	/** Team View: teammates glow in their team color. (Before 1.16: EntityMixin.) */
	//? if >=1.16 {
	@ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
	private boolean orbit$teamGlow(boolean glowing, @Local(argsOnly = true) Entity entity) {
		return glowing || TeamViewModule.glows(entity);
	}
	//?}
}
