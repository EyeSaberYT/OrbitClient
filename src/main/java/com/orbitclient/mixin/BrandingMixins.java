package com.orbitclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The client brand servers see (F3 "brand", plugins, crash reports): Orbit Client instead of "fabric". */
@Mixin(ClientBrandRetriever.class)
public class BrandingMixins {
	private static final String BRAND = "Orbit Client";

	@ModifyReturnValue(method = "getClientModName", at = @At("RETURN"))
	private static String orbit$brand(String brand) {
		return BRAND;
	}
}
