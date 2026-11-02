package com.orbitclient.waypoint;

import java.awt.Color;

public class Waypoint {
	/** Marker icons you can pick, and the menu icon each one uses. */
	public static final String[] ICONS = {"Pin", "Home", "Flag", "Diamond", "Star", "Skull", "Sword"};
	private static final String[] ICON_TEXTURES = {"waypoints", "wp_home", "wp_flag", "wp_diamond", "ui_star", "boss_bar", "wp_sword"};

	public String name;
	public int x, y, z;
	/** Which server or singleplayer world this belongs to, e.g. "server:play.example.net". */
	public String world;
	/** Dimension id, e.g. "minecraft:overworld". */
	public String dimension;
	/** Which world inside that server (see WaypointManager); null for waypoints saved before worlds existed. */
	public String subWorld;
	public int color;
	/** Rainbow color that shifts over time instead of the fixed color. */
	public boolean chroma;
	public String icon = "Pin";
	public boolean beam = true;
	/** Show a marker with an arrow at the screen edge when this waypoint is off-screen. */
	public boolean edge = true;
	public boolean visible = true;

	public Waypoint(String name, int x, int y, int z, String world, String dimension, int color) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.dimension = dimension;
		this.color = color;
	}

	/** The color to draw with right now (opaque), following chroma if it's on. */
	public int currentColor() {
		if (!chroma) return color | 0xFF000000;
		float hue = (System.currentTimeMillis() % 4000L) / 4000f;
		return Color.HSBtoRGB(hue, 0.75f, 1f) | 0xFF000000;
	}

	public String iconTexture() {
		for (int i = 0; i < ICONS.length; i++) if (ICONS[i].equals(icon)) return ICON_TEXTURES[i];
		return ICON_TEXTURES[0];
	}
}
