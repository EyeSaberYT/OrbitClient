package com.orbitclient.cosmetics;

import com.orbitclient.OrbitClient;
import com.orbitclient.config.ClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Orbit Client badge in front of names (nametags and the tab list) of players who use Orbit Client,
 * like other clients' badges. It's one glyph of a small font (assets/orbitclient/font/badge.json),
 * so it shows wherever the name is drawn. Who uses Orbit Client comes from the cosmetics server.
 */
public final class OrbitBadge {
	private static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "badge"));
	private static final Component BADGE = Component.literal("✦").withStyle(s -> s.withColor(0x8C7BFF));
	private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();

	private OrbitBadge() {
	}

	public static void markUser(UUID uuid) {
		USERS.add(uuid);
	}

	public static boolean isUser(UUID uuid) {
		Minecraft mc = Minecraft.getInstance();
		return USERS.contains(uuid) || (mc.player != null && mc.player.getUUID().equals(uuid));
	}

	/** The name with the badge in front, if that player uses Orbit Client and badges are on. */
	public static Component decorate(UUID uuid, Component name) {
		if (name == null || uuid == null || !ClientSettings.INSTANCE.orbitBadges.isOn() || !isUser(uuid)) return name;
		MutableComponent out = Component.empty().append(BADGE).append(" ").append(name);
		return out;
	}
}
