package com.orbitclient.gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Player heads for menus (accounts, friends): the default skin until the real one is fetched from Mojang. */
public final class Heads {
	private static final Map<UUID, Supplier<PlayerSkin>> SKINS = new HashMap<>();

	private Heads() {
	}

	/** Game thread only. */
	public static Supplier<PlayerSkin> get(UUID uuid, String name) {
		Supplier<PlayerSkin> known = SKINS.get(uuid);
		if (known != null) return known;
		Minecraft mc = Minecraft.getInstance();
		// Your own profile already has its textures.
		if (uuid.equals(mc.getUser().getProfileId()) && mc.getGameProfile().id().equals(uuid)) {
			Supplier<PlayerSkin> own = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
			SKINS.put(uuid, own);
			return own;
		}
		Supplier<PlayerSkin> fallback = mc.getSkinManager().createLookup(new GameProfile(uuid, name.isEmpty() ? "Player" : name), false);
		SKINS.put(uuid, fallback);
		CompletableFuture.supplyAsync(() -> mc.services().sessionService().fetchProfile(uuid, false))
			.thenAccept(result -> {
				if (result != null) mc.execute(() -> SKINS.put(uuid, mc.getSkinManager().createLookup(result.profile(), false)));
			})
			.exceptionally(e -> null);
		return fallback;
	}

	public static void draw(GuiGraphics g, UUID uuid, String name, int x, int y, int size, int color) {
		PlayerFaceRenderer.draw(g, get(uuid, name).get(), x, y, size, color);
	}
}
