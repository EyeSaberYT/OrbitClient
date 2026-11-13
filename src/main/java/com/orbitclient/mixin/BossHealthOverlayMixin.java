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
	//? if >=1.20 {
	@WrapMethod(method = "render")
	private void orbit$bossBar(GuiGraphics graphics, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.BOSS_BAR, graphics, Minecraft.getInstance().getDeltaTracker(), (g, d) -> original.call(g));
	}
	//?}
	//? if >=1.16 <1.20 {
	/^// Before 1.20 vanilla hands the boss bars a PoseStack.
	@WrapMethod(method = "render")
	private void orbit$bossBar(com.mojang.blaze3d.vertex.PoseStack pose, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.BOSS_BAR, GuiGraphics.of(pose), Minecraft.getInstance().getDeltaTracker(), (g, d) -> original.call(g.poseStack()));
	}
	^///?}
	//? if <1.16 {
	/^// Before 1.16 they draw straight to OpenGL.
	@WrapMethod(method = "render")
	private void orbit$bossBar(Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.BOSS_BAR, GuiGraphics.legacy(), Minecraft.getInstance().getDeltaTracker(), (g, d) -> original.call());
	}
	^///?}
}
*///?}
