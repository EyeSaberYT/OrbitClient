import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the animated cosmetic capes. Each cape is a loop of FRAMES frames in the vanilla cape layout
 * (64x32 units) at SCALE times the resolution: the outside design at (1,1)-(11,17), the inside at
 * (12,1)-(22,17), edges around them. Every animation loops seamlessly (time t goes 0..1).
 * Usage: java -Djava.awt.headless=true tools/CosmeticGenerator.java <capes dir> [preview.png]
 */
public class CosmeticGenerator {
	static final int SCALE = 4, FRAMES = 16;
	static final int W = 64 * SCALE, H = 32 * SCALE;
	/** Design area (outside of the cape) in pixels. */
	static final int DW = 10 * SCALE, DH = 16 * SCALE;

	interface Design {
		/** ARGB of the cape's outside at (u, v) in 0..1 (v down), time t in 0..1. */
		int at(double u, double v, double t);
	}

	record Cape(String id, Design design, int lining, int edge) {
	}

	public static void main(String[] args) throws Exception {
		File dir = new File(args[0]);
		dir.mkdirs();
		Cape[] capes = {
			new Cape("eclipse", CosmeticGenerator::eclipse, 0xFF0B0D16, 0xFF1A1F38),
			new Cape("galaxy", CosmeticGenerator::galaxy, 0xFF120B24, 0xFF2A1650),
			new Cape("flame", CosmeticGenerator::flame, 0xFF1A0804, 0xFF5A1606),
			new Cape("aurora", CosmeticGenerator::aurora, 0xFF07121A, 0xFF0E3A3A),
			new Cape("ocean", CosmeticGenerator::ocean, 0xFF061826, 0xFF0D3C5E),
			new Cape("circuit", CosmeticGenerator::circuit, 0xFF06090E, 0xFF0F2A33),
			new Cape("sakura", CosmeticGenerator::sakura, 0xFF3A1A2A, 0xFFE7A6C1),
			new Cape("midnight", CosmeticGenerator::midnight, 0xFF0A0C1E, 0xFF23214F),
			new Cape("stormfront", CosmeticGenerator::stormfront, 0xFF0B0E16, 0xFF2A3346),
			new Cape("magma", CosmeticGenerator::magma, 0xFF140806, 0xFF3A160C),
			new Cape("prism_wave", CosmeticGenerator::prismWave, 0xFF120E1E, 0xFF5A3FA8),
			new Cape("firefly_forest", CosmeticGenerator::fireflyForest, 0xFF07110C, 0xFF1C3A26),
			new Cape("frostbite", CosmeticGenerator::frostbite, 0xFF0C1A26, 0xFFB9E4F5),
			new Cape("retrowave", CosmeticGenerator::retrowave, 0xFF12061E, 0xFFFF3FA4),
			new Cape("koi_pond", CosmeticGenerator::koiPond, 0xFF06202A, 0xFF1E6B6B),
			new Cape("hive", CosmeticGenerator::hive, 0xFF2A1A06, 0xFF8A5A12),
			new Cape("regal", CosmeticGenerator::regal, 0xFF1A0612, 0xFFC9963A),
			new Cape("plasma", CosmeticGenerator::plasma, 0xFF0E0618, 0xFF3A1460),
		};
		BufferedImage preview = new BufferedImage(capes.length * (DW * 3 + 12), 4 * (DH * 3 + 12), BufferedImage.TYPE_INT_ARGB);
		for (int ci = 0; ci < capes.length; ci++) {
			Cape cape = capes[ci];
			for (int f = 0; f < FRAMES; f++) {
				double t = f / (double) FRAMES;
				BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
				// Edges and top/bottom strips in the edge color, inside in the lining color.
				fill(img, 0, 0, 23 * SCALE, 17 * SCALE, cape.edge);
				for (int y = 0; y < DH; y++) {
					for (int x = 0; x < DW; x++) {
						int c = cape.design.at((x + 0.5) / DW, (y + 0.5) / DH, t) | 0xFF000000;
						img.setRGB(SCALE + x, SCALE + y, c);
						// The inside shows a darker, mirrored hint of the design over the lining.
						int inside = mix(cape.lining, c, 0.18);
						img.setRGB(12 * SCALE + (DW - 1 - x), SCALE + y, inside);
					}
				}
				ImageIO.write(img, "png", new File(dir, cape.id + "_" + f + ".png"));
				if (f % 4 == 0) {
					int px = ci * (DW * 3 + 12), py = (f / 4) * (DH * 3 + 12);
					for (int y = 0; y < DH * 3; y++) for (int x = 0; x < DW * 3; x++) preview.setRGB(px + x, py + y, img.getRGB(SCALE + x / 3, SCALE + y / 3));
				}
			}
		}
		if (args.length > 1) ImageIO.write(preview, "png", new File(args[1]));
		System.out.println("Wrote " + capes.length + " capes x " + FRAMES + " frames to " + dir);
	}

