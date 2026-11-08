package com.orbitclient.test;

import com.orbitclient.module.Module;
import com.orbitclient.module.ModuleManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.authlib.GameProfile;
import com.google.gson.JsonObject;
import com.orbitclient.cosmetics.Cosmetic;
import com.orbitclient.cosmetics.CosmeticManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.cosmetics.Cosmetics;
import net.minecraft.client.CameraType;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.player.RemotePlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What each Orbit mod costs per frame (`gradlew :1.21.11:runClientGameTest -PperfTest`, only this test runs).
 * Frame rate is uncapped; the player stands still so only the mods change between measurements.
 * Prints [PERF] lines: milliseconds per frame over the run with every Orbit mod off.
 */
@SuppressWarnings("UnstableApiUsage")
public class PerformanceTest implements FabricClientGameTest {
	private static final int SAMPLES = 5;
	private static final int PLAYERS = 40;
	private static final List<UUID> DRESSED = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!Boolean.getBoolean("orbit.perfTest")) return;
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(mc -> {
			mc.options.framerateLimit().set(260);  // 260 is "Unlimited"
			mc.options.enableVsync().set(false);
			// Otherwise the game drops to 30 FPS after a while without input, which a test never gives it.
			mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
		});
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			Map<Module, Boolean> setup = context.computeOnClient(mc -> {
				Map<Module, Boolean> states = new LinkedHashMap<>();
				for (Module module : ModuleManager.all()) states.put(module, module.isEnabled());
				return states;
			});
			context.waitTicks(100);  // let chunk building and the JIT settle

			setAll(context, setup, false);
			double off = frameMs(context);
			restore(context, setup);
			double mine = frameMs(context);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			setAll(context, setup, false);
			double offThird = frameMs(context);
			restore(context, setup);
			double mineThird = frameMs(context);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			report("All Orbit mods off", off, off);
			report("Your Orbit setup (" + setup.values().stream().filter(b -> b).count() + " mods on)", mine, off);
			report("All Orbit mods off, third person", offThird, offThird);
			report("Your Orbit setup, third person", mineThird, offThird);

			// A lobby: other players in view, half of them Orbit players wearing something in every cosmetic slot.
			context.runOnClient(mc -> {
				var level = mc.level;
				var me = mc.player;
				float yaw = (float) Math.toRadians(me.getYRot());
				double forwardX = -Math.sin(yaw), forwardZ = Math.cos(yaw);
				for (int i = 0; i < PLAYERS; i++) {
					double ahead = 4 + (i / 8) * 2.5, across = (i % 8 - 3.5) * 1.6;
					UUID id = UUID.randomUUID();
					RemotePlayer other = new RemotePlayer(level, new GameProfile(id, "Player" + i));
					other.setPos(me.getX() + forwardX * ahead - forwardZ * across, me.getY(), me.getZ() + forwardZ * ahead + forwardX * across);
					other.setYRot(me.getYRot() + 180);
					other.yBodyRot = other.yHeadRot = me.getYRot() + 180;
					level.addEntity(other);
					if (i % 2 == 0) {
						CosmeticManager.setOther(id, fullLoadout());
						DRESSED.add(id);
					}
				}
			});
			setAll(context, setup, false);
			double offLobby = frameMs(context);
			restore(context, setup);
			double mineLobby = frameMs(context);
			report("All Orbit mods off, " + PLAYERS + " players in view", offLobby, offLobby);
			report("Your Orbit setup, " + PLAYERS + " players in view", mineLobby, offLobby);
			// One cosmetic slot at a time, measured twice in a row: what each costs, and whether it builds up.
			for (CosmeticSlot slot : CosmeticSlot.values()) {
				context.runOnClient(mc -> DRESSED.forEach(id -> CosmeticManager.setOther(id, new CosmeticManager.Loadout())));
				double bare = frameMs(context);
				context.runOnClient(mc -> DRESSED.forEach(id -> CosmeticManager.setOther(id, loadout(slot))));
				double first = frameMs(context);
				double second = frameMs(context);
				double third = frameMs(context);
				System.out.printf("[PERF] slot %-6s on %d players: bare %.3f ms, worn %.3f -> %.3f -> %.3f ms%n",
					slot, DRESSED.size(), bare, first, second, third);
			}
			context.runOnClient(mc -> DRESSED.forEach(id -> CosmeticManager.setOther(id, new CosmeticManager.Loadout())));
			report("No cosmetics again at the end", frameMs(context), offLobby);
			restore(context, setup);
		}
	}

	/** The first cosmetic of every slot: a cape, hat, face, back item, wings, aura and pet. */
	private static CosmeticManager.Loadout fullLoadout() {
		return loadout(CosmeticSlot.values());
	}

	private static CosmeticManager.Loadout loadout(CosmeticSlot... slots) {
		JsonObject json = new JsonObject();
		for (CosmeticSlot slot : slots) {
			List<Cosmetic> options = Cosmetics.in(slot);
			if (options.isEmpty()) continue;
			JsonObject worn = new JsonObject();
			worn.addProperty("id", options.get(0).id);
			json.add(slot.name(), worn);
		}
		return CosmeticManager.Loadout.fromJson(json);
	}

	private static void setAll(ClientGameTestContext context, Map<Module, Boolean> setup, boolean on) {
		context.runOnClient(mc -> setup.keySet().forEach(module -> module.setEnabled(on)));
		context.waitTicks(10);
	}

	private static void restore(ClientGameTestContext context, Map<Module, Boolean> setup) {
		context.runOnClient(mc -> setup.forEach(Module::setEnabled));
		context.waitTicks(10);
	}

	/** Average frame time in milliseconds, from the game's frames-per-second counter (one reading a second). */
	private static double frameMs(ClientGameTestContext context) {
		context.waitTicks(40);
		double total = 0;
		for (int i = 0; i < SAMPLES; i++) {
			context.waitTicks(20);
			total += 1000.0 / Math.max(1, context.computeOnClient(mc -> mc.getFps()));
		}
		return total / SAMPLES;
	}

	private static void report(String label, double ms, double baseline) {
		System.out.printf("[PERF] %-48s %6.3f ms/frame (%4.0f FPS)  %+6.3f ms vs off%n", label, ms, 1000 / ms, ms - baseline);
	}
}
