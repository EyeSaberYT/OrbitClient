package com.orbitclient.module.hud;

import com.orbitclient.module.HudModule;
import com.orbitclient.setting.NumberSetting;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Render2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shows "+3 Arrow" / "-1 Ender Pearl" as items enter and leave your inventory. Lines slide in and fade out. */
public class ItemTrackerModule extends HudModule {
	private static final long FADE_MS = 400;

	private static final class Change {
		final Item item;
		final int amount;
		final long time;
		float y = -1;

		Change(Item item, int amount, long time) {
			this.item = item;
			this.amount = amount;
			this.time = time;
		}
	}

	private final NumberSetting duration;
	private final List<Change> changes = new ArrayList<>();
	private final Anim.Clock clock = new Anim.Clock();
	private Map<Item, Integer> last;

	public ItemTrackerModule() {
		super("Item Tracker", "Shows items you pick up and lose.", 0.70, 0.40);
		setIcon(Items.BUNDLE);
		markNew();
		duration = add(new NumberSetting("Show For (seconds)", 4, 1, 10, 1));
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			last = null;
			return;
		}
		Map<Item, Integer> now = new HashMap<>();
		var inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty()) now.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		// Skip the first tick and open containers, where items just move around.
		if (last != null && mc.screen == null) {
			long t = System.currentTimeMillis();
			for (Map.Entry<Item, Integer> e : now.entrySet()) {
				int diff = e.getValue() - last.getOrDefault(e.getKey(), 0);
				if (diff != 0) changes.add(new Change(e.getKey(), diff, t));
			}
			for (Map.Entry<Item, Integer> e : last.entrySet()) {
				if (!now.containsKey(e.getKey())) changes.add(new Change(e.getKey(), -e.getValue(), t));
			}
		}
		last = now;
		long cutoff = System.currentTimeMillis() - lifetimeMs();
		changes.removeIf(c -> c.time < cutoff);
		while (changes.size() > 8) changes.removeFirst();
	}

	private long lifetimeMs() {
		return duration.getInt() * 1000L;
	}

	private List<Change> visible(boolean editing) {
		if (changes.isEmpty() && editing) {
			long t = System.currentTimeMillis() - 1000;
			return List.of(new Change(Items.ARROW, 12, t), new Change(Items.ENDER_PEARL, -1, t));
		}
		return changes;
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		float dt = clock.tick();
		long now = System.currentTimeMillis();
		float base = Render2D.alpha;
		int index = 0;
		for (Change c : visible(editing)) {
			float in = Anim.easeOutCubic(Anim.since(c.time, 0.2f));
			float out = editing ? 1f : Anim.clamp01((c.time + lifetimeMs() - now) / (float) FADE_MS);
			float target = index++ * 10;
			c.y = c.y < 0 ? target : Anim.approach(c.y, target, 14f, dt);
			Render2D.alpha = base * Math.min(in, out);
			String text = (c.amount > 0 ? "+" : "") + c.amount + " " + new ItemStack(c.item).getHoverName().getString();
			Render2D.text(graphics, mc.font, text, Math.round((1 - in) * -10), Math.round(c.y), c.amount > 0 ? 0xFF55FF55 : 0xFFFF5555, true);
		}
		Render2D.alpha = base;
	}

	@Override
	public boolean shouldRender() {
		return !changes.isEmpty();
	}

	@Override
	public int getWidth() {
		return 100;
	}

	@Override
	public int getHeight() {
		return Math.max(9, visible(true).size() * 10 - 1);
	}
}
