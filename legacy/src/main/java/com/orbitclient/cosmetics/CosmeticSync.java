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

	// Where the cosmetics server is comes from DNS, not from the mod: an SRV record on this name gives its
	// host and port, so the address can change without a new build. The same record the main client reads.
	private static final String SRV_NAME = "_eclipse-cosmetics._tcp.minearcade.org";
	private static final long RESOLVE_MS = 30 * 60_000;
	private static volatile String resolved;
	private static volatile long resolvedAt;
	private static volatile boolean resolving;
	/** For tests only: use this server instead of looking it up. */
	public static volatile String testServer;

	/** Short status for the wardrobe. */
	public static String status() {
		if (server() == null) return resolving || resolvedAt == 0 ? "Connecting to the cosmetics server..." : "Cosmetics server unavailable: only you see these right now";
		return status;
	}

	/** The cosmetics server's address, or null while it isn't known. */
	public static String server() {
		if (testServer != null) return testServer;
		long now = System.currentTimeMillis();
		if (!resolving && now - resolvedAt > (resolved == null ? 60_000 : RESOLVE_MS)) {
			resolving = true;
			CompletableFuture.runAsync(() -> {
				try {
					resolved = lookup();
				} catch (Throwable e) {
					// Keep the last known address (or none) and try again later.
					System.err.println("[Orbit Client] Couldn't look up the cosmetics server: " + e);
				} finally {
					resolvedAt = System.currentTimeMillis();
					resolving = false;
				}
			}, WORKER);
		}
		return resolved;
	}

	/** The server's address from its SRV record, or null when there is none. */
	private static String lookup() throws Exception {
		try {
			return lookupWithSystemDns();
		} catch (Throwable blocked) {
			// Minecraft 1.8 to 1.13 bundles a library ("patchy") that takes over Java's naming lookups and
			// can't reach the DNS provider on Java 17 and newer. The same record is then asked for over HTTPS.
			return lookupOverHttps();
		}
	}

	/** "priority weight port host", the way an SRV record's data is written. */
	private static String address(String record) {
		String[] parts = record.trim().split("\\s+");
		if (parts.length < 4) return null;
		int port = Integer.parseInt(parts[2]);
		String host = parts[3].endsWith(".") ? parts[3].substring(0, parts[3].length() - 1) : parts[3];
		return "http://" + host + ":" + port;
	}

	private static String lookupWithSystemDns() throws Exception {
		java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		env.put("java.naming.provider.url", "dns:");
		env.put("com.sun.jndi.dns.timeout.retries", "1");
		javax.naming.directory.DirContext ctx = new javax.naming.directory.InitialDirContext(env);
		try {
			javax.naming.directory.Attribute srv = ctx.getAttributes(SRV_NAME, new String[]{"SRV"}).get("srv");
			if (srv == null || srv.size() == 0) return null;
			return address(srv.get(0).toString());
		} finally {
			ctx.close();
		}
	}

	/** Public DNS-over-HTTPS resolvers that answer in JSON. Only the record's name is sent. */
	private static final String[] HTTPS_RESOLVERS = {"https://cloudflare-dns.com/dns-query", "https://dns.google/resolve"};

	private static String lookupOverHttps() throws Exception {
		Exception last = null;
		for (String resolver : HTTPS_RESOLVERS) {
			try {
				HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(resolver + "?name=" + SRV_NAME + "&type=SRV"))
					.timeout(Duration.ofSeconds(6)).header("Accept", "application/dns-json").GET().build(), HttpResponse.BodyHandlers.ofString());
				if (r.statusCode() != 200) continue;
				JsonObject answer = new JsonParser().parse(r.body()).getAsJsonObject();
				if (!answer.has("Answer") || !answer.get("Answer").isJsonArray()) return null;
				for (JsonElement entry : answer.getAsJsonArray("Answer")) {
					JsonObject record = entry.getAsJsonObject();
					// 33 is the SRV record type.
					if (record.has("type") && record.get("type").getAsInt() == 33 && record.has("data")) return address(record.get("data").getAsString());
				}
				return null;
			} catch (Exception e) {
				last = e;
			}
		}
		if (last != null) throw last;
		return null;
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
			HttpResponse.BodyHandlers.ofString());
		if (ch.statusCode() != 200) {
			status = "Cosmetics server error (" + ch.statusCode() + ")";
			return false;
		}
		String serverId = new JsonParser().parse(ch.body()).getAsJsonObject().get("serverId").getAsString();
		try {
			mc.getSessionService().joinServer(user.getProfile(), user.getAccessToken(), serverId);
		} catch (Exception e) {
			status = "Couldn't sign in (offline account?): others can't see your cosmetics";
			return false;
		}
		HttpResponse<String> v = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/auth/verify?name=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
			+ "&serverId=" + serverId)).timeout(Duration.ofSeconds(12)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
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
					.PUT(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
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
				.header("Authorization", "Bearer " + session.token()).GET().build(), HttpResponse.BodyHandlers.ofString());
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
				HttpResponse.BodyHandlers.ofString());
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
