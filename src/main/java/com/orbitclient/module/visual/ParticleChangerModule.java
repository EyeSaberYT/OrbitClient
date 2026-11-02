package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

/** Shows critical-hit and sharpness particles on every hit, so hits are easy to see. */
public class ParticleChangerModule extends Module {
	private static ParticleChangerModule instance;
	private final BooleanSetting crits;
	private final BooleanSetting sharpness;

	public ParticleChangerModule() {
		super("Particle Changer", "Always shows crit and sharpness particles when you hit.", Category.MECHANIC);
		setIcon(Items.NETHER_STAR);
		instance = this;
		crits = add(new BooleanSetting("Always Crit Particles", true));
		sharpness = add(new BooleanSetting("Always Sharpness Particles", false));
	}

	public static void onAttack(Entity target) {
		ParticleChangerModule m = instance;
		Minecraft mc = Minecraft.getInstance();
		if (m == null || !m.isEnabled() || mc.player == null || !(target instanceof LivingEntity)) return;
		if (m.crits.isOn()) mc.player.crit(target);
		if (m.sharpness.isOn()) mc.player.magicCrit(target);
	}
}
