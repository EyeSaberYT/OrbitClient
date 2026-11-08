package com.orbitclient.test;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import com.orbitclient.module.HudModule;
import com.orbitclient.module.Module;
import com.orbitclient.module.ModuleManager;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Where frame time goes (`gradlew runClientGameTest -PperfTest`, only this test runs): the frame rate is
 * uncapped and the player stands still, so only the setting being measured changes. Prints [PERF] lines
 * in milliseconds per frame.
 */
@SuppressWarnings("UnstableApiUsage")
public class PerformanceTest implements FabricClientGameTest {
	private static final int SAMPLES = 5;
	private static final int PLAYERS = 40;

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
			context.waitTicks(100);  // let chunk building and the JIT settle

			compare(context, "HUD (everything F1 hides)", mc -> mc.options.hideGui = true, mc -> mc.options.hideGui = false);
			List<HudModule> hud = context.computeOnClient(mc -> ModuleManager.hudModules().stream().filter(Module::isEnabled).toList());
			System.out.println("[PERF] Orbit HUD mods on: " + hud.stream().map(Module::getName).toList());
			compare(context, "Orbit's HUD mods", mc -> hud.forEach(m -> m.setEnabled(false)), mc -> hud.forEach(m -> m.setEnabled(true)));

			context.runOnClient(PerformanceTest::addPlayers);
			System.out.printf("[PERF] %d players in view from here on; FPS each second after they appear:%n", PLAYERS);
			StringBuilder settling = new StringBuilder();
			for (int second = 0; second < 15; second++) {
				context.waitTicks(20);
				int fps = context.computeOnClient(mc -> mc.getFps());
				settling.append(fps).append(' ');
			}
			System.out.println("[PERF] " + settling);
			compare(context, "Nametags and HUD (F1)", mc -> mc.options.hideGui = true, mc -> mc.options.hideGui = false);
			compare(context, "Nametags and HUD again", mc -> mc.options.hideGui = true, mc -> mc.options.hideGui = false);
			compare(context, "Orbit's HUD mods, players", mc -> hud.forEach(m -> m.setEnabled(false)), mc -> hud.forEach(m -> m.setEnabled(true)));
		}
	}

	/** Measures with a setting off, then on (the order alternates, so slow drift shows as noise, not cost). */
	private static void compare(ClientGameTestContext context, String what, Consumer<Minecraft> off, Consumer<Minecraft> on) {
		double offMs = 0, onMs = 0;
		for (int round = 0; round < 2; round++) {
			boolean offFirst = round == 0;
			context.runOnClient(offFirst ? off::accept : on::accept);
			double first = frameMs(context);
			context.runOnClient(offFirst ? on::accept : off::accept);
			double second = frameMs(context);
			offMs += offFirst ? first : second;
			onMs += offFirst ? second : first;
		}
		offMs /= 2;
		onMs /= 2;
		System.out.printf("[PERF] %-28s on %6.3f ms (%4.0f FPS), off %6.3f ms (%4.0f FPS): costs %+6.3f ms%n",
			what, onMs, 1000 / onMs, offMs, 1000 / offMs, onMs - offMs);
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

	/** Other players in a block in front of the camera, like a lobby. */
	private static void addPlayers(Minecraft mc) {
		var me = mc.player;
		float yaw = (float) Math.toRadians(me.getYRot());
		double forwardX = -Math.sin(yaw), forwardZ = Math.cos(yaw);
		for (int i = 0; i < PLAYERS; i++) {
			double ahead = 4 + (i / 8) * 2.5, across = (i % 8 - 3.5) * 1.6;
			RemotePlayer other = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "Player" + i));
			other.setPos(me.getX() + forwardX * ahead - forwardZ * across, me.getY(), me.getZ() + forwardZ * ahead + forwardX * across);
			other.setYRot(me.getYRot() + 180);
			other.yBodyRot = other.yHeadRot = me.getYRot() + 180;
			mc.level.addEntity(other);
		}
	}
}