	// ------------------------------------------------------------------ designs

	static int eclipse(double u, double v, double t) {
		double x = (u - 0.5) * 10, y = (v - 0.36) * 16; // in cape pixels, centred on the eclipse
		double d = Math.hypot(x, y);
		int bg = gradient(v, 0xFF0B1024, 0xFF04050C);
		bg = stars(u, v, t, bg, 11, 0.012);
		double pulse = 0.5 + 0.5 * Math.sin(t * Math.PI * 2);
		double ang = Math.atan2(y, x);
		// Corona: bright ring with slowly turning rays.
		double rays = 0.55 + 0.45 * Math.sin(ang * 9 + t * Math.PI * 2);
		double corona = Math.exp(-Math.pow(d - 3.1, 2) / (0.9 + 0.5 * pulse)) + 0.6 * rays * Math.exp(-(d - 3.1) / (1.6 + 0.6 * pulse)) * (d > 3.1 ? 1 : 0);
		int c = mix(bg, 0xFFB8D4FF, clamp(corona * 0.9));
		c = mix(c, 0xFFFFFFFF, clamp(Math.exp(-Math.pow(d - 3.0, 2) / 0.12)));
		if (d < 2.9) c = mix(0xFF020205, 0xFF0D1020, clamp((2.9 - d) / 3));
		// The diamond-ring flare travelling around the rim.
		double fa = t * Math.PI * 2 - Math.PI / 4;
		double fd = Math.hypot(x - Math.cos(fa) * 3.0, y - Math.sin(fa) * 3.0);
		c = mix(c, 0xFFFFFFFF, clamp(Math.exp(-fd * fd / 0.35)));
		// Orbit ring across the lower half, with a moving dot.
		double ry = (v - 0.72) * 16 + (u - 0.5) * 3.2;
		if (Math.abs(ry) < 0.35) c = mix(c, 0xFF6E8CFF, 0.85);
		double dotU = 0.5 + 0.42 * Math.cos(t * Math.PI * 2), dotV = 0.72 - (dotU - 0.5) * 0.2;
		if (Math.hypot((u - dotU) * 10, (v - dotV) * 16) < 0.6) c = 0xFFFFFFFF;
		return c;
	}

	static int galaxy(double u, double v, double t) {
		double n = fbm(u * 3 + Math.cos(t * Math.PI * 2) * 0.15, v * 4 + Math.sin(t * Math.PI * 2) * 0.15, 4);
		double swirl = Math.sin(u * 6 + v * 9 + n * 4);
		int c = gradient(v, 0xFF1A0E3A, 0xFF07040F);
		c = mix(c, 0xFF7B2FBF, clamp((n - 0.35) * 1.8));
		c = mix(c, 0xFF2F6FDF, clamp((swirl * 0.5 + 0.5) * (n - 0.45) * 2.2));
		c = mix(c, 0xFFFF7AD9, clamp((n - 0.68) * 3));
		return stars(u, v, t, c, 23, 0.03);
	}

	static int flame(double u, double v, double t) {
		// Noise rising upward, looping: sampled on a cylinder in time.
		double rise = v * 3 + t * 3;
		double n = fbmPeriodic(u * 4, rise, 4, 3);
		double heat = clamp((v * 1.25) + (n - 0.5) * 0.9 - 0.15);
		int c = 0xFF0A0302;
		c = mix(c, 0xFF7A1206, clamp(heat * 2));
		c = mix(c, 0xFFE2490F, clamp(heat * 2 - 0.6));
		c = mix(c, 0xFFFFB02E, clamp(heat * 2 - 1.1));
		c = mix(c, 0xFFFFF2B0, clamp(heat * 2 - 1.6));
		// Embers floating up.
		for (int i = 0; i < 6; i++) {
			double eu = hash(i * 7.1) * 0.9 + 0.05, ev = (1 - ((t + hash(i * 3.3)) % 1.0));
			if (Math.hypot((u - eu - Math.sin(ev * 9 + i) * 0.05) * 10, (v - ev) * 16) < 0.45) c = mix(c, 0xFFFFD27A, 0.9);
		}
		return c;
	}

