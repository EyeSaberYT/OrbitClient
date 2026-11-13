package com.orbitclient.legacy.gui;

import com.orbitclient.cosmetics.CosmeticSlot;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The wardrobe screen for Minecraft 1.8.9 to 1.12.2: it only passes drawing and input on to WardrobeView, which is the
 * same for every version.
 */
public final class WardrobeScreen extends Screen {
	private final WardrobeView view;

	public WardrobeScreen() {
		this(CosmeticSlot.CAPE);
	}

	public WardrobeScreen(CosmeticSlot slot) {
		view = new WardrobeView(slot);
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	@Override
	public void removed() {
		view.closed();
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		view.render(client, textRenderer, width, height, mouseX, mouseY);
		super.render(mouseX, mouseY, tickDelta);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		view.clicked(mouseX, mouseY, button);
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) view.scrolled(wheel < 0 ? 1 : -1);
	}

	@Override
	protected void keyPressed(char character, int code) {
		if (code == Keyboard.KEY_F5) {
			view.refresh();
			return;
		}
		super.keyPressed(character, code);
	}
}
