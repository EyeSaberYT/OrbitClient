package com.orbitclient.cosmetics;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Exports the cosmetic library from the mod's own definitions (Cosmetics, VoxelModel.icon and the cape
 * textures), so listings can't drift from the game:
 * <ul>
 * <li>--website DIR: DIR/data/cosmetics.json and DIR/img/cosmetics/&lt;id&gt;.png (256x256, transparent)</li>
 * <li>--sheets DIR: contact sheets per slot (new cosmetics marked) and sample frames of the capes</li>
 * </ul>
 * Run with {@code gradlew exportCosmetics -Pwebsite=<public dir> -Psheets=<dir>}.
 */
public final class ExportCosmetics {
	static final int SIZE = 256;
	/** Everything in the library before 1.0.14; the rest are marked new. */
	static final Set<String> BEFORE_1_0_14 = Set.of(
		"eclipse", "galaxy", "flame", "aurora", "ocean", "circuit", "sakura", "midnight",
		"crown", "halo", "cat_ears", "headphones", "top_hat",
		"bandana", "shades", "prism_visor", "rune_mask",
		"backpack", "sword_sheath", "jetpack", "quiver",
		"dragon_wings", "angel_wings", "mech_wings", "butterfly_wings", "void_wings",
		"orbit", "rune_ring", "sparkles", "trail", "prism_orbit", "phoenix_crown",
		"wisp", "cube_buddy", "moss_frog", "ember_drake");

	public static void main(String[] args) throws IOException {
		File website = null, sheets = null, capes = new File("src/main/resources/assets/orbitclient/textures/cosmetics/capes");
		for (int i = 0; i + 1 < args.length; i += 2) {
			switch (args[i]) {
				case "--website" -> website = new File(args[i + 1]);
				case "--sheets" -> sheets = new File(args[i + 1]);
				case "--capes" -> capes = new File(args[i + 1]);
				default -> throw new IllegalArgumentException("Unknown option " + args[i]);
			}
		}
		List<Cosmetic> all = Cosmetics.all();
		List<BufferedImage> pictures = new ArrayList<>();
		for (Cosmetic c : all) pictures.add(picture(c, capes));

		if (website != null) {
			File img = new File(website, "img/cosmetics"), data = new File(website, "data");
			img.mkdirs();
			data.mkdirs();
			JsonArray list = new JsonArray();
			for (int i = 0; i < all.size(); i++) {
				Cosmetic c = all.get(i);
				ImageIO.write(pictures.get(i), "png", new File(img, c.id + ".png"));
				JsonObject o = new JsonObject();
				o.addProperty("id", c.id);
				o.addProperty("name", c.name);
				o.addProperty("slot", c.slot.name());
				o.addProperty("slotTitle", c.slot.title);
				o.addProperty("colorable", c.colorable);
				o.addProperty("defaultColor", String.format(Locale.ROOT, "#%06x", c.defaultColor & 0xFFFFFF));
				o.addProperty("animated", c.slot == CosmeticSlot.CAPE ? c.capeFrames > 1 : c.animated);
				o.addProperty("image", "/img/cosmetics/" + c.id + ".png");
				if (!BEFORE_1_0_14.contains(c.id)) o.addProperty("isNew", true);
				list.add(o);
			}
			Files.writeString(new File(data, "cosmetics.json").toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(list) + "\n", StandardCharsets.UTF_8);
			System.out.println("Wrote " + all.size() + " cosmetics to " + website);
		}

		if (sheets != null) {
			sheets.mkdirs();
			for (CosmeticSlot slot : CosmeticSlot.values()) {
				List<Integer> idx = new ArrayList<>();
				for (int i = 0; i < all.size(); i++) if (all.get(i).slot == slot) idx.add(i);
				File out = new File(sheets, "sheet_" + slot.name().toLowerCase(Locale.ROOT) + ".png");
				ImageIO.write(sheet(idx.stream().map(all::get).toList(), idx.stream().map(pictures::get).toList()), "png", out);
			}
			for (Cosmetic c : all) {
				if (c.slot != CosmeticSlot.CAPE || BEFORE_1_0_14.contains(c.id)) continue;
				BufferedImage strip = new BufferedImage(4 * 170, 270, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = strip.createGraphics();
				g.setColor(new Color(0xFF1B1E27, true));
				g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
				for (int f = 0; f < 4; f++) g.drawImage(capeFront(capes, c.id, f * 4, 4), f * 170 + 5, 5, null);
				g.dispose();
				ImageIO.write(strip, "png", new File(sheets, "cape_" + c.id + "_frames.png"));
			}
			System.out.println("Wrote contact sheets to " + sheets);
		}
	}

	/** The cosmetic's picture: the menu icon in its default color, or the front of a cape's first frame. */
	static BufferedImage picture(Cosmetic c, File capes) throws IOException {
		BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		if (c.slot == CosmeticSlot.CAPE) {
			BufferedImage front = capeFront(capes, c.id, 0, 4);
			Graphics2D g = img.createGraphics();
			g.drawImage(front, (SIZE - front.getWidth()) / 2, (SIZE - front.getHeight()) / 2, null);
			g.dispose();
			return img;
		}
		int[] px = c.iconModel.icon(SIZE, c.defaultColor | 0xFF000000, c.iconPair);
		img.setRGB(0, 0, SIZE, SIZE, px, 0, SIZE);
		return img;
	}

	/** The outside of a cape (10x16 cape pixels at (1, 1) of the 64x32 layout), scaled up with crisp pixels. */
	static BufferedImage capeFront(File capes, String id, int frame, int scale) throws IOException {
		BufferedImage tex = ImageIO.read(new File(capes, id + "_" + frame + ".png"));
		int unit = tex.getWidth() / 64;
		BufferedImage out = new BufferedImage(40 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < out.getHeight(); y++) {
			for (int x = 0; x < out.getWidth(); x++) {
				int u = unit + x * 10 * unit / out.getWidth(), v = unit + y * 16 * unit / out.getHeight();
				out.setRGB(x, y, tex.getRGB(u, v));
			}
		}
		return out;
	}

	/** A labeled grid of pictures; new cosmetics get a NEW tag. */
	static BufferedImage sheet(List<Cosmetic> items, List<BufferedImage> pictures) {
		int tile = 180, cols = 6, rows = Math.max(1, (items.size() + cols - 1) / cols);
		BufferedImage img = new BufferedImage(cols * tile, rows * tile, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setColor(new Color(0xFF12141B, true));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		for (int i = 0; i < items.size(); i++) {
			Cosmetic c = items.get(i);
			int x = (i % cols) * tile, y = (i / cols) * tile;
			g.setColor(new Color(0xFF1E2230, true));
			g.fillRoundRect(x + 4, y + 4, tile - 8, tile - 8, 14, 14);
			g.drawImage(pictures.get(i), x + 20, y + 10, 140, 140, null);
			g.setColor(Color.WHITE);
			String label = c.name + " (" + c.id + ")";
			g.drawString(label, x + (tile - g.getFontMetrics().stringWidth(label)) / 2, y + tile - 14);
			if (!BEFORE_1_0_14.contains(c.id)) {
				g.setColor(new Color(0xFF3FD07A, true));
				g.drawString("NEW", x + 12, y + 20);
			}
		}
		g.dispose();
		return img;
	}
}
