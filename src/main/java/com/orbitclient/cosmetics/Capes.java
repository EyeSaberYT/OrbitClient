package com.orbitclient.cosmetics;

import static com.orbitclient.cosmetics.Cosmetics.cape;

/** Capes: animated textures drawn by tools/CosmeticGenerator.java (textures/cosmetics/capes/<id>_<frame>.png). */
final class Capes {
	private Capes() {
	}

	static void register() {
		cape("eclipse", "Eclipse", 16);
		cape("galaxy", "Galaxy", 16);
		cape("flame", "Inferno", 16);
		cape("aurora", "Aurora", 16);
		cape("ocean", "Tide", 16);
		cape("circuit", "Neon Circuit", 16);
		cape("sakura", "Sakura", 16);
		cape("midnight", "Midnight", 16);
		cape("stormfront", "Stormfront", 16);
		cape("magma", "Magma Flow", 16);
		cape("prism_wave", "Prism Wave", 16);
		cape("firefly_forest", "Firefly Forest", 16);
		cape("frostbite", "Frostbite", 16);
		cape("retrowave", "Retrowave", 16);
		cape("koi_pond", "Koi Pond", 16);
		cape("hive", "Honeycomb", 16);
		cape("regal", "Regal Velvet", 16);
		cape("plasma", "Plasma Lamp", 16);
	}
}
