package com.orbitclient.module.hud;

import com.orbitclient.OrbitClient;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import com.orbitclient.util.Render2D;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.util.List;

/**
 * Shows food info on the hunger bar itself: your hidden saturation as gold outlines around the
 * drumsticks, a flashing preview of what the food in your hand will restore, your exhaustion
 * (how close you are to losing the next bit of food, singleplayer only), and food values in tooltips.
 */
public class SaturationModule extends Module {
	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	private static final Identifier OUTLINE = Identifier.fromNamespaceAndPath(OrbitClient.MOD_ID, "saturation_outline");
	private static SaturationModule instance;

	private final BooleanSetting saturationOverlay;
	private final ColorSetting saturationColor;
	private final BooleanSetting foodPreview;
	private final BooleanSetting exhaustion;
	private final BooleanSetting tooltips;
	private boolean outlineReady;

	public SaturationModule() {
		super("Saturation", "Shows saturation and food previews on your hunger bar.", Category.HUD);
		setIcon(Items.COOKED_BEEF);
		instance = this;
		saturationOverlay = add(new BooleanSetting("Saturation Overlay", true));
		saturationColor = add(new ColorSetting("Saturation Color", 0xFFFFD447).visibleWhen(saturationOverlay::isOn));
		foodPreview = add(new BooleanSetting("Held Food Preview", true));
		exhaustion = add(new BooleanSetting("Exhaustion Bar", true));
		tooltips = add(new BooleanSetting("Food Values in Tooltips", true));
	}

	private static boolean on() {
		return instance != null && instance.isEnabled();
	}

	/** Same checks vanilla uses before it draws the food bar. */
	private static Player foodBarPlayer() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return null;
		if (mc.player.getVehicle() instanceof LivingEntity) return null;
		return mc.player;
	}

	private static int right(GuiGraphics g) {
		return g.guiWidth() / 2 + 91;
	}

	private static int top(GuiGraphics g) {
		return g.guiHeight() - 39;
	}

	/** Drawn before the food bar: exhaustion as a faint bar filling from the right. */
	public static void renderUnder(GuiGraphics g) {
		if (!on() || !instance.exhaustion.isOn()) return;
		Player player = foodBarPlayer();
		Minecraft mc = Minecraft.getInstance();
		if (player == null || mc.getSingleplayerServer() == null) return;
		// Exhaustion isn't sent to clients, so it's only known in singleplayer.
		var serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID());
		if (serverPlayer == null) return;
		float ex = Math.min(1f, ((com.orbitclient.mixin.FoodDataAccessor) serverPlayer.getFoodData()).orbit$exhaustion() / 4f);
		int w = Math.round(81 * ex);
		if (w > 0) Render2D.fill(g, right(g) - w, top(g), right(g), top(g) + 9, 0x50FFFFFF);
	}

	/** Drawn after the food bar: saturation outlines and the held-food preview. */
	public static void renderOver(GuiGraphics g) {
		if (!on()) return;
		Player player = foodBarPlayer();
		if (player == null) return;
		FoodData food = player.getFoodData();
		int level = food.getFoodLevel();
		float sat = food.getSaturationLevel();
		int right = right(g), y = top(g);
		float pulse = 0.35f + 0.45f * (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 160.0));

		FoodProperties held = heldFood(player);
		if (held != null && instance.foodPreview.isOn()) {
			int newLevel = Math.min(20, level + held.nutrition());
			for (int i = 0; i < 10; i++) {
				int points = Math.max(0, Math.min(2, newLevel - i * 2)), current = Math.max(0, Math.min(2, level - i * 2));
				if (points <= current) continue;
				g.blitSprite(RenderPipelines.GUI_TEXTURED, points == 2 ? FOOD_FULL : FOOD_HALF, right - i * 8 - 9, y, 9, 9,
					Render2D.withAlpha(0xFFFFFFFF, pulse));
			}
		}
		if (instance.saturationOverlay.isOn()) {
			instance.ensureOutline();
			int color = instance.saturationColor.getColor() | 0xFF000000;
			outlines(g, right, y, 0, sat, Render2D.withAlpha(color, 0.95f));
			if (held != null && instance.foodPreview.isOn()) {
				int newLevel = Math.min(20, level + held.nutrition());
				float newSat = Math.min(newLevel, sat + held.saturation());
				outlines(g, right, y, sat, newSat, Render2D.withAlpha(color, pulse));
			}
		}
	}

	/** Gold outlines for saturation points from..to, a quarter drumstick at a time, filling from the right. */
	private static void outlines(GuiGraphics g, int right, int y, float from, float to, int color) {
		if (!instance.outlineReady || to <= from) return;
		for (int i = 0; i < 10; i++) {
			float a = Math.max(0, Math.min(2, from - i * 2)) / 2f, b = Math.max(0, Math.min(2, to - i * 2)) / 2f;
			// Round to quarters so partial outlines stay on whole pixels.
			int px0 = Math.round(a * 4) * 9 / 4, px1 = Math.round(b * 4) * 9 / 4;
			if (px1 <= px0) continue;
			int x = right - i * 8 - 9;
			g.blit(RenderPipelines.GUI_TEXTURED, OUTLINE, x + 9 - px1, y, 9 - px1, 0, px1 - px0, 9, 9, 9, color);
		}
	}

	private static FoodProperties heldFood(Player player) {
		for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food != null) return food;
		}
		return null;
	}

	/** Builds the outline from the empty-drumstick sprite, so it matches resource packs too. */
	private void ensureOutline() {
		if (outlineReady) return;
		outlineReady = true;
		Minecraft mc = Minecraft.getInstance();
		var res = mc.getResourceManager().getResource(Identifier.withDefaultNamespace("textures/gui/sprites/hud/food_empty.png"));
		if (res.isEmpty()) {
			outlineReady = false;
			return;
		}
		try (InputStream in = res.get().open(); NativeImage src = NativeImage.read(in)) {
			int w = src.getWidth(), h = src.getHeight();
			NativeImage out = new NativeImage(w, h, true);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					boolean solid = alpha(src, x, y) > 0;
					boolean edge = solid && (alpha(src, x - 1, y) == 0 || alpha(src, x + 1, y) == 0 || alpha(src, x, y - 1) == 0 || alpha(src, x, y + 1) == 0);
					out.setPixel(x, y, edge ? 0xFFFFFFFF : 0);
				}
			}
			mc.getTextureManager().register(OUTLINE, new DynamicTexture(() -> "Orbit saturation outline", out));
		} catch (Exception e) {
			outlineReady = false;
		}
	}

	private static int alpha(NativeImage img, int x, int y) {
		if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return 0;
		return (img.getPixel(x, y) >>> 24) & 0xFF;
	}

	/** Adds "Hunger +4  Saturation +2.4" under the name of food items. */
	public static void tooltip(ItemStack stack, List<Component> lines) {
		if (!on() || !instance.tooltips.isOn()) return;
		FoodProperties food = stack.get(DataComponents.FOOD);
		if (food == null) return;
		Component line = Component.literal("Hunger +" + food.nutrition()).withStyle(ChatFormatting.GRAY)
			.append(Component.literal("  Saturation +" + String.format("%.1f", food.saturation())).withStyle(ChatFormatting.GOLD));
		lines.add(Math.min(1, lines.size()), line);
	}
}
