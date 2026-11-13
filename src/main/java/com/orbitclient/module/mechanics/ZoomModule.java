package com.orbitclient.module.mechanics;

import net.minecraft.world.item.Items;
import com.orbitclient.OrbitClient;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;

public class ZoomModule extends Module {
	private static final float MAX_ZOOM = 50f;
	private static final float SCROLL_STEP = 1.2f;

	private static ZoomModule instance;

	public final NumberSetting factor;
	public final BooleanSetting scrollToZoom;
	public final BooleanSetting resetScroll;
	public final BooleanSetting hideHand;
	public final BooleanSetting smoothZoom;
	public final BooleanSetting smoothCamera;
	public final BooleanSetting reduceSensitivity;

	private float current = 1f;
	/** Extra zoom from scrolling while the zoom key is held (1 = none). */
	private float scrollMultiplier = 1f;
	private long lastFrame = System.currentTimeMillis();
	private boolean smoothCameraWasOn;
	private boolean wasZooming;

	public ZoomModule() {
		super("Zoom", "Hold the zoom key (C by default, change it in Controls) to zoom in. Scroll while zooming to zoom further.", Category.MECHANIC);
		setIcon(Items.SPYGLASS);
		instance = this;
		factor = add(new NumberSetting("Zoom Factor", 4, 1.5, 15, 0.5));
		scrollToZoom = add(new BooleanSetting("Scroll to Zoom", true));
		resetScroll = add(new BooleanSetting("Reset Scroll Zoom on Release", true).visibleWhen(scrollToZoom::isOn));
		hideHand = add(new BooleanSetting("Hide Hand While Zooming", true));
		smoothZoom = add(new BooleanSetting("Smooth Zoom", true));
		smoothCamera = add(new BooleanSetting("Cinematic Camera", false));
		reduceSensitivity = add(new BooleanSetting("Reduce Sensitivity", true));
	}

	public static boolean isZooming() {
		// In a world only: this is also asked while the game starts, before there are any screens (26.3).
		return instance != null && instance.isEnabled() && mc.player != null && mc.screen == null && OrbitClient.zoomKey.isDown();
	}

	@Override
	public void onTick() {
		boolean zooming = isZooming();
		if (zooming != wasZooming && smoothCamera.isOn()) {
			if (zooming) {
				smoothCameraWasOn = mc.options.smoothCamera;
				mc.options.smoothCamera = true;
			} else {
				mc.options.smoothCamera = smoothCameraWasOn;
			}
		}
		if (!zooming && wasZooming && resetScroll.isOn()) scrollMultiplier = 1f;
		wasZooming = zooming;
	}

	/**
	 * Mouse wheel while zooming: scroll up zooms in, down zooms out (never past no zoom).
	 * Returns true if the scroll was used, so the hotbar doesn't also change slot.
	 */
	public static boolean onScroll(double amount) {
		if (instance == null || amount == 0 || !instance.scrollToZoom.isOn() || !isZooming()) return false;
		float base = instance.factor.getFloat();
		float zoom = base * instance.scrollMultiplier * (amount > 0 ? SCROLL_STEP : 1f / SCROLL_STEP);
		zoom = Math.max(1f, Math.min(MAX_ZOOM, zoom));
		instance.scrollMultiplier = zoom / base;
		return true;
	}

	/** Whether to skip drawing the first-person hand: while zoomed in, including while zooming back out. */
	public static boolean hidesHand() {
		return instance != null && instance.isEnabled() && instance.hideHand.isOn() && instance.current > 1.02f;
	}

	/** Called from the FOV mixin every frame. */
	public static float modifyFov(float fov) {
		if (instance == null) return fov;
		return instance.apply(fov);
	}

	private float apply(float fov) {
		long now = System.currentTimeMillis();
		float dt = Math.min(1f, (now - lastFrame) / 1000f);
		lastFrame = now;
		float target = isZooming() ? factor.getFloat() * scrollMultiplier : 1f;
		if (smoothZoom.isOn()) current += (target - current) * Math.min(1f, dt * 12f);
		else current = target;
		if (Math.abs(current - 1f) < 0.001f) current = 1f;
		return fov / current;
	}

	/** Mouse sensitivity multiplier while zoomed, so aiming doesn't feel twitchy. */
	public static double sensitivityMultiplier() {
		if (instance == null || !instance.reduceSensitivity.isOn() || !isZooming()) return 1;
		return 1.0 / instance.current;
	}
}
