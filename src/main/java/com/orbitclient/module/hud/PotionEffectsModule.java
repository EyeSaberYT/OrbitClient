package com.orbitclient.module.hud;

import com.orbitclient.module.HudModule;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.setting.NumberSetting;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Render2D;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Active effects with their icon and time left. Cards style adds a background, an accent in the
 * effect's color and a bar that drains as the effect runs out. New effects slide in, expired ones
 * slide out, the list reflows smoothly, and effects about to run out blink.
 */
public class PotionEffectsModule extends HudModule {
	private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

	public final BooleanSetting hideVanilla;
	private final ModeSetting style;
	private final BooleanSetting icons;
	private final BooleanSetting timeBar;
	private final NumberSetting background;
	private final BooleanSetting effectColors;
	private final BooleanSetting blink;
	private final ColorSetting nameColor;
	private final ColorSetting durationColor;

	private static final class Entry {
		Holder<MobEffect> effect;
		String name;
		String duration;
		int color;
		int ticksLeft;
		/** Longest duration seen, so the bar shows how much is left. */
		int ticksMax;
		boolean present;
		float anim;
		float bar = -1;
		float y = -1;
	}

	private final Map<String, Entry> entries = new LinkedHashMap<>();
	private final Anim.Clock clock = new Anim.Clock();

	public PotionEffectsModule() {
		super("Potion Effects", "Shows your active potion effects and their time left.", 0.90, 0.12);
		setIcon(Items.POTION);
		hideVanilla = add(new BooleanSetting("Hide Vanilla Icons", true));
		style = add(new ModeSetting("Style", "Cards", "Cards", "Compact", "Text"));
		icons = add(new BooleanSetting("Show Icons", true));
		timeBar = add(new BooleanSetting("Time Bar", true).visibleWhen(() -> style.is("Cards")));
		background = add(new NumberSetting("Background", 55, 0, 100, 5).visibleWhen(() -> !style.is("Text")));
		effectColors = add(new BooleanSetting("Use Effect Colors", true));
		blink = add(new BooleanSetting("Blink When Ending", true));
		nameColor = add(new ColorSetting("Name Color", 0xFFFFFFFF).visibleWhen(() -> !effectColors.isOn()));
		durationColor = add(new ColorSetting("Duration Color", 0xFFAAAAAA));
	}

