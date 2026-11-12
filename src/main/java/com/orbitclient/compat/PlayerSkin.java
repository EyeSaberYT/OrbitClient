package com.orbitclient.compat;

//? if <1.20.2 {
/*import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/^* Before 1.20.2 a skin was just its texture locations: the part of 1.20.2's PlayerSkin Orbit uses. ^/
public record PlayerSkin(Identifier texture, Identifier capeTexture) {
	public static PlayerSkin of(AbstractClientPlayer player) {
		return new PlayerSkin(player.getSkinTextureLocation(), player.getCloakTextureLocation());
	}

	/^* A profile's skin, the default one until Mojang's has loaded (asked again on every get). ^/
	public static Supplier<PlayerSkin> lookup(GameProfile profile) {
		return () -> new PlayerSkin(Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile), null);
	}
}
*///?}
