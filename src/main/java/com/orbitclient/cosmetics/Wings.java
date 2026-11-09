package com.orbitclient.cosmetics;

import com.orbitclient.cosmetics.VoxelModel.Paint;

import static com.orbitclient.cosmetics.Cosmetics.*;

/** Wings. Each model is the right wing, extending toward +x from a hinge at x = 0; the left is drawn mirrored. */
final class Wings {
	private Wings() {
	}

	static void register() {
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
		wingPair("dragon_wings", "Dragon Wings", 0xFF7A1F2B, true, dragon, 1.1, 1);
		wingPair("angel_wings", "Angel Wings", 0xFFFFFFFF, false, angel, 0.8, 1);
		wingPair("mech_wings", "Mech Wings", 0xFF3FE0FF, true, mech, 0.5, 1);

		wingPair("butterfly_wings", "Luna Moth Wings", 0xFF9B73E8, true, VoxelModel.shape(0.5f, 0, -6f, 0, palette(
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
			}), 0.6, 1);

		wingPair("void_wings", "Voidfeather Wings", 0xFF7654C8, true, VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF17151E), 'v', Paint.tintGlow(1f), 'w', Paint.glow(0xFFBDF9FF)),
			30, 27, 1, (x, y, z) -> {
				double edge = 26 - x * 0.42 - 2 * Math.sin(x * Math.PI / 6.0);
				if (x < 1 || y < 2 + x * 0.08 || y > edge) return '.';
				if (x < 3 || Math.abs(y - (5 + x * 0.18)) < 1) return 'k';
				if ((x % 6 == 0) && y > 9 && y < edge - 2) return 'v';
				if (x > 25 && y % 5 == 0) return 'w';
				return ((x / 3 + y / 3) % 4 == 0) ? '%' : '#';
			}), 0.45, 1);

		// Bat: thin membrane between finger bones, scalloped between the fingertips, a claw at the wrist.
		double[][] batTips = {{0, 6}, {1.5, 21}, {10, 24}, {19, 22}, {26, 15}, {29, 4}};
		double wristX = 12, wristY = 3;
		VoxelModel.Shape batBones = (x, y, z) -> {
			double px = x + 0.5, py = y + 0.5;
			if (seg(px, py, 0, 6, wristX, wristY) < 1.0 || seg(px, py, wristX, wristY, 29, 4) < 0.8) return 'b';
			for (int i = 1; i < batTips.length - 1; i++) if (seg(px, py, wristX, wristY, batTips[i][0], batTips[i][1]) < 0.6) return 'b';
			if (seg(px, py, wristX, wristY, wristX - 1.2, wristY - 2.5) < 0.6) return 'c';
			return '.';
		};
		wingPair("bat_wings", "Bat Wings", 0xFF2B2233, true, VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.72f), 'b', Paint.of(0xFF120F16), 'c', Paint.of(0xFFEDE6D6)),
			30, 26, 1, (x, y, z) -> {
				char bone = batBones.at(x, y, z);
				if (bone != '.') return bone;
				double px = x + 0.5 - wristX, py = y + 0.5 - wristY;
				double a = Math.atan2(py, px), r = Math.hypot(px, py);
				for (int i = batTips.length - 1; i > 0; i--) {
					double[] p0 = batTips[i], p1 = batTips[i - 1];
					double a0 = Math.atan2(p0[1] - wristY, p0[0] - wristX), a1 = Math.atan2(p1[1] - wristY, p1[0] - wristX);
					if (a < a0 || a > a1) continue;
					double s = (a - a0) / (a1 - a0);
					double r0 = Math.hypot(p0[0] - wristX, p0[1] - wristY), r1 = Math.hypot(p1[0] - wristX, p1[1] - wristY);
					double edge = r0 + (r1 - r0) * s - (i == 1 ? 0 : 3.2 * Math.sin(Math.PI * s));
					if (r > edge) return '.';
					return r > edge - 1.2 ? '%' : ((x + y) % 7 == 0 ? '%' : '#');
				}
				return '.';
			}), 1.0, 1);

		// Fairy: two pairs of rounded lobes, pale glassy panes with glowing outlines, veins and sparkles; they flutter fast.
		wingPair("fairy_wings", "Fairy Wings", 0xFF8FE8FF, true, VoxelModel.shape(0.5f, 0, -8f, 0, palette(
				'g', Paint.tintGlow(1f), 'v', Paint.tint(0.85f), 'p', Paint.of(0xFFF4F8FF), 'w', Paint.glow(0xFFFFFFFF)),
			28, 32, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5;
				double[][] lobes = {{13, 9, 13.5, 7.5, -0.45}, {9, 22, 9, 6, 0.5}};
				for (double[] l : lobes) {
					double c = Math.cos(l[4]), s = Math.sin(l[4]);
					double dx = px - l[0], dy = py - l[1];
					double u = (dx * c + dy * s) / l[2], v = (-dx * s + dy * c) / l[3];
					double e = u * u + v * v;
					if (e > 1) continue;
					if (e > 0.72) return 'g';
					if (Math.abs(v) < 0.08 || Math.abs(v - u * 0.5) < 0.07 || Math.abs(v + u * 0.5) < 0.07) return 'v';
					if ((x * 7 + y * 3) % 23 == 0) return 'w';
					return 'p';
				}
				return '.';
			}), 0.7, 2.6);

		// Phoenix: flame feathers, dark red at the root to white-hot tips, flickering.
		var phoenixPaint = palette('r', Paint.glow(0xFFD7261E), 'o', Paint.glow(0xFFFF7A1A), 'y', Paint.glow(0xFFFFC93C), 'w', Paint.glow(0xFFFFF1B0));
		VoxelModel[] phoenix = new VoxelModel[4];
		for (int frame = 0; frame < 4; frame++) {
			double phase = frame * Math.PI / 2;
			phoenix[frame] = VoxelModel.shape(0.6f, 0, -7f, 0, phoenixPaint, 30, 34, 1, (x, y, z) -> {
				double armY = x < 17 ? 7 - x * 6.0 / 17 : 1 + (x - 17) * 3.0 / 12;
				if (y < armY - 1.2) return '.';
				if (y < armY + 1.2) return 'r';
				double bestT = -1;
				for (int k = 0; k < 10; k++) {
					double f = k / 9.0;
					double bx = 1 + f * 26, by = (bx < 17 ? 7 - bx * 6.0 / 17 : 1 + (bx - 17) * 3.0 / 12) + 1;
					double ang = Math.toRadians(100 - f * 62);
					double len = 12 + f * 12 + 1.6 * Math.sin(phase + k * 1.3);
					double dx = Math.cos(ang), dy = Math.sin(ang);
					double px = x + 0.5 - bx, py = y + 0.5 - by;
					double t = Math.max(0, Math.min(len, px * dx + py * dy));
					double wob = Math.sin(t * 0.7 + phase + k) * 0.7 * (t / len);
					double dist = Math.abs((px - t * dx) * -dy + (py - t * dy) * dx - wob);
					double along = Math.hypot(px - t * dx, py - t * dy);
					if (along <= 2.2 && dist <= 2.0 - 1.6 * (t / len)) bestT = t / len;
				}
				if (bestT < 0) return '.';
				return bestT < 0.35 ? 'r' : bestT < 0.65 ? 'o' : bestT < 0.88 ? 'y' : 'w';
			});
		}
		add("phoenix_wings", "Phoenix Wings", CosmeticSlot.WINGS, 0xFFFF7A1A, false, ctx -> wings(ctx, phoenix[(int) Math.floor(ctx.time() * 9) % 4], 0.9));
		icon("phoenix_wings", phoenix[0], true);

		// Bee: small veined wings that buzz.
		wingPair("bee_wings", "Bee Wings", 0xFFDDEFF8, false, VoxelModel.shape(0.5f, 0, -8f, 0, palette(
				'p', Paint.of(0xFFDDEFF8), 'k', Paint.of(0xFF4A4F5A), 'e', Paint.of(0xFF9FB4C4)),
			22, 22, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5;
				double[][] lobes = {{11, 7, 10.5, 4.2, -0.5}, {8, 15, 7, 3.2, 0.3}};
				for (double[] l : lobes) {
					double c = Math.cos(l[4]), s = Math.sin(l[4]);
					double dx = px - l[0], dy = py - l[1];
					double u = (dx * c + dy * s) / l[2], v = (-dx * s + dy * c) / l[3];
					double e = u * u + v * v;
					if (e > 1) continue;
					if (e > 0.7) return 'e';
					if (Math.abs(v) < 0.12 || Math.abs(u - 0.3) < 0.06 || Math.abs(u + 0.3) < 0.06) return 'k';
					return 'p';
				}
				return '.';
			}), 0.35, 4.5);

		// Crystal: faceted shards fanning out, one face glowing, a bright core line.
		double[][] shards = {{-40, 24}, {-15, 28}, {10, 26}, {32, 22}, {52, 17}, {72, 12}};
		wingPair("crystal_wings", "Crystal Wings", 0xFF7FE3FF, true, VoxelModel.shape(0.5f, 0, -8f, 0, palette(
				'#', Paint.tint(0.8f), 'g', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFFFFF)),
			30, 34, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5 - 14;
				char out = '.';
				for (double[] sh : shards) {
					double a = Math.toRadians(sh[0]), len = sh[1];
					double dx = Math.cos(a), dy = Math.sin(a);
					double t = px * dx + py * dy, q = -px * dy + py * dx;
					if (t < 0 || t > len) continue;
					double half = 2.3 * Math.min(t / (0.3 * len), (len - t) / (0.7 * len));
					if (Math.abs(q) > half) continue;
					out = Math.abs(q) < 0.35 ? 'w' : q < 0 ? 'g' : '#';
				}
				return out;
			}), 0.4, 0.8);

		// Cyber: an angular dark frame traced with neon lines and bright nodes at the corners.
		double[] cyberPoly = {0, 4, 14, 0, 29, 2, 24, 10, 27, 14, 18, 18, 20, 24, 8, 22, 0, 16};
		wingPair("cyber_wings", "Cyber Wings", 0xFFFF3FD8, true, VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'k', Paint.of(0xFF141821), 'n', Paint.of(0xFF1C2230), 'c', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFFFFF)),
			30, 26, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5;
				for (int i = 0; i < cyberPoly.length; i += 2) if (Math.hypot(px - cyberPoly[i], py - cyberPoly[i + 1]) < 1.1 && i > 0) return 'w';
				if (!inPolygon(px, py, cyberPoly) && polygonEdge(px, py, cyberPoly) > 0.6) return '.';
				if (polygonEdge(px, py, cyberPoly) < 0.7) return 'c';
				for (int i = 2; i < cyberPoly.length - 2; i += 4) if (seg(px, py, 0, 10, cyberPoly[i], cyberPoly[i + 1]) < 0.45) return 'c';
				return (x / 4 + y / 4) % 2 == 0 ? 'k' : 'n';
			}), 0.5, 1);

		// Leaf: three big leaves with a midrib and veins.
		double[][] leaves = {{-28, 28, 5.5}, {12, 25, 5}, {48, 18, 4}};
		wingPair("leaf_wings", "Leaf Wings", 0xFF5DBB4A, true, VoxelModel.shape(0.5f, 0, -8f, 0, palette(
				'#', Paint.tint(1f), 'e', Paint.tint(0.8f), '%', Paint.tint(0.62f), 'l', Paint.tint(1.18f)),
			30, 34, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5 - 14;
				char out = '.';
				for (double[] lf : leaves) {
					double a = Math.toRadians(lf[0]), len = lf[1], wide = lf[2];
					double dx = Math.cos(a), dy = Math.sin(a);
					double t = px * dx + py * dy, q = -px * dy + py * dx;
					if (t < 0 || t > len) continue;
					double f = t / len;
					double half = wide * Math.sin(Math.PI * Math.pow(f, 0.8));
					if (Math.abs(q) > half) continue;
					if (Math.abs(q) < 0.45) out = '%';
					else if (Math.abs(q) > half - 0.8) out = 'e';
					else if (((t - Math.abs(q) * 1.1) % 3.6 + 3.6) % 3.6 < 0.55) out = '%';
					else out = q < 0 ? 'l' : '#';
				}
				return out;
			}), 0.6, 0.8);

		// Origami: folded paper facets in two shades with crisp folds.
		double[][] folds = {{29, 2}, {25, 13}, {15, 22}, {5, 21}};
		wingPair("origami_wings", "Origami Wings", 0xFFF4F1E8, true, VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.82f), 'f', Paint.tint(0.62f)),
			30, 24, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5, hx = 0, hy = 5;
				for (int i = 0; i < folds.length - 1; i++) {
					double[] a = folds[i], b = folds[i + 1];
					double mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2;
					if (inPolygon(px, py, hx, hy, a[0], a[1], b[0], b[1])) {
						if (seg(px, py, hx, hy, a[0], a[1]) < 0.5 || seg(px, py, hx, hy, b[0], b[1]) < 0.5) return 'f';
						// Each facet is folded along its middle: one half lit, one half shaded.
						double side = (mx - hx) * (py - hy) - (my - hy) * (px - hx);
						return side > 0 ? '#' : '%';
					}
				}
				return '.';
			}), 0.6, 0.9);

		// Bone: just the skeleton of a wing, its joints glowing faintly.
		wingPair("bone_wings", "Bone Wings", 0xFF7CF7C0, true, VoxelModel.shape(0.5f, 0, -6f, 0, palette(
				'b', Paint.of(0xFFEDE6D3), 'B', Paint.of(0xFFC9BFA5), 'j', Paint.tintGlow(1f)),
			30, 26, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5;
				if (Math.hypot(px - wristX, py - wristY) < 1.6) return 'j';
				for (int i = 1; i < batTips.length; i++) if (Math.hypot(px - batTips[i][0], py - batTips[i][1]) < 1.1) return 'j';
				if (seg(px, py, 0, 6, wristX, wristY) < 1.1) return 'B';
				for (int i = 1; i < batTips.length; i++) {
					double d = seg(px, py, wristX, wristY, batTips[i][0], batTips[i][1]);
					if (d < 0.75) return d < 0.3 ? 'b' : 'B';
				}
				return '.';
			}), 0.9, 1);

		// Raven: short, ragged black feathers with a sheen in your color.
		wingPair("raven_wings", "Raven Wings", 0xFF4A55A8, true, VoxelModel.shape(0.55f, 0, -7f, 0, palette(
				'k', Paint.of(0xFF17161C), 'n', Paint.of(0xFF23222B), 's', Paint.tint(1f), 'e', Paint.of(0xFF0E0D12)),
			28, 28, 1, (x, y, z) -> {
				double armY = x < 15 ? 7 - x * 5.0 / 15 : 2 + (x - 15) * 2.0 / 12;
				if (y < armY - 1.2) return '.';
				int best = -1;
				double bestDist = 0, bestT = 0;
				for (int k = 0; k < 9; k++) {
					double f = k / 8.0;
					double bx = 1 + f * 24, by = (bx < 15 ? 7 - bx * 5.0 / 15 : 2 + (bx - 15) * 2.0 / 12) + 1;
					double ang = Math.toRadians(105 - f * 70);
					double len = (11 + f * 11) * (k % 3 == 1 ? 0.82 : 1);
					double dx = Math.cos(ang), dy = Math.sin(ang);
					double px = x + 0.5 - bx, py = y + 0.5 - by;
					double t = Math.max(0, Math.min(len, px * dx + py * dy));
					double dist = Math.hypot(px - t * dx, py - t * dy);
					double width = 2.2 - 1.2 * (t / len);
					if (dist <= width) {
						best = k;
						bestDist = dist / width;
						bestT = t / len;
					}
				}
				boolean covert = y <= armY + 3;
				if (best < 0) return covert ? 'n' : '.';
				if (covert && bestT < 0.3) return (x + y) % 4 == 0 ? 's' : 'n';
				if (bestDist > 0.75) return 'e';
				if (bestDist < 0.25 && bestT > 0.2 && bestT < 0.8) return 's';
				return best % 2 == 0 ? 'k' : 'n';
			}), 0.9, 1);

		// Stained glass: bright panes between dark lead lines.
		double[][] seeds = {{4, 6}, {10, 3}, {17, 4}, {25, 5}, {7, 12}, {14, 10}, {21, 11}, {27, 13}, {6, 19}, {13, 17}, {20, 19}, {11, 24}};
		int[] glass = {0xFFE5484D, 0xFF3F7CFF, 0xFFF5C542, 0xFF57D68D, 0xFF9B6CFF, 0xFF3FE0FF};
		var glassPaint = palette('k', Paint.of(0xFF151417), '0', Paint.glow(glass[0]), '1', Paint.glow(glass[1]), '2', Paint.glow(glass[2]),
			'3', Paint.glow(glass[3]), '4', Paint.glow(glass[4]), '5', Paint.glow(glass[5]));
		wingPair("stained_glass_wings", "Stained Glass Wings", 0xFFFFFFFF, false, VoxelModel.shape(0.5f, 0, -6f, 0, glassPaint,
			30, 28, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5;
				double c = Math.cos(-0.25), s = Math.sin(-0.25);
				double dx = px - 15, dy = py - 13;
				double u = (dx * c + dy * s) / 15, v = (-dx * s + dy * c) / 11.5;
				double e = u * u + v * v;
				if (e > 1 || px < 1) return '.';
				if (e > 0.86 || px < 2) return 'k';
				double b1 = 99, b2 = 99;
				int nearest = 0;
				for (int i = 0; i < seeds.length; i++) {
					double d = Math.hypot(px - seeds[i][0], py - seeds[i][1]);
					if (d < b1) {
						b2 = b1;
						b1 = d;
						nearest = i;
					} else if (d < b2) b2 = d;
				}
				if (b2 - b1 < 0.8) return 'k';
				return (char) ('0' + nearest % 6);
			}), 0.5, 1);
	}
}
