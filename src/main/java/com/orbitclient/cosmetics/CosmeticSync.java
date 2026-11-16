package com.orbitclient.cosmetics;

import com.orbitclient.config.ClientSettings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

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
 * Talks to the cosmetics server (server/CosmeticsServer.java, over HTTPS at {@link #SERVER}): signs you in the way
 * Minecraft servers do (Mojang joinServer, checked by the server with hasJoined), uploads your loadout
 * when it changes, and fetches the loadouts of players around you. 
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
	public record Session(String token, UUID user) {
	}

	private static volatile String token;
	private static volatile String tokenServer;
	/** The account the token was issued to: switching accounts in-game needs a new one. */
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
	 * Ask the server for this account's claims on the next tick: opening the wardrobe, its Refresh
	 * button, and signing in all call this. Website redemptions then show up without a restart.
	 */
	public static void requestRefresh() {
		claimsAt = 0;
	}

	/** True while a claims request is in flight. */
	public static boolean checkingClaims() {
		return checkingClaims;
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		UUID account = mc.getUser().getProfileId();
		// Each account wears and owns only its own things, even before the server is known.
		CosmeticManager.useAccount(account);
		String server = server();
		if (server == null || busy) return;
		if (!server.equals(tokenServer) || !account.equals(tokenUser)) {
			// Another server or another account (switched in-game): sign in again and upload this account's loadout.
			uploadedOnce = false;
			FETCHED.clear();
			claimsAt = 0;
			uploadRetryAt = 0;
		}
		if (!uploadedOnce || (uploadPending && System.currentTimeMillis() >= uploadAt)) {
			if (System.currentTimeMillis() < uploadRetryAt) return;
			uploadPending = false;
			uploadedOnce = true;
			String body = CosmeticManager.own().toJson().toString();
			run(() -> upload(server, body));
			return;
		}
		if (System.currentTimeMillis() - claimsAt >= REFRESH_MS) {
			claimsAt = System.currentTimeMillis();
			run(() -> refreshClaims(server));
			return;
		}
		if (mc.level == null || mc.player == null) return;
		if (++tickCounter % 100 != 0 || !ClientSettings.INSTANCE.showOthersCosmetics.isOn()) return;
		long now = System.currentTimeMillis();
		List<UUID> wanted = new ArrayList<>();
		for (Player p : mc.level.players()) {
			if (p == mc.player) continue;
			Long last = FETCHED.get(p.getUUID());
			if (last == null || now - last > REFRESH_MS) wanted.add(p.getUUID());
			if (wanted.size() >= 100) break;
		}
		if (wanted.isEmpty()) return;
		for (UUID u : wanted) FETCHED.put(u, now);
		if (FETCHED.size() > 2000) FETCHED.clear();
		run(() -> fetch(server, wanted));
	}

	private static void run(Runnable job) {
		busy = true;
		CompletableFuture.runAsync(() -> {
			try {
				job.run();
			} catch (Exception e) {
				status = "Can't reach the cosmetics server";
			} finally {
				busy = false;
			}
		}, WORKER);
	}

	private static String host(String server) {
		return URI.create(server).getHost();
	}

	/**
	 * A sign-in token for the current account on this server, signing in first when there's none
	 * (or it was issued to another account or server). Null when signing in failed. Blocks: call it
	 * off the game thread. Shared with the friends service.
	 */
	public static synchronized String token(String server) throws Exception {
		Session s = session(server);
		return s == null ? null : s.token();
	}

	/** Like {@link #token}, together with the account the token belongs to. Null when signing in failed. */
	public static synchronized Session session(String server) throws Exception {
		UUID user = Minecraft.getInstance().getUser().getProfileId();
		if (!server.equals(tokenServer) || !user.equals(tokenUser)) {
			token = null;
			tokenServer = server;
			tokenUser = user;
		}
		if (token == null && !signIn(server)) return null;
		return new Session(token, user);
	}

	/** The server answered 401 with this token: the next {@link #token} signs in again. */
	public static synchronized void invalidate(String rejected) {
		if (rejected != null && rejected.equals(token)) token = null;
	}

	/** Signs in: a one-time challenge, joinServer with Mojang, then the server checks it. */
	private static boolean signIn(String server) throws Exception {
		Minecraft mc = Minecraft.getInstance();
		var user = mc.getUser();
		HttpResponse<String> ch = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/auth/challenge")).timeout(Duration.ofSeconds(8)).GET().build(),
			limited());
		if (ch.statusCode() != 200) {
			status = "Cosmetics server error (" + ch.statusCode() + ")";
			return false;
		}
		String serverId = JsonParser.parseString(ch.body()).getAsJsonObject().get("serverId").getAsString();
		// The server's challenges are 32 hex digits; anything else isn't one.
		if (!serverId.matches("[0-9a-f]{32}")) {
			status = "Cosmetics server error (sign-in)";
			return false;
		}
		String client = randomHex();
		String signed = signInId(serverId, client);
		try {
			mc.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), signed);
		} catch (Exception e) {
			status = "Couldn't sign in (offline account?): others can't see your cosmetics";
			return false;
		}
		HttpResponse<String> v = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/auth/verify?name=" + URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)
			+ "&serverId=" + serverId + "&client=" + client)).timeout(Duration.ofSeconds(12)).POST(HttpRequest.BodyPublishers.noBody()).build(), limited());
		if (v.statusCode() != 200) {
			status = "Cosmetics server refused sign-in (" + v.statusCode() + ")";
			return false;
		}
		token = JsonParser.parseString(v.body()).getAsJsonObject().get("token").getAsString();
		return true;
	}

	private static void upload(String server, String ignoredBody) {
		try {
			for (int attempt = 0; attempt < 2; attempt++) {
				Session session = session(server);
				if (session == null) {
					uploadedOnce = false;
					uploadRetryAt = System.currentTimeMillis() + 15_000;
					return;
				}
				String t = session.token();
				if (!refreshClaims(server, session)) {
					uploadedOnce = false;
					uploadRetryAt = System.currentTimeMillis() + 15_000;
					return;
				}
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
			Session session = session(server);
			return session != null && refreshClaims(server, session);
		} catch (Exception e) {
			status = "Can't verify cosmetic claims right now";
			return false;
		}
	}

	/**
	 * Ask for the claims of {@code session}'s account and install them. On any failure (offline,
	 * 401/503, a malformed answer, an answer about another account, or the player switched accounts
	 * meanwhile) nothing changes: the ownership already shown stays, and the next refresh tries again.
	 */
	private static boolean refreshClaims(String server, Session session) throws Exception {
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
			Minecraft.getInstance().execute(() -> {
				try {
					// Still the same account? A late answer must not land on the account switched to.
					boolean same = session.user().equals(Minecraft.getInstance().getUser().getProfileId());
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
				if (!q.isEmpty()) q.append(',');
				q.append(u);
			}
			HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/loadouts?uuids=" + q)).timeout(Duration.ofSeconds(8)).GET().build(),
				limited());
			if (r.statusCode() != 200) return;
			JsonObject all = JsonParser.parseString(r.body()).getAsJsonObject();
			Minecraft mc = Minecraft.getInstance();
			for (UUID u : uuids) {
				var data = all.get(u.toString());
				// The cosmetics service exposes known Orbit users through /loadouts; there is no
				// /users route. A returned loadout (even an empty one) is the server-backed
				// confirmation that this UUID has connected with Orbit Client.
				if (data != null && !data.isJsonNull()) OrbitBadge.markUser(u);
				var loadout = CosmeticManager.Loadout.fromJson(data);
				mc.execute(() -> CosmeticManager.setOther(u, loadout));
			}
		} catch (Exception e) {
			status = "Can't reach the cosmetics server";
		}
	}
}
