package com.orbitclient.gui;

import com.orbitclient.OrbitClient;
import com.orbitclient.config.ClientSettings;
import com.orbitclient.config.ConfigManager;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.module.ModuleManager;
import com.orbitclient.setting.ActionSetting;
import com.orbitclient.setting.Setting;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Icons;
import com.orbitclient.util.Render2D;
import com.orbitclient.util.Shapes;
import com.orbitclient.waypoint.Waypoint;
import com.orbitclient.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Orbit, the Orbit Client mod menu.
 *
 * <p>Mods are glowing nodes on a ring around an eclipse: a dark core with a shimmering corona.
 * Spin the ring (scroll / arrow keys) and the mod at the pointer is selected; its settings appear in
 * the glass panel on the right. The core shows the selected mod and is its power button: the corona
 * glows green when it's on. Four arcs around the core switch category (HUD, Server, Mechanic,
 * Favorites); typing anywhere searches every mod. The dock on the left opens profiles, waypoints,
 * client settings and the HUD editor.</p>
 */
public class OrbitScreen extends Screen {
	private enum Panel {MOD, PROFILES, WAYPOINTS, SETTINGS}

	private static final float TAU = (float) (Math.PI * 2);
	private static final String[] CATEGORIES = {"HUD", "SERVER", "MECHANIC", "FAVORITES"};
	private static final int[] CAT_COLORS = {Theme.CAT_HUD, Theme.CAT_SERVER, Theme.CAT_MECHANIC, Theme.CAT_FAVORITES};
	/** Arc centers in degrees (0 = right, clockwise): upper-left, upper-right, lower-right, lower-left. */
	private static final float[] CAT_CENTER = {225, 315, 45, 135};
	private static final float CAT_SWEEP = (float) Math.toRadians(78);

	private record DockItem(String icon, String label, Panel panel) {
	}

	private static final DockItem[] DOCK = {
		new DockItem("ui_mods", "Mods", Panel.MOD),
		new DockItem("ui_profiles", "Profiles", Panel.PROFILES),
		new DockItem("waypoints", "Waypoints", Panel.WAYPOINTS),
		new DockItem("ui_settings", "Settings", Panel.SETTINGS),
		new DockItem("ui_wardrobe", "Wardrobe", null),
		new DockItem("ui_hud", "Edit HUD", null),
		new DockItem("ui_close", "Close", null),
	};

	private static int lastCategory;
	private static Module lastSelected;

	private int category;
	private String search = "";
	private Module selected;
	private Panel panel = Panel.MOD;

	private final SettingsList settingsList = new SettingsList(a -> {
		if (a.closesMenu()) closeThen(a::run);
		else a.run();
	});
	private final Anim.Clock clock = new Anim.Clock();
	private float dt;
	private final Map<String, Float> anims = new HashMap<>();

	// Animation state
	private float openProgress;
	private boolean closing;
	private Runnable afterClose;
	private float ringAngle;
	private long burstStart = System.currentTimeMillis();
	private List<Module> lastList = List.of();
	private final Map<Module, Long> ripples = new HashMap<>();
	private long coreRipple;
	private long panelChange = System.currentTimeMillis();
	private int panelDir;
	private final float[][] stars = new float[120][4];

	// Geometry, recomputed every frame
	private int cx, cy, ringR, coreR, catR;
	private float nodeR;
	private int panelX, panelY, panelW, panelH, dockX, dockY;
	private final List<Object[]> nodeHits = new ArrayList<>(); // {Module, x, y, r}

	// Name editing (profiles, waypoints) and waypoint list scroll
	private String editingProfile;
	private WaypointManager.SubWorld editingSubWorld;
	private WaypointEditor wpEditor;
	private String editBuffer = "";
	private float listScroll, listScrollTarget;

	public OrbitScreen() {
		this(null);
	}

