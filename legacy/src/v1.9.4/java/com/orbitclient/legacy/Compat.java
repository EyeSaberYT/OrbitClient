package com.orbitclient.legacy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.entity.LivingEntity;

import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;

/** The few calls that differ between the old versions this client is built for. This one is for 1.9.4. */
public final class Compat {
	private Compat() {
	}

	/** The world light on an entity, packed the way the light map takes it. */
	public static int lightmap(LivingEntity entity, float tickDelta) {
		return entity.getLightmapCoordinates(tickDelta);
	}

	/** Gliding on an elytra (wings flap wider then). */
	public static boolean fallFlying(LivingEntity entity) {
		return entity.method_13055(); // "is gliding" has no name in these mappings
	}

	/** A flat creative world, for the screenshot audit. */
	public static LevelInfo auditWorld() {
		return new LevelInfo(1L, LevelInfo.GameMode.CREATIVE, false, false, LevelGeneratorType.FLAT);
	}

	public static void screenshot(MinecraftClient mc, String name) {
		ScreenshotUtils.method_12154(mc.runDirectory, name, mc.width, mc.height, mc.getFramebuffer());
	}
}
