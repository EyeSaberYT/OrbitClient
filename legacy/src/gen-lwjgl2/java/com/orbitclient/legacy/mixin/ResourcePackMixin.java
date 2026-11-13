package com.orbitclient.legacy.mixin;

import com.orbitclient.legacy.OrbitResourcePack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Minecraft 1.8.9 to 1.12.2: Orbit's own assets are a resource pack of their own (see OrbitResourcePack). */
@Mixin(MinecraftClient.class)
public class ResourcePackMixin {
	@Shadow
	@Final
	private List<ResourcePack> resourcePacks;

	@Inject(method = "reloadResources", at = @At("HEAD"))
	private void orbit$assets(CallbackInfo ci) {
		if (!resourcePacks.contains(OrbitResourcePack.INSTANCE)) resourcePacks.add(OrbitResourcePack.INSTANCE);
	}
}
