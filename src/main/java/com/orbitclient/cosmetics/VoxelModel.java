package com.orbitclient.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.HashMap;
import java.util.Map;

/**
 * A small 3D pixel-art model: layers of characters, one cube per character, drawn with only the faces
 * that aren't hidden by a neighbour. Positions are in model pixels (1/16 block); y grows downward like
 * Minecraft's entity models. Each character's {@link Paint} gives its color, whether it follows the
 * cosmetic's chosen color, and whether it glows (drawn at full brightness).
 */
public final class VoxelModel {
	public static final int FULL_BRIGHT = 0xF000F0;

	/** How a voxel character is colored. */
	public record Paint(int argb, boolean tinted, float shade, boolean glow) {
		public static Paint of(int argb) {
			return new Paint(argb, false, 1f, false);
		}

		public static Paint glow(int argb) {
			return new Paint(argb, false, 1f, true);
		}

		/** Takes the chosen color, darkened by shade (1 = as chosen). */
		public static Paint tint(float shade) {
			return new Paint(0xFFFFFFFF, true, shade, false);
		}

		public static Paint tintGlow(float shade) {
			return new Paint(0xFFFFFFFF, true, shade, true);
		}
	}

	// Per face: 4 corners x 3 coordinates, a normal, a paint index.
	private final float[] corners;
	private final byte[] normals;
	private final int[] paints;
	private final Paint[] palette;
	private final int faces;
	// The voxel grid itself, kept for drawing the model's picture (see icon).
	private char[][][] grid;
	private Map<Character, Paint> gridPalette;