	static int aurora(double u, double v, double t) {
		int c = gradient(v, 0xFF041018, 0xFF0B1B2E);
		c = stars(u, v, t, c, 31, 0.012);
		double ph = t * Math.PI * 2;
		for (int band = 0; band < 3; band++) {
			double center = 0.3 + band * 0.17 + 0.06 * Math.sin(u * 7 + ph + band * 2.1);
			double width = 0.05 + 0.03 * Math.sin(u * 11 - ph * 2 + band);
			double k = Math.exp(-Math.pow((v - center) / width, 2));
			// Curtain streaks hanging down from each band.
			double streak = 0.5 + 0.5 * Math.sin(u * 40 + band * 3 + Math.sin(ph + u * 5) * 2);
			double curtain = v > center ? Math.exp(-(v - center) * 9) * streak : 0;
			int col = band == 0 ? 0xFF3CF0A0 : band == 1 ? 0xFF2FD3E0 : 0xFFB06CFF;
			c = mix(c, col, clamp(k * 0.9 + curtain * 0.5));
		}
		// Mountains silhouette at the bottom.
		double ridge = 0.86 - 0.06 * Math.abs(Math.sin(u * 7.3)) - 0.04 * Math.abs(Math.sin(u * 17));
		if (v > ridge) c = mix(0xFF03070C, 0xFF07131C, clamp((v - ridge) * 4));
		return c;
	}

	static int ocean(double u, double v, double t) {
		int c = gradient(v, 0xFF1C6FA8, 0xFF041C33);
		double ph = t * Math.PI * 2;
		for (int i = 0; i < 6; i++) {
			double base = 0.12 + i * 0.15;
			double wave = base + 0.03 * Math.sin(u * 9 + ph * (i % 2 == 0 ? 1 : -1) + i * 1.7) + 0.015 * Math.sin(u * 23 - ph * 2 + i);
			double dv = v - wave;
			if (dv > 0 && dv < 0.06) c = mix(c, 0xFF0A3558, 0.35);
			if (Math.abs(dv) < 0.012) c = mix(c, 0xFFE8F6FF, 0.85);
			else if (Math.abs(dv) < 0.025) c = mix(c, 0xFF8FD3F5, 0.5);
		}
		// Sun glints.
		double g = Math.sin(u * 50 + ph * 3) * Math.sin(v * 60 - ph * 2);
		if (g > 0.93 && v < 0.6) c = mix(c, 0xFFFFFFFF, 0.6);
		return c;
	}

	static int circuit(double u, double v, double t) {
		int c = gradient(v, 0xFF070C12, 0xFF03050A);
		int gx = (int) (u * 10 * 4), gy = (int) (v * 16 * 4); // fine grid, 4 cells per cape pixel
		// Traces: pseudo-random horizontal and vertical lines on a 5-cell grid.
		boolean trace = false;
		double along = 0;
		if (gy % 5 == 0 && hash(gy * 1.7 + (gx / 9) * 13.1) > 0.58) {
			trace = true;
			along = gx / 40.0 + gy * 0.13;
		} else if (gx % 5 == 0 && hash(gx * 3.1 + (gy / 11) * 5.3) > 0.66) {
			trace = true;
			along = gy / 64.0 + gx * 0.21;
		}
		if (trace) {
			c = 0xFF0F3A44;
			// Pulses travelling along each trace.
			double p = (along - t) % 0.25;
			if (p < 0) p += 0.25;
			if (p < 0.035) c = mix(0xFF3FE0FF, 0xFFFFFFFF, 1 - p / 0.035);
		}
		if (trace && gx % 5 == 0 && gy % 5 == 0) c = 0xFF5FF5FF; // nodes where traces meet
		return c;
	}

