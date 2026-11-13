package com.orbitclient.cosmetics;

import com.orbitclient.legacy.OrbitLegacy;
import com.orbitclient.legacy.gl.PoseStack;
import com.orbitclient.legacy.gl.VertexConsumer;
import com.orbitclient.legacy.render.PlayerParts;
import com.orbitclient.legacy.render.PlayerState;
import net.minecraft.util.Identifier;

/**
 * One wearable cosmetic, for the old engine. The same shape as the main client's Cosmetic, so the
 * shared art (Hats, Backs, Wings, ...) compiles against it unchanged: capes are textures, everything
 * else draws itself from voxel models attached to the head or body of the player model.
 */
public final class Cosmetic {
	public static final Identifier WHITE = new Identifier(OrbitLegacy.MOD_ID, "textures/cosmetics/white.png");

	/** Everything a cosmetic needs to draw itself for one player this frame. */
	public record Context(PoseStack pose, VertexConsumer collector, int light, PlayerParts model,
	                      PlayerState state, int color, double time, float delta) {
		/** Moves the pose onto a body part (push; the caller pops). */
		public void attach(PlayerParts.Part part) {
			pose.pushPose();
			model.root().translateAndRotate(pose);
			part.translateAndRotate(pose);
		}

		public void draw(VoxelModel voxels) {
			draw(voxels, color);
		}

		public void draw(VoxelModel voxels, int tint) {
			collector.draw(voxels, tint);
		}
	}

	public interface Renderer {
		void render(Context ctx);
	}

	public final String id;
	public final String name;
	public final CosmeticSlot slot;
	public final int defaultColor;
	public final boolean colorable;
	final Renderer renderer;
	/** Capes only: the animation frames of the cape texture. */
	final int capeFrames;
	/** The model drawn as this cosmetic's picture in menus (null for capes), and whether it's a mirrored pair. */
	VoxelModel iconModel;
	boolean iconPair;
	/** Hats: how far (in blocks) the hat rises above the head. */
	float nameLift;
	/** Whether it moves (capes animate, wings flap, pets bob); false for cosmetics that just sit still. */
	boolean animated = true;

	Cosmetic(String id, String name, CosmeticSlot slot, int defaultColor, boolean colorable, Renderer renderer, int capeFrames) {
		this.id = id;
		this.name = name;
		this.slot = slot;
		this.defaultColor = defaultColor;
		this.colorable = colorable;
		this.renderer = renderer;
		this.capeFrames = capeFrames;
	}

	public Renderer renderer() {
		return renderer;
	}

	public VoxelModel iconModel() {
		return iconModel;
	}

	public boolean iconPair() {
		return iconPair;
	}

	/** The cape texture to show right now (capes animate through their frames). */
	public Identifier capeTexture(double time) {
		int frame = capeFrames <= 1 ? 0 : (int) Math.floor(time * 10) % capeFrames;
		return new Identifier(OrbitLegacy.MOD_ID, "textures/cosmetics/capes/" + id + "_" + frame + ".png");
	}
}
