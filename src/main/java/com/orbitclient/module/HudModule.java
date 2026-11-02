package com.orbitclient.module;

import com.orbitclient.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A mod that draws something on the HUD and can be dragged around in the HUD editor.
 * Position is stored as a fraction of the screen so layouts survive window resizes and GUI scale changes.
 */
public abstract class HudModule extends Module {
	private final double defaultX;
	private final double defaultY;
	private double xPercent;
	private double yPercent;
	public final NumberSetting scale;

	protected HudModule(String name, String description, double defaultX, double defaultY) {
		this(name, description, Category.HUD, defaultX, defaultY);
	}

	protected HudModule(String name, String description, Category category, double defaultX, double defaultY) {
		super(name, description, category);
		this.defaultX = defaultX;
		this.defaultY = defaultY;
		this.xPercent = defaultX;
		this.yPercent = defaultY;
		this.scale = add(new NumberSetting("Scale", 1.0, 0.25, 2.5, 0.05));
	}

	/** Draws the element at local (0, 0); the HUD renderer handles position and scale. */
	public abstract void render(GuiGraphics graphics, boolean editing);

	/** Unscaled size of the element. */
	public abstract int getWidth();

	public abstract int getHeight();

	public int getX(int screenWidth) {
		return (int) Math.round(xPercent * screenWidth);
	}

	public int getY(int screenHeight) {
		return (int) Math.round(yPercent * screenHeight);
	}

	/**
	 * The scale actually used for drawing. Minecraft's font is pixel art: at a scale where one font
	 * pixel doesn't cover a whole number of screen pixels, some pixel rows and columns get dropped and
	 * letters look broken. With Pixel-Perfect HUD Scaling on, the scale snaps to the nearest size where
	 * every font pixel is exactly 1, 2, 3... screen pixels (at GUI scale 3: 33%, 67%, 100%, 133%...).
	 */
	public float effectiveScale() {
		return snapScale(scale.getFloat());
	}

	public static float snapScale(float scale) {
		if (!com.orbitclient.config.ClientSettings.INSTANCE.pixelPerfectHud.isOn()) return scale;
		int guiScale = Math.max(1, mc.getWindow().getGuiScale());
		int screenPixels = Math.max(1, Math.round(scale * guiScale));
		return screenPixels / (float) guiScale;
	}

	/** One step when resizing in the HUD editor: one screen pixel per font pixel when snapping, else 5%. */
	public static float scaleStep() {
		if (!com.orbitclient.config.ClientSettings.INSTANCE.pixelPerfectHud.isOn()) return 0.05f;
		return 1f / Math.max(1, mc.getWindow().getGuiScale());
	}

	public int getScaledWidth() {
		return (int) Math.ceil(getWidth() * effectiveScale());
	}

	public int getScaledHeight() {
		return (int) Math.ceil(getHeight() * effectiveScale());
	}

	public void setPosition(int x, int y, int screenWidth, int screenHeight) {
		x = Math.max(0, Math.min(screenWidth - getScaledWidth(), x));
		y = Math.max(0, Math.min(screenHeight - getScaledHeight(), y));
		this.xPercent = (double) x / screenWidth;
		this.yPercent = (double) y / screenHeight;
	}

	public double getXPercent() {
		return xPercent;
	}

	public double getYPercent() {
		return yPercent;
	}

	public void setPercent(double x, double y) {
		this.xPercent = x;
		this.yPercent = y;
	}

	// Animation state, driven by HudRenderer.
	/** 0..1 fade for appearing and disappearing. */
	public float visibility;
	/** Scale as drawn, easing toward the scale setting. */
	public float shownScale = -1f;
	private long pulseTime;

	/** A short "pop" on the next frames, e.g. when a combo goes up. */
	public void pulse() {
		pulseTime = System.currentTimeMillis();
	}

	/** Extra scale from the pulse: rises and falls back to 1 over 0.3 seconds. */
	public float pulseScale() {
		if (pulseTime == 0) return 1f;
		float t = com.orbitclient.util.Anim.since(pulseTime, 0.3f);
		if (t >= 1f) return 1f;
		return 1f + 0.15f * (float) Math.sin(t * Math.PI);
	}

	@Override
	public void resetToDefaults() {
		super.resetToDefaults();
		setPercent(defaultX, defaultY);
	}

	/** Whether the element should be drawn outside the editor (e.g. potion HUD with no effects has nothing to show). */
	public boolean shouldRender() {
		return true;
	}
}
