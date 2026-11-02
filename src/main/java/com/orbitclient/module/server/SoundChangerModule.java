package com.orbitclient.module.server;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.item.Items;

/** Volume for groups of sounds that vanilla's sound sliders don't separate. */
public class SoundChangerModule extends Module {
	private static SoundChangerModule instance;

	private record Group(NumberSetting volume, String... parts) {
	}

	private final Group[] groups;

	public SoundChangerModule() {
		super("Sound Changer", "Change the volume of specific sounds like explosions or rain.", Category.SERVER);
		setIcon(Items.NOTE_BLOCK);
		instance = this;
		groups = new Group[]{
			new Group(add(new NumberSetting("Hits & Hurt", 100, 0, 200, 5)), ".hurt", "attack.", ".death"),
			new Group(add(new NumberSetting("Explosions", 100, 0, 200, 5)), "explode", "explosion"),
			new Group(add(new NumberSetting("Rain & Thunder", 100, 0, 200, 5)), "weather."),
			new Group(add(new NumberSetting("Footsteps", 100, 0, 200, 5)), ".step"),
			new Group(add(new NumberSetting("Eating & Drinking", 100, 0, 200, 5)), "entity.generic.eat", "entity.generic.drink", "entity.player.burp"),
			new Group(add(new NumberSetting("Fireworks", 100, 0, 200, 5)), "firework_rocket"),
			new Group(add(new NumberSetting("Portals", 100, 0, 200, 5)), "block.portal."),
			new Group(add(new NumberSetting("XP & Level Up", 100, 0, 200, 5)), "experience_orb", "player.levelup"),
		};
	}

	/** Multiplies a sound's volume by its group's setting. */
	public static float volume(SoundInstance sound, float vanilla) {
		SoundChangerModule m = instance;
		if (m == null || !m.isEnabled() || sound.getIdentifier() == null) return vanilla;
		String id = sound.getIdentifier().getPath();
		for (Group g : m.groups) {
			for (String part : g.parts) {
				if (id.contains(part)) return vanilla * g.volume.getFloat() / 100f;
			}
		}
		return vanilla;
	}
}
