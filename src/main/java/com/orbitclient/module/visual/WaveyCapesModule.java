package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * Wavey Capes: capes move like cloth (Orbit's own simulation, {@link com.orbitclient.cape.ClothCape}).
 * The settings and defaults match the WaveyCapes mod's, so it feels the same. If that mod is installed
 * too, this steps aside and lets it draw capes.
 */
public class WaveyCapesModule extends Module {
	private static final boolean WAVEYCAPES_INSTALLED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("waveycapes");
	private static WaveyCapesModule instance;
	/** For tests: a cape texture to use for players without one (dev accounts have no cape). */
	public static Identifier testCape;

	private final ModeSetting style;
	private final ModeSetting wind;
	private final NumberSetting gravity;
	private final NumberSetting heightMultiplier;
	private final NumberSetting strafeMultiplier;

	public WaveyCapesModule() {
		super("Wavey Capes", "Capes move like cloth.", Category.MECHANIC);
		setIcon(Items.ELYTRA);
		instance = this;
		style = add(new ModeSetting("Style", "Smooth", "Smooth", "Blocky"));
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
}
