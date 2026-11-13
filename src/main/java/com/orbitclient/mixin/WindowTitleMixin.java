package com.orbitclient.mixin;

// Minecraft.createTitle is new in 1.15.2.
//? if >=1.15.2 {
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class WindowTitleMixin {
	/** "Minecraft* 1.21.11 - Multiplayer" becomes "Orbit Client 1.21.11 - Multiplayer". */
	@ModifyReturnValue(method = "createTitle", at = @At("RETURN"))
	private String orbit$title(String title) {
		return title.replaceFirst("^Minecraft\\*?", "Orbit Client");
	}
}
//?}
