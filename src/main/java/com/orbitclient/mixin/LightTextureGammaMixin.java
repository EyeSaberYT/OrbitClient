package com.orbitclient.mixin;

//? if <1.19 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.orbitclient.module.visual.FullbrightModule;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Before 1.19 options were plain fields (see OptionInstanceMixin for 1.19+): Fullbright changes the gamma
// where the light map reads it, so the real value in options.txt is never changed.
@Mixin(LightTexture.class)
public class LightTextureGammaMixin {
	@ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;gamma:D"))
	private double orbit$gamma(double gamma) {
		return FullbrightModule.modifyGamma(gamma);
	}
}
*///?}