	public OrbitScreen(Module focus) {
		super(Component.literal("Orbit Client"));
		Random random = new Random(42);
		for (float[] s : stars) {
			s[0] = random.nextFloat();
			s[1] = random.nextFloat();
			s[2] = 0.3f + random.nextFloat() * 0.7f;
			s[3] = random.nextFloat() * TAU;
		}
		if (focus != null) {
			category = focus.getCategory().ordinal();
			selected = focus;
		} else {
			category = lastCategory;
			selected = lastSelected;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ================================================================ helpers

	/** Menu icons are referred to by name; see Icons. */
	private static String icon(String name) {
		return name;
	}

	private void blitIcon(GuiGraphics g, String name, float x, float y, float size, int color) {
		Icons.draw(g, name, x, y, size, color);
	}

	private void moduleIcon(GuiGraphics g, Module m, float cx, float cy, float size, int color) {
		if (ClientSettings.INSTANCE.iconStyle.is("Custom") && Icons.exists(m.getIconName())) {
			Icons.drawCentered(g, m.getIconName(), cx, cy, size, color);
			return;
		}
		float grow = Anim.easeOutCubic((Render2D.alpha - 0.35f) / 0.65f);
		if (grow <= 0.02f) return;
		float s = size / 16f * grow;
		g.pose().pushMatrix();
		g.pose().translate(cx - 8 * s, cy - 8 * s);
		g.pose().scale(s, s);
		g.renderItem(m.getIcon(), 0, 0);
		g.pose().popMatrix();
	}

	private float anim(String key, boolean target, float speed) {
		float v = Anim.approach(anims.getOrDefault(key, target ? 1f : 0f), target ? 1f : 0f, speed, dt);
		anims.put(key, v);
		return v;
	}

	private static int categoryColor(Module m) {
		return CAT_COLORS[m.getCategory().ordinal()];
	}

	private List<Module> nodes() {
		String q = search.toLowerCase(Locale.ROOT);
		List<Module> list = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			boolean match = !q.isEmpty() ? m.getName().toLowerCase(Locale.ROOT).contains(q) : switch (category) {
				case 0 -> m.getCategory() == Category.HUD;
				case 1 -> m.getCategory() == Category.SERVER;
				case 2 -> m.getCategory() == Category.MECHANIC;
				default -> m.favorite;
			};
			if (match) list.add(m);
		}
		return list;
	}

	private void select(Module m, int dir) {
		if (m == selected) return;
		selected = m;
		if (panel == Panel.MOD) {
			panelChange = System.currentTimeMillis();
			panelDir = dir;
		}
	}

	private void toggle(Module m) {
		m.toggle();
		ripples.put(m, System.currentTimeMillis());
		if (m == selected) coreRipple = System.currentTimeMillis();
	}

	private void openPanel(Panel p) {
		if (p == panel) return;
		commitNames();
		settingsList.commit();
		panelDir = Integer.compare(p.ordinal(), panel.ordinal());
		panel = p;
		panelChange = System.currentTimeMillis();
		listScroll = listScrollTarget = 0;
	}

	private void layout() {
		int dockW = 44;
		panelW = Math.max(190, Math.min(290, Math.round(width * 0.38f)));
		panelX = width - panelW - 14;
		panelH = Math.min(height - 28, 320);
		int dialLeft = dockW + 14, dialRight = panelX - 26;
		int dialW = dialRight - dialLeft;
		ringR = Math.max(56, Math.min(Math.round(height * 0.34f), dialW / 2 - 26));
		cx = dialLeft + dialW / 2;
		cy = height / 2 + 6;
		coreR = Math.max(18, Math.round(ringR * 0.3f));
		catR = Math.round(ringR * 0.56f);
		int n = Math.max(1, lastList.size());
		float spacing = (float) (TAU * ringR / n);
		nodeR = Math.max(6, Math.min(11, spacing * 0.36f));
		panelY = Math.max(12, Math.min(height - panelH - 12, cy - panelH / 2));
		dockX = 12;
		dockY = height / 2 - DOCK.length * 28 / 2;
	}

	// ================================================================ open / close

	@Override
	public void onClose() {
		closeThen(null);
	}

	private void closeThen(Runnable action) {
		if (closing) return;
		commitNames();
		settingsList.commit();
		if (wpEditor != null) wpEditor.apply();
		lastCategory = category;
		lastSelected = selected;
		ConfigManager.save();
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

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		float e = Anim.easeOutCubic(openProgress);
		if (ClientSettings.INSTANCE.menuBlur.isOn()) super.renderBackground(g, mx, my, delta);
		g.fill(0, 0, width, height, Render2D.withAlpha(0x7A05060A, e));
	}

	// ================================================================ rendering

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		dt = clock.tick();
		openProgress = Anim.step(openProgress, !closing, closing ? 0.2f : 0.35f, dt);
		float eased = closing ? Anim.easeInOutCubic(openProgress) : Anim.easeOutCubic(openProgress);

		// Keep the selection valid for the current category / search, and spin the ring toward it.
		List<Module> list = nodes();
		if (!list.equals(lastList)) {
			lastList = list;
			if (selected == null || !list.contains(selected)) selected = list.isEmpty() ? null : list.getFirst();
			burstStart = System.currentTimeMillis();
			ringAngle = targetAngle(list);
		}
		layout();
		float target = targetAngle(list);
		while (target - ringAngle > Math.PI) target -= TAU;
		while (target - ringAngle < -Math.PI) target += TAU;
		ringAngle = Anim.approach(ringAngle, target, 9f, dt);

		if (panel == Panel.MOD && selected != null) settingsList.show(selected.getSettings());
		else if (panel == Panel.SETTINGS) settingsList.show(ClientSettings.INSTANCE.getSettings());
		else if (panel == Panel.WAYPOINTS && wpEditor != null) settingsList.show(wpEditor.settings());

		Render2D.alpha = eased;
		try {
			renderStars(g, eased);
			renderDial(g, mx, my, eased, list);
			renderSearch(g, eased);
			renderDock(g, mx, my, eased);
			renderPanel(g, mx, my, eased);
			renderTooltip(g, mx, my);
			String hint = "Scroll to spin  •  Click the core to toggle  •  Right-click a mod: quick toggle  •  Type to search";
			// Centered between the dock and the panel, shrunk to fit.
			int left = dockX + 34, right = panelX - 10;
			float hs = Math.min(0.75f, (right - left) / (float) font.width(hint));
			g.pose().pushMatrix();
			g.pose().translate((left + right) / 2f - font.width(hint) * hs / 2f, height - 12);
			g.pose().scale(hs, hs);
			UiText.text(g, hint, 0, 0, Theme.O_TEXT_FAINT);
			g.pose().popMatrix();
		} finally {
			Render2D.alpha = 1f;
		}
	}

	/**
	 * Seconds for looping animations, as a double: the clock in seconds is ~1.8 billion, where a float only
	 * has a precision of about 64, which made every corona arc land in the same spot and the stars jump.
	 * Angles and positions are reduced (modulo) before converting to float.
	 */
	private static double clockSeconds() {
		return System.currentTimeMillis() / 1000.0;
	}

	private float targetAngle(List<Module> list) {
		int i = list.indexOf(selected);
		return i < 0 ? 0 : -TAU * i / list.size();
	}

	/** A slow drifting starfield that twinkles. */
	private void renderStars(GuiGraphics g, float eased) {
		double t = clockSeconds();
		int n = stars.length;
		float[] xs = new float[n], ys = new float[n];
		int[] sizes = new int[n], colors = new int[n];
		for (int i = 0; i < n; i++) {
			float[] s = stars[i];
			xs[i] = (float) (((s[0] * width + t * s[2] * 3.0) % width + width) % width);
			ys[i] = s[1] * height;
			sizes[i] = s[2] > 0.8f ? 2 : 1;
			float twinkle = 0.45f + 0.35f * (float) Math.sin(t * 1.4 + s[3]);
			colors[i] = Render2D.withAlpha(0xFFFFFFFF, twinkle * s[2] * eased);
		}
		Shapes.dots(g, xs, ys, sizes, colors);
	}

