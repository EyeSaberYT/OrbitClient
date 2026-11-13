package com.orbitclient.legacy.gl;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.orbitclient.cosmetics.VoxelModel;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Collects the quads of one voxel model and draws them with the old engine's tessellator: the lit
 * faces first, under the world's light, then the glowing ones at full brightness.
 */
public final class VertexConsumer {
	/** Light value the shared code passes for faces that glow (VoxelModel.FULL_BRIGHT). */
	private static final int FULL_BRIGHT = 0xF000F0;
	/** Floats per vertex: position, texture, normal. Colors are kept apart, as whole numbers. */
	private static final int STRIDE = 8;

	/** The vertices of one of the two groups (lit or glowing). */
	private static final class Batch {
		float[] data = new float[STRIDE * 256];
		int[] colors = new int[256];
		int count;

		void add(float x, float y, float z, int argb, float u, float v, float nx, float ny, float nz) {
			if (count == colors.length) {
				colors = java.util.Arrays.copyOf(colors, count * 2);
				data = java.util.Arrays.copyOf(data, count * 2 * STRIDE);
			}
			int n = count * STRIDE;
			data[n] = x;
			data[n + 1] = y;
			data[n + 2] = z;
			data[n + 3] = u;
			data[n + 4] = v;
			data[n + 5] = nx;
			data[n + 6] = ny;
			data[n + 7] = nz;
			colors[count++] = argb;
		}

		void draw() {
			if (count == 0) return;
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.getBuffer();
			buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
			for (int i = 0; i < count; i++) {
				int n = i * STRIDE, argb = colors[i];
				buffer.vertex(data[n], data[n + 1], data[n + 2])
					.texture(data[n + 3], data[n + 4])
					.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24)
					.normal(data[n + 5], data[n + 6], data[n + 7])
					.next();
			}
			tessellator.draw();
			count = 0;
		}
	}

	private final Batch lit = new Batch();
	private final Batch glow = new Batch();
	/** The light map spot of the player being drawn, put back after glowing faces. */
	private float lightU = 240f, lightV = 240f;

	/** The world light on the player being drawn, as Entity.getLightmapCoordinates gives it. */
	public void setLight(int packed) {
		lightU = packed % 65536;
		lightV = packed / 65536;
	}

	public void add(float x, float y, float z, int argb, float u, float v, int light, float nx, float ny, float nz) {
		(light == FULL_BRIGHT ? glow : lit).add(x, y, z, argb, u, v, nx, ny, nz);
	}

	/** Draws what was collected and empties the collector. The white texture must be bound. */
	public void flush() {
		boolean drew = lit.count > 0 || glow.count > 0;
		lit.draw();
		if (glow.count > 0) {
			glowOn();
			glow.draw();
			glowOff();
		}
		if (drew) resetColor();
	}

	/** Full brightness for glowing faces: no shading from the sun, and the brightest spot of the light map. */
	private void glowOn() {
		GlStateManager.disableLighting();
		GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, 240f, 240f);
	}

	private void glowOff() {
		GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, lightU, lightV);
		GlStateManager.enableLighting();
	}

	/**
	 * Drawing with per-vertex colors leaves OpenGL's current color at the last one drawn, while the game
	 * still believes it is white: forget what it believes and set white for real.
	 */
	private static void resetColor() {
		GlStateManager.clearCurrentColor();
		GlStateManager.color4f(1f, 1f, 1f, 1f);
	}

	// ---- models kept on the graphics card

	/** One model in one color, compiled into display lists (0 where it has no such faces). */
	private static final class Compiled {
		int lit, glow;
		boolean compiled;
	}

	/** A model and a tint. Models are compared by identity: each cosmetic builds its models once. */
	private record Key(VoxelModel model, int tint) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.model == model && k.tint == tint;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(model) * 31 + tint;
		}
	}

	private static final int CACHE_LIMIT = 4096;
	/** Least recently drawn first. The oldest is dropped (and its lists freed) past the limit. */
	private static final java.util.LinkedHashMap<Key, Compiled> CACHE = new java.util.LinkedHashMap<>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<Key, Compiled> eldest) {
			if (size() <= CACHE_LIMIT) return false;
			free(eldest.getValue());
			return true;
		}
	};

	private static void free(Compiled c) {
		if (c.lit != 0) GL11.glDeleteLists(c.lit, 1);
		if (c.glow != 0) GL11.glDeleteLists(c.glow, 1);
	}

	private static int compile(Batch batch) {
		if (batch.count == 0) return 0;
		int list = GL11.glGenLists(1);
		GL11.glNewList(list, GL11.GL_COMPILE);
		batch.draw();
		GL11.glEndList();
		return list;
	}

	/**
	 * Draws a voxel model in a tint. A model's faces never change, so from the second time one is drawn in
	 * a color it is replayed from display lists instead of being rebuilt vertex by vertex every frame.
	 * (The first time is drawn directly: a color that is only passing through, like a fading one, isn't
	 * worth compiling.)
	 */
	public void draw(VoxelModel model, int tint) {
		Key key = new Key(model, tint);
		Compiled entry = CACHE.get(key);
		if (entry == null) {
			CACHE.put(key, new Compiled());
			model.render(PoseStack.GL.last(), this, 0, tint, 1f);
			flush();
			return;
		}
		if (!entry.compiled) {
			model.render(PoseStack.GL.last(), this, 0, tint, 1f);
			entry.lit = compile(lit);
			entry.glow = compile(glow);
			entry.compiled = true;
		}
		if (entry.lit != 0) GL11.glCallList(entry.lit);
		if (entry.glow != 0) {
			glowOn();
			GL11.glCallList(entry.glow);
			glowOff();
		}
		resetColor();
	}
}
