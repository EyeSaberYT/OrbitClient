package com.orbitclient.compat;

//? if <1.21.2 {
/*import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Vector3f;

/^*
 * Before 1.21.2 NativeImage pixels were read and written as ABGR (setPixelRGBA), where 1.21.2's setPixel
 * and getPixel use ARGB like the rest of Orbit. build.gradle turns image.setPixel(...) into these.
 ^/
public final class Pixels {
	private Pixels() {
	}

	public static void set(NativeImage image, int x, int y, int argb) {
		image.setPixelRGBA(x, y, swapRedBlue(argb));
	}

	public static int get(NativeImage image, int x, int y) {
		return swapRedBlue(image.getPixelRGBA(x, y));
	}

	/^* An ARGB color as the red, green, blue vector older particle options take. ^/
	public static Vector3f rgb(int argb) {
		return new Vector3f((argb >> 16 & 0xFF) / 255f, (argb >> 8 & 0xFF) / 255f, (argb & 0xFF) / 255f);
	}

	private static int swapRedBlue(int color) {
		return (color & 0xFF00FF00) | (color >> 16 & 0xFF) | (color & 0xFF) << 16;
	}
}
*///?}
