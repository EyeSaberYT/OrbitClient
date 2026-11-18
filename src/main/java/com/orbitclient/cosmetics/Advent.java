package com.orbitclient.cosmetics;

import com.orbitclient.util.PoseUtil;
import com.orbitclient.cosmetics.VoxelModel.Paint;
import com.mojang.math.Axis;

import static com.orbitclient.cosmetics.Cosmetics.*;

/**
 * The Christmas advent calendar: one limited-edition cosmetic for each day from December 1st to 24th, claimed
 * in Orbit Launcher on its day only. Ownership comes from the cosmetics server like every other cosmetic; this
 * only defines how they look and which day each belongs to ({@link Cosmetic#adventDay}).
 *
 * <p>Head: x/z -4..4, y -8..0 (top at -8, front at -z). Body: x -4..4, y 0..12, z -2..2 (back at +z).
 */
final class Advent {
	private Advent() {
	}

	// Shared colours.
	static final int RED = 0xFFD62828, RED_DARK = 0xFFA21C22, WHITE = 0xFFF4F6FA, SNOW = 0xFFDDE8F2,
		GREEN = 0xFF2E8B3E, GREEN_DARK = 0xFF1E6230, GOLD = 0xFFF2C14E, GOLD_DARK = 0xFFC48A1C,
		ICE = 0xFFBFE8FF, ICE_DEEP = 0xFF6FB6E6, BROWN = 0xFFB0703A, BROWN_DARK = 0xFF7A4A24;

	static void register() {
		santaHat();
		candyCane();
		snowflakeHalo();
		gingerbread();
		reindeerNose();
		hollyCrown();
		giftPack();
		wreath();
		treeTopper();
		snowman();
		snowGoggles();
		stocking();
		bauble();
		antlers();
		peppermints();
		iceCrown();
		frostWings();
		cape("xmas_festive_cape", "Festive Cape", 16);
		northStar();
		scarf();
		cookies();
		elfHat();
		snowGlobe();
		starAura();

		String[] days = {
			"xmas_santa_hat", "xmas_candy_cane", "xmas_snowflake_halo", "xmas_gingerbread", "xmas_reindeer_nose",
			"xmas_holly_crown", "xmas_gift_pack", "xmas_wreath", "xmas_tree_topper", "xmas_snowman",
			"xmas_snow_goggles", "xmas_stocking", "xmas_bauble", "xmas_antlers", "xmas_peppermints",
			"xmas_ice_crown", "xmas_frost_wings", "xmas_festive_cape", "xmas_north_star", "xmas_scarf",
			"xmas_cookies", "xmas_elf_hat", "xmas_snow_globe", "xmas_star_aura"};
		for (int i = 0; i < days.length; i++) advent(days[i], i + 1);
	}

	// ---------------------------------------------------------------- hats

	/** Red and fluffy, the tip flopping back with a white pom-pom. */
	static void santaHat() {
		hat("xmas_santa_hat", "Santa Hat", RED, false, VoxelModel.shape(1f, -5.5f, -18.5f, -5.5f, palette(
				'r', Paint.of(RED), 'R', Paint.of(RED_DARK), 'w', Paint.of(WHITE), 'W', Paint.of(SNOW)),
			11, 11, 11, (x, y, z) -> {
				double px = x + 0.5 - 5.5, pz = z + 0.5 - 5.5;
				if (y >= 9) return px * px + pz * pz <= 5.6 * 5.6 ? (y == 10 || (x + z) % 3 != 0 ? 'w' : 'W') : '.';
				double h = (8 - y) / 8.0;
				double tipZ = h * h * 4.2;
				// The pom-pom on the tip.
				if (y <= 1 && Math.hypot(px, pz - 4.2) <= 1.6) return 'w';
				double r = 4.6 * (1 - h) + 0.7;
				double dz = pz - tipZ;
				if (px * px + dz * dz > r * r) return '.';
				return px < -r * 0.35 ? 'R' : 'r';
			}));
	}

