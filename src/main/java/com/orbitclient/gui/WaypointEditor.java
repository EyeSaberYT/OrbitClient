package com.orbitclient.gui;

import com.orbitclient.setting.*;
import com.orbitclient.waypoint.Waypoint;
import com.orbitclient.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * One waypoint's properties as settings, so the standard settings list can edit it, including the
 * full color picker (hue, saturation/brightness, alpha, chroma). {@link #apply()} copies edits back.
 */
final class WaypointEditor {
	/** Quick colors shown as swatches above the picker. */
	static final int[] PALETTE = {0xFF55AAFF, 0xFF3FD07A, 0xFFE5484D, 0xFFF0A93A, 0xFFF5D547, 0xFFA06CF0, 0xFFF05A7E, 0xFF55FFFF, 0xFFFFFFFF};

	final Waypoint waypoint;
	private final TextSetting name;
	final ColorSetting color;
	private final ModeSetting icon;
	private final BooleanSetting visible;
	private final BooleanSetting beam;
	private final BooleanSetting edge;
	private final List<Setting<?>> settings;

	WaypointEditor(Waypoint wp) {
		this.waypoint = wp;
		name = new TextSetting("Name", wp.name, 32);
		color = new ColorSetting("Color", wp.color | 0xFF000000);
		color.setChroma(wp.chroma);
		icon = new ModeSetting("Icon", contains(wp.icon) ? wp.icon : Waypoint.ICONS[0], Waypoint.ICONS);
		visible = new BooleanSetting("Visible", wp.visible);
		beam = new BooleanSetting("Beam", wp.beam);
		edge = new BooleanSetting("Show at Screen Edge", wp.edge);
		ActionSetting moveHere = new ActionSetting("Position", "Move Here", this::moveHere, false);
		settings = List.of(name, color, icon, visible, beam, edge, moveHere);
	}

	private static boolean contains(String icon) {
		for (String s : Waypoint.ICONS) if (s.equals(icon)) return true;
		return false;
	}

	List<Setting<?>> settings() {
		return settings;
	}

	/** Copies the edited values to the waypoint (called every frame while editing). */
	void apply() {
		if (!name.get().isBlank()) waypoint.name = name.get().trim();
		waypoint.color = color.get() | 0xFF000000;
		waypoint.chroma = color.isChroma();
		waypoint.icon = icon.get();
		waypoint.visible = visible.isOn();
		waypoint.beam = beam.isOn();
		waypoint.edge = edge.isOn();
	}

	void setColor(int argb) {
		color.set(argb | 0xFF000000);
		color.setChroma(false);
	}

	private void moveHere() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		waypoint.x = mc.player.getBlockX();
		waypoint.y = mc.player.getBlockY();
		waypoint.z = mc.player.getBlockZ();
		String dim = WaypointManager.currentDimension();
		if (dim != null) waypoint.dimension = dim;
	}
}
