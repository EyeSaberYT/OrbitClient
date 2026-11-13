package com.orbitclient.legacy;

import com.orbitclient.cosmetics.Cosmetic;
import com.orbitclient.cosmetics.CosmeticManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.cosmetics.Cosmetics;
import net.minecraft.client.MinecraftClient;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Development checks, off unless asked for.
 *
 * `gradlew runClient -PstartAudit`: once the game shows its first menu, print one line and quit, so a
 * build can be proven to start without anyone watching.
 *
 * `gradlew runClient -PshotAudit`: after that, open a flat test world, dress the player in cosmetics
 * (pretending to own them: this never talks to the cosmetics server) and save screenshots to
 * run/screenshots, so the drawing can be looked at.
 */
final class StartAudit {
	private static final boolean SHOTS = Boolean.getBoolean("orbit.shotAudit");
	private static final boolean ON = SHOTS || Boolean.getBoolean("orbit.startAudit");
	private static int ticks;
	private static int phase;
	private static int phaseAt;
	private static boolean done;

	private StartAudit() {
	}

	/** True while the audit dresses the player itself: the real sync must not touch the outfit then. */
	static boolean dressing() {
		return SHOTS;
	}

	static void tick(MinecraftClient mc) {
		if (!ON || done) return;
		ticks++;
		if (ticks > 20 * 180) {
			finish(mc, "FAILED: timed out in phase " + phase + " (on "
				+ Game.screenName(mc) + ")");
			return;
		}
		switch (phase) {
			case 0 -> {
				if (!Game.onTitleScreen(mc) || ticks <= 20) return;
				System.out.println("ORBIT-LEGACY-AUDIT OK: title screen on Java " + System.getProperty("java.version"));
				if (!SHOTS) {
					finish(mc, null);
					return;
				}
				Platform.auditSetup(mc);
				Game.startAuditWorld(mc);
				next();
			}
			case 1 -> {
				// A stray menu (the pause menu, if the window got a key press) would hold the audit up.
				if (Game.inWorld(mc) && !Game.noScreen(mc)) Game.closeScreen(mc);
				if (!Game.inWorld(mc) || !Game.noScreen(mc)) {
					phaseAt = ticks;
					return;
				}
				if (ticks - phaseAt < 60) return;
				Game.setPerspective(mc, 2); // in front of the player, looking back at them
				Set<String> all = new HashSet<>();
				for (Cosmetic c : Cosmetics.all()) all.add(c.id);
				CosmeticManager.auditOwn(Game.playerId(mc), all);
				next();
			}
			case 2 -> shotThen(mc, "orbit-plain", () -> dress(0));
			case 3 -> shotThen(mc, "orbit-front-1", () -> dress(1));
			case 4 -> shotThen(mc, "orbit-front-2", () -> dress(2));
			case 5 -> shotThen(mc, "orbit-front-3", () -> {
				dress(0);
				Game.setPerspective(mc, 1); // behind the player
			});
			case 6 -> shotThen(mc, "orbit-back-1", () -> dress(1));
			case 7 -> shotThen(mc, "orbit-back-2", () -> Game.openWardrobe(mc, CosmeticSlot.CAPE));
			case 8 -> shotThen(mc, "orbit-wardrobe-capes", () -> Game.openWardrobe(mc, CosmeticSlot.HAT));
			case 9 -> shotThen(mc, "orbit-wardrobe-hats", () -> Game.openWardrobe(mc, CosmeticSlot.PET));
			case 10 -> shotThen(mc, "orbit-wardrobe-pets", () -> {
			});
			default -> finish(mc, null);
		}
	}

	/** Wears the n-th cosmetic of every slot. */
	private static void dress(int n) {
		for (CosmeticSlot slot : CosmeticSlot.values()) {
			List<Cosmetic> in = Cosmetics.in(slot);
			if (in.isEmpty()) continue;
			Cosmetic c = in.get(n % in.size());
			CosmeticManager.wear(c, c.defaultColor);
		}
	}

	private static void shotThen(MinecraftClient mc, String name, Runnable then) {
		if (ticks - phaseAt < 30) return;
		double micros = Perf.takeMicrosPerPlayer();
		if (micros >= 0) System.out.printf(java.util.Locale.ROOT, "ORBIT-LEGACY-PERF before %s: %.1f microseconds per player drawn%n", name, micros);
		Compat.screenshot(mc, name + ".png");
		System.out.println("ORBIT-LEGACY-AUDIT shot " + name + ": " + CosmeticManager.own().toJson());
		then.run();
		next();
	}

	private static void next() {
		phase++;
		phaseAt = ticks;
	}

	private static void finish(MinecraftClient mc, String failure) {
		done = true;
		if (failure != null) System.out.println("ORBIT-LEGACY-AUDIT " + failure);
		else if (SHOTS) System.out.println("ORBIT-LEGACY-AUDIT OK: screenshots saved");
		Game.quit(mc);
	}
}
