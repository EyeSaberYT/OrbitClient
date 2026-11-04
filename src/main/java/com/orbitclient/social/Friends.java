package com.orbitclient.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbitclient.config.ClientSettings;
import com.orbitclient.cosmetics.CosmeticSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Friends through the cosmetics server (server/CosmeticsServer.java): your friends and whether they're
 * online (and on which server), friend requests, blocks and direct messages. Polls every few seconds,
 * on menus and in-game; the poll is also the heartbeat that shows you online to your friends.
 * State is read and changed on the game thread only; the network runs on a worker.
 */
public final class Friends {
	public record Person(UUID uuid, String name) {
	}

	public record Friend(UUID uuid, String name, boolean online, String server) {
		/** "Online", "Playing on X", "In singleplayer", "In Orbit Launcher" or "Offline". */
		public String status() {
			if (!online) return "Offline";
			return switch (server) {
				case "" -> "Online";
				case "singleplayer" -> "In singleplayer";
				case "launcher" -> "In Orbit Launcher";
				default -> "Playing on " + server;
			};
		}

		/** A server address their friends can join (not singleplayer, menus or the launcher). */
		public boolean joinable() {
			return online && !server.isEmpty() && !server.equals("singleplayer") && !server.equals("launcher");
		}
	}

	public record Message(long id, UUID from, UUID to, String text, long time) {
	}

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Orbit friends");
		t.setDaemon(true);
		return t;
	});
	private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(5000L);
	private static final long POLL_MS = 6000, FAST_POLL_MS = 2000, RETRY_MS = 15000;
	private static final int CHAT_HISTORY = 200;

	private static List<Friend> friends = List.of();
	private static List<Person> incoming = List.of(), outgoing = List.of(), blocked = List.of();
	private static volatile Set<UUID> friendIds = Set.of();
	private static final Map<UUID, List<Message>> CHATS = new HashMap<>();
	private static final Map<UUID, Integer> UNREAD = new HashMap<>();
	private static long lastMessage;
	private static boolean loaded;
	private static String status = "Connecting...";
	private static UUID account;
	private static long nextPoll;
	private static volatile boolean polling;
	/** The conversation on screen: it polls faster and its messages aren't unread. */
	public static UUID openChat;
	/** While the friends screen is open, polls are faster. */
	public static boolean screenOpen;

	private Friends() {
	}

	// ---------------------------------------------------------------- reading (game thread)

	public static List<Friend> friends() {
		return friends;
	}

	public static List<Person> incoming() {
		return incoming;
	}

	public static List<Person> outgoing() {
		return outgoing;
	}

	public static List<Person> blocked() {
		return blocked;
	}

	public static List<Message> chat(UUID friend) {
		return CHATS.getOrDefault(friend, List.of());
	}

	public static int unread(UUID friend) {
		return UNREAD.getOrDefault(friend, 0);
	}

	public static int totalUnread() {
		return UNREAD.values().stream().mapToInt(Integer::intValue).sum();
	}

	public static void markRead(UUID friend) {
		UNREAD.remove(friend);
	}

	public static long onlineCount() {
		return friends.stream().filter(Friend::online).count();
	}

	/** Badges, nametags: safe from any thread. */
	public static boolean isFriend(UUID uuid) {
		return friendIds.contains(uuid);
	}

	public static boolean loaded() {
		return loaded;
	}

	/** Empty when all is well, else why the list may be out of date. */
	public static String status() {
		return status;
	}

	public static UUID me() {
		return Minecraft.getInstance().getUser().getProfileId();
	}

	// ---------------------------------------------------------------- polling

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		UUID me = me();
		if (!me.equals(account)) {
			// Another account (switched in-game): its friends, not the last one's.
			account = me;
			friends = List.of();
			incoming = outgoing = blocked = List.of();
			friendIds = Set.of();
			CHATS.clear();
			UNREAD.clear();
			lastMessage = 0;
			loaded = false;
			status = "Connecting...";
			nextPoll = 0;
		}
		long now = System.currentTimeMillis();
		if (polling || now < nextPoll) return;
		String server = CosmeticSync.server();
		if (server == null) {
			nextPoll = now + 2000;
			return;
		}
		nextPoll = now + (screenOpen || openChat != null ? FAST_POLL_MS : POLL_MS);
		StringBuilder path = new StringBuilder("/social/poll?since=").append(lastMessage).append("&server=").append(enc(presence(mc)));
		if (!ClientSettings.INSTANCE.friendsOnline.isOn()) path.append("&presence=0");
		polling = true;
		WORKER.execute(() -> {
			try {
				JsonObject result = call(server, path.toString(), null);
				mc.execute(() -> apply(me, result));
			} catch (Exception e) {
				String why = e instanceof ActionException ? e.getMessage() : "Can't reach Orbit's friends server";
				mc.execute(() -> {
					status = why;
					nextPoll = System.currentTimeMillis() + RETRY_MS;
				});
			} finally {
				polling = false;
			}
		});
	}

	/** What friends see you doing: a server address, singleplayer, or nothing (menus, or hidden). */
	private static String presence(Minecraft mc) {
		if (!ClientSettings.INSTANCE.shareServer.isOn()) return "";
		if (mc.getSingleplayerServer() != null) return "singleplayer";
		ServerData data = mc.getCurrentServer();
		if (data == null || data.isRealm() || mc.level == null) return "";
		String ip = data.ip.trim();
		return ip.matches("[A-Za-z0-9._:\\-]{1,100}") ? ip : "";
	}

	private static void apply(UUID me, JsonObject r) {
		if (!me.equals(account)) return; // switched accounts while this was on its way
		Minecraft mc = Minecraft.getInstance();
		boolean notify = loaded && ClientSettings.INSTANCE.friendNotifications.isOn();

		List<Friend> newFriends = new ArrayList<>();
		for (JsonElement e : array(r, "friends")) {
			JsonObject o = e.getAsJsonObject();
			UUID u = uuid(o);
			if (u != null) newFriends.add(new Friend(u, str(o, "name"), o.has("online") && o.get("online").getAsBoolean(), str(o, "server")));
		}
		newFriends.sort((a, b) -> a.online() != b.online() ? (a.online() ? -1 : 1) : a.name().compareToIgnoreCase(b.name()));
		List<Person> newIncoming = people(r, "incoming"), newOutgoing = people(r, "outgoing"), newBlocked = people(r, "blocked");

		if (notify) {
			Map<UUID, Friend> before = new HashMap<>();
			for (Friend f : friends) before.put(f.uuid(), f);
			Set<UUID> askedBefore = new HashSet<>();
			for (Person p : outgoing) askedBefore.add(p.uuid());
			for (Friend f : newFriends) {
				Friend old = before.get(f.uuid());
				if (old == null && askedBefore.contains(f.uuid())) toast(f.name() + " accepted your friend request", "You're friends now");
				else if (old != null && !old.online() && f.online()) toast(f.name() + " is online", f.status());
			}
			Set<UUID> requestsBefore = new HashSet<>();
			for (Person p : incoming) requestsBefore.add(p.uuid());
			for (Person p : newIncoming) {
				if (!requestsBefore.contains(p.uuid())) toast("Friend request from " + p.name(), "Open Friends in the Orbit menu to accept");
			}
		}

		Map<UUID, String> names = new HashMap<>();
		for (Friend f : newFriends) names.put(f.uuid(), f.name());
		for (JsonElement e : array(r, "messages")) {
			JsonObject o = e.getAsJsonObject();
			long id = o.get("id").getAsLong();
			UUID from = parse(str(o, "from")), to = parse(str(o, "to"));
			if (from == null || to == null || id <= lastMessage) continue;
			lastMessage = id;
			UUID other = from.equals(me) ? to : from;
			List<Message> chat = CHATS.computeIfAbsent(other, k -> new ArrayList<>());
			chat.add(new Message(id, from, to, str(o, "text"), o.get("time").getAsLong()));
			if (chat.size() > CHAT_HISTORY) chat.removeFirst();
			if (!from.equals(me) && loaded && !other.equals(openChat)) {
				UNREAD.merge(other, 1, Integer::sum);
				if (notify) toast(names.getOrDefault(other, "A friend"), str(o, "text"));
			}
		}

		friends = List.copyOf(newFriends);
		incoming = newIncoming;
		outgoing = newOutgoing;
		blocked = newBlocked;
		Set<UUID> ids = new HashSet<>();
		for (Friend f : newFriends) ids.add(f.uuid());
		friendIds = Set.copyOf(ids);
		// Chats of people who aren't friends anymore (unfriended, blocked) are dropped.
		CHATS.keySet().retainAll(ids);
		UNREAD.keySet().retainAll(ids);
		loaded = true;
		status = "";
	}

	/** For tests only: shows this poll result as if the server sent it, and stops polling. */
	public static void loadForTest(JsonObject poll) {
		account = me();
		nextPoll = Long.MAX_VALUE;
		apply(account, poll);
	}

	private static void toast(String title, String text) {
		Minecraft mc = Minecraft.getInstance();
		SystemToast.add(mc.getToastManager(), TOAST, Component.literal(title), Component.literal(text.length() > 80 ? text.substring(0, 79) + "…" : text));
	}

	// ---------------------------------------------------------------- actions

	/** Sends a friend request by Minecraft name; done gets null when sent, else why not. */
	public static void add(String name, Consumer<String> done) {
		action("/friends/request?name=" + enc(name.trim()), null, done);
	}

	public static void accept(UUID uuid, Consumer<String> done) {
		action("/friends/accept?uuid=" + uuid, null, done);
	}

	/** Unfriend, decline a request or cancel yours. */
	public static void remove(UUID uuid, Consumer<String> done) {
		action("/friends/remove?uuid=" + uuid, null, done);
	}

	public static void block(UUID uuid, Consumer<String> done) {
		action("/block?uuid=" + uuid, null, done);
	}

	public static void unblock(UUID uuid, Consumer<String> done) {
		action("/unblock?uuid=" + uuid, null, done);
	}

	public static void message(UUID to, String text, Consumer<String> done) {
		action("/messages/send?to=" + to, text, done);
	}

	private static void action(String path, String body, Consumer<String> done) {
		Minecraft mc = Minecraft.getInstance();
		String server = CosmeticSync.server();
		if (server == null) {
			done.accept("Orbit's friends server isn't reachable right now");
			return;
		}
		UUID me = me();
		WORKER.execute(() -> {
			String error = null;
			try {
				call(server, path, body);
			} catch (ActionException e) {
				error = e.getMessage();
			} catch (Exception e) {
				error = "Can't reach Orbit's friends server";
			}
			String result = error;
			mc.execute(() -> {
				if (!me.equals(account)) return;
				nextPoll = 0; // show the change right away
				done.accept(result);
			});
		});
	}

	// ---------------------------------------------------------------- HTTP

	/** A refusal from the server, with its message (e.g. "No Minecraft player is called X"). */
	private static final class ActionException extends IOException {
		ActionException(String message) {
			super(message);
		}
	}

	private static JsonObject call(String server, String path, String body) throws Exception {
		for (int attempt = 0; attempt < 2; attempt++) {
			String token = CosmeticSync.token(server);
			if (token == null) throw new ActionException("Couldn't sign in to Orbit's friends server (offline account?)");
			HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(server + path)).timeout(Duration.ofSeconds(10))
				.header("Authorization", "Bearer " + token).header("Content-Type", "text/plain; charset=utf-8")
				.POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build(), HttpResponse.BodyHandlers.ofString());
			if (r.statusCode() == 401 && attempt == 0) {
				CosmeticSync.invalidate(token);
				continue;
			}
			JsonObject json;
			try {
				json = JsonParser.parseString(r.body()).getAsJsonObject();
			} catch (Exception e) {
				// An old cosmetics server without these routes answers with a 404 page.
				throw new ActionException(r.statusCode() == 404 ? "Orbit's friends server needs an update" : "Friends server error (" + r.statusCode() + ")");
			}
			if (r.statusCode() >= 400) throw new ActionException(json.has("error") ? json.get("error").getAsString() : "Friends server error (" + r.statusCode() + ")");
			return json;
		}
		throw new ActionException("Couldn't sign in to Orbit's friends server");
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static JsonArray array(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
	}

	private static List<Person> people(JsonObject r, String key) {
		List<Person> list = new ArrayList<>();
		for (JsonElement e : array(r, key)) {
			UUID u = uuid(e.getAsJsonObject());
			if (u != null) list.add(new Person(u, str(e.getAsJsonObject(), "name")));
		}
		list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		return List.copyOf(list);
	}

	private static UUID uuid(JsonObject o) {
		return parse(str(o, "uuid"));
	}

	private static UUID parse(String s) {
		try {
			return UUID.fromString(s);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
	}
}
