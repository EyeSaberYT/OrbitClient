package com.orbitclient.gui;

import com.orbitclient.config.ConfigManager;
import com.orbitclient.cosmetics.Cosmetic;
import com.orbitclient.cosmetics.CosmeticIcons;
import com.orbitclient.cosmetics.CosmeticManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.cosmetics.CosmeticSync;
import com.orbitclient.cosmetics.Cosmetics;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Icons;
import com.orbitclient.util.Render2D;
import com.orbitclient.util.Shapes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The wardrobe, built like the Orbit menu: you turn slowly inside the eclipse core, the slots (capes,
 * hats, ...) are arcs around it, and the cosmetics of the chosen slot orbit as nodes with their
 * pictures. Hover a node to try it on, click to wear it (again to take it off); the glass panel on the
 * right shows the cosmetic, a wear button and its colors.
 */
public class WardrobeScreen extends Screen {
	private static final float TAU = (float) (Math.PI * 2);
	private static final int[] SLOT_COLORS = {0xFF7C6CFF, 0xFFF2C14E, 0xFFE5484D, 0xFF57D68D, 0xFF55AAFF, 0xFF3FE0FF, 0xFFF05A7E};
	private static final int[] QUICK_COLORS = {0xFFFFFFFF, 0xFF1C1D22, 0xFFE5484D, 0xFFF0A93A, 0xFFF5D547, 0xFF57D68D, 0xFF3FE0FF, 0xFF55AAFF, 0xFF7C6CFF, 0xFFF05A7E};
	private static CosmeticSlot lastSlot = CosmeticSlot.CAPE;

	private final Anim.Clock clock = new Anim.Clock();
	private final Map<String, Float> anims = new HashMap<>();
	private final SettingsList colorList = new SettingsList(a -> {
	});
	private ColorSetting colorSetting;
	private String colorFor;
	private float dt;

	private CosmeticSlot slot = lastSlot;
	private long burstStart = System.currentTimeMillis();
	private float openProgress;
	private boolean closing;

	private int cx, cy, coreR, slotR, ringR, nodeR, panelX, panelY, panelW, panelH;
	private float ringAngle, ringTarget;
	// When a slot has more cosmetics than fit around the ring, they slide around it as you scroll
	// (scroll counts nodes); the ones entering and leaving fade at the bottom.
	private boolean carousel;
	private float scroll, scrollTarget;
	private String search = "";
	private float yaw = 200f;
	private boolean dragging;
	private long lastDrag;

	private Cosmetic hovered;
	private boolean hoverNone;
	private final List<Object[]> nodeHits = new ArrayList<>();

	private record Hit(int x, int y, int w, int h, Runnable action) {
	}

	private final List<Hit> hits = new ArrayList<>();

	public WardrobeScreen() {
		super(Component.literal("Wardrobe"));
		// Cosmetics redeemed on the website since the last check show up as soon as the wardrobe opens.
		CosmeticSync.requestRefresh();
	}

