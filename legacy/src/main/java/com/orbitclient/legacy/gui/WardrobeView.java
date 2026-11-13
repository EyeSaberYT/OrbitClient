package com.orbitclient.legacy.gui;

import com.orbitclient.cosmetics.Cosmetic;
import com.orbitclient.cosmetics.CosmeticManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.cosmetics.CosmeticSync;
import com.orbitclient.cosmetics.Cosmetics;
import com.orbitclient.legacy.Game;
import com.orbitclient.legacy.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The wardrobe on the old engine: pick a slot, see every cosmetic in it, wear the ones this account
 * owns. What is owned comes from the cosmetics server only; opening the screen and the Refresh button
 * ask it again, so something redeemed on the website shows up without a restart. Nothing equips itself.
 *
 * This is everything the wardrobe draws and does. The game's Screen class changed between the old
 * versions, so the screen itself (WardrobeScreen, one per engine generation) only passes input on to this.
 */
public final class WardrobeView {
	private static final int[] COLORS = {
		0xFFFFFFFF, 0xFFE5484D, 0xFFF2994A, 0xFFF2C14E, 0xFF4CC38A, 0xFF3FD0D4, 0xFF5B8DEF, 0xFF8C7BFF, 0xFFE879C6, 0xFF2B2D36
	};
	private static final int CELL = 34, GAP = 4, TAB_W = 62, DETAIL_W = 112;
	private static final Map<String, Identifier> ICONS = new HashMap<>();

	private CosmeticSlot slot = CosmeticSlot.CAPE;
	private Cosmetic selected;
	private int scrollRows;
	private int x0, y0, panelW, panelH;
	private MinecraftClient client;
	private TextRenderer textRenderer;
	private int width, height;

	/** Opens on a given slot. */
	public WardrobeView(CosmeticSlot slot) {
		this.slot = slot;
		// Cosmetics redeemed on the website since the last check show up as soon as the wardrobe opens.
		CosmeticSync.requestRefresh();
	}

	/** The screen was closed: nothing is being tried on any more. */
	public void closed() {
		CosmeticManager.setPreview(null);
	}

	/** The Refresh button and F5. */
	public void refresh() {
		CosmeticSync.requestRefresh();
	}

	private int gridX() {
		return x0 + 8 + TAB_W + 6;
	}

	private int gridY() {
		return y0 + 26;
	}

	private int gridW() {
		return panelW - 16 - TAB_W - 6 - DETAIL_W - 6;
	}

	private int gridH() {
		return panelH - 26 - 22;
	}

	private int columns() {
		return Math.max(1, (gridW() + GAP) / (CELL + GAP));
	}

	private int visibleRows() {
		return Math.max(1, (gridH() + GAP) / (CELL + GAP));
	}

