package com.orbitclient.mixin;

//? if <1.21.6 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.orbitclient.compat.HudElementRegistry;
import com.orbitclient.compat.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/^* Before 1.21.6: vanilla's HUD parts go through HudElementRegistry (Fabric's HUD element API is newer). ^/
// Named in full: build.gradle retargets the short form to Hud for 26.2's HUD mixins.
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiHudMixin {
	@WrapMethod(method = "renderCrosshair")
	private void orbit$crosshair(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CROSSHAIR, graphics, delta, (g, d) -> original.call(g, d));
	}

	@WrapMethod(method = "renderScoreboardSidebar")
	private void orbit$scoreboard(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.SCOREBOARD, graphics, delta, (g, d) -> original.call(g, d));
	}

	@WrapMethod(method = "renderTabList")
	private void orbit$tabList(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.PLAYER_LIST, graphics, delta, (g, d) -> original.call(g, d));
	}

	@WrapMethod(method = "renderTitle")
	private void orbit$title(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.TITLE_AND_SUBTITLE, graphics, delta, (g, d) -> original.call(g, d));
	}

	@WrapMethod(method = "renderChat")
	private void orbit$chat(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.CHAT, graphics, delta, (g, d) -> original.call(g, d));
	}

	@WrapMethod(method = "renderEffects")
	private void orbit$effects(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.STATUS_EFFECTS, graphics, delta, (g, d) -> original.call(g, d));
	}

	@WrapMethod(method = "renderFood")
	private void orbit$food(GuiGraphics graphics, Player player, int y, int x, Operation<Void> original) {
		HudElementRegistry.render(VanillaHudElements.FOOD_BAR, graphics, Minecraft.getInstance().getDeltaTracker(), (g, d) -> original.call(g, player, y, x));
	}
}
*///?}
