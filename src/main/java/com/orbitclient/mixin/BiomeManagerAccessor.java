package com.orbitclient.mixin;

import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The hashed world seed the server sends; used to tell a server's worlds apart for waypoints. */
@Mixin(BiomeManager.class)
public interface BiomeManagerAccessor {
	@Accessor("biomeZoomSeed")
	long orbit$getSeed();
}