	private void renderDial(GuiGraphics g, int mx, int my, float eased, List<Module> list) {
		float base = Render2D.alpha;
		long now = System.currentTimeMillis();

		// Faint orbit track.
		Shapes.ring(g, cx, cy, ringR - 0.6f, ringR + 0.6f, 0x22FFFFFF);

		// Category arcs.
		boolean searching = !search.isEmpty();
		for (int c = 0; c < 4; c++) {
			float mid = (float) Math.toRadians(CAT_CENTER[c]);
			float start = mid - CAT_SWEEP / 2;
			boolean active = !searching && category == c;
			float hover = anim("cat" + c, !searching && overArc(mx, my, c), 16f);
			float on = anim("catOn" + c, active, 12f);
			float thick = 2f + 2.5f * on + hover;
			float a = searching ? 0.25f : 0.3f + 0.7f * Math.max(on, hover * 0.6f);
			Shapes.arc(g, cx, cy, catR - thick / 2, catR + thick / 2, start, start + CAT_SWEEP, Render2D.withAlpha(CAT_COLORS[c], a));
			float lr = catR + 10 + on * 2;
			float lx = cx + (float) Math.cos(mid) * lr, ly = cy + (float) Math.sin(mid) * lr;
			UiText.spacedSmall(g, CATEGORIES[c], lx, ly - 2, Render2D.lerpColor(Theme.O_TEXT_FAINT, CAT_COLORS[c], Math.max(on, hover)), true, 0.5f);
		}

		// Nodes burst out of the core when the list changes, and fall back in when closing.
		nodeHits.clear();
		Module hovered = null;
		float elapsed = (now - burstStart) / 1000f * Anim.speed();
		int n = list.size();
		for (int i = 0; i < n; i++) {
			Module m = list.get(i);
			float a = TAU * i / n + ringAngle;
			float bp = Anim.enabled() ? Anim.easeOutBack(Anim.clamp01((elapsed - i * 0.012f) / 0.4f)) : 1f;
			float rad = ringR * bp * (closing ? eased : 1f);
			float nx = cx + (float) Math.cos(a) * rad, ny = cy + (float) Math.sin(a) * rad;
			float dist = (float) Math.hypot(mx - nx, my - ny);
			boolean over = dist <= nodeR + 3 && hovered == null;
			if (over) hovered = m;
			nodeHits.add(new Object[]{m, nx, ny});

			String key = m.getName();
			float sel = anim("sel" + key, m == selected, 12f);
			float hov = anim("hov" + key, over, 18f);
			float en = anim("en" + key, m.isEnabled(), 10f);
			float r = nodeR * (1 + 0.14f * hov + 0.25f * sel);
			Render2D.alpha = base * Anim.clamp01(bp * 1.4f);

			if (en > 0.01f) {
				Shapes.ring(g, nx, ny, r + 1f, r + 2.6f, Render2D.withAlpha(Theme.ON, 0.9f * en));
				Shapes.ring(g, nx, ny, r + 2.6f, r + 5f, Render2D.withAlpha(Theme.ON, 0.18f * en));
			}
			if (sel > 0.01f) Shapes.ring(g, nx, ny, r + 6f, r + 7.2f, Render2D.withAlpha(categoryColor(m), sel));
			Long ripple = ripples.get(m);
			if (ripple != null) {
				float p = Anim.since(ripple, 0.5f);
				if (p >= 1f) ripples.remove(m);
				else Shapes.ring(g, nx, ny, r + p * 16, r + p * 16 + 1.5f, Render2D.withAlpha(m.isEnabled() ? Theme.ON : 0xFFFFFFFF, 1 - p));
			}
			Shapes.disc(g, nx, ny, r, Render2D.lerpColor(0xF0151821, 0xFF242A3B, Math.max(hov, sel)));
			float size = Math.max(8, r * 1.25f);
			moduleIcon(g, m, nx, ny, size, Render2D.lerpColor(0xFF7E8597, 0xFFFFFFFF, Math.max(en, Math.max(hov, sel))));
		}
		Render2D.alpha = base;
		hoveredNode = hovered;

		// Pointer at the right of the ring, with a flowing line to the panel.
		if (selected != null && panel == Panel.MOD) {
			int px0 = Math.round(cx + ringR + nodeR * 1.25f + 10);
			int color = categoryColor(selected);
			for (int k = 0; k <= 4; k++) Render2D.fill(g, px0 + k, cy - (4 - k), px0 + k + 1, cy + (4 - k) + 1, color);
			int lineEnd = panelX - 4;
			int offset = (int) ((now / 45) % 7);
			for (int x = px0 + 8 - offset; x < lineEnd; x += 7) {
				int x1 = Math.max(x, px0 + 7), x2 = Math.min(x + 4, lineEnd);
				if (x2 > x1) Render2D.fill(g, x1, cy, x2, cy + 1, Render2D.withAlpha(color, 0.55f));
			}
		}

		renderCore(g, mx, my, eased);
	}

	private Module hoveredNode;

	private boolean overArc(double mx, double my, int c) {
		double dx = mx - cx, dy = my - cy, dist = Math.hypot(dx, dy);
		if (dist < catR - 9 || dist > catR + 16) return false;
		double a = Math.atan2(dy, dx);
		double mid = Math.toRadians(CAT_CENTER[c]);
		double diff = Math.abs(((a - mid) % TAU + TAU * 1.5) % TAU - Math.PI);
		return diff <= CAT_SWEEP / 2 + 0.05;
	}

	private boolean overCore(double mx, double my) {
		return Math.hypot(mx - cx, my - cy) <= coreR + 2;
	}

	/** The eclipse: a dark disc with a glowing, shimmering corona colored by the selected mod's state. */
	private void renderCore(GuiGraphics g, int mx, int my, float eased) {
		float coreScale = closing ? eased : Anim.easeOutBack(openProgress);
		float hover = anim("core", selected != null && overCore(mx, my), 16f);
		float r = coreR * coreScale * (1 + 0.05f * hover);
		if (r <= 1) return;
		float en = anim("coreOn", selected != null && selected.isEnabled(), 8f);
		int glow = selected == null ? Theme.CORONA : Render2D.lerpColor(Theme.OFF, Theme.ON, en);
		double t = clockSeconds();

		for (int k = 0; k < 6; k++) {
			Shapes.ring(g, cx, cy, r + k * 2.2f, r + k * 2.2f + 2.2f, Render2D.withAlpha(glow, 0.2f * (1 - k / 6f) * (0.6f + 0.4f * en)));
		}
		int corona = Render2D.lerpColor(Theme.CORONA, glow, 0.55f);
		for (int i = 0; i < 18; i++) {
			float a0 = i * TAU / 18 + (float) ((t * 0.35) % TAU);
			float flare = 0.5f + 0.5f * (float) Math.sin(t * 2.1 + i * 1.7);
			// Flare length snapped to half pixels so only a few shape textures are ever needed.
			float length = Math.round(flare * 6f) / 2f;
			Shapes.arc(g, cx, cy, r + 0.5f, r + 2.5f + length, a0, a0 + TAU / 18 * 0.55f, Render2D.withAlpha(corona, 0.35f + 0.4f * flare));
		}
		Shapes.disc(g, cx, cy, r, 0xFF06070A);
		Shapes.ring(g, cx, cy, r - 1.2f, r, Render2D.withAlpha(glow, 0.85f));
		if (coreRipple > 0) {
			float p = Anim.since(coreRipple, 0.6f);
			if (p < 1f) Shapes.ring(g, cx, cy, r + p * 34, r + p * 34 + 2, Render2D.withAlpha(glow, (1 - p) * 0.8f));
		}

		if (selected != null) {
			float size = Math.max(12, r * 0.8f);
			moduleIcon(g, selected, cx, cy - r * 0.12f, size, Render2D.lerpColor(0xFF9AA0B0, 0xFFFFFFFF, Math.max(en, hover)));
			String state = hover > 0.5f ? "TOGGLE" : selected.isEnabled() ? "ON" : "OFF";
			UiText.spacedSmall(g, state, cx, cy + r * 0.42f, glow, true, 0.55f);
		} else {
			UiText.spacedSmall(g, "ORBIT", cx, cy - 3, Theme.CORONA, true, 0.7f);
		}
	}

