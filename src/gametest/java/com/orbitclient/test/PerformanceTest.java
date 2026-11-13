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
	/** Only a few named modules are measured: the general comparisons are skipped and theirs alternate 12 times. */
	private static boolean quick;

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
		// A few modules named (-PperfModules): only those are measured, more carefully.
		String wanted = System.getProperty("orbit.perfModules", "");
		quick = !wanted.isBlank() && wanted.split(",").length <= 3;

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
			// The same players, each wearing a full outfit (hat, face, back, wings, aura and pet).
			compare(context, "Cosmetics on " + PLAYERS + " players", mc -> showCosmetics(mc, false), mc -> showCosmetics(mc, true));
			compare(context, "Cosmetics, again", mc -> showCosmetics(mc, false), mc -> showCosmetics(mc, true));
			// How much geometry that is: models and faces drawn per frame with everyone dressed.
			context.runOnClient(mc -> showCosmetics(mc, true));
			context.waitTicks(20);
			long[] before = context.computeOnClient(mc -> new long[] {com.orbitclient.cosmetics.VoxelModel.drawnModels, com.orbitclient.cosmetics.VoxelModel.drawnFaces});
			double frames = 0;
			for (int second = 0; second < 5; second++) {
				context.waitTicks(20);
				frames += context.computeOnClient(mc -> mc.getFps());
			}
			long[] after = context.computeOnClient(mc -> new long[] {com.orbitclient.cosmetics.VoxelModel.drawnModels, com.orbitclient.cosmetics.VoxelModel.drawnFaces});
			System.out.printf(java.util.Locale.ROOT, "[PERF] Cosmetics per frame: %.0f models, %.0f faces (%.0f per player)%n",
				(after[0] - before[0]) / frames, (after[1] - before[1]) / frames, (after[1] - before[1]) / frames / PLAYERS);
			context.runOnClient(mc -> showCosmetics(mc, false));

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

			modules(context);
		}
	}

	/**
	 * Each module on its own, then all of them together, with the players and holograms in view
	 * (-PperfModules="Motion Blur,Cape Physics": the modules a player has on; without it, every module).
	 */
	private static void modules(ClientGameTestContext context) {
		String wanted = System.getProperty("orbit.perfModules", "");
		List<String> names = wanted.isBlank() ? List.of() : List.of(wanted.split("\\s*,\\s*"));
		List<Module> all = context.computeOnClient(mc -> ModuleManager.all().stream()
			.filter(m -> names.isEmpty() || names.contains(m.getName())).toList());
		List<Module> wereOn = context.computeOnClient(mc -> all.stream().filter(Module::isEnabled).toList());
		// A full HD window: full-screen effects (Motion Blur) cost by the pixel.
		context.getInput().resizeWindow(1920, 1080);
		context.runOnClient(mc -> all.forEach(m -> m.setEnabled(false)));
		context.waitTicks(40);
		if (!Boolean.getBoolean("orbit.perfProfileOnly")) {  // -PperfProfileOnly: skip to the profile
			System.out.printf("[PERF] %d modules, one at a time at 1920x1080, the others off:%n", all.size());
			for (Module module : all) {
				compare(context, "> " + module.getName(), mc -> module.setEnabled(false), mc -> module.setEnabled(true));
				context.runOnClient(mc -> module.setEnabled(false));
			}
		}
		compare(context, "> All of them together", mc -> all.forEach(m -> m.setEnabled(false)), mc -> all.forEach(m -> m.setEnabled(true)));
		// Where Orbit's own render-thread time goes with all of them on.
		context.runOnClient(mc -> all.forEach(m -> m.setEnabled(true)));
		context.waitTicks(40);
		// Motion Blur keeps two images and swaps them each frame (its default strength is 5).
		System.out.println("[PERF] Motion Blur's kept images: " + context.computeOnClient(mc -> {
			var chain = mc.getShaderManager().getPostChain(net.minecraft.resources.Identifier.fromNamespaceAndPath("orbitclient", "motion_blur_5"),
				net.minecraft.client.renderer.LevelTargetBundle.MAIN_TARGETS);
			if (chain == null) return "the effect didn't load";
			var kept = ((com.orbitclient.mixin.PostChainAccessor) chain).orbit$persistentTargets();
			return kept.keySet() + ", distinct: " + (new java.util.HashSet<>(kept.values()).size() == kept.size());
		}));
		profile(context, 30);
		context.runOnClient(mc -> all.forEach(m -> m.setEnabled(wereOn.contains(m))));
	}

	/**
	 * Samples the render thread every millisecond and prints the share of samples spent under Orbit's code:
	 * each line is the innermost Orbit method on the stack and what it had called ("self" when nothing).
	 * Mixin handlers count as Orbit's (their names contain orbit$).
	 */
	private static void profile(ClientGameTestContext context, int seconds) {
		Thread render = context.computeOnClient(mc -> Thread.currentThread());
		java.util.Map<String, Integer> counts = new java.util.HashMap<>();
		int[] totals = new int[2];  // samples, samples under Orbit code
		java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(true);
		Thread sampler = new Thread(() -> {
			while (running.get()) {
				StackTraceElement[] stack = render.getStackTrace();
				totals[0]++;
				for (int i = 0; i < stack.length; i++) {
					if (!isOrbit(stack[i])) continue;
					totals[1]++;
					String callee = i == 0 ? "self" : simple(stack[i - 1]);
					counts.merge(simple(stack[i]) + " > " + callee, 1, Integer::sum);
					break;
				}
				java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
			}
		}, "Orbit profiler");
		sampler.setDaemon(true);
		sampler.start();
		context.waitTicks(seconds * 20);
		running.set(false);
		try {
			sampler.join(1000);
		} catch (InterruptedException ignored) {
		}
		System.out.printf("[PERF] Profile: %d samples, %.1f%% under Orbit's code%n", totals[0], 100.0 * totals[1] / Math.max(1, totals[0]));
		counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(40)
			.forEach(e -> System.out.printf("[PERF]   %5.2f%%  %s%n", 100.0 * e.getValue() / Math.max(1, totals[0]), e.getKey()));
	}

	private static boolean isOrbit(StackTraceElement frame) {
		String cls = frame.getClassName();
		return (cls.startsWith("com.orbitclient.") && !cls.startsWith("com.orbitclient.test.")) || frame.getMethodName().contains("orbit$");
	}

	private static String simple(StackTraceElement frame) {
		String cls = frame.getClassName();
		return cls.substring(cls.lastIndexOf('.') + 1) + "." + frame.getMethodName();
	}

	/** Measures with a setting off, then on (the order alternates, so slow drift shows as noise, not cost). */
	private static void compare(ClientGameTestContext context, String what, Consumer<Minecraft> off, Consumer<Minecraft> on) {
		boolean module = what.startsWith("> ");
		if (quick && !module) return;
		int rounds = quick ? 12 : 2;
		double offMs = 0, onMs = 0;
		for (int round = 0; round < rounds; round++) {
			boolean offFirst = round % 2 == 0;
			context.runOnClient(offFirst ? off::accept : on::accept);
			double first = frameMs(context);
			context.runOnClient(offFirst ? on::accept : off::accept);
			double second = frameMs(context);
			offMs += offFirst ? first : second;
			onMs += offFirst ? second : first;
		}
		// The second round ends with it off: left on, as it was, for what's measured next.
		context.runOnClient(on::accept);
		offMs /= rounds;
		onMs /= rounds;
		System.out.printf("[PERF] %-28s on %6.3f ms (%4.0f FPS), off %6.3f ms (%4.0f FPS): costs %+6.3f ms%n",
			what, onMs, 1000 / onMs, offMs, 1000 / offMs, onMs - offMs);
	}

	/** Average frame time in milliseconds, from the game's frames-per-second counter (one reading a second). */
	private static double frameMs(ClientGameTestContext context) {
		context.waitTicks(quick ? 20 : 40);
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
	private static final String OUTFIT = "{\"HAT\":{\"id\":\"crown\",\"color\":-868018},\"FACE\":{\"id\":\"bandana\",\"color\":-2734787},"
		+ "\"BACK\":{\"id\":\"backpack\",\"color\":-11634113},\"WINGS\":{\"id\":\"dragon_wings\",\"color\":-8773845},"
		+ "\"AURA\":{\"id\":\"orbit\",\"color\":-8622849},\"PET\":{\"id\":\"wisp\",\"color\":-8590000}}";

	/** Dresses every other player (or undresses them). Done again each time: the real sync may clear made-up players. */
	private static void showCosmetics(Minecraft mc, boolean on) {
		var outfit = on ? com.orbitclient.cosmetics.CosmeticManager.Loadout.fromJson(com.google.gson.JsonParser.parseString(OUTFIT)) : null;
		for (var player : mc.level.players()) {
			if (player != mc.player) com.orbitclient.cosmetics.CosmeticManager.setOther(player.getUUID(), outfit);
		}
	}

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
