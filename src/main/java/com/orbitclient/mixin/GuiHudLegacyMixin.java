package com.orbitclient.mixin;

//? if >=1.20 <1.20.5 {
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

/^*
 * 1.20 to 1.20.4 (see GuiHudMixin for 1.20.5 to 1.21.5, GuiHudOldMixin before 1.20): the HUD is drawn in one method, so the parts with a
 * method of their own are wrapped, and the chat and tab list where render() draws them. The food bar is drawn
 * inside the hearts, so things attached to it go around those. Titles have no seam to hook here.
 ^/
// Named in full: build.gradle retargets the short form to Hud for 26.2's HUD mixins.
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiHudLegacyMixin {
	@WrapMethod(method = "renderCrosshair")
	private void orbit$crosshair(GuiGraphics graphics, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CROSSHAIR, graphics, delta(), (g, d) -> original.call(g));
	}

	@WrapMethod(method = "renderEffects")
	private void orbit$effects(GuiGraphics graphics, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.STATUS_EFFECTS, graphics, delta(), (g, d) -> original.call(g));
	}

	@WrapMethod(method = "displayScoreboardSidebar")
	private void orbit$scoreboard(GuiGraphics graphics, Objective objective, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.SCOREBOARD, graphics, delta(), (g, d) -> original.call(g, objective));
	}

	@WrapMethod(method = "renderPlayerHealth")
	private void orbit$food(GuiGraphics graphics, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.FOOD_BAR, graphics, delta(), (g, d) -> original.call(g));
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;III)V"))
	private void orbit$chat(ChatComponent chat, GuiGraphics graphics, int tick, int mx, int my, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CHAT, graphics, delta(), (g, d) -> original.call(chat, g, tick, mx, my));
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"))
	private void orbit$tabList(PlayerTabOverlay tabList, GuiGraphics graphics, int width, Scoreboard scoreboard, Objective objective, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.PLAYER_LIST, graphics, delta(), (g, d) -> original.call(tabList, g, width, scoreboard, objective));
	}

	private static DeltaTracker delta() {
		return DeltaTracker.of(Minecraft.getInstance());
	}
}
*///?}
