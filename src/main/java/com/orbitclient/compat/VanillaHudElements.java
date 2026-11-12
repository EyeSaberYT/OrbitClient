package com.orbitclient.compat;

//? if <1.21.6 {
/*import net.minecraft.resources.Identifier;

/^* Before 1.21.6: the vanilla HUD parts HudRenderer attaches to or replaces (see HudElementRegistry). ^/
public final class VanillaHudElements {
	public static final Identifier CROSSHAIR = id("crosshair");
	public static final Identifier SCOREBOARD = id("scoreboard");
	public static final Identifier BOSS_BAR = id("boss_bar");
	public static final Identifier PLAYER_LIST = id("player_list");
	public static final Identifier TITLE_AND_SUBTITLE = id("title_and_subtitle");
	public static final Identifier CHAT = id("chat");
	public static final Identifier STATUS_EFFECTS = id("status_effects");
	public static final Identifier FOOD_BAR = id("food_bar");

	private VanillaHudElements() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("minecraft", path);
	}
}
*///?}
