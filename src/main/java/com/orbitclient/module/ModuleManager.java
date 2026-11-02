package com.orbitclient.module;

import com.orbitclient.module.hud.*;
import com.orbitclient.module.mechanics.*;
import com.orbitclient.module.server.*;
import com.orbitclient.module.server.ScaledHudModule;
import com.orbitclient.module.server.WaypointsModule;
import com.orbitclient.module.visual.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();
	private static final Map<Class<? extends Module>, Module> BY_CLASS = new HashMap<>();
	private static final Map<Module, Boolean> KEY_WAS_DOWN = new HashMap<>();

	public static ScaledHudModule tabEditor;
	public static ScaledHudModule titles;

	private ModuleManager() {
	}

	public static void init() {
		// HUD
		register(new ArmorStatusModule());
		register(new ClockModule());
		register(new ComboCounterModule());
		register(new CooldownsModule());
		register(new CoordinatesModule());
		register(new CpsModule());
		register(new DayCounterModule());
		register(new DirectionModule());
		register(new FpsModule());
		register(new ItemCounterModule());
		register(new ItemTrackerModule());
		register(new KeystrokesModule());
		register(new MemoryModule());
		register(new PackDisplayModule());
		register(new PlaytimeModule());
		register(new PotionCounterModule());
		register(new PotionEffectsModule());
		register(new ReachDisplayModule());
		register(new SaturationModule());
		register(new SpeedModule());
		register(new StopwatchModule());
		register(new WailaModule());
		// Server
		register(new AutoTextModule());
		register(new BossBarModule());
		register(new PingModule());
		register(new ScoreboardModule());
		register(new ServerAddressModule());
		register(tabEditor = ScaledHudModule.tabEditor());
		register(titles = ScaledHudModule.titles());
		register(new WaypointsModule());
		// Mechanic
		register(DebugToggleModule.chunkBorders());
		register(new CrosshairModule());
		register(new FovChangerModule());
		register(new FreelookModule());
		register(new FullbrightModule());
		register(DebugToggleModule.hitbox());
		register(new MotionBlurModule());
		register(new BlockOutlineModule());
		register(new HitColorModule());
		register(new NametagsModule());
		register(new ShinyPotsModule());
		register(new LowFireModule());
		register(new ItemPhysicsModule());
		register(new MenuBlurModule());
		register(new GlintColorizerModule());
		register(new TeamViewModule());
		register(new WaveyCapesModule());
		register(new ParticleChangerModule());
		register(new ChatModule());
		register(new NickHiderModule());
		register(new ScreenshotModule());
		register(new SoundChangerModule());
		register(new NoHurtCamModule());
		register(new ParticleMultiplierModule());
		register(new SnaplookModule());
		register(new TimeChangerModule());
		register(new ToggleSprintModule());
		register(new WeatherChangerModule());
		register(new ZoomModule());

		MODULES.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

		// Lunar's defaults for a fresh install; saved profiles override these.
		for (Class<? extends Module> type : List.of(ArmorStatusModule.class, BossBarModule.class, CoordinatesModule.class,
			DirectionModule.class, FreelookModule.class, PotionEffectsModule.class, ScoreboardModule.class,
			ToggleSprintModule.class, WaypointsModule.class, ZoomModule.class)) {
			get(type).setDefaultEnabled(true);
		}
		tabEditor.setDefaultEnabled(true);
		titles.setDefaultEnabled(true);
	}

	private static void register(Module module) {
		MODULES.add(module);
		BY_CLASS.put(module.getClass(), module);
	}

	public static List<Module> all() {
		return MODULES;
	}

	public static List<HudModule> hudModules() {
		List<HudModule> list = new ArrayList<>();
		for (Module module : MODULES) if (module instanceof HudModule hud) list.add(hud);
		return list;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Module> T get(Class<T> type) {
		return (T) BY_CLASS.get(type);
	}

	public static void tick(Minecraft mc) {
		long window = mc.getWindow().handle();
		for (Module module : MODULES) {
			if (module.toggleKey.isBound()) {
				boolean down = mc.screen == null && module.toggleKey.isDown();
				if (down && !KEY_WAS_DOWN.getOrDefault(module, false)) module.toggle();
				KEY_WAS_DOWN.put(module, down);
			}
			if (module.isEnabled()) module.onTick();
		}
	}
}
