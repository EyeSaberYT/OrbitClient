package com.orbitclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Tracks the reach of your last hit and your current combo (hits landed without getting hit back). */
public final class CombatTracker {
	private static final long COMBO_TIMEOUT_MS = 2000;
	private static final long REACH_TIMEOUT_MS = 3000;

	private static double lastReach = -1;
	private static long lastHitTime;
	private static int combo;
	private static int lastHurtTime;

	private CombatTracker() {
	}

	public static void onAttack(Player player, Entity target) {
		Vec3 eyes = player.getEyePosition();
		Vec3 end = eyes.add(player.getViewVector(1f).scale(8));
		AABB box = target.getBoundingBox();
		lastReach = box.contains(eyes) ? 0 : box.clip(eyes, end).map(eyes::distanceTo)
			.orElseGet(() -> Math.sqrt(box.distanceToSqr(eyes)));
		lastHitTime = System.currentTimeMillis();
		combo++;
	}

	public static void tick() {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			combo = 0;
			return;
		}
		// hurtTime jumps to 10 when we take damage, which breaks the combo.
		if (player.hurtTime > lastHurtTime) combo = 0;
		lastHurtTime = player.hurtTime;
		if (System.currentTimeMillis() - lastHitTime > COMBO_TIMEOUT_MS) combo = 0;
	}

	/** Reach of the last hit in blocks, or -1 if there was no recent hit. */
	public static double getReach() {
		return System.currentTimeMillis() - lastHitTime > REACH_TIMEOUT_MS ? -1 : lastReach;
	}

	public static int getCombo() {
		return combo;
	}
}
