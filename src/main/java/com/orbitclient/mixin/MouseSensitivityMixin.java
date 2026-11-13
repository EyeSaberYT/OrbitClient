package com.orbitclient.mixin;

//? if <1.19 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.orbitclient.module.mechanics.ZoomModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Before 1.19 options were plain fields (see OptionInstanceMixin for 1.19+): Zoom slows the mouse where
// the sensitivity is read, so the real value in options.txt is never changed.
@Mixin(MouseHandler.class)
public class MouseSensitivityMixin {
	@ModifyExpressionValue(method = "turnPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;sensitivity:D"))
	private double orbit$sensitivity(double sensitivity) {
		return sensitivity * ZoomModule.sensitivityMultiplier();
	}
}
*///?}
