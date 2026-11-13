package com.orbitclient.gui;

import com.orbitclient.OrbitClient;
import com.orbitclient.config.ClientSettings;
import com.orbitclient.config.ConfigManager;
import com.orbitclient.module.HudModule;
import com.orbitclient.module.ModuleManager;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Render2D;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lunar-style HUD layout editor: drag mods around, scroll to resize, right-click for options,
 * and a MODS button in the middle that opens the mod menu. Fades in and out; outlines, snap
 * guides and the button animate.
 */
public class HudEditorScreen extends Screen {
	private static final int SNAP = 4;
	private static final int BUTTON_W = 90;
	private static final int BUTTON_H = 22;

	private HudModule dragging;
	private int dragOffsetX, dragOffsetY;
	private boolean snapX, snapY;
	private float snapXAlpha, snapYAlpha;
	private final Anim.Clock clock = new Anim.Clock();
	private float openProgress;
	private boolean closing;
	private Runnable afterClose;
	private float buttonHover;
	private final Map<HudModule, Float> hover = new HashMap<>();

	public HudEditorScreen() {
		super(Component.literal("Edit HUD Layout"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		// No blur: you need to see the game to place things. The dim fades in with the editor.
		int dim = minecraft.level == null ? 0xFF101014 : 0x50000000;
		g.fill(0, 0, width, height, Render2D.withAlpha(dim, Anim.easeOutCubic(openProgress)));
	}

	private List<HudModule> enabledHud() {
		return ModuleManager.hudModules().stream().filter(HudModule::isEnabled).toList();
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		float dt = clock.tick();
		openProgress = Anim.step(openProgress, !closing, closing ? 0.15f : 0.25f, dt);
		float open = Anim.easeOutCubic(openProgress);

		snapXAlpha = Anim.approach(snapXAlpha, dragging != null && snapX ? 1f : 0f, 18f, dt);
		snapYAlpha = Anim.approach(snapYAlpha, dragging != null && snapY ? 1f : 0f, 18f, dt);
		Render2D.alpha = open;
		try {
			if (snapXAlpha > 0.01f) Render2D.fill(g, width / 2, 0, width / 2 + 1, height, Render2D.withAlpha(Theme.ACCENT, snapXAlpha));
			if (snapYAlpha > 0.01f) Render2D.fill(g, 0, height / 2, width, height / 2 + 1, Render2D.withAlpha(Theme.ACCENT, snapYAlpha));

			HudModule hovered = moduleAt(mx, my);
			for (HudModule module : enabledHud()) {
				float h = Anim.approach(hover.getOrDefault(module, 0f), module == hovered || module == dragging ? 1f : 0f, 16f, dt);
				hover.put(module, h);
				int x = module.getX(width), y = module.getY(height);
				int w = HudRenderer.shownWidth(module), hgt = HudRenderer.shownHeight(module);
				Render2D.fill(g, x - 1, y - 1, x + w + 1, y + hgt + 1, Render2D.lerpColor(0x15FFFFFF, 0x30FFFFFF, h));
				Render2D.outline(g, x - 1, y - 1, w + 2, hgt + 2, Render2D.lerpColor(0x50FFFFFF, 0xC0FFFFFF, h));
				float base = Render2D.alpha;
				Render2D.alpha = 1f;
				HudRenderer.renderModule(g, module, true, dt);
				Render2D.alpha = base;
				if (h > 0.01f && dragging == null) {
					String label = module.getName() + "  " + Math.round(module.effectiveScale() * 100) + "%";
					int ly = y - 11 < 0 ? y + hgt + 3 : y - 11;
					Render2D.alpha = base * h;
					Render2D.text(g, font, label, x, ly + Math.round((1 - h) * 3), 0xFFFFFFFF, true);
					Render2D.alpha = base;
				}
			}

			// The MODS button pops in with a slight overshoot and lifts on hover.
			int bx = (width - BUTTON_W) / 2, by = (height - BUTTON_H) / 2;
			boolean overButton = dragging == null && hovered == null && Render2D.hovered(mx, my, bx, by, BUTTON_W, BUTTON_H);
			buttonHover = Anim.approach(buttonHover, overButton ? 1 : 0, 16f, dt);
			float pop = closing ? open : Anim.easeOutBack(openProgress);
			float scale = (0.8f + 0.2f * pop) * (1 + 0.04f * buttonHover);
			g.pose().pushMatrix();
			g.pose().translate(width / 2f, height / 2f);
			g.pose().scale(scale, scale);
			g.pose().translate(-width / 2f, -height / 2f);
			Render2D.roundedRectBordered(g, bx, by, BUTTON_W, BUTTON_H, 3,
				Render2D.lerpColor(0xE0161616, 0xF02A2A2A, buttonHover), Render2D.lerpColor(Theme.TAB_BORDER, 0xFF6A6A6A, buttonHover));
			String label = "MODS";
			int lw = 0;
			for (char c : label.toCharArray()) lw += UiText.width(Component.literal(String.valueOf(c)).withStyle(ChatFormatting.BOLD)) + 2;
			int lx = (width - (lw - 2)) / 2;
			for (char c : label.toCharArray()) {
				Component ch = Component.literal(String.valueOf(c)).withStyle(ChatFormatting.BOLD);
				Render2D.text(g, font, ch, lx, by + 7, 0xFFFFFFFF, false);
				lx += UiText.width(ch) + 2;
			}
			g.pose().popMatrix();

			String hint = "Drag to move  •  Scroll to resize  •  Right-click for options  •  ESC to save";
			Render2D.text(g, font, hint, (width - font.width(hint)) / 2, by + BUTTON_H + 8 + Math.round((1 - open) * 6), 0xB0FFFFFF, true);
		} finally {
			Render2D.alpha = 1f;
		}
	}

	private HudModule moduleAt(double mx, double my) {
		List<HudModule> modules = enabledHud();
		for (int i = modules.size() - 1; i >= 0; i--) {
			HudModule m = modules.get(i);
			if (Render2D.hovered(mx, my, m.getX(width), m.getY(height), m.getScaledWidth(), m.getScaledHeight())) return m;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) return true;
		if (OrbitScreen.isMenuMouseButton(event.button())) {
			onClose();
			return true;
		}
		double mx = event.x(), my = event.y();
		HudModule module = moduleAt(mx, my);
		if (module != null) {
			if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				closeThen(() -> minecraft.setScreen(new OrbitScreen(module)));
			} else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				dragging = module;
				dragOffsetX = (int) mx - module.getX(width);
				dragOffsetY = (int) my - module.getY(height);
			}
			return true;
		}
		int bx = (width - BUTTON_W) / 2, by = (height - BUTTON_H) / 2;
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && Render2D.hovered(mx, my, bx, by, BUTTON_W, BUTTON_H)) {
			closeThen(() -> minecraft.setScreen(new OrbitScreen()));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(event, dx, dy);
		int w = dragging.getScaledWidth(), h = dragging.getScaledHeight();
		int x = (int) event.x() - dragOffsetX;
		int y = (int) event.y() - dragOffsetY;

		// Snap the element's center to the screen center, and its edges to the screen edges.
		boolean snap = ClientSettings.INSTANCE.snapping.isOn();
		snapX = snap && Math.abs(x + w / 2 - width / 2) <= SNAP;
		snapY = snap && Math.abs(y + h / 2 - height / 2) <= SNAP;
		if (snapX) x = width / 2 - w / 2;
		if (snapY) y = height / 2 - h / 2;
		if (snap) {
			if (Math.abs(x) <= SNAP) x = 0;
			if (Math.abs(x + w - width) <= SNAP) x = width - w;
			if (Math.abs(y) <= SNAP) y = 0;
			if (Math.abs(y + h - height) <= SNAP) y = height - h;
		}

		dragging.setPosition(x, y, width, height);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		snapX = snapY = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		HudModule module = moduleAt(mx, my);
		if (module == null) return false;
		// Step from the size actually shown, so every scroll visibly changes it (snapped sizes skip in-betweens).
		module.scale.set((double) (module.effectiveScale() + Math.signum(vertical) * HudModule.scaleStep()));
		module.setPosition(module.getX(width), module.getY(height), width, height);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (closing) return true;
		var menuBinding = KeyBindingHelper.getBoundKeyOf(OrbitClient.menuKey);
		boolean menuKey = menuBinding.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM && event.key() == menuBinding.getValue();
		if (menuKey || event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		closeThen(null);
	}

	private void closeThen(Runnable action) {
		if (closing) return;
		ConfigManager.save();
		dragging = null;
		closing = true;
		afterClose = action;
	}

	@Override
	public void tick() {
		super.tick();
		if (closing && openProgress <= 0f) {
			if (afterClose != null) afterClose.run();
			else super.onClose();
		}
	}
}
