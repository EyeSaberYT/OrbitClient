package com.orbitclient.mixin;

//? if >=26.2 {
/*import com.orbitclient.gui.OrbitHomeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/^* Orbit's home screen from 26.2 on, where Gui opens the screens instead of Minecraft (see MinecraftMixin). *^/
// Named in full: build.gradle retargets the short form to Hud for 26.2's HUD mixins.
@Mixin(net.minecraft.client.gui.Gui.class)
public class GuiScreenMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen orbit$homeScreen(Screen screen) {
		return OrbitHomeScreen.replacing(screen, minecraft);
	}
}
*///?}
