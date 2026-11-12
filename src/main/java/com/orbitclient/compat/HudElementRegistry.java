package com.orbitclient.compat;

//? if <1.21.6 {
/*import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/^*
 * Before 1.21.6 (no Fabric HUD element API): the part of Fabric's HudElementRegistry that HudRenderer uses.
 * GuiHudMixin and BossHealthOverlayMixin run vanilla's HUD parts through render(), which draws what's
 * attached before, the part itself (or its replacement) and what's attached after.
 ^/
public final class HudElementRegistry {
	private static final Map<Identifier, List<HudElement>> BEFORE = new HashMap<>();
	private static final Map<Identifier, List<HudElement>> AFTER = new HashMap<>();
	private static final Map<Identifier, Function<HudElement, HudElement>> REPLACE = new HashMap<>();

	private HudElementRegistry() {
	}

	public static void attachElementBefore(Identifier vanilla, Identifier id, HudElement element) {
		BEFORE.computeIfAbsent(vanilla, k -> new ArrayList<>()).add(element);
	}

	public static void attachElementAfter(Identifier vanilla, Identifier id, HudElement element) {
		AFTER.computeIfAbsent(vanilla, k -> new ArrayList<>()).add(element);
	}

	public static void replaceElement(Identifier vanilla, Function<HudElement, HudElement> replacer) {
		Function<HudElement, HudElement> previous = REPLACE.get(vanilla);
		REPLACE.put(vanilla, previous == null ? replacer : original -> replacer.apply(previous.apply(original)));
	}

	public static void render(Identifier vanilla, GuiGraphics graphics, DeltaTracker delta, HudElement original) {
		for (HudElement element : BEFORE.getOrDefault(vanilla, List.of())) element.render(graphics, delta);
		Function<HudElement, HudElement> replacer = REPLACE.get(vanilla);
		(replacer == null ? original : replacer.apply(original)).render(graphics, delta);
		for (HudElement element : AFTER.getOrDefault(vanilla, List.of())) element.render(graphics, delta);
	}
}
*///?}
