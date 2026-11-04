package com.orbitclient.gui;

import com.orbitclient.OrbitClient;
import com.orbitclient.account.LauncherAccounts;
import com.orbitclient.social.Friends;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Icons;
import com.orbitclient.util.Render2D;
import com.orbitclient.util.Shapes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
	private float dt, openProgress;
	private String hoveredLabel;


	// Account popup
	private boolean popupOpen;
	private List<LauncherAccounts.Account> accounts = List.of();
	private String accountError;
	private LauncherAccounts.Account switching;
	private String switchStatus;
	private int switchStatusColor;
	private final List<Hit> popupHits = new ArrayList<>();
	private int chipX, chipY, chipW, chipH, popupX, popupY, popupW, popupH;

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
			// While the account popup is open, the rest of the screen doesn't react to the mouse.
			int bx = popupOpen ? -10000 : mx, by = popupOpen ? -10000 : my;
			renderPlayButtons(g, bx, by, top);
			renderShortcuts(g, bx, by, top + 70);
			renderFooter(g, bx, by);
			renderTooltip(g, bx, by);
			renderAccount(g, mx, my);
			renderFriendsChip(g, bx, by);
			renderAccountPopup(g, mx, my);
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

	/** Your account (head, name) in the top right; click it for the account popup. */
	private void renderAccount(GuiGraphics g, int mx, int my) {
		String name = minecraft.getUser().getName();
		chipH = 22;
		chipW = 22 + font.width(name) + 8 + font.width("SWITCH") + 12;
		chipX = width - chipW - 10;
		chipY = 10;
		boolean over = Render2D.hovered(mx, my, chipX, chipY, chipW, chipH);
		float hov = anim("account", over || popupOpen, 16f);
		Render2D.roundedRectBordered(g, chipX, chipY, chipW, chipH, 11, Render2D.lerpColor(Theme.GLASS, 0xF01A1F2C, hov),
			Render2D.lerpColor(Theme.GLASS_BORDER, Theme.CAT_HUD, hov));
		Heads.draw(g, minecraft.getUser().getProfileId(), name, chipX + 5, chipY + 5, 12, Render2D.a(0xFFFFFFFF));
		UiText.text(g, name, chipX + 22, chipY + 7, 0xFFFFFFFF);
		UiText.text(g, "SWITCH", chipX + chipW - 8 - font.width("SWITCH"), chipY + 7, Render2D.lerpColor(Theme.O_TEXT_FAINT, Theme.CAT_HUD, hov));
	}

	/** Friends, left of the account chip: how many are online, and a red count of unread messages and requests. */
	private void renderFriendsChip(GuiGraphics g, int mx, int my) {
		long online = Friends.onlineCount();
		String label = "FRIENDS", count = online > 0 ? online + " online" : "";
		int h = chipH, w = 18 + font.width(label) + (count.isEmpty() ? 0 : 6 + font.width(count)) + 10;
		int x = chipX - w - 6, y = chipY;
		float hov = anim("friends", hit(mx, my, x, y, w, h, () -> minecraft.setScreen(new FriendsScreen())), 16f);
		Render2D.roundedRectBordered(g, x, y, w, h, 11, Render2D.lerpColor(Theme.GLASS, 0xF01A1F2C, hov),
			Render2D.lerpColor(Theme.GLASS_BORDER, Theme.ON, hov));
		Shapes.disc(g, x + 10, y + h / 2f, 3, online > 0 ? Theme.ON : Theme.OFF);
		UiText.text(g, label, x + 18, y + 7, 0xFFFFFFFF);
		if (!count.isEmpty()) UiText.text(g, count, x + 24 + font.width(label), y + 7, Theme.ON);
		int alerts = Friends.totalUnread() + Friends.incoming().size();
		if (alerts > 0) {
			String n = alerts > 9 ? "9+" : String.valueOf(alerts);
			float r = Math.max(5, font.width(n) / 2f + 3);
			Shapes.disc(g, x + w - 2, y + 2, r, Theme.DISABLED);
			UiText.text(g, n, Math.round(x + w - 2 - font.width(n) / 2f), y - 2, 0xFFFFFFFF);
		}
	}

	// ---------------------------------------------------------------- account popup

	private void togglePopup() {
		popupOpen = !popupOpen;
		if (!popupOpen) return;
		accountError = null;
		try {
			accounts = LauncherAccounts.load();
			if (accounts.isEmpty()) accountError = "No accounts in Orbit Launcher yet";
		} catch (Exception e) {
			accounts = List.of();
			accountError = "Couldn't read Orbit Launcher's accounts";
		}
	}

	private void switchTo(LauncherAccounts.Account account) {
		if (switching != null) return;
		switching = account;
		switchStatus = "Signing in as " + account.name() + "...";
		switchStatusColor = Theme.O_TEXT_DIM;
		LauncherAccounts.switchTo(account).whenComplete((ignored, error) -> minecraft.execute(() -> {
			switching = null;
			if (error != null) {
				OrbitClient.LOGGER.warn("Couldn't switch to account {}: {}", account.name(), rootMessage(error));
				switchStatus = "Couldn't sign in. Sign in again in Orbit Launcher.";
				switchStatusColor = Theme.DISABLED;
			} else {
				switchStatus = "Signed in as " + account.name();
				switchStatusColor = Theme.ON;
			}
		}));
	}

	private static String rootMessage(Throwable t) {
		while (t.getCause() != null) t = t.getCause();
		return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
	}

	/** The account list under the chip, like Lunar's and Feather's: click an account to switch to it. */
	private void renderAccountPopup(GuiGraphics g, int mx, int my) {
		popupHits.clear();
		float p = anim("popup", popupOpen, 14f);
		if (p < 0.01f) return;
		float base = Render2D.alpha;
		Render2D.alpha = base * Anim.easeOutCubic(p);
		try {
			int rowH = 24, rows = Math.max(1, accounts.size());
			popupW = Math.max(190, chipW);
			popupX = width - popupW - 10;
			popupY = chipY + chipH + 6 - Math.round((1 - p) * 6);
			popupH = 22 + rows * rowH + 4 + (switchStatus != null ? 14 : 0) + 16;
			Render2D.roundedRectBordered(g, popupX, popupY, popupW, popupH, 8, 0xF40E1017, Theme.GLASS_BORDER);
			UiText.spacedSmall(g, "ACCOUNTS", popupX + popupW / 2f, popupY + 8, Theme.O_TEXT_DIM, true, 0.6f);
			Render2D.fill(g, popupX + 8, popupY + 19, popupX + popupW - 8, popupY + 20, 0x20FFFFFF);

			int y = popupY + 22;
			if (accountError != null) {
				UiText.centered(g, accountError, popupX + popupW / 2, y + 8, Theme.O_TEXT_FAINT);
			}
			UUID current = minecraft.getUser().getProfileId();
			for (LauncherAccounts.Account a : accounts) {
				int rx = popupX + 4, rw = popupW - 8;
				boolean active = a.uuid().equals(current);
				boolean busy = a == switching;
				boolean clickable = popupOpen && !active && switching == null;
				boolean over = clickable && Render2D.hovered(mx, my, rx, y, rw, rowH - 2);
				if (clickable) popupHits.add(new Hit(rx, y, rw, rowH - 2, () -> switchTo(a)));
				float hov = anim("acc" + a.uuid(), over || busy, 18f);
				if (hov > 0.01f) Render2D.roundedRect(g, rx, y, rw, rowH - 2, 6, Render2D.withAlpha(Theme.O_ROW_HOVER, hov));
				if (active) Render2D.roundedRect(g, rx + 2, y + 6, 2, rowH - 14, 1, Theme.ON);
				Heads.draw(g, a.uuid(), a.name(), rx + 8, y + 3, 16, Render2D.a(0xFFFFFFFF));
				UiText.fit(g, a.name(), rx + 30, y + 7, rw - 30 - 52, active ? 0xFFFFFFFF : Theme.O_TEXT);
				String tag = active ? "ACTIVE" : busy ? ".".repeat(1 + (int) (System.currentTimeMillis() / 300 % 3)) : "SWITCH";
				int tagColor = active ? Theme.ON : busy ? Theme.CAT_HUD : Render2D.withAlpha(Theme.CAT_HUD, hov);
				UiText.text(g, tag, rx + rw - 8 - font.width(tag), y + 7, tagColor);
				y += rowH;
			}
			if (accounts.isEmpty()) y += rowH;
			y += 4;
			if (switchStatus != null) {
				UiText.fit(g, switchStatus, popupX + 10, y, popupW - 20, switchStatusColor);
				y += 14;
			}
			UiText.centered(g, "Add accounts in Orbit Launcher", popupX + popupW / 2, y + 1, Theme.O_TEXT_FAINT);
		} finally {
			Render2D.alpha = base;
		}
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
		double mx = event.x(), my = event.y();
		if (event.button() != 0) return super.mouseClicked(event, doubleClick);
		if (Render2D.hovered(mx, my, chipX, chipY, chipW, chipH)) {
			click();
			togglePopup();
			return true;
		}
		if (popupOpen) {
			// The popup is modal: a click outside it only closes it.
			for (Hit h : popupHits) {
				if (Render2D.hovered(mx, my, h.x, h.y, h.w, h.h)) {
					click();
					h.action.run();
					return true;
				}
			}
			if (!Render2D.hovered(mx, my, popupX, popupY, popupW, popupH)) popupOpen = false;
			return true;
		}
		for (Hit h : hits) {
			if (Render2D.hovered(mx, my, h.x, h.y, h.w, h.h)) {
				click();
				h.action.run();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (popupOpen && event.key() == GLFW.GLFW_KEY_ESCAPE) {
			popupOpen = false;
			return true;
		}
		return super.keyPressed(event);
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
	}
}
