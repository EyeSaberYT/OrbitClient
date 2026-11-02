package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

/** More hit particles, and sharpness particles on every hit. */
public class ParticleMultiplierModule extends Module {
	private final NumberSetting multiplier;
	private final BooleanSetting alwaysSharpness;
	private final BooleanSetting alwaysCrits;

	public ParticleMultiplierModule() {
		super("Particle Multiplier", "Shows more hit particles when you attack.", Category.MECHANIC);
		setIcon(Items.BLAZE_POWDER);
		multiplier = add(new NumberSetting("Multiplier", 2, 1, 10, 1));
		alwaysSharpness = add(new BooleanSetting("Always Sharpness", true));
		alwaysCrits = add(new BooleanSetting("Always Critical", false));
	}

	public void onAttack(Entity target) {
		if (!isEnabled() || !(target instanceof LivingEntity)) return;
		int times = multiplier.getInt();
		for (int i = 0; i < times; i++) {
			if (alwaysSharpness.isOn()) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
			if (alwaysCrits.isOn()) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
		}
	}
}
