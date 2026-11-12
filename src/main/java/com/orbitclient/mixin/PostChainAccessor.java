package com.orbitclient.mixin;

//? if >=1.21.6 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** The images a post effect keeps between frames (Motion Blur swaps its two instead of copying one, see MotionBlurModule). */
@Mixin(PostChain.class)
public interface PostChainAccessor {
	@Accessor("persistentTargets")
	Map<Identifier, RenderTarget> orbit$persistentTargets();
}
//?}
