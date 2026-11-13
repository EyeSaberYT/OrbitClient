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
		//? if >=26.2 {
		/*mixins.add("GuiScreenMixin");
		*///?}
		//? if >=1.21.6
		mixins.add("PostChainAccessor");
		//? if >=1.20.5 <1.21.6 {
		/*mixins.add("GuiHudMixin");
		*///?}
		//? if >=1.20 <1.20.5 {
		/*mixins.add("GuiHudLegacyMixin");
		*///?}
		//? if >=1.16 <1.20 {
		/*mixins.add("GuiHudOldMixin");
		*///?}
		//? if <1.16 {
		/*mixins.add("GuiHud115Mixin");
		*///?}
		//? if >=1.21.6 {
		mixins.add("FontMixin");
		//?}
		//? if >=1.15.2 {
		mixins.add("WindowTitleMixin");
		//?}
		//? if <1.16.1 {
		/*mixins.add("PlayerRendererLayerMixin");
		mixins.add("OwnEventsMixin");
		mixins.add("OwnEventsItemMixin");
		*///?}
		//? if >=1.19 {
		mixins.add("OptionInstanceMixin");
		//?} else {
		/*mixins.add("LightTextureGammaMixin");
		mixins.add("MouseSensitivityMixin");
		*///?}
		//? if <1.21.6 {
		/*mixins.add("BossHealthOverlayMixin");
		*///?}
		// Before 1.19.3 there's no renderWithTooltip to hook: Orbit's screens draw their background themselves.
		//? if >=1.19.3 <1.20.2 {
		/*mixins.add("ScreenBackgroundMixin");
		*///?}
		//? if >=1.21.5 <1.21.6 {
		/*mixins.add("ItemEntityRendererAccessor");
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
		//? if <1.21.6 {
		/*// Before 1.21.6 text isn't laid out with Font.prepareText, so there's nothing for the text cache to reuse.
		if (mixinClassName.endsWith(".FontMixin")) return false;
		*///?}
		//? if <1.20.5 {
		/*// Menu Blur uses vanilla's menu blur, which is new in 1.20.5.
		if (mixinClassName.endsWith(".AbstractContainerScreenMixin")) return false;
		*///?}
		//? if >=26.2 || <1.20.3 {
		/*// Only SessionSwap uses it, which is only in 1.20.3 to 26.1.
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
