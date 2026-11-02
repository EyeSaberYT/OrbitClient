package com.orbitclient.module.hud;

import com.orbitclient.util.Render2D;
import com.orbitclient.module.HudModule;
import com.orbitclient.setting.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shows items that are on cooldown (ender pearls, chorus fruit, shields, wind charges...) with a bar. */
public class CooldownsModule extends HudModule {
	private static final int BAR_W = 40;
	private final ColorSetting barColor;

	public CooldownsModule() {
		super("Cooldowns", "Shows item cooldowns such as ender pearls.", 0.48, 0.60);
		setIcon(Items.ENDER_PEARL);
		barColor = add(new ColorSetting("Bar Color", 0xFF55AAFF));
	}

	/** {stack, percent remaining}. */
	private List<Object[]> entries(boolean editing) {
		List<Object[]> list = new ArrayList<>();
		if (mc.player != null) {
			Map<Item, ItemStack> seen = new LinkedHashMap<>();
			var inv = mc.player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack stack = inv.getItem(i);
				if (!stack.isEmpty() && !seen.containsKey(stack.getItem())) seen.put(stack.getItem(), stack);
			}
			for (ItemStack stack : seen.values()) {
				float pct = mc.player.getCooldowns().getCooldownPercent(stack, 0f);
				if (pct > 0) list.add(new Object[]{stack, pct});
			}
		}
		if (list.isEmpty() && editing) list.add(new Object[]{getIcon(), 0.6f});
		return list;
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		int y = 0;
		for (Object[] e : entries(editing)) {
			graphics.renderItem((ItemStack) e[0], 0, y);
			int filled = Math.round(BAR_W * (float) e[1]);
			Render2D.fill(graphics, 19, y + 6, 19 + BAR_W, y + 10, 0x80000000);
			Render2D.fill(graphics, 19, y + 6, 19 + filled, y + 10, barColor.getColor());
			y += 17;
		}
	}

	@Override
	public boolean shouldRender() {
		return !entries(false).isEmpty();
	}

	@Override
	public int getWidth() {
		return 19 + BAR_W;
	}

	@Override
	public int getHeight() {
		return Math.max(16, entries(true).size() * 17 - 1);
	}
}
