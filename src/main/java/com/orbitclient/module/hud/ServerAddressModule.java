package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.Category;
import com.orbitclient.module.TextHudModule;
import net.minecraft.client.multiplayer.ServerData;

public class ServerAddressModule extends TextHudModule {
	public ServerAddressModule() {
		super("Server Address", "Shows the server you are playing on.", Category.SERVER, 0.16, 0.19);
		setIcon(Items.NAME_TAG);
	}

	@Override
	protected String getText() {
		if (mc.hasSingleplayerServer()) return "Singleplayer";
		ServerData server = mc.getCurrentServer();
		return server == null ? "Not connected" : server.ip;
	}

	@Override
	protected String getPreviewText() {
		return "play.example.net";
	}
}
