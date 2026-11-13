package com.orbitclient.test;

import com.orbitclient.account.SessionSwitcher;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.User;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Switches the game to another account from the menus and checks the game then runs as it. Uses a made-up
 * account with a token Mojang rejects, so it also covers the fallback when the account services refuse it.
 */
@SuppressWarnings("UnstableApiUsage")
public class SessionSwitchTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (Boolean.getBoolean("orbit.perfTest")) return;  // a PerformanceTest run
		if (Boolean.getBoolean("orbit.shotsOnly")) return;  // a screenshots-only run (-PshotsOnly)
		UUID uuid = UUID.fromString("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0");
		CompletableFuture<Void> switched = context.computeOnClient(mc -> SessionSwitcher.switchTo("OrbitTestAcct", uuid, "not-a-real-token"));
		long deadline = System.currentTimeMillis() + 30_000;
		while (!switched.isDone() && System.currentTimeMillis() < deadline) context.waitTick();
		if (!switched.isDone()) throw new AssertionError("Account switch didn't finish in 30 seconds");
		try {
			switched.get(0, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new AssertionError("Account switch failed", e);
		}
		context.waitTicks(5);
		User user = context.computeOnClient(mc -> mc.getUser());
		if (!user.getName().equals("OrbitTestAcct") || !user.getProfileId().equals(uuid)) {
			throw new AssertionError("Game still runs as " + user.getName() + " " + user.getProfileId());
		}
		String title = context.computeOnClient(mc -> mc.getWindow() != null ? "ok" : "no window");
		System.out.println("[ACCOUNT] Switched to " + user.getName() + " (" + user.getProfileId() + "), window " + title);
	}
}
