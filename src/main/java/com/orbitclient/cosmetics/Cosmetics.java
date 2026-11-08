package com.orbitclient.cosmetics;

import com.orbitclient.cosmetics.VoxelModel.Paint;
import com.mojang.math.Axis;
import com.orbitclient.util.PoseUtil;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every cosmetic, with its art. Models are voxel pixel art in model pixels (1/16 block). On the head,
 * the origin is the neck pivot and the head spans x/z -4..4, y -8..0 (front is -z). On the body, the
 * origin is the neck and the body spans x -4..4, y 0..12, z -2..2 (back is +z).
 */
public final class Cosmetics {
	private static final Map<String, Cosmetic> BY_ID = new LinkedHashMap<>();

	private Cosmetics() {
	}

	public static List<Cosmetic> all() {
		return List.copyOf(BY_ID.values());
	}

	public static List<Cosmetic> in(CosmeticSlot slot) {
		List<Cosmetic> list = new ArrayList<>();
		for (Cosmetic c : BY_ID.values()) if (c.slot == slot) list.add(c);
		return Collections.unmodifiableList(list);
	}

	public static Cosmetic get(String id) {
		return id == null ? null : BY_ID.get(id);
	}

	private static void add(String id, String name, CosmeticSlot slot, int color, boolean colorable, Cosmetic.Renderer r) {
		BY_ID.put(id, new Cosmetic(id, name, slot, color, colorable, r, 1));
	}

	/** Sets the model shown as a cosmetic's picture in menus. */
	private static void icon(String id, VoxelModel model, boolean pair) {
		Cosmetic c = BY_ID.get(id);
		c.iconModel = model;
		c.iconPair = pair;
	}

	/** A hat's height above the head in model pixels, for the nametag. */
	private static void lift(String id, float pixels) {
		BY_ID.get(id).nameLift = pixels / 16f;
	}

	private static void cape(String id, String name, int frames) {
		BY_ID.put(id, new Cosmetic(id, name, CosmeticSlot.CAPE, 0xFFFFFFFF, false, null, frames));
	}

	private static Map<Character, Paint> palette(Object... pairs) {
		Map<Character, Paint> m = new HashMap<>();
		for (int i = 0; i < pairs.length; i += 2) m.put((Character) pairs[i], (Paint) pairs[i + 1]);
		return m;
	}

	/** Shared helpers for shapes: is (x, y) within the ring between radii r0 and r1 around (c, c)? */
	private static boolean ring(double x, double y, double c, double r0, double r1) {
		double d = Math.hypot(x - c, y - c);
		return d >= r0 && d <= r1;
	}

	static {
		// ------------------------------------------------------------ capes (animated textures)
		cape("eclipse", "Eclipse", 16);
		cape("galaxy", "Galaxy", 16);
		cape("flame", "Inferno", 16);
		cape("aurora", "Aurora", 16);
		cape("ocean", "Tide", 16);
		cape("circuit", "Neon Circuit", 16);
		cape("sakura", "Sakura", 16);
		cape("midnight", "Midnight", 16);

		// ------------------------------------------------------------ hats
		VoxelModel crown = VoxelModel.build(1.05f, -4.2f, -13.2f, -4.2f, palette(
				'W', Paint.glow(0xFFFFF6D8), 'G', Paint.of(0xFFF2C14E), 'Y', Paint.of(0xFFC48A1C),
				'r', Paint.glow(0xFFE5484D), 'b', Paint.glow(0xFF4FA8FF), 'C', Paint.glow(0xFF7CF7FF),
				'c', Paint.of(0xFF9D5BFF), 'D', Paint.of(0xFF8A5A18)),
			"...W..W.../..W....W../.W......W./........../........../.W......W./..W....W../...W..W...",
			"...G..G.../..G....G../.G......G./........../........../.G......G./..G....G../...G..G...",
			"GGGGGGGGGG/G........G/G........G/G........G/G........G/G........G/G........G/G........G/G........G/GGGGGGGGGG",
			"YYYYYYYYYY/Y........Y/Y...C....Y/Y..G..G..Y/Y...c....Y/Y........Y/Y...G....Y/Y........Y/Y........Y/YYYYYYYYYY",
			"DDDDDDDDDD/D........D/D..r..b..D/D........D/D....W...D/D........D/D..b..r..D/D........D/D........D/DDDDDDDDDD");
		add("crown", "Royal Crown", CosmeticSlot.HAT, 0xFFF2C14E, false, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(crown);
			ctx.pose().popPose();
		});

