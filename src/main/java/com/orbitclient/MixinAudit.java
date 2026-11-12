package com.orbitclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbitclient.gui.OrbitHomeScreen;
import com.orbitclient.mixin.OrbitMixinPlugin;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Development check, off unless asked for (`gradlew :<version>:runClient -PmixinAudit`): once the game has
 * started, loads every class Orbit's mixins target, so each mixin is applied now instead of when its class
 * is first used, then waits for the first menu (it must be Orbit's home screen) and quits with status 0 if
 * all of that worked and 1 if anything failed.
 */
public final class MixinAudit {
	private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
	private static boolean reported;

	private MixinAudit() {
	}

	public static void runIfRequested() {
		if (!Boolean.getBoolean("orbit.mixinAudit")) return;
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			List<String> failures = new ArrayList<>();
			Set<String> targets = new LinkedHashSet<>();
			try {
				for (String mixin : mixinNames()) targets.addAll(targetsOf(mixin));
			} catch (Exception error) {
				failures.add("reading the mixins: " + error);
			}
			ClassLoader loader = MixinAudit.class.getClassLoader();
			for (String target : targets) {
				try {
					Class.forName(target, false, loader);
				} catch (Throwable error) {
					failures.add(target + ": " + causes(error));
				}
			}
			if (!failures.isEmpty()) report(failures, targets.size() + " target classes", null);
			// Then the first menu once loading is done: Orbit's home screen, not the vanilla title screen.
			int[] ticks = {0};
			ClientTickEvents.END_CLIENT_TICK.register(mc -> {
				Screen screen = mc.screen;
				if (++ticks[0] > 1200) {
					failures.add("no menu opened within a minute of starting" + (screen == null ? "" : " (on " + screen.getClass().getName() + ")"));
					report(failures, targets.size() + " target classes", mc);
				} else if (mc.getOverlay() == null && screen instanceof AccessibilityOnboardingScreen) {
					// A new game folder asks about accessibility first. Going on from it is what the game does
					// then: remember it was answered and open the title screen (which must become Orbit's).
					mc.options.onboardAccessibility = false;
					mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
				} else if (mc.getOverlay() == null && screen != null) {
					if (!(screen instanceof OrbitHomeScreen)) failures.add("the first menu is " + screen.getClass().getName() + ", not Orbit's home screen");
					report(failures, targets.size() + " target classes, first menu " + screen.getClass().getSimpleName(), mc);
				}
			});
		});
	}

	/** Prints the result and quits: normally when it passed (halting mid-frame can crash the driver), at once with 1 when not. */
	private static void report(List<String> failures, String summary, net.minecraft.client.Minecraft mc) {
		// The game keeps ticking while it stops.
		if (reported) return;
		reported = true;
		for (String failure : failures) System.out.println("ORBIT-MIXIN-AUDIT FAILED " + failure);
		System.out.println("ORBIT-MIXIN-AUDIT " + (failures.isEmpty() ? "OK" : "FAILED") + ": " + summary);
		if (failures.isEmpty() && mc != null) mc.stop();
		else Runtime.getRuntime().halt(failures.isEmpty() ? 0 : 1);
	}

	/** Every mixin this version applies: the config's list plus the version-specific ones from the plugin. */
	private static List<String> mixinNames() throws Exception {
		List<String> names = new ArrayList<>();
		try (InputStream in = MixinAudit.class.getResourceAsStream("/orbitclient.mixins.json")) {
			// The instance parse(): JsonParser.parseReader isn't in the older Gson that 1.16 to 1.17 ship.
			@SuppressWarnings("deprecation")
			JsonObject config = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (String side : new String[]{"mixins", "client"}) {
				if (config.has(side)) for (JsonElement name : config.getAsJsonArray(side)) names.add(name.getAsString());
			}
		}
		names.addAll(new OrbitMixinPlugin().getMixins());
		return names;
	}

	/** The classes a mixin targets, read from its @Mixin annotation without loading it (mixins can't be loaded). */
	private static List<String> targetsOf(String mixin) throws Exception {
		List<String> targets = new ArrayList<>();
		try (InputStream in = MixinAudit.class.getResourceAsStream("/com/orbitclient/mixin/" + mixin + ".class")) {
			if (in == null) throw new IllegalStateException("no class for mixin " + mixin);
			ClassNode node = new ClassNode();
			new ClassReader(in.readAllBytes()).accept(node, ClassReader.SKIP_CODE);
			List<AnnotationNode> annotations = new ArrayList<>();
			if (node.invisibleAnnotations != null) annotations.addAll(node.invisibleAnnotations);
			if (node.visibleAnnotations != null) annotations.addAll(node.visibleAnnotations);
			for (AnnotationNode annotation : annotations) {
				if (!MIXIN.equals(annotation.desc) || annotation.values == null) continue;
				for (int i = 0; i < annotation.values.size(); i += 2) {
					Object value = annotation.values.get(i + 1);
					if (!(value instanceof List<?> list)) continue;
					for (Object target : list) {
						if (target instanceof Type type) targets.add(type.getClassName());
						else if (target instanceof String name) targets.add(name.replace('/', '.'));
					}
				}
			}
		}
		return targets;
	}

	private static String causes(Throwable error) {
		StringBuilder causes = new StringBuilder(error.toString());
		for (Throwable cause = error.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
			causes.append(" <- ").append(cause);
		}
		return causes.toString();
	}
}
