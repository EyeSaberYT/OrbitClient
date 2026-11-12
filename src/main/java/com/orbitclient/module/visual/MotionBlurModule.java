package com.orbitclient.module.visual;

import com.orbitclient.OrbitClient;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
//? if >=1.21.6 {
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
//?}
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

	/** One post-effect per strength, motion_blur_1 to motion_blur_10 (named once, not every frame). */
	private static final Identifier[] SHADERS = java.util.stream.IntStream.rangeClosed(1, 10)
		.mapToObj(level -> Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "motion_blur_" + level))
		.toArray(Identifier[]::new);

	// Only registered from 1.21.6 (ModuleManager): post effects can't keep an image between frames before that.
	//? if >=1.21.6 {
	/** Called right after the world is rendered, before the HUD and menus. */
	public static void apply(GraphicsResourceAllocator allocator) {
		MotionBlurModule m = instance;
		Minecraft mc = Minecraft.getInstance();
		if (m == null || !m.isEnabled() || mc.level == null || (mc.screen != null && !m.inMenus.isOn())) return;
		int level = Math.max(1, Math.min(10, (int) Math.round(m.strength.get())));
		PostChain chain = mc.getShaderManager().getPostChain(SHADERS[level - 1], LevelTargetBundle.MAIN_TARGETS);
		if (chain == null) return;
		chain.process(mc.getMainRenderTarget(), allocator);
		// The effect blends into "next" and shows that. Swapping its two kept images makes this frame's result
		// the next frame's history without copying the screen a third time.
		var kept = ((com.orbitclient.mixin.PostChainAccessor) chain).orbit$persistentTargets();
		if (kept.size() == 2) {
			var ids = kept.keySet().toArray(new Identifier[0]);
			kept.put(ids[0], kept.put(ids[1], kept.get(ids[0])));
		}
	}
	//?}
}