	public void render(MinecraftClient client, TextRenderer textRenderer, int width, int height, int mouseX, int mouseY) {
		this.client = client;
		this.textRenderer = textRenderer;
		this.width = width;
		this.height = height;
		panelW = Math.min(width - 16, 400);
		panelH = Math.min(height - 16, 230);
		x0 = (width - panelW) / 2;
		y0 = (height - panelH) / 2;

		Game.fill(0, 0, width, height, 0x90000000);
		Game.fill(x0, y0, x0 + panelW, y0 + panelH, 0xF0141519);
		Game.fill(x0, y0, x0 + panelW, y0 + 1, 0xFF8C7BFF);
		Game.textShadow(textRenderer, "Orbit Wardrobe", x0 + 8, y0 + 8, 0xFFFFFFFF);

		// Refresh (F5 does the same).
		boolean checking = CosmeticSync.checkingClaims();
		int rw = 56, rx = x0 + panelW - 8 - rw, ry = y0 + 5;
		boolean overRefresh = !checking && over(mouseX, mouseY, rx, ry, rw, 14);
		Game.fill(rx, ry, rx + rw, ry + 14, overRefresh ? 0xFF3A3E4A : 0xFF2A2D36);
		String refresh = checking ? "Checking" : "Refresh";
		Platform.text(textRenderer, refresh, rx + (rw - Game.textWidth(textRenderer, refresh)) / 2, ry + 3, checking ? 0xFF8A8F9C : 0xFFFFFFFF);

		// Slots.
		int ty = gridY();
		for (CosmeticSlot s : CosmeticSlot.values()) {
			boolean current = s == slot, hover = over(mouseX, mouseY, x0 + 8, ty, TAB_W, 15);
			Game.fill(x0 + 8, ty, x0 + 8 + TAB_W, ty + 15, current ? 0xFF8C7BFF : hover ? 0xFF2F323C : 0xFF22242B);
			Platform.text(textRenderer, s.title, x0 + 13, ty + 4, current ? 0xFFFFFFFF : 0xFFC8CCD6);
			if (CosmeticManager.own().get(s) != null) Game.fill(x0 + 8 + TAB_W - 5, ty + 6, x0 + 8 + TAB_W - 2, ty + 9, 0xFF4CC38A);
			ty += 17;
		}

		// The cosmetics of this slot.
		List<Cosmetic> items = Cosmetics.in(slot);
		int cols = columns(), rows = (items.size() + cols - 1) / cols;
		scrollRows = Math.max(0, Math.min(scrollRows, rows - visibleRows()));
		CosmeticManager.Worn worn = CosmeticManager.own().get(slot);
		Cosmetic hovered = null;
		for (int i = scrollRows * cols; i < items.size(); i++) {
			int row = i / cols - scrollRows;
			if (row >= visibleRows()) break;
			Cosmetic c = items.get(i);
			int cx = gridX() + (i % cols) * (CELL + GAP), cy = gridY() + row * (CELL + GAP);
			boolean owned = CosmeticManager.owns(c.id), isWorn = worn != null && worn.id().equals(c.id);
			boolean hover = over(mouseX, mouseY, cx, cy, CELL, CELL);
			if (hover) hovered = c;
			Game.fill(cx, cy, cx + CELL, cy + CELL, isWorn ? 0xFF2D4A3C : hover ? 0xFF2F323C : 0xFF22242B);
			drawIcon(c, cx + 3, cy + 3, CELL - 6, isWorn ? worn.color() : c.defaultColor);
			// What isn't owned is shown, dimmed: it can be looked at but not worn.
			if (!owned) Game.fill(cx, cy, cx + CELL, cy + CELL, 0xB0141519);
			int edge = c == selected ? 0xFFFFFFFF : isWorn ? 0xFF4CC38A : 0;
			if (edge != 0) outline(cx, cy, CELL, CELL, edge);
		}
		if (rows > visibleRows()) {
			String more = (scrollRows + 1) + "-" + Math.min(rows, scrollRows + visibleRows()) + " / " + rows;
			Platform.text(textRenderer, more, gridX() + gridW() - Game.textWidth(textRenderer, more), y0 + 12, 0xFF8A8F9C);
		}
		// Try on what the mouse is over (only what's owned can be tried on).
		CosmeticManager.setPreview(hovered);

		drawDetail(mouseX, mouseY, hovered != null ? hovered : selected);

		List<String> status = Game.wrap(textRenderer, CosmeticSync.status(), panelW - 16);
		if (!status.isEmpty()) Platform.text(textRenderer, status.get(0), x0 + 8, y0 + panelH - 13, 0xFF8A8F9C);
	}

	private void drawDetail(int mouseX, int mouseY, Cosmetic c) {
		int dx = x0 + panelW - 8 - DETAIL_W, dy = gridY();
		Game.fill(dx, dy, dx + DETAIL_W, dy + gridH(), 0xFF1B1D23);
		// You, wearing what you have on (and what the mouse is over).
		int px = dx + DETAIL_W / 2, py = dy + gridH() - 8;
		Game.drawPlayer(client, px, py, Math.min(38, (gridH() - 70) / 2), px - mouseX, py - 50 - mouseY);
		if (c == null) {
			Platform.text(textRenderer, "Pick a cosmetic", dx + 6, dy + 6, 0xFF8A8F9C);
			return;
		}
		Game.textShadow(textRenderer, Game.trim(textRenderer, c.name, DETAIL_W - 12), dx + 6, dy + 6, 0xFFFFFFFF);
		CosmeticManager.Worn worn = CosmeticManager.own().get(c.slot);
		boolean isWorn = worn != null && worn.id().equals(c.id);
		int y = dy + 19;
		if (!CosmeticManager.ownershipLoaded()) {
			y = lines("Checking what this account owns...", dx + 6, y, 0xFF8A8F9C);
		} else if (!CosmeticManager.owns(c.id)) {
			y = lines("Not owned. Get it at orbit.minearcade.org/cosmetics, then press Refresh.", dx + 6, y, 0xFFE0A458);
		} else {
			y = lines(isWorn ? "Wearing. Click it to take it off." : "Owned. Click it to wear it.", dx + 6, y, isWorn ? 0xFF4CC38A : 0xFFC8CCD6);
		}
		if (isWorn && c.colorable) {
			for (int i = 0; i < COLORS.length; i++) {
				int sx = dx + 6 + (i % 5) * 20, sy = y + (i / 5) * 14;
				Game.fill(sx, sy, sx + 17, sy + 11, COLORS[i]);
				if (worn.color() == COLORS[i]) outline(sx, sy, 17, 11, 0xFFFFFFFF);
			}
		}
	}

