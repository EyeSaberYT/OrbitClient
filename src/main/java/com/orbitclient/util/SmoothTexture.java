package com.orbitclient.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** A texture sampled with linear filtering, so it stays smooth when drawn slightly scaled or rotated. */
public final class SmoothTexture extends DynamicTexture {
	public SmoothTexture(String name, NativeImage image) {
		super(() -> "orbit " + name, image);
		this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
	}
}
