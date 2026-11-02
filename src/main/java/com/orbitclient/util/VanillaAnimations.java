package com.orbitclient.util;

import com.orbitclient.config.ClientSettings;
import net.minecraft.client.Minecraft;

/** State for animations layered on vanilla HUD parts: the hotbar selector and chat. */
public final class VanillaAnimations {
	private static final Anim.Clock HOTBAR_CLOCK = new Anim.Clock();
	private static float hotbarSlot = -1;
	private static long lastChatMessage;

	private VanillaAnimations() {
	}

	/** Shifts the hotbar selection highlight so it glides between slots instead of jumping. */
	public static int hotbarSelectionX(int vanillaX) {
		Minecraft mc = Minecraft.getInstance();
		float dt = HOTBAR_CLOCK.tick();
		if (mc.player == null || !Anim.enabled() || !ClientSettings.INSTANCE.smoothHotbar.isOn()) {
			hotbarSlot = -1;
			return vanillaX;
		}
		int slot = mc.player.getInventory().getSelectedSlot();
		hotbarSlot = hotbarSlot < 0 ? slot : Anim.approach(hotbarSlot, slot, 22f, dt);
		return vanillaX + Math.round((hotbarSlot - slot) * 20);
	}

	public static void onChatMessage() {
		lastChatMessage = System.currentTimeMillis();
	}

	/** How far (in pixels) to push chat down so the newest line slides up into place. */
	public static float chatOffset() {
		if (!Anim.enabled() || !ClientSettings.INSTANCE.chatAnimation.isOn()) return 0;
		float p = Anim.easeOutCubic(Anim.since(lastChatMessage, 0.2f));
		return (1 - p) * 9;
	}
}
