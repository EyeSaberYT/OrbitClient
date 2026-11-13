package com.orbitclient.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.legacy.gui.WardrobeScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Everything the shared code (sync, wardrobe, audit) asks of the game, by the names modern Fabric's mappings
 * give Minecraft 1.14. The legacy client (../legacy) has its own Game with the same methods.
 */
public final class Game {
	private Game() {
	}

	// ---- state

	public static File runDirectory(MinecraftClient mc) {
		return mc.runDirectory;
	}

	public static boolean inWorld(MinecraftClient mc) {
		return mc.world != null && mc.player != null;
	}

	public static boolean noScreen(MinecraftClient mc) {
		return mc.currentScreen == null;
	}

	public static boolean onTitleScreen(MinecraftClient mc) {
		return mc.currentScreen instanceof TitleScreen;
	}

	public static String screenName(MinecraftClient mc) {
		return mc.currentScreen == null ? "no screen" : mc.currentScreen.getClass().getName();
	}

	/** The local player's id in the world (null outside one). */
	public static UUID playerId(MinecraftClient mc) {
		return mc.player == null ? null : mc.player.getUuid();
	}

	/** The ids of the players in the world. */
	public static List<UUID> playerIds(MinecraftClient mc) {
		List<UUID> ids = new ArrayList<>();
		for (PlayerEntity player : mc.world.getPlayers()) ids.add(player.getUuid());
		return ids;
	}

	// ---- actions

	public static void onGameThread(MinecraftClient mc, Runnable work) {
		mc.execute(work);
	}

	public static void openWardrobe(MinecraftClient mc, CosmeticSlot slot) {
		mc.openScreen(new WardrobeScreen(slot));
	}

	public static void closeScreen(MinecraftClient mc) {
		mc.openScreen(null);
	}

	public static void quit(MinecraftClient mc) {
		mc.scheduleStop();
	}

	/** For the screenshot audit: a flat creative world. */
	public static void startAuditWorld(MinecraftClient mc) {
		mc.startIntegratedServer("orbit_audit", "orbit_audit", Compat.auditWorld());
	}

	/** 0 first person, 1 behind the player, 2 in front of them. */
	public static void setPerspective(MinecraftClient mc, int perspective) {
		mc.options.perspective = perspective;
	}

	// ---- drawing for the wardrobe

	public static int textWidth(TextRenderer renderer, String text) {
		return renderer.getStringWidth(text);
	}

	public static List<String> wrap(TextRenderer renderer, String text, int width) {
		return renderer.wrapStringToWidthAsList(text, width);
	}

	public static String trim(TextRenderer renderer, String text, int width) {
		return renderer.trimToWidth(text, width);
	}

	public static void textShadow(TextRenderer renderer, String text, int x, int y, int color) {
		renderer.drawWithShadow(text, x, y, color);
	}

	public static void fill(int x1, int y1, int x2, int y2, int color) {
		DrawableHelper.fill(x1, y1, x2, y2, color);
	}

	/** Draws a piece of the bound texture (u, v, regionWidth, regionHeight of a textureWidth x textureHeight image). */
	public static void texture(int x, int y, float u, float v, int regionWidth, int regionHeight, int width, int height, float textureWidth, float textureHeight) {
		DrawableHelper.blit(x, y, width, height, u, v, regionWidth, regionHeight, (int) textureWidth, (int) textureHeight);
	}

	public static void bind(MinecraftClient mc, Identifier texture) {
		mc.getTextureManager().bindTexture(texture);
	}

	/** White, opaque: filling a rectangle leaves its color set. */
	public static void white() {
		GlStateManager.color4f(1f, 1f, 1f, 1f);
	}

	public static void blend() {
		GlStateManager.enableBlend();
	}

	/** The local player as the inventory draws them, looking towards (lookX, lookY). Nothing outside a world. */
	public static void drawPlayer(MinecraftClient mc, int x, int y, int scale, float lookX, float lookY) {
		if (mc.player == null) return;
		white();
		InventoryScreen.drawEntity(x, y, scale, lookX, lookY, mc.player);
		white();
	}
}
