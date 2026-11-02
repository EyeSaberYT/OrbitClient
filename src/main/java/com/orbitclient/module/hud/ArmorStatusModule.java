package com.orbitclient.module.hud;

import com.orbitclient.util.Render2D;
import com.orbitclient.module.HudModule;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.setting.ModeSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorStatusModule extends HudModule {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** Example items for the HUD editor; created lazily because items aren't ready during mod init. */
	private static ItemStack[] preview;

	private final ModeSetting durability;
	private final BooleanSetting showHeldItem;
	private final ColorSetting textColor;

	public ArmorStatusModule() {
		super("Armor Status", "Shows your armor and its durability.", 0.90, 0.40);
		setIcon(Items.IRON_CHESTPLATE);
		durability = add(new ModeSetting("Durability", "Value", "Value", "Percent", "Hidden"));
		showHeldItem = add(new BooleanSetting("Show Held Item", true));
		textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
	}

	private static ItemStack[] preview() {
		if (preview == null) {
			preview = new ItemStack[]{
				new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
				new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD)
			};
		}
		return preview;
	}

	private List<ItemStack> items(boolean editing) {
		List<ItemStack> list = new ArrayList<>();
		if (mc.player == null) {
			if (editing) for (int i = 0; i < (showHeldItem.isOn() ? 5 : 4); i++) list.add(preview()[i]);
			return list;
		}
		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = mc.player.getItemBySlot(slot);
			if (!stack.isEmpty()) list.add(stack);
		}
		if (showHeldItem.isOn() && !mc.player.getMainHandItem().isEmpty()) list.add(mc.player.getMainHandItem());
		if (list.isEmpty() && editing) for (int i = 0; i < 4; i++) list.add(preview()[i]);
		return list;
	}

	private String durabilityText(ItemStack stack) {
		if (durability.is("Hidden")) return "";
		if (!stack.isDamageableItem()) return stack.getCount() > 1 ? String.valueOf(stack.getCount()) : "";
		int left = stack.getMaxDamage() - stack.getDamageValue();
		if (durability.is("Percent")) return (left * 100 / stack.getMaxDamage()) + "%";
		return String.valueOf(left);
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		int y = 0;
		for (ItemStack stack : items(editing)) {
			graphics.renderItem(stack, 0, y);
			String text = durabilityText(stack);
			if (!text.isEmpty()) {
				int color = textColor.getColor();
				if (stack.isDamageableItem()) {
					float left = 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
					if (left < 0.25f) color = 0xFFFF5555;
					else if (left < 0.5f) color = 0xFFFFAA00;
				}
				Render2D.text(graphics, mc.font, text, 18, y + 4, color, true);
			}
			y += 16;
		}
	}

	@Override
	public boolean shouldRender() {
		return !items(false).isEmpty();
	}

	@Override
	public int getWidth() {
		int max = 0;
		for (ItemStack stack : items(true)) max = Math.max(max, mc.font.width(durabilityText(stack)));
		return 18 + Math.max(max, 20);
	}

	@Override
	public int getHeight() {
		return Math.max(16, items(true).size() * 16);
	}
}
