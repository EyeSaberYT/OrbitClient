package com.orbitclient.mixin;

import com.orbitclient.module.server.NickHiderModule;
import com.orbitclient.cosmetics.OrbitBadge;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nick Hider: player names above heads (nametags use the display name). */
@Mixin(Player.class)
public class PlayerNameMixin {
	@ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
	private Component orbit$hideNametag(Component name) {
		return OrbitBadge.decorate(((Player) (Object) this).getUUID(), NickHiderModule.hide(name));
	}
}
