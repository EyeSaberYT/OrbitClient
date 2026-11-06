package com.orbitclient.cosmetics;

import com.orbitclient.cosmetics.VoxelModel.Paint;
import com.mojang.math.Axis;

import static com.orbitclient.cosmetics.Cosmetics.*;

/** Hats, on the head: the head spans x/z -4..4, y -8..0 (top at -8, front at -z). */
final class Hats {
	private Hats() {
	}

	static void register() {
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
		icon("crown", crown, false);
		lift("crown", 5.6f);
		still("crown");

		VoxelModel halo = VoxelModel.shape(1f, -6.5f, 0, -6.5f, palette('H', Paint.tintGlow(1f), 'h', Paint.tintGlow(0.75f)),
			13, 1, 13, (x, y, z) -> ring(x, z, 6, 4.6, 6.2) ? ((x + z) % 3 == 0 ? 'h' : 'H') : '.');
		add("halo", "Halo", CosmeticSlot.HAT, 0xFFFFE89A, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.pose().translate(0, (-12.5f + (float) Math.sin(ctx.time() * 2.2) * 0.6f) / 16f, 0);
			ctx.pose().mulPose(Axis.YP.rotationDegrees((float) (ctx.time() * 25 % 360)));
			ctx.draw(halo);
			ctx.pose().popPose();
		});
		icon("halo", halo, false);
		lift("halo", 5.4f);

		VoxelModel catEars = VoxelModel.build(1f, -4f, -11f, -0.5f, palette('#', Paint.tint(1f), '%', Paint.tint(0.75f), 'p', Paint.of(0xFFF7A1C4)),
			".#....#.", "#%#..#%#", "#p#..#p#");
		add("cat_ears", "Cat Ears", CosmeticSlot.HAT, 0xFF33313B, true, ctx -> {
			ctx.attach(ctx.model().head);
			// A small twitch every few seconds.
			float twitch = (float) Math.max(0, Math.sin(ctx.time() * 1.3) - 0.9) * 60f;
			ctx.pose().mulPose(Axis.XP.rotationDegrees(-twitch * 0.3f));
			ctx.draw(catEars);
			ctx.pose().popPose();
		});
		icon("cat_ears", catEars, false);
		lift("cat_ears", 3.2f);

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
		icon("headphones", headphones, false);
		still("headphones");

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
		icon("top_hat", topHat, false);
		lift("top_hat", 7.6f);
		still("top_hat");

