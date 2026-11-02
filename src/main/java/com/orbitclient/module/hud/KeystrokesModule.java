package com.orbitclient.module.hud;

import net.minecraft.world.item.Items;
import com.orbitclient.module.Category;
import com.orbitclient.module.HudModule;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.util.CpsTracker;
import com.orbitclient.util.Render2D;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

public class KeystrokesModule extends HudModule {
	private static final int KEY = 22;
	private static final int GAP = 2;
	private static final int WIDTH = KEY * 3 + GAP * 2;

	private final BooleanSetting showMouse;
	private final BooleanSetting showSpace;
	private final BooleanSetting showCps;
	private final ColorSetting backgroundColor;
	private final ColorSetting pressedColor;
	private final ColorSetting textColor;
	private final ColorSetting pressedTextColor;

	private final Map<String, Float> fade = new HashMap<>();
	private long lastFrame = System.currentTimeMillis();

	public KeystrokesModule() {
		super("Key Strokes", "Shows WASD, mouse and jump key presses.", Category.HUD, 0.005, 0.62);
		setIcon(Items.LEVER);
		showMouse = add(new BooleanSetting("Show Mouse Buttons", true));
		showCps = add(new BooleanSetting("Show CPS", true).visibleWhen(showMouse::isOn));
		showSpace = add(new BooleanSetting("Show Spacebar", true));
		backgroundColor = add(new ColorSetting("Background Color", 0x66000000));
		pressedColor = add(new ColorSetting("Pressed Color", 0x99FFFFFF));
		textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
		pressedTextColor = add(new ColorSetting("Pressed Text Color", 0xFF000000));
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		long now = System.currentTimeMillis();
		float delta = Math.min(0.1f, (now - lastFrame) / 1000f);
		lastFrame = now;

		var o = mc.options;
		key(graphics, "W", "W", o.keyUp, KEY + GAP, 0, KEY, KEY, delta);
		key(graphics, "A", "A", o.keyLeft, 0, KEY + GAP, KEY, KEY, delta);
		key(graphics, "S", "S", o.keyDown, KEY + GAP, KEY + GAP, KEY, KEY, delta);
		key(graphics, "D", "D", o.keyRight, (KEY + GAP) * 2, KEY + GAP, KEY, KEY, delta);

		int y = (KEY + GAP) * 2;
		if (showMouse.isOn()) {
			int half = (WIDTH - GAP) / 2;
			String lmb = showCps.isOn() ? CpsTracker.left() + " CPS" : "LMB";
			String rmb = showCps.isOn() ? CpsTracker.right() + " CPS" : "RMB";
			key(graphics, "LMB", lmb, o.keyAttack, 0, y, half, KEY, delta);
			key(graphics, "RMB", rmb, o.keyUse, half + GAP, y, WIDTH - half - GAP, KEY, delta);
			y += KEY + GAP;
		}
		if (showSpace.isOn()) {
			float p = animate("Space", o.keyJump.isDown(), delta);
			Render2D.fill(graphics, 0, y, WIDTH, y + 12, Render2D.lerpColor(backgroundColor.getColor(), pressedColor.getColor(), p));
			int lineColor = Render2D.lerpColor(textColor.getColor(), pressedTextColor.getColor(), p);
			Render2D.fill(graphics, WIDTH / 2 - 12, y + 5, WIDTH / 2 + 12, y + 6, lineColor);
		}
	}

	private void key(GuiGraphics graphics, String id, String label, KeyMapping mapping, int x, int y, int w, int h, float delta) {
		float p = animate(id, mapping.isDown(), delta);
		Render2D.fill(graphics, x, y, x + w, y + h, Render2D.lerpColor(backgroundColor.getColor(x), pressedColor.getColor(x), p));
		int color = Render2D.lerpColor(textColor.getColor(x), pressedTextColor.getColor(x), p);
		Render2D.text(graphics, mc.font, label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2, color, false);
	}

	private float animate(String id, boolean down, float delta) {
		// Fast press-in, slightly slower release, eased so it feels soft rather than linear.
		float current = com.orbitclient.util.Anim.step(fade.getOrDefault(id, 0f), down, down ? 0.06f : 0.14f, delta);
		fade.put(id, current);
		return com.orbitclient.util.Anim.easeInOutCubic(current);
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		int h = (KEY + GAP) * 2 - GAP;
		if (showMouse.isOn()) h += KEY + GAP;
		if (showSpace.isOn()) h += 12 + GAP;
		return h;
	}
}
