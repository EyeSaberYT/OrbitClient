package com.orbitclient.test;

import com.orbitclient.module.ModuleManager;
import com.orbitclient.module.visual.GlintColorizerModule;
import com.orbitclient.setting.ColorSetting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

/**
 * Glint Colorizer with a chroma color: the tinted glint is made once and recolored in place (not a new
 * texture every tick), and turning the mod off gives the glint back to Minecraft.
 */
@SuppressWarnings("UnstableApiUsage")
public class GlintColorizerTest implements FabricClientGameTest {
	private static final Identifier GLINT = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");

	@Override
	public void runTest(ClientGameTestContext context) {
		if (Boolean.getBoolean("orbit.perfTest")) return;  // a PerformanceTest run
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			GlintColorizerModule glint = ModuleManager.get(GlintColorizerModule.class);
			context.runOnClient(mc -> {
				glint.getSettings().stream().filter(s -> s instanceof ColorSetting).forEach(s -> ((ColorSetting) s).setChroma(true));
				glint.setEnabled(true);
			});
			context.waitTicks(3);
			AbstractTexture first = context.computeOnClient(mc -> mc.getTextureManager().getTexture(GLINT));
			context.waitTicks(40);
			AbstractTexture later = context.computeOnClient(mc -> mc.getTextureManager().getTexture(GLINT));
			if (!first.getClass().getSimpleName().equals("GlintTexture")) {
				throw new AssertionError("The tinted glint isn't in place: " + first.getClass());
			}
			if (later != first) throw new AssertionError("A chroma glint made a new texture instead of recoloring its own");

			context.runOnClient(mc -> glint.setEnabled(false));
			context.waitTicks(3);
			AbstractTexture back = context.computeOnClient(mc -> mc.getTextureManager().getTexture(GLINT));
			if (back.getClass().getSimpleName().equals("GlintTexture")) throw new AssertionError("The tinted glint stayed after turning the mod off");
			context.runOnClient(mc -> glint.getSettings().stream().filter(s -> s instanceof ColorSetting).forEach(s -> ((ColorSetting) s).setChroma(false)));
			System.out.println("[GLINT] Chroma glint recolored in place; Minecraft's glint is back after turning it off (" + back.getClass().getSimpleName() + ")");
		}
	}
}