	private static final int[][] DIRS = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};

	private VoxelModel(float[] corners, byte[] normals, int[] paints, Paint[] palette, int faces) {
		this.corners = corners;
		this.normals = normals;
		this.paints = paints;
		this.palette = palette;
		this.faces = faces;
	}

	/**
	 * Builds a model. Each slice is one horizontal layer (top first), rows separated by '/'; within a row,
	 * each character is one voxel along x, and rows go along z (first row = front, -z). '.' or ' ' is empty.
	 *
	 * @param size voxel edge length in pixels
	 * @param ox   x of the first column's left edge, in pixels
	 * @param oy   y of the top slice's top edge
	 * @param oz   z of the first row's front edge
	 */
	public static VoxelModel build(float size, float ox, float oy, float oz, Map<Character, Paint> palette, String... slices) {
		int ny = slices.length, nz = 0, nx = 0;
		String[][] rows = new String[ny][];
		for (int y = 0; y < ny; y++) {
			rows[y] = slices[y].split("/", -1);
			nz = Math.max(nz, rows[y].length);
			for (String r : rows[y]) nx = Math.max(nx, r.length());
		}
		char[][][] grid = new char[ny][nz][nx];
		for (int y = 0; y < ny; y++) {
			for (int z = 0; z < nz; z++) {
				for (int x = 0; x < nx; x++) {
					String r = z < rows[y].length ? rows[y][z] : "";
					char c = x < r.length() ? r.charAt(x) : '.';
					grid[y][z][x] = (c == ' ' || !palette.containsKey(c)) ? '.' : c;
				}
			}
		}
		Map<Character, Integer> index = new HashMap<>();
		Paint[] paintList = new Paint[palette.size()];
		int pi = 0;
		for (var e : palette.entrySet()) {
			index.put(e.getKey(), pi);
			paintList[pi++] = e.getValue();
		}

		int max = nx * ny * nz * 6;
		float[] corners = new float[max * 12];
		byte[] normals = new byte[max * 3];
		int[] paints = new int[max];
		int f = 0;
		for (int y = 0; y < ny; y++) {
			for (int z = 0; z < nz; z++) {
				for (int x = 0; x < nx; x++) {
					char c = grid[y][z][x];
					if (c == '.') continue;
					float x0 = ox + x * size, y0 = oy + y * size, z0 = oz + z * size;
					float x1 = x0 + size, y1 = y0 + size, z1 = z0 + size;
					for (int d = 0; d < 6; d++) {
						int ax = x + DIRS[d][0], ay = y + DIRS[d][1], az = z + DIRS[d][2];
						boolean covered = ax >= 0 && ay >= 0 && az >= 0 && ax < nx && ay < ny && az < nz && grid[ay][az][ax] != '.';
						if (covered) continue;
						float[] q = switch (d) {
							case 0 -> new float[]{x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0};
							case 1 -> new float[]{x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1};
							case 2 -> new float[]{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1};
							case 3 -> new float[]{x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0};
							case 4 -> new float[]{x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0};
							default -> new float[]{x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1};
						};
						System.arraycopy(q, 0, corners, f * 12, 12);
						normals[f * 3] = (byte) DIRS[d][0];
						normals[f * 3 + 1] = (byte) DIRS[d][1];
						normals[f * 3 + 2] = (byte) DIRS[d][2];
						paints[f] = index.get(c);
						f++;
					}
				}
			}
		}
		VoxelModel model = new VoxelModel(corners, normals, paints, paintList, f);
		model.grid = grid;
		model.gridPalette = palette;
		return model;
	}

	/** A voxel's character from its grid position (x across, y down, z front to back); '.' for empty. */
	public interface Shape {
		char at(int x, int y, int z);
	}

	/** Builds a model from a shape function over an nx * ny * nz grid. */
	public static VoxelModel shape(float size, float ox, float oy, float oz, Map<Character, Paint> palette, int nx, int ny, int nz, Shape shape) {
		String[] slices = new String[ny];
		for (int y = 0; y < ny; y++) {
			StringBuilder sb = new StringBuilder();
			for (int z = 0; z < nz; z++) {
				if (z > 0) sb.append('/');
				for (int x = 0; x < nx; x++) sb.append(shape.at(x, y, z));
			}
			slices[y] = sb.toString();
		}
		return build(size, ox, oy, oz, palette, slices);
	}

	/**
	 * Draws the model as a small 3D picture (isometric, seen from the front-left and above), for menus.
	 * pair adds a mirrored copy beside it (for wings). Returns ARGB pixels, size x size.
	 */
	public int[] icon(int size, int tint, boolean pair) {
		java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = img.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		int ny = grid.length, nz = grid[0].length, nx = grid[0][0].length;
		if (pair) {
			// Flat panels like wings read best straight on: both sides spread out, seen from behind.
			flatPair(g, size, tint, nx, ny, nz);
			g.dispose();
			return img.getRGB(0, 0, size, size, null, 0, size);
		}
		// Voxels as (x, y, z, paint), with the mirrored copy for pairs.
		java.util.List<int[]> cells = new java.util.ArrayList<>();
		java.util.List<Character> chars = new java.util.ArrayList<>();
		for (int y = 0; y < ny; y++) {
			for (int z = 0; z < nz; z++) {
				for (int x = 0; x < nx; x++) {
					char c = grid[y][z][x];
					if (c == '.') continue;
					cells.add(new int[]{x, y, z});
					chars.add(c);
					if (pair) {
						cells.add(new int[]{-x - 3, y, z});
						chars.add(c);
					}
				}
			}
		}
		if (cells.isEmpty()) return new int[size * size];
		java.util.Set<Long> filled = new java.util.HashSet<>();
		for (int[] v : cells) filled.add(key(v[0], v[1], v[2]));
		// Projection: x goes up-right, z up-left, y down; nearer = smaller x + y + z.
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (int[] v : cells) {
			for (int dx = 0; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = 0; dz <= 1; dz++) {
				double sx = projX(v[0] + dx, v[2] + dz), sy = projY(v[0] + dx, v[1] + dy, v[2] + dz);
				minX = Math.min(minX, sx);
				maxX = Math.max(maxX, sx);
				minY = Math.min(minY, sy);
				maxY = Math.max(maxY, sy);
			}
		}
		double scale = (size - 6) / Math.max(maxX - minX, maxY - minY);
		double ox = (size - (maxX - minX) * scale) / 2 - minX * scale, oy = (size - (maxY - minY) * scale) / 2 - minY * scale;
		Integer[] order = new Integer[cells.size()];
		for (int i = 0; i < order.length; i++) order[i] = i;
		java.util.Arrays.sort(order, (a, b) -> {
			int[] va = cells.get(a), vb = cells.get(b);
			return Integer.compare(vb[0] + vb[1] + vb[2], va[0] + va[1] + va[2]); // far first
		});
		for (int i : order) {
			int[] v = cells.get(i);
			Paint p = gridPalette.get(chars.get(i));
			int color = p.tinted ? shade(tint, p.shade) : p.argb;
			int x = v[0], y = v[1], z = v[2];
			// Top, front (-z) and left (-x) faces, shaded like daylight from above.
			if (!filled.contains(key(x, y - 1, z))) face(g, color, p.glow ? 1.15f : 1f, ox, oy, scale,
				new double[][]{{x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}});
			if (!filled.contains(key(x, y, z - 1))) face(g, color, p.glow ? 1f : 0.8f, ox, oy, scale,
				new double[][]{{x, y, z}, {x + 1, y, z}, {x + 1, y + 1, z}, {x, y + 1, z}});
			if (!filled.contains(key(x - 1, y, z))) face(g, color, p.glow ? 0.92f : 0.62f, ox, oy, scale,
				new double[][]{{x, y, z}, {x, y, z + 1}, {x, y + 1, z + 1}, {x, y + 1, z}});
		}
		g.dispose();
		return img.getRGB(0, 0, size, size, null, 0, size);
	}

	/** The model and its mirror image side by side (hinges in the middle), each voxel a flat square. */
	private void flatPair(java.awt.Graphics2D g, int size, int tint, int nx, int ny, int nz) {
		int gap = 2, w = nx * 2 + gap;
		double scale = (size - 6) / (double) Math.max(w, ny);
		double ox = (size - w * scale) / 2, oy = (size - ny * scale) / 2;
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
		for (int z = nz - 1; z >= 0; z--) { // back to front
			for (int y = 0; y < ny; y++) {
				for (int x = 0; x < nx; x++) {
					char c = grid[y][z][x];
					if (c == '.') continue;
					Paint p = gridPalette.get(c);
					int color = p.tinted ? shade(tint, p.shade) : p.argb;
					// A little darker toward the bottom, like light from above.
					color = shade(color, p.glow ? 1f : 1.05f - 0.25f * y / ny) & 0xFFFFFF | 0xFF000000;
					g.setColor(new java.awt.Color(color, true));
					int sy0 = (int) Math.floor(oy + y * scale), sy1 = (int) Math.floor(oy + (y + 1) * scale);
					for (int side = 0; side < 2; side++) {
						int col = side == 0 ? nx + gap + x : nx - 1 - x; // right wing, then its mirror on the left
						int sx0 = (int) Math.floor(ox + col * scale), sx1 = (int) Math.floor(ox + (col + 1) * scale);
						g.fillRect(sx0, sy0, Math.max(1, sx1 - sx0), Math.max(1, sy1 - sy0));
					}
				}
			}
		}
	}

	private static long key(int x, int y, int z) {
		return ((long) (x + 512) << 40) | ((long) (y + 512) << 20) | (z + 512);
	}

	private static double projX(double x, double z) {
		return (x - z) * 0.866;
	}

	private static double projY(double x, double y, double z) {
		return -(x + z) * 0.5 + y;
	}

	private static void face(java.awt.Graphics2D g, int argb, float light, double ox, double oy, double scale, double[][] pts) {
		java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
		for (int i = 0; i < pts.length; i++) {
			double sx = ox + projX(pts[i][0], pts[i][2]) * scale, sy = oy + projY(pts[i][0], pts[i][1], pts[i][2]) * scale;
			if (i == 0) path.moveTo(sx, sy);
			else path.lineTo(sx, sy);
		}
		path.closePath();
		g.setColor(new java.awt.Color(shade(argb, light) & 0xFFFFFF | 0xFF000000, true));
		g.fill(path);
		// A hairline in the same color closes anti-aliasing gaps between neighbouring faces.
		g.setStroke(new java.awt.BasicStroke(0.6f));
		g.draw(path);
	}

	/** Draws the model at the pose (in model pixels). tint is the chosen color; alpha fades the whole model. */
	public void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int tint, float alpha) {
		float s = 1 / 16f;
		for (int f = 0; f < faces; f++) {
			Paint p = palette[paints[f]];
			int color = p.tinted ? shade(tint, p.shade) : p.argb;
			int a = Math.round(((color >>> 24) & 0xFF) * alpha);
			color = (a << 24) | (color & 0xFFFFFF);
			int lit = p.glow ? FULL_BRIGHT : light;
			float nx = normals[f * 3], ny = normals[f * 3 + 1], nz = normals[f * 3 + 2];
			int o = f * 12;
			for (int v = 0; v < 4; v++) {
				consumer.addVertex(pose, corners[o + v * 3] * s, corners[o + v * 3 + 1] * s, corners[o + v * 3 + 2] * s)
					.setColor(color).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lit).setNormal(pose, nx, ny, nz);
			}
		}
	}

	static int shade(int argb, float shade) {
		int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * shade));
		int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * shade));
		int b = Math.min(255, Math.round((argb & 0xFF) * shade));
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}
}
