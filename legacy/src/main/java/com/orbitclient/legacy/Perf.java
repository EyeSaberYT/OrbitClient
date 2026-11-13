package com.orbitclient.legacy;

/**
 * How long drawing cosmetics takes, for the audit (`-PshotAudit` prints it): the time spent in the
 * cosmetics layer and how many players it drew. Two additions and a clock read per player otherwise.
 */
public final class Perf {
	private static long nanos;
	private static long players;

	private Perf() {
	}

	public static long start() {
		return System.nanoTime();
	}

	public static void end(long startedAt) {
		nanos += System.nanoTime() - startedAt;
		players++;
	}

	/** Microseconds per player drawn since the last call, or -1 when none was. */
	public static synchronized double takeMicrosPerPlayer() {
		double result = players == 0 ? -1 : nanos / 1000.0 / players;
		nanos = 0;
		players = 0;
		return result;
	}
}
