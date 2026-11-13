package com.orbitclient.legacy;

import com.orbitclient.cosmetics.CosmeticSync;
import net.fabricmc.api.ClientModInitializer;
import com.orbitclient.cosmetics.CosmeticSlot;
import net.minecraft.client.MinecraftClient;

/** Orbit Client on the old engine (Minecraft 1.8.9 to 1.13.2, Legacy Fabric): Orbit cosmetics and the wardrobe. */
public final class OrbitLegacy implements ClientModInitializer {
	public static final String MOD_ID = "orbitclient";
	private static boolean loaded;
	private static boolean wardrobeKeyDown;

	@Override
	public void onInitializeClient() {
		CosmeticSync.init();
		System.out.println("[Orbit Client] Cosmetics ready. Right Shift opens the wardrobe.");
	}

	/** Called at the end of every client tick (MinecraftClientMixin). */
	public static void tick(MinecraftClient mc) {
		if (!loaded) {
			// The game folder is only known once the game has started.
			loaded = true;
			OrbitConfig.load();
		}
		if (!StartAudit.dressing()) CosmeticSync.tick(mc);
		StartAudit.tick(mc);

		// Right Shift opens the wardrobe, in a world or from the title screen.
		boolean down = Platform.wardrobeKeyDown();
		if (down && !wardrobeKeyDown && (Game.noScreen(mc) || Game.onTitleScreen(mc))) Game.openWardrobe(mc, CosmeticSlot.CAPE);
		wardrobeKeyDown = down;
	}
}
