package com.orbitclient.legacy.gui;

import com.orbitclient.cosmetics.CosmeticSlot;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * The wardrobe screen for Minecraft 1.13.2: it only passes drawing and input on to WardrobeView, which is the
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
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		view.clicked((int) mouseX, (int) mouseY, button);
		return true;
	}

	@Override
	public boolean mouseScrolled(double amount) {
		if (amount != 0) view.scrolled(amount < 0 ? 1 : -1);
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (key == GLFW.GLFW_KEY_F5) {
			view.refresh();
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}
}