	private void renderSearch(GuiGraphics g, float eased) {
		int y = Math.max(8, Math.round(cy - ringR - nodeR - 34));
		float on = anim("search", !search.isEmpty(), 12f);
		if (on < 0.99f) {
			float base = Render2D.alpha;
			Render2D.alpha = base * (1 - on) * 0.7f;
			UiText.spacedSmall(g, "TYPE TO SEARCH", cx + 6, y + 5, Theme.O_TEXT_FAINT, true, 0.6f);
			blitIcon(g, icon("ui_search"), cx - UiText.spacedWidth("TYPE TO SEARCH", true) * 0.3f - 8, y + 3, 8, Theme.O_TEXT_FAINT);
			Render2D.alpha = base;
		}
		if (on > 0.01f) {
			float base = Render2D.alpha;
			Render2D.alpha = base * on;
			String count = lastList.size() + (lastList.size() == 1 ? " match" : " matches");
			int w = Math.max(120, font.width(search) + font.width(count) + 40);
			int x = cx - w / 2, yy = y + Math.round((1 - on) * 6);
			Render2D.roundedRectBordered(g, x, yy, w, 16, 8, Theme.GLASS, Render2D.withAlpha(Theme.CAT_HUD, 0.8f));
			blitIcon(g, icon("ui_search"), x + 6, yy + 4, 8, 0xFFFFFFFF);
			UiText.text(g, search, x + 18, yy + 4, 0xFFFFFFFF);
			UiText.text(g, "_", x + 18 + font.width(search), yy + 4, UiText.caretColor());
			UiText.text(g, count, x + w - 8 - font.width(count), yy + 4, Theme.O_TEXT_DIM);
			Render2D.alpha = base;
		}
	}

	private void renderDock(GuiGraphics g, int mx, int my, float eased) {
		int x = dockX - Math.round((1 - eased) * 30);
		for (int i = 0; i < DOCK.length; i++) {
			DockItem item = DOCK[i];
			int y = dockY + i * 28;
			boolean over = Render2D.hovered(mx, my, dockX, y, 22, 22);
			float h = anim("dock" + i, over, 16f);
			float active = anim("dockOn" + i, item.panel != null && item.panel == panel, 12f);
			float ccx = x + 11, ccy = y + 11;
			Shapes.disc(g, ccx, ccy, 11 + h, Render2D.lerpColor(0xD0141720, 0xFF232838, Math.max(h, active)));
			int ring = i == DOCK.length - 1 ? Render2D.lerpColor(0x40FFFFFF, Theme.DISABLED, h) : Render2D.lerpColor(0x30FFFFFF, Theme.CAT_HUD, active);
			Shapes.ring(g, ccx, ccy, 11 + h - 1.2f, 11 + h, ring);
			blitIcon(g, icon(item.icon), ccx - 6, ccy - 6, 12, Render2D.lerpColor(0xFF9AA0B0, 0xFFFFFFFF, Math.max(h, active)));
			if (h > 0.01f) {
				float base = Render2D.alpha;
				Render2D.alpha = base * h;
				int lw = font.width(item.label) + 10, lx = x + 28 + Math.round((1 - h) * -6);
				Render2D.roundedRect(g, lx, y + 4, lw, 14, 7, Theme.GLASS);
				UiText.text(g, item.label, lx + 5, y + 7, 0xFFFFFFFF);
				Render2D.alpha = base;
			}
		}
	}

	private void renderTooltip(GuiGraphics g, int mx, int my) {
		if (hoveredNode == null || hoveredNode == selected) return;
		String name = hoveredNode.getName();
		int w = font.width(name) + 10;
		int x = mx - w / 2, y = my - 22;
		Render2D.roundedRectBordered(g, x, y, w, 14, 7, Theme.GLASS, Render2D.withAlpha(categoryColor(hoveredNode), 0.7f));
		UiText.text(g, name, x + 5, y + 3, 0xFFFFFFFF);
	}

	// ---------------------------------------------------------------- panel

	private void renderPanel(GuiGraphics g, int mx, int my, float eased) {
		int x = panelX + Math.round((1 - eased) * 40), y = panelY, w = panelW, h = panelH;
		int accent = panel == Panel.MOD && selected != null ? categoryColor(selected) : Theme.CAT_HUD;
		Render2D.roundedRectBordered(g, x, y, w, h, 8, Theme.GLASS, Theme.GLASS_BORDER);
		Render2D.roundedRect(g, x + 12, y, w - 24, 2, 1, Render2D.withAlpha(accent, 0.9f));

		float p = Anim.easeOutCubic(Anim.since(panelChange, 0.25f));
		float base = Render2D.alpha;
		Render2D.alpha = base * p;
		g.pose().pushMatrix();
		g.pose().translate((1 - p) * 12 * panelDir, 0);
		switch (panel) {
			case MOD -> renderModPanel(g, mx, my, x, y, w, h);
			case SETTINGS -> {
				header(g, x, y, "CLIENT SETTINGS", "Shared by every profile");
				settingsList.render(g, x + 10, y + 46, w - 24, h - 56, mx, my, dt);
			}
			case PROFILES -> renderProfiles(g, mx, my, x, y, w, h);
			case WAYPOINTS -> renderWaypoints(g, mx, my, x, y, w, h);
		}
		g.pose().popMatrix();
		Render2D.alpha = base;
	}

	private void header(GuiGraphics g, int x, int y, String title, String subtitle) {
		UiText.spaced(g, title, x + 12, y + 12, 0xFFFFFFFF, true);
		UiText.fit(g, subtitle, x + 12, y + 25, panelW - 24, Theme.O_TEXT_DIM);
		Render2D.fill(g, x + 12, y + 39, x + panelW - 12, y + 40, 0x18FFFFFF);
	}