	private static String formatDuration(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) return "∞";
		int seconds = effect.getDuration() / 20;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}

	/** Syncs the entry list with the player's effects (or examples in the editor). */
	private void update(boolean editing) {
		for (Entry e : entries.values()) e.present = false;
		boolean any = false;
		if (mc.player != null) {
			for (MobEffectInstance effect : mc.player.getActiveEffects()) {
				String key = effect.getEffect().getRegisteredName();
				int amp = effect.getAmplifier();
				String name = effect.getEffect().value().getDisplayName().getString()
					+ (amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1));
				put(key, effect.getEffect(), name, formatDuration(effect), effect.getEffect().value().getColor(),
					effect.isInfiniteDuration() ? Integer.MAX_VALUE : effect.getDuration());
				any = true;
			}
		}
		if (!any && editing) {
			put("preview_speed", MobEffects.SPEED, "Speed II", "1:30", 0x33EBFF, 1800);
			put("preview_strength", MobEffects.STRENGTH, "Strength", "0:07", 0xFFC700, 140);
			entries.get("preview_speed").ticksMax = 3600;
			entries.get("preview_strength").ticksMax = 3600;
		}
	}

	private void put(String key, Holder<MobEffect> effect, String name, String duration, int color, int ticks) {
		Entry e = entries.computeIfAbsent(key, k -> new Entry());
		e.effect = effect;
		e.name = name;
		e.duration = duration;
		e.color = color;
		// A refreshed effect (longer than before) restarts the bar from its new length.
		if (ticks > e.ticksLeft || e.ticksMax == 0) e.ticksMax = Math.max(ticks, 1);
		e.ticksLeft = ticks;
		e.present = true;
	}

	private int entryHeight() {
		return switch (style.get()) {
			case "Cards" -> 26;
			case "Compact" -> 15;
			default -> icons.isOn() ? 22 : 21;
		};
	}

	private int iconSpace() {
		if (!icons.isOn()) return 0;
		return style.is("Compact") ? 13 : 22;
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		float dt = clock.tick();
		update(editing);
		float base = Render2D.alpha;
		int width = getWidth();
		int step = entryHeight() + (style.is("Text") ? 0 : 3);
		int index = 0;
		for (Iterator<Entry> it = entries.values().iterator(); it.hasNext(); ) {
			Entry e = it.next();
			e.anim = Anim.step(e.anim, e.present, e.present ? 0.25f : 0.2f, dt);
			if (!e.present && e.anim <= 0f) {
				it.remove();
				continue;
			}
			float target = index * step;
			e.y = e.y < 0 ? target : Anim.approach(e.y, target, 14f, dt);
			if (e.present) index++;
			float fraction = e.ticksLeft == Integer.MAX_VALUE ? 1f : Anim.clamp01(e.ticksLeft / (float) e.ticksMax);
			e.bar = e.bar < 0 ? fraction : Anim.approach(e.bar, fraction, 10f, dt);

			float a = Anim.easeOutCubic(e.anim);
			boolean ending = e.present && e.ticksLeft < 200;
			float pulse = blink.isOn() && ending ? 0.6f + 0.4f * (float) Math.cos(System.currentTimeMillis() / 1000.0 * Math.PI * 2) : 1f;
			Render2D.alpha = base * a;
			int x = Math.round((1 - a) * 14);
			int y = Math.round(e.y);
			int effectColor = 0xFF000000 | e.color;
			int color = effectColors.isOn() ? effectColor : nameColor.getColor();
			switch (style.get()) {
				case "Cards" -> renderCard(graphics, e, x, y, width, effectColor, color, pulse);
				case "Compact" -> renderCompact(graphics, e, x, y, width, effectColor, color, pulse);
				default -> {
					drawIcon(graphics, e, x, y + 1, 18, pulse);
					int tx = x + iconSpace();
					Render2D.alpha = base * a * pulse;
					Render2D.text(graphics, mc.font, e.name, tx, y, color, true);
					Render2D.text(graphics, mc.font, e.duration, tx, y + 10, durationColor.getColor(), true);
				}
			}
		}
		Render2D.alpha = base;
	}

	private void renderCard(GuiGraphics g, Entry e, int x, int y, int w, int effectColor, int color, float pulse) {
		int h = entryHeight();
		Render2D.roundedRect(g, x, y, w, h, 4, Render2D.withAlpha(0xFF0B0D12, background.getFloat() / 100f));
		// Accent strip in the effect's color.
		Render2D.roundedRect(g, x, y + 4, 2, h - 8, 1, effectColor);
		drawIcon(g, e, x + 6, y + 4, 18, pulse);
		int tx = x + 6 + iconSpace();
		float base = Render2D.alpha;
		Render2D.text(g, mc.font, e.name, tx, y + 4, color, true);
		Render2D.alpha = base * pulse;
		int durColor = e.ticksLeft < 200 ? 0xFFFF6B6B : durationColor.getColor();
		Render2D.text(g, mc.font, e.duration, tx, y + 14, durColor, true);
		Render2D.alpha = base;
		if (timeBar.isOn()) {
			int bx = tx + mc.font.width(e.duration) + 5, bw = x + w - 6 - bx;
			if (bw > 6) {
				Render2D.roundedRect(g, bx, y + 17, bw, 2, 1, 0x30FFFFFF);
				int fill = Math.round(bw * e.bar);
				if (fill > 0) Render2D.roundedRect(g, bx, y + 17, fill, 2, 1, effectColor);
			}
		}
	}

	private void renderCompact(GuiGraphics g, Entry e, int x, int y, int w, int effectColor, int color, float pulse) {
		int h = entryHeight();
		Render2D.roundedRect(g, x, y, w, h, 3, Render2D.withAlpha(0xFF0B0D12, background.getFloat() / 100f));
		Render2D.fill(g, x, y + 3, x + 1, y + h - 3, effectColor);
		drawIcon(g, e, x + 3, y + 2, 11, pulse);
		Render2D.text(g, mc.font, e.name, x + 4 + iconSpace(), y + 4, color, true);
		float base = Render2D.alpha;
		Render2D.alpha = base * pulse;
		int durW = mc.font.width(e.duration);
		Render2D.text(g, mc.font, e.duration, x + w - 4 - durW, y + 4, e.ticksLeft < 200 ? 0xFFFF6B6B : durationColor.getColor(), true);
		Render2D.alpha = base;
	}

	private void drawIcon(GuiGraphics g, Entry e, int x, int y, int size, float pulse) {
		if (!icons.isOn() || e.effect == null) return;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(e.effect), x, y, size, size,
			Render2D.withAlpha(0xFFFFFFFF, Render2D.alpha * pulse));
	}

	@Override
	public boolean shouldRender() {
		return mc.player != null && !mc.player.getActiveEffects().isEmpty();
	}

	@Override
	public int getWidth() {
		int max = 40;
		for (Entry e : entries.values()) {
			int w = switch (style.get()) {
				case "Cards" -> 6 + iconSpace() + Math.max(mc.font.width(e.name), mc.font.width(e.duration) + (timeBar.isOn() ? 30 : 0)) + 8;
				case "Compact" -> 4 + iconSpace() + mc.font.width(e.name) + 10 + mc.font.width(e.duration) + 4;
				default -> iconSpace() + Math.max(mc.font.width(e.name), mc.font.width(e.duration));
			};
			max = Math.max(max, w);
		}
		return max;
	}

	@Override
	public int getHeight() {
		long count = Math.max(1, entries.values().stream().filter(e -> e.present).count());
		int gap = style.is("Text") ? 0 : 3;
		return (int) (count * (entryHeight() + gap) - gap);
	}
}
