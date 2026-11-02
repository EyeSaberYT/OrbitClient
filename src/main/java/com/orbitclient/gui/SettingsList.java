package com.orbitclient.gui;

import com.orbitclient.setting.*;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Render2D;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.*;
import java.util.function.Consumer;

/**
 * A scrollable list of setting rows with their controls: switches, sliders, mode selectors,
 * color pickers, keybinds, text fields and buttons. Animated: rows stagger in, switches ease,
 * sliders glide, color pickers expand, mode values slide, keybind boxes pulse while listening.
 */
public class SettingsList {
	private static final int ROW_H = 22;
	private static final int GAP = 3;
	private static final int PICKER_H = 64;

	private final Consumer<ActionSetting> actionRunner;

	private List<Setting<?>> settings = List.of();
	private int x, y, w, h;
	private float dt;
	private float scroll, scrollTarget, maxScroll, scrollbarAlpha;
	private long lastScrollTime;
	private long contentStart = System.currentTimeMillis();
	private final Map<Object, Float> anims = new HashMap<>();
	private final Map<ColorSetting, Float> pickerOpen = new HashMap<>();
	private final Map<ModeSetting, long[]> modeChange = new HashMap<>();
	private final Map<ModeSetting, String> modePrevious = new HashMap<>();
	private final Map<NumberSetting, Float> sliderShown = new HashMap<>();

	private NumberSetting draggingSlider;
	private int sliderX;
	private ColorSetting draggingColor;
	private int colorPart, colorX, colorY;
	private ColorSetting expandedColor;
	private KeybindSetting listening;
	private TextSetting editingText;
	private String editBuffer = "";

	public SettingsList(Consumer<ActionSetting> actionRunner) {
		this.actionRunner = actionRunner;
	}

	/** Shows a different list (e.g. another mod); resets scroll and replays the entrance animation. */
	public void show(List<Setting<?>> list) {
		if (list.equals(settings)) return;
		commit();
		settings = list;
		scroll = scrollTarget = 0;
		expandedColor = null;
		listening = null;
		contentStart = System.currentTimeMillis();
	}

	/** Full height of the current rows (for embedding the list in a page that scrolls by itself). */
	public int contentHeight() {
		int total = 0;
		for (Setting<?> s : settings) {
			if (!s.isVisible()) continue;
			float picker = s instanceof ColorSetting c ? Anim.easeInOutCubic(pickerOpen.getOrDefault(c, 0f)) : 0f;
			total += ROW_H + Math.round(PICKER_H * picker) + GAP;
		}
		return Math.max(0, total - GAP);
	}

	/** True while a keybind is waiting for input or a text field is being typed in. */
	public boolean isCapturing() {
		return listening != null || editingText != null;
	}

	public void commit() {
		if (editingText != null) editingText.set(editBuffer);
		editingText = null;
		editBuffer = "";
	}

	// ---------------------------------------------------------------- layout

	private record Row(Setting<?> setting, int y, int h, float picker) {
	}

	private List<Row> layout() {
		List<Row> rows = new ArrayList<>();
		int ry = y - Math.round(scroll), total = 0;
		for (Setting<?> s : settings) {
			if (!s.isVisible()) continue;
			float picker = s instanceof ColorSetting c ? Anim.easeInOutCubic(pickerOpen.getOrDefault(c, 0f)) : 0f;
			int rh = ROW_H + Math.round(PICKER_H * picker);
			rows.add(new Row(s, ry, rh, picker));
			ry += rh + GAP;
			total += rh + GAP;
		}
		maxScroll = Math.max(0, total - GAP - h);
		return rows;
	}

	private float anim(Object key, boolean target, float speed) {
		float v = Anim.approach(anims.getOrDefault(key, target ? 1f : 0f), target ? 1f : 0f, speed, dt);
		anims.put(key, v);
		return v;
	}

	private static String id(Object o) {
		return String.valueOf(System.identityHashCode(o));
	}

	// ---------------------------------------------------------------- rendering

