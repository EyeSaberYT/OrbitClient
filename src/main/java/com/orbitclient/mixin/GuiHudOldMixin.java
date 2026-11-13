package com.orbitclient.mixin;

//? if >=1.16 <1.20 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.orbitclient.compat.DeltaTracker;
import com.orbitclient.compat.HudElementRegistry;
import com.orbitclient.compat.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 1.16 to 1.19.4 (see GuiHudLegacyMixin for 1.20 to 1.20.4, GuiHud115Mixin before 1.16): the same seams, but vanilla hands the HUD a PoseStack
// instead of a GuiGraphics. Orbit's own GuiGraphics (com.orbitclient.compat) wraps it.
// Named in full: build.gradle retargets the short form to Hud for 26.2's HUD mixins.
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiHudOldMixin {
	@WrapMethod(method = "renderCrosshair")
	private void orbit$crosshair(PoseStack pose, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CROSSHAIR, GuiGraphics.of(pose), delta(), (g, d) -> original.call(g.poseStack()));
	}

	@WrapMethod(method = "renderEffects")
	private void orbit$effects(PoseStack pose, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.STATUS_EFFECTS, GuiGraphics.of(pose), delta(), (g, d) -> original.call(g.poseStack()));
	}

	@WrapMethod(method = "displayScoreboardSidebar")
	private void orbit$scoreboard(PoseStack pose, Objective objective, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.SCOREBOARD, GuiGraphics.of(pose), delta(), (g, d) -> original.call(g.poseStack(), objective));
	}

	@WrapMethod(method = "renderPlayerHealth")
	private void orbit$food(PoseStack pose, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.FOOD_BAR, GuiGraphics.of(pose), delta(), (g, d) -> original.call(g.poseStack()));
	}

	//? if >=1.19.3 {
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lcom/mojang/blaze3d/vertex/PoseStack;III)V"))
	private void orbit$chat(ChatComponent chat, PoseStack pose, int tick, int mx, int my, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CHAT, GuiGraphics.of(pose), delta(), (g, d) -> original.call(chat, g.poseStack(), tick, mx, my));
	}
	//?} else {
	/^// Before 1.19.3 the chat wasn't told where the mouse is.
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lcom/mojang/blaze3d/vertex/PoseStack;I)V"))
	private void orbit$chat(ChatComponent chat, PoseStack pose, int tick, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CHAT, GuiGraphics.of(pose), delta(), (g, d) -> original.call(chat, g.poseStack(), tick));
	}
	^///?}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"))
	private void orbit$tabList(PlayerTabOverlay tabList, PoseStack pose, int width, Scoreboard scoreboard, Objective objective, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.PLAYER_LIST, GuiGraphics.of(pose), delta(), (g, d) -> original.call(tabList, g.poseStack(), width, scoreboard, objective));
	}

	private static DeltaTracker delta() {
		return DeltaTracker.of(Minecraft.getInstance());
	}
}
*///?}
