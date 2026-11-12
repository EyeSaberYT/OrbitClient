package com.orbitclient.account;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Signs the running game in as another Microsoft account, without restarting it (see SessionSwap). */
public final class SessionSwitcher {
	private SessionSwitcher() {
	}

	/** Completes on the main thread once the game uses the new account. The token must be a valid Minecraft access token. */
	public static CompletableFuture<Void> switchTo(String name, UUID uuid, String accessToken) {
		//? if >=1.20.3 <26.2 {
		return SessionSwap.switchTo(name, uuid, accessToken);
		//?} else {
		/*// Not ported to 26.2's friends service in the signed-in session, nor to authlib before 6 (1.20.2 and older).
		return CompletableFuture.failedFuture(new IllegalStateException("Switch accounts in Orbit Launcher on this Minecraft version"));
		*///?}
	}
}
