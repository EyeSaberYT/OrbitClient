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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to the cosmetics server (server/CosmeticsServer.java, found through DNS): signs you in the way
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
	 * Where the cosmetics server is comes from DNS, not from the mod: an SRV record on this name gives
	 * its host and port (like Minecraft's _minecraft._tcp records), so the address can change without
	 * a new build and isn't written in the jar. Not a setting.
	 */
	// Kept from the Eclipse Client days: the record and server didn't change with the rename.
	private static final String SRV_NAME = "_eclipse-cosmetics._tcp.minearcade.org";
	private static final long RESOLVE_MS = 30 * 60_000;
	private static volatile String resolved;
	private static volatile long resolvedAt;
	private static volatile boolean resolving;
	/** For tests only: use this server instead of looking it up. */
	public static volatile String testServer;

	/** Short status for the wardrobe, e.g. "Synced: other Orbit Client players see your cosmetics". */
	public static String status() {
		if (server() == null) return resolving || resolvedAt == 0 ? "Connecting to the cosmetics server..." : "Cosmetics server unavailable: only you see these right now";
		return status;
	}

	/** The cosmetics (and friends) server's address, or null while it isn't known. */
	public static String server() {
		if (testServer != null) return testServer;
		long now = System.currentTimeMillis();
		if (!resolving && now - resolvedAt > (resolved == null ? 60_000 : RESOLVE_MS)) {
			resolving = true;
			CompletableFuture.runAsync(() -> {
				try {
					resolved = lookup();
				} catch (Exception e) {
					// Keep the last known address (or none) and try again later.
				} finally {
					resolvedAt = System.currentTimeMillis();
					resolving = false;
				}
			}, WORKER);
		}
		return resolved;
	}

	/** Reads the SRV record: "priority weight port host". */
	private static String lookup() throws Exception {
		java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		env.put("java.naming.provider.url", "dns:");
		env.put("com.sun.jndi.dns.timeout.retries", "1");
		javax.naming.directory.DirContext ctx = new javax.naming.directory.InitialDirContext(env);
		try {
			javax.naming.directory.Attribute srv = ctx.getAttributes(SRV_NAME, new String[]{"SRV"}).get("srv");
			if (srv == null || srv.size() == 0) return null;
			String[] parts = srv.get(0).toString().trim().split("\\s+");
			if (parts.length < 4) return null;
			int port = Integer.parseInt(parts[2]);
			String host = parts[3].endsWith(".") ? parts[3].substring(0, parts[3].length() - 1) : parts[3];
			return "http://" + host + ":" + port;
		} finally {
			ctx.close();
		}
	}
	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		String server = server();
		if (server == null || mc.level == null || mc.player == null || busy) return;
		if (!server.equals(tokenServer) || !mc.getUser().getProfileId().equals(tokenUser)) {
			// Another server or another account (switched in-game): sign in again and upload this account's loadout.
			uploadedOnce = false;
			FETCHED.clear();
		}
		if (!uploadedOnce || (uploadPending && System.currentTimeMillis() >= uploadAt)) {
			uploadPending = false;
			uploadedOnce = true;
			String body = CosmeticManager.own().toJson().toString();
			run(() -> upload(server, body));
			return;
		}
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
		UUID user = Minecraft.getInstance().getUser().getProfileId();
		if (!server.equals(tokenServer) || !user.equals(tokenUser)) {
			token = null;
			tokenServer = server;
			tokenUser = user;
		}
		if (token == null && !signIn(server)) return null;
		return token;
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
			HttpResponse.BodyHandlers.ofString());
		if (ch.statusCode() != 200) {
			status = "Cosmetics server error (" + ch.statusCode() + ")";
			return false;
		}
		String serverId = JsonParser.parseString(ch.body()).getAsJsonObject().get("serverId").getAsString();
		try {
			mc.services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), serverId);
		} catch (Exception e) {
			status = "Couldn't sign in (offline account?): others can't see your cosmetics";
			return false;
		}
		HttpResponse<String> v = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/auth/verify?name=" + URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)
			+ "&serverId=" + serverId)).timeout(Duration.ofSeconds(12)).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
		if (v.statusCode() != 200) {
			status = "Cosmetics server refused sign-in (" + v.statusCode() + ")";
			return false;
		}
		token = JsonParser.parseString(v.body()).getAsJsonObject().get("token").getAsString();
		return true;
	}

	private static void upload(String server, String body) {
		try {
			for (int attempt = 0; attempt < 2; attempt++) {
				String t = token(server);
				if (t == null) return;
				HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(server + "/loadout")).timeout(Duration.ofSeconds(8))
					.header("Authorization", "Bearer " + t).header("Content-Type", "application/json")
					.PUT(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
				if (r.statusCode() == 401 && attempt == 0) {
					invalidate(t);
					continue;
				}
				status = r.statusCode() == 200 ? "Synced: other Orbit Client players see your cosmetics"
					: "Cosmetics server error (" + r.statusCode() + ")";
				return;
			}
		} catch (Exception e) {
			status = "Can't reach the cosmetics server";
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
				HttpResponse.BodyHandlers.ofString());
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
