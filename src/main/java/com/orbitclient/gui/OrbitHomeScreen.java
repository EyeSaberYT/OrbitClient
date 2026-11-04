package com.orbitclient.gui;

import com.orbitclient.OrbitClient;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Icons;
import com.orbitclient.util.Render2D;
import com.orbitclient.util.Shapes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Orbit's title screen, in the look of the Orbit menu: Minecraft's panorama under a dark veil and a
 * drifting starfield, the eclipse emblem, two big play buttons, a row of round shortcuts and your
 * account in the corner (click it to switch to another Orbit Launcher account).
 *
 * <p>It is a TitleScreen, so the game and other mods still recognize it as the title screen, but it
 * never calls TitleScreen's init or render: nothing of the vanilla screen (logo, splash, buttons, or
 * buttons other mods add to it) gets drawn over it. Everything is drawn and hit-tested here, from one
 * layout computed each frame.</p>
 */
public final class OrbitHomeScreen extends TitleScreen {
	private static final float TAU = (float) (Math.PI * 2);
	private static final String COPYRIGHT = "Copyright Mojang AB. Do not distribute!";

	private record Shortcut(String icon, String label, int hover, Runnable action) {
	}

	private record Hit(int x, int y, int w, int h, Runnable action) {
	}

	private final Anim.Clock clock = new Anim.Clock();
	private final Map<String, Float> anims = new HashMap<>();
	private final List<Hit> hits = new ArrayList<>();
	private final float[][] stars = new float[90][4];
	private final List<Shortcut> shortcuts = new ArrayList<>();
	private Supplier<PlayerSkin> skin;
	private float dt, openProgress;
	private String hoveredLabel;

	public OrbitHomeScreen() {
		super();
		Random random = new Random(7);
		for (float[] s : stars) {
			s[0] = random.nextFloat();
			s[1] = random.nextFloat();
			s[2] = 0.3f + random.nextFloat() * 0.7f;
			s[3] = random.nextFloat() * TAU;
		}
	}

