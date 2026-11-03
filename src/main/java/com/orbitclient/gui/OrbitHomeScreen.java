package com.orbitclient.gui;

import com.orbitclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/** Orbit's replacement for Minecraft's title screen. */
public final class OrbitHomeScreen extends TitleScreen {
	private boolean accountCard;

	public OrbitHomeScreen() {
		super();
	}

	@Override
	protected void init() {
		clearWidgets();
		int center = width / 2;
		int buttonWidth = Math.min(190, (width - 56) / 2);
		int gap = 10;
		int rowWidth = buttonWidth * 2 + gap;
		int left = center - rowWidth / 2;
		int top = height / 2 + 18;
		
		addRenderableWidget(Button.builder(Component.literal("Singleplayer"), b ->
			minecraft.setScreen(new SelectWorldScreen(this)))
			.bounds(left, top, buttonWidth, 28).build());
		addRenderableWidget(Button.builder(Component.literal("Multiplayer"), b ->
			minecraft.setScreen(new JoinMultiplayerScreen(this)))
			.bounds(left + buttonWidth + gap, top, buttonWidth, 28).build());
		addRenderableWidget(Button.builder(Component.literal("Options"), b ->
			minecraft.setScreen(new OptionsScreen(this, minecraft.options)))
			.bounds(left, top + 34, buttonWidth, 24).build());
		addRenderableWidget(Button.builder(Component.literal("Quit Game"), b -> minecraft.stop())
			.bounds(left + buttonWidth + gap, top + 34, buttonWidth, 24).build());

		int accountWidth = Math.min(190, Math.max(132, width / 5));
		addRenderableWidget(Button.builder(accountLabel(), b -> accountCard = !accountCard)
			.bounds(width - accountWidth - 18, 18, accountWidth, 26).build());

		int dockY = height - 42;
		addRenderableWidget(Button.builder(Component.literal("ORBIT  ·  MODS"), b -> minecraft.setScreen(new OrbitScreen()))
			.bounds(18, dockY, 132, 26).build());
		addRenderableWidget(Button.builder(Component.literal("WARDROBE"), b -> minecraft.setScreen(new WardrobeScreen()))
			.bounds(158, dockY, 100, 26).build());
		addRenderableWidget(Button.builder(Component.literal("CLIENT SETTINGS"), b -> {
			OrbitScreen screen = new OrbitScreen();
			screen.showPanel("SETTINGS");
			minecraft.setScreen(screen);
		})
			.bounds(266, dockY, 138, 26).build());
	}

	private Component accountLabel() {
		String name = minecraft.getUser().getName();
		return Component.literal("◉  " + name + "  ▾");
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// Keep Minecraft's animated panorama, then lay an Orbit-colored veil and glass panels over it.
		renderPanorama(g, partialTick);
		g.fill(0, 0, width, height, 0x9B080C18);
		g.fill(0, 0, width, height / 3, 0x321B2B55);

		int panelW = Math.min(540, width - 36);
		int panelH = 218;
		int x = (width - panelW) / 2;
		int y = (height - panelH) / 2 - 18;
		g.fill(x, y, x + panelW, y + panelH, 0xC4101420);
		g.fill(x, y, x + panelW, y + 1, 0xB56E8CFF);
		g.fill(x, y + 1, x + 1, y + panelH, 0x665F7FE8);
		g.fill(x + panelW - 1, y + 1, x + panelW, y + panelH, 0x665F7FE8);
		g.fill(x + 1, y + panelH - 1, x + panelW - 1, y + panelH, 0x665F7FE8);

		g.drawString(font, "ORBIT CLIENT", x + 22, y + 18, 0xFF9DB4FF, false);
		g.drawString(font, "YOUR WORLD. YOUR ORBIT.", x + 22, y + 42, 0xFFFFFFFF, false);
		g.drawString(font, "Choose where you want to go.", x + 22, y + 62, 0xFFBEC6DA, false);

		// Small celestial mark as a polished title-screen signature.
		int markX = x + panelW - 52;
		int markY = y + 37;
		g.fill(markX - 14, markY - 14, markX + 14, markY + 14, 0x553F62C8);
		g.fill(markX - 5, markY - 5, markX + 5, markY + 5, 0xFFFFD27A);
		g.fill(markX - 2, markY - 11, markX + 2, markY + 11, 0xFF9DB4FF);
		g.fill(markX - 11, markY - 2, markX + 11, markY + 2, 0xFF9DB4FF);

		g.drawString(font, "PLAY", x + 22, y + 99, 0xFF9AA8C9, false);
		g.drawString(font, "The adventure starts here", x + 22, y + 119, 0xFFE7EAF2, false);
		g.drawString(font, "MODS  " + ModuleManager.all().size() + "   ·   FABRIC CLIENT", x + 22, y + panelH - 21, 0xFF929AB0, false);

		if (accountCard) renderAccountCard(g, mouseX, mouseY, width - 220, 52);
		g.drawString(font, "Minecraft Java Edition  ·  Orbit Client", 12, height - 12, 0xAABFC5D5, false);
		g.drawString(font, "© Mojang AB", width - font.width("© Mojang AB") - 12, height - 12, 0x88BFC5D5, false);
		super.render(g, mouseX, mouseY, partialTick);
	}

	private void renderAccountCard(GuiGraphics g, int mouseX, int mouseY, int x, int y) {
		int w = Math.min(204, width - 24);
		g.fill(x, y, x + w, y + 91, 0xF0161B2A);
		g.fill(x, y, x + w, y + 1, 0xFF6E8CFF);
		g.drawString(font, "CURRENT ACCOUNT", x + 12, y + 12, 0xFF9DB4FF, false);
		g.drawString(font, minecraft.getUser().getName(), x + 12, y + 30, 0xFFFFFFFF, false);
		g.drawString(font, "To switch accounts, change the", x + 12, y + 51, 0xFFB8C0D2, false);
		g.drawString(font, "account in your launcher, then", x + 12, y + 63, 0xFFB8C0D2, false);
		g.drawString(font, "restart Minecraft.", x + 12, y + 75, 0xFFB8C0D2, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