	private void renderModPanel(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
		if (selected == null) {
			String msg = category == 3 && search.isEmpty() ? "Middle-click a mod to favorite it." : "No mods here.";
			UiText.centered(g, msg, x + w / 2, y + h / 2 - 4, Theme.O_TEXT_DIM);
			return;
		}
		Module m = selected;
		moduleIcon(g, m, x + 20, y + 20, 16, 0xFFFFFFFF);
		UiText.fit(g, m.getName().toUpperCase(Locale.ROOT), x + 34, y + 11, w - 120, 0xFFFFFFFF);
		String cat = m.getCategory().displayName.toUpperCase(Locale.ROOT);
		int chipW = Math.round(UiText.spacedWidth(cat, true) * 0.55f) + 10;
		Render2D.roundedRect(g, x + 34, y + 22, chipW, 9, 4, Render2D.withAlpha(categoryColor(m), 0.25f));
		UiText.spacedSmall(g, cat, x + 34 + chipW / 2f, y + 24, categoryColor(m), true, 0.55f);

		// Power switch, favorite star and reset.
		int sx = x + w - 12 - 30;
		float en = Anim.easeInOutCubic(anim("power" + m.getName(), m.isEnabled(), 12f));
		float ph = anim("powerHover", Render2D.hovered(mx, my, sx, y + 12, 30, 14), 16f);
		Render2D.roundedRect(g, sx, y + 12, 30, 14, 7, Render2D.lerpColor(Render2D.lerpColor(Theme.OFF, Theme.ON, en), 0xFFFFFFFF, ph * 0.1f));
		Render2D.circle(g, sx + 7 + Math.round(en * 16), y + 19, 5, 0xFFFFFFFF);
		int starX = sx - 20;
		float fh = anim("starHover", Render2D.hovered(mx, my, starX, y + 11, 16, 16), 16f);
		float fav = anim("fav" + m.getName(), m.favorite, 12f);
		blitIcon(g, icon(m.favorite ? "ui_star" : "ui_star_outline"), starX + 1 - fh, y + 12 - fh, 14 + Math.round(fh * 2),
			Render2D.lerpColor(Render2D.lerpColor(0xFF7E8597, 0xFFFFFFFF, fh), Theme.CAT_FAVORITES, fav));
		int resetX = starX - 18;
		float rh = anim("resetHover", Render2D.hovered(mx, my, resetX, y + 11, 16, 16), 16f);
		g.pose().pushMatrix();
		g.pose().translate(resetX + 8, y + 19);
		g.pose().rotate(-rh * (float) Math.PI);
		blitIcon(g, icon("ui_reset"), -6, -6, 12, Render2D.lerpColor(0xFF7E8597, 0xFFFFFFFF, rh));
		g.pose().popMatrix();

		List<String> desc = UiText.wrap(m.getDescription(), w - 24, 2);
		for (int i = 0; i < desc.size(); i++) UiText.text(g, desc.get(i), x + 12, y + 36 + i * 10, Theme.O_TEXT_DIM);
		int extra = (desc.size() - 1) * 10;
		Render2D.fill(g, x + 12, y + 49 + extra, x + w - 12, y + 50 + extra, 0x18FFFFFF);
		settingsList.render(g, x + 10, y + 56 + extra, w - 24, h - 66 - extra, mx, my, dt);
	}

