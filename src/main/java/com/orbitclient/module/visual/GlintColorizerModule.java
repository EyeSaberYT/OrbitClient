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
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.util.OptionalDouble;

/**
 * Recolors the enchantment glint. The glint textures are replaced with copies tinted to your color
 * (brightness kept from the original), and put back when the mod is turned off.
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
	private final AbstractTexture[] originals = new AbstractTexture[GLINTS.length];

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
			if (originals[i] != null) textures.register(GLINTS[i], originals[i]);
			originals[i] = null;
		}
		applied = 0;
	}

	private void apply(int argb) {
		Minecraft mc = Minecraft.getInstance();
		var textures = mc.getTextureManager();
		float r = ((argb >> 16) & 0xFF) / 255f, g = ((argb >> 8) & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
		for (int i = 0; i < GLINTS.length; i++) {
			var resource = mc.getResourceManager().getResource(GLINTS[i]);
			if (resource.isEmpty()) continue;
			try (InputStream in = resource.get().open()) {
				NativeImage image = NativeImage.read(in);
				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < image.getWidth(); x++) {
						int p = image.getPixel(x, y);
						int a = (p >>> 24) & 0xFF;
						// Brightness of the original glint pixel, re-tinted with the chosen color.
						float lum = Math.max((p >> 16) & 0xFF, Math.max((p >> 8) & 0xFF, p & 0xFF)) / 255f;
						image.setPixel(x, y, (a << 24) | (Math.round(lum * r * 255) << 16) | (Math.round(lum * g * 255) << 8) | Math.round(lum * b * 255));
					}
				}
				if (originals[i] == null) originals[i] = textures.getTexture(GLINTS[i]);
				GlintTexture tinted = new GlintTexture(image);
				tinted.upload();
				textures.register(GLINTS[i], tinted);
			} catch (Exception e) {
				// Leave the vanilla glint if the texture can't be read (e.g. a resource pack replaced it oddly).
			}
		}
	}
}
