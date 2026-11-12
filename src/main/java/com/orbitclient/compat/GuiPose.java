package com.orbitclient.compat;

//? if <1.21.6 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.orbitclient.util.PoseUtil;
import net.minecraft.client.gui.GuiGraphics;

/^*
 * Before 1.21.6: GuiGraphics.pose() was a 3D PoseStack, where 1.21.6 made it a 2D matrix stack. This gives
 * Orbit's menus and HUD the 2D calls on the old stack; build.gradle turns g.pose() into GuiPose.of(g).
 ^/
public final class GuiPose {
	/^* The last one made: every draw call asks, and there's one GUI pose stack, so it's normally reused. ^/
	private static GuiPose last;
	private final PoseStack pose;

	private GuiPose(PoseStack pose) {
		this.pose = pose;
	}

	public static GuiPose of(GuiGraphics gui) {
		PoseStack pose = gui.pose();
		GuiPose cached = last;
		if (cached == null || cached.pose != pose) last = cached = new GuiPose(pose);
		return cached;
	}

	public GuiPose pushMatrix() {
		pose.pushPose();
		return this;
	}

	public GuiPose popMatrix() {
		pose.popPose();
		return this;
	}

	public GuiPose translate(float x, float y) {
		pose.translate(x, y, 0);
		return this;
	}

	public GuiPose scale(float x, float y) {
		pose.scale(x, y, 1);
		return this;
	}

	/^* Radians, clockwise on screen like 1.21.6's. ^/
	public GuiPose rotate(float angle) {
		PoseUtil.mulPose(pose, Axis.ZP.rotation(angle));
		return this;
	}
}
*///?}