	/** Deliberately doesn't call TitleScreen.init (see the class comment). */
	@Override
	protected void init() {
		skin = minecraft.getSkinManager().createLookup(minecraft.getGameProfile(), false);
		shortcuts.clear();
		shortcuts.add(new Shortcut("ui_mods", "Orbit Mods", Theme.CAT_HUD, () -> minecraft.setScreen(new OrbitScreen())));
		shortcuts.add(new Shortcut("ui_wardrobe", "Wardrobe", Theme.CAT_SERVER, () -> minecraft.setScreen(new WardrobeScreen())));
		if (FabricLoader.getInstance().isModLoaded("modmenu")) {
			shortcuts.add(new Shortcut("ui_import", "Installed Mods", Theme.CAT_MECHANIC, this::openModMenu));
		}
		shortcuts.add(new Shortcut("ui_settings", "Options", Theme.CAT_FAVORITES, () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
		shortcuts.add(new Shortcut("ui_power", "Quit Game", Theme.DISABLED, minecraft::stop));
	}

	/** Mod Menu's list, if it's installed (it's optional, so no compile-time dependency). */
	private void openModMenu() {
		try {
			Class<?> screen = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
			minecraft.setScreen((Screen) screen.getConstructor(Screen.class).newInstance(this));
		} catch (ReflectiveOperationException | LinkageError e) {
			OrbitClient.LOGGER.warn("Couldn't open Mod Menu", e);
		}
	}

	private void singleplayer() {
		minecraft.setScreen(new SelectWorldScreen(this));
	}

	private void multiplayer() {
		minecraft.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	// ================================================================ rendering

	/** Panorama under a dark veil; no blur (the frame's single blur stays free for other screens). */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		renderPanorama(g, delta);
		g.fillGradient(0, 0, width, height, 0x70070910, 0xC8040508);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		// No super.render: TitleScreen's draws the vanilla menu. This screen has no widgets of its own.
		dt = clock.tick();
		openProgress = Anim.step(openProgress, true, 0.6f, dt);
		float eased = Anim.easeOutCubic(openProgress);
		hits.clear();
		hoveredLabel = null;

		Render2D.alpha = eased;
		try {
			renderStars(g, eased);
			int slide = Math.round((1 - eased) * 14);
			int emblemY = Math.max(52, height / 2 - 92) + slide;
			renderEmblem(g, width / 2, emblemY);
			int top = emblemY + 58;
			renderPlayButtons(g, mx, my, top);
			renderShortcuts(g, mx, my, top + 70);
			renderAccount(g, mx, my);
			renderFooter(g, mx, my);
			renderTooltip(g, mx, my);
		} finally {
			Render2D.alpha = 1f;
		}
	}

	private float anim(String key, boolean target, float speed) {
		float v = Anim.approach(anims.getOrDefault(key, target ? 1f : 0f), target ? 1f : 0f, speed, dt);
		anims.put(key, v);
		return v;
	}

	private boolean hit(int mx, int my, int x, int y, int w, int h, Runnable action) {
		hits.add(new Hit(x, y, w, h, action));
		return Render2D.hovered(mx, my, x, y, w, h);
	}

	private void renderStars(GuiGraphics g, float eased) {
		double t = System.currentTimeMillis() / 1000.0;
		int n = stars.length;
		float[] xs = new float[n], ys = new float[n];
		int[] sizes = new int[n], colors = new int[n];
		for (int i = 0; i < n; i++) {
			float[] s = stars[i];
			xs[i] = (float) (((s[0] * width + t * s[2] * 2.0) % width + width) % width);
			ys[i] = s[1] * height;
			sizes[i] = s[2] > 0.85f ? 2 : 1;
			float twinkle = 0.4f + 0.35f * (float) Math.sin(t * 1.3 + s[3]);
			colors[i] = Render2D.withAlpha(0xFFFFFFFF, twinkle * s[2] * eased);
		}
		Shapes.dots(g, xs, ys, sizes, colors);
	}

	/** The eclipse from the Orbit menu, with the wordmark under it. */
	private void renderEmblem(GuiGraphics g, int cx, int cy) {
		double t = System.currentTimeMillis() / 1000.0;
		float r = 15;
		for (int k = 0; k < 6; k++) {
			Shapes.ring(g, cx, cy, r + k * 2.2f, r + k * 2.2f + 2.2f, Render2D.withAlpha(Theme.CORONA, 0.16f * (1 - k / 6f)));
		}
		for (int i = 0; i < 16; i++) {
			float a0 = i * TAU / 16 + (float) ((t * 0.3) % TAU);
			float flare = 0.5f + 0.5f * (float) Math.sin(t * 2.0 + i * 1.7);
			float length = Math.round(flare * 6f) / 2f;
			Shapes.arc(g, cx, cy, r + 0.5f, r + 2.5f + length, a0, a0 + TAU / 16 * 0.55f, Render2D.withAlpha(Theme.CORONA, 0.35f + 0.4f * flare));
		}
		Shapes.disc(g, cx, cy, r, 0xFF06070A);
		Shapes.ring(g, cx, cy, r - 1.2f, r, Render2D.withAlpha(Theme.CORONA, 0.85f));

		g.pose().pushMatrix();
		g.pose().translate(cx, cy + r + 12);
		g.pose().scale(2f, 2f);
		int w = UiText.spacedWidth("ORBIT", true);
		UiText.spaced(g, "ORBIT", -w / 2, 0, 0xFFFFFFFF, true);
		g.pose().popMatrix();
		UiText.spacedSmall(g, "CLIENT", cx, cy + r + 33, Theme.O_TEXT_DIM, true, 0.7f);
	}

	private void renderPlayButtons(GuiGraphics g, int mx, int my, int y) {
		int w = Math.min(220, width - 40), h = 26, x = width / 2 - w / 2;
		bigButton(g, mx, my, "SINGLEPLAYER", x, y, w, h, Theme.CAT_HUD, this::singleplayer);
		bigButton(g, mx, my, "MULTIPLAYER", x, y + h + 6, w, h, Theme.CAT_SERVER, this::multiplayer);
	}

	private void bigButton(GuiGraphics g, int mx, int my, String label, int x, int y, int w, int h, int accent, Runnable action) {
		float hov = anim("btn" + label, hit(mx, my, x, y, w, h, action), 16f);
		int lift = Math.round(hov);
		int fill = Render2D.lerpColor(Theme.GLASS, 0xF01A1F2C, hov);
		int border = Render2D.lerpColor(Theme.GLASS_BORDER, accent, hov);
		Render2D.roundedRectBordered(g, x, y - lift, w, h, 8, fill, border);
		// Accent bar that grows from the left edge on hover.
		int bar = Math.round(3 + hov * 3);
		Render2D.roundedRect(g, x + 4, y - lift + 6, bar, h - 12, 1, Render2D.withAlpha(accent, 0.55f + 0.45f * hov));
		int tw = UiText.spacedWidth(label, true);
		UiText.spaced(g, label, x + w / 2 - tw / 2, y - lift + (h - 8) / 2, Render2D.lerpColor(Theme.O_TEXT, 0xFFFFFFFF, hov), true);
	}

	private void renderShortcuts(GuiGraphics g, int mx, int my, int y) {
		int size = 24, gap = 12, n = shortcuts.size();
		int x0 = width / 2 - (n * size + (n - 1) * gap) / 2;
		for (int i = 0; i < n; i++) {
			Shortcut s = shortcuts.get(i);
			int x = x0 + i * (size + gap);
			boolean over = hit(mx, my, x, y, size, size, s.action);
			float hov = anim("sc" + s.label, over, 16f);
			if (over) hoveredLabel = s.label;
			float cx = x + size / 2f, cy = y + size / 2f, r = size / 2f + hov;
			Shapes.disc(g, cx, cy, r, Render2D.lerpColor(0xD0141720, 0xFF232838, hov));
			Shapes.ring(g, cx, cy, r - 1.2f, r, Render2D.lerpColor(0x30FFFFFF, s.hover, hov));
			Icons.drawCentered(g, s.icon, cx, cy, 12, Render2D.lerpColor(0xFF9AA0B0, 0xFFFFFFFF, hov));
		}
	}

	/** Your account (head, name) in the top right; click to switch accounts. */
	private void renderAccount(GuiGraphics g, int mx, int my) {
		String name = minecraft.getUser().getName();
		int h = 22, w = 22 + font.width(name) + 8 + font.width("SWITCH") + 12;
		int x = width - w - 10, y = 10;
		float hov = anim("account", hit(mx, my, x, y, w, h, () -> minecraft.setScreen(new LauncherAccountsScreen(this))), 16f);
		Render2D.roundedRectBordered(g, x, y, w, h, 11, Render2D.lerpColor(Theme.GLASS, 0xF01A1F2C, hov),
			Render2D.lerpColor(Theme.GLASS_BORDER, Theme.CAT_HUD, hov));
		PlayerFaceRenderer.draw(g, skin.get(), x + 5, y + 5, 12);
		UiText.text(g, name, x + 22, y + 7, 0xFFFFFFFF);
		UiText.text(g, "SWITCH", x + w - 8 - font.width("SWITCH"), y + 7, Render2D.lerpColor(Theme.O_TEXT_FAINT, Theme.CAT_HUD, hov));
	}

	private void renderFooter(GuiGraphics g, int mx, int my) {
		String version = FabricLoader.getInstance().getModContainer(OrbitClient.MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
		UiText.text(g, "Orbit Client " + version + "  •  Minecraft " + SharedConstants.getCurrentVersion().name(), 8, height - 14, Theme.O_TEXT_FAINT);
		int cw = font.width(COPYRIGHT), cx = width - cw - 8, cy = height - 14;
		boolean over = hit(mx, my, cx, cy - 2, cw, 12, () -> minecraft.setScreen(new CreditsAndAttributionScreen(this)));
		UiText.text(g, COPYRIGHT, cx, cy, over ? 0xFFFFFFFF : Theme.O_TEXT_FAINT);
		if (over) Render2D.fill(g, cx, cy + 9, cx + cw, cy + 10, 0xFFFFFFFF);
	}

	private void renderTooltip(GuiGraphics g, int mx, int my) {
		if (hoveredLabel == null) return;
		int w = font.width(hoveredLabel) + 10;
		int x = Math.max(4, Math.min(width - w - 4, mx - w / 2)), y = my - 22;
		Render2D.roundedRect(g, x, y, w, 14, 7, Theme.GLASS);
		UiText.text(g, hoveredLabel, x + 5, y + 3, 0xFFFFFFFF);
	}

	// ================================================================ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (Hit h : hits) {
				if (Render2D.hovered(event.x(), event.y(), h.x, h.y, h.w, h.h)) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
					h.action.run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