	/** A ring of ice snowflake arms, slowly turning above the head. */
	static void snowflakeHalo() {
		VoxelModel flake = VoxelModel.shape(1f, -6.5f, 0, -6.5f, palette('I', Paint.glow(ICE), 'i', Paint.glow(0xFFEAF8FF), 'd', Paint.glow(ICE_DEEP)),
			13, 1, 13, (x, y, z) -> {
				double px = x - 6, pz = z - 6, r = Math.hypot(px, pz);
				if (r > 6.3) return '.';
				if (r < 1.1) return 'i';
				for (int k = 0; k < 6; k++) {
					double a = Math.PI / 3 * k, ax = Math.cos(a), az = Math.sin(a);
					double along = px * ax + pz * az, across = Math.abs(-px * az + pz * ax);
					if (along > 0 && across < 0.6) return r > 5 ? 'i' : 'I';
					// Little side branches two thirds of the way out.
					if (along > 2.8 && along < 4.6 && Math.abs(across - (along - 2.8)) < 0.55) return 'd';
				}
				return '.';
			});
		add("xmas_snowflake_halo", "Snowflake Halo", CosmeticSlot.HAT, ICE, false, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.pose().translate(0, (-12.5f + (float) Math.sin(ctx.time() * 2) * 0.6f) / 16f, 0);
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 20 % 360)));
			ctx.draw(flake);
			ctx.pose().popPose();
		});
		icon("xmas_snowflake_halo", flake, false);
		lift("xmas_snowflake_halo", 5.4f);
	}

	/** Holly leaves woven into a band, red berries between them. */
	static void hollyCrown() {
		hat("xmas_holly_crown", "Holly Crown", GREEN, false, VoxelModel.shape(1f, -5f, -11f, -5f, palette(
				'g', Paint.of(GREEN), 'G', Paint.of(GREEN_DARK), 'b', Paint.glow(0xFFE5303A), 'B', Paint.of(0xFF9E1820)),
			10, 3, 10, (x, y, z) -> {
				boolean edge = x == 0 || x == 9 || z == 0 || z == 9;
				if (!edge) return '.';
				int around = (x + z * 3) % 5;
				if (y == 0) return around == 0 ? 'b' : around == 2 ? 'g' : '.';
				if (y == 1) return around == 0 ? 'B' : 'g';
				return (x + z) % 2 == 0 ? 'G' : 'g';
			}));
	}

	/** A little Christmas tree on your head, baubles and a gold star on top. */
	static void treeTopper() {
		hat("xmas_tree_topper", "Tree Topper", GREEN, false, VoxelModel.shape(1f, -4.5f, -21f, -4.5f, palette(
				'g', Paint.of(GREEN), 'G', Paint.of(GREEN_DARK), 'S', Paint.glow(GOLD), 'r', Paint.glow(0xFFE5303A),
				'b', Paint.glow(0xFF4FA8FF), 'y', Paint.glow(0xFFFFE066), 't', Paint.of(BROWN_DARK)),
			9, 13, 9, (x, y, z) -> {
				double px = x - 4, pz = z - 4;
				if (y <= 1) return (px == 0 && pz == 0) || (y == 1 && Math.abs(px) + Math.abs(pz) == 1 && pz == 0) ? 'S' : '.';
				if (y == 12) return Math.abs(px) <= 1 && Math.abs(pz) <= 1 ? 't' : '.';
				// Three tiers, each wider at its bottom.
				int tier = (y - 2) / 3, step = (y - 2) % 3;
				double r = 1.2 + tier * 1.1 + step * 0.9;
				if (px * px + pz * pz > r * r) return '.';
				boolean outside = px * px + pz * pz > (r - 1.2) * (r - 1.2);
				int k = Math.floorMod(x * 5 + y * 7 + z * 3, 11);
				if (outside && k == 0) return 'r';
				if (outside && k == 4) return 'b';
				if (outside && k == 8) return 'y';
				return px < -1 ? 'G' : 'g';
			}));
	}

	/** Branching reindeer antlers on either side of the head. */
	static void antlers() {
		// One antler drawn by hand (left side, top row first); the right one is its mirror image.
		String[] left = {
			"#...#...",
			"#...#.#.",
			"##..#.#.",
			".#.####.",
			".####...",
			"...##...",
			"...##...",
			"....##..",
			".....##.",
			"......##"};
		VoxelModel antler = VoxelModel.shape(1f, -9f, -18f, -1f, palette('a', Paint.of(0xFF9A6A3C), 'A', Paint.of(0xFF6E4826), 'f', Paint.of(0xFF5A3A1E)),
			18, 10, 2, (x, y, z) -> {
				int lx = x < 9 ? x - 1 : 16 - x;
				if (lx < 0 || lx >= 8 || left[y].charAt(lx) != '#') return '.';
				return y >= 8 ? 'f' : z == 1 ? 'A' : 'a';
			});
		add("xmas_antlers", "Reindeer Antlers", CosmeticSlot.HAT, 0xFF9A6A3C, false, ctx -> {
			ctx.attach(ctx.model().head);
			// A gentle wobble, as if they were a little too big.
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees((float) Math.sin(ctx.time() * 2.4) * 1.5f));
			ctx.draw(antler);
			ctx.pose().popPose();
		});
		icon("xmas_antlers", antler, false);
		lift("xmas_antlers", 9.5f);
	}

	/** A crown of ice spikes, glowing faintly. */
	static void iceCrown() {
		hat("xmas_ice_crown", "Ice Crown", ICE, false, VoxelModel.shape(1f, -5f, -15f, -5f, palette(
				'I', Paint.glow(ICE), 'i', Paint.glow(0xFFEAF8FF), 'd', Paint.of(ICE_DEEP), 'D', Paint.of(0xFF3F84B8)),
			10, 7, 10, (x, y, z) -> {
				boolean edge = x == 0 || x == 9 || z == 0 || z == 9;
				if (!edge) return '.';
				if (y >= 5) return y == 6 ? 'D' : 'd';
				// Spikes of different heights all the way round.
				int around = x == 0 ? z : z == 9 ? 9 + x : x == 9 ? 27 - z : 36 - x;
				int height = new int[]{5, 2, 3, 1, 3, 2}[around % 6];
				if (y < height) return '.';
				return y == height ? 'i' : 'I';
			}));
	}

	/** A golden star that floats above you and turns, with a faint ring of light. */
	static void northStar() {
		VoxelModel star = VoxelModel.shape(1f, -5.5f, -5.5f, -1f, palette('S', Paint.glow(GOLD), 's', Paint.glow(0xFFFFF4C2), 'o', Paint.of(GOLD_DARK)),
			11, 11, 2, (x, y, z) -> {
				double px = x - 5, py = y - 5;
				double a = Math.atan2(py, px) + Math.PI / 2, r = Math.hypot(px, py);
				// A five-pointed star: the radius swings between the points and the dips.
				double edge = 2.4 + 3.1 * Math.pow(Math.abs(Math.cos(a * 2.5)), 3);
				if (r > edge) return '.';
				if (r < 1.3) return 's';
				return r > edge - 0.9 ? 'o' : 'S';
			});
		VoxelModel ring = VoxelModel.shape(0.5f, -5.25f, 0, -5.25f, palette('h', Paint.glow(0xFFFFE89A)),
			21, 1, 21, (x, y, z) -> ring(x, z, 10, 9, 10.4) && (x + z) % 3 != 0 ? 'h' : '.');
		add("xmas_north_star", "North Star", CosmeticSlot.HAT, GOLD, false, ctx -> {
			ctx.attach(ctx.model().head);
			ctx.pose().translate(0, (-15f + (float) Math.sin(ctx.time() * 1.8) * 0.8f) / 16f, 0);
			ctx.pose().pushPose();
			ctx.pose().translate(0, 4.5f / 16f, 0);
			ctx.draw(ring);
			ctx.pose().popPose();
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 45 % 360)));
			ctx.draw(star);
			ctx.pose().popPose();
		});
		icon("xmas_north_star", star, false);
		lift("xmas_north_star", 12.5f);
	}

	/** Pointy and green, folding over to one side, with a striped band and a bell on the tip. */
	static void elfHat() {
		hat("xmas_elf_hat", "Elf Hat", GREEN, false, VoxelModel.shape(1f, -5.5f, -19.5f, -5.5f, palette(
				'g', Paint.of(GREEN), 'G', Paint.of(GREEN_DARK), 'r', Paint.of(RED), 'w', Paint.of(WHITE), 'y', Paint.glow(GOLD)),
			11, 12, 11, (x, y, z) -> {
				double px = x + 0.5 - 5.5, pz = z + 0.5 - 5.5;
				if (y >= 10) return px * px + pz * pz <= 5.4 * 5.4 ? ((x + z + y) % 2 == 0 ? 'r' : 'w') : '.';
				double h = (9 - y) / 9.0;
				double tipX = h * h * 4.8;
				if (y <= 1 && Math.hypot(px - 4.8, pz) <= 1.2) return 'y';
				double r = 4.8 * (1 - h) + 0.5;
				double dx = px - tipX;
				if (dx * dx + pz * pz > r * r) return '.';
				return pz > r * 0.35 ? 'G' : 'g';
			}));
	}

	// ---------------------------------------------------------------- face

	/** A shiny red nose that glows. */
	static void reindeerNose() {
		worn("xmas_reindeer_nose", "Reindeer Nose", CosmeticSlot.FACE, RED, false, VoxelModel.build(1f, -1f, -3.5f, -5.4f, palette(
				'n', Paint.glow(0xFFFF3B3B), 'h', Paint.glow(0xFFFFB0B0)),
			"hn/nn", "nn/nn"));
	}

	/** Ski goggles with icy blue lenses on a red strap. */
	static void snowGoggles() {
		worn("xmas_snow_goggles", "Snow Goggles", CosmeticSlot.FACE, RED, false, VoxelModel.shape(1f, -5f, -6.5f, -5f, palette(
				's', Paint.of(RED), 'S', Paint.of(RED_DARK), 'f', Paint.of(0xFFF4F6FA), 'l', Paint.glow(0xFF8FD4FF), 'L', Paint.glow(0xFFD8F2FF)),
			10, 3, 10, (x, y, z) -> {
				boolean edge = x == 0 || x == 9 || z == 0 || z == 9;
				if (!edge) return '.';
				if (z == 0 && x >= 1 && x <= 8) {
					boolean lens = y == 1 && (x == 2 || x == 3 || x == 6 || x == 7);
					if (lens) return x == 2 || x == 6 ? 'L' : 'l';
					return 'f';
				}
				return y == 1 ? (x % 2 == 0 ? 's' : 'S') : '.';
			}));
	}

	/** A knitted scarf in red, white and green stripes, one end hanging down the front. */
	static void scarf() {
		worn("xmas_scarf", "Knitted Scarf", CosmeticSlot.FACE, RED, false, VoxelModel.shape(1f, -5f, -0.5f, -5f, palette(
				'r', Paint.of(RED), 'w', Paint.of(WHITE), 'g', Paint.of(GREEN), 'R', Paint.of(RED_DARK)),
			10, 7, 10, (x, y, z) -> {
				boolean edge = x == 0 || x == 9 || z == 0 || z == 9;
				if (y <= 1) return edge ? (y == 0 ? 'r' : 'R') : '.';
				// The hanging end at the front, a little to the right, striped.
				if (z == 0 && (x == 6 || x == 7)) return new char[]{'r', 'w', 'g', 'w', 'r'}[(y - 2) % 5];
				return '.';
			}));
	}

	// ---------------------------------------------------------------- back

	/** A big striped candy cane worn across the back. */
	static void candyCane() {
		VoxelModel cane = VoxelModel.shape(1f, -6f, -1f, 2.2f, palette('r', Paint.of(RED), 'w', Paint.of(WHITE)),
			12, 15, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5;
				boolean stick = seg(px, py, 3, 14, 8.5, 4) < 0.95;
				// The hook: an arc over the top.
				double hx = px - 6.5, hy = py - 4;
				double r = Math.hypot(hx, hy);
				boolean hook = r > 1.6 && r < 3.4 && hy <= 0.5 && !(hx > 1.5 && hy > -1);
				if (!stick && !hook) return '.';
				// Diagonal stripes.
				return Math.floorMod((int) Math.floor((px + py) / 1.6), 2) == 0 ? 'r' : 'w';
			});
		add("xmas_candy_cane", "Candy Cane", CosmeticSlot.BACK, RED, false, ctx -> {
			ctx.attach(ctx.model().body);
			ctx.draw(cane);
			ctx.pose().popPose();
		});
		icon("xmas_candy_cane", cane, false);
		still("xmas_candy_cane");
	}

	/** A wrapped present worn like a backpack: ribbon cross and a bow on top. */
	static void giftPack() {
		VoxelModel gift = VoxelModel.shape(1f, -3.5f, -1.5f, 2f, palette(
				'b', Paint.of(0xFF2F6FD6), 'B', Paint.of(0xFF2154A8), 'y', Paint.of(GOLD), 'Y', Paint.of(GOLD_DARK)),
			7, 11, 3, (x, y, z) -> {
				if (y <= 1) {
					// The bow: two loops either side of a knot.
					if (z != 1) return '.';
					if (x == 3) return 'Y';
					return (y == 0 && (x == 1 || x == 5)) || (y == 1 && x >= 1 && x <= 5) ? 'y' : '.';
				}
				if (x == 3 || y == 6) return 'y';
				return (x == 0 || x == 6 || y == 10) ? 'B' : 'b';
			});
		add("xmas_gift_pack", "Gift Backpack", CosmeticSlot.BACK, 0xFF2F6FD6, false, ctx -> {
			ctx.attach(ctx.model().body);
			ctx.draw(gift);
			ctx.pose().popPose();
		});
		icon("xmas_gift_pack", gift, false);
		still("xmas_gift_pack");
	}

	/** An evergreen wreath on your back, with berries and a red bow. */
	static void wreath() {
		VoxelModel wreath = VoxelModel.shape(1f, -5.5f, 0.5f, 2.3f, palette(
				'g', Paint.of(GREEN), 'G', Paint.of(GREEN_DARK), 'b', Paint.glow(0xFFE5303A), 'r', Paint.of(RED), 'R', Paint.of(RED_DARK)),
			11, 12, 1, (x, y, z) -> {
				// The bow at the bottom.
				if (y >= 9) {
					if (x == 5) return y == 9 ? 'R' : 'r';
					return y == 10 && (x == 3 || x == 4 || x == 6 || x == 7) ? 'r' : y == 11 && (x == 4 || x == 6) ? 'R' : '.';
				}
				if (!ring(x, y, 5, 2.4, 5.3)) return '.';
				int k = Math.floorMod(x * 7 + y * 5, 9);
				if (k == 0) return 'b';
				return (x + y) % 2 == 0 ? 'g' : 'G';
			});
		add("xmas_wreath", "Wreath", CosmeticSlot.BACK, GREEN, false, ctx -> {
			ctx.attach(ctx.model().body);
			ctx.draw(wreath);
			ctx.pose().popPose();
		});
		icon("xmas_wreath", wreath, false);
		still("xmas_wreath");
	}

	/** A Christmas stocking hung on your back, presents peeking out. */
	static void stocking() {
		VoxelModel sock = VoxelModel.shape(1f, -3.5f, -0.5f, 2.2f, palette(
				'w', Paint.of(WHITE), 'W', Paint.of(SNOW), 'r', Paint.of(RED), 'R', Paint.of(RED_DARK), 'g', Paint.of(GREEN),
				'y', Paint.glow(GOLD), 'b', Paint.of(0xFF2F6FD6)),
			8, 13, 1, (x, y, z) -> {
				// Presents peeking out of the top.
				if (y == 0) return x == 1 || x == 2 ? 'b' : x == 4 ? 'y' : '.';
				if (y == 1) return x >= 1 && x <= 4 ? (x == 3 ? 'g' : x == 4 ? 'y' : 'b') : '.';
				if (y <= 3) return x <= 5 ? (y == 2 ? 'w' : 'W') : '.';
				if (y <= 9) return x <= 4 ? (x == 0 || y % 3 == 0 ? 'R' : 'r') : '.';
				// The foot, turned to the right.
				return x <= 7 && !(y == 12 && (x == 0 || x == 7)) ? (x >= 5 && y >= 11 ? 'w' : 'r') : '.';
			});
		add("xmas_stocking", "Stocking", CosmeticSlot.BACK, RED, false, ctx -> {
			ctx.attach(ctx.model().body);
			// Swings a little as you walk.
			float swing = (float) Math.sin(ctx.time() * 3) * ctx.state().walkAnimationSpeed * 10f;
			ctx.pose().translate(0, 0.5f / 16f, 0);
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees(swing));
			ctx.draw(sock);
			ctx.pose().popPose();
		});
		icon("xmas_stocking", sock, false);
	}

	// ---------------------------------------------------------------- wings

	/** Wings of ice shards that catch the light. */
	static void frostWings() {
		VoxelModel wing = VoxelModel.shape(0.6f, 0, -8f, 0, palette(
				'I', Paint.glow(ICE), 'i', Paint.glow(0xFFEAF8FF), 'd', Paint.of(ICE_DEEP), 'D', Paint.of(0xFF3F84B8)),
			30, 32, 1, (x, y, z) -> {
				double px = x + 0.5, py = y + 0.5;
				// Six long shards fanning out from the shoulder at (1, 8).
				for (int k = 0; k < 6; k++) {
					double a = Math.toRadians(-38 + k * 26);
					double len = 27 - Math.abs(k - 1.5) * 3.2;
					double tx = 1 + Math.cos(a) * len, ty = 8 + Math.sin(a) * len;
					double width = 2.3 - k * 0.15;
					// Each shard: wide near the shoulder, tapering to a point.
					double dx = tx - 1, dy = ty - 8, l2 = dx * dx + dy * dy;
					double t = Math.max(0, Math.min(1, ((px - 1) * dx + (py - 8) * dy) / l2));
					double dist = Math.hypot(px - 1 - t * dx, py - 8 - t * dy);
					double w = width * (1 - t) + 0.25;
					if (dist < w) {
						if (dist > w - 0.6) return 'D';
						return t > 0.85 ? 'i' : (k % 2 == 0 ? 'I' : 'd');
					}
				}
				return '.';
			});
		wingPair("xmas_frost_wings", "Frost Wings", ICE, false, wing, 0.7, 0.9);
	}

	// ---------------------------------------------------------------- pets

	/** A gingerbread man with icing and gumdrop buttons, waving now and then. */
	static void gingerbread() {
		java.util.Map<Character, Paint> paint = palette('k', Paint.of(BROWN), 'K', Paint.of(BROWN_DARK), 'w', Paint.of(WHITE),
			'e', Paint.of(0xFF2A1A10), 'r', Paint.glow(0xFFE5303A), 'g', Paint.glow(0xFF4CC86A));
		String[] body = {
			"...kkk...", "..kkkkk..", "..kekek..", "..kkkkk..", "..kwwwk..", "...kkk...",
			"kkkkkkkkk", "wkkkrkkkw", ".kkkkkkk.", "...kgk...", "...kkk...", "..kk.kk..", ".wkk.kkw.", ".kk...kk."};
		VoxelModel front = sprite(0.5f, paint, body);
		add("xmas_gingerbread", "Gingerbread Buddy", CosmeticSlot.PET, BROWN, false, ctx -> pet(ctx, () -> {
			PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees((float) Math.sin(ctx.time() * 3) * 6f));
			ctx.draw(front);
		}));
		icon("xmas_gingerbread", front, false);
	}

	/** A snowman: three snowballs, coal eyes, a carrot nose and a little top hat. */
	static void snowman() {
		VoxelModel man = VoxelModel.shape(0.5f, -2.5f, -4.5f, -2.5f, palette(
				'w', Paint.of(WHITE), 'W', Paint.of(SNOW), 'k', Paint.of(0xFF15151A), 'o', Paint.of(0xFFF08A24), 'r', Paint.of(RED)),
			10, 19, 10, (x, y, z) -> {
				double px = x - 4.5, pz = z - 4.5;
				// The hat.
				if (y <= 2) return Math.abs(px) <= (y == 2 ? 3 : 1.6) && Math.abs(pz) <= (y == 2 ? 3 : 1.6) ? (y == 1 ? 'r' : 'k') : '.';
				double[][] balls = {{6, 2.6}, {10.5, 3.2}, {15.5, 3.9}};
				for (double[] b : balls) {
					double d = Math.sqrt(px * px + (y - b[0]) * (y - b[0]) + pz * pz);
					if (d <= b[1]) {
						if (b[0] == 6 && z == 2 && (y == 5) && (x == 3 || x == 6)) return 'k';
						if (b[0] == 6 && y == 6 && x >= 4 && x <= 5 && z <= 2) return 'o';
						if (b[0] != 6 && z <= 1 && Math.abs(px) < 0.6 && (y == 9 || y == 11 || y == 15)) return 'k';
						return d > b[1] - 0.8 && px < 0 ? 'W' : 'w';
					}
				}
				// The carrot sticks out past the face.
				if (y == 6 && (x == 4 || x == 5) && z == 1) return 'o';
				return '.';
			});
		add("xmas_snowman", "Mini Snowman", CosmeticSlot.PET, WHITE, false, ctx -> pet(ctx, () -> ctx.draw(man)));
		icon("xmas_snowman", man, false);
	}

	/** A violet bauble with a gold band, turning slowly. */
	static void bauble() {
		VoxelModel ball = VoxelModel.shape(0.5f, -3f, -3.5f, -3f, palette(
				'v', Paint.of(0xFF8A5CE0), 'V', Paint.of(0xFF5F3CA8), 'h', Paint.glow(0xFFE8DCFF), 'y', Paint.glow(GOLD), 'Y', Paint.of(GOLD_DARK)),
			12, 14, 12, (x, y, z) -> {
				double px = x - 5.5, pz = z - 5.5;
				if (y <= 1) return Math.abs(px) <= 1 && Math.abs(pz) <= 1 ? (y == 0 ? 'Y' : 'y') : '.';
				double py = y - 8;
				double d = Math.sqrt(px * px + py * py + pz * pz);
				if (d > 5.8) return '.';
				if (Math.abs(py) < 0.8) return 'y';
				if (px < -2 && py < -2 && pz < -1) return 'h';
				return px > 1.5 ? 'V' : 'v';
			});
		add("xmas_bauble", "Bauble", CosmeticSlot.PET, 0xFF8A5CE0, false, ctx -> pet(ctx, () -> {
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 60 % 360)));
			ctx.draw(ball);
		}));
		icon("xmas_bauble", ball, false);
	}

	/** A snow globe with a tiny tree inside, its snow drifting round and round. */
	static void snowGlobe() {
		VoxelModel globe = VoxelModel.shape(0.5f, -3f, -3.5f, -3f, palette(
				'c', Paint.glow(0xFFCFEFFF), 'g', Paint.of(GREEN), 'y', Paint.glow(GOLD), 'k', Paint.of(BROWN_DARK), 'K', Paint.of(BROWN), 'w', Paint.of(WHITE)),
			12, 14, 12, (x, y, z) -> {
				double px = x - 5.5, pz = z - 5.5, py = y - 6;
				if (y >= 12) return Math.abs(px) <= 4.5 && Math.abs(pz) <= 4.5 ? (y == 13 ? 'k' : 'K') : '.';
				double d = Math.sqrt(px * px + py * py + pz * pz);
				if (d > 5.8) return '.';
				// The glass: a sparse shell you can see through.
				if (d > 5.2) return (x + y + z) % 4 == 0 && (x + z) % 2 == 0 ? 'c' : '.';
				if (d > 5) return '.';
				// Snow on the ground inside, and the tree.
				if (y == 11) return 'w';
				double r = (y - 3) * 0.55;
				if (y >= 4 && y <= 10 && px * px + pz * pz <= r * r) return 'g';
				return y == 3 && Math.abs(px) < 1 && Math.abs(pz) < 1 ? 'y' : '.';
			});
		add("xmas_snow_globe", "Snow Globe", CosmeticSlot.PET, 0xFFCFEFFF, false, ctx -> pet(ctx, () -> {
			PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 30 % 360)));
			ctx.draw(globe);
		}));
		icon("xmas_snow_globe", globe, false);
	}

	// ---------------------------------------------------------------- auras

	/** Peppermint sweets circling your waist. */
	static void peppermints() {
		VoxelModel mint = VoxelModel.shape(0.5f, -2.25f, -2.25f, -0.25f, palette('r', Paint.of(RED), 'w', Paint.of(WHITE), 'R', Paint.of(RED_DARK)),
			9, 9, 1, (x, y, z) -> {
				double px = x - 4, py = y - 4, r = Math.hypot(px, py);
				if (r > 4.5) return '.';
				if (r > 3.9) return 'R';
				// Six swirled stripes: the angle bends further the further out it is.
				double a = Math.atan2(py, px) + r * 0.55;
				return Math.floorMod((int) Math.floor(a / (Math.PI / 3)), 2) == 0 ? 'r' : 'w';
			});
		add("xmas_peppermints", "Peppermint Orbit", CosmeticSlot.AURA, RED, false, ctx -> {
			ctx.attach(ctx.model().body);
			for (int i = 0; i < 6; i++) {
				double a = ctx.time() * 1.1 + i * Math.PI / 3;
				ctx.pose().pushPose();
				at(ctx.pose(), Math.cos(a) * 11, 8 + Math.sin(ctx.time() * 2 + i) * 1.2, Math.sin(a) * 11);
				PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) Math.toDegrees(-a) + 90));
				PoseUtil.mulPose(ctx.pose(), Axis.ZP.rotationDegrees((float) (ctx.time() * 140 % 360)));
				ctx.draw(mint);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});
		icon("xmas_peppermints", mint, false);
	}

	/** Cookies floating up around you and fading away. */
	static void cookies() {
		VoxelModel cookie = sprite(0.5f, palette('k', Paint.of(BROWN), 'K', Paint.of(BROWN_DARK), 'c', Paint.of(0xFF3A2414)),
			"..kkk..", ".kkckkk", "kckkkKk", "kkkkckk", "kKkckkk", ".kkkkK.", "..kkk..");
		add("xmas_cookies", "Cookie Crumbs", CosmeticSlot.AURA, BROWN, false, ctx -> motes(ctx, 9, 0.22, (pose, life, a, b, c, i) -> {
			double ang = a * Math.PI * 2, r = 7 + b * 7;
			at(pose, Math.cos(ang) * r, 14 - life * 26, Math.sin(ang) * r);
			PoseUtil.mulPose(pose, Axis.YP.rotationDegrees((float) (life * 200 + c * 360)));
			float s = fade(life, 0.15) * (0.8f + (float) c * 0.4f);
			pose.scale(s, s, s);
			return cookie;
		}));
		icon("xmas_cookies", cookie, false);
	}

	/** Golden stars circling you, twinkling, for Christmas Eve. */
	static void starAura() {
		VoxelModel star = VoxelModel.shape(0.5f, -2.75f, -2.75f, -0.25f, palette('S', Paint.glow(GOLD), 's', Paint.glow(0xFFFFF4C2)),
			11, 11, 1, (x, y, z) -> {
				double px = x - 5, py = y - 5;
				double a = Math.atan2(py, px) + Math.PI / 2, r = Math.hypot(px, py);
				double edge = 2.2 + 3.2 * Math.pow(Math.abs(Math.cos(a * 2.5)), 3);
				if (r > edge) return '.';
				return r < 1.6 ? 's' : 'S';
			});
		add("xmas_star_aura", "Christmas Star Aura", CosmeticSlot.AURA, GOLD, false, ctx -> {
			ctx.attach(ctx.model().body);
			for (int i = 0; i < 8; i++) {
				double a = ctx.time() * 0.7 + i * Math.PI / 4;
				double h = 2 + (i % 4) * 3.5 + Math.sin(ctx.time() * 1.6 + i) * 1.2;
				float twinkle = 0.75f + 0.35f * (float) Math.max(0, Math.sin(ctx.time() * 4 + i * 1.7));
				ctx.pose().pushPose();
				at(ctx.pose(), Math.cos(a) * 12, h, Math.sin(a) * 12);
				PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (ctx.time() * 90 + i * 45) % 360));
				ctx.pose().scale(twinkle, twinkle, twinkle);
				ctx.draw(star);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});
		icon("xmas_star_aura", star, false);
	}
}
