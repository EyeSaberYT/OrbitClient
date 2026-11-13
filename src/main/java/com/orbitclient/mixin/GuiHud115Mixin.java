package com.orbitclient.mixin;

//? if <1.16 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

// Before 1.16 (see GuiHudOldMixin for 1.16 to 1.19.4): the same seams, but the HUD draws straight to OpenGL
// with no pose stack at all. Orbit's own GuiGraphics keeps one for its transforms.
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiHud115Mixin {
	@WrapMethod(method = "renderCrosshair")
	private void orbit$crosshair(Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CROSSHAIR, GuiGraphics.legacy(), delta(), (g, d) -> original.call());
	}

	@WrapMethod(method = "renderEffects")
	private void orbit$effects(Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.STATUS_EFFECTS, GuiGraphics.legacy(), delta(), (g, d) -> original.call());
	}

	@WrapMethod(method = "displayScoreboardSidebar")
	private void orbit$scoreboard(Objective objective, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.SCOREBOARD, GuiGraphics.legacy(), delta(), (g, d) -> original.call(objective));
	}

	@WrapMethod(method = "renderPlayerHealth")
	private void orbit$food(Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.FOOD_BAR, GuiGraphics.legacy(), delta(), (g, d) -> original.call());
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;render(I)V"))
	private void orbit$chat(ChatComponent chat, int tick, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CHAT, GuiGraphics.legacy(), delta(), (g, d) -> original.call(chat, tick));
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"))
	private void orbit$tabList(PlayerTabOverlay tabList, int width, Scoreboard scoreboard, Objective objective, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.PLAYER_LIST, GuiGraphics.legacy(), delta(), (g, d) -> original.call(tabList, width, scoreboard, objective));
	}

	private static DeltaTracker delta() {
		return DeltaTracker.of(Minecraft.getInstance());
	}
}
*///?}
