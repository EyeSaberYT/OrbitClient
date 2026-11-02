package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * Client-side cape motion profiles implemented independently in {@link com.orbitclient.cape.ClothCape}.
 * If WaveyCapes is installed too, this module steps aside to avoid drawing the cape twice.
 */
public class WaveyCapesModule extends Module {
	private static final boolean WAVEYCAPES_INSTALLED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("waveycapes");
	private static WaveyCapesModule instance;
	/** For tests: a cape texture to use for players without one (dev accounts have no cape). */
	public static Identifier testCape;

	private final ModeSetting style;
	private final ModeSetting physics;
	private final ModeSetting wind;
	private final NumberSetting gravity;
	private final NumberSetting heightMultiplier;
	private final NumberSetting strafeMultiplier;

	public WaveyCapesModule() {
		super("Cape Physics", "Adds independent cloth motion to capes.", Category.MECHANIC);
		setIcon(Items.ELYTRA);
		instance = this;
		style = add(new ModeSetting("Style", "Smooth", "Smooth", "Blocky"));
		physics = add(new ModeSetting("Physics", "Dynamic", "Dynamic", "Pendulum", "Classic", "Dungeon Arc"));
		wind = add(new ModeSetting("Wind", "None", "None", "Waves"));
		gravity = add(new NumberSetting("Gravity", 25, 5, 50, 1));
		heightMultiplier = add(new NumberSetting("Height Multiplier", 6, 2, 10, 1));
		strafeMultiplier = add(new NumberSetting("Strafe Multiplier", 2, 0, 4, 1));
	}

	public static boolean active() {
		return instance != null && instance.isEnabled() && !WAVEYCAPES_INSTALLED;
	}

	public static boolean smooth() {
		return instance == null || instance.style.is("Smooth");
	}

	public static boolean windWaves() {
		return instance != null && instance.wind.is("Waves");
	}

	public static float gravity() {
		return instance == null ? 25 : instance.gravity.getFloat();
	}

	public static float heightMultiplier() {
		return instance == null ? 6 : instance.heightMultiplier.getFloat();
	}

	public static float strafeMultiplier() {
		return instance == null ? 2 : instance.strafeMultiplier.getFloat();
	}

	public static float damping() {
		if (instance == null) return 0.94f;
		if (instance.physics.is("Classic")) return 0.97f;
		if (instance.physics.is("Pendulum")) return 0.90f;
		if (instance.physics.is("Dungeon Arc")) return 0.92f;
		return 0.94f;
	}

	public static float motionScale() {
		if (instance == null) return 1f;
		if (instance.physics.is("Classic")) return 0.48f;
		if (instance.physics.is("Pendulum")) return 1.18f;
		if (instance.physics.is("Dungeon Arc")) return 0.92f;
		return 1f;
	}

	public static float lateralScale() {
		if (instance == null) return 1f;
		if (instance.physics.is("Classic")) return 0.28f;
		if (instance.physics.is("Pendulum")) return 0.62f;
		if (instance.physics.is("Dungeon Arc")) return 0.50f;
		return 1f;
	}

	public static float rippleScale() {
		if (instance == null) return 1f;
		if (instance.physics.is("Classic")) return 0.2f;
		if (instance.physics.is("Pendulum")) return 0f;
		if (instance.physics.is("Dungeon Arc")) return 0.12f;
		return 1f;
	}

	public static float gravityScale() {
		if (instance == null) return 1f;
		if (instance.physics.is("Classic")) return 0.65f;
		if (instance.physics.is("Dungeon Arc")) return 0.82f;
		if (instance.physics.is("Pendulum")) return 1.1f;
		return 1f;
	}

	public static boolean dungeonArc() {
		return instance != null && instance.physics.is("Dungeon Arc");
	}
}
