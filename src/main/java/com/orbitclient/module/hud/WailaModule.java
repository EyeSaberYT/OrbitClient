package com.orbitclient.module.hud;

import com.orbitclient.util.Render2D;
import com.orbitclient.module.HudModule;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** "What Am I Looking At": the block or entity under your crosshair. */
public class WailaModule extends HudModule {
	private final BooleanSetting background;
	private final BooleanSetting showEntities;
	private final BooleanSetting centerHorizontally;
	private final ColorSetting textColor;

	public WailaModule() {
		super("WAILA", "Shows what block or mob you are looking at.", 0.5, 0.01);
		setIcon(Items.KNOWLEDGE_BOOK);
		markNew();
		background = add(new BooleanSetting("Background", true));
		showEntities = add(new BooleanSetting("Show Mobs", true));
		centerHorizontally = add(new BooleanSetting("Center Horizontally", true));
		textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
	}

	/** Center the whole label by its live width, so short and long player names stay aligned. */
	@Override
	public int getX(int screenWidth) {
		if (!centerHorizontally.isOn()) return super.getX(screenWidth);
		return Math.max(0, (screenWidth - getScaledWidth()) / 2);
	}

	/** A manual HUD-editor drag switches to custom placement so dragging still feels natural. */
	@Override
	public void setPosition(int x, int y, int screenWidth, int screenHeight) {
		centerHorizontally.set(false);
		super.setPosition(x, y, screenWidth, screenHeight);
	}

	/** {name, icon or null}, or null when looking at nothing. */
	private Object[] target(boolean editing) {
		HitResult hit = mc.hitResult;
		if (mc.level != null && hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
			BlockState state = mc.level.getBlockState(bhr.getBlockPos());
			return new Object[]{state.getBlock().getName().getString(), new ItemStack(state.getBlock().asItem())};
		}
		if (showEntities.isOn() && hit instanceof EntityHitResult ehr) {
			return new Object[]{ehr.getEntity().getName().getString(), null};
		}
		return editing ? new Object[]{"Grass Block", new ItemStack(Items.GRASS_BLOCK)} : null;
	}

	@Override
	public void render(GuiGraphics graphics, boolean editing) {
		Object[] t = target(editing);
		if (t == null) return;
		if (background.isOn()) Render2D.fill(graphics, 0, 0, getWidth(), getHeight(), 0x6F000000);
		ItemStack icon = (ItemStack) t[1];
		int textX = 5;
		if (icon != null && !icon.isEmpty()) {
			graphics.renderItem(icon, 3, 2);
			textX = 22;
		}
		Render2D.text(graphics, mc.font, (String) t[0], textX, 6, textColor.getColor(), true);
	}

	@Override
	public boolean shouldRender() {
		return target(false) != null;
	}

	@Override
	public int getWidth() {
		Object[] t = target(true);
		if (t == null) return 60;
		return mc.font.width((String) t[0]) + (t[1] != null ? 27 : 10);
	}

	@Override
	public int getHeight() {
		return 20;
	}
}
