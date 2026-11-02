package com.orbitclient.mixin;

import com.orbitclient.module.server.NickHiderModule;
import com.orbitclient.cosmetics.OrbitBadge;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nick Hider: names in the tab list. */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
	@ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
	private Component orbit$hideTabName(Component name, PlayerInfo info) {
		return OrbitBadge.decorate(info.getProfile().id(), NickHiderModule.hide(name));
	}
}
