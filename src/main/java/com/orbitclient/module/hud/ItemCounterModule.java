package com.orbitclient.module.hud;

import com.orbitclient.util.Render2D;
import com.orbitclient.module.HudModule;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Counts useful PvP items in your inventory: arrows, golden apples, pearls, totems, blocks. */
public class ItemCounterModule extends HudModule {
	private final List<Object[]> tracked = new ArrayList<>(); // {BooleanSetting, Item[]}
	private final BooleanSetting hideEmpty;
	private final ColorSetting textColor;

	public ItemCounterModule() {
		super("Item Counter", "Counts arrows, golden apples, pearls and more in your inventory.", 0.90, 0.70);
		setIcon(Items.ARROW);
		track("Arrows", true, Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW);
		track("Golden Apples", true, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
		track("Ender Pearls", true, Items.ENDER_PEARL);
		track("Totems", false, Items.TOTEM_OF_UNDYING);
		track("Wind Charges", false, Items.WIND_CHARGE);
		track("Cobwebs", false, Items.COBWEB);
		hideEmpty = add(new BooleanSetting("Hide Empty", true));
		textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
	}

	private void track(String name, boolean on, Item... items) {
		tracked.add(new Object[]{add(new BooleanSetting("Show " + name, on)), items});
	}

	private int count(Item[] items) {
		if (mc.player == null) return 0;
		int total = 0;
		var inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			for (Item item : items) if (stack.is(item)) total += stack.getCount();
		}
		return total;
	}

	/** Visible rows: {icon item, count}. */
	private List<Object[]> rows(boolean editing) {
		List<Object[]> rows = new ArrayList<>();
		for (Object[] t : tracked) {
			if (!((BooleanSetting) t[0]).isOn()) continue;
			Item[] items = (Item[]) t[1];
			int count = mc.player == null ? 16 : count(items);
			if (count == 0 && hideEmpty.isOn() && !editing) continue;
			rows.add(new Object[]{items[0], count});
		}
		return rows;
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		int y = 0;
		for (Object[] row : rows(editing)) {
			graphics.renderItem(new ItemStack((Item) row[0]), 0, y);
			Render2D.text(graphics, mc.font, String.valueOf(row[1]), 19, y + 4, textColor.getColor(), true);
			y += 17;
		}
	}

	@Override
	public boolean shouldRender() {
		return !rows(false).isEmpty();
	}

	@Override
	public int getWidth() {
		return 40;
	}

	@Override
	public int getHeight() {
		return Math.max(16, rows(true).size() * 17 - 1);
	}
}