	public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float dt) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.dt = dt;
		for (Setting<?> s : settings) {
			if (s instanceof ColorSetting c) pickerOpen.put(c, Anim.step(pickerOpen.getOrDefault(c, 0f), c == expandedColor, 0.22f, dt));
		}
		scroll = Anim.approach(scroll, scrollTarget, 18f, dt);
		List<Row> rows = layout();
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		boolean inList = Render2D.hovered(mx, my, x, y, w, h);
		float elapsed = (System.currentTimeMillis() - contentStart) / 1000f * Anim.speed();

		float base = Render2D.alpha;
		g.enableScissor(x, y, x + w, y + h);
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			float stagger = Anim.easeOutCubic(Anim.enabled() ? (elapsed - Math.min(i, 15) * 0.025f) / 0.25f : 1f);
			int ry = row.y + Math.round((1 - stagger) * 10);
			if (ry + row.h < y || ry > y + h || stagger <= 0.01f) continue;
			Render2D.alpha = base * stagger;
			float hover = anim("row" + id(row.setting), inList && Render2D.hovered(mx, my, x, ry, w, ROW_H), 16f);
			Render2D.roundedRect(g, x, ry, w, row.h, 4, Render2D.lerpColor(Theme.O_ROW, Theme.O_ROW_HOVER, hover));
			UiText.text(g, row.setting.getName(), x + 8, ry + 7, Theme.O_TEXT);
			control(g, new Row(row.setting, ry, row.h, row.picker), mx, my);
		}
		Render2D.alpha = base;
		g.disableScissor();
		scrollbar(g, inList);
	}

	private void scrollbar(GuiGraphics g, boolean hovered) {
		boolean wanted = maxScroll > 0 && (hovered || System.currentTimeMillis() - lastScrollTime < 900);
		scrollbarAlpha = Anim.approach(scrollbarAlpha, wanted ? 1f : 0f, 8f, dt);
		if (maxScroll <= 0 || scrollbarAlpha <= 0.01f) return;
		float base = Render2D.alpha;
		Render2D.alpha = base * scrollbarAlpha;
		int sx = x + w + 3;
		int thumbH = Math.max(20, Math.round(h * (h / (h + maxScroll))));
		int thumbY = y + Math.round((h - thumbH) * Math.min(1f, scroll / maxScroll));
		Render2D.roundedRect(g, sx, thumbY, 2, thumbH, 1, 0x80FFFFFF);
		Render2D.alpha = base;
	}

	private void drawSwitch(GuiGraphics g, int sx, int sy, boolean on, Object key) {
		float t = Anim.easeInOutCubic(anim(key, on, 12f));
		Render2D.roundedRect(g, sx, sy, 22, 10, 5, Render2D.lerpColor(Theme.OFF, Theme.ON, t));
		Render2D.circle(g, sx + 5 + Math.round(t * 12), sy + 5, 4, 0xFFFFFFFF);
	}

	private void control(GuiGraphics g, Row row, int mx, int my) {
		int rx = x + w - 8, ry = row.y;
		Setting<?> s = row.setting;
		if (s instanceof BooleanSetting b) {
			drawSwitch(g, rx - 22, ry + 6, b.isOn(), b);
		} else if (s instanceof NumberSetting n) {
			int width = Math.min(100, w / 3), sx = rx - width;
			float shown = Anim.approach(sliderShown.getOrDefault(n, (float) n.getPercent()), (float) n.getPercent(), 20f, dt);
			sliderShown.put(n, shown);
			float grow = anim("knob" + id(n), n == draggingSlider || Render2D.hovered(mx, my, sx - 4, ry + 3, width + 8, 16), 16f);
			Render2D.roundedRect(g, sx, ry + 10, width, 2, 1, Theme.OFF);
			int fill = Math.round(shown * width);
			Render2D.roundedRect(g, sx, ry + 10, fill, 2, 1, Theme.CAT_HUD);
			Render2D.circle(g, sx + fill, ry + 11, 3 + Math.round(grow), 0xFFFFFFFF);
			String v = n.format();
			UiText.text(g, v, sx - 8 - UiText.font().width(v), ry + 7, Render2D.lerpColor(Theme.O_TEXT_DIM, 0xFFFFFFFF, grow));
		} else if (s instanceof ModeSetting m) {
			int bw = 90, bx = rx - bw;
			float hover = anim("mode" + id(m), Render2D.hovered(mx, my, bx, ry + 4, bw, 14), 16f);
			Render2D.roundedRectBordered(g, bx, ry + 4, bw, 14, 4, Render2D.lerpColor(Theme.O_FIELD, Theme.O_ROW_HOVER, hover), Theme.O_FIELD_BORDER);
			UiText.text(g, "<", bx + 5 - Math.round(hover), ry + 7, Theme.O_TEXT_DIM);
			UiText.text(g, ">", bx + bw - 10 + Math.round(hover), ry + 7, Theme.O_TEXT_DIM);
			long[] change = modeChange.get(m);
			float p = change == null ? 1f : Anim.easeOutCubic(Anim.since(change[0], 0.2f));
			int dir = change == null ? 0 : (int) change[1];
			g.enableScissor(bx + 12, ry + 4, bx + bw - 12, ry + 18);
			float base = Render2D.alpha;
			if (p < 1f && modePrevious.containsKey(m)) {
				Render2D.alpha = base * (1 - p);
				UiText.centered(g, modePrevious.get(m), bx + bw / 2 - Math.round(p * 14 * dir), ry + 7, Theme.O_TEXT);
			}
			Render2D.alpha = base * p;
			UiText.centered(g, m.get(), bx + bw / 2 + Math.round((1 - p) * 14 * dir), ry + 7, Theme.O_TEXT);
			Render2D.alpha = base;
			g.disableScissor();
		} else if (s instanceof ColorSetting c) {
			float hover = anim("swatch" + id(c), Render2D.hovered(mx, my, rx - 24, ry + 4, 26, 14), 16f);
			int grow = Math.round(hover);
			Render2D.roundedRectBordered(g, rx - 22 - grow, ry + 6 - grow, 22 + grow * 2, 10 + grow * 2, 3,
				c.getColor() | 0xFF000000, Render2D.lerpColor(0xFF505566, 0xFFFFFFFF, hover));
			if (row.picker > 0.01f) {
				g.enableScissor(x, ry + ROW_H, x + w, ry + row.h);
				float base = Render2D.alpha;
				Render2D.alpha = base * row.picker;
				picker(g, c, x + 8, ry + ROW_H - Math.round((1 - row.picker) * 10));
				Render2D.alpha = base;
				g.disableScissor();
			}
		} else if (s instanceof KeybindSetting k) {
			int bw = 80, bx = rx - bw;
			boolean waiting = k == listening;
			float hover = anim("key" + id(k), Render2D.hovered(mx, my, bx, ry + 4, bw, 14), 16f);
			int fill = Render2D.lerpColor(Theme.O_FIELD, Theme.O_ROW_HOVER, hover), border = Theme.O_FIELD_BORDER;
			if (waiting) {
				float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 160.0));
				fill = Render2D.lerpColor(0xFF2B4C8A, 0xFF3D68B8, pulse);
				border = Theme.CAT_HUD;
			}
			Render2D.roundedRectBordered(g, bx, ry + 4, bw, 14, 4, fill, border);
			UiText.centered(g, waiting ? "Key or mouse..." : k.getKeyName(), bx + bw / 2, ry + 7, waiting ? 0xFFFFFFFF : Theme.O_TEXT);
		} else if (s instanceof TextSetting t) {
			int bw = Math.min(150, w / 2), bx = rx - bw;
			boolean editing = t == editingText;
			float focus = anim("text" + id(t), editing, 14f);
			Render2D.roundedRectBordered(g, bx, ry + 4, bw, 14, 4, Theme.O_FIELD, Render2D.lerpColor(Theme.O_FIELD_BORDER, Theme.CAT_HUD, focus));
			String value = editing ? editBuffer : t.get();
			String shown = UiText.font().plainSubstrByWidth(value, bw - 10, true);
			if (!editing && value.isEmpty()) {
				UiText.text(g, "Click to type...", bx + 5, ry + 7, Theme.O_TEXT_FAINT);
			} else {
				UiText.text(g, shown, bx + 5, ry + 7, Theme.O_TEXT);
				if (editing) UiText.text(g, "_", bx + 5 + UiText.font().width(shown), ry + 7, UiText.caretColor());
			}
		} else if (s instanceof ActionSetting a) {
			String label = a.get().toUpperCase(Locale.ROOT);
			int bw = UiText.spacedWidth(label, true) * 4 / 5 + 16, bx = rx - bw;
			float hover = anim("action" + id(a), Render2D.hovered(mx, my, bx, ry + 4, bw, 14), 16f);
			Render2D.roundedRect(g, bx, ry + 4, bw, 14, 7, Render2D.lerpColor(0xFF3B6FD8, 0xFF5A8CF0, hover));
			UiText.spacedSmall(g, label, bx + bw / 2f, ry + 8, 0xFFFFFFFF, true, 0.8f);
		}
	}

	private void picker(GuiGraphics g, ColorSetting c, int px, int py) {
		float[] hsb = c.getHsb();
		int sbW = 80, sbH = 56;
		for (int i = 0; i < sbW; i++) {
			int top = Color.HSBtoRGB(hsb[0], i / (float) (sbW - 1), 1f) | 0xFF000000;
			Render2D.gradient(g, px + i, py, px + i + 1, py + sbH, top, 0xFF000000);
		}
		int markX = px + Math.round(hsb[1] * (sbW - 1)), markY = py + Math.round((1 - hsb[2]) * (sbH - 1));
		Render2D.outline(g, markX - 2, markY - 2, 5, 5, 0xFFFFFFFF);

		int hx = px + sbW + 8;
		for (int i = 0; i < sbH; i++) Render2D.fill(g, hx, py + i, hx + 10, py + i + 1, Color.HSBtoRGB(i / (float) sbH, 1f, 1f) | 0xFF000000);
		int hueY = py + Math.round(hsb[0] * (sbH - 1));
		Render2D.fill(g, hx - 1, hueY - 1, hx + 11, hueY + 1, 0xFFFFFFFF);

		int ax = hx + 16;
		Render2D.fill(g, ax, py, ax + 10, py + sbH, 0xFF2A2A2A);
		Render2D.gradient(g, ax, py, ax + 10, py + sbH, c.get() | 0xFF000000, c.get() & 0x00FFFFFF);
		int alphaY = py + Math.round((1 - c.getAlpha() / 255f) * (sbH - 1));
		Render2D.fill(g, ax - 1, alphaY - 1, ax + 11, alphaY + 1, 0xFFFFFFFF);

		int ox = ax + 20;
		drawSwitch(g, ox, py + 2, c.isChroma(), "chroma" + id(c));
		UiText.text(g, "Chroma", ox + 28, py + 3, Theme.O_TEXT);
		UiText.text(g, String.format("#%06X", c.get() & 0xFFFFFF), ox, py + 20, Theme.O_TEXT_DIM);
		UiText.text(g, "Alpha " + (c.getAlpha() * 100 / 255) + "%", ox, py + 32, Theme.O_TEXT_DIM);
	}

	// ---------------------------------------------------------------- input

	public boolean mouseClicked(double mx, double my, int button) {
		if (listening != null) {
			// Any mouse button except left click (which cancels) can be bound: middle, side buttons, etc.
			if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) listening.set(KeybindSetting.mouse(button));
			listening = null;
			return true;
		}
		commit();
		if (!Render2D.hovered(mx, my, x, y, w, h)) return false;
		int rx = x + w - 8;
		for (Row row : layout()) {
			int ry = row.y;
			Setting<?> s = row.setting;
			if (s instanceof ColorSetting c && c == expandedColor && row.picker > 0.9f
				&& Render2D.hovered(mx, my, x, ry + ROW_H, w, PICKER_H)) {
				return clickPicker(c, x + 8, ry + ROW_H, mx, my);
			}
			if (!Render2D.hovered(mx, my, x, ry, w, ROW_H)) continue;

			if (s instanceof BooleanSetting b) {
				b.toggle();
			} else if (s instanceof NumberSetting n) {
				int width = Math.min(100, w / 3);
				if (Render2D.hovered(mx, my, rx - width - 4, ry + 3, width + 8, 16)) {
					draggingSlider = n;
					sliderX = rx - width;
					n.setPercent((mx - sliderX) / width);
				}
			} else if (s instanceof ModeSetting m) {
				int bw = 90;
				if (Render2D.hovered(mx, my, rx - bw, ry + 4, bw, 14)) {
					int dir = button == 1 || mx < rx - bw * 2 / 3 ? -1 : 1;
					modePrevious.put(m, m.get());
					m.cycle(dir);
					modeChange.put(m, new long[]{System.currentTimeMillis(), dir});
				}
			} else if (s instanceof ColorSetting c) {
				expandedColor = expandedColor == c ? null : c;
			} else if (s instanceof KeybindSetting k) {
				if (Render2D.hovered(mx, my, rx - 80, ry + 4, 80, 14)) listening = k;
			} else if (s instanceof TextSetting t) {
				int bw = Math.min(150, w / 2);
				if (Render2D.hovered(mx, my, rx - bw, ry + 4, bw, 14)) {
					editingText = t;
					editBuffer = t.get();
				}
			} else if (s instanceof ActionSetting a) {
				actionRunner.accept(a);
			}
			return true;
		}
		return false;
	}

	private boolean clickPicker(ColorSetting c, int px, int py, double mx, double my) {
		if (Render2D.hovered(mx, my, px + 124, py + 2, 22, 10)) {
			c.setChroma(!c.isChroma());
			return true;
		}
		if (Render2D.hovered(mx, my, px, py, 80, 56)) colorPart = 1;
		else if (Render2D.hovered(mx, my, px + 86, py, 14, 56)) colorPart = 2;
		else if (Render2D.hovered(mx, my, px + 102, py, 14, 56)) colorPart = 3;
		else return true;
		draggingColor = c;
		colorX = px;
		colorY = py;
		dragColor(mx, my);
		return true;
	}

	private void dragColor(double mx, double my) {
		float[] hsb = draggingColor.getHsb();
		float fx = (float) Math.max(0, Math.min(1, (mx - colorX) / 79.0));
		float fy = (float) Math.max(0, Math.min(1, (my - colorY) / 55.0));
		switch (colorPart) {
			case 1 -> draggingColor.setHsb(hsb[0], fx, 1 - fy);
			case 2 -> draggingColor.setHsb(Math.min(fy, 0.999f), hsb[1], hsb[2]);
			case 3 -> draggingColor.setAlpha(Math.round((1 - fy) * 255));
			default -> {
			}
		}
	}

	public boolean mouseDragged(double mx, double my) {
		if (draggingSlider != null) {
			draggingSlider.setPercent((mx - sliderX) / Math.min(100, w / 3));
			return true;
		}
		if (draggingColor != null) {
			dragColor(mx, my);
			return true;
		}
		return false;
	}

	public void mouseReleased() {
		draggingSlider = null;
		draggingColor = null;
		colorPart = 0;
	}

	public boolean mouseScrolled(double mx, double my, double vertical) {
		if (!Render2D.hovered(mx, my, x, y, w + 6, h)) return false;
		scrollTarget = (float) Math.max(0, Math.min(maxScroll, scrollTarget - vertical * 30));
		lastScrollTime = System.currentTimeMillis();
		return true;
	}

	/** Keys while capturing (keybind listening / text editing). Returns true if handled. */
	public boolean keyPressed(int key) {
		if (listening != null) {
			boolean clear = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE;
			listening.set(clear ? GLFW.GLFW_KEY_UNKNOWN : key);
			listening = null;
			return true;
		}
		if (editingText != null) {
			switch (key) {
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commit();
				case GLFW.GLFW_KEY_ESCAPE -> {
					editingText = null;
					editBuffer = "";
				}
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
				}
				default -> {
				}
			}
			return true;
		}
		return false;
	}

	public boolean charTyped(String s) {
		if (editingText == null) return false;
		if (editBuffer.length() < editingText.getMaxLength()) editBuffer += s;
		return true;
	}
}
