package com.orbitclient.module.visual;

import com.orbitclient.gui.CrosshairEditorScreen;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.*;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Render2D;
//? if >=1.21.6 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Custom crosshair, laid out like Lunar's crosshair mod: Simple (thickness/size/gap/dot of the
 * vanilla cross), Preset (14 designs) or Custom (a 17x17 pixel grid drawn in the editor), with
 * size options and three color modes (vanilla blending, static, dynamic enemy/friendly colors).
 */
public class CrosshairModule extends Module {
	private static final int N = PixelGridSetting.SIZE;

	public static final String[] PRESETS = {
		"Plus", "Sniper", "Chevron", "Dot", "Circle", "Square", "Tipped Plus",
		"Corners", "Brackets", "Wide Corners", "Scope", "Bold X", "Gapped X", "Diamond"
	};

	public final ModeSetting mode;
	public final NumberSetting preset;
	public final NumberSetting thickness;
	public final NumberSetting length;
	public final NumberSetting gap;
	public final BooleanSetting dot;
	public final PixelGridSetting custom;
	public final BooleanSetting customScale;
	public final ModeSetting scale;
	public final ModeSetting colorMode;
	public final ColorSetting color;
	public final ColorSetting enemyColor;
	public final ColorSetting friendlyColor;
	public final BooleanSetting outline;

	private final Anim.Clock clock = new Anim.Clock();
	private float enemyFade, friendlyFade;
	private boolean[] simpleCache;
	private String simpleKey;
	private static final boolean[][] PRESET_CACHE = new boolean[PRESETS.length][];

	public CrosshairModule() {
		super("Crosshair", "Replaces the vanilla crosshair with a customizable one.", Category.MECHANIC);
		setIcon(Items.TARGET);
		mode = add(new ModeSetting("Mode", "Simple", "Simple", "Preset", "Custom"));
		add(new ActionSetting("Crosshair Editor", "Open Editor", () -> mc.setScreen(new CrosshairEditorScreen(this))));
		preset = add(new NumberSetting("Preset", 1, 0, PRESETS.length - 1, 1).visibleWhen(() -> false));
		thickness = add(new NumberSetting("Thickness", 1, 1, 3, 1).visibleWhen(() -> mode.is("Simple")));
		length = add(new NumberSetting("Size", 4, 1, 7, 1).visibleWhen(() -> mode.is("Simple")));
		gap = add(new NumberSetting("Gap", 1, 0, 4, 1).visibleWhen(() -> mode.is("Simple")));
		dot = add(new BooleanSetting("Crosshair Dot", true).visibleWhen(() -> mode.is("Simple")));
		custom = add(new PixelGridSetting("Custom Pixels", preset(1)).visibleWhen(() -> false));
		customScale = add(new BooleanSetting("Use Custom Scale", false));
		scale = add(new ModeSetting("Scale", "Normal", "Small", "Normal", "Large", "Auto").visibleWhen(customScale::isOn));
		colorMode = add(new ModeSetting("Color Mode", "Static", "Vanilla Blending", "Static", "Dynamic"));
		color = add(new ColorSetting("Color", 0xFFFFFFFF).visibleWhen(() -> !colorMode.is("Vanilla Blending")));
		enemyColor = add(new ColorSetting("Enemy Color", 0xFFFF4040).visibleWhen(() -> colorMode.is("Dynamic")));
		friendlyColor = add(new ColorSetting("Friendly Color", 0xFF40FF60).visibleWhen(() -> colorMode.is("Dynamic")));
		outline = add(new BooleanSetting("Outline", true).visibleWhen(() -> !colorMode.is("Vanilla Blending")));
	}

	// ---------------------------------------------------------------- designs

