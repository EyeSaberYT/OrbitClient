package com.orbitclient.cosmetics;

import com.orbitclient.util.PoseUtil;
import com.orbitclient.cosmetics.VoxelModel.Paint;
import com.mojang.math.Axis;

import static com.orbitclient.cosmetics.Cosmetics.*;

/**
 * Back cosmetics, on the body: the body spans x -4..4, y 0..12, z -2..2 (back at z = 2). The cape hangs
 * at z 2..3.5, so flat things sit out past z 3.6 to stay in front of it.
 */
final class Backs {
	private Backs() {
	}

	static void register() {
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
		icon("backpack", backpack, false);
		still("backpack");

		// Sits out past the cape (z 2..3.5) so the cape doesn't hide it.
		// Tan leather by default: stands out against dark capes.
		worn("sword_sheath", "Blade Sheath", CosmeticSlot.BACK, 0xFFA8733F, true, VoxelModel.shape(1f, -7f, -5f, 3.6f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.7f), 'G', Paint.of(0xFFE2B33C), 'b', Paint.of(0xFF6B4423),
				'B', Paint.glow(0xFF9FE7FF)),
			15, 18, 1, (x, y, z) -> {
				// Along the diagonal from the left hip (bottom left) to above the right shoulder (top right).
				double t = (x - (14 - y * 0.78)); // 0 on the centre line
				if (y >= 4) return Math.abs(t) <= 1.0 ? (Math.abs(t) <= 0.4 ? '%' : '#') : '.';
				if (y == 3) return Math.abs(t) <= 2.2 ? 'G' : '.'; // guard
				if (y >= 1) return Math.abs(t) <= 0.5 ? 'b' : '.'; // grip
				return Math.abs(t) <= 0.7 ? 'B' : '.'; // glowing pommel gem
			}));

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
		icon("jetpack", jetpack, false);

		worn("quiver", "Wayfinder Quiver", CosmeticSlot.BACK, 0xFF7B573A, true, VoxelModel.shape(1f, -3.5f, 0f, 2.4f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF30251F), 'f', Paint.of(0xFFDDC79A), 't', Paint.tintGlow(1f)),
			7, 13, 3, (x, y, z) -> {
				if (y == 0) return x >= 1 && x <= 5 ? 'f' : '.';
				if (y == 1 && x >= 1 && x <= 5) return 'k';
				if (y == 2 && (x == 2 || x == 3 || x == 4) && z == 2) return 't';
				if (y >= 3 && y <= 11 && (x == 1 || x == 5 || z == 2)) return (y % 4 == 0) ? '%' : '#';
				if (y == 12 && (x == 1 || x == 3 || x == 5) && z == 2) return 'f';
				return (y >= 3 && y <= 11 && x >= 1 && x <= 5) ? 'k' : '.';
			}));

		// An electric guitar slung diagonally: body at the hip, headstock past the right shoulder.
		worn("electric_guitar", "Electric Guitar", CosmeticSlot.BACK, 0xFFE5484D, true, VoxelModel.shape(1f, -8f, -6f, 3.6f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.7f), 'w', Paint.of(0xFFF4F1EA), 'k', Paint.of(0xFF1E1E22), 'm', Paint.of(0xFFB9C0CC),
				'n', Paint.of(0xFF5A3A22), 'f', Paint.of(0xFFD8D8D8)),
			16, 20, 2, (x, y, z) -> {
				double px = -8 + x + 0.5, py = -6 + y + 0.5;
				double sx = px + 4.8, sy = py - 13;
				double ux = 0.566, uy = -0.824;
				double s = sx * ux + sy * uy, q = sx * -uy + sy * ux;
				if (s >= 0 && s <= 7.6) {
					double low = Math.hypot(s - 2.6, q), up = Math.hypot(s - 5.8, q * 1.15);
					if (low <= 3.4 || up <= 2.7) {
						boolean rim = low > 2.6 && up > 1.9;
						if (z == 1) return rim ? '%' : '#';
						if (Math.abs(q) <= 1.3 && (Math.abs(s - 3.6) < 0.5 || Math.abs(s - 5.2) < 0.5)) return 'k';
						if (Math.abs(q) <= 1.3 && Math.abs(s - 2) < 0.5) return 'm';
						if (q > 0.6 && s > 1.2 && s < 6.4 && !rim) return 'w';
						return rim ? '%' : '#';
					}
				}
				if (z == 1) return '.';
				if (s > 7.6 && s <= 17 && Math.abs(q) <= 0.7) return ((int) (s * 1.4)) % 2 == 0 ? 'n' : 'f';
				if (s > 17 && s <= 19.6 && Math.abs(q) <= 1.3) return Math.abs(q) > 0.8 && ((int) (s * 2)) % 2 == 0 ? 'f' : 'k';
				return '.';
			}));

		// A round shield: painted quarters over planks, an iron rim with rivets and a domed boss.
		worn("round_shield", "Round Shield", CosmeticSlot.BACK, 0xFF2F6FB0, true, VoxelModel.shape(1f, -6f, 0f, 3.6f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.8f), 'w', Paint.of(0xFFA7783F), 'W', Paint.of(0xFF8A6131), 'm', Paint.of(0xFF8E96A3),
				'r', Paint.of(0xFFD7DCE3), 'b', Paint.of(0xFFB9C0CC), 'B', Paint.glow(0xFFFFFFFF)),
			12, 12, 2, (x, y, z) -> {
				double dx = x - 5.5, dy = y - 5.5, d = Math.hypot(dx, dy);
				if (z == 1) return d <= 1.6 ? (dx < 0 && dy < 0 ? 'B' : 'b') : '.';
				if (d > 6.05) return '.';
				if (d > 5) return Math.floorMod((int) Math.round(Math.atan2(dy, dx) * 4 / Math.PI * 2), 2) == 0 && d > 5.4 ? 'r' : 'm';
				if (d <= 1.6) return 'b';
				if ((dx > 0) != (dy > 0)) return x % 3 == 0 ? '%' : '#';
				return x % 3 == 0 ? 'W' : 'w';
			}));

		// A leather scroll case with brass bands and a sealed scroll peeking out.
		worn("scroll_case", "Scroll Case", CosmeticSlot.BACK, 0xFF8A5A33, true, VoxelModel.shape(1f, -4f, -4f, 3.6f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.72f), 'G', Paint.of(0xFFE2B33C), 'p', Paint.of(0xFFF1E4C3), 'r', Paint.of(0xFFC0262D)),
			10, 16, 3, (x, y, z) -> {
				double cx = 5.5 + (8 - y) * 0.22;
				double t = x + 0.5 - cx;
				boolean core = Math.abs(t) <= 1.5 && !(Math.abs(t) > 0.9 && z != 1);
				if (!core) return '.';
				if (y <= 1) return y == 0 && z == 1 && Math.abs(t) < 1 ? 'r' : 'p';
				if (y == 2 || y == 3) return y == 2 ? 'G' : '%';
				if (y == 7 || y == 12) return 'G';
				if (y == 15) return '%';
				return t < -0.6 ? '%' : '#';
			}));

		// A balloon on a string tied to your back, bobbing and swaying above your shoulder.
		var balloonPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.75f), 'w', Paint.glow(0xFFFFFFFF), 's', Paint.of(0xFFF4F4F4));
		VoxelModel.Shape balloonShape = (x, y, z) -> {
			if (y >= 9) return x == 3 && z == 3 ? 's' : '.';
			double d = Math.pow((x - 3) / 3.4, 2) + Math.pow((y - 3.6) / 4.1, 2) + Math.pow((z - 3) / 3.4, 2);
			if (y == 8) return x == 3 && z == 3 ? '%' : '.';
			if (d > 1) return '.';
			if (x == 1 && y == 1 && z <= 2) return 'w';
			return y >= 6 ? '%' : '#';
		};
		VoxelModel balloon = VoxelModel.shape(1f, -3.5f, -9f, -3.5f, balloonPaint, 7, 9, 7, balloonShape);
		VoxelModel balloonIcon = VoxelModel.shape(1f, -3.5f, -9f, -3.5f, balloonPaint, 7, 15, 7, balloonShape);
		VoxelModel knot = VoxelModel.shape(0.5f, -0.25f, -0.25f, -0.25f, balloonPaint, 1, 1, 1, (x, y, z) -> 's');
		add("party_balloon", "Party Balloon", CosmeticSlot.BACK, 0xFFF05A7E, true, ctx -> {
			double t = ctx.time();
			double bx = 7 + Math.sin(t * 1.1) * 1.5, by = -15 + Math.sin(t * 1.7) * 0.8, bz = 8 + Math.cos(t * 0.9) * 1.2;
			ctx.attach(ctx.model().body);
			// The string: dots from the knot at the back up to the balloon, sagging a little.
			for (int i = 0; i <= 26; i++) {
				double k = i / 26.0;
				ctx.pose().pushPose();
				at(ctx.pose(), k * bx + Math.sin(k * Math.PI) * 1.2, 5 + k * (by - 5), 2.4 + k * (bz - 2.4));
				ctx.draw(knot, 0xFFFFFFFF);
				ctx.pose().popPose();
			}
			at(ctx.pose(), bx, by, bz);
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees((float) Math.sin(t * 1.1) * -8f));
			ctx.draw(balloon);
			ctx.pose().popPose();
		});
		icon("party_balloon", balloonIcon, false);

		// Two katanas crossed in an X, wrapped grips over the shoulders.
		var katanaPaint = palette('b', Paint.of(0xFFDDE3EA), 'e', Paint.glow(0xFFFFFFFF), 'G', Paint.of(0xFFE2B33C), 't', Paint.tint(1f), 'k', Paint.of(0xFF15151A));
		worn("twin_katanas", "Twin Katanas", CosmeticSlot.BACK, 0xFF8E1F2B, true, VoxelModel.shape(0.5f, -7f, -5f, 3.6f, katanaPaint,
			28, 38, 2, (x, y, z) -> {
				double px = -7 + (x + 0.5) * 0.5, py = -5 + (y + 0.5) * 0.5;
				int side = z == 0 ? 1 : -1;
				double ax = side * -6.2, ay = -4.2, bx = side * 5.4, by = 13.4;
				double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy);
				double ux = dx / len, uy = dy / len;
				double s = (px - ax) * ux + (py - ay) * uy, q = (px - ax) * -uy + (py - ay) * ux;
				if (s < 0 || s > len) return '.';
				double f = s / len;
				q -= side * Math.sin(f * Math.PI) * 0.7;
				if (f < 0.24) return Math.abs(q) <= 0.55 ? (((int) (s * 2)) % 2 == 0 ? 'k' : 't') : '.';
				if (f < 0.27) return Math.abs(q) <= 1.3 ? 'G' : '.';
				double w = f > 0.95 ? 0.45 * (1 - f) / 0.05 + 0.05 : 0.5;
				if (Math.abs(q) > w + 0.05) return '.';
				return q * side > 0.15 ? 'e' : 'b';
			}));

		// A lantern hanging from your belt that swings as you move.
		VoxelModel lantern = VoxelModel.shape(0.75f, -1.875f, 0f, -1.875f, palette(
				'k', Paint.of(0xFF2A2522), 'K', Paint.of(0xFF4A423B), 'L', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFF4D6)),
			5, 9, 5, (x, y, z) -> {
				boolean corner = (x == 0 || x == 4) && (z == 0 || z == 4);
				boolean inner = x >= 1 && x <= 3 && z >= 1 && z <= 3;
				return switch (y) {
					case 0 -> x == 2 && z == 2 ? 'k' : '.';
					case 1 -> (x == 1 || x == 3) && z == 2 ? 'k' : '.';
					case 2 -> inner ? 'K' : '.';
					case 3 -> 'k';
					case 4, 5, 6 -> corner ? 'k' : (x == 2 && z == 2 ? 'w' : (y == 5 && (x == 2 || z == 2) ? 'w' : 'L'));
					case 7 -> 'k';
					default -> inner ? 'K' : '.';
				};
			});
		add("wanderer_lantern", "Wanderer's Lantern", CosmeticSlot.BACK, 0xFFFFB347, true, ctx -> {
			var s = ctx.state();
			ctx.attach(ctx.model().body);
			at(ctx.pose(), 3.2, 8.6, 3.8);
			float swing = (float) (Math.sin(ctx.time() * 2.1) * 6 + Math.sin(s.walkAnimationPos * 0.6662) * 14 * Math.min(1, s.walkAnimationSpeed));
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees(swing));
			ctx.draw(lantern);
			ctx.pose().popPose();
		});
		icon("wanderer_lantern", lantern, false);

		// A domed shell with plates outlined in darker seams and a pale rim.
		double[][] scutes = {{0, 1.5}, {0, 5.5}, {0, 9.5}, {-3.4, 3.5}, {3.4, 3.5}, {-3.4, 7.5}, {3.4, 7.5}};
		worn("turtle_shell", "Turtle Shell", CosmeticSlot.BACK, 0xFF4F8A3C, true, VoxelModel.shape(1f, -5.5f, -1.5f, 2f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.85f), 'k', Paint.tint(0.55f), 'y', Paint.of(0xFFC9B26A)),
			11, 14, 4, (x, y, z) -> {
				double px = x - 5, wy = -1.5 + y + 0.5, py = wy - 5.5;
				double e2 = Math.pow(px / 5.6, 2) + Math.pow(py / 7.1, 2);
				if (e2 > 1) return '.';
				double depth = 4.2 * Math.sqrt(1 - e2);
				if (z + 0.5 > depth + 0.3) return '.';
				if (e2 > 0.78) return 'y';
				double b1 = 99, b2 = 99;
				int nearest = 0;
				for (int i = 0; i < scutes.length; i++) {
					double d = Math.hypot(px - scutes[i][0], wy - scutes[i][1]);
					if (d < b1) {
						b2 = b1;
						b1 = d;
						nearest = i;
					} else if (d < b2) b2 = d;
				}
				if (b2 - b1 < 0.75) return 'k';
				return nearest % 2 == 0 ? '#' : '%';
			}));

		// A fishing rod over the shoulder; the bobber dangles and sways from the tip.
		var rodPaint = palette('c', Paint.of(0xFFC69C6D), 'r', Paint.tint(1f), 'R', Paint.tint(0.7f), 'm', Paint.of(0xFF9AA3AE), 'l', Paint.of(0xFFE8E8E8),
			'b', Paint.of(0xFFE53935), 'w', Paint.of(0xFFF7F7F7));
		VoxelModel.Shape rodShape = (x, y, z) -> {
			double px = -5 + (x + 0.5) * 0.5, py = -13 + (y + 0.5) * 0.5;
			double d = seg(px, py, -4, 11, 7.4, -12.4);
			double f = (11 - py) / 23.4;
			if (d <= 0.55 && f < 0.22) return 'c';
			if (Math.hypot(px - (-1.6), py - 6.4) <= 1.0) return 'm';
			return d <= (f < 0.6 ? 0.45 : 0.3) ? (((int) (f * 18)) % 4 == 0 ? 'R' : 'r') : '.';
		};
		VoxelModel rod = VoxelModel.shape(0.5f, -5f, -13f, 3.6f, rodPaint, 26, 50, 1, rodShape);
		VoxelModel bobber = VoxelModel.centered(0.5f, rodPaint, 4, 4, 4, (x, y, z) -> (x == 0 || x == 3) && (z == 0 || z == 3) ? '.' : (y < 2 ? 'b' : 'w'));
		VoxelModel lineDot = VoxelModel.centered(0.3f, rodPaint, 1, 1, 1, (x, y, z) -> 'l');
		// The picture adds the line and bobber hanging still from the tip.
		VoxelModel rodIcon = VoxelModel.shape(0.5f, -5f, -13f, 3.6f, rodPaint, 26, 50, 1, (x, y, z) -> {
			double px = -5 + (x + 0.5) * 0.5, py = -13 + (y + 0.5) * 0.5;
			if (Math.hypot(px - 7.4, py + 3.5) < 1) return py < -3.5 ? 'b' : 'w';
			if (Math.abs(px - 7.4) < 0.25 && py > -12.4 && py < -4) return 'l';
			return rodShape.at(x, y, z);
		});
		add("fishing_rod", "Angler's Rod", CosmeticSlot.BACK, 0xFF2E86DE, true, ctx -> {
			double t = ctx.time();
			double sway = Math.sin(t * 1.8) * 1.6;
			ctx.attach(ctx.model().body);
			ctx.draw(rod);
			double tipX = 7.4, tipY = -12.4, z = 4;
			double endX = tipX + sway, endY = -4.5 + Math.abs(Math.sin(t * 1.8)) * 0.6;
			for (int i = 0; i < 14; i++) {
				double k = i / 14.0;
				ctx.pose().pushPose();
				at(ctx.pose(), tipX + (endX - tipX) * k, tipY + (endY - tipY) * k, z);
				ctx.draw(lineDot, 0xFFFFFFFF);
				ctx.pose().popPose();
			}
			at(ctx.pose(), endX, endY + 1, z);
			ctx.draw(bobber);
			ctx.pose().popPose();
		});
		icon("fishing_rod", rodIcon, false);

		// A recurve bow across the back, grip wrapped in your color.
		worn("hunters_bow", "Hunter's Bow", CosmeticSlot.BACK, 0xFF2F6B3A, true, VoxelModel.shape(0.5f, -6.5f, -4.5f, 3.6f, palette(
				'w', Paint.of(0xFF8B5A2B), 'W', Paint.of(0xFF6E4520), 'g', Paint.tint(1f), 's', Paint.of(0xFFEFE8D8), 'G', Paint.of(0xFFE2B33C)),
			28, 38, 1, (x, y, z) -> {
				double px = -6.5 + (x + 0.5) * 0.5, py = -4.5 + (y + 0.5) * 0.5;
				double ax = 6, ay = -3.5, bx = -5.5, by = 13.5;
				double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
				double s = (px - ax) * ux + (py - ay) * uy, q = (px - ax) * -uy + (py - ay) * ux;
				double f = s / len;
				if (f < -0.02 || f > 1.02) return '.';
				if (Math.abs(q) <= 0.2 && f >= 0 && f <= 1) return 's';
				double bend = -(2.6 * Math.sin(Math.PI * f) - 0.9 * Math.pow(Math.abs(2 * f - 1), 6));
				double thick = f > 0.42 && f < 0.58 ? 0.65 : 0.42 - 0.12 * Math.abs(2 * f - 1);
				if (Math.abs(q - bend) > thick) return '.';
				if (f > 0.42 && f < 0.58) return 'g';
				if (f < 0.04 || f > 0.96) return 'G';
				return q - bend > 0 ? 'W' : 'w';
			}));

		// A big clockwork key in your back that keeps turning.
		VoxelModel key = VoxelModel.centered(1f, palette('G', Paint.tint(1f), 'D', Paint.tint(0.7f), 'w', Paint.tint(1.3f)),
			14, 7, 6, (x, y, z) -> {
				double ax = x - 6.5, ay = y - 3;
				if (z <= 3) return Math.abs(ax) < 1 && Math.abs(ay) < 1 ? (z == 0 ? 'D' : 'G') : '.';
				if (z == 5) return '.';
				for (int side = -1; side <= 1; side += 2) {
					double d = Math.hypot(ax - side * 3.6, ay);
					if (d <= 3.2 && d >= 1.3) return d > 2.6 ? 'D' : (ay < -1 ? 'w' : 'G');
				}
				return Math.abs(ax) <= 1.5 && Math.abs(ay) <= 1.6 ? 'G' : '.';
			});
		add("wind_up_key", "Wind-up Key", CosmeticSlot.BACK, 0xFFE2B33C, true, ctx -> {
			ctx.attach(ctx.model().body);
			at(ctx.pose(), 0, 5, 5);
			// Turns in little steps, like it's being wound.
			double turns = ctx.time() * 1.5;
			double step = Math.floor(turns) + Math.min(1, (turns % 1) * 3);
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees((float) (step * 45 % 360)));
			ctx.draw(key);
			ctx.pose().popPose();
		});
		icon("wind_up_key", key, false);

		// A plush bear riding on your back, peeking over your shoulder.
		VoxelModel.Shape bear = (x, y, z) -> {
			boolean ear = y <= 1 && (x <= 1 || x >= 6) && z >= 2 && z <= 3;
			if (ear) return y == 1 && z == 2 && (x == 1 || x == 6) ? 'p' : '#';
			if (y >= 1 && y <= 6 && x >= 1 && x <= 6 && z >= 1 && z <= 4) {
				boolean edge = (y == 1 || y == 6) && (x == 1 || x == 6);
				if (edge) return '.';
				if (z == 1 && y == 3 && (x == 2 || x == 5)) return 'k';
				return '#';
			}
			if (z == 0 && y == 4 && (x == 3 || x == 4)) return 'k';
			if (z == 0 && y == 5 && (x == 3 || x == 4)) return 'm';
			if (y >= 7 && y <= 12 && x >= 1 && x <= 6 && z >= 1 && z <= 4) {
				if ((y == 12) && (x == 1 || x == 6)) return '.';
				if (z == 1 && y >= 8 && y <= 11 && x >= 3 && x <= 4) return 'm';
				return '%';
			}
			if (y >= 7 && y <= 9 && (x == 0 || x == 7) && z >= 1 && z <= 2) return '#';
			if (y >= 11 && z == 0 && (x == 1 || x == 2 || x == 5 || x == 6)) return 'm';
			return '.';
		};
		var bearPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.85f), 'm', Paint.of(0xFFE8C9A0), 'k', Paint.of(0xFF1E1A1A), 'p', Paint.of(0xFFF2A0B4));
		worn("plush_bear", "Plush Bear", CosmeticSlot.BACK, 0xFFB07A4F, true, VoxelModel.shape(0.8f, -3.2f, -3f, 3.6f, bearPaint, 8, 13, 5, flipZ(bear, 5)));
		icon("plush_bear", VoxelModel.shape(0.8f, -3.2f, -3f, 3.6f, bearPaint, 8, 13, 5, bear), false);

		// A pickaxe with a crystal head over the shoulder.
		worn("crystal_pickaxe", "Crystal Pickaxe", CosmeticSlot.BACK, 0xFF5CE1E6, true, VoxelModel.shape(0.5f, -7f, -7f, 3.6f, palette(
				'h', Paint.of(0xFF7A5230), 'H', Paint.of(0xFF5C3D22), 'g', Paint.of(0xFF3A3F4A), 'c', Paint.tintGlow(1f), 'C', Paint.tint(0.75f), 'w', Paint.glow(0xFFFFFFFF)),
			30, 40, 1, (x, y, z) -> {
				double px = -7 + (x + 0.5) * 0.5, py = -7 + (y + 0.5) * 0.5;
				// Handle from the left hip up to the head above the right shoulder.
				double ax = -4.5, ay = 12.5, bx = 4.6, by = -3.6;
				double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
				double s = (px - ax) * ux + (py - ay) * uy, q = (px - ax) * -uy + (py - ay) * ux;
				// Head: a bar across the handle's end, curving down and tapering to points.
				double hs = s - len;
				if (Math.abs(q) <= 6.2 && hs > -1.6 && hs < 1.6) {
					double curve = -Math.pow(q / 6.2, 2) * 1.6;
					double half = 1.1 * (1 - Math.pow(Math.abs(q) / 6.4, 1.5)) + 0.15;
					if (Math.abs(hs - curve) <= half) {
						if (Math.abs(q) < 1) return 'C';
						return hs - curve > 0.3 ? 'C' : (Math.abs(q) > 2.2 && Math.abs(q) < 3 ? 'w' : 'c');
					}
				}
				if (s >= 0 && s <= len && Math.abs(q) <= 0.55) return s < 3.5 ? 'g' : (((int) (s * 2)) % 5 == 0 ? 'H' : 'h');
				return '.';
			}));
	}
}
