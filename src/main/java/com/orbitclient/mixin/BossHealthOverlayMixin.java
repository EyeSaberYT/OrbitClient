package com.orbitclient.mixin;

//? if <1.21.6 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.orbitclient.compat.HudElementRegistry;
import com.orbitclient.compat.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;

/^* Before 1.21.6: the boss bars go through HudElementRegistry (see GuiHudMixin). ^/
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
	@WrapMethod(method = "render")
	private void orbit$bossBar(GuiGraphics graphics, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.BOSS_BAR, graphics, Minecraft.getInstance().getDeltaTracker(), (g, d) -> original.call(g));
	}
}
*///?}