	@Override
	protected void init() {
		panelW = Math.min(230, Math.max(170, width / 4));
		panelH = Math.min(height - 40, 300);
		panelX = width - panelW - 16;
		panelY = (height - panelH) / 2;
		cx = (panelX - 40) / 2 + 30;
		cy = height / 2 + 4;
		coreR = Math.max(36, Math.min(60, Math.min(height, panelX) / 7));
		slotR = coreR + 16;
		ringR = Math.min(slotR + 56, Math.min(height / 2 - 26, (panelX - 40) / 2 - 10));
		nodeR = Math.max(11, Math.min(17, ringR / 8));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private float anim(String key, boolean target, float speed) {
		float v = Anim.approach(anims.getOrDefault(key, target ? 1f : 0f), target ? 1f : 0f, speed, dt);
		anims.put(key, v);
		return v;
	}

	private int slotColor(CosmeticSlot s) {
		return SLOT_COLORS[s.ordinal()];
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		// Dark enough that the HUD and hotbar behind don't clutter the ring.
		float e = Anim.easeOutCubic(openProgress);
		g.fillGradient(0, 0, width, height, Render2D.withAlpha(0xEA07080D, e), Render2D.withAlpha(0xF404050A, e));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		dt = clock.tick();
		openProgress = Anim.step(openProgress, !closing, closing ? 0.16f : 0.25f, dt);
		float eased = closing ? Anim.easeInOutCubic(openProgress) : Anim.easeOutCubic(openProgress);
		hits.clear();
		hovered = null;
		hoverNone = false;
		if (!dragging && System.currentTimeMillis() - lastDrag > 1500) yaw += dt * 28f;
		ringAngle = Anim.approach(ringAngle, ringTarget, 10f, dt);

		Render2D.alpha = eased;
		try {
			renderSlots(g, mx, my);
			renderNodes(g, mx, my, eased);
			renderCore(g, eased);
			renderTooltip(g, mx, my);
			renderPanel(g, mx, my, eased);
			renderSearch(g);
			UiText.spacedSmall(g, (carousel ? "SCROLL TO BROWSE" : "SCROLL TO SPIN") + "  •  TYPE TO SEARCH  •  DRAG YOURSELF TO TURN  •  ← → SWITCH SLOT",
				cx, height - 14, Theme.O_TEXT_FAINT, true, 0.55f);
		} finally {
			Render2D.alpha = 1f;
		}
		CosmeticManager.setPreview(hovered);
	}

	// ---------------------------------------------------------------- orbit

	/** The slots as arcs around the core, like the Orbit menu's categories. */
	private void renderSlots(GuiGraphics g, int mx, int my) {
		Shapes.ring(g, cx, cy, ringR - 0.6f, ringR + 0.6f, 0x22FFFFFF);
		CosmeticSlot[] slots = CosmeticSlot.values();
		float sweep = TAU / slots.length;
		for (int i = 0; i < slots.length; i++) {
			CosmeticSlot s = slots[i];
			float start = -TAU / 4 - sweep / 2 + i * sweep + 0.06f;
			float end = start + sweep - 0.12f;
			float mid = (start + end) / 2;
			float hover = anim("slot" + s, overSlot(mx, my, i), 16f);
			float on = anim("slotOn" + s, s == slot, 12f);
			float thick = 2f + 2.5f * on + hover;
			Shapes.arc(g, cx, cy, slotR - thick / 2, slotR + thick / 2, start, end, Render2D.withAlpha(slotColor(s), 0.3f + 0.7f * Math.max(on, hover * 0.6f)));
			float lr = slotR + 10 + on * 2;
			UiText.spacedSmall(g, s.title.toUpperCase(Locale.ROOT), cx + (float) Math.cos(mid) * lr, cy + (float) Math.sin(mid) * lr - 2,
				Render2D.lerpColor(Theme.O_TEXT_FAINT, slotColor(s), Math.max(on, hover)), true, 0.5f);
			// A dot on slots where something is worn.
			if (CosmeticManager.own().get(s) != null) {
				float dr = slotR - 7;
				Shapes.disc(g, cx + (float) Math.cos(mid) * dr, cy + (float) Math.sin(mid) * dr, 1.6f, Render2D.withAlpha(Theme.ON, 0.9f));
			}
		}
	}

	private int slotAt(double mx, double my) {
		double dx = mx - cx, dy = my - cy, d = Math.hypot(dx, dy);
		if (d < slotR - 9 || d > slotR + 18) return -1;
		int n = CosmeticSlot.values().length;
		double a = Math.atan2(dy, dx) + Math.PI / 2 + Math.PI / n;
		a = ((a % TAU) + TAU) % TAU;
		return (int) (a / (TAU / n)) % n;
	}

	private boolean overSlot(double mx, double my, int i) {
		return slotAt(mx, my) == i;
	}

	/** The slot's cosmetics, narrowed by the search. */
	private List<Cosmetic> shown() {
		List<Cosmetic> all = Cosmetics.in(slot);
		if (search.isEmpty()) return all;
		String q = search.toLowerCase(Locale.ROOT);
		List<Cosmetic> out = new ArrayList<>();
		for (Cosmetic c : all) if (c.name.toLowerCase(Locale.ROOT).contains(q) || c.id.contains(q)) out.add(c);
		return out;
	}

	/** How many nodes fit around the ring without touching. */
	private int capacity() {
		return Math.max(6, (int) (TAU * ringR / (nodeR * 2 + 9)));
	}

	/** The cosmetics of the slot, orbiting as nodes; they burst out of the core when the slot changes. */
	private void renderNodes(GuiGraphics g, int mx, int my, float eased) {
		List<Cosmetic> list = shown();
		int n = list.size() + 1; // "none" first
		int cap = capacity();
		carousel = n > cap;
		scrollTarget = Math.max(0, Math.min(carousel ? n - cap : 0, scrollTarget));
		scroll = Anim.approach(scroll, scrollTarget, 12f, dt);
		float base = Render2D.alpha;
		float elapsed = (System.currentTimeMillis() - burstStart) / 1000f * Anim.speed();
		var worn = CosmeticManager.own().get(slot);
		nodeHits.clear();
		boolean blocked = false;
		for (int i = 0; i < n; i++) {
			Cosmetic c = i == 0 ? null : list.get(i - 1);
			boolean owned = c == null || CosmeticManager.owns(c.id);
			float a, fade = 1f, order = i;
			if (carousel) {
				// Evenly spaced from the bottom of the ring, clockwise, shifted by the scroll.
				float p = i - scroll;
				if (p <= -1 || p >= cap) continue;
				fade = Anim.clamp01(Math.min(p + 1, cap - p));
				a = TAU / 4 + (p + 0.5f) * TAU / cap;
				order = Math.max(0, p);
			} else {
				a = -TAU / 4 + TAU * i / n + ringAngle;
			}
			float bp = Anim.enabled() ? Anim.easeOutBack(Anim.clamp01((elapsed - order * 0.03f) / 0.4f)) : 1f;
			float rad = ringR * bp * (closing ? eased : 1f);
			float nx = cx + (float) Math.cos(a) * rad, ny = cy + (float) Math.sin(a) * rad;
			boolean over = !blocked && !dragging && fade > 0.5f && Math.hypot(mx - nx, my - ny) <= nodeR + 3;
			if (over) {
				blocked = true;
				if (c == null) hoverNone = true;
				else hovered = c;
			}
			if (fade > 0.5f) nodeHits.add(new Object[]{c, nx, ny});
			boolean isWorn = c == null ? worn == null : worn != null && worn.id().equals(c.id);
			String key = c == null ? "none" : c.id;
			float hov = anim("hov" + key, over, 18f), en = anim("en" + key, isWorn, 10f);
			float r = nodeR * (1 + 0.16f * hov + 0.12f * en);
			Render2D.alpha = base * Anim.clamp01(bp * 1.4f) * fade * (owned ? 1f : 0.48f);
			if (en > 0.01f) {
				Shapes.ring(g, nx, ny, r + 1f, r + 2.6f, Render2D.withAlpha(Theme.ON, 0.9f * en));
				Shapes.ring(g, nx, ny, r + 2.6f, r + 5f, Render2D.withAlpha(Theme.ON, 0.18f * en));
			}
			if (hov > 0.01f) Shapes.ring(g, nx, ny, r + 6f, r + 7.2f, Render2D.withAlpha(slotColor(slot), hov));
			Shapes.disc(g, nx, ny, r, Render2D.lerpColor(0xF0151821, 0xFF242A3B, Math.max(hov, en)));
			// "None" counts as worn when nothing is worn, so only read a color from a real cosmetic.
			int color = c == null ? 0 : isWorn ? worn.color() : c.defaultColor;
			drawPicture(g, c, nx, ny, r * 1.5f, color, Math.max(hov, en));
			if (c != null && !owned) {
				Shapes.disc(g, nx, ny, r * 0.55f, 0xB5080A10);
				UiText.spacedSmall(g, "LOCK", nx, ny - 2, 0xFFE4D8C8, true, 0.5f);
			}
			Render2D.alpha = base;
		}
	}

	/** Top left: the search (type anywhere), and which part of a long slot is showing. */
	private void renderSearch(GuiGraphics g) {
		int x = 12, y = 10;
		int total = shown().size();
		if (search.isEmpty()) {
			Icons.draw(g, "ui_search", x, y + 3, 8, Theme.O_TEXT_FAINT);
			UiText.spacedSmall(g, "TYPE TO SEARCH", x + 12 + UiText.spacedWidth("TYPE TO SEARCH", true) * 0.3f, y + 5, Theme.O_TEXT_FAINT, true, 0.6f);
		} else {
			String count = total == 0 ? "no matches" : total + (total == 1 ? " match" : " matches");
			int w = Math.max(120, font.width(search) + font.width(count) + 40);
			Render2D.roundedRectBordered(g, x, y, w, 16, 8, Theme.GLASS, Render2D.withAlpha(slotColor(slot), 0.8f));
			Icons.draw(g, "ui_search", x + 6, y + 4, 8, 0xFFFFFFFF);
			UiText.text(g, search, x + 18, y + 4, 0xFFFFFFFF);
			UiText.text(g, "_", x + 18 + font.width(search), y + 4, UiText.caretColor());
			UiText.text(g, count, x + w - 8 - font.width(count), y + 4, Theme.O_TEXT_DIM);
		}
		if (carousel) {
			// The "none" node counts as the first one, so cosmetic numbers start one lower.
			int cap = capacity(), first = Math.max(1, Math.round(scroll)), last = Math.min(total, Math.round(scroll) + cap - 1);
			String range = first + "–" + last + " OF " + total;
			UiText.spacedSmall(g, range, x + UiText.spacedWidth(range, true) * 0.275f, y + 22, Theme.O_TEXT_DIM, true, 0.55f);
		}
	}

	/** A cosmetic's picture centered on (x, y): the cape design, the model's 3D picture, or an X for none. */
	private void drawPicture(GuiGraphics g, Cosmetic c, float x, float y, float size, int color, float highlight) {
		if (c == null) {
			Icons.drawCentered(g, "ui_close", x, y, size * 0.45f, Render2D.lerpColor(0xFF7E8597, 0xFFFFFFFF, highlight));
			return;
		}
		if (c.slot == CosmeticSlot.CAPE) {
			Identifier tex = c.capeTexture(System.nanoTime() / 1e9);
			float h = size * 0.82f, w = h * 10f / 16f;
			g.pose().pushMatrix();
			g.pose().translate(x - w / 2, y - h / 2);
			g.pose().scale(w / 40f, h / 64f);
			g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 4f, 4f, 40, 64, 40, 64, 256, 128, Render2D.a(0xFFFFFFFF));
			g.pose().popMatrix();
			return;
		}
		Identifier pic = CosmeticIcons.get(c, c.colorable ? color : c.defaultColor);
		if (pic == null) return;
		g.pose().pushMatrix();
		g.pose().translate(x - size / 2, y - size / 2);
		g.pose().scale(size / CosmeticIcons.SIZE, size / CosmeticIcons.SIZE);
		g.blit(RenderPipelines.GUI_TEXTURED, pic, 0, 0, 0f, 0f, CosmeticIcons.SIZE, CosmeticIcons.SIZE, CosmeticIcons.SIZE, CosmeticIcons.SIZE, Render2D.a(0xFFFFFFFF));
		g.pose().popMatrix();
	}

