package com.orbitclient.gui;

import com.orbitclient.OrbitClient;
import com.orbitclient.config.ClientSettings;
import com.orbitclient.module.HudModule;
import com.orbitclient.module.ModuleManager;
import com.orbitclient.module.hud.PotionEffectsModule;
import com.orbitclient.module.server.ScaledHudModule;
import com.orbitclient.module.server.WaypointsModule;
import com.orbitclient.module.visual.BossBarModule;
import com.orbitclient.module.visual.CrosshairModule;
import com.orbitclient.module.visual.ScoreboardModule;
import com.orbitclient.util.Anim;
import com.orbitclient.util.CpsTracker;
import com.orbitclient.util.Render2D;
import com.orbitclient.util.VanillaAnimations;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public final class HudRenderer {
	private static final Minecraft mc = Minecraft.getInstance();
	private static final Anim.Clock HUD_CLOCK = new Anim.Clock();
	private static final Anim.Clock TAB_CLOCK = new Anim.Clock();
	private static float tabProgress;
	private static boolean tabHeld;

	private HudRenderer() {
	}

	public static void register() {
		// Saturation: exhaustion under the hunger bar, saturation and food preview on top of it.
		HudElementRegistry.attachElementBefore(VanillaHudElements.FOOD_BAR, Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "food_under"),
			(graphics, delta) -> com.orbitclient.module.hud.SaturationModule.renderUnder(graphics));
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "food_over"),
			(graphics, delta) -> com.orbitclient.module.hud.SaturationModule.renderOver(graphics));
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
			com.orbitclient.module.hud.SaturationModule.tooltip(stack, lines));
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "hud"), HudRenderer::renderMods);

		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
			CrosshairModule crosshair = ModuleManager.get(CrosshairModule.class);
			if (crosshair.isEnabled() && mc.options.getCameraType().isFirstPerson()) crosshair.render(graphics);
			else original.render(graphics, delta);
		});

		HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, original -> (graphics, delta) -> {
			ScoreboardModule sb = ModuleManager.get(ScoreboardModule.class);
			if (!sb.isEnabled()) original.render(graphics, delta);
			else if (!sb.hide.isOn()) scaled(graphics, delta, original, sb.scale.getFloat(), graphics.guiWidth(), graphics.guiHeight() / 2f);
		});

		HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, original -> (graphics, delta) -> {
			BossBarModule bb = ModuleManager.get(BossBarModule.class);
			if (!bb.isEnabled()) original.render(graphics, delta);
			else if (!bb.hide.isOn()) scaled(graphics, delta, original, bb.scale.getFloat(), graphics.guiWidth() / 2f, 0);
		});

		HudElementRegistry.replaceElement(VanillaHudElements.PLAYER_LIST, original -> HudRenderer::renderTabList);

		HudElementRegistry.replaceElement(VanillaHudElements.TITLE_AND_SUBTITLE, original -> (graphics, delta) -> {
			ScaledHudModule titles = ModuleManager.titles;
			if (!titles.isEnabled()) original.render(graphics, delta);
			else if (!titles.hide.isOn()) scaled(graphics, delta, original, titles.scale.getFloat(), graphics.guiWidth() / 2f, graphics.guiHeight() / 2f);
		});

		HudElementRegistry.replaceElement(VanillaHudElements.CHAT, original -> (graphics, delta) -> {
			float offset = VanillaAnimations.chatOffset();
			if (offset < 0.05f) {
				original.render(graphics, delta);
				return;
			}
			// Clip at chat's bottom edge so the new line rises from behind it instead of over the hearts.
			graphics.enableScissor(0, 0, graphics.guiWidth(), graphics.guiHeight() - 29);
			graphics.pose().pushMatrix();
			graphics.pose().translate(0, offset);
			original.render(graphics, delta);
			graphics.pose().popMatrix();
			graphics.disableScissor();
		});

		HudElementRegistry.replaceElement(VanillaHudElements.STATUS_EFFECTS, original -> (graphics, delta) -> {
			PotionEffectsModule potions = ModuleManager.get(PotionEffectsModule.class);
			if (!potions.isEnabled() || !potions.hideVanilla.isOn()) original.render(graphics, delta);
		});
	}

	/** Draws a vanilla element scaled around an anchor point so it stays in its usual spot. */
	private static void scaled(GuiGraphics graphics, DeltaTracker delta, HudElement element, float scale, float anchorX, float anchorY) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(anchorX, anchorY);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-anchorX, -anchorY);
		element.render(graphics, delta);
		graphics.pose().popMatrix();
	}

	// ---------------------------------------------------------------- tab list

	/**
	 * Replaces vanilla's tab list drawing so it can unfold when you press Tab and fold away when
	 * you let go (vanilla only draws it while the key is held, so it can't animate out by itself).
	 * Also applies the Tab Editor's hide and scale options.
	 */
	private static void renderTabList(GuiGraphics graphics, DeltaTracker delta) {
		if (mc.level == null || mc.player == null) return;
		ScaledHudModule editor = ModuleManager.tabEditor;
		boolean hidden = editor.isEnabled() && editor.hide.isOn();
		float scale = editor.isEnabled() ? editor.scale.getFloat() : 1f;

		Scoreboard scoreboard = mc.level.getScoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
		boolean held = !hidden && mc.options.keyPlayerList.isDown()
			&& (!mc.isLocalServer() || mc.player.connection.getListedOnlinePlayers().size() > 1 || objective != null);
		PlayerTabOverlay tabList = mc.gui.getTabList();

		boolean animate = Anim.enabled() && ClientSettings.INSTANCE.animatedTabList.isOn();
		float dt = TAB_CLOCK.tick();
		tabProgress = animate ? Anim.step(tabProgress, held, held ? 0.2f : 0.14f, dt) : (held ? 1f : 0f);
		if (tabProgress <= 0f) {
			if (tabHeld) tabList.setVisible(false);
			tabHeld = false;
			return;
		}
		if (held && !tabHeld) tabList.setVisible(true);
		tabHeld = tabHeld || held;

		float e = held ? Anim.easeOutCubic(tabProgress) : Anim.easeInOutCubic(tabProgress);
		int w = graphics.guiWidth();
		float s = scale * (0.92f + 0.08f * e);
		graphics.pose().pushMatrix();
		graphics.pose().translate(w / 2f, -14f * (1 - e));
		graphics.pose().scale(s, s);
		graphics.pose().translate(-w / 2f, 0);
		// Reveal from the top down, so the list unrolls rather than popping in.
		int reveal = Math.round((graphics.guiHeight() * e + 16) / s);
		graphics.enableScissor(-w, -32, w * 2, reveal);
		tabList.render(graphics, w, scoreboard, objective);
		graphics.disableScissor();
		graphics.pose().popMatrix();
	}

	// ---------------------------------------------------------------- mods

	private static void renderMods(GuiGraphics graphics, DeltaTracker delta) {
		CpsTracker.update();
		float dt = HUD_CLOCK.tick();
		if (mc.options.hideGui) return;
		WaypointsModule waypoints = ModuleManager.get(WaypointsModule.class);
		waypoints.render(graphics, dt);
		if (mc.screen instanceof HudEditorScreen) return;
		boolean debugHidden = !ClientSettings.INSTANCE.hudInDebug.isOn() && mc.debugEntries.isOverlayVisible();

		for (HudModule module : ModuleManager.hudModules()) {
			boolean show = module.isEnabled() && module.shouldRender() && !debugHidden;
			module.visibility = Anim.step(module.visibility, show, show ? 0.22f : 0.16f, dt);
			if (module.visibility <= 0f) continue;
			Render2D.alpha = Anim.easeOutCubic(module.visibility);
			try {
				renderModule(graphics, module, false, dt);
			} finally {
				Render2D.alpha = 1f;
			}
		}
	}

	/** Shown size in screen pixels, following the animated scale. */
	public static int shownWidth(HudModule module) {
		return Math.round(module.getWidth() * shownScale(module));
	}

	public static int shownHeight(HudModule module) {
		return Math.round(module.getHeight() * shownScale(module));
	}

	private static float shownScale(HudModule module) {
		return module.shownScale < 0 ? module.effectiveScale() : module.shownScale;
	}

	/**
	 * Draws one mod at its position. The scale eases toward the setting (so resizing glides),
	 * and fading or pulsing mods shrink/grow slightly around their center.
	 */
	public static void renderModule(GuiGraphics graphics, HudModule module, boolean editing, float dt) {
		float target = module.effectiveScale();
		module.shownScale = module.shownScale < 0 ? target : Anim.approach(module.shownScale, target, 16f, dt);
		float scale = module.shownScale;
		float extra = module.pulseScale() * (editing ? 1f : 0.9f + 0.1f * Anim.easeOutCubic(module.visibility));

		int x = module.getX(graphics.guiWidth());
		int y = module.getY(graphics.guiHeight());
		graphics.pose().pushMatrix();
		if (Math.abs(extra - 1f) > 0.001f) {
			// Fading or pulsing: grow/shrink around the center. Skipped once settled, so text sits on
			// exact pixel positions (fractional offsets are another cause of blurry/broken text).
			float halfW = module.getWidth() * scale / 2f, halfH = module.getHeight() * scale / 2f;
			graphics.pose().translate(x + halfW, y + halfH);
			graphics.pose().scale(extra, extra);
			graphics.pose().translate(-halfW, -halfH);
		} else {
			graphics.pose().translate(x, y);
		}
		graphics.pose().scale(scale, scale);
		module.render(graphics, editing);
		graphics.pose().popMatrix();
	}
}
