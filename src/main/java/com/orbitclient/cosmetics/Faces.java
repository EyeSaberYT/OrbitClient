package com.orbitclient.cosmetics;

import com.orbitclient.cosmetics.VoxelModel.Paint;

import static com.orbitclient.cosmetics.Cosmetics.*;

/** Face cosmetics, on the head: the face is the -z side (z = -4); the eyes are at y -4..-3, x 1..3 either side. */
final class Faces {
	private Faces() {
	}

	static void register() {
		worn("bandana", "Bandana", CosmeticSlot.FACE, 0xFFD6453D, true, VoxelModel.shape(1f, -5f, -7f, -5f, palette('#', Paint.tint(1f), '%', Paint.tint(0.72f), 'w', Paint.of(0xFFF4F4F4)),
			10, 6, 10, (x, y, z) -> {
				boolean edge = x == 0 || x == 9 || z == 0 || z == 9;
				if (y <= 1) return edge ? ((x * 3 + z + y) % 5 == 0 ? 'w' : (y == 1 ? '%' : '#')) : '.';
				// Knot tails hanging at the back.
				if (z == 9 && (x == 4 || x == 5) && y <= 4) return y % 2 == 0 ? '#' : '%';
				if (z == 9 && x == 3 && y == 2) return '#';
				if (z == 9 && x == 6 && y == 3) return '%';
				return '.';
			}));

		worn("shades", "Shades", CosmeticSlot.FACE, 0xFF3A4C78, true, VoxelModel.build(1f, -5f, -5f, -5f, palette('k', Paint.of(0xFF141418), 'l', Paint.tint(0.55f), 'g', Paint.glow(0xFFEAF6FF)),
			".kkkkkkkk./k........k/k........k/k........k/k........k/k........k",
			".kglk.kglk/..........",
			"..kk...kk."));

		worn("prism_visor", "Prism Visor", CosmeticSlot.FACE, 0xFF32D6E8, true, VoxelModel.shape(1f, -6f, -5.5f, -5.2f, palette(
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
			}));

		worn("rune_mask", "Runed Mask", CosmeticSlot.FACE, 0xFF8C55E8, true, VoxelModel.shape(1f, -5f, -5f, -5.2f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.65f), 'k', Paint.of(0xFF171523), 'r', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFE7B0)),
			10, 8, 2, (x, y, z) -> {
				if (y < 1 || y > 6 || x < 1 || x > 8) return '.';
				if ((x == 1 || x == 8) && y < 3) return '.';
				if (y == 2 && (x == 2 || x == 3 || x == 6 || x == 7)) return 'k';
				if (y == 3 && (x == 2 || x == 3 || x == 6 || x == 7)) return 'w';
				if (y == 4 && x >= 4 && x <= 5) return 'r';
				if (y == 5 && (x == 3 || x == 6)) return 'r';
				return (x == 1 || x == 8 || y == 1 || y == 6) ? '%' : '#';
			}));

		// A gold ring over the right eye with a glint on the glass and a chain down to the cheek.
		worn("monocle", "Monocle", CosmeticSlot.FACE, 0xFFE2B33C, true, VoxelModel.shape(0.5f, -4.5f, -6f, -4.6f, palette(
				'G', Paint.tint(1f), 'D', Paint.tint(0.7f), 'l', Paint.of(0xFFCFE3EE), 'w', Paint.glow(0xFFFFFFFF)),
			10, 16, 1, (x, y, z) -> {
				double d = Math.hypot(x - 4.5, y - 4.5);
				if (d >= 3.3 && d <= 4.5) return y > 5 ? 'D' : 'G';
				if (d < 3.3) return (x == 3 && y == 3) || (x == 4 && y == 2) ? 'w' : 'l';
				if (y >= 9 && Math.abs(x - (7 + (y - 9) * 0.3)) < 0.6) return y % 2 == 0 ? 'G' : 'D';
				return '.';
			}));

		// Stepped pixel shades with white glints.
		worn("pixel_shades", "Pixel Shades", CosmeticSlot.FACE, 0xFF141418, false, VoxelModel.build(1f, -5f, -5f, -5f, palette(
				'k', Paint.of(0xFF141418), 'w', Paint.of(0xFFFFFFFF)),
			"kkkkkkkkkk/k........k/k........k/k........k/k........k/k........k/k........k/k........k/k........k",
			".wkkk.wkkk",
			".kwkk.kwkk",
			"..kk...kk."));

		// Rubber mask with glowing lenses, a snout and a filter canister; straps around the back.
		worn("gas_mask", "Gas Mask", CosmeticSlot.FACE, 0xFF6CFF8E, true, VoxelModel.shape(1f, -5f, -8f, -9f, palette(
				'k', Paint.of(0xFF2A2D2A), 'K', Paint.of(0xFF3A3F3A), 'm', Paint.of(0xFF7D8590), 'v', Paint.of(0xFF4C535C), 'L', Paint.tintGlow(1f)),
			10, 9, 14, (x, y, z) -> {
				double ax = x - 4.5;
				if (z == 4 && Math.abs(ax) <= 3.5 && y >= 1 && !(y == 1 && Math.abs(ax) == 3.5)) {
					if ((y == 3 || y == 4) && Math.abs(Math.abs(ax) - 2) <= 0.5) return 'L';
					if ((y == 2 || y == 5) && Math.abs(Math.abs(ax) - 2) <= 0.5) return 'm';
					return 'K';
				}
				if ((z == 2 || z == 3) && Math.abs(ax) <= 1.5 && y >= 5 && y <= 7) return 'k';
				if ((z == 0 || z == 1) && Math.abs(ax) <= 1.5 && y >= 5 && y <= 7) return z == 0 && (x + y) % 2 == 0 ? 'v' : 'm';
				if (Math.abs(ax) == 4.5 && z >= 5 && (y == 2 || y == 6)) return 'k';
				if (z == 13 && (y == 2 || y == 6)) return 'k';
				return '.';
			}));

		// A long beard down to the chest, with a mustache.
		worn("sage_beard", "Sage Beard", CosmeticSlot.FACE, 0xFFD9D6CF, true, VoxelModel.shape(1f, -4f, -3f, -5f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.78f), 'w', Paint.tint(1.15f)),
			8, 12, 2, (x, y, z) -> {
				double ax = x - 3.5;
				if (y <= 2 && z == 1) return '.';
				if (y == 1 && Math.abs(ax) < 1.5) return '.';
				if (y >= 3) {
					double w = 3.6 - (y - 3) * 0.4;
					if (Math.abs(ax) > w) return '.';
				}
				if ((x + y) % 3 == 0) return '%';
				return (x * 5 + y) % 7 == 0 ? 'w' : '#';
			}));

		// A curled handlebar mustache.
		worn("handlebar", "Dapper Mustache", CosmeticSlot.FACE, 0xFF3B2A20, true, VoxelModel.shape(0.5f, -4.5f, -4f, -4.5f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.72f)),
			18, 6, 1, (x, y, z) -> {
				double X = Math.abs(x + 0.5 - 9), Y = y + 0.5;
				if (X <= 5.6 && Y >= 2 + X * 0.15 && Y <= 5.3 - X * 0.33) return Y > 4 ? '%' : '#';
				double d = Math.hypot(X - 6.6, Y - 2.1);
				if (d >= 0.7 && d <= 1.7 && !(X < 6.6 && Y > 2.3)) return '#';
				return '.';
			}));

		// A black patch over the left eye on a strap that wraps the head.
		worn("eye_patch", "Eye Patch", CosmeticSlot.FACE, 0xFF1E1A1A, true, VoxelModel.shape(1f, -5f, -8f, -5f, palette(
				'#', Paint.tint(1f), 'e', Paint.tint(1.6f), 's', Paint.of(0xFF2A2222)),
			10, 6, 10, (x, y, z) -> {
				double ax = x - 4.5;
				if (z == 0 && ax >= 1.5 && ax <= 3.5 && y >= 3) {
					if (y == 5 && ax == 3.5) return '.';
					return (y == 3 && ax == 1.5) ? 'e' : '#';
				}
				if (z == 0) return y == 2 + (int) Math.floor((ax + 4.5) / 6) && ax < 1.5 ? 's' : '.';
				boolean ringEdge = x == 0 || x == 9 || z == 9;
				return ringEdge && y == 2 ? 's' : '.';
			}));

		// A round red nose that squeaks (squishes) now and then.
		VoxelModel nose = VoxelModel.shape(0.75f, -1.5f, -4.2f, -5.6f, palette(
				'r', Paint.tint(1f), '%', Paint.tint(0.72f), 'w', Paint.glow(0xFFFFE0E0)),
			4, 4, 4, (x, y, z) -> {
				double d = Math.sqrt(Math.pow(x - 1.5, 2) + Math.pow(y - 1.5, 2) + Math.pow(z - 1.5, 2));
				if (d > 2.05) return '.';
				if (x == 1 && y == 0 && z <= 1) return 'w';
				return y == 3 ? '%' : 'r';
			});
		add("clown_nose", "Clown Nose", CosmeticSlot.FACE, 0xFFE02B2B, true, ctx -> {
			ctx.attach(ctx.model().head);
			double t = ctx.time() % 5;
			float squish = t < 0.3 ? (float) Math.sin(t / 0.3 * Math.PI) * 0.25f : 0;
			ctx.pose().translate(0, -2.7f / 16f, -4.5f / 16f);
			ctx.pose().scale(1 + squish, 1 - squish, 1 + squish);
			ctx.pose().translate(0, 2.7f / 16f, 4.5f / 16f);
			ctx.draw(nose);
			ctx.pose().popPose();
		});
		icon("clown_nose", nose, false);

		// Big mirrored goggles with a sunset lens on a colored strap.
		worn("ski_goggles", "Ski Goggles", CosmeticSlot.FACE, 0xFF3FE0FF, true, VoxelModel.shape(1f, -5f, -7f, -5f, palette(
				'k', Paint.of(0xFF1A1B20), 'a', Paint.glow(0xFFFFB347), 'b', Paint.glow(0xFFFF6F91), 'c', Paint.glow(0xFF9B6CFF),
				'w', Paint.glow(0xFFFFF4E0), '#', Paint.tint(1f), '%', Paint.tint(0.7f)),
			10, 5, 10, (x, y, z) -> {
				if (z == 0) {
					if (y == 4 && (x == 4 || x == 5)) return '.';
					if ((y == 0 || y == 4) && (x == 0 || x == 9)) return '.';
					if (y == 0 || y == 4 || x == 0 || x == 9) return 'k';
					if (x == y + 1 || x == y + 6) return 'w';
					return y == 1 ? 'a' : y == 2 ? 'b' : 'c';
				}
				boolean ringEdge = x == 0 || x == 9 || z == 9;
				if (ringEdge && (y == 2 || y == 3)) return y == 2 ? '#' : '%';
				return '.';
			}));

		// Heart-shaped lenses with a shine.
		worn("heart_glasses", "Heart Glasses", CosmeticSlot.FACE, 0xFFFF5C9A, true, VoxelModel.shape(0.5f, -4.5f, -5.5f, -4.6f, palette(
				'p', Paint.tint(1f), 'P', Paint.tint(0.75f), 'w', Paint.glow(0xFFFFFFFF), 'f', Paint.tint(0.55f)),
			18, 9, 18, (x, y, z) -> {
				if (z > 0) return (x == 0 || x == 17) && y == 1 ? 'f' : '.';
				for (int side = 0; side < 2; side++) {
					double cx = side == 0 ? 4.5 : 13.5;
					double X = (x + 0.5 - (cx + 0.5)) / 4.2, Y = -(y + 0.5 - 4.2) / 3.9 + 0.15;
					double v = Math.pow(X * X + Y * Y - 1, 3) - X * X * Y * Y * Y;
					if (v <= 0) {
						if (y == 2 && Math.abs(x - (cx - 2)) < 0.6) return 'w';
						return y >= 6 ? 'P' : 'p';
					}
				}
				if (y == 2 && (x == 8 || x == 9)) return 'f';
				if (y == 1 && (x == 0 || x == 17)) return 'f';
				return '.';
			}));

		// Paper-white frames with red and cyan lenses.
		worn("retro_3d_glasses", "Retro 3D Glasses", CosmeticSlot.FACE, 0xFFF4F4F4, false, VoxelModel.build(1f, -5f, -5f, -5f, palette(
				'w', Paint.of(0xFFF4F4F4), 'r', Paint.of(0xFFE53935), 'c', Paint.of(0xFF29D3E6)),
			"wwwwwwwwww/w........w/w........w/w........w/w........w/w........w/w........w/w........w/w........w",
			"wrrr..cccw",
			"wrrr..cccw",
			"wwww..wwww"));

		// Cloth over the nose and mouth, wrapped around the head and knotted at the back.
		worn("shinobi_mask", "Shinobi Mask", CosmeticSlot.FACE, 0xFF2B2F45, true, VoxelModel.shape(1f, -5f, -3f, -5f, palette(
				'#', Paint.tint(1f), '%', Paint.tint(0.7f), 'v', Paint.tint(0.85f)),
			10, 8, 10, (x, y, z) -> {
				boolean ringEdge = x == 0 || x == 9 || z == 0 || z == 9;
				if (y <= 2) {
					if (!ringEdge) return '.';
					if (z == 0 && y == 1 && (x == 3 || x == 6)) return 'v';
					return y == 1 ? '%' : '#';
				}
				if (z == 9 && (x == 4 || x == 5) && y <= 6) return y % 2 == 0 ? '#' : '%';
				if (z == 9 && x == 3 && y == 4) return '%';
				if (z == 9 && x == 6 && y == 5) return '#';
				return '.';
			}));

		// A fox mask worn tilted on the side of the head (drawn facing front for its picture).
		VoxelModel.Shape kitsune = (u, v, w) -> {
			if (w == 1) return (v >= 6 && v <= 8 && u >= 2 && u <= 4) ? (v == 8 && u == 3 ? 'r' : 'w') : '.';
			if (w == 2) return v == 6 && u == 3 ? 'k' : '.';
			if (v <= 2) {
				int reach = v; // ears widen downward
				boolean left = u <= reach, right = u >= 6 - reach;
				if (!left && !right) return '.';
				return v == 2 && (u == 1 || u == 5) ? 'r' : 'w';
			}
			if (v == 7 && (u == 0 || u == 6)) return '.';
			if (v == 8 && (u <= 1 || u >= 5)) return '.';
			if (v == 9 || v == 10) return '.';
			if ((v == 3 || v == 4) && u == 3) return 'r';
			if (v == 4 && (u == 1 || u == 5)) return 'r';
			if (v == 5 && (u == 1 || u == 2 || u == 4 || u == 5)) return 'k';
			if (v == 7 && (u == 1 || u == 5)) return 'r';
			return 'w';
		};
		var kitsunePaint = palette('w', Paint.of(0xFFF6F2EA), 'r', Paint.tint(1f), 'k', Paint.of(0xFF1A1418));
		// Worn: the mask's front faces out of the head's left side (+x).
		VoxelModel kitsuneWorn = VoxelModel.shape(1f, 4f, -10f, -3.5f, kitsunePaint, 3, 9, 7, (x, y, z) -> kitsune.at(z, y, x));
		VoxelModel kitsuneIcon = VoxelModel.shape(1f, -3.5f, -10f, -6f, kitsunePaint, 7, 9, 3, (x, y, z) -> kitsune.at(x, y, 2 - z));
		add("kitsune_mask", "Kitsune Mask", CosmeticSlot.FACE, 0xFFD8323C, true, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.draw(kitsuneWorn);
			ctx.pose().popPose();
		});
		icon("kitsune_mask", kitsuneIcon, false);
		still("kitsune_mask");

		// A metal eyepiece over the right eye with a glowing lens, wired to an earpiece.
		worn("cyber_eye", "Cyber Eye", CosmeticSlot.FACE, 0xFFFF3B3B, true, VoxelModel.shape(1f, -5f, -6f, -6f, palette(
				'm', Paint.of(0xFF5A6070), 'n', Paint.of(0xFF8C94A6), 'k', Paint.of(0xFF1C1F26), 'L', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFFFFF)),
			6, 5, 8, (x, y, z) -> {
				if (z <= 1 && x >= 1 && x <= 4 && y <= 3) {
					boolean lens = (x == 2 || x == 3) && (y == 1 || y == 2);
					if (lens) return z == 0 ? (x == 2 && y == 1 ? 'w' : 'L') : 'k';
					return y == 0 ? 'n' : 'm';
				}
				if (x == 0 && y >= 1 && y <= 3 && z >= 2 && z <= 5) return z == 4 && y == 2 ? 'L' : 'm';
				if (x == 0 && y == 1 && z == 1) return 'k';
				return '.';
			}));

		// A diving mask with a snorkel up the left side.
		worn("snorkel", "Snorkel Mask", CosmeticSlot.FACE, 0xFFFFB02E, true, VoxelModel.shape(1f, -5f, -12f, -6f, palette(
				'f', Paint.tint(1f), 'F', Paint.tint(0.75f), 'g', Paint.of(0xFFBFE6F5), 'w', Paint.glow(0xFFFFFFFF), 'k', Paint.of(0xFF1E2024),
				'b', Paint.of(0xFF2E86DE), 'o', Paint.of(0xFFFF6A2B)),
			11, 12, 11, (x, y, z) -> {
				double ax = x - 4.5;
				if (x == 10) {
					if (z == 5 && y <= 10) return y <= 1 ? 'o' : 'b';
					if (y == 10 && z >= 1 && z < 5) return 'b';
					return '.';
				}
				if (y == 10 && z == 1 && ax >= 0.5) return 'b';
				if (z == 1 && Math.abs(ax) <= 4 && y >= 6 && y <= 9) {
					if (Math.abs(ax) <= 3 && (y == 7 || y == 8)) return (ax == -2.5 && y == 7) ? 'w' : 'g';
					return y == 9 ? 'F' : 'f';
				}
				if (z == 0 && y == 9 && Math.abs(ax) <= 1) return 'f';
				if (y == 7 && (Math.abs(ax) == 4.5 && z >= 2 || z == 10) && x <= 9) return 'k';
				return '.';
			}));
	}
}