	/** The eclipse core with you inside it, turning; the corona takes the slot's color. */
	private void renderCore(GuiGraphics g, float eased) {
		float scale = closing ? eased : Anim.easeOutBack(openProgress);
		float r = coreR * scale;
		if (r <= 1) return;
		int glow = slotColor(slot);
		double t = System.nanoTime() / 1e9;
		for (int k = 0; k < 6; k++) Shapes.ring(g, cx, cy, r + k * 2.2f, r + k * 2.2f + 2.2f, Render2D.withAlpha(glow, 0.16f * (1 - k / 6f)));
		for (int i = 0; i < 18; i++) {
			float a0 = i * TAU / 18 + (float) ((t * 0.35) % TAU);
			float flare = 0.5f + 0.5f * (float) Math.sin(t * 2.1 + i * 1.7);
			float length = Math.round(flare * 6f) / 2f;
			Shapes.arc(g, cx, cy, r + 0.5f, r + 2.5f + length, a0, a0 + TAU / 18 * 0.55f, Render2D.withAlpha(Render2D.lerpColor(Theme.CORONA, glow, 0.5f), 0.3f + 0.35f * flare));
		}
		Shapes.disc(g, cx, cy, r, 0xFF06070A);
		Shapes.ring(g, cx, cy, r - 1.2f, r, Render2D.withAlpha(glow, 0.85f));
		if (scale > 0.6f) renderPlayer(g, Math.round(cx - r * 1.6f), Math.round(cy - r), Math.round(cx + r * 1.6f), Math.round(cy + r));
	}

