package com.orbitclient.cosmetics;

import com.orbitclient.cosmetics.VoxelModel.Paint;
import com.mojang.math.Axis;

import java.util.Map;

import static com.orbitclient.cosmetics.Cosmetics.*;

/** Pets float beside your shoulder (see Cosmetics.pet). Models are centered on the origin and face -z. */
final class Pets {
	private Pets() {
	}

	static void register() {
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
				ctx.pose().mulPose(Axis.XP.rotationDegrees(70));
				ctx.pose().mulPose(Axis.ZP.rotationDegrees((float) (ctx.time() * 120 % 360)));
				ctx.draw(wispRing);
				ctx.pose().popPose();
			});
		});
		icon("wisp", wisp, false);

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
		icon("cube_buddy", cubeOpen, false);

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
		icon("moss_frog", frog, false);

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
			ctx.pose().mulPose(Axis.YP.rotationDegrees((float) Math.sin(ctx.time() * 0.8) * 10));
			ctx.draw(miniDrake);
		}));
		icon("ember_drake", miniDrake, false);

		// Cat: sitting, tail curled up behind, green eyes.
		var catPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.72f), 'w', Paint.of(0xFFF7F2EA), 'e', Paint.glow(0xFF8CE06A),
			'k', Paint.of(0xFF1E1A1A), 'p', Paint.of(0xFFF2A0B4));
		VoxelModel.Shape cat = (x, y, z) -> {
			if (y <= 1 && z == 2 && (x == 1 || x == 5 || (y == 1 && (x == 2 || x == 4)))) return y == 1 && (x == 2 || x == 4) ? 'p' : '#';
			if (y >= 2 && y <= 5 && z >= 1 && z <= 4) {
				if ((x == 0 || x == 6) && (y == 2 || y == 5)) return '.';
				if (z == 1 && y == 3 && (x == 1 || x == 5)) return 'e';
				if (z == 1 && y == 4 && x == 3) return 'p';
				if (z == 1 && y >= 4 && x >= 2 && x <= 4) return 'w';
				return y == 2 && x % 2 == 1 ? '%' : '#';
			}
			if (y >= 6 && y <= 9 && x >= 1 && x <= 5 && z >= 3 && z <= 7) {
				if (z == 3 && x >= 2 && x <= 4 && y <= 7) return 'w';
				return z % 2 == 0 && (x == 1 || x == 5 || y == 6) ? '%' : '#';
			}
			if (y == 9 && z == 2 && (x == 1 || x == 2 || x == 4 || x == 5)) return 'w';
			if (x == 3 && z == 8 && y >= 4 && y <= 8) return y % 2 == 0 ? '%' : '#';
			if (x == 3 && z == 7 && y == 3) return '#';
			return '.';
		};
		pet3("pet_cat", "Pocket Cat", 0xFFF0A040, catPaint, 0.7f, 7, 10, 9, cat, 'e');

		// Fox: pointed snout, tall ears, a bushy tail with a white tip.
		var foxPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.78f), 'w', Paint.of(0xFFF7F2EA), 'k', Paint.of(0xFF1E1A1A), 'e', Paint.of(0xFF2A1A12));
		VoxelModel.Shape fox = (x, y, z) -> {
			if (y <= 2 && z == 3 && (x == 1 || x == 6 || (y >= 1 && (x == 2 || x == 5)))) return y == 0 ? 'k' : '#';
			if (y >= 3 && y <= 6 && x >= 1 && x <= 6 && z >= 2 && z <= 5) {
				if (z == 2 && y == 4 && (x == 2 || x == 5)) return 'e';
				if (y >= 5 && (x == 1 || x == 6)) return 'w';
				return '#';
			}
			if (z <= 1 && y >= 5 && y <= 6 && x >= 3 && x <= 4) return z == 0 && y == 5 ? 'k' : (y == 6 ? 'w' : '#');
			if (y >= 7 && y <= 10 && x >= 2 && x <= 5 && z >= 4 && z <= 8) {
				if (z == 4 && y <= 8 && x >= 3 && x <= 4) return 'w';
				if (y == 10 && (x == 2 || x == 5) && (z == 4 || z == 8)) return 'k';
				if (y == 10) return '.';
				return '#';
			}
			if (y >= 7 && y <= 9 && z >= 9 && z <= 11 && x >= 3 && x <= 5) return z == 11 ? 'w' : (y == 7 ? '%' : '#');
			return '.';
		};
		pet3("pet_fox", "Pocket Fox", 0xFFE8742C, foxPaint, 0.65f, 8, 11, 12, fox, 'e');

		// Axolotl: pink, wide head with feathery gills, little legs, a tail fin; it wiggles as it swims.
		var axoPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.82f), 'g', Paint.of(0xFFE0508F), 'G', Paint.of(0xFFB83A72),
			'k', Paint.of(0xFF1E1A1A), 'm', Paint.of(0xFF8A3A5A));
		VoxelModel.Shape axolotl = (x, y, z) -> {
			if (z <= 3 && y >= 1 && y <= 3 && x >= 1 && x <= 7) {
				if (z == 0 && y == 2 && (x == 2 || x == 6)) return 'k';
				if (z == 0 && y == 3 && x >= 3 && x <= 5) return 'm';
				if (z == 0 && (x == 1 || x == 7) && y == 1) return '.';
				return '#';
			}
			if (z >= 1 && z <= 3 && (x == 0 || x == 8) && y >= 0 && y <= 3) return y % 2 == 0 ? 'g' : 'G';
			if (z == 2 && (x == 1 || x == 7) && y == 0) return 'g';
			if (z >= 4 && z <= 8 && y >= 2 && y <= 3 && x >= 2 && x <= 6) return y == 2 ? '#' : '%';
			if ((z == 4 || z == 7) && y == 4 && (x == 1 || x == 7)) return '#';
			if (z >= 9 && z <= 11 && x == 4 && y >= 1 && y <= 3) return z == 11 && y != 2 ? '.' : (y == 1 ? 'g' : '#');
			return '.';
		};
		VoxelModel axoModel = VoxelModel.centered(0.6f, axoPaint, 9, 5, 12, axolotl);
		add("pet_axolotl", "Axolotl", CosmeticSlot.PET, 0xFFF7A1C4, true, ctx -> pet(ctx, () -> {
			ctx.pose().mulPose(Axis.YP.rotationDegrees((float) Math.sin(ctx.time() * 4) * 12));
			ctx.draw(axoModel);
		}));
		icon("pet_axolotl", axoModel, false);

		// Bee: fuzzy striped body, buzzing wings, antennae and a stinger.
		var beePaint = palette('y', Paint.tint(1f), 'k', Paint.of(0xFF231F20), 'w', Paint.of(0xFFE8F4FF), 'W', Paint.of(0xFFC6DCEB), 'e', Paint.glow(0xFFFFFFFF));
		VoxelModel.Shape beeBody = (x, y, z) -> {
			if (y <= 1) return (y == 0 ? (x == 1 || x == 5) && z == 1 : (x == 2 || x == 4) && z == 2) ? 'k' : '.';
			double d = Math.pow((x - 3) / 3.1, 2) + Math.pow((y - 4.5) / 2.6, 2) + Math.pow((z - 4) / 3.9, 2);
			if (z == 8 && x == 3 && y == 5) return 'k';
			if (d > 1) return '.';
			if (z == 1 && y == 4 && (x == 1 || x == 5)) return 'k';
			if (z == 1 && y == 3 && (x == 1 || x == 5)) return 'e';
			return (z == 4 || z == 6) ? 'k' : 'y';
		};
		VoxelModel.Shape beeWing = (x, y, z) -> {
			double d = Math.pow((x - 2) / 2.4, 2) + Math.pow((z - 2) / 1.8, 2);
			return d <= 1 ? (d > 0.6 ? 'W' : 'w') : '.';
		};
		VoxelModel bee = VoxelModel.centered(0.65f, beePaint, 7, 8, 9, beeBody);
		VoxelModel wing = VoxelModel.shape(0.65f, 0, 0, -1.3f, beePaint, 5, 1, 4, beeWing);
		VoxelModel beeIcon = VoxelModel.centered(0.65f, beePaint, 11, 8, 9, (x, y, z) -> {
			if (y == 2 && z >= 2 && z <= 5 && (x <= 3 || x >= 7)) {
				int wx = x <= 3 ? 3 - x : x - 7;
				return beeWing.at(wx, 0, z - 2);
			}
			return x >= 2 && x <= 8 ? beeBody.at(x - 2, y, z) : '.';
		});
		add("pet_bee", "Bumble", CosmeticSlot.PET, 0xFFF5C518, true, ctx -> pet(ctx, () -> {
			ctx.draw(bee);
			float buzz = (float) Math.sin(ctx.time() * 60) * 25;
			for (int side = -1; side <= 1; side += 2) {
				ctx.pose().pushPose();
				at(ctx.pose(), side * 1.3, -1.6, -0.2);
				ctx.pose().mulPose(Axis.ZP.rotationDegrees(side * (-15 - buzz)));
				if (side < 0) ctx.pose().scale(-1, 1, 1);
				ctx.draw(wing);
				ctx.pose().popPose();
			}
		}));
		icon("pet_bee", beeIcon, false);

		// Ghost: a glowing sheet with a wavy hem that ripples.
		var ghostPaint = palette('#', Paint.tintGlow(1f), '%', Paint.tintGlow(0.82f), 'k', Paint.of(0xFF1E1A2A), 'p', Paint.glow(0xFFFFB6CE));
		VoxelModel[] ghost = new VoxelModel[2];
		VoxelModel[] ghostBlink = new VoxelModel[2];
		for (int f = 0; f < 2; f++) {
			int frame = f;
			VoxelModel.Shape g = (x, y, z) -> {
				if (y == 0) return x >= 2 && x <= 4 && z >= 1 && z <= 4 ? '#' : '.';
				if (y == 1 && (x == 0 || x == 6)) return '.';
				if (y == 7) return (x + frame) % 2 == 0 ? '%' : '.';
				if (y == 8) return (x + frame) % 4 == 0 && z % 2 == 0 ? '%' : '.';
				if (z == 0 && (y == 3 || y == 4) && (x == 2 || x == 4)) return 'k';
				if (z == 0 && y == 5 && x == 3) return 'k';
				if (z == 0 && y == 5 && (x == 1 || x == 5)) return 'p';
				return '#';
			};
			ghost[f] = VoxelModel.centered(0.75f, ghostPaint, 7, 9, 6, g);
			ghostBlink[f] = VoxelModel.centered(0.75f, ghostPaint, 7, 9, 6, (x, y, z) -> z == 0 && y == 3 && (x == 2 || x == 4) ? '#' : g.at(x, y, z));
		}
		add("pet_ghost", "Boo Ghost", CosmeticSlot.PET, 0xFFF2F4FF, true, ctx -> pet(ctx, () -> {
			int f = (int) (ctx.time() * 3) % 2;
			ctx.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.sin(ctx.time() * 1.3) * 6));
			ctx.draw(blink(ctx, 3.5) ? ghostBlink[f] : ghost[f]);
		}));
		icon("pet_ghost", ghost[0], false);

		// Goo drop: a glossy droplet that bounces with a squash and stretch.
		var gooPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.75f), 'w', Paint.glow(0xFFFFFFFF), 'k', Paint.of(0xFF15151A));
		VoxelModel.Shape goo = (x, y, z) -> {
			double r = y < 3 ? 0.6 + y * 0.9 : 3.4 * Math.sqrt(Math.max(0, 1 - Math.pow((y - 5.2) / 3.4, 2)));
			double d = Math.hypot(x - 3.5, z - 3.5);
			if (d > r || y > 8) return '.';
			if (z <= 1 && y == 5 && (x == 2 || x == 5)) return 'k';
			if (y == 3 && x == 2 && z <= 2) return 'w';
			return y >= 7 ? '%' : '#';
		};
		VoxelModel gooModel = VoxelModel.centered(0.75f, gooPaint, 8, 9, 8, goo);
		VoxelModel gooBlink = VoxelModel.centered(0.75f, gooPaint, 8, 9, 8, (x, y, z) -> {
			char c = goo.at(x, y, z);
			return c == 'k' ? '#' : c;
		});
		add("pet_goo", "Goo Drop", CosmeticSlot.PET, 0xFF5CE08A, true, ctx -> pet(ctx, () -> {
			double p = (ctx.time() * 1.6) % 1;
			double air = Math.sin(p * Math.PI);
			float squash = p < 0.12 ? (float) Math.sin(p / 0.12 * Math.PI) * 0.22f : 0;
			at(ctx.pose(), 0, 3.4 - air * 3, 0);
			ctx.pose().scale(1 + squash, 1 - squash, 1 + squash);
			at(ctx.pose(), 0, -3.4, 0);
			ctx.draw(blink(ctx, 4.2) ? gooBlink : gooModel);
		}));
		icon("pet_goo", gooModel, false);

		// Owl: big eyes, ear tufts, a little beak; it tilts its head now and then.
		var owlPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.72f), 'b', Paint.of(0xFFE0C9A6), 'y', Paint.glow(0xFFFFD447),
			'k', Paint.of(0xFF15151A), 'o', Paint.of(0xFFF08A2C));
		VoxelModel.Shape owl = (x, y, z) -> {
			if (y == 0) return (x == 1 || x == 5) && z == 2 ? '%' : '.';
			if (y >= 1 && y <= 8 && x >= 0 && x <= 6 && z >= 1 && z <= 5) {
				if ((y == 1 || y == 8) && (x == 0 || x == 6)) return '.';
				if (z == 1) {
					if (y >= 2 && y <= 3 && (x == 1 || x == 2 || x == 4 || x == 5)) return (y == 3 && (x == 2 || x == 4)) ? 'k' : 'y';
					if (y == 4 && x == 3) return 'o';
					if (y >= 5 && x >= 2 && x <= 4) return (x + y) % 2 == 0 ? '%' : 'b';
				}
				if ((x == 0 || x == 6) && y >= 4) return '%';
				return '#';
			}
			if (y == 9 && z == 2 && (x == 2 || x == 4)) return 'o';
			return '.';
		};
		VoxelModel owlModel = VoxelModel.centered(0.75f, owlPaint, 7, 10, 6, owl);
		VoxelModel owlBlink = VoxelModel.centered(0.75f, owlPaint, 7, 10, 6, (x, y, z) -> z == 1 && y >= 2 && y <= 3 && x != 3 && x >= 1 && x <= 5 ? '%' : owl.at(x, y, z));
		add("pet_owl", "Hoot Owl", CosmeticSlot.PET, 0xFF8B6A4E, true, ctx -> pet(ctx, () -> {
			double t = ctx.time() % 6;
			float tilt = t > 4 && t < 5.5 ? (float) Math.sin((t - 4) / 1.5 * Math.PI) * 20 : 0;
			ctx.pose().mulPose(Axis.ZP.rotationDegrees(tilt));
			ctx.draw(blink(ctx, 3.1) ? owlBlink : owlModel);
		}));
		icon("pet_owl", owlModel, false);

		// Mini rocket: white body, colored nose and fins, a glowing window, a flickering flame.
		var rocketPaint = palette('w', Paint.of(0xFFF2F4F8), 'g', Paint.of(0xFFC4CAD6), '#', Paint.tint(1f), '%', Paint.tint(0.72f),
			'c', Paint.glow(0xFF7CF7FF), 'f', Paint.glow(0xFFFFB347), 'F', Paint.glow(0xFFFFF1B0));
		VoxelModel.Shape rocketShape = (x, y, z) -> {
			double d = Math.hypot(x - 2, z - 2);
			if (y <= 1) return d <= (y == 0 ? 0.5 : 1.2) ? '#' : '.';
			if (y == 2) return d <= 1.6 ? '#' : '.';
			if (y <= 8) {
				if (d <= 1.8 || (d <= 2.1 && (x == 2 || z == 2))) {
					if (z == 0 && x == 2 && (y == 4 || y == 5)) return 'c';
					if (y == 7) return 'g';
					return 'w';
				}
				if (y >= 7 && (x == 2 || z == 2) && d <= 3.1) return '%';
				return '.';
			}
			return d <= 1 ? 'g' : '.';
		};
		VoxelModel rocket = VoxelModel.centered(0.7f, rocketPaint, 5, 10, 5, rocketShape);
		VoxelModel flame = VoxelModel.shape(0.6f, -0.9f, 3.5f, -0.9f, rocketPaint, 3, 4, 3, (x, y, z) -> {
			if (y == 0) return 'F';
			if (y <= 2) return x == 1 || z == 1 ? 'f' : '.';
			return x == 1 && z == 1 ? 'f' : '.';
		});
		VoxelModel rocketIcon = VoxelModel.centered(0.7f, rocketPaint, 5, 13, 5, (x, y, z) -> {
			if (y < 10) return rocketShape.at(x, y, z);
			return x == 2 && z == 2 || y == 10 && (x == 2 || z == 2) && Math.abs(x - 2) + Math.abs(z - 2) <= 1 ? (y == 10 ? 'F' : 'f') : '.';
		});
		add("pet_rocket", "Mini Rocket", CosmeticSlot.PET, 0xFFE5484D, true, ctx -> pet(ctx, () -> {
			ctx.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.sin(ctx.time() * 1.4) * 8));
			ctx.draw(rocket);
			float flick = 0.75f + 0.35f * (float) Math.abs(Math.sin(ctx.time() * 23));
			ctx.pose().translate(0, 3.5f / 16f, 0);
			ctx.pose().scale(1, flick, 1);
			ctx.pose().translate(0, -3.5f / 16f, 0);
			ctx.draw(flame);
		}));
		icon("pet_rocket", rocketIcon, false);

		// Jellyfish: a glowing bell that pulses, trailing waving tentacles.
		var jellyPaint = palette('#', Paint.tintGlow(1f), '%', Paint.tintGlow(0.7f), 'w', Paint.glow(0xFFFFFFFF), 't', Paint.tintGlow(0.55f));
		VoxelModel bell = VoxelModel.shape(0.7f, -2.8f, -3.5f, -2.8f, jellyPaint, 8, 5, 8, Pets::jellyBell);
		VoxelModel[] tentacles = new VoxelModel[2];
		for (int f = 0; f < 2; f++) {
			int frame = f;
			tentacles[f] = VoxelModel.shape(0.5f, -3f, -0.2f, -3f, jellyPaint, 12, 10, 12, (x, y, z) -> {
				int[][] roots = {{3, 3}, {8, 3}, {3, 8}, {8, 8}, {5, 6}};
				for (int i = 0; i < roots.length; i++) {
					int sway = (int) Math.round(Math.sin(y * 0.8 + frame * Math.PI + i) * 0.9);
					if (x == roots[i][0] + sway && z == roots[i][1] && y < 10 - i % 3) return 't';
				}
				return '.';
			});
		}
		VoxelModel jellyIcon = VoxelModel.shape(0.7f, -2.8f, -3.5f, -2.8f, jellyPaint, 8, 11, 8, (x, y, z) -> {
			if (y < 5) return jellyBell(x, y, z);
			return (x == 2 || x == 5) && (z == 2 || z == 5) && y < 11 - (x + z) % 3 ? 't' : '.';
		});
		add("pet_jellyfish", "Jellyfish", CosmeticSlot.PET, 0xFFC77DFF, true, ctx -> pet(ctx, () -> {
			double p = (ctx.time() * 0.9) % 1;
			float pulse = p < 0.3 ? (float) Math.sin(p / 0.3 * Math.PI) * 0.18f : 0;
			at(ctx.pose(), 0, -pulse * 4, 0);
			ctx.pose().pushPose();
			ctx.pose().scale(1 - pulse * 0.6f, 1 + pulse, 1 - pulse * 0.6f);
			ctx.draw(bell);
			ctx.pose().popPose();
			ctx.draw(tentacles[(int) (ctx.time() * 2.5) % 2]);
		}));
		icon("pet_jellyfish", jellyIcon, false);

		// Penguin: black back, white front, orange beak and feet, and a scarf; it waddles.
		var penguinPaint = palette('k', Paint.of(0xFF1E2028), 'K', Paint.of(0xFF2E3140), 'w', Paint.of(0xFFF7F7F2), 'o', Paint.of(0xFFF39A2C),
			'#', Paint.tint(1f), '%', Paint.tint(0.72f), 'e', Paint.of(0xFF0E0E12));
		VoxelModel.Shape penguin = (x, y, z) -> {
			if (y <= 3 && x >= 1 && x <= 5 && z >= 1 && z <= 4) {
				if (y == 0 && (x == 1 || x == 5)) return '.';
				if (z == 1 && y >= 1 && y <= 3 && x >= 2 && x <= 4) {
					if (y == 2 && (x == 2 || x == 4)) return 'e';
					if (y == 3 && x == 3) return 'o';
					return 'w';
				}
				return 'k';
			}
			if (y == 4 && x >= 0 && x <= 6 && z >= 1 && z <= 5) return (x == 0 || x == 6) && (z == 1 || z == 5) ? '.' : (z == 5 && x == 2 ? '%' : '#');
			if (y == 5 && z == 5 && x == 2) return '%';
			if (y >= 5 && y <= 9 && x >= 1 && x <= 5 && z >= 1 && z <= 5) {
				if (z == 1 && x >= 2 && x <= 4) return 'w';
				if (y == 9 && (x == 1 || x == 5)) return '.';
				return 'k';
			}
			if (y >= 5 && y <= 8 && (x == 0 || x == 6) && z >= 2 && z <= 4) return 'K';
			if (y == 10 && z <= 2 && (x == 2 || x == 4)) return 'o';
			return '.';
		};
		pet3("pet_penguin", "Waddle Penguin", 0xFFE5484D, penguinPaint, 0.7f, 7, 11, 6, penguin, 'e');

		// Duckling: round fluffy chick with an orange beak.
		var duckPaint = palette('#', Paint.tint(1f), '%', Paint.tint(0.8f), 'o', Paint.of(0xFFF08A2C), 'k', Paint.of(0xFF15151A));
		VoxelModel.Shape duck = (x, y, z) -> {
			double head = Math.sqrt(Math.pow(x - 3, 2) + Math.pow(y - 2.5, 2) + Math.pow(z - 2.5, 2));
			if (head <= 2.4) {
				if (z <= 1 && y == 2 && (x == 2 || x == 4)) return 'k';
				return '#';
			}
			if (z == 0 && y == 3 && x >= 2 && x <= 4) return 'o';
			double body = Math.pow((x - 3) / 3.1, 2) + Math.pow((y - 6.5) / 2.4, 2) + Math.pow((z - 4.5) / 3.2, 2);
			if (body <= 1) return (x == 0 || x == 6) && y >= 6 ? '%' : '#';
			if (y == 9 && z == 3 && (x == 2 || x == 4)) return 'o';
			return '.';
		};
		pet3("pet_duckling", "Duckling", 0xFFFFD84A, duckPaint, 0.7f, 7, 10, 8, duck, 'k');

		// Bolt Bot: a hovering robot with a screen face, a blinking antenna and a little thruster.
		var botPaint = palette('m', Paint.of(0xFFB9C0CC), 'M', Paint.of(0xFF8C94A3), 'k', Paint.of(0xFF151923), 'e', Paint.tintGlow(1f),
			'r', Paint.glow(0xFFFF4040), 'f', Paint.glow(0xFF7CF7FF));
		VoxelModel.Shape bot = (x, y, z) -> {
			if (y == 0) return x == 3 && z == 3 ? 'r' : '.';
			if (y == 1) return x == 3 && z == 3 ? 'M' : '.';
			if (y >= 2 && y <= 7 && x >= 0 && x <= 6 && z >= 1 && z <= 5) {
				if (z == 1 && y >= 3 && y <= 6 && x >= 1 && x <= 5) {
					if (y == 4 && (x == 2 || x == 4)) return 'e';
					if (y == 6 && x >= 2 && x <= 4) return 'e';
					return 'k';
				}
				if ((x == 0 || x == 6) && y == 4 && z == 3) return 'M';
				return (y == 2 || y == 7) ? 'M' : 'm';
			}
			if (y == 8 && x >= 2 && x <= 4 && z >= 2 && z <= 4) return x == 3 && z == 3 ? 'f' : 'M';
			return '.';
		};
		VoxelModel botModel = VoxelModel.centered(0.75f, botPaint, 7, 9, 7, bot);
		VoxelModel botBlink = VoxelModel.centered(0.75f, botPaint, 7, 9, 7, (x, y, z) -> {
			char c = bot.at(x, y, z);
			if (c == 'r') return 'M';
			if (z == 1 && y == 4 && (x == 2 || x == 4)) return 'k';
			return c;
		});
		add("pet_robot", "Bolt Bot", CosmeticSlot.PET, 0xFF3FE0FF, true, ctx -> pet(ctx, () -> {
			ctx.pose().mulPose(Axis.YP.rotationDegrees((float) Math.sin(ctx.time() * 2.2) * 15));
			ctx.draw(ctx.time() % 1.6 < 0.8 ? botModel : botBlink);
		}));
		icon("pet_robot", botModel, false);
	}

	/** The jellyfish's bell: a dome with glowing spots and a frilled rim. */
	private static char jellyBell(int x, int y, int z) {
		double d = Math.hypot(x - 3.5, z - 3.5);
		double r = y == 0 ? 2 : y == 1 ? 3 : 3.9;
		if (d > r) return '.';
		if (y == 4) return d > 2.6 ? '%' : '.';
		if ((x + z) % 4 == 0 && y == 2) return 'w';
		return y >= 3 ? '%' : '#';
	}

	/**
	 * A pet from one shape that blinks: every few seconds, its eye voxels (eye) close into the color
	 * of the voxel above them.
	 */
	private static void pet3(String id, String name, int color, Map<Character, Paint> paint, float size, int nx, int ny, int nz, VoxelModel.Shape shape, char eye) {
		VoxelModel open = VoxelModel.centered(size, paint, nx, ny, nz, shape);
		VoxelModel closed = VoxelModel.centered(size, paint, nx, ny, nz, (x, y, z) -> {
			char c = shape.at(x, y, z);
			if (c != eye) return c;
			char above = y > 0 ? shape.at(x, y - 1, z) : '.';
			return above == '.' || above == eye ? '#' : above;
		});
		add(id, name, CosmeticSlot.PET, color, true, ctx -> pet(ctx, () -> ctx.draw(blink(ctx, 3.7) ? closed : open)));
		icon(id, open, false);
	}
}
