package com.orbitclient.account;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Signs the running game in as another Microsoft account, without restarting it (see SessionSwap). */
public final class SessionSwitcher {
	private SessionSwitcher() {
	}

	/** Completes on the main thread once the game uses the new account. The token must be a valid Minecraft access token. */
	public static CompletableFuture<Void> switchTo(String name, UUID uuid, String accessToken) {
		//? if <26.2 {
		return SessionSwap.switchTo(name, uuid, accessToken);
		//?} else {
		/*// 26.2 made Mojang's friends part of the signed-in session; switching in the game isn't ported to that yet.
		return CompletableFuture.failedFuture(new IllegalStateException("Switch accounts in Orbit Launcher on this Minecraft version"));
		*///?}
	}
}