	static int sakura(double u, double v, double t) {
		int c = gradient(v, 0xFFFBE3EC, 0xFFE6A0BE);
		// A branch across the top.
		double bx = u * 10, by = v * 16;
		double branch = 2.2 + Math.sin(bx * 0.7) * 0.8 + bx * 0.25;
		if (Math.abs(by - branch) < 0.35) c = 0xFF3B2128;
		for (int i = 0; i < 5; i++) {
			double fx = 1 + i * 2.1, fy = 2.2 + Math.sin(fx * 0.7) * 0.8 + fx * 0.25 - 0.4;
			if (Math.hypot(bx - fx, by - fy) < 0.7) c = mix(0xFFFF8FB8, 0xFFFFFFFF, 0.3);
		}
		// Falling, drifting petals.
		for (int i = 0; i < 9; i++) {
			double pv = (hash(i * 4.2) + t) % 1.0;
			double pu = hash(i * 9.7) + Math.sin(pv * 8 + i) * 0.06;
			pu -= Math.floor(pu);
			double dx = (u - pu) * 10, dy = (v - pv) * 16;
			if (dx * dx / 0.18 + dy * dy / 0.08 < 1) c = mix(c, 0xFFFF6FA5, 0.9);
		}
		return c;
	}

	static int midnight(double u, double v, double t) {
		int c = gradient(v, 0xFF151A45, 0xFF050713);
		c = stars(u, v, t, c, 47, 0.02);
		double x = (u - 0.62) * 10, y = (v - 0.22) * 16;
		double d = Math.hypot(x, y);
		// Crescent moon with a soft glow.
		if (d < 2.2 && Math.hypot(x + 1.0, y - 0.6) > 1.9) c = mix(0xFFFFF4D6, 0xFFE8D9A8, clamp((d - 1) / 1.2));
		else if (d >= 2.2) c = mix(c, 0xFF8C93D9, clamp(0.35 * Math.exp(-(d - 2.2) * 0.9)));
		// Clouds drifting across, looping.
		double cu = u + t;
		double cloud = fbmPeriodic(cu * 2, v * 3, 3, 2);
		double band = Math.exp(-Math.pow((v - 0.62) / 0.12, 2));
		c = mix(c, 0xFF2C3168, clamp((cloud - 0.45) * 2.5 * band));
		return c;
	}

	static int stormfront(double u, double v, double t) {
		double ph = t * Math.PI * 2;
		// Rolling storm clouds drifting sideways (looping), darker toward the bottom.
		double n = fbmPeriodic(v * 3 + 7, u * 2 + t * 2, 4, 2);
		int c = gradient(v, 0xFF2A3346, 0xFF080A10);
		c = mix(c, 0xFF4A5670, clamp((n - 0.45) * 2.2));
		c = mix(c, 0xFF141822, clamp((0.5 - n) * 1.5) * v);
		// A forked bolt that strikes twice per loop, flashing the clouds.
		double strike = Math.max(0, Math.cos(ph * 2)) ;
		double flash = Math.pow(strike, 12);
		c = mix(c, 0xFFB8C8FF, flash * 0.35 * (1 - v));
		double bx = 0.55 + 0.12 * Math.sin(v * 9) + 0.07 * Math.sin(v * 23 + 1);
		double branch = 0.38 + 0.1 * Math.sin(v * 13 + 2);
		double w = Math.abs(u - bx) * 10, wb = v > 0.35 && v < 0.62 ? Math.abs(u - branch - (v - 0.35) * 0.4) * 10 : 9;
		double bolt = Math.min(w, wb);
		if (flash > 0.05) {
			if (bolt < 0.35) c = mix(c, 0xFFFFFFFF, flash * 1.2);
			else if (bolt < 1.1) c = mix(c, 0xFF8FB8FF, flash * (1.1 - bolt));
		}
		// Rain streaks falling.
		double rain = ((v * 22 + u * 3 - t * 8) % 1 + 1) % 1;
		if (hash(Math.floor(u * 40) * 3.7) > 0.72 && rain < 0.18) c = mix(c, 0xFF9FB3D1, 0.35);
		return c;
	}