	/** A preset design on the 17x17 grid. */
	public static boolean[] preset(int index) {
		if (PRESET_CACHE[index] != null) return PRESET_CACHE[index];
		boolean[] p = new boolean[N * N];
		int c = N / 2;
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				int dx = x - c, dy = y - c, ax = Math.abs(dx), ay = Math.abs(dy);
				double dist = Math.sqrt(dx * dx + dy * dy);
				p[y * N + x] = switch (index) {
					case 0 -> (dx == 0 && ay <= 4) || (dy == 0 && ax <= 4);
					case 1 -> (dx == 0 && ay >= 2 && ay <= 4) || (dy == 0 && ax >= 2 && ax <= 4) || (dx == 0 && dy == 0);
					case 2 -> dy == ax - 1 && ax <= 3;
					case 3 -> ax <= 1 && ay <= 1;
					case 4 -> Math.abs(dist - 3.6) < 0.6 || (dx == 0 && dy == 0);
					case 5 -> Math.max(ax, ay) == 3 || (dx == 0 && dy == 0);
					case 6 -> (dx == 0 && ay <= 4) || (dy == 0 && ax <= 4) || (ay == 4 && ax <= 1) || (ax == 4 && ay <= 1);
					case 7 -> ((ax == 3 && ay >= 2 && ay <= 3) || (ay == 3 && ax >= 2 && ax <= 3)) || (dx == 0 && dy == 0);
					case 8 -> (dx == 0 && ay >= 2 && ay <= 3) || (dy == 0 && ax >= 2 && ax <= 3) || (ax == 3 && ay == 3) || (dx == 0 && dy == 0);
					case 9 -> (ax == 4 && ay >= 2 && ay <= 4) || (ay == 4 && ax >= 2 && ax <= 4);
					case 10 -> Math.abs(dist - 2.2) < 0.55 || (dy == 0 && ax >= 4 && ax <= 6);
					case 11 -> Math.abs(ax - ay) <= 1 && Math.max(ax, ay) <= 3 && !(ax == 0 && ay == 1) && !(ax == 1 && ay == 0);
					case 12 -> ax == ay && ax >= 2 && ax <= 4;
					case 13 -> ax + ay == 3 || (dx == 0 && dy == 0);
					default -> false;
				};
			}
		}
		PRESET_CACHE[index] = p;
		return p;
	}

	/** The Simple mode cross (vanilla style) as a grid, built from thickness, size, gap and dot. */
	private boolean[] simple() {
		int t = thickness.getInt(), s = length.getInt(), g = Math.min(gap.getInt(), N / 2 - s);
		String key = t + "/" + s + "/" + g + "/" + dot.isOn();
		if (key.equals(simpleKey)) return simpleCache;
		boolean[] p = new boolean[N * N];
		int c = N / 2;
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				int dx = x - c, dy = y - c;
				boolean bandX = dx >= -(t - 1) / 2 && dx <= t / 2, bandY = dy >= -(t - 1) / 2 && dy <= t / 2;
				boolean vertical = bandX && Math.abs(dy) > g && Math.abs(dy) <= g + s;
				boolean horizontal = bandY && Math.abs(dx) > g && Math.abs(dx) <= g + s;
				p[y * N + x] = vertical || horizontal || (dot.isOn() && bandX && bandY);
			}
		}
		simpleKey = key;
		simpleCache = p;
		return p;
	}

	/** The pixels of the crosshair currently in use. */
	public boolean[] pixels() {
		return switch (mode.get()) {
			case "Preset" -> preset(preset.getInt());
			case "Custom" -> custom.copy();
			default -> simple();
		};
	}

	// ---------------------------------------------------------------- rendering

	/** Screen pixels per crosshair pixel: Small/Normal/Large look like GUI scale 1/2/3, Auto follows your GUI scale. */
	public int cellPixels(int guiScale) {
		if (!customScale.isOn()) return guiScale;
		return switch (scale.get()) {
			case "Small" -> 1;
			case "Normal" -> 2;
			case "Large" -> 3;
			default -> guiScale;
		};
	}

	/** Crosshair color right now: fades to enemy/friendly colors in Dynamic mode. */
	private int currentColor() {
		float dt = clock.tick();
		Entity target = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
		boolean dynamic = colorMode.is("Dynamic");
		boolean enemy = dynamic && (target instanceof Enemy || target instanceof Player);
		boolean friendly = dynamic && !enemy && target instanceof LivingEntity;
		enemyFade = Anim.approach(enemyFade, enemy ? 1f : 0f, 18f, dt);
		friendlyFade = Anim.approach(friendlyFade, friendly ? 1f : 0f, 18f, dt);
		int c = color.getColor();
		c = Render2D.lerpColor(c, enemyColor.getColor(), enemyFade);
		return Render2D.lerpColor(c, friendlyColor.getColor(), friendlyFade);
	}

	/** In game: centered exactly where the vanilla crosshair's middle pixel is. */
	public void render(GuiGraphics g) {
		int w = g.guiWidth(), h = g.guiHeight();
		renderAt(g, pixels(), (w - 15) / 2 + 7, (h - 15) / 2 + 7, currentColor(), colorMode.is("Vanilla Blending"), outline.isOn(), -1);
	}

	/** For the editor preview: the current design at (gx, gy) with the current colors and size. */
	public void renderPreview(GuiGraphics g, boolean[] pixels, int gx, int gy) {
		renderAt(g, pixels, gx, gy, currentColor(), colorMode.is("Vanilla Blending"), outline.isOn(), -1);
	}

	/**
	 * Draws a 17x17 design whose center pixel sits on GUI pixel (gx, gy), in real screen pixels.
	 * Vanilla Blending uses the game's own crosshair blend mode (inverts what's behind it).
	 */
	public void renderAt(GuiGraphics g, boolean[] px, int gx, int gy, int color, boolean blend, boolean withOutline, int forceCell) {
		int guiScale = Math.max(1, mc.getWindow().getGuiScale());
		int cell = forceCell > 0 ? forceCell : cellPixels(guiScale);
		int c = N / 2;
		// Top-left of the grid in screen pixels, with the center cell centered in that GUI pixel.
		int ox = gx * guiScale + (guiScale - cell) / 2 - c * cell;
		int oy = gy * guiScale + (guiScale - cell) / 2 - c * cell;
		// GUI_INVERT is vanilla's inverting blend for plain fills (CROSSHAIR is the same blend but needs a texture).
		//? if >=1.21.7 {
		RenderPipeline pipeline = blend ? RenderPipelines.GUI_INVERT : RenderPipelines.GUI;
		//?}
		//? if >=1.21.6 <1.21.7 {
		/*RenderPipeline pipeline = RenderPipelines.GUI; // 1.21.6 has no inverting fill: blend draws in its color
		*///?}
		//? if <1.21.6 {
		/*// Before 1.21.6 GUI fills had no inverting blend either: blend draws in its color.
		net.minecraft.client.renderer.RenderType pipeline = net.minecraft.client.renderer.RenderType.gui();
		*///?}
		// The inverting blend has to run in its own layer after what's behind it (vanilla does the same),
		// otherwise menus draw it before their own backgrounds and it disappears.
		if (blend) g.nextStratum();
		g.pose().pushMatrix();
		g.pose().scale(1f / guiScale, 1f / guiScale);
		if (withOutline && !blend) {
			int shadow = Render2D.a(0xB0000000);
			for (int y = -1; y <= N; y++) {
				for (int x = -1; x <= N; x++) {
					if (on(px, x, y) || !touches(px, x, y)) continue;
					g.fill(ox + x * cell, oy + y * cell, ox + (x + 1) * cell, oy + (y + 1) * cell, shadow);
				}
			}
		}
		int fill = blend ? Render2D.a(0xFFFFFFFF) : Render2D.a(color);
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				if (px[y * N + x]) g.fill(pipeline, ox + x * cell, oy + y * cell, ox + (x + 1) * cell, oy + (y + 1) * cell, fill);
			}
		}
		g.pose().popMatrix();
		if (blend) g.nextStratum();
	}

	private static boolean on(boolean[] px, int x, int y) {
		return x >= 0 && y >= 0 && x < N && y < N && px[y * N + x];
	}

	private static boolean touches(boolean[] px, int x, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) if (on(px, x + dx, y + dy)) return true;
		}
		return false;
	}
}
