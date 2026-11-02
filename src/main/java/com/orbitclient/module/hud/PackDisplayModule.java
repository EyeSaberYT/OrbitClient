package com.orbitclient.module.hud;

import com.orbitclient.module.TextHudModule;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class PackDisplayModule extends TextHudModule {
	public PackDisplayModule() {
		super("Pack Display", "Shows the resource pack you are using.", 0.30, 0.10);
		setIcon(Items.PAINTING);
	}

	@Override
	protected String getText() {
		List<Pack> packs = new ArrayList<>(mc.getResourcePackRepository().getSelectedPacks());
		// The last selected pack is the one on top, which wins.
		for (int i = packs.size() - 1; i >= 0; i--) {
			String id = packs.get(i).getId();
			if (id.equals("vanilla") || id.startsWith("fabric") || id.equals("orbitclient")) continue;
			return packs.get(i).getTitle().getString();
		}
		return "Default";
	}
}