		VoxelModel halo = VoxelModel.shape(1f, -6.5f, 0, -6.5f, palette('H', Paint.tintGlow(1f), 'h', Paint.tintGlow(0.75f)),
			13, 1, 13, (x, y, z) -> ring(x, z, 6, 4.6, 6.2) ? ((x + z) % 3 == 0 ? 'h' : 'H') : '.');
		add("halo", "Halo", CosmeticSlot.HAT, 0xFFFFE89A, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.pose().translate(0, (-12.5f + (float) Math.sin(ctx.time() * 2.2) * 0.6f) / 16f, 0);
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 25 % 360)));
			ctx.draw(halo);
			ctx.pose().popPose();
		});

		VoxelModel catEars = VoxelModel.build(1f, -4f, -11f, -0.5f, palette('#', Paint.tint(1f), '%', Paint.tint(0.75f), 'p', Paint.of(0xFFF7A1C4)),
			".#....#.", "#%#..#%#", "#p#..#p#");
		add("cat_ears", "Cat Ears", CosmeticSlot.HAT, 0xFF33313B, true, ctx -> {
			ctx.attach(ctx.model().head);
			// A small twitch every few seconds.
			float twitch = (float) Math.max(0, Math.sin(ctx.time() * 1.3) - 0.9) * 60f;
			PoseUtil.mulPose(ctx.pose(), Axis.XP.rotationDegrees(-twitch * 0.3f));
			ctx.draw(catEars);
			ctx.pose().popPose();
		});

		VoxelModel headphones = VoxelModel.build(1f, -6f, -9f, -1.5f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.7f), 'k', Paint.of(0xFF1C1D22), 'L', Paint.glow(0xFF3FE0FF)),
			"............/..%%%%%%%%../............",
			"............/.#........#./............",
			"............/.#........#./............",
			"............/.#........#./............",
			"#k........k#/#k........k#/#k........k#",
			"#k........k#/Lk........kL/#k........k#",
			"#k........k#/Lk........kL/#k........k#",
			"%k........k%/%k........k%/%k........k%");
		add("headphones", "Headphones", CosmeticSlot.HAT, 0xFFE5484D, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(headphones);
			ctx.pose().popPose();
		});

		VoxelModel topHat = VoxelModel.shape(1f, -5f, -15.5f, -5f, palette('k', Paint.of(0xFF18181C), 'g', Paint.of(0xFF2A2A30), '#', Paint.tint(1f)),
			10, 7, 10, (x, y, z) -> {
				if (y == 6) return 'k';
				boolean inner = x >= 2 && x <= 7 && z >= 2 && z <= 7;
				if (!inner) return '.';
				if (y == 5) return '#';
				return (x == 2 || z == 2) ? 'g' : 'k';
			});
		add("top_hat", "Top Hat", CosmeticSlot.HAT, 0xFFB5303A, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(topHat);
			ctx.pose().popPose();
		});

		// ------------------------------------------------------------ face
		VoxelModel bandana = VoxelModel.shape(1f, -5f, -7f, -5f, palette('#', Paint.tint(1f), '%', Paint.tint(0.72f), 'w', Paint.of(0xFFF4F4F4)),
			10, 6, 10, (x, y, z) -> {
				boolean edge = x == 0 || x == 9 || z == 0 || z == 9;
				if (y <= 1) return edge ? ((x * 3 + z + y) % 5 == 0 ? 'w' : (y == 1 ? '%' : '#')) : '.';
				// Knot tails hanging at the back.
				if (z == 9 && (x == 4 || x == 5) && y <= 4) return y % 2 == 0 ? '#' : '%';
				if (z == 9 && x == 3 && y == 2) return '#';
				if (z == 9 && x == 6 && y == 3) return '%';
				return '.';
			});
		add("bandana", "Bandana", CosmeticSlot.FACE, 0xFFD6453D, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(bandana);
			ctx.pose().popPose();
		});

		VoxelModel shades = VoxelModel.build(1f, -5f, -5f, -5f, palette('k', Paint.of(0xFF141418), 'l', Paint.tint(0.55f), 'g', Paint.glow(0xFFEAF6FF)),
			".kkkkkkkk./k........k/k........k/k........k/k........k/k........k",
			".kglk.kglk/..........",
			"..kk...kk.");
		add("shades", "Shades", CosmeticSlot.FACE, 0xFF3A4C78, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(shades);
			ctx.pose().popPose();
		});

		VoxelModel visor = VoxelModel.shape(1f, -6f, -5.5f, -5.2f, palette(
				'k', Paint.of(0xFF151923), 'm', Paint.tint(0.55f), 'L', Paint.tintGlow(1f), 'w', Paint.glow(0xFFEAFBFF)),
			12, 5, 2, (x, y, z) -> {
				if (y == 0 && x >= 2 && x <= 9) return 'k';
				if (y == 1 && (x == 0 || x == 1 || x == 10 || x == 11)) return 'm';
				if (y >= 1 && y <= 3 && x >= 2 && x <= 9) {
					if (x == 5 || x == 6) return 'k';
					return (y == 1 && (x == 2 || x == 9)) ? 'w' : (y == 3 ? 'm' : 'L');
				}
				if (z == 1 && y == 2 && (x == 0 || x == 11)) return 'k';
				return '.';
			});
		add("prism_visor", "Prism Visor", CosmeticSlot.FACE, 0xFF32D6E8, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(visor);
			ctx.pose().popPose();
		});

		VoxelModel runeMask = VoxelModel.shape(1f, -5f, -5f, -5.2f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF171523), 'r', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFE7B0)),
			10, 8, 2, (x, y, z) -> {
				if (y < 1 || y > 6 || x < 1 || x > 8) return '.';
				if ((x == 1 || x == 8) && y < 3) return '.';
				if (y == 2 && (x == 2 || x == 3 || x == 6 || x == 7)) return 'k';
				if (y == 3 && (x == 2 || x == 3 || x == 6 || x == 7)) return 'w';
				if (y == 4 && x >= 4 && x <= 5) return 'r';
				if (y == 5 && (x == 3 || x == 6)) return 'r';
				return (x == 1 || x == 8 || y == 1 || y == 6) ? '%' : '#';
			});
		add("rune_mask", "Runed Mask", CosmeticSlot.FACE, 0xFF8C55E8, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(runeMask);
			ctx.pose().popPose();
		});

		// ------------------------------------------------------------ back
		VoxelModel backpack = VoxelModel.shape(1f, -3.5f, 1f, 2f, palette('#', Paint.tint(1f), '%', Paint.tint(0.7f), 'G', Paint.of(0xFFE2B33C), 'k', Paint.of(0xFF2A2A2E)),
			7, 9, 3, (x, y, z) -> {
				boolean side = x == 0 || x == 6;
				if (y == 0) return (x >= 1 && x <= 5) ? '%' : '.';
				if (y == 1) return z == 2 && x == 3 ? 'G' : '%';
				if (y == 8) return side ? '.' : 'k';
				if (z == 2 && y == 4 && x >= 1 && x <= 5) return 'G';
				if (z == 2 && y >= 5 && y <= 6 && x >= 1 && x <= 5) return '%';
				return side && (y == 2 || y == 7) ? '%' : '#';
			});
		VoxelModel straps = VoxelModel.build(1f, -3f, 0f, -2.6f, palette('%', Paint.tint(0.6f)),
			"%....%", "%....%", "%....%", "%....%", "%....%", "%....%");
		add("backpack", "Backpack", CosmeticSlot.BACK, 0xFF4E7A3F, true, ctx -> {
			ctx.attach(ctx.model().body);
			ctx.draw(backpack);
			ctx.draw(straps);
			ctx.pose().popPose();
		});

		// Sits out past the cape (z 2..3.5) so the cape doesn't hide it.
		VoxelModel sheath = VoxelModel.shape(1f, -7f, -5f, 3.6f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.7f), 'G', Paint.of(0xFFE2B33C), 'b', Paint.of(0xFF6B4423),
				'B', Paint.glow(0xFF9FE7FF)),
			15, 18, 1, (x, y, z) -> {
				// Along the diagonal from the left hip (bottom left) to above the right shoulder (top right).
				double t = (x - (14 - y * 0.78)); // 0 on the centre line
				if (y >= 4) return Math.abs(t) <= 1.0 ? (Math.abs(t) <= 0.4 ? '%' : '#') : '.';
				if (y == 3) return Math.abs(t) <= 2.2 ? 'G' : '.'; // guard
				if (y >= 1) return Math.abs(t) <= 0.5 ? 'b' : '.'; // grip
				return Math.abs(t) <= 0.7 ? 'B' : '.'; // glowing pommel gem
			});
		// Tan leather by default: stands out against dark capes.
		add("sword_sheath", "Blade Sheath", CosmeticSlot.BACK, 0xFFA8733F, true, ctx -> {
			ctx.attach(ctx.model().body);
			ctx.draw(sheath);
			ctx.pose().popPose();
		});

		VoxelModel jetpack = VoxelModel.shape(1f, -4.5f, 0f, 2.2f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF242A35), 'c', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFC96A)),
			9, 11, 5, (x, y, z) -> {
				if ((x == 0 || x == 8) && z >= 3 && y >= 2 && y <= 8) return 'k';
				if (y == 0 && x >= 2 && x <= 6) return '%';
				if (y == 10 && x >= 2 && x <= 6) return 'k';
				if (z == 4 && y >= 2 && y <= 8) {
					if ((x == 1 || x == 7) && (y == 3 || y == 7)) return 'w';
					if ((x == 3 || x == 5) && y >= 4 && y <= 7) return 'c';
					if (x == 4 && y == 5) return 'w';
				}
				return (x == 1 || x == 7 || y == 1 || y == 9) ? '%' : '#';
			});
		add("jetpack", "Ion Jetpack", CosmeticSlot.BACK, 0xFF3BCBEA, true, ctx -> {
			ctx.attach(ctx.model().body);
			ctx.pose().translate(0, (float) Math.sin(ctx.time() * 5) * 0.012f, 0);
			ctx.draw(jetpack);
			ctx.pose().popPose();
		});

		VoxelModel quiver = VoxelModel.shape(1f, -3.5f, 0f, 2.4f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF30251F), 'f', Paint.of(0xFFDDC79A), 't', Paint.tintGlow(1f)),
			7, 13, 3, (x, y, z) -> {
				if (y == 0) return x >= 1 && x <= 5 ? 'f' : '.';
				if (y == 1 && x >= 1 && x <= 5) return 'k';
				if (y == 2 && (x == 2 || x == 3 || x == 4) && z == 2) return 't';
				if (y >= 3 && y <= 11 && (x == 1 || x == 5 || z == 2)) return (y % 4 == 0) ? '%' : '#';
				if (y == 12 && (x == 1 || x == 3 || x == 5) && z == 2) return 'f';
				return (y >= 3 && y <= 11 && x >= 1 && x <= 5) ? 'k' : '.';
			});
		add("quiver", "Wayfinder Quiver", CosmeticSlot.BACK, 0xFF7B573A, true, ctx -> {
			ctx.attach(ctx.model().body);
			ctx.draw(quiver);
			ctx.pose().popPose();
		});

		// ------------------------------------------------------------ wings
		// Right wing extends toward +x from a hinge at x=0; the left wing is drawn mirrored.
		VoxelModel dragon = VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.72f), 'k', Paint.of(0xFF201A22),
				'w', Paint.of(0xFFEDE6D6), 'r', Paint.tintGlow(1.2f)),
			32, 28, 1, (x, y, z) -> {
				double t = x / 31.0;
				double bone = 9 - x * 0.27;
				double scallop = 2.2 * Math.abs(Math.sin(x * Math.PI / 7.5));
				double bottom = 27 - x * 0.58 - scallop;
				if ((x == 30 || x == 31) && y <= 2) return 'w';
				if (Math.abs(y - bone) < 0.8) return 'k';
				if (y < bone || y > bottom) return '.';
				// Three raised spars break the membrane into individual, readable panels.
				for (int f : new int[]{8, 16, 23}) {
					double fy = bone + 1;
					double fx = 31 + (f - 31) * ((y - fy) / Math.max(1, (27 - f * 0.58) - fy));
					if (Math.abs(x - fx) < 0.75) return 'k';
				}
				if (x > 25 && Math.abs(y - bottom) < 1.2 && (x + y) % 3 == 0) return 'r';
				return ((x / 3 + y / 2) % 5 == 0 || t > 0.75 && y % 4 == 0) ? '%' : '#';
			});
		// Angel: a row of long feathers fanning out from the arm of the wing, longest at the tip, with
		// soft covert feathers over their roots; each feather has a slightly darker edge and a glowing tip.
		VoxelModel angel = VoxelModel.shape(0.6f, 0, -7f, 0, palette(
				'w', Paint.of(0xFFFBFBFF), 'v', Paint.of(0xFFEDEFF7), 'e', Paint.of(0xFFC9CDDA), 'W', Paint.glow(0xFFFFF1C2)),
			30, 34, 1, (x, y, z) -> {
				// The arm: from the shoulder (0, 7) up to the wrist (17, 1), then out to the tip (29, 4).
				double armY = x < 17 ? 7 - x * 6.0 / 17 : 1 + (x - 17) * 3.0 / 12;
				if (y < armY - 1.2) return '.';
				int best = -1;
				double bestDist = 0, bestT = 0;
				int feathers = 11;
				for (int k = 0; k < feathers; k++) {
					double f = k / (double) (feathers - 1);
					double bx = 1 + f * 26, by = (bx < 17 ? 7 - bx * 6.0 / 17 : 1 + (bx - 17) * 3.0 / 12) + 1;
					double ang = Math.toRadians(100 - f * 62); // pointing down, fanning outward toward the tip
					double len = 13 + f * 13;
					double dx = Math.cos(ang), dy = Math.sin(ang);
					double px = x + 0.5 - bx, py = y + 0.5 - by;
					double t = Math.max(0, Math.min(len, px * dx + py * dy));
					double dist = Math.hypot(px - t * dx, py - t * dy);
					double width = 2.1 - 0.9 * (t / len); // tapering feather
					if (dist <= width) {
						best = k; // later (outer) feathers lie over earlier ones
						bestDist = dist / width;
						bestT = t / len;
					}
				}
				boolean covert = y <= armY + 3.2;
				if (best < 0) return covert ? 'v' : '.';
				if (covert && bestT < 0.3) return 'v';
				if (bestT > 0.9) return 'W';
				if (bestDist > 0.7) return 'e';
				return best % 2 == 0 ? 'w' : 'v';
			});
		VoxelModel mech = VoxelModel.shape(0.5f, 0, -7f, 0, palette(
				'm', Paint.of(0xFF6B7180), 'n', Paint.of(0xFF9AA4B8), 'k', Paint.of(0xFF252A36),
				'c', Paint.tintGlow(1f), 's', Paint.of(0xFF475166), 'w', Paint.glow(0xFFE8FCFF)),
			30, 26, 1, (x, y, z) -> {
				double top = 2 + x * 0.12;
				int plate = x / 5;
				double bottom = 24 - plate * 2.5 - (x % 5) * 0.2;
				if (y < top || y > bottom) return '.';
				if (Math.abs(y - top) < 0.8 || y == 24 - plate * 2) return 's';
				if (x % 5 == 0) return 'k';
				if (x % 5 == 2 && y > top + 2 && y < bottom - 1) return (y % 5 == 0) ? 'w' : 'c';
				if (y == (int) (top + 3) && x % 5 == 3) return 'k';
				return plate % 2 == 0 ? 'm' : 'n';
			});
		add("dragon_wings", "Dragon Wings", CosmeticSlot.WINGS, 0xFF7A1F2B, true, ctx -> wings(ctx, dragon, 1.1));
		add("angel_wings", "Angel Wings", CosmeticSlot.WINGS, 0xFFFFFFFF, false, ctx -> wings(ctx, angel, 0.8));
		add("mech_wings", "Mech Wings", CosmeticSlot.WINGS, 0xFF3FE0FF, true, ctx -> wings(ctx, mech, 0.5));

		VoxelModel butterfly = VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.7f), 'k', Paint.of(0xFF292039), 'g', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFF0C9)),
			30, 30, 1, (x, y, z) -> {
				double nx = x / 29.0, ny = y / 29.0;
				// Two scalloped lobes form a butterfly wing with a clear waist and wing-tip points.
				double upper = Math.pow((nx - 0.72) / 0.38, 2) + Math.pow((ny - 0.28) / 0.33, 2);
				double lower = Math.pow((nx - 0.70) / 0.34, 2) + Math.pow((ny - 0.72) / 0.28, 2);
				boolean inside = (upper < 1 || lower < 1) && x >= 2 && y >= 2;
				if (!inside) return '.';
				if (x < 4 || y < 3 || y > 27) return 'k';
				if (Math.abs(x - (6 + y * 0.62)) < 1) return '%';
				if ((x == 21 || x == 22) && (y == 8 || y == 9 || y == 20 || y == 21)) return 'w';
				if (x >= 19 && x <= 24 && (y == 10 || y == 11 || y == 18 || y == 19)) return 'g';
				return ((x + y) % 6 == 0) ? '%' : '#';
			});
		add("butterfly_wings", "Luna Moth Wings", CosmeticSlot.WINGS, 0xFF9B73E8, true, ctx -> wings(ctx, butterfly, 0.6));

		VoxelModel voidWings = VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF17151E), 'v', Paint.tintGlow(1f), 'w', Paint.glow(0xFFBDF9FF)),
			30, 27, 1, (x, y, z) -> {
				double edge = 26 - x * 0.42 - 2 * Math.sin(x * Math.PI / 6.0);
				if (x < 1 || y < 2 + x * 0.08 || y > edge) return '.';
				if (x < 3 || Math.abs(y - (5 + x * 0.18)) < 1) return 'k';
				if ((x % 6 == 0) && y > 9 && y < edge - 2) return 'v';
				if (x > 25 && y % 5 == 0) return 'w';
				return ((x / 3 + y / 3) % 4 == 0) ? '%' : '#';
			});
		add("void_wings", "Voidfeather Wings", CosmeticSlot.WINGS, 0xFF7654C8, true, ctx -> wings(ctx, voidWings, 0.45));

		// ------------------------------------------------------------ auras
		VoxelModel orb = VoxelModel.build(1f, -1f, -1f, -1f, palette('o', Paint.tintGlow(1f), 'O', Paint.glow(0xFFFFFFFF)),
			".o./ooo/.o.", "ooo/oOo/ooo", ".o./ooo/.o.");
		add("orbit", "Orbit", CosmeticSlot.AURA, 0xFF7C6CFF, true, ctx -> {
			ctx.attach(ctx.model().body);
			for (int i = 0; i < 3; i++) {
				double a = ctx.time() * 1.6 + i * Math.PI * 2 / 3;
				ctx.pose().pushPose();
				ctx.pose().translate((float) Math.cos(a) * 11 / 16f, (8 + (float) Math.sin(a * 2 + i) * 2.5f) / 16f, (float) Math.sin(a) * 11 / 16f);
				PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 90 % 360)));
				ctx.draw(orb);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});

		VoxelModel runeRing = VoxelModel.shape(1f, -12.5f, 0, -12.5f, palette('R', Paint.tintGlow(1f), 'r', Paint.tintGlow(0.6f)),
			25, 1, 25, (x, y, z) -> {
				if (ring(x, z, 12, 10.6, 12.3)) return 'R';
				// Rune marks just inside the ring.
				double ang = Math.atan2(z - 12, x - 12);
				if (ring(x, z, 12, 8.4, 9.6) && ((int) Math.floor((ang + Math.PI) / (Math.PI / 8))) % 2 == 0) return 'r';
				return '.';
			});
		add("rune_ring", "Rune Ring", CosmeticSlot.AURA, 0xFF3FE0FF, true, ctx -> {
			ctx.pose().pushPose();
			ctx.model().root().translateAndRotate(ctx.pose());
			ctx.pose().translate(0, 23.6f / 16f, 0);
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 20 % 360)));
			ctx.draw(runeRing);
			ctx.pose().popPose();
		});

		VoxelModel star = VoxelModel.build(0.5f, -0.75f, -0.75f, -0.75f, palette('s', Paint.tintGlow(1f), 'S', Paint.glow(0xFFFFFFFF)),
			".s./sss/.s.", "sss/sSs/sss", ".s./sss/.s.");
		add("sparkles", "Sparkles", CosmeticSlot.AURA, 0xFFFFD447, true, ctx -> {
			ctx.attach(ctx.model().body);
			for (int i = 0; i < 7; i++) {
				// Each star fades in and out at its own spot, then reappears somewhere else.
				double cycle = ctx.time() * 0.5 + i * 0.37;
				long n = (long) Math.floor(cycle);
				double life = cycle - n;
				float scale = (float) Math.sin(life * Math.PI);
				if (scale <= 0.05f) continue;
				long seed = n * 31 + i * 1013;
				double ang = (seed * 2654435761L % 1000) / 1000.0 * Math.PI * 2;
				double r = 8 + (seed * 40503L % 600) / 100.0;
				double h = -2 + (seed * 9176L % 1600) / 100.0;
				ctx.pose().pushPose();
				ctx.pose().translate((float) (Math.cos(ang) * r) / 16f, (float) h / 16f, (float) (Math.sin(ang) * r) / 16f);
				ctx.pose().scale(scale, scale, scale);
				ctx.draw(star);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});

		add("trail", "Stardust Trail", CosmeticSlot.AURA, 0xFF9B7CFF, true, ctx -> {
			// Drawn as particles while moving (see CosmeticManager.tickTrails).
		});

		VoxelModel crystal = VoxelModel.shape(0.5f, -3f, -5f, -3f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'w', Paint.glow(0xFFFFFFFF), 'c', Paint.tintGlow(1f)),
			6, 10, 6, (x, y, z) -> {
				int width = y < 2 || y > 7 ? 2 : 3;
				if (x < 3 - width || x > 2 + width || z < 3 - width || z > 2 + width) return '.';
				if (y == 0 || y == 9) return (x == 2 || x == 3) && (z == 2 || z == 3) ? 'w' : '.';
				if (x == 2 && z == 2) return 'w';
				if (y == 4 || y == 5) return 'c';
				return (x == 1 || z == 1 || x == 4 || z == 4) ? '%' : '#';
			});
		add("prism_orbit", "Prism Orbit", CosmeticSlot.AURA, 0xFF52D8F2, true, ctx -> {
			ctx.attach(ctx.model().body);
			for (int i = 0; i < 4; i++) {
				double a = ctx.time() * 0.9 + i * Math.PI / 2;
				ctx.pose().pushPose();
				ctx.pose().translate((float) Math.cos(a) * 10 / 16f, (5 + (float) Math.sin(a * 1.5) * 5) / 16f, (float) Math.sin(a) * 7 / 16f);
				PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (-ctx.time() * 55 % 360)));
				ctx.draw(crystal);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});

		VoxelModel flameHalo = VoxelModel.shape(0.75f, -10.5f, 0, -10.5f, palette(
				'f', Paint.tintGlow(1f), 'o', Paint.tint(0.55f), 'w', Paint.glow(0xFFFFE9A6)),
			22, 1, 22, (x, y, z) -> {
				if (!ring(x, z, 10.5, 7.8, 10.4)) return '.';
				double a = Math.atan2(z - 10.5, x - 10.5);
				return Math.sin(a * 8) > 0.45 ? 'w' : ((x + z) % 3 == 0 ? 'o' : 'f');
			});
		add("phoenix_crown", "Phoenix Circlet", CosmeticSlot.AURA, 0xFFFF843D, true, ctx -> {
			ctx.pose().pushPose();
			ctx.model().root().translateAndRotate(ctx.pose());
			ctx.pose().translate(0, 22.5f / 16f, 0);
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 16 % 360)));
			ctx.draw(flameHalo);
			ctx.pose().popPose();
		});

		// ------------------------------------------------------------ pets
		VoxelModel wisp = VoxelModel.shape(1f, -2.5f, -2.5f, -2.5f, palette('o', Paint.tintGlow(1f), 'c', Paint.glow(0xFFFFFFFF), 'k', Paint.of(0xFF101018)),
			5, 5, 5, (x, y, z) -> {
				double d = Math.sqrt((x - 2) * (x - 2) + (y - 2) * (y - 2) + (z - 2) * (z - 2));
				if (d > 2.6) return '.';
				if (z == 0 && y == 2 && (x == 1 || x == 3)) return 'k';
				return d < 1.2 ? 'c' : 'o';
			});
		VoxelModel wispRing = VoxelModel.shape(0.5f, -4.25f, 0, -4.25f, palette('o', Paint.tintGlow(0.8f)),
			17, 1, 17, (x, y, z) -> ring(x, z, 8, 7, 8.4) && (x + z) % 4 != 0 ? 'o' : '.');
		add("wisp", "Eclipse Wisp", CosmeticSlot.PET, 0xFF7CC8FF, true, ctx -> {
			pet(ctx, () -> {
				ctx.draw(wisp);
				ctx.pose().pushPose();
				PoseUtil.mulPose(ctx.pose(), Axis.XP.rotationDegrees(70));
				PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees((float) (ctx.time() * 120 % 360)));
				ctx.draw(wispRing);
				ctx.pose().popPose();
			});
		});

		Map<Character, Paint> cubePaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.78f), 'k', Paint.of(0xFF15151A), 'w', Paint.of(0xFFFFFFFF), 'a', Paint.glow(0xFFFFE066));
		VoxelModel.Shape cubeBody = (x, y, z) -> {
			if (y == 0) return x == 3 && z == 3 ? 'a' : '.';
			if (y == 1) return x == 3 && z == 3 ? '%' : '.';
			int cy = y - 2;
			if (x < 0 || x > 6 || z < 0 || z > 6 || cy > 6) return '.';
			if (z == 0 && cy == 2 && (x == 1 || x == 5)) return 'w';
			if (z == 0 && cy == 3 && (x == 1 || x == 5)) return 'k';
			if (z == 0 && cy == 5 && x >= 2 && x <= 4) return 'k';
			return cy == 6 ? '%' : '#';
		};
		VoxelModel cubeOpen = VoxelModel.shape(1f, -3.5f, -5.5f, -3.5f, cubePaint, 7, 9, 7, cubeBody);
		VoxelModel cubeBlink = VoxelModel.shape(1f, -3.5f, -5.5f, -3.5f, cubePaint, 7, 9, 7,
			(x, y, z) -> (z == 0 && y - 2 == 2 && (x == 1 || x == 5)) ? 'k' : (z == 0 && y - 2 == 3 && (x == 1 || x == 5)) ? '#' : cubeBody.at(x, y, z));
		add("cube_buddy", "Cube Buddy", CosmeticSlot.PET, 0xFF57D68D, true, ctx -> {
			boolean blink = (ctx.time() % 4.0) < 0.15;
			pet(ctx, () -> {
				ctx.pose().scale(0.75f, 0.75f, 0.75f);
				ctx.draw(blink ? cubeBlink : cubeOpen);
			});
		});

		Map<Character, Paint> frogPaint = palette(
				'g', Paint.tint(1f), 'G', Paint.tint(0.68f), 'k', Paint.of(0xFF26352B), 'w', Paint.glow(0xFFFFFFFF), 'p', Paint.of(0xFFEF84A8));
		VoxelModel frog = VoxelModel.shape(0.8f, -3.2f, -4.2f, -3f, frogPaint, 8, 7, 7, (x, y, z) -> {
				if (y == 0 && (x == 0 || x == 1 || x == 6 || x == 7) && z >= 2 && z <= 4) return 'G';
				if (y == 1 && (x == 0 || x == 7) && z >= 2 && z <= 4) return 'g';
				if (x == 0 || x == 7 || y == 6 || z == 0 || z == 6) return '.';
				if (z == 0 && y == 1 && (x == 1 || x == 6)) return 'w';
				if (z == 0 && y == 2 && (x == 1 || x == 6)) return 'k';
				if (z == 0 && y == 4 && x >= 3 && x <= 4) return 'p';
				if (y == 5) return 'G';
				return 'g';
			});
		add("moss_frog", "Moss Hopper", CosmeticSlot.PET, 0xFF78C65A, true, ctx -> pet(ctx, () -> {
			double hop = Math.max(0, Math.sin(ctx.time() * 3.2));
			ctx.pose().translate(0, (float) hop * 0.08f, 0);
			ctx.pose().scale(0.8f, 0.8f, 0.8f);
			ctx.draw(frog);
		}));

		Map<Character, Paint> drakePaint = palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF29202D), 'e', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFE4A1));
		VoxelModel miniDrake = VoxelModel.shape(0.75f, -4f, -4f, -3.5f, drakePaint, 9, 9, 8, (x, y, z) -> {
				if (y == 0 && (x == 1 || x == 7) && z <= 4) return 'w';
				if (y == 1 && (x == 1 || x == 7) && z <= 4) return 'k';
				if (z == 0 && y == 2 && (x == 2 || x == 6)) return 'w';
				if (z == 0 && y == 3 && (x == 2 || x == 6)) return 'k';
				if (z == 0 && y == 5 && x >= 3 && x <= 5) return 'e';
				if (x == 0 || x == 8 || y == 8) return '.';
				if (x >= 3 && x <= 5 && y == 1 && z >= 2 && z <= 5) return 'k';
				if (y == 7 && x >= 2 && x <= 6) return '%';
				return ((x + y + z) % 5 == 0) ? '%' : '#';
			});
		add("ember_drake", "Pocket Drake", CosmeticSlot.PET, 0xFFEA7750, true, ctx -> pet(ctx, () -> {
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) Math.sin(ctx.time() * 0.8) * 10));
			ctx.draw(miniDrake);
		}));
		// Pictures for the menus.
		icon("crown", crown, false);
		icon("halo", halo, false);
		icon("cat_ears", catEars, false);
		icon("headphones", headphones, false);
		icon("top_hat", topHat, false);
		icon("bandana", bandana, false);
		icon("shades", shades, false);
		icon("prism_visor", visor, false);
		icon("rune_mask", runeMask, false);
		icon("backpack", backpack, false);
		icon("sword_sheath", sheath, false);
		icon("jetpack", jetpack, false);
		icon("quiver", quiver, false);
		icon("dragon_wings", dragon, true);
		icon("angel_wings", angel, true);
		icon("mech_wings", mech, true);
		icon("butterfly_wings", butterfly, true);
		icon("void_wings", voidWings, true);
		icon("orbit", orb, false);
		icon("rune_ring", runeRing, false);
		icon("sparkles", star, false);
		icon("trail", star, false);
		icon("prism_orbit", crystal, false);
		icon("phoenix_crown", flameHalo, false);
		icon("wisp", wisp, false);
		icon("cube_buddy", cubeOpen, false);
		icon("moss_frog", frog, false);
		icon("ember_drake", miniDrake, false);

		// How far hats rise above the head, in model pixels (the nametag moves up past them).
		lift("crown", 5.6f);
		lift("halo", 5.4f);
		lift("top_hat", 7.6f);
		lift("cat_ears", 3.2f);
	}

	/** Both wings on the back, flapping: faster and wider while sprinting, flying or falling. */
	private static void wings(Cosmetic.Context ctx, VoxelModel wing, double flapScale) {
		var s = ctx.state();
		boolean active = s.isFallFlying || s.walkAnimationSpeed > 0.15f;
		double speed = active ? 7.5 : 1.8;
		float flap = (float) (Math.sin(ctx.time() * speed) * (active ? 19 : 5) * flapScale);
		ctx.attach(ctx.model().body);
		ctx.pose().translate(0, 3f / 16f, 2.6f / 16f);
		for (int side = -1; side <= 1; side += 2) {
			ctx.pose().pushPose();
			ctx.pose().translate(side * 1f / 16f, 0, 0);
			// Swept back and slightly up, flapping around the hinge.
			float phase = side < 0 ? 0.22f : 0f;
			float localFlap = (float) (Math.sin(ctx.time() * speed + phase) * (active ? 3.5 : 1.2) * flapScale);
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees(side * (-28f - flap)));
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees(side * (-12f + localFlap)));
			if (side < 0) ctx.pose().scale(-1, 1, 1);
			ctx.draw(wing);
			ctx.pose().popPose();
		}
		ctx.pose().popPose();
	}

	/** A pet floats beside the player's shoulder, trailing a little behind as they move, and bobs. */
	private static void pet(Cosmetic.Context ctx, Runnable draw) {
		var s = ctx.state();
		Vec3 offset = CosmeticManager.petOffset(s.id, s.x, s.y, s.z, s.bodyRot, ctx.time());
		ctx.pose().pushPose();
		ctx.model().root().translateAndRotate(ctx.pose());
		ctx.pose().translate((float) offset.x, (float) offset.y, (float) offset.z);
		ctx.pose().translate(0, (float) Math.sin(ctx.time() * 2.4) * 0.06f, 0);
		PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) Math.sin(ctx.time() * 0.9) * 25f));
		draw.run();
		ctx.pose().popPose();
	}
}
