package com.orbitclient.module.server;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Hides your name (and optionally other players') in chat, the tab list and nametags, for
 * streaming and recording. Only changes what you see; nothing is sent to the server.
 */
public class NickHiderModule extends Module {
	private static NickHiderModule instance;
	private final TextSetting nick;
	private final BooleanSetting hideOthers;
	private final TextSetting othersNick;

	public NickHiderModule() {
		super("Nick Hider", "Hides your name in chat, the tab list and nametags.", Category.SERVER);
		setIcon(Items.PLAYER_HEAD);
		instance = this;
		nick = add(new TextSetting("Your Name Shows As", "You", 16));
		hideOthers = add(new BooleanSetting("Hide Other Names", false));
		othersNick = add(new TextSetting("Others Show As", "Player", 16).visibleWhen(hideOthers::isOn));
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** Replaces hidden names inside a text component, keeping colors and click/hover actions. */
	public static Component hide(Component component) {
		if (!active() || component == null) return component;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return component;
		String own = mc.player.getGameProfile().name();
		boolean others = instance.hideOthers.isOn() && mc.getConnection() != null;
		String text = component.getString();
		boolean any = text.contains(own);
		if (!any && others) {
			for (var info : mc.getConnection().getOnlinePlayers()) {
				if (text.contains(info.getProfile().name())) {
					any = true;
					break;
				}
			}
		}
		if (!any) return component;
		MutableComponent out = Component.empty();
		component.visit((style, part) -> {
			String replaced = part.replace(own, instance.nick.get());
			if (others) {
				for (var info : mc.getConnection().getOnlinePlayers()) {
					String name = info.getProfile().name();
					if (!name.equals(own) && name.length() > 2) replaced = replaced.replace(name, instance.othersNick.get());
				}
			}
			out.append(Component.literal(replaced).withStyle(style));
			return Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		return out;
	}
}
