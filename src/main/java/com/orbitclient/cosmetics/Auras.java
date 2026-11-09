package com.orbitclient.cosmetics;

import com.orbitclient.util.PoseUtil;
import com.orbitclient.cosmetics.VoxelModel.Paint;
import com.mojang.math.Axis;

import static com.orbitclient.cosmetics.Cosmetics.*;

/** Auras: things floating around the player. From the neck, the head top is at y = -8 and the feet at y = 24. */
final class Auras {
	private Auras() {
	}

	static void register() {
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
		icon("orbit", orb, false);

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
		icon("rune_ring", runeRing, false);

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
		icon("sparkles", star, false);

		add("trail", "Stardust Trail", CosmeticSlot.AURA, 0xFF9B7CFF, true, ctx -> {
			// Drawn as particles while moving (see CosmeticManager.tickTrails).
		});
		icon("trail", star, false);

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
		icon("prism_orbit", crystal, false);

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
		icon("phoenix_crown", flameHalo, false);

		// Snowflakes drifting down around you, turning as they fall.
		VoxelModel flake = sprite(0.45f, palette('#', Paint.tint(1f), 'w', Paint.glow(0xFFF4FAFF)),
			"#.#.#", ".###.", "##w##", ".###.", "#.#.#");
		add("snowfall", "Snowfall", CosmeticSlot.AURA, 0xFFBFE6FF, true, ctx -> motes(ctx, 14, 0.25, (pose, life, a, b, c, i) -> {
			double ang = a * Math.PI * 2, r = 6 + b * 8;
			at(pose, Math.cos(ang) * r + Math.sin(life * 9 + i) * 1.5, -14 + life * 38, Math.sin(ang) * r);
			PoseUtil.mulPose(pose, Axis.YP.rotationDegrees((float) (life * 400 + c * 360)));
			float s = fade(life, 0.1) * (0.7f + (float) c * 0.5f);
			pose.scale(s, s, s);
			return flake;
		}));
		icon("snowfall", flake, false);

		// Hearts rising around you, pulsing gently.
		VoxelModel heart = sprite(0.5f, palette('#', Paint.tintGlow(1f), '%', Paint.tintGlow(0.75f), 'w', Paint.glow(0xFFFFFFFF)),
			".##.##.", "#w#####", "#######", ".#####.", "..%%%..", "...%...");
		add("heart_float", "Floating Hearts", CosmeticSlot.AURA, 0xFFFF4D7A, true, ctx -> motes(ctx, 8, 0.3, (pose, life, a, b, c, i) -> {
			double ang = a * Math.PI * 2, r = 7 + b * 5;
			at(pose, Math.cos(ang) * r + Math.sin(life * 6 + i) * 1.2, 16 - life * 28, Math.sin(ang) * r);
			PoseUtil.mulPose(pose, Axis.YP.rotationDegrees((float) (-Math.toDegrees(ang) + 90)));
			float s = fade(life, 0.15) * (0.8f + 0.12f * (float) Math.sin(ctx.time() * 7 + i));
			pose.scale(s, s, s);
			return heart;
		}));
		icon("heart_float", heart, false);

		// Music notes circling your head, bouncing to a beat.
		var notePaint = palette('#', Paint.tintGlow(1f));
		VoxelModel note = sprite(0.5f, notePaint, "..##..", "..#.#.", "..#..#", "..#...", "..#...", "..#...", ".##...", "###...", ".#....");
		VoxelModel notes = sprite(0.5f, notePaint, "..######", "..######", "..#....#", "..#....#", "..#....#", ".##...##", "###..###", ".#....#.");
		add("music_notes", "Music Notes", CosmeticSlot.AURA, 0xFF7C6CFF, true, ctx -> {
			ctx.pose().pushPose();
			ctx.model().root().translateAndRotate(ctx.pose());
			double t = ctx.time();
			float beat = 1 + 0.18f * (float) Math.max(0, Math.sin(t * 6.3));
			for (int i = 0; i < 4; i++) {
				double a = t * 0.8 + i * Math.PI / 2;
				ctx.pose().pushPose();
				at(ctx.pose(), Math.cos(a) * 10, -5 + Math.sin(t * 2 + i * 1.7) * 2.5, Math.sin(a) * 10);
				PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) (-Math.toDegrees(a) + 90)));
				ctx.pose().scale(beat, beat, beat);
				ctx.draw(i % 2 == 0 ? note : notes);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});
		icon("music_notes", notes, false);

		// Autumn leaves fluttering down.
		String[] leafRows = {"..#..", ".###.", "##%##", "#%#%#", ".#%#.", "..s.."};
		VoxelModel[] autumn = {
			sprite(0.5f, palette('#', Paint.of(0xFFE8742C), '%', Paint.of(0xFFB8521C), 's', Paint.of(0xFF6E4520)), leafRows),
			sprite(0.5f, palette('#', Paint.of(0xFFD63A2A), '%', Paint.of(0xFF9E2A1E), 's', Paint.of(0xFF6E4520)), leafRows),
			sprite(0.5f, palette('#', Paint.of(0xFFF2C14E), '%', Paint.of(0xFFC4932A), 's', Paint.of(0xFF6E4520)), leafRows)};
		add("autumn_leaves", "Autumn Leaves", CosmeticSlot.AURA, 0xFFE8742C, false, ctx -> motes(ctx, 10, 0.22, (pose, life, a, b, c, i) -> {
			double ang = a * Math.PI * 2 + life * 1.5, r = 6 + b * 8;
			at(pose, Math.cos(ang) * r, -12 + life * 36 + Math.sin(life * 12) * 1.2, Math.sin(ang) * r);
			PoseUtil.mulPose(pose, Axis.YP.rotationDegrees((float) (c * 360 + life * 200)));
			PoseUtil.mulPose(pose, Axis.ZP.rotationDegrees((float) Math.sin(life * 12 + i) * 50));
			float s = fade(life, 0.1);
			pose.scale(s, s, s);
			return autumn[i % 3];
		}));
		icon("autumn_leaves", autumn[0], false);

		// Bubbles wobbling up from your feet, growing, then popping.
		VoxelModel bubble = VoxelModel.centered(0.33f, palette('#', Paint.tintGlow(0.85f), 'w', Paint.glow(0xFFFFFFFF)), 9, 9, 9, (x, y, z) -> {
			double d = Math.sqrt(Math.pow(x - 4, 2) + Math.pow(y - 4, 2) + Math.pow(z - 4, 2));
			if (d > 4.3) return '.';
			return (x == 2 || x == 3) && y == 2 ? 'w' : '#';
		});
		add("bubbles", "Bubbles", CosmeticSlot.AURA, 0xFF9BE7FF, true, ctx -> motes(ctx, 9, 0.28, (pose, life, a, b, c, i) -> {
			double ang = a * Math.PI * 2, r = 6 + b * 7;
			at(pose, Math.cos(ang) * r + Math.sin(life * 10 + i) * 0.8, 22 - life * 34, Math.sin(ang) * r);
			float s = (float) (0.5 + life * 0.7) * (life > 0.93 ? (float) ((1 - life) / 0.07) : 1) * Math.min(1, (float) life * 8);
			pose.scale(s, s, s);
			return bubble;
		}));
		icon("bubbles", bubble, false);

		// Lightning bolts crackling around you for an instant, with sparks circling between strikes.
		var boltPaint = palette('#', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFFFFF));
		VoxelModel bolt = sprite(0.5f, boltPaint, "...##", "..##.", ".##..", "#####", "..##.", ".##..", "##...", "#....", "w....");
		VoxelModel spark = VoxelModel.centered(0.5f, boltPaint, 2, 2, 2, (x, y, z) -> x == y ? 'w' : '#');
		add("storm_crackle", "Storm Crackle", CosmeticSlot.AURA, 0xFF8FB8FF, true, ctx -> {
			motes(ctx, 3, 0.7, (pose, life, a, b, c, i) -> {
				if (life > 0.16 || (int) (life * 40) % 2 == 1) return null;
				double ang = a * Math.PI * 2, r = 9 + b * 3;
				at(pose, Math.cos(ang) * r, -2 + c * 16, Math.sin(ang) * r);
				PoseUtil.mulPose(pose, Axis.YP.rotationDegrees((float) (c * 360)));
				pose.scale(1.4f, 1.4f, 1.4f);
				return bolt;
			});
			motes(ctx, 3, 0.4, (pose, life, a, b, c, i) -> {
				double ang = ctx.time() * 3 + i * Math.PI * 2 / 3;
				at(pose, Math.cos(ang) * 9, 6 + Math.sin(ctx.time() * 5 + i) * 4, Math.sin(ang) * 9);
				return spark;
			});
		});
		icon("storm_crackle", bolt, false);

		// A little rain cloud over your head.
		VoxelModel cloud = VoxelModel.centered(0.75f, palette('c', Paint.of(0xFFB9C0CC), 'C', Paint.of(0xFFE4E8EF), 'd', Paint.of(0xFF8C94A3)),
			13, 6, 9, (x, y, z) -> {
				double[][] puffs = {{3.5, 3.5, 4, 2.7}, {6.5, 2.6, 4, 3.3}, {9.5, 3.5, 4, 2.6}};
				for (double[] p : puffs) {
					double d = Math.sqrt(Math.pow(x - p[0], 2) + Math.pow(y - p[1], 2) + Math.pow(z - p[2], 2));
					if (d <= p[3] && y <= 4) return y == 4 ? 'd' : (y <= 1 || d < p[3] - 1.2 && y <= 2 ? 'C' : 'c');
				}
				return '.';
			});
		VoxelModel drop = VoxelModel.centered(0.4f, palette('#', Paint.tint(1f), 'w', Paint.tint(1.25f)), 1, 3, 1, (x, y, z) -> y == 0 ? 'w' : '#');
		add("rain_cloud", "Rain Cloud", CosmeticSlot.AURA, 0xFF6FB6FF, true, ctx -> {
			ctx.pose().pushPose();
			ctx.model().root().translateAndRotate(ctx.pose());
			ctx.pose().pushPose();
			at(ctx.pose(), 0, -17 + Math.sin(ctx.time() * 1.5) * 0.6, 0);
			ctx.draw(cloud);
			ctx.pose().popPose();
			ctx.pose().popPose();
			motes(ctx, 10, 1.3, (pose, life, a, b, c, i) -> {
				at(pose, (a - 0.5) * 8, -15 + life * 17, (b - 0.5) * 5);
				return life > 0.92 ? null : drop;
			});
		});
		icon("rain_cloud", cloud, false);

		// Purple motes spiralling in toward you and vanishing.
		var moteFace = palette('#', Paint.tintGlow(1f), 'k', Paint.of(0xFF2A0F3D));
		VoxelModel moteLight = VoxelModel.centered(0.6f, moteFace, 1, 1, 1, (x, y, z) -> '#');
		VoxelModel moteDark = VoxelModel.centered(0.6f, moteFace, 1, 1, 1, (x, y, z) -> 'k');
		add("ender_motes", "Void Motes", CosmeticSlot.AURA, 0xFFB04CFF, true, ctx -> motes(ctx, 18, 0.6, (pose, life, a, b, c, i) -> {
			double ang = a * Math.PI * 2 + life * 4, r = 14 * (1 - life) + 2;
			at(pose, Math.cos(ang) * r, (b * 26 - 8) * (1 - life) + 6 * life, Math.sin(ang) * r);
			PoseUtil.mulPose(pose, Axis.YP.rotationDegrees((float) (life * 720)));
			float s = fade(life, 0.2) * (0.8f + (float) c * 0.6f);
			pose.scale(s, s, s);
			return i % 3 == 0 ? moteDark : moteLight;
		}));
		icon("ender_motes", VoxelModel.centered(0.6f, moteFace, 5, 5, 5, (x, y, z) -> (x + y + z) % 4 == 0 ? ((x + z) % 3 == 0 ? 'k' : '#') : '.'), false);

		// Two tilted rings turning around your waist, a small moon on each.
		VoxelModel bandA = VoxelModel.shape(0.75f, -11.25f, 0, -11.25f, palette('#', Paint.tint(1f), '%', Paint.tint(0.72f)),
			30, 1, 30, (x, y, z) -> {
				double d = Math.hypot(x - 14.5, z - 14.5);
				return d >= 12 && d <= 14.6 ? (d < 13.2 ? '%' : '#') : '.';
			});
		VoxelModel bandB = VoxelModel.shape(0.75f, -9f, 0, -9f, palette('#', Paint.tint(0.85f), '%', Paint.tint(0.6f)),
			24, 1, 24, (x, y, z) -> {
				double d = Math.hypot(x - 11.5, z - 11.5);
				return d >= 10 && d <= 11.6 ? ((x + z) % 4 == 0 ? '%' : '#') : '.';
			});
		VoxelModel moon = VoxelModel.centered(0.6f, palette('m', Paint.of(0xFFDAD7E6), 'M', Paint.of(0xFFA9A5B8)), 3, 3, 3, (x, y, z) -> x == 2 && y == 2 ? 'M' : 'm');
		add("planet_rings", "Planet Rings", CosmeticSlot.AURA, 0xFFF2C14E, true, ctx -> {
			double t = ctx.time();
			ctx.pose().pushPose();
			ctx.model().root().translateAndRotate(ctx.pose());
			at(ctx.pose(), 0, 9, 0);
			for (int k = 0; k < 2; k++) {
				ctx.pose().pushPose();
				PoseUtil.mulPose(ctx.pose(), k == 0 ? Axis.XP.rotationDegrees(18) : Axis.ZP.rotationDegrees(-24));
				PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees((float) ((k == 0 ? t * 22 : -t * 30) % 360)));
				ctx.draw(k == 0 ? bandA : bandB);
				at(ctx.pose(), k == 0 ? 10 : 7.9, 0.5, 0);
				ctx.draw(moon);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});
		icon("planet_rings", bandA, false);

		// Fireflies wandering slowly around you, blinking.
		VoxelModel firefly = VoxelModel.centered(0.5f, palette('#', Paint.tintGlow(1f), 'w', Paint.glow(0xFFFFFFFF)), 2, 2, 2, (x, y, z) -> x == 0 && y == 0 && z == 0 ? 'w' : '#');
		add("fireflies", "Fireflies", CosmeticSlot.AURA, 0xFFD8FF6A, true, ctx -> {
			double t = ctx.time();
			ctx.pose().pushPose();
			ctx.model().root().translateAndRotate(ctx.pose());
			for (int i = 0; i < 8; i++) {
				double a = i * 0.785 + t * 0.25 * (i % 2 == 0 ? 1 : -1) + Math.sin(t * 0.6 + i) * 0.8;
				double r = 9 + Math.sin(t * 0.4 + i * 2.1) * 3;
				double glow = Math.max(0, Math.sin(t * 1.7 + i * 2.3));
				ctx.pose().pushPose();
				at(ctx.pose(), Math.cos(a) * r, 6 + Math.sin(t * 0.7 + i * 1.3) * 9, Math.sin(a) * r);
				ctx.draw(firefly, VoxelModel.shade(ctx.color(), (float) (0.3 + 0.7 * glow)));
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});
		icon("fireflies", firefly, false);

		// Three electron orbits, each with an electron zipping round.
		VoxelModel orbitPath = VoxelModel.shape(0.5f, -12.5f, 0, -12.5f, palette('#', Paint.tintGlow(0.55f)),
			50, 1, 50, (x, y, z) -> {
				double d = Math.hypot(x - 24.5, z - 24.5);
				return d >= 23.2 && d <= 24.6 && Math.floorMod((int) Math.round(Math.atan2(z - 24.5, x - 24.5) * 12), 3) != 0 ? '#' : '.';
			});
		add("atom_orbit", "Atom Orbit", CosmeticSlot.AURA, 0xFF55E0FF, true, ctx -> {
			double t = ctx.time();
			ctx.pose().pushPose();
			ctx.model().root().translateAndRotate(ctx.pose());
			at(ctx.pose(), 0, 6, 0);
			for (int k = 0; k < 3; k++) {
				ctx.pose().pushPose();
				PoseUtil.mulPose(ctx.pose(), Axis.YP.rotationDegrees(k * 60));
				PoseUtil.mulPose(ctx.pose(), Axis.XP.rotationDegrees(65));
				ctx.draw(orbitPath);
				double a = t * 3 + k * 2.1;
				at(ctx.pose(), Math.cos(a) * 12, 0.25, Math.sin(a) * 12);
				ctx.draw(orb);
				ctx.pose().popPose();
			}
			ctx.pose().popPose();
		});
		icon("atom_orbit", orbitPath, false);

		// Sleepy Zs floating up from your head.
		VoxelModel zee = sprite(0.5f, palette('#', Paint.tintGlow(1f)), "#####", "...#.", "..#..", ".#...", "#####");
		add("sleepy_zzz", "Sleepy Zs", CosmeticSlot.AURA, 0xFFBFD4FF, true, ctx -> motes(ctx, 3, 0.35, (pose, life, a, b, c, i) -> {
			at(pose, -3 - life * 7 + Math.sin(life * 8) * 1, -10 - life * 12, -1);
			PoseUtil.mulPose(pose, Axis.ZP.rotationDegrees((float) Math.sin(life * 6 + i) * 15));
			float s = (float) (0.5 + life * 0.8) * fade(life, 0.15);
			pose.scale(s, s, s);
			return zee;
		}));
		icon("sleepy_zzz", zee, false);
	}
}
