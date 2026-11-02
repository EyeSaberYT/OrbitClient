package com.orbitclient.test;

import com.orbitclient.util.Shapes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Test-only: arcs of different sizes, sweeps and rotations, to check the shape textures. */
class ShapeDebugScreen extends Screen {
	ShapeDebugScreen() {
		super(Component.literal("Shapes"));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0xFF101010);
		float[] sweeps = {11, 30, 78, 180};
		for (int i = 0; i < sweeps.length; i++) {
			float cx = 40 + i * 60, sweep = (float) Math.toRadians(sweeps[i]);
			// Small radius (like the corona) at four rotations, and a large one.
			for (int r = 0; r < 4; r++) {
				float start = (float) (r * Math.PI / 2);
				Shapes.arc(g, cx, 50, 20, 26, start, start + sweep, 0xFFFFFFFF);
			}
			Shapes.arc(g, cx, 130, 22, 25, 0.3f, 0.3f + sweep, 0xFF55FF55);
			Shapes.ring(g, cx, 130, 10, 12, 0xFF5599FF);
		}
		// The eclipse corona exactly as OrbitScreen draws it, built up step by step left to right:
		// 1) corona arcs only, 2) + glow rings underneath, 3) + dark disc on top.
		float tau = (float) (Math.PI * 2);
		for (int step = 0; step < 3; step++) {
			float cx = 300 + step * 70, cy = 60, r = 22;
			if (step >= 1) {
				for (int k = 0; k < 6; k++) {
					Shapes.ring(g, cx, cy, r + k * 2.2f, r + k * 2.2f + 2.2f, (Math.round(0.2f * (1 - k / 6f) * 255) << 24) | 0x3FD07A);
				}
			}
			for (int i = 0; i < 18; i++) {
				float a0 = i * tau / 18, flare = 0.5f + 0.5f * (float) Math.sin(i * 1.7f);
				int alpha = Math.round((0.35f + 0.4f * flare) * 255);
				Shapes.arc(g, cx, cy, r + 0.5f, r + 2.5f + flare * 3f, a0, a0 + tau / 18 * 0.55f, (alpha << 24) | 0x9FDC8F);
			}
			if (step >= 2) Shapes.disc(g, cx, cy, r, 0xFF06070A);
		}
	}
}
