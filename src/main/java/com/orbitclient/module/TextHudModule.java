package com.orbitclient.module;

import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Render2D;
import net.minecraft.client.gui.GuiGraphics;

/** The classic Lunar text box: "[FPS: 144]" on a translucent black background. */
public abstract class TextHudModule extends HudModule {
	private static final int BOX_MIN_WIDTH = 56;
	private static final int BOX_HEIGHT = 18;

	public final BooleanSetting background;
	public final ColorSetting backgroundColor;
	public final ColorSetting textColor;
	public final BooleanSetting textShadow;
	public final ModeSetting brackets;

	private final Anim.Clock clock = new Anim.Clock();
	/** Box width as drawn, easing toward the real width so the box never jumps. */
	private float shownWidth = -1;
	/** Last non-empty text, so the box can fade out showing what it last said. */
	private String lastText = "";
	private String lastRendered;

	protected TextHudModule(String name, String description, double defaultX, double defaultY) {
		this(name, description, Category.HUD, defaultX, defaultY);
	}

	protected TextHudModule(String name, String description, Category category, double defaultX, double defaultY) {
		super(name, description, category, defaultX, defaultY);
		background = add(new BooleanSetting("Background", true));
		backgroundColor = add(new ColorSetting("Background Color", 0x6F000000).visibleWhen(background::isOn));
		textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
		textShadow = add(new BooleanSetting("Text Shadow", true));
		brackets = add(new ModeSetting("Brackets", "None", "None", "[ ]", "( )", "< >"));
	}

	/** The text to show, e.g. "144 FPS". */
	protected abstract String getText();

	/** Example text for the HUD editor when there is no real value (e.g. outside a world). */
	protected String getPreviewText() {
		return getText();
	}

	/** Whether the box should pop when its text changes (combo, reach). */
	protected boolean pulseOnChange() {
		return false;
	}

	private String decorated(boolean editing) {
		String text = mc.player == null ? null : getText();
		if (editing && (text == null || text.isEmpty())) text = getPreviewText();
		if (text == null || text.isEmpty()) text = lastText;
		else lastText = text;
		return switch (brackets.get()) {
			case "[ ]" -> "[" + text + "]";
			case "( )" -> "(" + text + ")";
			case "< >" -> "<" + text + ">";
			default -> text;
		};
	}

	private int widthFor(String text) {
		int textWidth = mc.font.width(text);
		return background.isOn() ? Math.max(BOX_MIN_WIDTH, textWidth + 10) : textWidth;
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		float dt = clock.tick();
		String text = decorated(editing);
		if (pulseOnChange() && lastRendered != null && !text.equals(lastRendered)) pulse();
		lastRendered = text;

		int target = widthFor(text);
		shownWidth = shownWidth < 0 ? target : Anim.approach(shownWidth, target, 18f, dt);
		int width = Math.round(shownWidth);
		int textWidth = mc.font.width(text);
		if (background.isOn()) {
			Render2D.fill(graphics, 0, 0, width, BOX_HEIGHT, backgroundColor.getColor());
			Render2D.text(graphics, mc.font, text, (width - textWidth) / 2, 5, textColor.getColor(), textShadow.isOn());
		} else {
			Render2D.text(graphics, mc.font, text, 0, 0, textColor.getColor(), textShadow.isOn());
		}
	}

	@Override
	public int getWidth() {
		return widthFor(decorated(true));
	}

	@Override
	public int getHeight() {
		return background.isOn() ? BOX_HEIGHT : mc.font.lineHeight;
	}
}
