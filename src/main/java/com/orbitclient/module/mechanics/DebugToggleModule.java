package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Keeps one of the vanilla F3 debug renderers (hitboxes, chunk borders) on while enabled.
 *
 * <p>Minecraft hides these while Reduced Debug Info is active, whether the player chose it in Chat
 * Settings or the server set the {@code reducedDebugInfo} game rule. That is respected: the module
 * then says why nothing shows instead of fighting it.
 */
public class DebugToggleModule extends Module {
	private final Identifier entry;
	private final String what;
	/** Whether this module switched the entry on, so turning the module off only undoes its own change. */
	private boolean switchedOn;
	/** Whether the "hidden by Reduced Debug Info" note was shown since it last became hidden. */
	private boolean toldHidden;

	private DebugToggleModule(String name, String description, String what, Item icon, Identifier entry) {
		super(name, description, Category.MECHANIC);
		setIcon(icon);
		this.entry = entry;
		this.what = what;
	}

	public static DebugToggleModule hitbox() {
		return new DebugToggleModule("Hitbox", "Shows entity hitboxes (like F3 + B).", "Hitboxes", Items.SCAFFOLDING, DebugScreenEntries.ENTITY_HITBOXES);
	}

	public static DebugToggleModule chunkBorders() {
		return new DebugToggleModule("Chunk Borders", "Shows chunk borders (like F3 + G).", "Chunk borders", Items.GRASS_BLOCK, DebugScreenEntries.CHUNK_BORDERS);
	}

	@Override
	public void onTick() {
		// Applied on tick rather than in onEnable: the debug list doesn't exist yet when the config loads.
		if (mc.debugEntries == null) return;
		// Compare the stored status, not whether it's showing: while Reduced Debug Info hides it,
		// it never shows, and setting it again every tick would rewrite the debug profile file.
		if (mc.debugEntries.getStatus(entry) != DebugScreenEntryStatus.ALWAYS_ON) {
			mc.debugEntries.setStatus(entry, DebugScreenEntryStatus.ALWAYS_ON);
			switchedOn = true;
		}
		boolean hidden = mc.player != null && mc.showOnlyReducedInfo();
		if (hidden && !toldHidden) {
			mc.player.displayClientMessage(Component.literal(what + " are hidden: Reduced Debug Info is on (your Chat Settings, or this server's rules).")
				.withStyle(ChatFormatting.GRAY), true);
		}
		toldHidden = hidden;
	}

	@Override
	protected void onDisable() {
		if (mc.debugEntries != null && switchedOn) mc.debugEntries.setStatus(entry, DebugScreenEntryStatus.NEVER);
		switchedOn = false;
		toldHidden = false;
	}
}
