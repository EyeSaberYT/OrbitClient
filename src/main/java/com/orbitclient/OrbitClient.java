package com.orbitclient;

import com.orbitclient.config.ConfigManager;
import com.orbitclient.gui.OrbitScreen;
import com.orbitclient.gui.HudRenderer;
import com.orbitclient.module.ModuleManager;
import com.orbitclient.module.mechanics.ParticleMultiplierModule;
import com.orbitclient.util.CombatTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrbitClient implements ClientModInitializer {
	public static final String MOD_ID = "orbitclient";
	public static final Logger LOGGER = LoggerFactory.getLogger("Orbit Client");

	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static KeyMapping menuKey;
	public static KeyMapping zoomKey;

	@Override
	public void onInitializeClient() {
		// Before the keys register and options.txt loads, so rebinds from Eclipse Client carry over.
		ConfigManager.migrateFromEclipse();
		menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.orbitclient.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, CATEGORY));
		zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.orbitclient.zoom", InputConstants.Type.KEYSYM, InputConstants.KEY_C, CATEGORY));

		ModuleManager.init();
		ConfigManager.load();
		HudRenderer.register();
		// Cosmetics on every player model (the cape is handled by the cape layer).
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
				@SuppressWarnings("unchecked")
				var parent = (net.minecraft.client.renderer.entity.RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState, net.minecraft.client.model.player.PlayerModel>) (Object) avatar;
				helper.register(new com.orbitclient.cosmetics.CosmeticsLayer(parent));
			}
		});
		com.orbitclient.cosmetics.CosmeticSync.init();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			com.orbitclient.cosmetics.CosmeticManager.tickTrails();
			com.orbitclient.cosmetics.CosmeticSync.tick();
			com.orbitclient.cape.ClothCape.tickAll(client);
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (menuKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new OrbitScreen());
			}
			CombatTracker.tick();
			ModuleManager.tick(client);
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide()) {
				CombatTracker.onAttack(player, entity);
				ModuleManager.get(ParticleMultiplierModule.class).onAttack(entity);
			}
			return InteractionResult.PASS;
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());
		LOGGER.info("Orbit Client loaded {} mods", ModuleManager.all().size());
	}
}
