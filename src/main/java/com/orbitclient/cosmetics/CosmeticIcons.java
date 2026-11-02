package com.orbitclient.cosmetics;

import com.orbitclient.OrbitClient;
import com.orbitclient.util.SmoothTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pictures of cosmetics for menus: each model drawn once per color and kept as a texture. */
public final class CosmeticIcons {
	// Render the pixel-art models at 2x the old size, then linearly sample them in the UI.
	// This keeps small wardrobe nodes crisp while making the detail readable in previews.
	public static final int SIZE = 192;
	private static final int MAX = 120;
	private static final Map<String, Identifier> CACHE = new LinkedHashMap<>(16, 0.75f, true);

	private CosmeticIcons() {
	}

	/** The picture for a cosmetic in a color, or null for capes (they show their own texture). */
	public static Identifier get(Cosmetic c, int color) {
		if (c.iconModel == null) return null;
		int tint = color | 0xFF000000;
		String key = c.id + "_" + Integer.toHexString(tint);
		Identifier id = CACHE.get(key);
		if (id != null) return id;
		int[] px = c.iconModel.icon(SIZE, tint, c.iconPair);
		NativeImage img = new NativeImage(SIZE, SIZE, true);
		for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) img.setPixel(x, y, px[y * SIZE + x]);
		id = Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "dynamic/cosmetic/" + key.toLowerCase());
		Minecraft.getInstance().getTextureManager().register(id, new SmoothTexture("cosmetic " + key, img));
		CACHE.put(key, id);
		// Forget the least recently used pictures (e.g. after trying many colors).
		while (CACHE.size() > MAX) {
			var oldest = CACHE.entrySet().iterator().next();
			Minecraft.getInstance().getTextureManager().release(oldest.getValue());
			CACHE.remove(oldest.getKey());
		}
		return id;
	}
}
