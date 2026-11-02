package com.orbitclient.module.hud;

import com.orbitclient.util.Render2D;
import net.minecraft.world.item.Items;
import com.orbitclient.module.HudModule;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.setting.ModeSetting;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class CoordinatesModule extends HudModule {
	private static final int PADDING = 4;

	private final ModeSetting layout;
	private final BooleanSetting showDirection;
	private final BooleanSetting background;
	private final ColorSetting backgroundColor;
	private final ColorSetting labelColor;
	private final ColorSetting valueColor;

	public CoordinatesModule() {
		super("Coordinates", "Shows your X, Y and Z position.", 0.005, 0.37);
		setIcon(Items.MAP);
		layout = add(new ModeSetting("Layout", "Vertical", "Vertical", "Horizontal"));
		showDirection = add(new BooleanSetting("Show Direction", true));
		background = add(new BooleanSetting("Background", true));
		backgroundColor = add(new ColorSetting("Background Color", 0x6F000000).visibleWhen(background::isOn));
		labelColor = add(new ColorSetting("Label Color", 0xFF55AAFF));
		valueColor = add(new ColorSetting("Value Color", 0xFFFFFFFF));
	}

	private List<String[]> lines() {
		int x = 0, y = 64, z = 0;
		String facing = "North";
		if (mc.player != null) {
			x = mc.player.getBlockX();
			y = mc.player.getBlockY();
			z = mc.player.getBlockZ();
			String name = mc.player.getDirection().getName();
			facing = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
		List<String[]> lines = new ArrayList<>();
		if (layout.is("Horizontal")) {
			lines.add(new String[]{"XYZ: ", x + ", " + y + ", " + z});
		} else {
			lines.add(new String[]{"X: ", String.valueOf(x)});
			lines.add(new String[]{"Y: ", String.valueOf(y)});
			lines.add(new String[]{"Z: ", String.valueOf(z)});
		}
		if (showDirection.isOn()) lines.add(new String[]{"Facing: ", facing});
		return lines;
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		if (background.isOn()) Render2D.fill(graphics, 0, 0, getWidth(), getHeight(), backgroundColor.getColor());
		int y = PADDING;
		for (String[] line : lines()) {
			Render2D.text(graphics, mc.font, line[0], PADDING, y, labelColor.getColor(), true);
			Render2D.text(graphics, mc.font, line[1], PADDING + mc.font.width(line[0]), y, valueColor.getColor(), true);
			y += mc.font.lineHeight + 1;
		}
	}

	@Override
	public int getWidth() {
		int max = 0;
		for (String[] line : lines()) max = Math.max(max, mc.font.width(line[0] + line[1]));
		return Math.max(60, max + PADDING * 2);
	}

	@Override
	public int getHeight() {
		return lines().size() * (mc.font.lineHeight + 1) + PADDING * 2 - 1;
	}
}
