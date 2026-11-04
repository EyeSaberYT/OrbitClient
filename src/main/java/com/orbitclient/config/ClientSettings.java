package com.orbitclient.config;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.setting.NumberSetting;
import com.orbitclient.setting.Setting;

import java.util.List;

/** Client-wide options shown in the SETTINGS tab. Shared by all profiles. */
public final class ClientSettings extends Module {
	public static final ClientSettings INSTANCE = new ClientSettings();

	public final BooleanSetting animations;
	public final NumberSetting animationSpeed;
	public final BooleanSetting animatedTabList;
	public final BooleanSetting chatAnimation;
	public final BooleanSetting copyChat;
	public final BooleanSetting showOthersCosmetics;
	public final BooleanSetting orbitBadges;
	public final BooleanSetting friendsOnline;
	public final BooleanSetting shareServer;
	public final BooleanSetting friendNotifications;
	public final BooleanSetting friendBadges;
	public final BooleanSetting smoothHotbar;
	/** Snap HUD mod scales so text never looks broken when made small. */
	public final BooleanSetting pixelPerfectHud;
	public final ModeSetting iconStyle;
	public final BooleanSetting menuBlur;
	public final BooleanSetting newBadges;
	public final BooleanSetting hudInDebug;
	public final BooleanSetting snapping;
	public final BooleanSetting rememberTab;

	private ClientSettings() {
		super("Settings", "Client settings", Category.HUD);
		animations = add(new BooleanSetting("Animations", true));
		animationSpeed = add(new NumberSetting("Animation Speed", 1.0, 0.5, 2.0, 0.1).visibleWhen(animations::isOn));
		animatedTabList = add(new BooleanSetting("Animated Tab List", true).visibleWhen(animations::isOn));
		chatAnimation = add(new BooleanSetting("Chat Slide Animation", true).visibleWhen(animations::isOn));
		copyChat = add(new BooleanSetting("Shift-Click to Copy Chat", true));
		showOthersCosmetics = add(new BooleanSetting("Show Others' Cosmetics", true));
		orbitBadges = add(new BooleanSetting("Show Orbit Badges", true));
		friendsOnline = add(new BooleanSetting("Friends: Appear Online", true));
		shareServer = add(new BooleanSetting("Friends: Show My Server", true).visibleWhen(friendsOnline::isOn));
		friendNotifications = add(new BooleanSetting("Friends: Notifications", true));
		friendBadges = add(new BooleanSetting("Friends: Green Badge For Friends", true));
		smoothHotbar = add(new BooleanSetting("Smooth Hotbar Selector", true).visibleWhen(animations::isOn));
		pixelPerfectHud = add(new BooleanSetting("Pixel-Perfect HUD Scaling", true));
		iconStyle = add(new ModeSetting("Mod Icons", "Custom", "Custom", "Items"));
		menuBlur = add(new BooleanSetting("Menu Blur", true));
		newBadges = add(new BooleanSetting("Show NEW Badges", true));
		hudInDebug = add(new BooleanSetting("Show HUD With Debug Screen (F3)", false));
		snapping = add(new BooleanSetting("Snap HUD Elements In Editor", true));
		rememberTab = add(new BooleanSetting("Remember Last Filter", true));
	}

	/** No toggle key for the settings page. */
	@Override
	public List<Setting<?>> getSettings() {
		List<Setting<?>> list = super.getSettings();
		list.remove(toggleKey);
		return list;
	}
}
