package com.orbitclient.util;

/**
 * Eases a game value (gamma, time of day, rain) from the real value to an override and back,
 * instead of snapping. Once it has eased back to the real value it stops touching it.
 */
public final class SmoothOverride {
	private final double rate;
	/** For wrapping values such as time of day (24000 ticks); 0 for none. */
	private final double period;
	private final Anim.Clock clock = new Anim.Clock();
	private boolean active;
	private double current;

	public SmoothOverride(double rate, double period) {
		this.rate = rate;
		this.period = period;
	}

	public double apply(double real, boolean enabled, double target) {
		float dt = clock.tick();
		if (!enabled && !active) return real;
		if (!active) {
			current = real;
			active = true;
		}
		double goal = enabled ? target : real;
		if (period > 0) {
			// Take the short way around, e.g. from 23000 to 1000 ticks goes forward through midnight.
			double diff = ((goal - current) % period + period * 1.5) % period - period / 2;
			goal = current + diff;
		}
		current = Anim.approach(current, goal, rate, dt);
		if (period > 0) current = ((current % period) + period) % period;
		if (!enabled && Math.abs(current - real) < (period > 0 ? 5 : 0.005)) {
			active = false;
			return real;
		}
		return current;
	}
}