		// A tall cone that flops backward at the tip, stars scattered over it.
		hat("wizard_hat", "Starweaver Hat", 0xFF3B3F9E, true, VoxelModel.shape(1f, -6.5f, -20.5f, -6.5f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.72f), 'b', Paint.of(0xFFE2B33C), 'S', Paint.glow(0xFFFFF0A8), 's', Paint.glow(0xFFBFE8FF)),
			13, 13, 13, (x, y, z) -> {
				double px = x + 0.5 - 6.5, pz = z + 0.5 - 6.5;
				if (y == 12) return px * px + pz * pz <= 6.4 * 6.4 ? '%' : '.';
				double h = (11 - y) / 11.0;
				double r = 4.6 * (1 - h) + 0.8;
				double dz = pz - h * h * 3.4;
				if (px * px + dz * dz > r * r) return '.';
				if (y == 11) return 'b';
				int k = Math.floorMod(x * 7 + y * 13 + z * 5, 17);
				if (k == 0) return 'S';
				if (k == 9) return 's';
				return px < -r * 0.3 ? '%' : '#';
			}));

		// Steel dome with a brass brow band, crest and nose guard; ivory horns curl up from the sides.
		hat("viking_helm", "Norse Helm", 0xFF9AA3AE, true, VoxelModel.shape(1f, -10f, -16f, -5f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.78f), 'b', Paint.of(0xFFC9963A), 'k', Paint.of(0xFF6E5424),
				'h', Paint.of(0xFFEDE3C8), 'H', Paint.of(0xFFC7B793), 'T', Paint.of(0xFF6F6352)),
			20, 12, 10, (x, y, z) -> {
				double ax = x - 10 + 0.5, ay = y + 0.5, az = z - 5 + 0.5;
				if (y >= 5 && y <= 10) {
					double w = y == 5 ? 3 : y == 6 ? 4.5 : 5;
					if (Math.abs(ax) <= w && Math.abs(az) <= w && Math.abs(ax) + Math.abs(az) <= w * 1.6) {
						if (y == 10) return (x + z) % 3 == 0 ? 'k' : 'b';
						if (Math.abs(ax) < 1) return 'b';
						return ax < -2 ? '%' : '#';
					}
				}
				if (y == 11 && Math.abs(ax) < 1 && z == 0) return 'b';
				double best = 9, bt = 0;
				for (int i = 0; i <= 30; i++) {
					double t = i / 30.0;
					double d = Math.sqrt(Math.pow(Math.abs(ax) - (4.8 + 4.6 * t), 2) + Math.pow(ay - (8.2 - 7.0 * t * t), 2) + az * az) - (1.45 - 0.95 * t);
					if (d < best) {
						best = d;
						bt = t;
					}
				}
				if (best > 0) return '.';
				if (bt > 0.85) return 'T';
				return ((int) (bt * 7)) % 2 == 0 ? 'h' : 'H';
			}));

		// Folded cuff, knitted ribs, a stripe and a fluffy pom-pom.
		hat("beanie", "Knit Beanie", 0xFFD6453D, true, VoxelModel.shape(1f, -5f, -13f, -5f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.78f), 'w', Paint.of(0xFFF4F1EA), 'v', Paint.of(0xFFD9D4C8)),
			10, 9, 10, (x, y, z) -> {
				double ax = x - 4.5, az = z - 4.5;
				if (y <= 1) {
					double r = y == 0 ? 1 : 1.6;
					return ax * ax + az * az <= r * r + 0.1 ? ((x + z + y) % 3 == 0 ? 'v' : 'w') : '.';
				}
				double w = y == 2 ? 2.6 : y == 3 ? 4 : 5;
				if (Math.abs(ax) > w || Math.abs(az) > w || Math.abs(ax) + Math.abs(az) > w * 1.65) return '.';
				if (y >= 7) return (x + z) % 2 == 0 ? '%' : '#';
				if (y == 5) return 'w';
				return '#';
			}));

		// Tall pleated toque on a band.
		hat("chef_hat", "Chef's Toque", 0xFFE5484D, true, VoxelModel.shape(1f, -6f, -17f, -6f, palette(
				'w', Paint.of(0xFFFAFAFA), 'v', Paint.of(0xFFE2E4EA), 's', Paint.of(0xFFC9CCD4), 'b', Paint.tint(1f)),
			12, 11, 12, (x, y, z) -> {
				double ax = x - 5.5, az = z - 5.5;
				double d = Math.hypot(ax, az), a = Math.atan2(az, ax);
				if (y == 0) return d <= 4.2 ? 'w' : '.';
				if (y <= 3) {
					double r = 5.4 + 0.5 * Math.sin(a * 6);
					return d <= r ? (y == 3 && d > r - 1 ? 's' : 'w') : '.';
				}
				if (y <= 8) return d <= 4.9 ? (Math.sin(a * 8) > 0.6 ? 'v' : 'w') : '.';
				return d <= 5.3 ? 'b' : '.';
			}));

		// Wide woven brim, a ribbon band and a drooping edge.
		hat("straw_hat", "Sun Straw Hat", 0xFFD6453D, true, VoxelModel.shape(1f, -8f, -12f, -8f, palette(
				's', Paint.of(0xFFE8C872), 'S', Paint.of(0xFFC9A44E), 'd', Paint.of(0xFFA9853A), 'r', Paint.tint(1f), 'R', Paint.tint(0.72f)),
			16, 5, 16, (x, y, z) -> {
				double ax = x - 7.5, az = z - 7.5, d = Math.hypot(ax, az);
				char weave = (x + z) % 2 == 0 ? 's' : 'S';
				return switch (y) {
					case 0 -> d <= 3.6 ? weave : '.';
					case 1 -> d <= 4.6 ? weave : '.';
					case 2 -> d <= 4.6 ? ((x + z) % 4 == 0 ? 'R' : 'r') : '.';
					case 3 -> d <= 7.6 ? (d > 4.8 && Math.floorMod((int) Math.round(Math.atan2(az, ax) * 6), 2) == 0 ? 'S' : weave) : '.';
					default -> d > 6.6 && d <= 8 ? 'd' : '.';
				};
			}));

		// Two short horns curving out, glowing at the tips like embers.
		hat("ember_horns", "Ember Horns", 0xFFC0262D, true, VoxelModel.shape(0.5f, -5f, -14f, -3f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'g', Paint.tintGlow(1.25f), 'w', Paint.glow(0xFFFFD27A)),
			20, 12, 8, (x, y, z) -> {
				double ax = Math.abs(x - 9.5) * 0.5, ay = (y + 0.5) * 0.5, az = (z + 0.5) * 0.5;
				double best = 9, bt = 0;
				for (int i = 0; i <= 40; i++) {
					double t = i / 40.0;
					double d = Math.sqrt(Math.pow(ax - (2.0 + 2.2 * t * t), 2) + Math.pow(ay - (5.8 - 5.4 * t), 2) + Math.pow(az - (1.6 + 0.8 * t), 2)) - (1.2 - 0.95 * t);
					if (d < best) {
						best = d;
						bt = t;
					}
				}
				if (best > 0) return '.';
				if (bt > 0.88) return 'w';
				if (bt > 0.65) return 'g';
				return ((int) (bt * 8)) % 2 == 1 ? '%' : '#';
			}));

		// Branching antlers with pale tips.
		double[][] antler = {{2.5, -8, 4.5, -12}, {4.5, -12, 7, -15}, {7, -15, 8.6, -18.5}, {4.5, -12, 3.6, -16.4}, {7, -15, 6.1, -18.8}, {3.2, -9.5, 5.6, -10.4}};
		hat("antlers", "Stag Antlers", 0xFFA0764A, true, VoxelModel.shape(1f, -10f, -20f, -2f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.72f), 'w', Paint.of(0xFFEFE6D2)),
			20, 13, 4, (x, y, z) -> {
				double ax = Math.abs(x - 10 + 0.5), ay = -20 + y + 0.5, az = z - 2 + 0.5;
				if (Math.abs(az) > 0.8) return '.';
				for (int i = 0; i < antler.length; i++) {
					double[] s = antler[i];
					double r = i < 3 ? 0.85 - i * 0.12 : 0.62;
					if (seg(ax, ay, s[0], s[1], s[2], s[3]) <= r) {
						boolean tip = i >= 2 && Math.hypot(ax - s[2], ay - s[3]) < 1.4;
						return tip ? 'w' : (ay > -10 ? '%' : '#');
					}
				}
				return '.';
			}));

		// One ear stands tall, the other leans out; pink inside.
		VoxelModel bunny = VoxelModel.shape(1f, -6f, -19f, -1f, palette('#', Paint.tint(1f), '%', Paint.tint(0.82f), 'p', Paint.of(0xFFF7A1C4)),
			12, 11, 2, (x, y, z) -> {
				for (int side = 0; side < 2; side++) {
					double c = side == 0 ? 3 - (10 - y) * 0.08 : 8 + (10 - y) * 0.3;
					double w = y == 0 ? 0.6 : y == 1 ? 1.1 : 1.6;
					double d = Math.abs(x + 0.5 - c);
					if (d > w) continue;
					if (z == 0 && d < 0.6 && y >= 2 && y <= 9) return 'p';
					return d > 1 ? '%' : '#';
				}
				return '.';
			});
		add("bunny_ears", "Bunny Ears", CosmeticSlot.HAT, 0xFFF4F1EA, true, ctx -> {
			ctx.attach(ctx.model().head);
			float twitch = (float) Math.max(0, Math.sin(ctx.time() * 1.1) - 0.85) * 50f;
			ctx.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.sin(ctx.time() * 1.6) * 2.5f));
			ctx.pose().mulPose(Axis.XP.rotationDegrees(-twitch * 0.35f));
			ctx.draw(bunny);
			ctx.pose().popPose();
		});
		icon("bunny_ears", bunny, false);
		liftTo("bunny_ears", bunny, 0.5f);

		// A ring of leaves around the brow with flowers in three colors (the main one follows your color).
		double[][] flowers = {{4.5, 4.5}, {-4.5, 4.5}, {4.5, -4.5}, {-4.5, -4.5}, {0.5, -4.5}, {-0.5, 4.5}, {4.5, -0.5}, {-4.5, 0.5}};
		hat("flower_crown", "Flower Crown", 0xFFF28AB8, true, VoxelModel.shape(1f, -6f, -9.5f, -6f, palette(
				'l', Paint.of(0xFF4E9A44), 'L', Paint.of(0xFF3B7A33), 'p', Paint.tint(1f), 'w', Paint.of(0xFFF7F4FF),
				'b', Paint.of(0xFF8FB8FF), 'y', Paint.of(0xFFF5D547)),
			12, 2, 12, (x, y, z) -> {
				double ax = x - 5.5, az = z - 5.5;
				for (int k = 0; k < flowers.length; k++) {
					double fx = flowers[k][0], fz = flowers[k][1];
					if (ax == fx && az == fz) return 'y';
					if (y == 1 && Math.abs(ax - fx) <= 1 && Math.abs(az - fz) <= 1 && Math.abs(ax - fx) + Math.abs(az - fz) <= 1)
						return k % 3 == 0 ? 'p' : k % 3 == 1 ? 'w' : 'b';
				}
				if (y == 1 && Math.max(Math.abs(ax), Math.abs(az)) == 4.5) return (x + z) % 3 == 0 ? 'L' : 'l';
				return '.';
			}));

		// Quartered cap with a visor; the propeller on top spins.
		VoxelModel.Shape cap = (x, y, z) -> {
			// Centered on the head (z index 8.5 is z = 0) so the propeller spins in place.
			double ax = x - 4.5, az = z - 8.5;
			if (y == 0) return Math.abs(ax) <= 4 && Math.abs(az) < 1 ? (ax < 0 ? 'r' : 'b') : '.';
			if (y == 1) return Math.abs(ax) < 1 && Math.abs(az) < 1 ? 'k' : '.';
			if (y == 6 && z >= 1 && z <= 3 && Math.abs(ax) <= 3.5) return '%';
			double w = y == 2 ? 1.6 : y == 3 ? 3.5 : 5;
			if (z < 4 || Math.abs(ax) > w || Math.abs(az) > w || Math.abs(ax) + Math.abs(az) > w * 1.6) return '.';
			if (y == 2) return 'r';
			return (ax < 0) == (az < 0) ? '#' : 'y';
		};
		var capPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.75f), 'y', Paint.of(0xFFF5D547), 'r', Paint.of(0xFFE5484D),
			'b', Paint.of(0xFF3F7CFF), 'k', Paint.of(0xFF2A2A30));
		VoxelModel capBody = VoxelModel.shape(1f, -5f, -14f, -9f, capPaint, 10, 7, 14, (x, y, z) -> y == 0 ? '.' : cap.at(x, y, z));
		VoxelModel propeller = VoxelModel.shape(1f, -5f, -14f, -9f, capPaint, 10, 1, 14, cap);
		VoxelModel propIcon = VoxelModel.shape(1f, -5f, -14f, -9f, capPaint, 10, 7, 14, cap);
		add("propeller_cap", "Propeller Cap", CosmeticSlot.HAT, 0xFF3FB8FF, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(capBody);
			ctx.pose().mulPose(Axis.YP.rotationDegrees((float) (ctx.time() * 400 % 360)));
			ctx.draw(propeller);
			ctx.pose().popPose();
		});
		icon("propeller_cap", propIcon, false);
		liftTo("propeller_cap", propIcon, 0);

		// A white helmet with a gold mirrored visor; the antenna light blinks.
		var astroPaint = palette('w', Paint.of(0xFFF2F4F8), 'g', Paint.of(0xFFC4CAD6), 'v', Paint.tint(1f), 'V', Paint.tint(0.7f),
			'h', Paint.glow(0xFFFFFFFF), 'k', Paint.of(0xFF3A3F4A), 'L', Paint.glow(0xFFFF4040));
		VoxelModel.Shape astroShape = (x, y, z) -> {
			double ax = x - 4.5, az = z - 4.5;
			if (y < 3) {
				if (x == 8 && z == 6) return y == 0 ? 'L' : 'k';
				return '.';
			}
			int vy = y - 3; // 0..8 for -9..0
			double m = Math.max(Math.abs(ax), Math.abs(az));
			if (vy == 0 && m > 3.5) return '.';
			if (vy == 8) return 'k';
			if (z == 0 && Math.abs(ax) <= 3 && vy >= 2 && vy <= 6) {
				if (ax - vy == -4 || ax - vy == -3) return 'h';
				return vy >= 5 ? 'V' : 'v';
			}
			if ((Math.abs(ax) == 4.5) && vy >= 3 && vy <= 5 && Math.abs(az) <= 1) return 'g';
			return (Math.abs(ax) == 4.5 && Math.abs(az) == 4.5) ? 'g' : 'w';
		};
		VoxelModel astro = VoxelModel.shape(1f, -5f, -12f, -5f, astroPaint, 10, 12, 10, (x, y, z) -> {
			char c = astroShape.at(x, y, z);
			return c == 'L' ? '.' : c;
		});
		VoxelModel astroLight = VoxelModel.shape(1f, -5f, -12f, -5f, astroPaint, 10, 1, 10, astroShape);
		VoxelModel astroIcon = VoxelModel.shape(1f, -5f, -12f, -5f, astroPaint, 10, 12, 10, astroShape);
		add("astro_helmet", "Astro Helmet", CosmeticSlot.HAT, 0xFFE8B04A, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(astro);
			if (ctx.time() % 1.4 < 0.7) ctx.draw(astroLight);
			ctx.pose().popPose();
		});
		icon("astro_helmet", astroIcon, false);
		liftTo("astro_helmet", astroIcon, 0);

		// Lacquered bowl, lamellar neck guard, swept-back side flaps and a golden crescent crest.
		hat("kabuto", "Samurai Kabuto", 0xFFB5303A, true, VoxelModel.shape(1f, -8f, -18f, -6f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.7f), 'l', Paint.of(0xFFE8D9B0), 'b', Paint.of(0xFF1E1A1A),
				'G', Paint.of(0xFFF2C14E), 'g', Paint.of(0xFFC48A1C)),
			16, 15, 13, (x, y, z) -> {
				double ax = x - 7.5, ay = -18 + y + 0.5, az = z - 6 + 0.5;
				// Crest: a crescent standing on the brow.
				if (z == 1) {
					double d = Math.hypot(ax, ay + 18);
					if (ay > -18 && ay < -11.5 && d >= 4 && d <= 5.4 && Math.abs(ax) > 1.2) return d > 4.8 ? 'g' : 'G';
					if (Math.abs(ax) < 1 && ay > -12 && ay < -10) return 'G';
				}
				double r = Math.hypot(ax, az);
				if (ay > -13 && ay < -8) {
					double w = ay < -12 ? 3 : ay < -11 ? 4.2 : 5;
					if (r <= w) return Math.floorMod((int) Math.round(Math.atan2(az, ax) * 8 / Math.PI), 2) == 0 ? '%' : '#';
				}
				if (ay > -8 && ay < -7 && r <= 5.4) return 'G';
				// Side flaps turned back at the front corners.
				if (az < -2.5 && az > -5 && Math.abs(ax) > 4.5 && Math.abs(ax) < 6.5 && ay > -8 && ay < -4)
					return Math.abs(ax) > 5.5 || ay < -7 ? 'G' : '#';
				// Neck guard: rows flaring outward at the back and sides.
				if (ay > -7 && ay < -2 && az > -2.5) {
					int row = (int) Math.floor(ay + 7);
					double w = 5 + row * 0.55;
					if (r <= w + 0.5 && r >= w - 0.9) return row % 2 == 0 ? '#' : ((x + z) % 3 == 0 ? 'l' : '%');
				}
				return '.';
			}));

		// Black tricorn trimmed in gold, with a plume in your color.
		hat("tricorn", "Corsair Tricorn", 0xFFD6453D, true, VoxelModel.shape(1f, -8f, -17f, -8f, palette(
				'k', Paint.of(0xFF1E1C22), 'K', Paint.of(0xFF2D2A33), 'G', Paint.of(0xFFE2B33C), 'f', Paint.tint(1f), 'F', Paint.tint(0.72f)),
			16, 10, 14, (x, y, z) -> {
				double ax = x - 7.5, az = z - 8 + 0.5, ay = -17 + y + 0.5;
				double[] tri = {0, -7.8, 7.6, 5.2, -7.6, 5.2};
				boolean in = inPolygon(ax, az, tri);
				double edge = polygonEdge(ax, az, tri);
				if (y == 9) return in ? 'k' : '.';
				if (y >= 6 && in && edge < 1.3) return y == 6 ? 'G' : 'K';
				if (y >= 5 && Math.abs(ax) <= 3.5 && Math.abs(az) <= 3.5 && (y > 5 || Math.abs(ax) + Math.abs(az) <= 5)) return 'k';
				// Plume over the left side.
				double d = seg(ax, ay, -4.5, -10, -6.5, -16.2);
				if (az > -1.5 && az < 0.5 && d < 1.2) return d < 0.45 ? 'F' : 'f';
				return '.';
			}));

		// A round red toadstool cap with white spots and cream gills underneath.
		double[][] spots = {{-3, -3}, {3, -1}, {0, 3}, {-5, 2}, {4, 4}, {1, -5}, {-1, 0}, {5, -4}, {-3, 5}};
		hat("toadstool", "Toadstool Cap", 0xFFD93A3A, true, VoxelModel.shape(1f, -7f, -13f, -7f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.78f), 'w', Paint.of(0xFFFAF6EC), 'g', Paint.of(0xFFE9DDBF), 'G', Paint.of(0xFFCFC09C)),
			14, 6, 14, (x, y, z) -> {
				double ax = x - 6.5, az = z - 6.5, d = Math.hypot(ax, az);
				if (y == 5) return d >= 4.6 && d <= 6.9 ? (Math.floorMod((int) Math.round(Math.atan2(az, ax) * 5), 2) == 0 ? 'G' : 'g') : '.';
				double e = 4.5 - y;
				double r = 7 * Math.sqrt(Math.max(0, 1 - Math.pow(e / 5.2, 2)));
				if (d > r) return '.';
				for (double[] s : spots) if (Math.hypot(ax - s[0], az - s[1]) < 1.15) return 'w';
				return y == 4 ? '%' : '#';
			}));
	}
}
