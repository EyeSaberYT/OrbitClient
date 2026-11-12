package com.orbitclient.mixin;

//? if >=1.21.5 <1.21.6 {
/*import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/^* 1.21.5 only: the dropped item's model bounds for Item Physics (public on the item render state from 1.21.6). ^/
@Mixin(ItemEntityRenderer.class)
public interface ItemEntityRendererAccessor {
	@Invoker("calculateModelBoundingBox")
	static AABB orbit$modelBoundingBox(ItemStackRenderState item) {
		throw new AssertionError();
	}
}
*///?}
