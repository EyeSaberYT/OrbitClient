package com.orbitclient.compat;

//? if <1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/^*
 * Before 1.21.9: the part of 1.21.9's net.minecraft.client.renderer.SubmitNodeCollector that Orbit draws
 * through (custom geometry), drawn straight into the old buffers. The cosmetics, the cape and Item Physics
 * use it unchanged; build.gradle swaps the import.
 ^/
public final class SubmitNodeCollector {
	private final MultiBufferSource buffers;

	public SubmitNodeCollector(MultiBufferSource buffers) {
		this.buffers = buffers;
	}

	public MultiBufferSource buffers() {
		return buffers;
	}

	public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
		renderer.render(pose.last(), buffers.getBuffer(type));
	}

	public interface CustomGeometryRenderer {
		void render(PoseStack.Pose pose, VertexConsumer consumer);
	}
}
*///?}
