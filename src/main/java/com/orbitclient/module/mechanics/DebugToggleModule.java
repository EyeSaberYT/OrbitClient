package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Keeps one of the vanilla F3 debug renderers (hitboxes, chunk borders) on while enabled. */
public class DebugToggleModule extends Module {
	private final Identifier entry;

	private DebugToggleModule(String name, String description, Item icon, Identifier entry) {
		super(name, description, Category.MECHANIC);
		setIcon(icon);
		this.entry = entry;
	}

	public static DebugToggleModule hitbox() {
		return new DebugToggleModule("Hitbox", "Shows entity hitboxes (like F3 + B).", Items.SCAFFOLDING, DebugScreenEntries.ENTITY_HITBOXES);
	}

	public static DebugToggleModule chunkBorders() {
		return new DebugToggleModule("Chunk Borders", "Shows chunk borders (like F3 + G).", Items.GRASS_BLOCK, DebugScreenEntries.CHUNK_BORDERS);
	}

	@Override
	public void onTick() {
		// Applied on tick rather than in onEnable: the debug list doesn't exist yet when the config loads.
		if (mc.debugEntries != null && !mc.debugEntries.isCurrentlyEnabled(entry)) {
			mc.debugEntries.setStatus(entry, DebugScreenEntryStatus.ALWAYS_ON);
		}
	}

	@Override
	protected void onDisable() {
		if (mc.debugEntries != null) mc.debugEntries.setStatus(entry, DebugScreenEntryStatus.NEVER);
	}
}
