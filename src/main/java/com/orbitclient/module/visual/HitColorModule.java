package com.orbitclient.module.visual;

import com.orbitclient.mixin.OverlayTextureAccessor;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.item.Items;

/**
 * Changes the tint entities flash when they're hurt. The tint comes from a small overlay texture
 * whose top half is the hurt color; this rewrites those pixels.
 */
public class HitColorModule extends Module {
	private static final int VANILLA = 0xB2FF0000;
	private final ColorSetting color;
	private int applied = VANILLA;

	public HitColorModule() {
		super("Hit Color", "Changes the color entities flash when you hit them.", Category.MECHANIC);
		setIcon(Items.RED_DYE);
		color = add(new ColorSetting("Color", 0x9955AAFF));
	}

	@Override
	public void onTick() {
		write(color.getColor());
	}

	@Override
	protected void onDisable() {
		write(VANILLA);
	}

	private void write(int argb) {
		if (argb == applied) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null) return;
		DynamicTexture texture = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).orbit$texture();
		var pixels = texture.getPixels();
		if (pixels == null) return;
		for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) pixels.setPixel(x, y, argb);
		texture.upload();
		applied = argb;
	}
}
