package com.orbitclient.module.visual;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ColorSetting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.util.OptionalDouble;

/**
 * Recolors the enchantment glint. The glint textures are replaced with copies tinted to your color
 * (brightness kept from the original), and Minecraft loads its own again when the mod is turned off.
 * The originals are read once; a new color (every tick with chroma) only recolors those copies.
 */
public class GlintColorizerModule extends Module {
	private static final Identifier[] GLINTS = {
		Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png"),
		Identifier.withDefaultNamespace("textures/misc/enchanted_glint_armor.png"),
	};
	private static GlintColorizerModule instance;
	private final ColorSetting color;
	private final BooleanSetting allItems;
	private int applied;
	/** Our tinted copy of each glint, registered in place of Minecraft's. */
	private final GlintTexture[] tinted = new GlintTexture[GLINTS.length];
	/** Each original glint pixel's alpha (top byte) and brightness (low byte). */
	private final int[][] source = new int[GLINTS.length][];

	/** A tinted glint; it scrolls across items, so it must repeat and be smooth. */
	private static final class GlintTexture extends DynamicTexture {
		GlintTexture(NativeImage image) {
			super(() -> "Orbit glint", image);
			sampler = RenderSystem.getDevice().createSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
		}
	}

	public GlintColorizerModule() {
		super("Glint Colorizer", "Changes the color of the enchantment glint.", Category.MECHANIC);
		setIcon(Items.ENCHANTED_BOOK);
		color = add(new ColorSetting("Color", 0xFFFF4D6D));
		allItems = add(new BooleanSetting("All Items", false));
		instance = this;
	}

	/** All Items: every item glints, enchanted or not (only on your screen). */
	public static boolean glintAll(ItemStack stack) {
		return instance != null && instance.isEnabled() && instance.allItems.isOn() && !stack.isEmpty();
	}

	@Override
	public void onTick() {
		int c = color.getColor() | 0xFF000000;
		if (c != applied) {
			apply(c);
			applied = c;
		}
	}

	@Override
	protected void onDisable() {
		var textures = Minecraft.getInstance().getTextureManager();
		for (int i = 0; i < GLINTS.length; i++) {
			// Closes our copy; Minecraft loads its own glint again the next time it's drawn.
			if (tinted[i] != null) textures.release(GLINTS[i]);
			tinted[i] = null;
			source[i] = null;
		}
		applied = 0;
	}

	private void apply(int argb) {
		Minecraft mc = Minecraft.getInstance();
		int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
		for (int i = 0; i < GLINTS.length; i++) {
			if (tinted[i] == null && !load(mc, i)) continue;
			NativeImage image = tinted[i].getPixels();
			int[] pixels = source[i];
			int width = image.getWidth();
			for (int p = 0; p < pixels.length; p++) {
				int alpha = pixels[p] >>> 24, lum = pixels[p] & 0xFF;
				// Brightness of the original glint pixel, re-tinted with the chosen color.
				image.setPixel(p % width, p / width, (alpha << 24) | (lum * r / 255 << 16) | (lum * g / 255 << 8) | lum * b / 255);
			}
			tinted[i].upload();
		}
	}

	/** Reads one original glint and puts our copy in its place. False if it can't be read. */
	private boolean load(Minecraft mc, int i) {
		var resource = mc.getResourceManager().getResource(GLINTS[i]);
		if (resource.isEmpty()) return false;
		try (InputStream in = resource.get().open()) {
			NativeImage image = NativeImage.read(in);
			int[] pixels = new int[image.getWidth() * image.getHeight()];
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int p = image.getPixel(x, y);
					int lum = Math.max((p >> 16) & 0xFF, Math.max((p >> 8) & 0xFF, p & 0xFF));
					pixels[y * image.getWidth() + x] = (p & 0xFF000000) | lum;
				}
			}
			source[i] = pixels;
			tinted[i] = new GlintTexture(image);
			mc.getTextureManager().register(GLINTS[i], tinted[i]);
			return true;
		} catch (Exception e) {
			// Leave the vanilla glint if the texture can't be read (e.g. a resource pack replaced it oddly).
			return false;
		}
	}
}
