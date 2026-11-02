import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Draws the module icons as bold white silhouettes. The menu tints them,
 * so they can be any shade and fade with animations.
 *
 * Every icon is rendered at several real pixel sizes (16 to 96) so the game can draw each one at
 * exactly the size it appears on screen, instead of shrinking one image (which looks blurry).
 *
 * Run from the project folder:
 *   java tools/IconGenerator.java src/main/resources/assets/orbitclient/textures/gui/icons
 *
 * Writes SIZE/NAME.png for each size. Names are the mod name in lower case with other characters
 * replaced by "_" ("Toggle Sneak/Sprint" -> toggle_sneak_sprint). Also writes _icon_preview.png.
 */
public class IconGenerator {
	static final int S = 32;
	/** Must match Icons.SIZES in the mod. */
	static final int[] SIZES = {16, 20, 24, 28, 32, 36, 40, 48, 56, 64, 72, 80, 96};
	static Graphics2D g;

	public static void main(String[] args) throws Exception {
		File out = new File(args.length > 0 ? args[0] : "icons");
		out.mkdirs();
		Map<String, Consumer<Graphics2D>> icons = icons();
		for (int size : SIZES) {
			File dir = new File(out, String.valueOf(size));
			dir.mkdirs();
			for (Map.Entry<String, Consumer<Graphics2D>> e : icons.entrySet()) {
				BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
				g = img.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g.scale(size / (double) S, size / (double) S);
				g.setColor(Color.WHITE);
				e.getValue().accept(g);
				g.dispose();
				if (!e.getKey().startsWith("ui_") && !e.getKey().startsWith("wp_")) {
					img = size <= 48 ? neonFill(img) : neonOutline(img, Math.max(2, Math.round(size / 32f * 2.2f)));
				}
				ImageIO.write(img, "png", new File(dir, e.getKey() + ".png"));
			}
		}
		writePreview(new File(out, "32"), icons.keySet().toArray(new String[0]));
		System.out.println("Wrote " + icons.size() + " icons at " + SIZES.length + " sizes to " + out);
	}