	static int magma(double u, double v, double t) {
		// Dark cracked rock with lava glowing in the cracks, slowly flowing downward.
		double x = u * 5, y = v * 8 - t * 2;
		double best = 9, second = 9;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				int cx = (int) Math.floor(x) + i, cy = (int) Math.floor(y) + j;
				int py = Math.floorMod(cy, 16);
				double px = cx + hash(cx * 17.3 + py * 3.1), pyy = cy + hash(cx * 5.7 + py * 11.9);
				double d = Math.hypot(x - px, y - pyy);
				if (d < best) {
					second = best;
					best = d;
				} else if (d < second) second = d;
			}
		}
		double crack = second - best;
		double pulse = 0.75 + 0.25 * Math.sin(t * Math.PI * 2 + v * 6);
		int rock = mix(0xFF2A1A16, 0xFF15100E, clamp(best));
		int c = rock;
		if (crack < 0.22) {
			double k = 1 - crack / 0.22;
			c = mix(rock, 0xFFB8300C, clamp(k * 1.5));
			c = mix(c, 0xFFFF8A1C, clamp(k * 1.6 - 0.5) * pulse);
			c = mix(c, 0xFFFFE08A, clamp(k * 2 - 1.5) * pulse);
		}
		// Heat glow rising from the bottom edge.
		return mix(c, 0xFFFF5A12, clamp((v - 0.8) * 2.5) * 0.5 * pulse);
	}

	static int prismWave(double u, double v, double t) {
		// Diagonal rainbow bands that ripple and scroll.
		double ph = t * Math.PI * 2;
		double s = v * 1.4 + u * 0.6 + 0.08 * Math.sin(u * 8 + ph) - t;
		double h = ((s % 1) + 1) % 1;
		int c = hsv(h, 0.72, 0.95);
		// Soft sheen line travelling down the cape.
		double sheen = Math.exp(-Math.pow(((v - t * 1.0 + 1) % 1) - 0.5, 2) / 0.003);
		c = mix(c, 0xFFFFFFFF, sheen * 0.45);
		// Thin dark separators between bands.
		double band = (s * 6 % 1 + 1) % 1;
		if (band < 0.06) c = mix(c, 0xFF1A1230, 0.35);
		return c;
	}

	static int fireflyForest(double u, double v, double t) {
		int c = gradient(v, 0xFF0F2A3A, 0xFF2A5A4A);
		c = stars(u, v, t, c, 61, 0.008);
		// A big pale moon behind the trees.
		double md = Math.hypot((u - 0.3) * 10, (v - 0.2) * 16);
		if (md < 1.8) c = mix(0xFFEAF2D6, 0xFFC9D6B0, clamp(md / 1.8));
		else c = mix(c, 0xFF4E6B5A, clamp(0.35 * Math.exp(-(md - 1.8) * 0.7)));
		// Two layers of pine silhouettes.
		for (int layer = 0; layer < 2; layer++) {
			double base = layer == 0 ? 0.62 : 0.78;
			double period = layer == 0 ? 0.23 : 0.31;
			double off = layer == 0 ? 0.05 : 0.17;
			double local = ((u + off) % period) / period - 0.5;
			double treeTop = base - 0.32 * (1 - Math.abs(local) * 2) - 0.05 * hash(Math.floor((u + off) / period) + layer * 9);
			boolean tiers = (Math.floor((v - treeTop) * 28) % 2 == 0) || Math.abs(local) < 0.32;
			if (v > treeTop && tiers) c = layer == 0 ? 0xFF0B2018 : 0xFF040C08;
		}
		// Fireflies wandering and blinking.
		for (int i = 0; i < 10; i++) {
			double ph = t * Math.PI * 2;
			double fu = 0.1 + 0.8 * hash(i * 3.3) + 0.05 * Math.sin(ph + i * 1.7);
			double fv = 0.45 + 0.45 * hash(i * 7.9) + 0.04 * Math.cos(ph * 2 + i);
			double blink = Math.max(0, Math.sin(ph * (1 + i % 2) + i * 2.3));
			double d = Math.hypot((u - fu) * 10, (v - fv) * 16);
			c = mix(c, 0xFFE6FF7A, clamp(blink * (1.2 - d * 1.4)));
		}
		return c;
	}

	static int frostbite(double u, double v, double t) {
		int c = gradient(v, 0xFFD9F1FA, 0xFF7CB8D6);
		// Frost creeping in from the edges: branching ice crystals.
		double edge = Math.min(Math.min(u, 1 - u) * 1.6, Math.min(v, 1 - v));
		double n = fbm(u * 6, v * 9, 4);
		double frost = clamp((0.22 - edge) * 6 + (n - 0.5) * 1.4);
		c = mix(c, 0xFFFFFFFF, frost * 0.8);
		// A big snowflake in the middle, slowly turning.
		double x = (u - 0.5) * 10, y = (v - 0.4) * 16;
		double r = Math.hypot(x, y), a = Math.atan2(y, x) + t * Math.PI / 3;
		double arm = Math.abs(Math.sin(a * 3));
		if (r < 3.4 && arm < 0.12 + 0.04 / Math.max(0.3, r)) c = mix(c, 0xFF3F8FBF, 0.85);
		double sub = Math.abs(Math.sin(a * 3 + 0.5));
		if (r > 1.4 && r < 2.6 && sub < 0.1) c = mix(c, 0xFF3F8FBF, 0.7);
		if (r < 0.6) c = 0xFF2F7FB0;
		// Snow falling.
		for (int i = 0; i < 14; i++) {
			double pv = (hash(i * 5.1) + t) % 1.0;
			double pu = hash(i * 8.3) + Math.sin(pv * 7 + i) * 0.04;
			if (Math.hypot((u - pu) * 10, (v - pv) * 16) < 0.4) c = mix(c, 0xFFFFFFFF, 0.95);
		}
		return c;
	}

	static int retrowave(double u, double v, double t) {
		int c = gradient(v / 0.55, 0xFF1A0838, 0xFFB0307A);
		c = stars(u, v, t, c, 71, v < 0.3 ? 0.02 : 0);
		// A striped sun sitting on the horizon.
		double x = (u - 0.5) * 10, y = (v - 0.42) * 16;
		double d = Math.hypot(x, y);
		if (d < 3.6 && v < 0.55) {
			int sun = mix(0xFFFFE066, 0xFFFF3F8E, clamp((v - 0.2) / 0.35));
			double stripe = (v * 40 + t * 2) % 2;
			boolean gap = v > 0.38 && stripe < 0.25 + (v - 0.38) * 6;
			if (!gap) c = sun;
		}
		// Horizon and the scrolling perspective grid below it.
		if (v > 0.55) {
			double depth = (v - 0.55) / 0.45;
			c = mix(0xFF1A0630, 0xFF2A0A40, depth);
			double z = 1 / (depth + 0.05);
			double row = ((z * 0.6 + t * 4) % 1 + 1) % 1;
			double col = Math.abs(((u - 0.5) * z * 1.2 % 0.5 + 0.5) % 0.5 - 0.25);
			if (row < 0.08 * (1 + depth * 2) || col < 0.012 * z * depth + 0.01) c = mix(c, 0xFFFF3FD8, clamp(0.4 + depth));
			if (Math.abs(v - 0.55) < 0.008) c = 0xFFFF9AE6;
		}
		return c;
	}

	static int koiPond(double u, double v, double t) {
		int c = gradient(v, 0xFF1C6B7A, 0xFF0A3A48);
		// Caustic shimmer on the water.
		double ph = t * Math.PI * 2;
		double ca = Math.sin(u * 23 + Math.sin(v * 17 + ph) * 2) * Math.sin(v * 29 + Math.cos(u * 13 - ph) * 2);
		if (ca > 0.6) c = mix(c, 0xFF7FD6D9, (ca - 0.6) * 1.2);
		// Two koi circling each other, head and a trailing body.
		for (int k = 0; k < 2; k++) {
			double a0 = ph + k * Math.PI;
			int body = k == 0 ? 0xFFFF7A2E : 0xFFF7F2EA, spot = k == 0 ? 0xFFF7F2EA : 0xFFE5484D;
			for (int s = 0; s < 9; s++) {
				double a = a0 - s * 0.16;
				double fx = 5 + Math.cos(a) * 2.6, fy = 8 + Math.sin(a) * 4.5;
				double r = s == 0 ? 0.85 : 0.85 - s * 0.08;
				double d = Math.hypot(u * 10 - fx, v * 16 - fy);
				if (d < r) c = (s == 2 || s == 5) ? spot : body;
				if (s == 8 && d < 0.9) c = mix(c, body, 0.6); // tail fin
			}
		}
		// Lily pads in the corners.
		for (double[] p : new double[][]{{1.6, 2.4}, {8.4, 13.2}, {8.0, 3.0}}) {
			double d = Math.hypot(u * 10 - p[0], v * 16 - p[1]);
			double a = Math.atan2(v * 16 - p[1], u * 10 - p[0]);
			if (d < 1.3 && !(Math.abs(a) < 0.3)) c = d > 1.1 ? 0xFF2E7A3A : 0xFF4E9A44;
		}
		return c;
	}

	static int hive(double u, double v, double t) {
		// Hexagonal honeycomb with honey filling and dripping.
		double x = u * 10 / 1.6, y = v * 16 / 1.6;
		double qy = y / 0.866;
		double row = Math.floor(qy);
		double qx = x - (row % 2 == 0 ? 0 : 0.5);
		double cx = Math.floor(qx) + 0.5 + (row % 2 == 0 ? 0 : 0.5), cy = (row + 0.5) * 0.866;
		double best = 9, second = 9;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				double rr = row + j;
				double ox = Math.floor(qx) + i + 0.5 + (Math.floorMod((long) rr, 2) == 0 ? 0 : 0.5);
				double d = Math.hypot(x - ox, y - (rr + 0.5) * 0.866);
				if (d < best) {
					second = best;
					best = d;
					cx = ox;
					cy = (rr + 0.5) * 0.866;
				} else if (d < second) second = d;
			}
		}
		double wall = second - best;
		double fill = hash(cx * 7.1 + cy * 3.3);
		int c;
		if (wall < 0.12) c = 0xFF8A5A12;
		else {
			int honey = mix(0xFFFFC72C, 0xFFE08A00, clamp(best * 1.6));
			c = fill > 0.3 ? honey : mix(0xFF3A2408, 0xFF5A3A0E, best);
			// A glint shimmering across full cells.
			if (fill > 0.3 && Math.abs(((u + v - t) % 1 + 1) % 1 - 0.5) < 0.03) c = mix(c, 0xFFFFF4C2, 0.7);
		}
		// Honey drips along the top, sliding down and looping.
		for (int i = 0; i < 4; i++) {
			double du = 0.15 + i * 0.24, len = 0.15 + 0.25 * ((t + hash(i * 4.1)) % 1);
			if (Math.abs(u - du) * 10 < 0.45 && v < len) c = 0xFFFFB81C;
			if (Math.hypot((u - du) * 10, (v - len) * 16) < 0.6) c = 0xFFFFC72C;
		}
		return c;
	}

	static int regal(double u, double v, double t) {
		// Deep velvet with a moving sheen, a gold border and a crest.
		double sheen = 0.5 + 0.5 * Math.sin((u * 3 - v * 2) * Math.PI + t * Math.PI * 2);
		int c = mix(0xFF4A0A24, 0xFF7A1438, sheen * 0.6 + fbm(u * 8, v * 12, 3) * 0.2);
		double bx = Math.min(u, 1 - u) * 10, by = Math.min(v, 1 - v) * 16;
		double edge = Math.min(bx, by);
		if (edge < 0.55) c = mix(0xFFF2C14E, 0xFFB8862A, (edge * 4) % 1);
		else if (edge < 0.8) c = 0xFF2A0612;
		// Diamond lattice of fine gold thread.
		double lu = u * 10 + v * 6.4, lv = u * 10 - v * 6.4;
		if (edge > 0.8 && (Math.abs(lu % 2.5) < 0.08 || Math.abs(lv % 2.5) < 0.08)) c = mix(c, 0xFFC9963A, 0.55);
		// The crest: a crown above a shield-shaped emblem.
		double x = (u - 0.5) * 10, y = (v - 0.45) * 16;
		boolean shield = Math.abs(x) < 2.2 - Math.max(0, y) * 0.55 && y > -1.6 && y < 3.6;
		if (shield) c = Math.abs(x) > 1.6 - Math.max(0, y) * 0.55 || y < -1.1 ? 0xFFF2C14E : 0xFF1A0612;
		if (shield && Math.abs(x) < 0.3 && y > -0.8 && y < 2.4) c = 0xFFF2C14E;
		if (y > -3.6 && y < -2.0 && Math.abs(x) < 1.8 && (y > -2.6 || Math.abs(Math.abs(x) - 0.9) > 0.5 && Math.abs(x) > 0.3 || Math.abs(x) < 0.3)) c = 0xFFF2C14E;
		// A glint sweeping across the gold.
		double glint = Math.exp(-Math.pow(((u + v * 0.6 - t * 1.6) % 1 + 1) % 1 - 0.5, 2) / 0.002);
		if (c == 0xFFF2C14E || edge < 0.55) c = mix(c, 0xFFFFF6D8, glint * 0.8);
		return c;
	}

	static int plasma(double u, double v, double t) {
		// Lava-lamp blobs of colored plasma rising and merging.
		double ph = t * Math.PI * 2;
		double field = 0;
		for (int i = 0; i < 6; i++) {
			double bu = 0.2 + 0.6 * hash(i * 2.7) + 0.1 * Math.sin(ph + i);
			double bv = ((hash(i * 6.1) - t * (i % 2 == 0 ? 1 : 2)) % 1 + 1) % 1;
			for (int k = -1; k <= 1; k++) {
				double d2 = Math.pow((u - bu) * 10, 2) + Math.pow((v - bv - k) * 16, 2);
				field += (1.4 + 0.4 * hash(i)) / (d2 + 0.4);
			}
		}
		int c = gradient(v, 0xFF1A0838, 0xFF0A0418);
		if (field > 0.5) {
			int hot = hsv(0.85 - clamp((field - 0.5) * 0.4) * 0.35 + 0.05 * Math.sin(ph), 0.8, 1);
			c = mix(c, hot, clamp((field - 0.5) * 3));
			if (field > 1.4) c = mix(c, 0xFFFFFFFF, clamp((field - 1.4) * 0.6));
		}
		return c;
	}

	// ------------------------------------------------------------------ helpers

	static int hsv(double h, double s, double v) {
		return 0xFF000000 | java.awt.Color.HSBtoRGB((float) (((h % 1) + 1) % 1), (float) s, (float) v);
	}

	static int stars(double u, double v, double t, int c, int seed, double density) {
		int sx = (int) (u * 40), sy = (int) (v * 64);
		double h = hash(sx * 12.9898 + sy * 78.233 + seed);
		if (h < density) {
			double tw = 0.5 + 0.5 * Math.sin(t * Math.PI * 2 + h * 900);
			return mix(c, 0xFFFFFFFF, 0.35 + 0.65 * tw);
		}
		return c;
	}

	static double hash(double n) {
		double s = Math.sin(n) * 43758.5453;
		return s - Math.floor(s);
	}

	static double noise(double x, double y, int periodY) {
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		double fx = x - x0, fy = y - y0;
		double a = hash2(x0, y0, periodY), b = hash2(x0 + 1, y0, periodY), c = hash2(x0, y0 + 1, periodY), d = hash2(x0 + 1, y0 + 1, periodY);
		double sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
		return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy;
	}

	static double hash2(int x, int y, int periodY) {
		if (periodY > 0) y = Math.floorMod(y, periodY);
		return hash(x * 127.1 + y * 311.7);
	}

	static double fbm(double x, double y, int octaves) {
		double sum = 0, amp = 0.5, f = 1;
		for (int i = 0; i < octaves; i++) {
			sum += amp * noise(x * f, y * f, 0);
			amp *= 0.5;
			f *= 2;
		}
		return sum / (1 - Math.pow(0.5, octaves));
	}

	/** Noise that repeats every `period` units along y (doubling with each octave), for looping scrolls. */
	static double fbmPeriodic(double x, double y, int octaves, int period) {
		double sum = 0, amp = 0.5, f = 1;
		for (int i = 0; i < octaves; i++) {
			sum += amp * noise(x * f, y * f, (int) (period * f));
			amp *= 0.5;
			f *= 2;
		}
		return sum / (1 - Math.pow(0.5, octaves));
	}

	static int gradient(double v, int top, int bottom) {
		return mix(top, bottom, clamp(v));
	}

	static double clamp(double x) {
		return Math.max(0, Math.min(1, x));
	}

	static int mix(int a, int b, double k) {
		k = clamp(k);
		int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return 0xFF000000 | ((int) Math.round(ar + (br - ar) * k) << 16) | ((int) Math.round(ag + (bg - ag) * k) << 8) | (int) Math.round(ab + (bb - ab) * k);
	}

	static void fill(BufferedImage img, int x0, int y0, int x1, int y1, int c) {
		for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) img.setRGB(x, y, c);
	}
}
