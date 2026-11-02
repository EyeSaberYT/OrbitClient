package com.orbitclient.gui;

import com.orbitclient.config.ConfigManager;
import com.orbitclient.module.visual.CrosshairModule;
import com.orbitclient.setting.PixelGridSetting;
import com.orbitclient.setting.Setting;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Icons;
import com.orbitclient.util.Render2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Crosshair editor, laid out like Lunar Client's crosshair mod settings:
 * a preview with background choices, SIMPLE / PRESETS / CUSTOM tabs, a 17x17 drawing grid with a
 * marked center and a help box (Mouse1 draw, Mouse2 erase, Delete clear, Tab preview), share/import
 * codes (Lunar-compatible), custom scale (Small/Normal/Large/Auto) and color options
 * (Vanilla Blending / Static / Dynamic).
 */
public class CrosshairEditorScreen extends Screen {
	private static final int N = PixelGridSetting.SIZE;
	private static final int MAX_UNDO = 50;
	private static final String[] BACKGROUNDS = {"SKY", "GRASS", "STONE", "SAND"};
	private static final Identifier[] BG_TEXTURES = {
		null,
		Identifier.withDefaultNamespace("textures/block/moss_block.png"),
		Identifier.withDefaultNamespace("textures/block/stone.png"),
		Identifier.withDefaultNamespace("textures/block/sand.png"),
	};
	private static int background;

	private final CrosshairModule module;
	private final PixelGridSetting grid;
	private final Deque<boolean[]> undo = new ArrayDeque<>();
	private final SettingsList simpleList = new SettingsList(a -> {
	});
	private final SettingsList colorList = new SettingsList(a -> {
	});
	private final Map<String, Float> anims = new HashMap<>();
	private final Anim.Clock clock = new Anim.Clock();
	private float dt;

	private int px, py, pw, ph;
	private float openProgress;
	private boolean closing;
	private float scroll, scrollTarget, maxScroll;
	private long tabChange;
	private boolean previewHeld;

	// Clickable areas registered while drawing (screen coordinates), and the drawing grid's position.
	private record Hit(int x, int y, int w, int h, Runnable action) {
	}

	private final List<Hit> hits = new ArrayList<>();
	private int gridX = -1, gridY, cell;
	private int painting = -1; // 1 draw, 0 erase
	private int lastCellX = -1, lastCellY = -1;
	private String toast;
	private long toastTime;

	public CrosshairEditorScreen(CrosshairModule module) {
		super(Component.literal("Crosshair"));
		this.module = module;
		this.grid = module.custom;
	}