	/** Compact sizes use the full, bold silhouette so the icon reads at a glance. */
	static BufferedImage neonFill(BufferedImage source) {
		int w = source.getWidth(), h = source.getHeight();
		BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			int pixel = source.getRGB(x, y);
			if ((pixel >>> 24) == 0) continue;
			result.setRGB(x, y, neonColor(pixel, y, h));
		}
		return result;
	}

	/** Thick, bright Eclipse-neon contour treatment for module glyphs. */
	static BufferedImage neonOutline(BufferedImage source, int radius) {
		int w = source.getWidth(), h = source.getHeight();
		BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			int pixel = source.getRGB(x, y);
			if ((pixel >>> 24) == 0) continue;
			boolean edge = false;
			for (int dy = -radius; dy <= radius && !edge; dy++) for (int dx = -radius; dx <= radius; dx++) {
				if (dx * dx + dy * dy > radius * radius) continue;
				int nx = x + dx, ny = y + dy;
				if (nx < 0 || ny < 0 || nx >= w || ny >= h || (source.getRGB(nx, ny) >>> 24) == 0) {
					edge = true;
					break;
				}
			}
			if (!edge) continue;
			result.setRGB(x, y, neonColor(pixel, y, h));
		}
		return result;
	}

	private static int neonColor(int pixel, int y, int h) {
		float t = h <= 1 ? 0.5f : y / (float) (h - 1);
		int r = Math.round(170 + (83 - 170) * t);
		int green = Math.round(137 + (231 - 137) * t);
		return (pixel & 0xFF000000) | r << 16 | green << 8 | 0xFF;
	}

	// ---------------------------------------------------------------- drawing helpers

	static void stroke(float w) {
		g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	}

	static void line(double x1, double y1, double x2, double y2) {
		g.draw(new Line2D.Double(x1, y1, x2, y2));
	}

	static void poly(boolean fill, double... pts) {
		Path2D p = new Path2D.Double();
		p.moveTo(pts[0], pts[1]);
		for (int i = 2; i < pts.length; i += 2) p.lineTo(pts[i], pts[i + 1]);
		if (fill) {
			p.closePath();
			g.fill(p);
		} else {
			g.draw(p);
		}
	}

	static void ring(double cx, double cy, double r) {
		g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
	}

	static void disc(double cx, double cy, double r) {
		g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
	}

	static void rect(double x, double y, double w, double h, double r) {
		g.fill(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2));
	}

	static void rectOutline(double x, double y, double w, double h, double r) {
		g.draw(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2));
	}

	/** Following draws erase instead of paint, for holes and cut-outs. */
	static void erase(Runnable r) {
		Composite old = g.getComposite();
		g.setComposite(AlphaComposite.Clear);
		r.run();
		g.setComposite(old);
	}

	static void star(double cx, double cy, double r) {
		double i = r * 0.28;
		poly(true, cx, cy - r, cx + i, cy - i, cx + r, cy, cx + i, cy + i, cx, cy + r, cx - i, cy + i, cx - r, cy, cx - i, cy - i);
	}

	static void potion() {
		rect(12, 2, 8, 3, 1);
		rect(13.5, 4, 5, 7, 0.5);
		disc(16, 20, 10);
		erase(() -> disc(12, 17, 2.2));
	}

	// ---------------------------------------------------------------- icons

	static Map<String, Consumer<Graphics2D>> icons() {
		Map<String, Consumer<Graphics2D>> m = new LinkedHashMap<>();

		m.put("armor_status", gr -> {
			poly(true, 4, 7, 11, 3, 16, 8, 21, 3, 28, 7, 28, 15, 23, 15, 23, 29, 9, 29, 9, 15, 4, 15);
			// Separate the shoulder plates and add a belt line so it reads as armor, not a shirt.
			erase(() -> {
				stroke(1.6f);
				line(9.5, 6, 9.5, 15);
				line(22.5, 6, 22.5, 15);
				line(9, 23, 23, 23);
			});
		});
		m.put("clock", gr -> {
			stroke(3);
			ring(16, 16, 12.5);
			line(16, 16, 16, 9);
			line(16, 16, 21.5, 19);
		});
		m.put("combo_counter", gr -> {
			stroke(3.5f);
			line(5, 21, 13, 5);
			line(11, 26, 19, 10);
			line(17, 29, 25, 13);
		});
		m.put("cooldowns", gr -> {
			stroke(2.8f);
			line(6, 4, 26, 4);
			line(6, 28, 26, 28);
			poly(false, 9, 4, 9, 9, 16, 16, 23, 9, 23, 4);
			poly(false, 9, 28, 9, 23, 16, 16, 23, 23, 23, 28);
			poly(true, 16, 20, 21.5, 26, 10.5, 26);
		});
		m.put("coordinates", gr -> {
			stroke(2.8f);
			line(12, 20, 12, 6);
			line(12, 20, 26, 20);
			line(12, 20, 4, 28);
			poly(true, 12, 2, 16, 8, 8, 8);
			poly(true, 30, 20, 24, 16, 24, 24);
		});
		m.put("cps", gr -> {
			stroke(2.8f);
			rectOutline(10, 7, 14, 22, 7);
			line(17, 7, 17, 15);
			line(10, 15, 24, 15);
			line(4, 4, 7, 7);
			line(3, 10, 6.5, 10.5);
			line(9, 1.5, 9.5, 4.5);
		});
		m.put("day_counter", gr -> {
			disc(16, 16, 6.5);
			stroke(2.8f);
			for (int i = 0; i < 8; i++) {
				double a = Math.PI / 4 * i;
				line(16 + Math.cos(a) * 10, 16 + Math.sin(a) * 10, 16 + Math.cos(a) * 14, 16 + Math.sin(a) * 14);
			}
		});
		m.put("direction_hud", gr -> poly(true, 16, 3, 27, 29, 16, 22, 5, 29));
		m.put("fps", gr -> {
			stroke(3.2f);
			g.draw(new Arc2D.Double(4, 7, 24, 24, -20, 220, Arc2D.OPEN));
			line(16, 19, 23, 11);
			disc(16, 19, 3);
		});
		m.put("item_counter", gr -> {
			rect(3, 15, 12, 12, 2);
			rect(17, 15, 12, 12, 2);
			rect(10, 2, 12, 12, 2);
		});
		m.put("item_tracker", gr -> {
			stroke(3);
			poly(false, 4, 18, 4, 28, 28, 28, 28, 18);
			line(16, 3, 16, 20);
			poly(false, 10, 14, 16, 20, 22, 14);
		});
		m.put("key_strokes", gr -> {
			rect(12, 3, 8, 8, 1.5);
			rect(3, 13, 8, 8, 1.5);
			rect(12, 13, 8, 8, 1.5);
			rect(21, 13, 8, 8, 1.5);
			rect(3, 23, 26, 6, 1.5);
		});
		m.put("memory_usage", gr -> {
			stroke(2.5f);
			rectOutline(7, 7, 18, 18, 2);
			rect(12, 12, 8, 8, 1);
			for (int i : new int[]{11, 16, 21}) {
				line(i, 2.5, i, 6);
				line(i, 26, i, 29.5);
				line(2.5, i, 6, i);
				line(26, i, 29.5, i);
			}
		});
		m.put("pack_display", gr -> {
			poly(true, 16, 3, 29, 10, 16, 17, 3, 10);
			stroke(2.8f);
			poly(false, 3, 16, 16, 23, 29, 16);
			poly(false, 3, 22, 16, 29, 29, 22);
		});
		m.put("playtime", gr -> {
			stroke(3);
			ring(16, 16, 12.5);
			poly(true, 13, 10, 23, 16, 13, 22);
		});
		m.put("potion_counter", gr -> {
			potion();
			erase(() -> {
				stroke(2.6f);
				line(16, 16, 16, 24);
				line(12, 20, 20, 20);
			});
		});
		m.put("potion_effects", gr -> potion());
		m.put("reach_display", gr -> {
			stroke(3);
			line(5, 16, 27, 16);
			poly(false, 10, 10, 4, 16, 10, 22);
			poly(false, 22, 10, 28, 16, 22, 22);
		});
		m.put("saturation", gr -> {
			AffineTransform t = g.getTransform();
			g.rotate(Math.toRadians(-45), 19, 12);
			g.fill(new Ellipse2D.Double(10, 6, 18, 13));
			g.setTransform(t);
			stroke(3.5f);
			line(12, 20, 7, 25);
			disc(4.5, 25, 2.6);
			disc(7, 28, 2.6);
		});
		m.put("speedometer", gr -> poly(true, 19, 2, 7, 18, 15, 18, 12, 30, 25, 13, 17, 13));
		m.put("stopwatch", gr -> {
			stroke(3);
			ring(16, 18, 11);
			rect(12.5, 2, 7, 3.5, 1);
			line(16, 6, 16, 7);
			line(16, 18, 16, 11);
			line(24, 8, 26.5, 5.5);
		});
		m.put("waila", gr -> poly(true, 8, 3, 8, 26, 13, 21.5, 17, 29.5, 21, 27.5, 17, 19.5, 24, 19.5));
		m.put("auto_text_hotkey", gr -> {
			rect(3, 5, 26, 18, 5);
			poly(true, 8, 21, 8, 29, 16, 21);
			erase(() -> {
				stroke(2.2f);
				line(9, 11, 23, 11);
				line(9, 16, 18, 16);
			});
		});
		m.put("boss_bar", gr -> {
			disc(16, 13, 11);
			rect(10, 17, 12, 10, 2);
			erase(() -> {
				disc(11.5, 13, 3.2);
				disc(20.5, 13, 3.2);
				stroke(1.8f);
				line(14, 22, 14, 27);
				line(18, 22, 18, 27);
			});
		});
		m.put("ping", gr -> {
			rect(3, 22, 5, 7, 1);
			rect(10, 16, 5, 13, 1);
			rect(17, 10, 5, 19, 1);
			rect(24, 4, 5, 25, 1);
		});
		m.put("scoreboard", gr -> {
			stroke(2.6f);
			rectOutline(4, 4, 24, 24, 3);
			stroke(2.2f);
			line(9, 11, 18, 11);
			line(9, 16, 18, 16);
			line(9, 21, 18, 21);
			disc(23, 11, 1.6);
			disc(23, 16, 1.6);
			disc(23, 21, 1.6);
		});
		m.put("server_address", gr -> {
			stroke(2.5f);
			ring(16, 16, 12.5);
			g.draw(new Ellipse2D.Double(11, 3.5, 10, 25));
			line(4.5, 12, 27.5, 12);
			line(4.5, 20, 27.5, 20);
		});
		m.put("tab_editor", gr -> {
			stroke(3);
			for (int y : new int[]{7, 16, 25}) {
				disc(6, y, 2.8);
				line(12, y, 28, y);
			}
		});
		m.put("titles", gr -> {
			rect(5, 4, 22, 5.5, 1);
			rect(13, 4, 6, 25, 1);
		});
		m.put("waypoints", gr -> {
			disc(16, 12, 9.5);
			poly(true, 7.5, 15, 24.5, 15, 16, 30);
			erase(() -> disc(16, 12, 3.8));
		});
		m.put("chunk_borders", gr -> {
			stroke(2.6f);
			rectOutline(4, 4, 24, 24, 2);
			stroke(2);
			line(12, 4, 12, 28);
			line(20, 4, 20, 28);
			line(4, 12, 28, 12);
			line(4, 20, 28, 20);
		});
		m.put("crosshair", gr -> {
			stroke(2.6f);
			ring(16, 16, 9.5);
			line(16, 2, 16, 10);
			line(16, 22, 16, 30);
			line(2, 16, 10, 16);
			line(22, 16, 30, 16);
			disc(16, 16, 1.8);
		});
		m.put("fov_changer", gr -> {
			stroke(3);
			line(5, 26, 28, 7);
			line(5, 26, 28, 26);
			stroke(2.5f);
			g.draw(new Arc2D.Double(-8, 13, 26, 26, 0, 40, Arc2D.OPEN));
		});
		m.put("freelook", gr -> {
			Path2D eye = new Path2D.Double();
			eye.moveTo(2, 16);
			eye.quadTo(16, 1, 30, 16);
			eye.quadTo(16, 31, 2, 16);
			g.fill(eye);
			erase(() -> disc(16, 16, 6.5));
			disc(16, 16, 3.8);
		});
		m.put("motion_blur", gr -> {
			disc(20, 16, 8);
			stroke(2.4f);
			line(3, 10, 10, 10);
			line(1, 16, 9, 16);
			line(3, 22, 10, 22);
		});
		m.put("block_outline", gr -> {
			stroke(2.4f);
			poly(false, 16, 3, 28, 9, 28, 23, 16, 29, 4, 23, 4, 9);
			line(4, 9, 16, 15);
			line(28, 9, 16, 15);
			line(16, 15, 16, 29);
		});
		m.put("hit_color", gr -> {
			poly(true, 16, 2, 19.5, 11, 29, 9, 22, 16.5, 29, 24, 19.5, 21.5, 16, 30, 12.5, 21.5, 3, 24, 10, 16.5, 3, 9, 12.5, 11);
			erase(() -> disc(16, 16, 4));
		});
		m.put("nametags", gr -> {
			rect(3, 4, 26, 10, 3);
			erase(() -> rect(7, 8, 18, 2.2, 1));
			disc(16, 20, 3.6);
			poly(true, 9, 30, 10.5, 25.5, 21.5, 25.5, 23, 30);
		});
		m.put("shiny_pots", gr -> {
			potion();
			star(26, 6, 4);
		});
		m.put("low_fire", gr -> {
			poly(true, 16, 10, 22, 17, 24, 22, 21, 28, 11, 28, 8, 22, 10, 17, 13, 19);
			stroke(2.2f);
			line(4, 5, 28, 5);
			erase(() -> disc(16, 24, 3));
		});
		m.put("item_physics", gr -> {
			poly(true, 5, 20, 19, 12, 27, 17, 13, 25);
			rect(3, 27, 26, 2.4, 1);
			stroke(2f);
			line(10, 6, 10, 12);
			line(16, 3, 16, 9);
		});
		m.put("menu_blur", gr -> {
			rectOutline(4, 6, 24, 20, 3);
			for (int i = 0; i < 3; i++) disc(11 + i * 5, 16, 2.4 - i * 0.5);
		});
		m.put("glint_colorizer", gr -> {
			stroke(3f);
			line(6, 26, 20, 12);
			star(23, 9, 6);
			disc(8, 8, 1.8);
			disc(26, 24, 1.8);
		});
		m.put("team_view", gr -> {
			disc(11, 10, 4);
			disc(22, 10, 4);
			poly(true, 3, 27, 5, 18, 17, 18, 19, 27);
			poly(true, 14, 27, 16, 18, 28, 18, 30, 27);
		});
		m.put("particle_changer", gr -> {
			star(16, 16, 9);
			disc(5, 6, 1.8);
			disc(27, 7, 1.8);
			disc(6, 27, 1.8);
			disc(27, 26, 1.8);
		});
		m.put("chat", gr -> {
			poly(true, 4, 6, 28, 6, 28, 22, 14, 22, 8, 28, 8, 22, 4, 22);
			erase(() -> {
				rect(8, 11, 16, 2.2, 1);
				rect(8, 16, 10, 2.2, 1);
			});
		});
		m.put("nick_hider", gr -> {
			disc(16, 11, 6);
			poly(true, 5, 29, 8, 20, 24, 20, 27, 29);
			erase(() -> rect(7, 9, 18, 4, 2));
		});
		m.put("screenshot", gr -> {
			rect(3, 9, 26, 18, 3);
			rect(11, 5, 10, 5, 1.5);
			erase(() -> disc(16, 18, 6));
			disc(16, 18, 3.5);
		});
		m.put("sound_changer", gr -> {
			poly(true, 4, 12, 10, 12, 17, 6, 17, 26, 10, 20, 4, 20);
			stroke(2.2f);
			line(22, 11, 22, 21);
			line(27, 7, 27, 25);
		});
		m.put("wavey_capes", gr -> {
			poly(true, 9, 3, 23, 3, 24, 9, 27, 15, 24, 21, 27, 28, 18, 26, 13, 29, 5, 27, 8, 21, 5, 15, 8, 9);
			erase(() -> rect(12, 3, 8, 3, 1));
		});
		m.put("ui_wardrobe", gr -> {
			stroke(2.4f);
			line(16, 9, 16, 6);
			poly(false, 16, 6, 18.5, 4.5, 16, 3, 13.5, 4.5);
			poly(false, 16, 10, 29, 22, 3, 22);
			line(3, 22, 3, 25);
			line(29, 22, 29, 25);
			line(3, 25, 29, 25);
		});
		m.put("fullbright", gr -> {
			disc(16, 12, 9.5);
			rect(11.5, 19, 9, 5, 1);
			stroke(2.2f);
			line(12, 26, 20, 26);
			line(13.5, 29, 18.5, 29);
		});
		m.put("hitbox", gr -> {
			stroke(2.2f);
			g.draw(new Rectangle2D.Double(4, 12, 15, 15));
			g.draw(new Rectangle2D.Double(12, 5, 15, 15));
			line(4, 12, 12, 5);
			line(19, 12, 27, 5);
			line(4, 27, 12, 20);
			line(19, 27, 27, 20);
		});
		m.put("hurt_cam", gr -> {
			Path2D heart = new Path2D.Double();
			heart.moveTo(16, 28);
			heart.curveTo(2, 19, 2, 5, 10, 5);
			heart.curveTo(13, 5, 15, 7, 16, 9);
			heart.curveTo(17, 7, 19, 5, 22, 5);
			heart.curveTo(30, 5, 30, 19, 16, 28);
			g.fill(heart);
			erase(() -> {
				stroke(2);
				poly(false, 16, 9, 13, 15, 18, 19, 15, 25);
			});
		});
		m.put("particle_multiplier", gr -> {
			star(13, 16, 11);
			star(25, 7, 5);
			star(25, 25, 4);
		});
		m.put("snaplook", gr -> {
			stroke(3);
			g.draw(new Arc2D.Double(5, 5, 22, 22, 50, 280, Arc2D.OPEN));
			poly(true, 23.5, 3, 25, 12, 16.5, 9);
		});
		m.put("time_changer", gr -> {
			disc(15, 17, 12);
			erase(() -> disc(21, 11, 10));
			star(25, 23, 4);
		});
		m.put("toggle_sneak_sprint", gr -> {
			disc(20, 5, 3.3);
			stroke(3.2f);
			line(18, 10, 14, 19);
			poly(false, 10, 13, 17, 11, 23, 15);
			poly(false, 14, 19, 20, 23, 18, 29);
			poly(false, 14, 19, 9, 24, 4, 24);
		});
		m.put("weather_changer", gr -> {
			disc(11, 14, 6);
			disc(18.5, 10.5, 8);
			disc(24.5, 15, 5);
			rect(6, 14, 22, 6, 3);
			stroke(2.5f);
			line(10, 24, 8, 29);
			line(16, 24, 14, 29);
			line(22, 24, 20, 29);
		});
		m.put("zoom", gr -> {
			stroke(3);
			ring(13, 13, 9.5);
			stroke(4.2f);
			line(20.5, 20.5, 28, 28);
			stroke(2.5f);
			line(9, 13, 17, 13);
			line(13, 9, 13, 17);
		});
		// ---- menu icons (not mods): dock buttons and panel controls
		m.put("ui_mods", gr -> {
			// A planet with an orbit and a moon: the Orbit menu itself.
			disc(16, 16, 6);
			stroke(2.2f);
			ring(16, 16, 12.5);
			double mx = 16 + Math.cos(-Math.PI / 4) * 12.5, my = 16 + Math.sin(-Math.PI / 4) * 12.5;
			erase(() -> disc(mx, my, 5));
			disc(mx, my, 3.2);
		});
		m.put("ui_profiles", gr -> {
			disc(12, 10, 5);
			Path2D body = new Path2D.Double();
			body.moveTo(3, 27);
			body.curveTo(3, 18, 21, 18, 21, 27);
			body.closePath();
			g.fill(body);
			disc(23, 12, 4);
			Path2D body2 = new Path2D.Double();
			body2.moveTo(19, 21);
			body2.curveTo(22, 18, 30, 19, 30, 26);
			body2.lineTo(23, 26);
			body2.closePath();
			g.fill(body2);
		});
		m.put("ui_settings", gr -> {
			for (int i = 0; i < 8; i++) {
				AffineTransform t = g.getTransform();
				g.rotate(Math.PI / 4 * i, 16, 16);
				rect(13.5, 2.5, 5, 7, 1.2);
				g.setTransform(t);
			}
			disc(16, 16, 10);
			erase(() -> disc(16, 16, 4.2));
		});
		m.put("ui_hud", gr -> {
			stroke(2.6f);
			rectOutline(3, 5, 26, 22, 3);
			rect(7, 9, 7, 5, 1);
			rect(19, 18, 6, 5, 1);
			stroke(2);
			line(7, 21, 14, 21);
		});
		m.put("ui_close", gr -> {
			stroke(3.5f);
			line(8, 8, 24, 24);
			line(24, 8, 8, 24);
		});
		m.put("ui_star", gr -> {
			Path2D star = new Path2D.Double();
			for (int i = 0; i < 10; i++) {
				double r = i % 2 == 0 ? 13 : 5.5, a = -Math.PI / 2 + Math.PI / 5 * i;
				if (i == 0) star.moveTo(16 + Math.cos(a) * r, 17 + Math.sin(a) * r);
				else star.lineTo(16 + Math.cos(a) * r, 17 + Math.sin(a) * r);
			}
			star.closePath();
			g.fill(star);
		});
		m.put("ui_star_outline", gr -> {
			Path2D star = new Path2D.Double();
			for (int i = 0; i < 10; i++) {
				double r = i % 2 == 0 ? 12.5 : 5.5, a = -Math.PI / 2 + Math.PI / 5 * i;
				if (i == 0) star.moveTo(16 + Math.cos(a) * r, 17 + Math.sin(a) * r);
				else star.lineTo(16 + Math.cos(a) * r, 17 + Math.sin(a) * r);
			}
			star.closePath();
			stroke(2.4f);
			g.draw(star);
		});
		m.put("ui_power", gr -> {
			stroke(3.2f);
			g.draw(new Arc2D.Double(5, 6, 22, 22, 120, 300, Arc2D.OPEN));
			line(16, 3, 16, 15);
		});
		m.put("ui_reset", gr -> {
			stroke(3);
			g.draw(new Arc2D.Double(5, 5, 22, 22, 90, 270, Arc2D.OPEN));
			poly(true, 16, 0.5, 16, 10, 9.5, 5);
		});
		m.put("ui_edit", gr -> {
			poly(true, 22, 4, 28, 10, 12, 26, 5, 27, 6, 20);
			erase(() -> {
				stroke(1.8f);
				line(18, 8, 24, 14);
			});
		});
		m.put("ui_share", gr -> {
			stroke(2.8f);
			poly(false, 13, 6, 6, 6, 6, 26, 26, 26, 26, 19);
			line(16, 16, 27, 5);
			poly(false, 19, 5, 27, 5, 27, 13);
		});
		m.put("ui_import", gr -> {
			stroke(2.8f);
			poly(false, 4, 18, 4, 28, 28, 28, 28, 18);
			line(16, 3, 16, 20);
			poly(false, 10, 14, 16, 20, 22, 14);
		});
		m.put("ui_trash", gr -> {
			rect(6, 7, 20, 3.5, 1);
			rect(12, 3, 8, 4, 1);
			poly(true, 8, 12, 24, 12, 22.5, 29, 9.5, 29);
			erase(() -> {
				stroke(1.8f);
				line(13, 15, 13, 26);
				line(19, 15, 19, 26);
			});
		});
		// ---- waypoint marker icons
		m.put("wp_home", gr -> {
			poly(true, 16, 3, 30, 15, 26, 15, 26, 29, 6, 29, 6, 15, 2, 15);
			erase(() -> rect(13, 19, 6, 10, 1));
		});
		m.put("wp_flag", gr -> {
			rect(6, 3, 3, 27, 1);
			poly(true, 9, 4, 27, 4, 22, 10.5, 27, 17, 9, 17);
		});
		m.put("wp_diamond", gr -> {
			poly(true, 9, 5, 23, 5, 29, 12, 16, 29, 3, 12);
			erase(() -> {
				stroke(1.6f);
				line(3, 12, 29, 12);
				poly(false, 9, 5, 12, 12, 16, 29, 20, 12, 23, 5);
			});
		});
		m.put("wp_sword", gr -> {
			poly(true, 27, 3, 28, 4, 13, 21, 10.5, 18.5);
			stroke(3);
			line(7, 17, 15, 25);
			stroke(3.4f);
			line(10, 22, 4, 28);
		});
		m.put("wp_arrow", gr -> poly(true, 29, 16, 6, 4, 11, 16, 6, 28));
		m.put("ui_search", gr -> {
			stroke(3);
			ring(13, 13, 8.5);
			stroke(4);
			line(19.5, 19.5, 27, 27);
		});
		return m;
	}

	/** A dark sheet with every icon at 1x and 2x, for checking the designs. */
	static void writePreview(File out, String[] names) throws Exception {
		int cols = 8, cell = 120, rows = (names.length + cols - 1) / cols;
		BufferedImage sheet = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
		Graphics2D s = sheet.createGraphics();
		s.setColor(new Color(0x1D1D1D));
		s.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
		s.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		for (int i = 0; i < names.length; i++) {
			BufferedImage icon = ImageIO.read(new File(out, names[i] + ".png"));
			BufferedImage small = ImageIO.read(new File(out.getParentFile(), "16/" + names[i] + ".png"));
			int x = (i % cols) * cell, y = (i / cols) * cell;
			s.drawImage(icon, x + 8, y + 8, 64, 64, null);
			s.drawImage(small, x + 80, y + 24, 16, 16, null);
			s.setColor(new Color(0xC8C8C8));
			s.drawString(names[i], x + 6, y + 100);
		}
		s.dispose();
		ImageIO.write(sheet, "png", new File(out.getParentFile(), "_icon_preview.png"));
	}
}
