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

	// ------------------------------------------------------------------ helpers

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
