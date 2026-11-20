package com.orbitclient.mixin;

// 26.3 has its own borderless fullscreen (Window.setExclusiveFullscreen), which BorderlessFullscreenModule uses instead.
//? if <26.3 {
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.orbitclient.compat.BorderlessWindow;
import com.orbitclient.module.visual.BorderlessFullscreenModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Borderless Fullscreen (see BorderlessFullscreenModule) up to 26.2, done the way Lunar does it: Window.setMode is where
 * Minecraft switches between windowed and fullscreen, so while the module is on, fullscreen becomes an undecorated
 * window the size of the monitor instead of exclusive fullscreen. Turning it off goes back to vanilla.
 */
@Mixin(Window.class)
public abstract class WindowMixin implements BorderlessWindow {
	//? if >=1.21.9 {
	@Shadow @Final private long handle;
	//?} else {
	/*@Shadow @Final private long window;
	*///?}
	@Shadow private int x;
	@Shadow private int y;
	@Shadow private int width;
	@Shadow private int height;
	@Shadow private int windowedX;
	@Shadow private int windowedY;
	@Shadow private int windowedWidth;
	@Shadow private int windowedHeight;
	@Shadow private boolean fullscreen;

	@Shadow public abstract Monitor findBestMonitor();

	@Shadow protected abstract void setMode();

	@Unique private boolean orbit$borderless;

	@Unique
	private long orbit$handle() {
		//? if >=1.21.9 {
		return handle;
		//?} else {
		/*return window;
		*///?}
	}

	@Inject(method = "setMode", at = @At("HEAD"), cancellable = true)
	private void orbit$setMode(CallbackInfo ci) {
		long handle = orbit$handle();
		if (fullscreen && BorderlessFullscreenModule.active()) {
			Monitor monitor = findBestMonitor();
			if (monitor == null) return; // let vanilla deal with (and log) a missing monitor
			//? if >=26.2 {
			/*VideoMode mode = monitor.currentMode();
			int monitorX = monitor.x(), monitorY = monitor.y();
			*///?} else {
			VideoMode mode = monitor.getCurrentMode();
			int monitorX = monitor.getX(), monitorY = monitor.getY();
			//?}
			// GLFW is written out in full: the 26.x build drops "import org.lwjgl.glfw.GLFW;" (26.3 moved to SDL).
			// Remember the windowed size only when coming from a normal window, not from exclusive fullscreen.
			if (!orbit$borderless && org.lwjgl.glfw.GLFW.glfwGetWindowMonitor(handle) == 0L) {
				windowedX = x;
				windowedY = y;
				windowedWidth = width;
				windowedHeight = height;
			}
			x = monitorX;
			y = monitorY;
			width = mode.getWidth();
			// One pixel taller than the screen on Windows, so Windows doesn't treat the window as exclusive
			// fullscreen (which is what traps Alt+Tab and the Windows key). The extra row is off screen.
			height = mode.getHeight() + (System.getProperty("os.name", "").startsWith("Windows") ? 1 : 0);
			org.lwjgl.glfw.GLFW.glfwSetWindowAttrib(handle, org.lwjgl.glfw.GLFW.GLFW_DECORATED, org.lwjgl.glfw.GLFW.GLFW_FALSE);
			org.lwjgl.glfw.GLFW.glfwSetWindowMonitor(handle, 0L, x, y, width, height, org.lwjgl.glfw.GLFW.GLFW_DONT_CARE);
			orbit$borderless = true;
			ci.cancel();
		} else if (orbit$borderless) {
			// Leaving borderless: put the frame back, and let vanilla go windowed or exclusive fullscreen.
			orbit$borderless = false;
			org.lwjgl.glfw.GLFW.glfwSetWindowAttrib(handle, org.lwjgl.glfw.GLFW.GLFW_DECORATED, org.lwjgl.glfw.GLFW.GLFW_TRUE);
			x = windowedX;
			y = windowedY;
			width = windowedWidth;
			height = windowedHeight;
			if (!fullscreen) {
				org.lwjgl.glfw.GLFW.glfwSetWindowMonitor(handle, 0L, x, y, width, height, org.lwjgl.glfw.GLFW.GLFW_DONT_CARE);
				ci.cancel();
			}
		}
	}

	@Override
	public boolean orbit$isBorderless() {
		return orbit$borderless;
	}

	@Override
	public void orbit$refreshMode() {
		setMode();
	}
}
//?}
