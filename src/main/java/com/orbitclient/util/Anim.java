package com.orbitclient.util;

import com.orbitclient.config.ClientSettings;

/** Easing curves and frame-rate independent smoothing, scaled by the Animation Speed setting. */
public final class Anim {
	private Anim() {
	}

	public static boolean enabled() {
		return ClientSettings.INSTANCE.animations.isOn();
	}

	/** Multiplier for every animation's speed (higher = faster). */
	public static float speed() {
		return ClientSettings.INSTANCE.animationSpeed.getFloat();
	}

	/** Exponential approach toward a target; jumps straight there when animations are off. */
	public static float approach(float current, float target, float rate, float dt) {
		if (!enabled()) return target;
		float next = current + (target - current) * Math.min(1f, dt * rate * speed());
		return Math.abs(next - target) < 0.0005f ? target : next;
	}

	public static double approach(double current, double target, double rate, float dt) {
		if (!enabled()) return target;
		double next = current + (target - current) * Math.min(1.0, dt * rate * speed());
		return Math.abs(next - target) < 0.0005 ? target : next;
	}

	/** Linear 0..1 progress that moves toward 1 (or 0) over the given duration in seconds. */
	public static float step(float progress, boolean forward, float durationSeconds, float dt) {
		if (!enabled()) return forward ? 1f : 0f;
		float delta = dt * speed() / durationSeconds;
		return clamp01(progress + (forward ? delta : -delta));
	}

	/** Progress of a timed animation that started at startMs. */
	public static float since(long startMs, float durationSeconds) {
		if (!enabled()) return 1f;
		return clamp01((System.currentTimeMillis() - startMs) / 1000f * speed() / durationSeconds);
	}

	public static float clamp01(float t) {
		return Math.max(0f, Math.min(1f, t));
	}

	public static float easeOutCubic(float t) {
		t = clamp01(t);
		float f = 1 - t;
		return 1 - f * f * f;
	}

	public static float easeInOutCubic(float t) {
		t = clamp01(t);
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	/** Overshoots slightly before settling, for "pop" effects. */
	public static float easeOutBack(float t) {
		t = clamp01(t);
		float c1 = 1.70158f, c3 = c1 + 1;
		return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
	}

	/** Tracks time between frames for one animated thing. */
	public static final class Clock {
		private long last = System.nanoTime();

		/** Seconds since the previous call, capped so a lag spike doesn't skip animations. */
		public float tick() {
			long now = System.nanoTime();
			float dt = (now - last) / 1_000_000_000f;
			last = now;
			return Math.min(dt, 0.1f);
		}
	}
}
