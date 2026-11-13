package com.orbitclient.legacy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;

/** The same few calls the legacy client keeps per version, for Minecraft 1.14. */
public final class Compat {
	private Compat() {
	}

	/** The world light on an entity, packed the way the light map takes it. */
	public static int lightmap(LivingEntity entity, float tickDelta) {
		return entity.getLightmapCoordinates();
	}

	/** Gliding on an elytra (wings flap wider then). */
	public static boolean fallFlying(LivingEntity entity) {
		return entity.isFallFlying();
	}

	/** A flat creative world, for the screenshot audit. */
	public static LevelInfo auditWorld() {
		return new LevelInfo(1L, GameMode.CREATIVE, false, false, LevelGeneratorType.FLAT);
	}

	public static void screenshot(MinecraftClient mc, String name) {
		Framebuffer target = mc.getFramebuffer();
		// (This call has no name in 1.14's mappings: save a screenshot under a given file name.)
		ScreenshotUtils.method_1662(mc.runDirectory, name, target.viewportWidth, target.viewportHeight, target, message -> {
		});
	}
}
