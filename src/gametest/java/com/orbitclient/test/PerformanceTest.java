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
import com.orbitclient.util.TextLayoutCache;

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
	private static final int HOLOGRAMS = 40;

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
		// Laying out text directly: the same lines many times, cache off then on (timed on the render thread,
		// so it doesn't depend on what else the computer is doing as much as frame rates do).
		System.out.println("[PERF] " + context.computeOnClient(PerformanceTest::textLayoutBenchmark));
		if (Boolean.getBoolean("orbit.perfBenchmarkOnly")) return;  // -PperfBenchmarkOnly: skip the frame-rate part

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

			// Holograms like a server lobby's: text displays with a few colored lines each.
			for (int i = 0; i < HOLOGRAMS; i++) {
				double side = (i % 8 - 3.5) * 2, ahead = 3 + (i / 8) * 3;
				sp.getServer().runCommand(String.format(java.util.Locale.ROOT,
					"execute as @p at @s run summon text_display ^%.1f ^2.5 ^%.1f {billboard:\"center\",text:[{text:\"Hologram %d\\n\",color:\"gold\",bold:true},"
						+ "{text:\"Click to play \",color:\"gray\"},{text:\"BedWars\\n\",color:\"aqua\"},{text:\"%d players online\",color:\"green\"}]}",
					side, ahead, i, 100 + i));
			}
			context.waitTicks(40);
			System.out.printf("[PERF] %d holograms in view from here on%n", HOLOGRAMS);
			compare(context, "Text layout cache", mc -> TextLayoutCache.setEnabled(false), mc -> TextLayoutCache.setEnabled(true));
			compare(context, "Text layout cache again", mc -> TextLayoutCache.setEnabled(false), mc -> TextLayoutCache.setEnabled(true));
			context.runOnClient(mc -> {
				TextLayoutCache.setEnabled(true);
				TextLayoutCache.stats();
			});
			context.waitTicks(40);
			System.out.println("[PERF] Text layouts over 2 seconds: " + context.computeOnClient(mc -> TextLayoutCache.stats()));
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

	private static String textLayoutBenchmark(Minecraft mc) {
		java.util.List<net.minecraft.network.chat.Component> components = new java.util.ArrayList<>();
		for (int i = 0; i < 40; i++) {
			components.add(net.minecraft.network.chat.Component.literal("Hologram " + i).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
			components.add(net.minecraft.network.chat.Component.literal("Click to play ").withStyle(net.minecraft.ChatFormatting.GRAY)
				.append(net.minecraft.network.chat.Component.literal("BedWars").withStyle(net.minecraft.ChatFormatting.AQUA)));
			components.add(net.minecraft.network.chat.Component.literal((100 + i) + " players online").withStyle(net.minecraft.ChatFormatting.GREEN));
		}
		java.util.List<net.minecraft.util.FormattedCharSequence> kept = components.stream()
			.map(net.minecraft.network.chat.Component::getVisualOrderText).toList();
		// Kept: the same text objects every time (holograms, signs, chat). Rebuilt: new ones every time (nametags).
		double[] keptNs = timeLayouts(mc, () -> kept);
		double[] rebuiltNs = timeLayouts(mc, () -> components.stream()
			.map(c -> net.minecraft.locale.Language.getInstance().getVisualOrder(c)).toList());
		TextLayoutCache.setEnabled(true);
		return String.format(java.util.Locale.ROOT, "Text layout per line: kept text %.0f ns -> %.0f ns with the cache (%.1fx); "
				+ "rebuilt text %.0f ns -> %.0f ns (%.2fx)",
			keptNs[0], keptNs[1], keptNs[0] / keptNs[1], rebuiltNs[0], rebuiltNs[1], rebuiltNs[0] / rebuiltNs[1]);
	}

	/** Nanoseconds per line without and with the cache. Passes 0-1 warm up the JIT; 2-3 are timed. */
	private static double[] timeLayouts(Minecraft mc, java.util.function.Supplier<java.util.List<net.minecraft.util.FormattedCharSequence>> lines) {
		long[] nanos = new long[2];
		int count = 0;
		for (int pass = 0; pass < 4; pass++) {
			for (int cached = 0; cached < 2; cached++) {
				TextLayoutCache.setEnabled(cached == 1);
				long start = System.nanoTime();
				for (int repeat = 0; repeat < 500; repeat++) {
					var batch = lines.get();
					for (var line : batch) mc.font.prepareText(line, -30f, 0f, 0xFFFFFFFF, false, false, 0x40000000);
					if (pass == 2 && cached == 0) count += batch.size();
				}
				if (pass >= 2) nanos[cached] += System.nanoTime() - start;
			}
		}
		return new double[] {nanos[0] / (2.0 * count), nanos[1] / (2.0 * count)};
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