	private int lines(String text, int x, int y, int color) {
		for (String line : Game.wrap(textRenderer, text, DETAIL_W - 12)) {
			Platform.text(textRenderer, line, x, y, color);
			y += 10;
		}
		return y + 2;
	}

	private void outline(int x, int y, int w, int h, int color) {
		Game.fill(x, y, x + w, y + 1, color);
		Game.fill(x, y + h - 1, x + w, y + h, color);
		Game.fill(x, y, x + 1, y + h, color);
		Game.fill(x + w - 1, y, x + w, y + h, color);
	}

	private static boolean over(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** A cosmetic's picture: the cape's front, or its voxel model drawn once into a texture. */
	private void drawIcon(Cosmetic c, int x, int y, int size, int tint) {
		Game.white();
		Game.blend();
		if (c.slot == CosmeticSlot.CAPE) {
			Identifier cape = c.capeTexture(System.nanoTime() / 1e9);
			Platform.ensureTexture(client, cape);
			Game.bind(client, cape);
			// The front of a cape is the 10x16 piece at (1, 1) of the 64x32 cape layout.
			int w = size * 10 / 16;
			Game.texture(x + (size - w) / 2, y, 1f, 1f, 10, 16, w, size, 64f, 32f);
			return;
		}
		if (c.iconModel() == null) return;
		String key = c.id + ":" + Integer.toHexString(tint);
		Identifier icon = ICONS.get(key);
		if (icon == null) {
			int px = 48;
			if (ICONS.size() > 400) ICONS.clear();
			icon = Platform.iconTexture(client, c.iconModel().icon(px, tint, c.iconPair()), px);
			ICONS.put(key, icon);
		}
		Game.bind(client, icon);
		Game.texture(x, y, 0f, 0f, 48, 48, size, size, 48f, 48f);
	}

	public void clicked(int mouseX, int mouseY, int button) {
		if (button != 0) return;
		if (!CosmeticSync.checkingClaims() && over(mouseX, mouseY, x0 + panelW - 8 - 56, y0 + 5, 56, 14)) {
			CosmeticSync.requestRefresh();
			return;
		}
		int ty = gridY();
		for (CosmeticSlot s : CosmeticSlot.values()) {
			if (over(mouseX, mouseY, x0 + 8, ty, TAB_W, 15)) {
				slot = s;
				selected = null;
				scrollRows = 0;
				return;
			}
			ty += 17;
		}
		List<Cosmetic> items = Cosmetics.in(slot);
		int cols = columns();
		for (int i = scrollRows * cols; i < items.size(); i++) {
			int row = i / cols - scrollRows;
			if (row >= visibleRows()) break;
			int cx = gridX() + (i % cols) * (CELL + GAP), cy = gridY() + row * (CELL + GAP);
			if (!over(mouseX, mouseY, cx, cy, CELL, CELL)) continue;
			Cosmetic c = items.get(i);
			selected = c;
			// Only what the server says this account owns can be worn; the rest is just selected.
			if (CosmeticManager.owns(c.id)) {
				CosmeticManager.Worn worn = CosmeticManager.own().get(slot);
				if (worn != null && worn.id().equals(c.id)) CosmeticManager.remove(slot);
				else CosmeticManager.wear(c, c.defaultColor);
			}
			return;
		}
		// Colors of the selected, worn cosmetic.
		if (selected != null && selected.colorable) {
			CosmeticManager.Worn worn = CosmeticManager.own().get(selected.slot);
			if (worn != null && worn.id().equals(selected.id)) {
				int dx = x0 + panelW - 8 - DETAIL_W, y = gridY() + 19 + 10 * Game.wrap(textRenderer, "Wearing. Click it to take it off.", DETAIL_W - 12).size() + 2;
				for (int i = 0; i < COLORS.length; i++) {
					if (over(mouseX, mouseY, dx + 6 + (i % 5) * 20, y + (i / 5) * 14, 17, 11)) {
						CosmeticManager.setColor(selected.slot, COLORS[i]);
						return;
					}
				}
			}
		}
	}

	/** The mouse wheel: positive scrolls down a row, negative up. */
	public void scrolled(int rows) {
		scrollRows = Math.max(0, scrollRows + rows);
	}
}
