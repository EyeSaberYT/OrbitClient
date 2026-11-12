package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import net.minecraft.ChatFormatting;
//? if >=1.21.9 {
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.network.chat.Component;
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
	/** Hitboxes (F3 + B), else chunk borders (F3 + G). */
	private final boolean hitboxes;
	private final String what;
	/** Whether this module switched the renderer on, so turning the module off only undoes its own change. */
	private boolean switchedOn;
	/** Whether the "hidden by Reduced Debug Info" note was shown since it last became hidden. */
	private boolean toldHidden;

	private DebugToggleModule(String name, String description, String what, Item icon, boolean hitboxes) {
		super(name, description, Category.MECHANIC);
		setIcon(icon);
		this.hitboxes = hitboxes;
		this.what = what;
	}

	public static DebugToggleModule hitbox() {
		return new DebugToggleModule("Hitbox", "Shows entity hitboxes (like F3 + B).", "Hitboxes", Items.SCAFFOLDING, true);
	}

	public static DebugToggleModule chunkBorders() {
		return new DebugToggleModule("Chunk Borders", "Shows chunk borders (like F3 + G).", "Chunk borders", Items.GRASS_BLOCK, false);
	}

	@Override
	public void onTick() {
		//? if >=1.21.9 {
		// Applied on tick rather than in onEnable: the debug list doesn't exist yet when the config loads.
		if (mc.debugEntries == null) return;
		// Compare the stored status, not whether it's showing: while Reduced Debug Info hides it,
		// it never shows, and setting it again every tick would rewrite the debug profile file.
		if (mc.debugEntries.getStatus(entry()) != DebugScreenEntryStatus.ALWAYS_ON) {
			mc.debugEntries.setStatus(entry(), DebugScreenEntryStatus.ALWAYS_ON);
			switchedOn = true;
		}
		//?} else {
		/*// Before 1.21.9 they're plain switches, kept on every tick (F3 + B or G would turn them off).
		show(true);
		switchedOn = true;
		*///?}
		boolean hidden = mc.player != null && mc.showOnlyReducedInfo();
		if (hidden && !toldHidden) {
			Component message = Component.literal(what + " are hidden: Reduced Debug Info is on (your Chat Settings, or this server's rules).")
				.withStyle(ChatFormatting.GRAY);
			mc.player.displayClientMessage(message, true);
		}
		toldHidden = hidden;
	}

	@Override
	protected void onDisable() {
		//? if >=1.21.9 {
		if (mc.debugEntries != null && switchedOn) mc.debugEntries.setStatus(entry(), DebugScreenEntryStatus.NEVER);
		//?} else
		/*if (switchedOn) show(false);*/
		switchedOn = false;
		toldHidden = false;
	}

	//? if >=1.21.9 {
	private Identifier entry() {
		return hitboxes ? DebugScreenEntries.ENTITY_HITBOXES : DebugScreenEntries.CHUNK_BORDERS;
	}
	//?} else {
	/*private void show(boolean on) {
		if (hitboxes) {
			mc.getEntityRenderDispatcher().setRenderHitBoxes(on);
		} else if (mc.debugRenderer.switchRenderChunkborder() != on) {
			// Chunk borders only have a switch: switched back when that left them the wrong way.
			mc.debugRenderer.switchRenderChunkborder();
		}
	}
	*///?}
}
