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
	// The same faces for drawing: neighbouring faces share their corners, so each distinct corner is kept
	// once (points, x y z in pixels) and a face names its four by index. dirs is each face's direction (DIRS).
	private final float[] points;
	private final int pointCount;
	private final int[] cornerIndex;
	private final byte[] dirs;
	private final int[] paints;
	private final Paint[] palette;
	private final int faces;
	// The voxel grid itself, kept for drawing the model's picture (see icon).
	private char[][][] grid;
	private Map<Character, Paint> gridPalette;
	// The y of the highest voxel's top edge, in pixels.
	private float top;

	private static final int[][] DIRS = {{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};

	private VoxelModel(float[] corners, byte[] normals, int[] paints, Paint[] palette, int faces) {
		this.corners = corners;
		this.normals = normals;
		this.paints = paints;
		this.palette = palette;
		this.faces = faces;
		Map<Long, Integer> seen = new HashMap<>();
		float[] unique = new float[faces * 12];
		this.cornerIndex = new int[faces * 4];
		this.dirs = new byte[faces];
		int count = 0;
		for (int f = 0; f < faces; f++) {
			for (int v = 0; v < 4; v++) {
				int o = f * 12 + v * 3;
				// Corners sit on a grid of at most a few hundred steps: 21 bits each tells them apart exactly.
				long key = ((long) Float.floatToIntBits(corners[o]) * 0x9E3779B97F4A7C15L)
					^ ((long) Float.floatToIntBits(corners[o + 1]) * 0xC2B2AE3D27D4EB4FL)
					^ ((long) Float.floatToIntBits(corners[o + 2]) * 0x165667B19E3779F9L);
				Integer index = seen.get(key);
				if (index == null || unique[index * 3] != corners[o] || unique[index * 3 + 1] != corners[o + 1] || unique[index * 3 + 2] != corners[o + 2]) {
					index = count++;
					unique[index * 3] = corners[o];
					unique[index * 3 + 1] = corners[o + 1];
					unique[index * 3 + 2] = corners[o + 2];
					seen.put(key, index);
				}
				cornerIndex[f * 4 + v] = index;
			}
			int nx = normals[f * 3], ny = normals[f * 3 + 1], nz = normals[f * 3 + 2];
			dirs[f] = (byte) (nx < 0 ? 0 : nx > 0 ? 1 : ny < 0 ? 2 : ny > 0 ? 3 : nz < 0 ? 4 : 5);
		}
		this.points = java.util.Arrays.copyOf(unique, count * 3);
		this.pointCount = count;
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
		float top = Float.NaN;
		for (int y = 0; y < ny && Float.isNaN(top); y++) {
			for (int z = 0; z < nz && Float.isNaN(top); z++) {
				for (int x = 0; x < nx; x++) {
					if (grid[y][z][x] != '.') {
						top = oy + y * size;
						break;
					}
				}
			}
		}

		// The surface, one direction at a time. Within a layer, neighbouring uncovered voxel faces of the same
		// paint are merged into rectangles: a flat one-color area is a few faces instead of one per voxel,
		// which is most of what there is to draw.
		int f = 0;
		int[] extent = {nx, ny, nz};
		for (int d = 0; d < 6; d++) {
			int axis = d / 2;
			// The two axes across the layer: (z, y) for x, (x, z) for y, (x, y) for z.
			int ua = axis == 0 ? 2 : 0, va = axis == 1 ? 2 : 1;
			int nu = extent[ua], nv = extent[va];
			int[][] mask = new int[nv][nu];
			int[] at = new int[3];
			for (int layer = 0; layer < extent[axis]; layer++) {
				for (int v = 0; v < nv; v++) {
					for (int u = 0; u < nu; u++) {
						at[axis] = layer;
						at[ua] = u;
						at[va] = v;
						char c = grid[at[1]][at[2]][at[0]];
						int ax = at[0] + DIRS[d][0], ay = at[1] + DIRS[d][1], az = at[2] + DIRS[d][2];
						boolean covered = ax >= 0 && ay >= 0 && az >= 0 && ax < nx && ay < ny && az < nz && grid[ay][az][ax] != '.';
						mask[v][u] = c == '.' || covered ? 0 : index.get(c) + 1;
					}
				}
				for (int v = 0; v < nv; v++) {
					for (int u = 0; u < nu; u++) {
						int paint = mask[v][u];
						if (paint == 0) continue;
						int w = 1;
						while (u + w < nu && mask[v][u + w] == paint) w++;
						int h = 1;
						grow:
						while (v + h < nv) {
							for (int k = 0; k < w; k++) if (mask[v + h][u + k] != paint) break grow;
							h++;
						}
						for (int dv = 0; dv < h; dv++) java.util.Arrays.fill(mask[v + dv], u, u + w, 0);
						// The rectangle as a box of voxels, one layer thick.
						int[] lo = new int[3], hi = new int[3];
						lo[axis] = hi[axis] = layer;
						lo[ua] = u;
						hi[ua] = u + w - 1;
						lo[va] = v;
						hi[va] = v + h - 1;
						float x0 = ox + lo[0] * size, y0 = oy + lo[1] * size, z0 = oz + lo[2] * size;
						float x1 = ox + (hi[0] + 1) * size, y1 = oy + (hi[1] + 1) * size, z1 = oz + (hi[2] + 1) * size;
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
						paints[f] = paint - 1;
						f++;
					}
				}
			}
		}
		VoxelModel model = new VoxelModel(corners, normals, paints, paintList, f);
		model.grid = grid;
		model.gridPalette = palette;
		model.top = Float.isNaN(top) ? oy : top;
		return model;
	}

	/** The y of the model's highest point, in pixels (y grows downward). */
	public float top() {
		return top;
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

	/** Builds a model from a shape function, centered on the origin (for pets and floating bits). */
	public static VoxelModel centered(float size, Map<Character, Paint> palette, int nx, int ny, int nz, Shape shape) {
		return shape(size, -nx * size / 2, -ny * size / 2, -nz * size / 2, palette, nx, ny, nz, shape);
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
		// Leave a generous inset for the circular wardrobe/orbit masks. Wide wing tips
		// otherwise sit too close to the node edge and can disappear under the circle.
		double scale = Math.max(1, size - 64) / (double) Math.max(w, ny);
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
	/** How many models and faces have been drawn since the game started (for the performance test). */
	public static long drawnModels, drawnFaces;

	public void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int tint, float alpha) {
		drawnModels++;
		drawnFaces += faces;
		// Every corner is placed once and every direction turned once for this draw (see Vertices.Batch);
		// the faces then only pick them. Drawing the same model vertex by vertex did that work up to four
		// times per corner and once per vertex for the direction.
		com.orbitclient.util.Vertices.Batch batch = com.orbitclient.util.Vertices.begin(consumer, pose, points, pointCount, 1 / 16f);
		int[] colors = new int[palette.length];
		for (int i = 0; i < palette.length; i++) {
			Paint p = palette[i];
			int color = p.tinted ? shade(tint, p.shade) : p.argb;
			int a = Math.round(((color >>> 24) & 0xFF) * alpha);
			colors[i] = (a << 24) | (color & 0xFFFFFF);
		}
		for (int f = 0; f < faces; f++) {
			int paint = paints[f], o = f * 4;
			batch.quad(cornerIndex[o], cornerIndex[o + 1], cornerIndex[o + 2], cornerIndex[o + 3],
				colors[paint], palette[paint].glow ? FULL_BRIGHT : light, dirs[f]);
		}
	}

	static int shade(int argb, float shade) {
		int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * shade));
		int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * shade));
		int b = Math.min(255, Math.round((argb & 0xFF) * shade));
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}
}