	@Override
	protected void init() {
		pw = Math.min(width - 30, 420);
		ph = height - 20;
		px = (width - pw) / 2;
		py = 10;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---------------------------------------------------------------- helpers

	private float anim(String key, boolean target, float speed) {
		float v = Anim.approach(anims.getOrDefault(key, target ? 1f : 0f), target ? 1f : 0f, speed, dt);
		anims.put(key, v);
		return v;
	}

	private int contentTop() {
		return py + 44;
	}

	private int contentBottom() {
		return py + ph - 8;
	}

	private void sectionTitle(GuiGraphics g, String title, int x, int y) {
		UiText.spacedSmall(g, title, x + UiText.spacedWidth(title, true) * 0.35f, y, Theme.O_TEXT_DIM, true, 0.7f);
	}

	/** A Lunar-style outlined button with letter-spaced text. Registers its click area. */
	private void button(GuiGraphics g, String label, int x, int y, int w, int h, int mx, int my, boolean active, boolean enabled, Runnable action) {
		boolean over = enabled && Render2D.hovered(mx, my, x, y, w, h) && inViewport(my);
		float hov = anim("b" + label + x + "_" + y, over, 16f);
		float on = anim("on" + label + x, active, 14f);
		int fill = Render2D.lerpColor(Render2D.lerpColor(0xFF1A1B20, 0xFF262830, hov), 0xFF3A3D48, on);
		int border = Render2D.lerpColor(0xFF3A3D48, 0xFFE0E2E8, on);
		float base = Render2D.alpha;
		if (!enabled) Render2D.alpha = base * 0.4f;
		Render2D.roundedRectBordered(g, x, y, w, h, 3, fill, border);
		UiText.spacedSmall(g, label, x + w / 2f, y + (h - 6) / 2f, on > 0.5f ? 0xFFFFFFFF : Theme.O_TEXT, true, 0.7f);
		Render2D.alpha = base;
		if (enabled && action != null) hits.add(new Hit(x, y, w, h, action));
	}

	private void iconButton(GuiGraphics g, String icon, int x, int y, int mx, int my, boolean active, Runnable action) {
		boolean over = Render2D.hovered(mx, my, x, y, 16, 16) && inViewport(my);
		float hov = anim("i" + icon, over, 16f);
		Render2D.roundedRectBordered(g, x, y, 16, 16, 3, Render2D.lerpColor(active ? 0xFF2B4C8A : 0xFF1A1B20, 0xFF30333E, hov),
			active ? Theme.CAT_HUD : 0xFF3A3D48);
		Icons.draw(g, icon, x + 3, y + 3, 10, Render2D.lerpColor(Theme.O_TEXT_DIM, 0xFFFFFFFF, Math.max(hov, active ? 1 : 0)));
		hits.add(new Hit(x, y, 16, 16, action));
	}

	private void toggle(GuiGraphics g, int x, int y, boolean on, String key) {
		float t = Anim.easeInOutCubic(anim(key, on, 12f));
		Render2D.roundedRect(g, x, y, 22, 10, 5, Render2D.lerpColor(Theme.OFF, Theme.ON, t));
		Render2D.circle(g, x + 5 + Math.round(t * 12), y + 5, 4, 0xFFFFFFFF);
	}

	private boolean inViewport(double my) {
		return my >= contentTop() && my < contentBottom();
	}

	private String tab() {
		return module.mode.get();
	}

	private void setTab(String mode) {
		if (!module.mode.is(mode)) {
			module.mode.set(mode);
			tabChange = System.currentTimeMillis();
		}
	}

	private void showToast(String text) {
		toast = text;
		toastTime = System.currentTimeMillis();
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		// No dim while holding TAB, so the preview shows the real game behind the crosshair.
		if (previewHeld) return;
		g.fill(0, 0, width, height, Render2D.withAlpha(0x90000000, Anim.easeOutCubic(openProgress)));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		dt = clock.tick();
		openProgress = Anim.step(openProgress, !closing, closing ? 0.16f : 0.22f, dt);
		float eased = closing ? Anim.easeInOutCubic(openProgress) : Anim.easeOutCubic(openProgress);
		hits.clear();

		if (previewHeld) {
			// The HUD keeps drawing the crosshair under menus. Drawing it again here would invert the same
			// pixels twice with Vanilla Blending (cancelling out), so only draw it when the HUD doesn't.
			boolean hudShowsIt = module.isEnabled() && minecraft.options.getCameraType().isFirstPerson();
			if (!hudShowsIt) module.renderPreview(g, module.pixels(), (width - 15) / 2 + 7, (height - 15) / 2 + 7);
			UiText.spacedSmall(g, "RELEASE TAB TO RETURN", width / 2f, height - 20, 0xC0FFFFFF, true, 0.7f);
			return;
		}

		float panelScale = 0.94f + 0.06f * eased;
		g.pose().pushMatrix();
		g.pose().translate(width / 2f, height / 2f);
		g.pose().scale(panelScale, panelScale);
		g.pose().translate(-width / 2f, -height / 2f);
		Render2D.alpha = eased;
		try {
			Render2D.roundedRectBordered(g, px, py, pw, ph, 6, Theme.GLASS, Theme.GLASS_BORDER);
			renderHeader(g, mx, my);
			scroll = Anim.approach(scroll, scrollTarget, 18f, dt);
			g.enableScissor(px + 1, contentTop(), px + pw - 1, contentBottom());
			int height = renderContent(g, mx, my, contentTop() + 4 - Math.round(scroll));
			g.disableScissor();
			maxScroll = Math.max(0, height + 8 - (contentBottom() - contentTop()));
			scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		} finally {
			Render2D.alpha = 1f;
			g.pose().popMatrix();
		}
	}

	private void renderHeader(GuiGraphics g, int mx, int my) {
		float back = anim("back", Render2D.hovered(mx, my, px + 10, py + 10, 14, 14), 16f);
		Render2D.roundedRectBordered(g, px + 10, py + 10, 14, 14, 3, Render2D.lerpColor(0xFF1A1B20, 0xFF30333E, back), 0xFF3A3D48);
		UiText.text(g, "<", px + 15 - Math.round(back), py + 13, 0xFFFFFFFF);
		hits.add(new Hit(px + 10, py + 10, 14, 14, this::onClose));
		Icons.draw(g, module.getIconName(), px + 30, py + 9, 16, 0xFFFFFFFF);
		UiText.spaced(g, "CROSSHAIR", px + 50, py + 10, 0xFFFFFFFF, true);
		UiText.fit(g, "Replace the default Minecraft crosshair with a customizable crosshair.", px + 50, py + 22, pw - 120, Theme.O_TEXT_DIM);

		int dw = 50, dx = px + pw - 10 - dw;
		float dh = anim("done", Render2D.hovered(mx, my, dx, py + 10, dw, 14), 16f);
		Render2D.roundedRect(g, dx, py + 10, dw, 14, 7, Render2D.lerpColor(0xFF2E9E4E, 0xFF3FD07A, dh));
		UiText.spacedSmall(g, "DONE", dx + dw / 2f, py + 14, 0xFFFFFFFF, true, 0.75f);
		hits.add(new Hit(dx, py + 10, dw, 14, this::onClose));
		Render2D.fill(g, px + 10, py + 37, px + pw - 10, py + 38, 0x18FFFFFF);
	}

	/** Draws the scrolling content starting at y; returns its total height. */
	private int renderContent(GuiGraphics g, int mx, int my, int y0) {
		int x = px + 14, w = pw - 28, y = y0;

		// ---- Preview
		sectionTitle(g, "PREVIEW", x, y);
		y += 11;
		int pvH = 70;
		drawBackground(g, background, x, y, w, pvH);
		boolean overPreview = Render2D.hovered(mx, my, x, y, w, pvH) && inViewport(my);
		int ccx = overPreview ? mx : x + w / 2, ccy = overPreview ? my : y + pvH / 2;
		g.enableScissor(x, Math.max(y, contentTop()), x + w, Math.min(y + pvH, contentBottom()));
		module.renderPreview(g, module.pixels(), ccx, ccy);
		g.disableScissor();
		if (!overPreview) UiText.spacedSmall(g, "HOVER TO AIM  •  HOLD TAB FOR FULL PREVIEW", x + w / 2f, y + pvH - 9, 0xA0FFFFFF, true, 0.55f);
		y += pvH + 4;
		int bw = (w - 9) / 4;
		for (int i = 0; i < BACKGROUNDS.length; i++) {
			int bx = x + i * (bw + 3);
			final int index = i;
			drawBackground(g, i, bx, y, bw, 14);
			float on = anim("bg" + i, background == i, 14f);
			float hov = anim("bgh" + i, Render2D.hovered(mx, my, bx, y, bw, 14) && inViewport(my), 16f);
			Render2D.outline(g, bx, y, bw, 14, Render2D.lerpColor(Render2D.lerpColor(0x40FFFFFF, 0xA0FFFFFF, hov), 0xFFFFFFFF, on));
			UiText.spacedSmall(g, BACKGROUNDS[i], bx + bw / 2f, y + 4, 0xFFFFFFFF, true, 0.6f);
			hits.add(new Hit(bx, y, bw, 14, () -> background = index));
		}
		y += 24;

		// ---- General options: Simple / Presets / Custom
		sectionTitle(g, "GENERAL OPTIONS", x, y);
		y += 11;
		String[] tabs = {"SIMPLE", "PRESETS", "CUSTOM"};
		String[] modes = {"Simple", "Preset", "Custom"};
		int tw = (w - 8) / 3;
		for (int i = 0; i < 3; i++) {
			final String m = modes[i];
			button(g, tabs[i], x + i * (tw + 4), y, i == 2 ? w - 2 * (tw + 4) : tw, 15, mx, my, tab().equals(m), true, () -> setTab(m));
		}
		y += 21;

		float p = Anim.easeOutCubic(Anim.since(tabChange, 0.22f));
		float base = Render2D.alpha;
		Render2D.alpha = base * p;
		int sectionStart = y;
		y += Math.round((1 - p) * 6);
		switch (tab()) {
			case "Simple" -> {
				simpleList.show(List.of(module.thickness, module.length, module.gap, module.dot));
				int h = simpleList.contentHeight();
				simpleList.render(g, x, y, w, h, mx, inViewport(my) ? my : -999, dt);
				y += h;
			}
			case "Preset" -> y = presetTiles(g, x, y, w, 26, mx, my, false);
			default -> y = renderCustom(g, x, y, w, mx, my);
		}
		Render2D.alpha = base;
		y = Math.max(y, sectionStart) + 12;

		// ---- Extra options: scale and outline
		sectionTitle(g, "EXTRA OPTIONS", x, y);
		y += 11;
		Render2D.roundedRect(g, x, y, w, 22, 4, Theme.O_ROW);
		UiText.text(g, "Use Custom Scale", x + 8, y + 7, Theme.O_TEXT);
		toggle(g, x + w - 30, y + 6, module.customScale.isOn(), "customScale");
		hits.add(new Hit(x, y, w, 22, module.customScale::toggle));
		y += 26;
		String[] scales = {"SMALL", "NORMAL", "LARGE", "AUTO"};
		int sw = (w - 9) / 4;
		for (int i = 0; i < 4; i++) {
			final String s = scales[i].charAt(0) + scales[i].substring(1).toLowerCase(Locale.ROOT);
			boolean active = module.customScale.isOn() && module.scale.is(s);
			button(g, scales[i], x + i * (sw + 3), y, i == 3 ? w - 3 * (sw + 3) : sw, 14, mx, my, active, module.customScale.isOn(), () -> module.scale.set(s));
		}
		y += 20;
		if (!module.colorMode.is("Vanilla Blending")) {
			Render2D.roundedRect(g, x, y, w, 22, 4, Theme.O_ROW);
			UiText.text(g, "Outline", x + 8, y + 7, Theme.O_TEXT);
			toggle(g, x + w - 30, y + 6, module.outline.isOn(), "outline");
			hits.add(new Hit(x, y, w, 22, module.outline::toggle));
			y += 26;
		}
		y += 8;

		// ---- Color options
		sectionTitle(g, "COLOR OPTIONS", x, y);
		y += 11;
		String[] colorModes = {"Vanilla Blending", "Static", "Dynamic"};
		int cw = (w - 8) / 3;
		for (int i = 0; i < 3; i++) {
			final String m = colorModes[i];
			button(g, m.toUpperCase(Locale.ROOT), x + i * (cw + 4), y, i == 2 ? w - 2 * (cw + 4) : cw, 15, mx, my, module.colorMode.is(m), true, () -> module.colorMode.set(m));
		}
		y += 21;
		if (module.colorMode.is("Vanilla Blending")) {
			UiText.fit(g, "Inverts the colors behind it, like the vanilla crosshair.", x + 2, y + 2, w, Theme.O_TEXT_DIM);
			y += 14;
		} else {
			List<Setting<?>> colors = module.colorMode.is("Dynamic")
				? List.of(module.color, module.enemyColor, module.friendlyColor) : List.of(module.color);
			colorList.show(colors);
			int h = colorList.contentHeight();
			colorList.render(g, x, y, w, h, mx, inViewport(my) ? my : -999, dt);
			y += h;
		}
		return y - y0 + 6;
	}

	private void drawBackground(GuiGraphics g, int index, int x, int y, int w, int h) {
		g.enableScissor(x, Math.max(y, contentTop()), x + w, Math.min(y + h, contentBottom()));
		if (BG_TEXTURES[index] == null) {
			Render2D.gradient(g, x, y, x + w, y + h, 0xFF6F9CE0, 0xFFB8D4F5);
		} else {
			int tint = Render2D.a(0xFFFFFFFF);
			for (int ty = y; ty < y + h; ty += 16) {
				for (int tx = x; tx < x + w; tx += 16) {
					g.blit(RenderPipelines.GUI_TEXTURED, BG_TEXTURES[index], tx, ty, 0f, 0f, 16, 16, 16, 16, tint);
				}
			}
		}
		g.disableScissor();
	}

	/** Preset tiles; in the Presets tab they select a preset, in Custom they copy it into the grid. */
	private int presetTiles(GuiGraphics g, int x, int y, int w, int tile, int mx, int my, boolean intoGrid) {
		int gap = 4, perRow = Math.max(1, (w + gap) / (tile + gap));
		int guiScale = Math.max(1, minecraft.getWindow().getGuiScale());
		for (int i = 0; i < CrosshairModule.PRESETS.length; i++) {
			int tx = x + (i % perRow) * (tile + gap), ty = y + (i / perRow) * (tile + gap);
			boolean selected = !intoGrid && module.mode.is("Preset") && module.preset.getInt() == i;
			boolean over = Render2D.hovered(mx, my, tx, ty, tile, tile) && inViewport(my);
			float hov = anim("pt" + intoGrid + i, over, 16f), on = anim("ps" + i, selected, 14f);
			Render2D.roundedRectBordered(g, tx, ty, tile, tile, 4, Render2D.lerpColor(0xFF34363E, 0xFF464954, hov),
				Render2D.lerpColor(0xFF4A4D58, 0xFFFFFFFF, Math.max(on, hov * 0.5f)));
			// One crosshair pixel per GUI pixel: every preset fits in 13 pixels, so even small tiles show it clearly.
			module.renderAt(g, CrosshairModule.preset(i), tx + tile / 2, ty + tile / 2, 0xFFFFFFFF, false, false, guiScale);
			final int index = i;
			hits.add(new Hit(tx, ty, tile, tile, () -> {
				if (intoGrid) {
					pushUndo();
					grid.restore(CrosshairModule.preset(index));
				} else {
					module.preset.set((double) index);
				}
			}));
			if (over) {
				String name = CrosshairModule.PRESETS[i];
				int nw = font.width(name) + 8;
				Render2D.roundedRect(g, tx + tile / 2 - nw / 2, ty - 13, nw, 11, 5, 0xF0101219);
				UiText.text(g, name, tx + tile / 2 - nw / 2 + 4, ty - 11, 0xFFFFFFFF);
			}
		}
		int rows = (CrosshairModule.PRESETS.length + perRow - 1) / perRow;
		return y + rows * (tile + gap) - gap;
	}

	/** The Custom tab: help box, share/import buttons and the 17x17 grid, then presets to start from. */
	private int renderCustom(GuiGraphics g, int x, int y, int w, int mx, int my) {
		int gridSpace = Math.min(w - 150, 17 * 9);
		cell = Math.max(5, gridSpace / N);
		int size = cell * N;
		int helpW = w - size - 30;
		int top = y;

		// Help box
		Render2D.roundedRect(g, x, y, helpW, size, 5, 0xFF15171D);
		UiText.spacedSmall(g, "HELP", x + helpW / 2f, y + 7, 0xFFFFFFFF, true, 0.8f);
		List<String> lines = UiText.wrap("Draw your own custom crosshair. Select a PRESET below to edit it.", helpW - 12, 3);
		int ly = y + 19;
		for (String line : lines) {
			UiText.centered(g, line, x + helpW / 2, ly, Theme.O_TEXT_DIM);
			ly += 10;
		}
		ly += 4;
		String[][] keys = {{"MOUSE1", "Draw pixels"}, {"MOUSE2", "Erase pixels"}, {"DELETE", "Clear"}, {"TAB", "Show preview"}, {"CTRL+Z", "Undo"}};
		for (String[] k : keys) {
			if (ly + 12 > y + size) break;
			int kw = 46;
			Render2D.roundedRectBordered(g, x + 8, ly, kw, 11, 2, 0xFF2A2D36, 0xFF4A4D58);
			UiText.spacedSmall(g, k[0], x + 8 + kw / 2f, ly + 3, 0xFFFFFFFF, true, 0.55f);
			UiText.fit(g, k[1], x + 8 + kw + 6, ly + 2, helpW - kw - 20, Theme.O_TEXT);
			ly += 14;
		}

		// Share / import / clear / mirror, then the grid
		int bx = x + helpW + 6;
		iconButton(g, "ui_share", bx, y, mx, my, false, () -> {
			minecraft.keyboardHandler.setClipboard(grid.toCode());
			showToast("Code copied to clipboard");
		});
		iconButton(g, "ui_import", bx, y + 20, mx, my, false, () -> {
			pushUndo();
			if (grid.fromCode(minecraft.keyboardHandler.getClipboard())) showToast("Crosshair imported");
			else {
				undo.pop();
				showToast("No crosshair code in clipboard");
			}
		});
		iconButton(g, "ui_trash", bx, y + 40, mx, my, false, () -> {
			pushUndo();
			grid.clear();
		});
		iconButton(g, "ui_reset", bx, y + 60, mx, my, false, this::undoLast);

		gridX = bx + 22;
		gridY = y;
		renderGrid(g, mx, my, size);
		y += size + 6;

		if (toast != null) {
			float t = Anim.since(toastTime, 2f);
			if (t < 1f) {
				float base = Render2D.alpha;
				Render2D.alpha = base * Math.min(1f, (1 - t) * 4f);
				UiText.text(g, toast, gridX, y, Theme.ON);
				Render2D.alpha = base;
			}
		}
		y += 12;
		UiText.spacedSmall(g, "SELECT A PRESET TO EDIT IT", x + UiText.spacedWidth("SELECT A PRESET TO EDIT IT", true) * 0.3f, y, Theme.O_TEXT_DIM, true, 0.6f);
		y += 10;
		y = presetTiles(g, x, y, w, 20, mx, my, true);
		return Math.max(y, top + size);
	}

	private void renderGrid(GuiGraphics g, int mx, int my, int size) {
		int c = N / 2;
		Render2D.fill(g, gridX - 1, gridY - 1, gridX + size + 1, gridY + size + 1, 0xFF3A3D48);
		int color = module.colorMode.is("Vanilla Blending") ? 0xFFFFFFFF : module.color.get() | 0xFF000000;
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				int x1 = gridX + x * cell, y1 = gridY + y * cell;
				int bg = (x == c || y == c) ? 0xFF262A34 : 0xFF1C1E25;
				float on = anim("px" + x + "_" + y, grid.get(x, y), 22f);
				Render2D.fill(g, x1, y1, x1 + cell, y1 + cell, Render2D.lerpColor(bg, color, on));
				// Grid lines
				Render2D.fill(g, x1 + cell - 1, y1, x1 + cell, y1 + cell, 0x40000000);
				Render2D.fill(g, x1, y1 + cell - 1, x1 + cell, y1 + cell, 0x40000000);
			}
		}
		// The center pixel is marked with a dot, so you always know where the middle is.
		int cx1 = gridX + c * cell, cy1 = gridY + c * cell, d = Math.max(1, cell / 3);
		Render2D.fill(g, cx1 + (cell - d) / 2, cy1 + (cell - d) / 2, cx1 + (cell - d) / 2 + d, cy1 + (cell - d) / 2 + d,
			grid.get(c, c) ? 0xFF1C1E25 : 0xFFE0453C);
		int hx = (mx - gridX) / cell, hy = (my - gridY) / cell;
		if (mx >= gridX && my >= gridY && hx < N && hy < N && inViewport(my)) {
			Render2D.outline(g, gridX + hx * cell, gridY + hy * cell, cell, cell, 0xFFFFFFFF);
		}
	}

	// ---------------------------------------------------------------- editing

	private void pushUndo() {
		undo.push(grid.copy());
		while (undo.size() > MAX_UNDO) undo.removeLast();
	}

	private void undoLast() {
		if (!undo.isEmpty()) grid.restore(undo.pop());
	}

	private int[] cellAt(double mx, double my) {
		if (gridX < 0 || !module.mode.is("Custom") || !inViewport(my) || mx < gridX || my < gridY) return null;
		int x = (int) ((mx - gridX) / cell), y = (int) ((my - gridY) / cell);
		return x < N && y < N ? new int[]{x, y} : null;
	}

	/** Paints from the last cell to this one, so fast drags don't leave gaps. */
	private void paintTo(int cx, int cy) {
		int x0 = lastCellX < 0 ? cx : lastCellX, y0 = lastCellY < 0 ? cy : lastCellY;
		int steps = Math.max(Math.abs(cx - x0), Math.abs(cy - y0));
		for (int i = 0; i <= steps; i++) {
			int x = steps == 0 ? cx : Math.round(x0 + (cx - x0) * (i / (float) steps));
			int y = steps == 0 ? cy : Math.round(y0 + (cy - y0) * (i / (float) steps));
			grid.setPixel(x, y, painting == 1);
		}
		lastCellX = cx;
		lastCellY = cy;
	}

	/** Undoes the open/close scale so clicks land where things are drawn. */
	private double tmx(double mx) {
		float s = 0.94f + 0.06f * Anim.easeOutCubic(openProgress);
		return width / 2.0 + (mx - width / 2.0) / s;
	}

	private double tmy(double my) {
		float s = 0.94f + 0.06f * Anim.easeOutCubic(openProgress);
		return height / 2.0 + (my - height / 2.0) / s;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing || previewHeld) return true;
		double mx = tmx(event.x()), my = tmy(event.y());
		int button = event.button();
		if (simpleList.isCapturing() || colorList.isCapturing()) {
			simpleList.mouseClicked(mx, my, button);
			colorList.mouseClicked(mx, my, button);
			return true;
		}
		int[] c = cellAt(mx, my);
		if (c != null && (button == 0 || button == 1)) {
			pushUndo();
			painting = button == 0 ? 1 : 0;
			lastCellX = lastCellY = -1;
			paintTo(c[0], c[1]);
			return true;
		}
		if (inViewport(my) || my < contentTop()) {
			if (module.mode.is("Simple") && simpleList.mouseClicked(mx, my, button)) return true;
			if (colorList.mouseClicked(mx, my, button)) return true;
		}
		if (button == 0) {
			for (int i = hits.size() - 1; i >= 0; i--) {
				Hit h = hits.get(i);
				boolean headerHit = h.y < contentTop();
				if (Render2D.hovered(mx, my, h.x, h.y, h.w, h.h) && (headerHit || inViewport(my))) {
					h.action.run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		double mx = tmx(event.x()), my = tmy(event.y());
		if (painting >= 0) {
			int[] c = cellAt(mx, my);
			if (c != null) paintTo(c[0], c[1]);
			return true;
		}
		if (simpleList.mouseDragged(mx, my) || colorList.mouseDragged(mx, my)) return true;
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		painting = -1;
		lastCellX = lastCellY = -1;
		simpleList.mouseReleased();
		colorList.mouseReleased();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		scrollTarget = (float) Math.max(0, Math.min(maxScroll, scrollTarget - vertical * 30));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (closing) return true;
		int key = event.key();
		if (simpleList.keyPressed(key) || colorList.keyPressed(key)) return true;
		if (key == GLFW.GLFW_KEY_TAB) {
			previewHeld = true;
			return true;
		}
		if ((key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) && module.mode.is("Custom")) {
			pushUndo();
			grid.clear();
			return true;
		}
		if (key == GLFW.GLFW_KEY_Z && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
			undoLast();
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_TAB) {
			previewHeld = false;
			return true;
		}
		return super.keyReleased(event);
	}

	/** Back to the Crosshair settings in Orbit, with the close animation first. */
	@Override
	public void onClose() {
		if (closing) return;
		simpleList.commit();
		colorList.commit();
		ConfigManager.save();
		closing = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (closing && openProgress <= 0f) minecraft.setScreen(new OrbitScreen(module));
	}

	/** For tests: show the full-screen preview as if TAB were held. */
	public void setPreviewHeld(boolean held) {
		previewHeld = held;
	}
}