	private void renderPlayer(GuiGraphics g, int x1, int y1, int x2, int y2) {
		if (minecraft.player == null) return;
		//? if <1.21.6 {
		/*// Before 1.21.6 the GUI draws the entity itself: turned like the preview for this one draw, then put back.
		var player = minecraft.player;
		float body = player.yBodyRot, bodyO = player.yBodyRotO, head = player.yHeadRot, headO = player.yHeadRotO;
		float yRot = player.getYRot(), yRotO = player.yRotO, xRot = player.getXRot(), xRotO = player.xRotO;
		float facing = 180f + yaw;
		player.yBodyRot = player.yBodyRotO = player.yHeadRot = player.yHeadRotO = facing;
		player.setYRot(facing);
		player.yRotO = facing;
		player.setXRot(0);
		player.xRotO = 0;
		float scale = (y2 - y1) / 3.1f;
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf camera = new Quaternionf().rotateX((float) Math.toRadians(-8));
		pose.mul(camera);
		net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(g, (x1 + x2) / 2f, (y1 + y2) / 2f, scale,
			new Vector3f(0, player.getBbHeight() / 2f + 0.16f, 0), pose, camera, player);
		player.yBodyRot = body;
		player.yBodyRotO = bodyO;
		player.yHeadRot = head;
		player.yHeadRotO = headO;
		player.setYRot(yRot);
		player.yRotO = yRotO;
		player.setXRot(xRot);
		player.xRotO = xRotO;
		*///?} else {
		var renderer = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
		EntityRenderState state = renderer.createRenderState(minecraft.player, 1f);
		//? if >=1.21.9 {
		state.lightCoords = 0xF000F0;
		state.shadowPieces.clear();
		state.outlineColor = 0;
		//?}
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180f + yaw;
			living.yRot = 0;
			living.xRot = 0;
		}
		// Sized so you, a hat and wings fit inside the core, with a little room around.
		float scale = (y2 - y1) / 3.1f;
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf camera = new Quaternionf().rotateX((float) Math.toRadians(-8));
		pose.mul(camera);
		// Centered on feet-to-hat-top rather than feet-to-head, so the hat doesn't poke out of the core.
		g.submitEntityRenderState(state, scale, new Vector3f(0, state.boundingBoxHeight / 2f + 0.16f, 0), pose, camera, x1, y1, x2, y2);
		//?}
	}

	private void renderTooltip(GuiGraphics g, int mx, int my) {
		String name = hovered != null ? hovered.name : hoverNone ? "None" : null;
		if (name == null) return;
		int w = font.width(name) + 10;
		int x = mx - w / 2, y = my - 24;
		Render2D.roundedRectBordered(g, x, y, w, 14, 7, Theme.GLASS, Render2D.withAlpha(slotColor(slot), 0.7f));
		UiText.text(g, name, x + 5, y + 3, 0xFFFFFFFF);
	}

	// ---------------------------------------------------------------- panel

	private void renderPanel(GuiGraphics g, int mx, int my, float eased) {
		int x = panelX + Math.round((1 - eased) * 40), y = panelY, w = panelW, h = panelH;
		int accent = slotColor(slot);
		Render2D.roundedRectBordered(g, x, y, w, h, 8, Theme.GLASS, Theme.GLASS_BORDER);
		Render2D.roundedRect(g, x + 12, y, w - 24, 2, 1, Render2D.withAlpha(accent, 0.9f));
		UiText.spaced(g, "WARDROBE", x + 12, y + 12, 0xFFFFFFFF, true);

		// Back / done.
		int dw = 46, dx = x + w - 12 - dw;
		float dh = anim("done", Render2D.hovered(mx, my, dx, y + 10, dw, 14), 16f);
		Render2D.roundedRect(g, dx, y + 10, dw, 14, 7, Render2D.lerpColor(0xFF2E9E4E, 0xFF3FD07A, dh));
		UiText.spacedSmall(g, "DONE", dx + dw / 2f, y + 14, 0xFFFFFFFF, true, 0.7f);
		hits.add(new Hit(dx, y + 10, dw, 14, this::onClose));

		// The cosmetic in focus: what you hover, else what you wear in this slot.
		var worn = CosmeticManager.own().get(slot);
		Cosmetic focus = hovered != null ? hovered : worn == null ? null : Cosmetics.get(worn.id());
		int top = y + 32;
		Render2D.fill(g, x + 12, top, x + w - 12, top + 1, 0x18FFFFFF);
		if (focus == null) {
			UiText.spacedSmall(g, slot.title.toUpperCase(Locale.ROOT), x + 12 + UiText.spacedWidth(slot.title.toUpperCase(Locale.ROOT), true) * 0.3f, top + 10, accent, true, 0.6f);
			List<String> lines = UiText.wrap("Nothing worn here. Hover a cosmetic in the ring to try it on, click to wear it.", w - 24, 4);
			for (int i = 0; i < lines.size(); i++) UiText.text(g, lines.get(i), x + 12, top + 24 + i * 10, Theme.O_TEXT_DIM);
		} else {
			boolean isWorn = worn != null && worn.id().equals(focus.id);
			int color = isWorn ? worn.color() : focus.defaultColor;
			// Big picture with a soft glow behind it.
			float pcx = x + w / 2f, pcy = top + 42;
			Shapes.disc(g, pcx, pcy, 30, Render2D.withAlpha(accent, 0.12f));
			drawPicture(g, focus, pcx, pcy, 58, color, 1f);
			UiText.centered(g, focus.name, x + w / 2, top + 78, 0xFFFFFFFF);
			String chip = focus.slot.title.toUpperCase(Locale.ROOT);
			int chipW = Math.round(UiText.spacedWidth(chip, true) * 0.55f) + 10;
			Render2D.roundedRect(g, x + (w - chipW) / 2, top + 90, chipW, 9, 4, Render2D.withAlpha(accent, 0.25f));
			UiText.spacedSmall(g, chip, x + w / 2f, top + 92, accent, true, 0.55f);

			if (!CosmeticManager.owns(focus.id)) {
				String message = CosmeticManager.ownershipLoaded()
					? "Get this free cosmetic at orbit.minearcade.org/cosmetics, then press Refresh. It's added to your account; nothing equips automatically."
					: "Checking your account's claims… Connect to the cosmetics server to unlock claimed items.";
				List<String> lines = UiText.wrap(message, w - 24, 5);
				for (int i = 0; i < lines.size(); i++) UiText.text(g, lines.get(i), x + 12, top + 108 + i * 10, Theme.O_TEXT_DIM);
			} else {
			// Wear / take off.
			int bw = w - 24, by = top + 104;
			float bh = anim("wearBtn", Render2D.hovered(mx, my, x + 12, by, bw, 16), 16f);
			int fill = isWorn ? Render2D.lerpColor(0xFF2A2D36, 0xFF3A3E4A, bh) : Render2D.lerpColor(0xFF3B6FD8, 0xFF5A8CF0, bh);
			Render2D.roundedRect(g, x + 12, by, bw, 16, 8, fill);
			UiText.spacedSmall(g, isWorn ? "TAKE OFF" : "WEAR", x + w / 2f, by + 5, 0xFFFFFFFF, true, 0.7f);
			final Cosmetic target = focus;
			hits.add(new Hit(x + 12, by, bw, 16, () -> toggleWear(target)));

			// Colors for what's worn.
			if (isWorn && focus.colorable) renderColors(g, mx, my, x, by + 24, w, focus);
			}
		}
		// Refresh claims (website redemptions appear without a restart); F5 does the same.
		boolean checking = CosmeticSync.checkingClaims();
		int rw = 50, rh = 12, rx = x + w - 12 - rw, ry = y + h - 23;
		float rhv = anim("refresh", !checking && Render2D.hovered(mx, my, rx, ry, rw, rh), 16f);
		Render2D.roundedRect(g, rx, ry, rw, rh, 6, Render2D.lerpColor(0xFF2A2D36, 0xFF3A3E4A, rhv));
		UiText.spacedSmall(g, checking ? "CHECKING" : "REFRESH", rx + rw / 2f, ry + 3, checking ? Theme.O_TEXT_DIM : 0xFFFFFFFF, true, 0.6f);
		if (!checking) hits.add(new Hit(rx, ry, rw, rh, CosmeticSync::requestRefresh));
		List<String> status = UiText.wrap(CosmeticSync.status(), w - 30 - rw, 2);
		for (int i = 0; i < status.size(); i++) UiText.text(g, status.get(i), x + 12, y + h - 12 - (status.size() - i) * 10, Theme.O_TEXT_FAINT);
		renderOutfit(g, mx, my, x, y + h - 16 - status.size() * 10 - OUTFIT_H, w);
	}

	private static final int OUTFIT_H = 42;

	/** Everything you wear, one chip per slot; click a chip to jump to that slot. */
	private void renderOutfit(GuiGraphics g, int mx, int my, int x, int y, int w) {
		Render2D.fill(g, x + 12, y, x + w - 12, y + 1, 0x18FFFFFF);
		UiText.spacedSmall(g, "OUTFIT", x + 12 + UiText.spacedWidth("OUTFIT", true) * 0.3f, y + 7, Theme.O_TEXT_DIM, true, 0.6f);
		CosmeticSlot[] slots = CosmeticSlot.values();
		float step = (w - 24f) / slots.length, r = Math.min(9.5f, step / 2 - 1.5f);
		for (int i = 0; i < slots.length; i++) {
			CosmeticSlot s = slots[i];
			float px = x + 12 + step * (i + 0.5f), py = y + 27;
			boolean over = Math.hypot(mx - px, my - py) <= r + 1;
			float hv = anim("outfit" + s, over, 16f), on = anim("outfitOn" + s, s == slot, 12f);
			var worn = CosmeticManager.own().get(s);
			Cosmetic c = worn == null ? null : Cosmetics.get(worn.id());
			Shapes.disc(g, px, py, r, Render2D.lerpColor(0xF0151821, 0xFF242A3B, Math.max(hv, on)));
			Shapes.ring(g, px, py, r, r + 1.2f, Render2D.withAlpha(slotColor(s), 0.25f + 0.75f * Math.max(on, hv)));
			if (c != null) drawPicture(g, c, px, py, r * 1.55f, worn.color(), 1f);
			else Shapes.disc(g, px, py, 1.5f, Render2D.withAlpha(slotColor(s), 0.5f));
			if (over) {
				String tip = s.title + ": " + (c == null ? "nothing" : c.name);
				int tw = font.width(tip) + 10;
				int tx = Math.round(Math.min(Math.max(px - tw / 2f, x + 4), x + w - 4 - tw));
				Render2D.roundedRectBordered(g, tx, Math.round(py - r - 18), tw, 14, 7, Theme.GLASS, Render2D.withAlpha(slotColor(s), 0.7f));
				UiText.text(g, tip, tx + 5, Math.round(py - r - 15), 0xFFFFFFFF);
			}
			final CosmeticSlot target = s;
			hits.add(new Hit(Math.round(px - r), Math.round(py - r), Math.round(r * 2), Math.round(r * 2), () -> setSlot(target)));
		}
	}

	private void renderColors(GuiGraphics g, int mx, int my, int x, int y, int w, Cosmetic c) {
		if (!c.id.equals(colorFor) || colorSetting == null) {
			colorFor = c.id;
			colorSetting = new ColorSetting("Color", CosmeticManager.own().get(c.slot).color());
		}
		int sw = 12, gap = 4, perRow = Math.max(1, (w - 24 + gap) / (sw + gap));
		for (int i = 0; i < QUICK_COLORS.length; i++) {
			int sx = x + 12 + (i % perRow) * (sw + gap), sy = y + (i / perRow) * (sw + gap);
			boolean over = Render2D.hovered(mx, my, sx, sy, sw, sw);
			float hv = anim("q" + i, over, 16f);
			boolean current = (colorSetting.get() | 0xFF000000) == QUICK_COLORS[i];
			int grow = Math.round(hv);
			Render2D.roundedRectBordered(g, sx - grow, sy - grow, sw + grow * 2, sw + grow * 2, 3, QUICK_COLORS[i],
				current ? 0xFFFFFFFF : Render2D.lerpColor(0x40FFFFFF, 0xC0FFFFFF, hv));
			final int col = QUICK_COLORS[i];
			hits.add(new Hit(sx, sy, sw, sw, () -> colorSetting.set(col)));
		}
		int rows = (QUICK_COLORS.length + perRow - 1) / perRow;
		int ly = y + rows * (sw + gap) + 2;
		colorList.show(List.of(colorSetting));
		// Leaves room for the outfit row and status at the bottom.
		colorList.render(g, x + 10, ly, w - 20, panelY + panelH - OUTFIT_H - 36 - ly, mx, my, dt);
		int chosen = colorSetting.get() | 0xFF000000;
		if (chosen != (CosmeticManager.own().get(c.slot).color() | 0xFF000000)) CosmeticManager.setColor(c.slot, chosen);
	}

	private void toggleWear(Cosmetic c) {
		if (!CosmeticManager.owns(c.id)) return;
		var worn = CosmeticManager.own().get(c.slot);
		if (worn != null && worn.id().equals(c.id)) CosmeticManager.remove(c.slot);
		else CosmeticManager.wear(c, c.defaultColor);
		colorSetting = null;
	}

	private void setSlot(CosmeticSlot s) {
		if (slot == s) return;
		slot = lastSlot = s;
		burstStart = System.currentTimeMillis();
		ringAngle = ringTarget = 0;
		scroll = scrollTarget = 0;
		colorSetting = null;
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) return true;
		double mx = event.x(), my = event.y();
		if (colorSetting != null && colorList.mouseClicked(mx, my, event.button())) return true;
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (Render2D.hovered(mx, my, h.x, h.y, h.w, h.h)) {
				h.action.run();
				return true;
			}
		}
		for (Object[] hit : nodeHits) {
			if (Math.hypot(mx - (float) hit[1], my - (float) hit[2]) > nodeR + 3) continue;
			Cosmetic c = (Cosmetic) hit[0];
			if (c == null) CosmeticManager.remove(slot);
			else toggleWear(c);
			return true;
		}
		int s = slotAt(mx, my);
		if (s >= 0) {
			setSlot(CosmeticSlot.values()[s]);
			return true;
		}
		if (Math.hypot(mx - cx, my - cy) <= coreR) {
			dragging = true;
			lastDrag = System.currentTimeMillis();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			yaw -= (float) dx * 1.4f;
			lastDrag = System.currentTimeMillis();
			return true;
		}
		if (colorSetting != null && colorList.mouseDragged(event.x(), event.y())) return true;
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		colorList.mouseReleased();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		if (carousel) scrollTarget -= (float) vertical * 2;
		else ringTarget += (float) vertical * 0.35f;
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (closing || !event.isAllowedChatCharacter()) return false;
		String s = event.codepointAsString();
		if (colorSetting != null && colorList.charTyped(s)) return true;
		if (search.length() < 24 && !(s.equals(" ") && search.isEmpty())) {
			search += s;
			scroll = scrollTarget = 0;
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (closing) return true;
		int key = event.key();
		if (colorSetting != null && colorList.keyPressed(key)) return true;
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (!search.isEmpty()) search = "";
			else onClose();
			return true;
		}
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
			return true;
		}
		if (key == GLFW.GLFW_KEY_F5) {
			CosmeticSync.requestRefresh();
			return true;
		}
		if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
			if (carousel) scrollTarget += key == GLFW.GLFW_KEY_DOWN ? 3 : -3;
			return true;
		}
		if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_LEFT) {
			CosmeticSlot[] s = CosmeticSlot.values();
			setSlot(s[Math.floorMod(slot.ordinal() + (key == GLFW.GLFW_KEY_RIGHT ? 1 : -1), s.length)]);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (closing) return;
		colorList.commit();
		CosmeticManager.setPreview(null);
		ConfigManager.save();
		closing = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (closing && openProgress <= 0f) minecraft.setScreen(new OrbitScreen());
	}

	/** For tests. */
	public void showTab(CosmeticSlot s) {
		setSlot(s);
	}

	/** For tests: search as if typed. */
	public void searchFor(String query) {
		search = query;
		scroll = scrollTarget = 0;
	}
}
