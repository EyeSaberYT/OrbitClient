package com.orbitclient.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Adds the mixins whose target only exists in some Minecraft versions; orbitclient.mixins.json lists the rest. */
public class OrbitMixinPlugin implements IMixinConfigPlugin {
	@Override
	public List<String> getMixins() {
		List<String> mixins = new ArrayList<>();
		//? if >=26.1 {
		/*mixins.add("ClientClockMixin");
		*///?}
		return mixins;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		//? if >=26.2 {
		/*// Only SessionSwap uses it, and that's not in 26.2 and newer.
		if (mixinClassName.endsWith(".MinecraftAccountAccessor")) return false;
		*///?}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
