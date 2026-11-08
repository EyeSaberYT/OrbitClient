package com.orbitclient.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/** Counts left/right clicks over the last second. Polled every frame so clicks between ticks still count. */
public final class CpsTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftWasDown;
	private static boolean rightWasDown;

	private CpsTracker() {
	}

	public static void update() {
		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		boolean inGame = mc.screen == null;

		boolean left = inGame && InputUtil.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		boolean right = inGame && InputUtil.isMouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		if (left && !leftWasDown) LEFT.addLast(now);
		if (right && !rightWasDown) RIGHT.addLast(now);
		leftWasDown = left;
		rightWasDown = right;

		while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.removeFirst();
		while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.removeFirst();
	}

	public static int left() {
		return LEFT.size();
	}

	public static int right() {
		return RIGHT.size();
	}
}
