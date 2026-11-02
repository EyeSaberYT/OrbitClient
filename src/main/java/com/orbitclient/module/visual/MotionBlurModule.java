package com.orbitclient.module.visual;

import com.orbitclient.OrbitClient;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * Motion blur: right after the world is drawn (before the HUD, so text and the crosshair stay sharp),
 * a post-processing shader blends the new frame with the previous ones it keeps. Fast camera turns
 * leave a smooth trail; standing still looks exactly like normal. Each strength level is its own
 * post effect (post_effect/motion_blur_N.json), since post shader settings are fixed in the file.
 */
public class MotionBlurModule extends Module {
	private static MotionBlurModule instance;

	public final NumberSetting strength;
	private final BooleanSetting inMenus;

	public MotionBlurModule() {
		super("Motion Blur", "Smooth trails when you move the camera quickly.", Category.MECHANIC);
		setIcon(Items.FEATHER);
		instance = this;
		strength = add(new NumberSetting("Strength", 5, 1, 10, 1));
		inMenus = add(new BooleanSetting("Blur With Menus Open", false));
	}

	/** Called right after the world is rendered, before the HUD and menus. */
	public static void apply(GraphicsResourceAllocator allocator) {
		MotionBlurModule m = instance;
		Minecraft mc = Minecraft.getInstance();
		if (m == null || !m.isEnabled() || mc.level == null || (mc.screen != null && !m.inMenus.isOn())) return;
		int level = Math.max(1, Math.min(10, (int) Math.round(m.strength.get())));
		Identifier id = Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "motion_blur_" + level);
		PostChain chain = mc.getShaderManager().getPostChain(id, LevelTargetBundle.MAIN_TARGETS);
		if (chain != null) chain.process(mc.getMainRenderTarget(), allocator);
	}
}
