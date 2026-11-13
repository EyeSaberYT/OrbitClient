package com.orbitclient.legacy.gui;

import com.orbitclient.cosmetics.CosmeticSlot;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

/**
 * The wardrobe screen for Minecraft 1.14: it only passes drawing and input on to WardrobeView, which is the
 * same for every version.
 */
public final class WardrobeScreen extends Screen {
	private final WardrobeView view;

	public WardrobeScreen() {
		this(CosmeticSlot.CAPE);
	}

	public WardrobeScreen(CosmeticSlot slot) {
		super(new LiteralText("Orbit Wardrobe"));
		view = new WardrobeView(slot);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		view.closed();
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		view.render(minecraft, font, width, height, mouseX, mouseY);
		super.render(mouseX, mouseY, tickDelta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		view.clicked((int) mouseX, (int) mouseY, button);
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
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
