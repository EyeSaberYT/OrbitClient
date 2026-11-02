package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.Category;
import com.orbitclient.module.TextHudModule;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PingModule extends TextHudModule {
	public PingModule() {
		super("Ping", "Shows your latency to the server.", Category.SERVER, 0.005, 0.19);
		setIcon(Items.REDSTONE_TORCH);
	}

	@Override
	protected String getText() {
		if (mc.player == null || mc.getConnection() == null) return "0 ms";
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return (info == null ? 0 : info.getLatency()) + " ms";
	}

	@Override
	protected String getPreviewText() {
		return "32 ms";
	}
}
