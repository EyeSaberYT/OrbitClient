package com.orbitclient.mixin;

// Options became OptionInstance in 1.19 (before: LightTextureGammaMixin and MouseSensitivityMixin).
//? if >=1.19 {
import com.orbitclient.module.mechanics.ZoomModule;
import com.orbitclient.module.visual.FullbrightModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Overrides gamma (Fullbright) and mouse sensitivity (Zoom) at read time,
 * so the real values in options.txt are never changed.
 */
@Mixin(OptionInstance.class)
public class OptionInstanceMixin {
	@ModifyReturnValue(method = "get", at = @At("RETURN"))
	private Object orbit$override(Object value) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null) return value;
		Options options = mc.options;
		if (options == null) return value;

		Object self = this;
		if (self == options.gamma() && value instanceof Double gamma) {
			return FullbrightModule.modifyGamma(gamma);
		} else if (self == options.sensitivity() && value instanceof Double sensitivity) {
			double multiplier = ZoomModule.sensitivityMultiplier();
			if (multiplier != 1) return sensitivity * multiplier;
		}
		return value;
	}
}
//?}