	private void renderProfiles(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
		header(g, x, y, "PROFILES", "Each profile has its own mods, layout and settings");
		String active = ConfigManager.activeProfile();
		List<String> names = ConfigManager.profiles();
		int ry = y + 48;
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			boolean isActive = name.equals(active);
			float stagger = Anim.easeOutCubic(Anim.since(panelChange + i * 25L, 0.25f));
			int rowY = ry + Math.round((1 - stagger) * 8);
			float hover = anim("prof" + name, Render2D.hovered(mx, my, x + 10, rowY, w - 20, 22), 16f);
			float on = anim("profOn" + name, isActive, 12f);
			float base = Render2D.alpha;
			Render2D.alpha = base * stagger;
			Render2D.roundedRectBordered(g, x + 10, rowY, w - 20, 22, 5, Render2D.lerpColor(Theme.O_ROW, Theme.O_ROW_HOVER, hover),
				Render2D.lerpColor(0x00000000, Render2D.withAlpha(Theme.ON, 0.8f), on));
			Shapes.disc(g, x + 20, rowY + 11, 3, Render2D.lerpColor(Theme.OFF, Theme.ON, on));
			if (name.equals(editingProfile)) {
				String shown = font.plainSubstrByWidth(editBuffer, w - 80, true);
				UiText.text(g, shown, x + 30, rowY + 7, 0xFFFFFFFF);
				UiText.text(g, "_", x + 30 + font.width(shown), rowY + 7, UiText.caretColor());
			} else {
				UiText.fit(g, name, x + 30, rowY + 7, w - 90, isActive ? 0xFFFFFFFF : Theme.O_TEXT);
				if (!name.equals(ConfigManager.DEFAULT_PROFILE) && hover > 0.01f) {
					Render2D.alpha = base * stagger * hover;
					blitIcon(g, icon("ui_edit"), x + w - 44, rowY + 5, 12, Render2D.hovered(mx, my, x + w - 46, rowY + 3, 16, 16) ? 0xFFFFFFFF : Theme.O_TEXT_DIM);
					blitIcon(g, icon("ui_close"), x + w - 26, rowY + 5, 12, Render2D.hovered(mx, my, x + w - 28, rowY + 3, 16, 16) ? Theme.DISABLED : Theme.O_TEXT_DIM);
				}
			}
			Render2D.alpha = base;
			ry += 26;
		}
		int by = y + h - 28;
		float bh = anim("newProfile", Render2D.hovered(mx, my, x + 10, by, w - 20, 18), 16f);
		Render2D.roundedRect(g, x + 10, by, w - 20, 18, 9, Render2D.lerpColor(0xFF3B6FD8, 0xFF5A8CF0, bh));
		UiText.spacedSmall(g, "+ NEW PROFILE FROM CURRENT", x + w / 2f, by + 6, 0xFFFFFFFF, true, 0.7f);
	}

	/** A waypoint's marker as it looks in game: dark disc, colored ring and icon. */
	private void waypointMarker(GuiGraphics g, Waypoint wp, float cx, float cy, float r) {
		int color = wp.currentColor();
		Shapes.ring(g, cx, cy, r, r + 2.5f, Render2D.withAlpha(color, 0.22f));
		Shapes.disc(g, cx, cy, r, 0xF0101219);
		Shapes.ring(g, cx, cy, r - 1.3f, r, color);
		Icons.drawCentered(g, wp.iconTexture(), cx, cy, r * 1.15f, color);
	}

	private void renderWaypoints(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
		if (wpEditor != null && !WaypointManager.inWorld().contains(wpEditor.waypoint)) wpEditor = null;
		if (wpEditor != null) {
			renderWaypointEditor(g, mx, my, x, y, w, h);
			return;
		}
		String world = WaypointManager.currentWorld();
		header(g, x, y, "WAYPOINTS", world == null ? "Join a world to manage waypoints" : world.substring(world.indexOf(':') + 1));
		if (world == null) return;
		int bw = 52, bx = x + w - 12 - bw;
		float ah = anim("addWp", Render2D.hovered(mx, my, bx, y + 10, bw, 14), 16f);
		Render2D.roundedRect(g, bx, y + 10, bw, 14, 7, Render2D.lerpColor(0xFF3B6FD8, 0xFF5A8CF0, ah));
		UiText.spacedSmall(g, "+ ADD", bx + bw / 2f, y + 14, 0xFFFFFFFF, true, 0.7f);
		renderWorldBar(g, mx, my, x, y + 44, w);

		List<Waypoint> list = WaypointManager.inWorld();
		if (list.isEmpty()) {
			UiText.centered(g, "No waypoints yet.", x + w / 2, y + h / 2 - 6, Theme.O_TEXT_DIM);
			UiText.centered(g, "Press + ADD to mark where you stand.", x + w / 2, y + h / 2 + 6, Theme.O_TEXT_FAINT);
		}
		listScroll = Anim.approach(listScroll, listScrollTarget, 18f, dt);
		int top = y + 68, bottom = y + h - 10, rowH = 28;
		float maxScroll = Math.max(0, list.size() * (rowH + 4) - (bottom - top));
		listScrollTarget = Math.max(0, Math.min(maxScroll, listScrollTarget));
		g.enableScissor(x + 8, top, x + w - 8, bottom);
		for (int i = 0; i < list.size(); i++) {
			Waypoint wp = list.get(i);
			int ry = top + i * (rowH + 4) - Math.round(listScroll);
			if (ry + rowH < top || ry > bottom) continue;
			String id = String.valueOf(System.identityHashCode(wp));
			float hover = anim("wp" + id, Render2D.hovered(mx, my, x + 10, ry, w - 20, rowH), 16f);
			float vis = anim("wpVis" + id, wp.visible, 12f);
			float stagger = Anim.easeOutCubic(Anim.since(panelChange + i * 25L, 0.25f));
			float base = Render2D.alpha;
			Render2D.alpha = base * stagger;
			int rowY = ry + Math.round((1 - stagger) * 8);
			Render2D.roundedRect(g, x + 10, rowY, w - 20, rowH, 6, Render2D.lerpColor(Theme.O_ROW, Theme.O_ROW_HOVER, hover));
			Render2D.roundedRect(g, x + 10, rowY + 5, 2, rowH - 10, 1, Render2D.withAlpha(wp.currentColor(), 0.4f + 0.6f * vis));
			float markerAlpha = Render2D.alpha;
			Render2D.alpha = markerAlpha * (0.45f + 0.55f * vis);
			waypointMarker(g, wp, x + 26, rowY + rowH / 2f, 8);
			Render2D.alpha = markerAlpha;
			UiText.fit(g, wp.name, x + 40, rowY + 5, w - 110, Render2D.lerpColor(Theme.O_TEXT_FAINT, Theme.O_TEXT, vis));
			String info = wp.x + ", " + wp.y + ", " + wp.z;
			if (minecraft.player != null && wp.dimension.equals(WaypointManager.currentDimension())) {
				info += "  •  " + Math.round(Math.sqrt(minecraft.player.distanceToSqr(wp.x + 0.5, wp.y, wp.z + 0.5))) + "m";
			}
			float cs = 0.7f;
			g.pose().pushMatrix();
			g.pose().translate(x + 40, rowY + 17);
			g.pose().scale(cs, cs);
			UiText.text(g, info, 0, 0, Theme.O_TEXT_DIM);
			g.pose().popMatrix();
			boolean eyeHover = Render2D.hovered(mx, my, x + w - 50, rowY + 6, 16, 16);
			blitIcon(g, icon("freelook"), x + w - 48, rowY + 8, 12, Render2D.lerpColor(Theme.O_TEXT_FAINT, eyeHover ? 0xFFFFFFFF : Theme.O_TEXT, vis));
			UiText.text(g, ">", x + w - 26 + Math.round(hover * 2), rowY + 10, Render2D.lerpColor(Theme.O_TEXT_FAINT, 0xFFFFFFFF, hover));
			Render2D.alpha = base;
		}
		g.disableScissor();
	}

	/** The world inside this server: arrows switch, clicking the name renames, + adds one, the trash removes it. */
	private void renderWorldBar(GuiGraphics g, int mx, int my, int x, int y, int w) {
		WaypointManager.SubWorld sub = WaypointManager.currentSubWorld();
		if (sub == null) {
			Render2D.roundedRect(g, x + 10, y, w - 20, 18, 5, Theme.O_ROW);
			UiText.text(g, "Detecting world...", x + 18, y + 5, Theme.O_TEXT_DIM);
			return;
		}
		List<WaypointManager.SubWorld> worlds = WaypointManager.worldsOf(sub.server);
		int index = worlds.indexOf(sub);
		Render2D.roundedRect(g, x + 10, y, w - 20, 18, 5, Theme.O_ROW);
		boolean many = worlds.size() > 1;
		float lh = anim("wLeft", many && Render2D.hovered(mx, my, x + 12, y + 1, 16, 16), 16f);
		float rh = anim("wRight", many && Render2D.hovered(mx, my, x + w - 74, y + 1, 16, 16), 16f);
		int arrow = many ? Theme.O_TEXT : Theme.O_TEXT_FAINT;
		UiText.text(g, "<", x + 17 - Math.round(lh), y + 5, Render2D.lerpColor(arrow, 0xFFFFFFFF, lh));
		UiText.text(g, ">", x + w - 69 + Math.round(rh), y + 5, Render2D.lerpColor(arrow, 0xFFFFFFFF, rh));
		int nameW = w - 108;
		if (editingSubWorld != null) {
			String shown = font.plainSubstrByWidth(editBuffer, nameW - 6, true);
			Render2D.roundedRect(g, x + 30, y + 2, nameW, 14, 3, 0xFF15171D);
			UiText.text(g, shown, x + 34, y + 5, 0xFFFFFFFF);
			if ((System.currentTimeMillis() / 500) % 2 == 0) Render2D.fill(g, x + 35 + font.width(shown), y + 4, x + 36 + font.width(shown), y + 14, 0xFFFFFFFF);
		} else {
			boolean over = Render2D.hovered(mx, my, x + 30, y + 1, nameW, 16);
			String count = (index + 1) + "/" + worlds.size() + "  •  " + WaypointManager.countIn(sub);
			int countW = font.width(count);
			UiText.fit(g, sub.name, x + 34, y + 5, nameW - countW - 14, over ? 0xFFFFFFFF : Theme.O_TEXT);
			UiText.text(g, count, x + 26 + nameW - countW, y + 5, Theme.O_TEXT_FAINT);
		}
		float ph = anim("wNew", Render2D.hovered(mx, my, x + w - 54, y + 1, 16, 16), 16f);
		UiText.text(g, "+", x + w - 49, y + 5, Render2D.lerpColor(Theme.O_TEXT_DIM, 0xFFFFFFFF, ph));
		float dh = anim("wDel", many && Render2D.hovered(mx, my, x + w - 34, y + 1, 16, 16), 16f);
		blitIcon(g, icon("ui_trash"), x + w - 32, y + 3, 12, many ? Render2D.lerpColor(Theme.O_TEXT_DIM, Theme.DISABLED, dh) : Theme.O_TEXT_FAINT);
	}

	/** Clicks on the world bar; returns true if it handled the click. */
	private boolean clickWorldBar(double mx, double my, int x, int y, int w) {
		WaypointManager.SubWorld sub = WaypointManager.currentSubWorld();
		if (sub == null || !Render2D.hovered(mx, my, x + 10, y, w - 20, 18)) return false;
		List<WaypointManager.SubWorld> worlds = WaypointManager.worldsOf(sub.server);
		int index = worlds.indexOf(sub);
		if (Render2D.hovered(mx, my, x + 12, y + 1, 16, 16)) {
			WaypointManager.selectSubWorld(worlds.get(Math.floorMod(index - 1, worlds.size())));
		} else if (Render2D.hovered(mx, my, x + w - 74, y + 1, 16, 16)) {
			WaypointManager.selectSubWorld(worlds.get(Math.floorMod(index + 1, worlds.size())));
		} else if (Render2D.hovered(mx, my, x + w - 54, y + 1, 16, 16)) {
			WaypointManager.SubWorld created = WaypointManager.newSubWorld();
			if (created != null) {
				editingSubWorld = created;
				editBuffer = created.name;
			}
		} else if (Render2D.hovered(mx, my, x + w - 34, y + 1, 16, 16)) {
			WaypointManager.removeSubWorld(sub);
		} else {
			editingSubWorld = sub;
			editBuffer = sub.name;
		}
		panelChange = System.currentTimeMillis();
		return true;
	}

	private void renderWaypointEditor(GuiGraphics g, int mx, int my, int x, int y, int w, int h) {
		WaypointEditor ed = wpEditor;
		ed.apply();
		Waypoint wp = ed.waypoint;
		float back = anim("wpBack", Render2D.hovered(mx, my, x + 10, y + 11, 14, 14), 16f);
		Render2D.roundedRectBordered(g, x + 10, y + 11, 14, 14, 3, Render2D.lerpColor(0xFF1A1B20, 0xFF30333E, back), 0xFF3A3D48);
		UiText.text(g, "<", x + 15 - Math.round(back), y + 14, 0xFFFFFFFF);
		UiText.spaced(g, "EDIT WAYPOINT", x + 30, y + 12, 0xFFFFFFFF, true);
		String where = wp.x + ", " + wp.y + ", " + wp.z + "  •  " + dimensionName(wp.dimension);
		UiText.fit(g, where, x + 30, y + 25, w - 70, Theme.O_TEXT_DIM);
		boolean delHover = Render2D.hovered(mx, my, x + w - 28, y + 11, 16, 16);
		float dh = anim("wpDel", delHover, 16f);
		blitIcon(g, icon("ui_trash"), x + w - 26, y + 13, 12, Render2D.lerpColor(Theme.O_TEXT_DIM, Theme.DISABLED, dh));
		Render2D.fill(g, x + 12, y + 39, x + w - 12, y + 40, 0x18FFFFFF);

		// Big preview of the marker, and quick color swatches next to it.
		float pulse = 1 + 0.04f * (float) Math.sin(System.currentTimeMillis() / 300.0);
		waypointMarker(g, wp, x + 28, y + 62, 13 * pulse);
		int sx = x + 52, sy = y + 50, sw = 14;
		UiText.spacedSmall(g, "QUICK COLORS", sx + UiText.spacedWidth("QUICK COLORS", true) * 0.3f, sy - 1, Theme.O_TEXT_DIM, true, 0.6f);
		for (int i = 0; i < WaypointEditor.PALETTE.length; i++) {
			int cx = sx + i * (sw + 4), cy = sy + 8;
			if (cx + sw > x + w - 10) break;
			boolean over = Render2D.hovered(mx, my, cx, cy, sw, sw);
			float hv = anim("sw" + i, over, 16f);
			boolean current = !wp.chroma && (wp.color | 0xFF000000) == WaypointEditor.PALETTE[i];
			int grow = Math.round(hv);
			Render2D.roundedRectBordered(g, cx - grow, cy - grow, sw + grow * 2, sw + grow * 2, 4, WaypointEditor.PALETTE[i],
				current ? 0xFFFFFFFF : Render2D.lerpColor(0x40FFFFFF, 0xC0FFFFFF, hv));
		}
		settingsList.render(g, x + 10, y + 86, w - 24, h - 96, mx, my, dt);
	}

	private static String dimensionName(String id) {
		return switch (id) {
			case "minecraft:overworld" -> "Overworld";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "End";
			default -> id.substring(id.indexOf(':') + 1);
		};
	}
	// ================================================================ input

	private void commitNames() {
		if (editingProfile != null) ConfigManager.renameProfile(editingProfile, editBuffer);
		if (editingSubWorld != null) WaypointManager.rename(editingSubWorld, editBuffer);
		editingProfile = null;
		editingSubWorld = null;
		editBuffer = "";
	}

	private boolean editingName() {
		return editingProfile != null || editingSubWorld != null;
	}

	static boolean isMenuMouseButton(int button) {
		var key = KeyBindingHelper.getBoundKeyOf(OrbitClient.menuKey);
		return button != GLFW.GLFW_MOUSE_BUTTON_LEFT
			&& key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE && key.getValue() == button;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (closing) return true;
		double mx = event.x(), my = event.y();
		int button = event.button();
		if (settingsList.isCapturing() && (panel == Panel.MOD || panel == Panel.SETTINGS || wpEditor != null)) {
			settingsList.mouseClicked(mx, my, button);
			return true;
		}
		if (editingName()) commitNames();
		if (isMenuMouseButton(button)) {
			onClose();
			return true;
		}

		for (int i = 0; i < DOCK.length; i++) {
			if (!Render2D.hovered(mx, my, dockX, dockY + i * 28, 22, 22)) continue;
			DockItem item = DOCK[i];
			if (item.panel != null) openPanel(item.panel);
			else if (item.icon.equals("ui_hud")) closeThen(() -> minecraft.setScreen(new HudEditorScreen()));
			else if (item.icon.equals("ui_wardrobe")) closeThen(() -> minecraft.setScreen(new WardrobeScreen()));
			else onClose();
			return true;
		}

		if (Render2D.hovered(mx, my, panelX, panelY, panelW, panelH)) return clickPanel(mx, my, button);

		if (selected != null && overCore(mx, my)) {
			toggle(selected);
			return true;
		}
		for (Object[] hit : nodeHits) {
			Module m = (Module) hit[0];
			if (Math.hypot(mx - (float) hit[1], my - (float) hit[2]) > nodeR + 3) continue;
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				toggle(m);
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
				m.favorite = !m.favorite;
				ripples.put(m, System.currentTimeMillis());
			} else if (m == selected) {
				toggle(m);
			} else {
				int from = lastList.indexOf(selected), to = lastList.indexOf(m);
				select(m, Integer.compare(to, from));
				openPanel(Panel.MOD);
			}
			return true;
		}
		for (int c = 0; c < 4; c++) {
			if (overArc(mx, my, c)) {
				search = "";
				category = c;
				openPanel(Panel.MOD);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean clickPanel(double mx, double my, int button) {
		int x = panelX, y = panelY, w = panelW, h = panelH;
		switch (panel) {
			case MOD -> {
				if (selected == null) return true;
				int sx = x + w - 12 - 30, starX = sx - 20, resetX = starX - 18;
				if (Render2D.hovered(mx, my, sx, y + 12, 30, 14)) toggle(selected);
				else if (Render2D.hovered(mx, my, starX, y + 11, 16, 16)) selected.favorite = !selected.favorite;
				else if (Render2D.hovered(mx, my, resetX, y + 11, 16, 16)) {
					for (Setting<?> s : selected.getSettings()) s.reset();
					panelChange = System.currentTimeMillis();
					panelDir = 0;
				} else settingsList.mouseClicked(mx, my, button);
			}
			case SETTINGS -> settingsList.mouseClicked(mx, my, button);
			case PROFILES -> clickProfiles(mx, my, x, y, w, h);
			case WAYPOINTS -> clickWaypoints(mx, my, x, y, w, h);
		}
		return true;
	}

	private void clickProfiles(double mx, double my, int x, int y, int w, int h) {
		int ry = y + 48;
		for (String name : ConfigManager.profiles()) {
			if (Render2D.hovered(mx, my, x + 10, ry, w - 20, 22)) {
				boolean editable = !name.equals(ConfigManager.DEFAULT_PROFILE);
				if (editable && Render2D.hovered(mx, my, x + w - 46, ry + 3, 16, 16)) {
					editingProfile = name;
					editBuffer = name;
				} else if (editable && Render2D.hovered(mx, my, x + w - 28, ry + 3, 16, 16)) {
					ConfigManager.deleteProfile(name);
				} else if (!name.equals(ConfigManager.activeProfile())) {
					ConfigManager.switchProfile(name);
					burstStart = System.currentTimeMillis();
				}
				return;
			}
			ry += 26;
		}
		if (Render2D.hovered(mx, my, x + 10, y + h - 28, w - 20, 18)) {
			editingProfile = ConfigManager.createProfile();
			editBuffer = editingProfile;
		}
	}

	private void openWaypoint(Waypoint wp) {
		wpEditor = new WaypointEditor(wp);
		panelChange = System.currentTimeMillis();
		panelDir = 1;
	}

	private void closeWaypoint() {
		settingsList.commit();
		if (wpEditor != null) wpEditor.apply();
		wpEditor = null;
		panelChange = System.currentTimeMillis();
		panelDir = -1;
	}

	private void clickWaypoints(double mx, double my, int x, int y, int w, int h) {
		if (WaypointManager.currentWorld() == null) return;
		if (wpEditor != null) {
			Waypoint wp = wpEditor.waypoint;
			if (Render2D.hovered(mx, my, x + 10, y + 11, 14, 14)) {
				closeWaypoint();
			} else if (Render2D.hovered(mx, my, x + w - 28, y + 11, 16, 16)) {
				WaypointManager.remove(wp);
				wpEditor = null;
				panelChange = System.currentTimeMillis();
				panelDir = -1;
			} else {
				int sx = x + 52, sy = y + 58, sw = 14;
				for (int i = 0; i < WaypointEditor.PALETTE.length; i++) {
					int cx = sx + i * (sw + 4);
					if (cx + sw > x + w - 10) break;
					if (Render2D.hovered(mx, my, cx - 1, sy - 1, sw + 2, sw + 2)) {
						wpEditor.setColor(WaypointEditor.PALETTE[i]);
						return;
					}
				}
				settingsList.mouseClicked(mx, my, 0);
			}
			return;
		}
		if (clickWorldBar(mx, my, x, y + 44, w)) return;
		if (Render2D.hovered(mx, my, x + w - 64, y + 10, 52, 14)) {
			Waypoint wp = WaypointManager.addAtPlayer("Waypoint " + (WaypointManager.inWorld().size() + 1));
			if (wp != null) openWaypoint(wp);
			return;
		}
		List<Waypoint> list = WaypointManager.inWorld();
		int top = y + 68, rowH = 28;
		for (int i = 0; i < list.size(); i++) {
			Waypoint wp = list.get(i);
			int ry = top + i * (rowH + 4) - Math.round(listScroll);
			if (!Render2D.hovered(mx, my, x + 10, ry, w - 20, rowH)) continue;
			if (Render2D.hovered(mx, my, x + w - 50, ry + 6, 16, 16)) wp.visible = !wp.visible;
			else openWaypoint(wp);
			return;
		}
	}
	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (settingsList.mouseDragged(event.x(), event.y())) return true;
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		settingsList.mouseReleased();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		if (Render2D.hovered(mx, my, panelX, panelY, panelW, panelH)) {
			if (panel == Panel.WAYPOINTS && wpEditor == null) listScrollTarget -= (float) vertical * 32;
			else settingsList.mouseScrolled(mx, my, vertical);
			return true;
		}
		step(vertical < 0 ? 1 : -1);
		return true;
	}

	/** Moves the selection around the ring (the ring spins to follow). */
	private void step(int dir) {
		if (lastList.isEmpty()) return;
		int i = Math.max(0, lastList.indexOf(selected));
		select(lastList.get(Math.floorMod(i + dir, lastList.size())), dir);
		openPanel(Panel.MOD);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (closing) return true;
		int key = event.key();
		if (settingsList.isCapturing() && settingsList.keyPressed(key)) return true;
		if (editingName()) {
			switch (key) {
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commitNames();
				case GLFW.GLFW_KEY_ESCAPE -> {
					editingProfile = null;
					editingSubWorld = null;
				}
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
				}
				default -> {
				}
			}
			return true;
		}
		switch (key) {
			case GLFW.GLFW_KEY_ESCAPE -> {
				if (!search.isEmpty()) search = "";
				else if (panel == Panel.WAYPOINTS && wpEditor != null) closeWaypoint();
				else onClose();
				return true;
			}
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_UP -> {
				step(-1);
				return true;
			}
			case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_DOWN -> {
				step(1);
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				if (selected != null) toggle(selected);
				return true;
			}
			case GLFW.GLFW_KEY_TAB -> {
				search = "";
				category = Math.floorMod(category + ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1), 4);
				openPanel(Panel.MOD);
				return true;
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
				return true;
			}
			default -> {
			}
		}
		var menu = KeyBindingHelper.getBoundKeyOf(OrbitClient.menuKey);
		if (menu.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM && key == menu.getValue() && search.isEmpty()) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (closing || !event.isAllowedChatCharacter()) return false;
		String s = event.codepointAsString();
		if (settingsList.charTyped(s)) return true;
		if (editingName()) {
			if (editBuffer.length() < 32) editBuffer += s;
			return true;
		}
		if (s.equals(" ") && search.isEmpty()) {
			if (selected != null) toggle(selected);
			return true;
		}
		if (search.length() < 24) {
			search += s;
			openPanel(Panel.MOD);
		}
		return true;
	}

	/** For tests: open a panel by name (MOD, PROFILES, WAYPOINTS, SETTINGS). */
	public void showPanel(String name) {
		openPanel(Panel.valueOf(name));
	}

	/** For tests: open the editor for one waypoint. */
	public void editWaypoint(Waypoint wp) {
		openPanel(Panel.WAYPOINTS);
		openWaypoint(wp);
	}

	public void setSearch(String query) {
		search = query;
	}
}
