package com.orbitclient.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to the Orbit cosmetics server, for the old engine: the same protocol as the main client's
 * CosmeticSync. It signs in with a Mojang session check (the server learns who you are from Mojang, not
 * from this client), asks which cosmetics the account owns, uploads your outfit and fetches the outfits
 * of the players around you. A failed request never empties what is already shown.
 */
public final class CosmeticSync {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Orbit cosmetics sync");
		t.setDaemon(true);
		return t;
	});
	private static final long REFRESH_MS = 60_000;
	/** When claims were last asked for; 0 asks on the next tick (see {@link #requestRefresh}). */
	private static volatile long claimsAt;
	private static long uploadRetryAt;
	/** A claims request is in flight (the wardrobe shows "Checking..."). */
	private static volatile boolean checkingClaims;

	/** A sign-in token and the account it was issued to. */
	public record Token(String token, UUID user) {
	}

	private static volatile String token;
	private static volatile String tokenServer;
	/** The account the token was issued to: another account needs a new one. */
	private static volatile UUID tokenUser;
	private static volatile String status = "";
	private static volatile boolean uploadPending;
	private static long uploadAt;
	private static boolean uploadedOnce;
	private static int tickCounter;
	private static final Map<UUID, Long> FETCHED = new HashMap<>();
	private static volatile boolean busy;

	private CosmeticSync() {
	}

	public static void init() {
		CosmeticManager.setOnChange(() -> {
			com.orbitclient.legacy.OrbitConfig.save();
			uploadPending = true;
			uploadAt = System.currentTimeMillis() + 1500; // wait for the user to finish picking
		});
	}

	/**
	 * The cosmetics and friends server. HTTPS only (Cloudflare is in front of it): over plain HTTP anyone on
	 * the player's network could read friends' messages and the sign-in token, or swap the sign-in challenge.
	 */
	public static final String SERVER = "https://orbit-social.minearcade.org";
	/** Fixed start of what is signed with Mojang; the server and Orbit Launcher use the same. */
	private static final String SIGN_IN_PREFIX = "orbit-sign-in-v2:";
	/** The largest reply taken from the server; a bigger one is refused instead of filling the game's memory. */
	private static final int MAX_REPLY = 1 << 20;
	/** For tests only: use this server instead. */
	public static volatile String testServer;

	/** Short status for the wardrobe. */
	public static String status() {
		return status.isEmpty() ? "Connecting to the cosmetics server..." : status;
	}

	/** The cosmetics (and friends) server's address. */
	public static String server() {
		String test = testServer;
		return test != null ? test : SERVER;
	}

	/** Reads a reply as text, refusing one larger than {@link #MAX_REPLY} bytes. */
	public static HttpResponse.BodyHandler<String> limited() {
		return info -> HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofInputStream(), in -> {
			try (java.io.InputStream body = in) {
				byte[] bytes = body.readNBytes(MAX_REPLY + 1);
				if (bytes.length > MAX_REPLY) throw new java.io.IOException("The cosmetics server's reply was too large");
				return new String(bytes, StandardCharsets.UTF_8);
			} catch (java.io.IOException e) {
				throw new java.io.UncheckedIOException(e);
			}
		});
	}

	/**
	 * What the account tells Mojang it joined: SHA-1 of a fixed prefix, the server's challenge and a random
	 * number of the client's own, as 40 hex digits. Signing the challenge as given would let whoever answers
	 * it pass on the handshake value of a real Minecraft server they are joining under the player's name, and
	 * Mojang would then let them into that server as the player. Nobody else can choose this value.
	 */
	static String signInId(String challenge, String client) {
		try {
			byte[] digest = java.security.MessageDigest.getInstance("SHA-1")
				.digest((SIGN_IN_PREFIX + challenge + ":" + client).getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(40);
			for (byte b : digest) hex.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
			return hex.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** 32 random hex digits. */
	private static String randomHex() {
		byte[] bytes = new byte[16];
		new java.security.SecureRandom().nextBytes(bytes);
		StringBuilder hex = new StringBuilder(32);
		for (byte b : bytes) hex.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
		return hex.toString();
	}

	/**
	 * Ask the server for this account's claims on the next tick: opening the wardrobe and its Refresh
	 * button call this. Website redemptions then show up without a restart.
	 */
	public static void requestRefresh() {
		claimsAt = 0;
	}

	/** True while a claims request is in flight. */
	public static boolean checkingClaims() {
		return checkingClaims;
	}

	/** The signed-in account's id, or null when the session has none (it always has one in a normal game). */
	public static UUID account(MinecraftClient mc) {
		GameProfile profile = mc.getSession().getProfile();
		return profile == null ? null : profile.getId();
	}

	public static void tick(MinecraftClient mc) {
		UUID account = account(mc);
		// Each account wears and owns only its own things, even before the server is known.
		CosmeticManager.useAccount(account);
		String server = server();
		if (server == null || account == null || busy) return;
		if (!server.equals(tokenServer) || !account.equals(tokenUser)) {
			// Another server or another account: sign in again and upload this account's outfit.
			uploadedOnce = false;
			FETCHED.clear();
			claimsAt = 0;
			uploadRetryAt = 0;
			tokenServer = server;
			tokenUser = account;
			token = null;
		}
		long now = System.currentTimeMillis();
		if (!uploadedOnce && now >= uploadRetryAt) {
			uploadedOnce = true;
			uploadPending = false;
			run(() -> upload(server));
			return;
		}
		if (uploadPending && now >= uploadAt) {
			uploadPending = false;
			run(() -> upload(server));
			return;
		}
		if (uploadedOnce && now - claimsAt > REFRESH_MS) {
			// Give up on this round for a while if it fails, without forgetting what's owned.
			claimsAt = now - REFRESH_MS + 15_000;
			run(() -> refreshClaims(server));
			return;
		}
		// Other players' outfits need a world; claims and your own upload don't.
		if (!com.orbitclient.legacy.Game.inWorld(mc)) return;
		if (++tickCounter % 100 != 0 || !com.orbitclient.legacy.OrbitConfig.showOthers) return;
		List<UUID> ask = new ArrayList<>();
		for (UUID id : com.orbitclient.legacy.Game.playerIds(mc)) {
			if (id == null || id.equals(account)) continue;
			Long last = FETCHED.get(id);
			if (last != null && now - last < REFRESH_MS) continue;
			FETCHED.put(id, now);
			ask.add(id);
			if (ask.size() >= 40) break;
		}
		if (FETCHED.size() > 2000) FETCHED.clear();
		if (!ask.isEmpty()) run(() -> fetch(server, ask));
	}

	private static void run(Runnable work) {
		busy = true;
		CompletableFuture.runAsync(() -> {
			try {
				work.run();
			} finally {
				busy = false;
			}
		}, WORKER);
	}

	/** A sign-in token for the current account on this server, signing in first when there's none. Blocks. */
	private static synchronized Token session(String server) throws Exception {
		UUID user = account(MinecraftClient.getInstance());
		if (user == null) return null;
		if (!server.equals(tokenServer) || !user.equals(tokenUser)) {
			token = null;
			tokenServer = server;
			tokenUser = user;
		}
		if (token == null && !signIn(server)) return null;
		return new Token(token, user);
	}

	/** The server answered 401 with this token: the next request signs in again. */
	private static synchronized void invalidate(String rejected) {
		if (rejected != null && rejected.equals(token)) token = null;
	}

	/** Signs in: a one-time challenge, joinServer with Mojang, then the server checks it. */
	private static boolean signIn(String server) throws Exception {
		MinecraftClient mc = MinecraftClient.getInstance();
		Session user = mc.getSession();
		HttpResponse<String> ch = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/auth/challenge")).timeout(Duration.ofSeconds(8)).GET().build(),
			limited());
		if (ch.statusCode() != 200) {
			status = "Cosmetics server error (" + ch.statusCode() + ")";
			return false;
		}
		String serverId = new JsonParser().parse(ch.body()).getAsJsonObject().get("serverId").getAsString();
		// The server's challenges are 32 hex digits; anything else isn't one.
		if (!serverId.matches("[0-9a-f]{32}")) {
			status = "Cosmetics server error (sign-in)";
			return false;
		}
		String client = randomHex();
		String signed = signInId(serverId, client);
		try {
			mc.getSessionService().joinServer(user.getProfile(), user.getAccessToken(), signed);
		} catch (Exception e) {
			status = "Couldn't sign in (offline account?): others can't see your cosmetics";
			return false;
		}
		HttpResponse<String> v = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/auth/verify?name=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
			+ "&serverId=" + serverId + "&client=" + client)).timeout(Duration.ofSeconds(12)).POST(HttpRequest.BodyPublishers.noBody()).build(), limited());
		if (v.statusCode() != 200) {
			status = "Cosmetics server refused sign-in (" + v.statusCode() + ")";
			return false;
		}
		token = new JsonParser().parse(v.body()).getAsJsonObject().get("token").getAsString();
		return true;
	}

	private static void upload(String server) {
		try {
			for (int attempt = 0; attempt < 2; attempt++) {
				Token session = session(server);
				if (session == null || !refreshClaims(server, session)) {
					uploadedOnce = false;
					uploadRetryAt = System.currentTimeMillis() + 15_000;
					return;
				}
				String t = session.token();
				String body = CosmeticManager.own().toJson().toString();
				HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/loadout")).timeout(Duration.ofSeconds(8))
					.header("Authorization", "Bearer " + t).header("Content-Type", "application/json")
					.PUT(HttpRequest.BodyPublishers.ofString(body)).build(), limited());
				if (r.statusCode() == 401 && attempt == 0) {
					invalidate(t);
					continue;
				}
				status = r.statusCode() == 200 ? "Synced: other Orbit Client players see your cosmetics"
					: "Cosmetics server error (" + r.statusCode() + ")";
				if (r.statusCode() == 503) status = "Claim verification is unavailable; your outfit was not synced";
				if (r.statusCode() != 200) {
					uploadPending = true;
					uploadAt = System.currentTimeMillis() + 15_000;
				}
				return;
			}
		} catch (Exception e) {
			status = "Can't reach the cosmetics server";
			uploadedOnce = false;
			uploadRetryAt = System.currentTimeMillis() + 15_000;
		}
	}

	/** Fetch account-bound claims through the authenticated cosmetics-server token. */
	private static boolean refreshClaims(String server) {
		try {
			Token session = session(server);
			return session != null && refreshClaims(server, session);
		} catch (Exception e) {
			status = "Can't verify cosmetic claims right now";
			return false;
		}
	}

	/**
	 * Ask for the claims of {@code session}'s account and install them. On any failure (offline,
	 * 401/503, a malformed answer, an answer about another account, or the account changed meanwhile)
	 * nothing changes: the ownership already shown stays, and the next refresh tries again.
	 */
	private static boolean refreshClaims(String server, Token session) throws Exception {
		checkingClaims = true;
		try {
			HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/shop/claims")).timeout(Duration.ofSeconds(8))
				.header("Authorization", "Bearer " + session.token()).GET().build(), limited());
			if (r.statusCode() == 401) {
				invalidate(session.token());
				status = "Sign in again to check cosmetic claims";
				return false;
			}
			if (r.statusCode() != 200) {
				status = r.statusCode() == 503 ? "Claim verification is unavailable; showing what you had" : "Cosmetics server error (" + r.statusCode() + ")";
				return false;
			}
			Set<String> owned;
			try {
				owned = ClaimsResponse.parse(r.body(), session.user());
			} catch (IllegalArgumentException malformed) {
				status = "Can't verify cosmetic claims right now";
				return false;
			}
			CompletableFuture<Boolean> applied = new CompletableFuture<>();
			MinecraftClient mc = MinecraftClient.getInstance();
			com.orbitclient.legacy.Game.onGameThread(mc, () -> {
				try {
					// Still the same account? A late answer must not land on another account.
					boolean same = session.user().equals(account(mc));
					if (same) CosmeticManager.setOwned(session.user(), owned);
					applied.complete(same);
				} catch (Throwable error) {
					applied.completeExceptionally(error);
				}
			});
			if (!applied.join()) return false;
			claimsAt = System.currentTimeMillis();
			return true;
		} finally {
			checkingClaims = false;
		}
	}

	private static void fetch(String server, List<UUID> uuids) {
		try {
			StringBuilder q = new StringBuilder();
			for (UUID u : uuids) {
				if (q.length() > 0) q.append(',');
				q.append(u);
			}
			HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/loadouts?uuids=" + q)).timeout(Duration.ofSeconds(8)).GET().build(),
				limited());
			if (r.statusCode() != 200) return;
			JsonObject all = new JsonParser().parse(r.body()).getAsJsonObject();
			MinecraftClient mc = MinecraftClient.getInstance();
			for (UUID u : uuids) {
				JsonElement data = all.get(u.toString());
				CosmeticManager.Loadout loadout = CosmeticManager.Loadout.fromJson(data);
				com.orbitclient.legacy.Game.onGameThread(mc, () -> CosmeticManager.setOther(u, loadout));
			}
		} catch (Exception e) {
			// Other players' outfits stay as they were; asked again later.
		}
	}
}
