package com.orbitclient.cosmetics;

import com.orbitclient.OrbitClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * One wearable cosmetic. Capes are textures (drawn by the cape layer); everything else draws itself
 * from voxel models, attached to the head or body of the player model.
 */
public final class Cosmetic {
	static final Identifier WHITE = Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "textures/cosmetics/white.png");

	// Made on first draw, so the cosmetics can be loaded without the game's renderer (scripts/export-cosmetics).
	private static final class Voxels {
		static final RenderType TYPE = RenderTypes.entityCutoutNoCull(WHITE);
	}

	/** Everything a cosmetic needs to draw itself for one player this frame. */
	public record Context(PoseStack pose, SubmitNodeCollector collector, int light, PlayerModel model,
						  AvatarRenderState state, int color, double time, float delta) {
		/** Moves the pose onto a body part (push; the caller pops). */
		public void attach(ModelPart part) {
			pose.pushPose();
			model.root().translateAndRotate(pose);
			part.translateAndRotate(pose);
		}

		public void draw(VoxelModel voxels) {
			draw(voxels, color);
		}

		public void draw(VoxelModel voxels, int tint) {
			collector.submitCustomGeometry(pose, Voxels.TYPE, (p, consumer) -> voxels.render(p, consumer, light, tint, 1f));
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
	/** Hats: how far (in blocks) the hat rises above the head, so the nametag can move up past it. */
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

	//? if <1.21 {
	/*/^* Development check (MixinAudit): draws every cosmetic's model once into the entity buffer, the way players' are drawn. ^/
	public static int drawAllOnce(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
		PoseStack pose = new PoseStack();
		com.mojang.blaze3d.vertex.VertexConsumer consumer = buffers.getBuffer(Voxels.TYPE);
		int drawn = 0;
		for (Cosmetic c : Cosmetics.all()) {
			if (c.iconModel == null) continue;
			c.iconModel.render(pose.last(), consumer, VoxelModel.FULL_BRIGHT, c.defaultColor, 1f);
			drawn++;
		}
		buffers.endBatch();
		return drawn;
	}
	*///?}

	/** The cape texture to show right now (capes animate through their frames). */
	public Identifier capeTexture(double time) {
		int frame = capeFrames <= 1 ? 0 : (int) Math.floor(time * 10) % capeFrames;
		return Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "textures/cosmetics/capes/" + id + "_" + frame + ".png");
	}
}
