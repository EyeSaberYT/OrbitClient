package com.orbitclient.legacy.render;

import com.orbitclient.legacy.gl.PoseStack;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.model.ModelPart;

/**
 * The player model as the shared cosmetic art sees it: a head and a body to attach things to, and a
 * root that everything hangs from. Wraps the old engine's model for one draw.
 */
public final class PlayerParts {
	/** One part of the model, or the root. */
	public static final class Part {
		private final ModelPart part;
		private final boolean sneaking;

		private Part(ModelPart part, boolean sneaking) {
			this.part = part;
			this.sneaking = sneaking;
		}

		/** Moves the pose to this part's pivot and turns it the way the part is turned right now. */
		public void translateAndRotate(PoseStack pose) {
			if (part == null) {
				// The root. While sneaking the whole model is drawn a little lower (the vanilla layers do the same).
				if (sneaking) pose.translate(0f, 0.2f, 0f);
				return;
			}
			part.preRender(0.0625f);
		}
	}

	public final Part head;
	public final Part body;
	private final Part root;

	public PlayerParts(BiPedModel model, boolean sneaking) {
		this.head = new Part(model.head, sneaking);
		this.body = new Part(model.body, sneaking);
		this.root = new Part(null, sneaking);
	}

	public Part root() {
		return root;
	}
}
