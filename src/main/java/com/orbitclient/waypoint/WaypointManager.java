package com.orbitclient.waypoint;

import com.orbitclient.mixin.BiomeManagerAccessor;
import com.orbitclient.module.ModuleManager;
import com.orbitclient.module.server.WaypointsModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Waypoints for every server and singleplayer world, and for each world inside a server (a lobby,
 * survival, a second SMP behind the same address...). The world is recognized from the hashed seed
 * and spawn point the server sends (either is enough, so randomized seeds still work); unknown worlds get\n * a new entry. Picking a world by hand links it to where you are, so the choice sticks next time.
 */
public final class WaypointManager {
	private static final int[] COLORS = {0xFF55AAFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFAA00, 0xFFFF55FF, 0xFF55FFFF, 0xFFFFFF55};
	private static final List<Waypoint> ALL = new ArrayList<>();

	/**
	 * A world inside a server, recognized by any of the seeds or spawn points it was seen with
	 * (servers that randomize the seed per join still keep their spawn point).
	 */
	public static final class SubWorld {
		public final String server;
		public final String id;
		public String name;
		final Set<Long> seeds = new HashSet<>();
		final Set<String> spawns = new HashSet<>();
		/** Created by detection and never named, picked or given waypoints: safe to reuse or clean up. */
		boolean auto;

		SubWorld(String server, String id, String name) {
			this.server = server;
			this.id = id;
			this.name = name;
		}
	}

	/** How long to wait after a world change for the server to send its spawn point before deciding. */
	private static final long SETTLE_MS = 1500;
	private static final List<SubWorld> WORLDS = new ArrayList<>();
	/** The world in use on each server. */
	private static final Map<String, String> ACTIVE = new HashMap<>();
	private static ClientLevel detectedLevel;
	private static long levelSince;
	private static boolean decided;

	private WaypointManager() {
	}

	/** Key for the current server or singleplayer world, or null when not in a world. */
	public static String currentWorld() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		ServerData server = mc.getCurrentServer();
		return "server:" + (server == null ? "unknown" : server.ip);
	}

	public static String currentDimension() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? null : mc.level.dimension().identifier().toString();
	}

	private static long currentSeed() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? 0 : ((BiomeManagerAccessor) mc.level.getBiomeManager()).orbit$getSeed();
	}

	/** The world spawn the server sent, as "dimension x y z", or null if it hasn't sent one. */
	private static String currentSpawn() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		//? if >=1.21.9 {
		LevelData.RespawnData spawn = mc.level.getLevelData().getRespawnData();
		if (spawn == null || spawn.equals(LevelData.RespawnData.DEFAULT)) return null;
		BlockPos pos = spawn.pos();
		return spawn.dimension().identifier() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
		//?} else {
		/*// Before 1.21.9 the world spawn is always in the overworld, and unset is the origin.
		BlockPos pos = mc.level.getLevelData().getSpawnPos();
		if (pos.equals(BlockPos.ZERO)) return null;
		return "minecraft:overworld " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
		*///?}
	}

	private static boolean autoDetect() {
		WaypointsModule module = ModuleManager.get(WaypointsModule.class);
		return module == null || module.worldDetection.is("Auto");
	}

	/** Works out which world we're in after the level changes (joining, switching servers or worlds). */
	private static void detect() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			detectedLevel = null;
			return;
		}
		if (mc.level != detectedLevel) {
			detectedLevel = mc.level;
			levelSince = System.currentTimeMillis();
			decided = false;
		}
		if (decided) return;
		String server = currentWorld();
		long seed = currentSeed();
		SubWorld active = find(server, ACTIVE.get(server));
		// Same seed as the world we're already in (e.g. through a portal): no need to wait.
		boolean sameAsActive = seed != 0 && active != null && active.seeds.contains(seed);
		if (!sameAsActive && System.currentTimeMillis() - levelSince < SETTLE_MS) return;
		decided = true;
		String spawn = currentSpawn();

		SubWorld match = null;
		if (!autoDetect()) {
			match = active;
		} else {
			for (SubWorld w : worldsOf(server)) if (seed != 0 && w.seeds.contains(seed)) match = w;
			if (match == null && spawn != null) for (SubWorld w : worldsOf(server)) if (w.spawns.contains(spawn)) match = w;
			// Nothing known: reuse the current world if it's an empty one detection made, instead of piling up new ones.
			if (match == null && active != null && active.auto && countIn(active) == 0) {
				active.seeds.clear();
				active.spawns.clear();
				match = active;
			}
			if (match == null) {
				match = createWorld(server);
				match.auto = true;
			}
		}
		if (match == null) match = worldsOf(server).isEmpty() ? createWorld(server) : worldsOf(server).getFirst();
		link(match, seed, spawn);
		ACTIVE.put(server, match.id);
		// Waypoints from before worlds existed join the first world they're seen in.
		for (Waypoint w : ALL) if (w.subWorld == null && w.world.equals(server)) w.subWorld = match.id;
		// Clean up other empty worlds detection made that were never used.
		SubWorld keep = match;
		WORLDS.removeIf(w -> w.server.equals(server) && w != keep && w.auto && countIn(w) == 0);
	}

	/** Remembers this seed and spawn for the world, and forgets them for the server's other worlds. */
	private static void link(SubWorld world, long seed, String spawn) {
		for (SubWorld w : worldsOf(world.server)) {
			if (w == world) continue;
			w.seeds.remove(seed);
			if (spawn != null) w.spawns.remove(spawn);
		}
		if (seed != 0) world.seeds.add(seed);
		if (spawn != null) world.spawns.add(spawn);
	}

	private static SubWorld find(String server, String id) {
		if (id == null) return null;
		for (SubWorld w : WORLDS) if (w.server.equals(server) && w.id.equals(id)) return w;
		return null;
	}

	private static SubWorld createWorld(String server) {
		int n = worldsOf(server).size() + 1;
		while (true) {
			String name = "World " + n;
			if (worldsOf(server).stream().noneMatch(w -> w.name.equals(name))) break;
			n++;
		}
		SubWorld w = new SubWorld(server, UUID.randomUUID().toString().substring(0, 8), "World " + n);
		WORLDS.add(w);
		return w;
	}
	/** The worlds known for a server, in the order they were found. */
	public static List<SubWorld> worldsOf(String server) {
		List<SubWorld> list = new ArrayList<>();
		for (SubWorld w : WORLDS) if (w.server.equals(server)) list.add(w);
		return list;
	}

	/** The world inside the current server you're in, or null when not in a world. */
	public static SubWorld currentSubWorld() {
		detect();
		String server = currentWorld();
		return server == null || !decided ? null : find(server, ACTIVE.get(server));
	}

	/** True while waiting to recognize the world after a world change. */
	public static boolean detecting() {
		return currentWorld() != null && currentSubWorld() == null;
	}

	/** Switches to another world of this server by hand; it's linked to where you are, so next time it's picked automatically. */
	public static void selectSubWorld(SubWorld world) {
		String server = currentWorld();
		if (server == null || world == null || !world.server.equals(server)) return;
		world.auto = false;
		link(world, currentSeed(), currentSpawn());
		ACTIVE.put(server, world.id);
		decided = true;
	}

	public static void rename(SubWorld world, String name) {
		if (world == null || name.isBlank()) return;
		world.name = name.trim();
		world.auto = false;
	}

	/** Adds a new, empty world for where you are now and switches to it. */
	public static SubWorld newSubWorld() {
		String server = currentWorld();
		if (server == null) return null;
		SubWorld w = createWorld(server);
		selectSubWorld(w);
		return w;
	}
	/** Removes a world and its waypoints; the server always keeps at least one world. */
	public static void removeSubWorld(SubWorld world) {
		if (world == null || worldsOf(world.server).size() <= 1) return;
		ALL.removeIf(w -> w.world.equals(world.server) && world.id.equals(w.subWorld));
		WORLDS.remove(world);
		if (world.id.equals(ACTIVE.get(world.server))) ACTIVE.put(world.server, worldsOf(world.server).getFirst().id);
	}

	public static int countIn(SubWorld world) {
		int n = 0;
		for (Waypoint w : ALL) if (w.world.equals(world.server) && world.id.equals(w.subWorld)) n++;
		return n;
	}

	private static boolean here(Waypoint w, String server, SubWorld sub) {
		return w.world.equals(server) && sub != null && sub.id.equals(w.subWorld);
	}

	/** Waypoints of the current server and world (all dimensions), for the menu list. */
	public static List<Waypoint> inWorld() {
		SubWorld sub = currentSubWorld();
		String server = currentWorld();
		List<Waypoint> list = new ArrayList<>();
		for (Waypoint w : ALL) if (here(w, server, sub)) list.add(w);
		return list;
	}

	/** Waypoints to draw right now. */
	public static List<Waypoint> visibleHere() {
		SubWorld sub = currentSubWorld();
		String server = currentWorld(), dim = currentDimension();
		List<Waypoint> list = new ArrayList<>();
		for (Waypoint w : ALL) if (w.visible && here(w, server, sub) && w.dimension.equals(dim)) list.add(w);
		return list;
	}

	public static Waypoint addAtPlayer(String name) {
		Minecraft mc = Minecraft.getInstance();
		SubWorld sub = currentSubWorld();
		if (mc.player == null || sub == null) return null;
		int color = COLORS[ALL.size() % COLORS.length];
		Waypoint w = new Waypoint(name, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(),
			currentWorld(), currentDimension(), color);
		w.subWorld = sub.id;
		sub.auto = false;
		ALL.add(w);
		return w;
	}

	public static void remove(Waypoint w) {
		ALL.remove(w);
	}

	public static int nextColor(int color) {
		for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == color) return COLORS[(i + 1) % COLORS.length];
		return COLORS[0];
	}

	public static JsonArray save() {
		JsonArray arr = new JsonArray();
		for (Waypoint w : ALL) {
			JsonObject o = new JsonObject();
			o.addProperty("name", w.name);
			o.addProperty("x", w.x);
			o.addProperty("y", w.y);
			o.addProperty("z", w.z);
			o.addProperty("world", w.world);
			if (w.subWorld != null) o.addProperty("subWorld", w.subWorld);
			o.addProperty("dimension", w.dimension);
			o.addProperty("color", w.color);
			o.addProperty("visible", w.visible);
			o.addProperty("chroma", w.chroma);
			o.addProperty("icon", w.icon);
			o.addProperty("beam", w.beam);
			o.addProperty("edge", w.edge);
			arr.add(o);
		}
		return arr;
	}

	public static void load(JsonArray arr) {
		ALL.clear();
		for (JsonElement e : arr) {
			JsonObject o = e.getAsJsonObject();
			Waypoint w = new Waypoint(o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(),
				o.get("z").getAsInt(), o.get("world").getAsString(), o.get("dimension").getAsString(), o.get("color").getAsInt());
			w.subWorld = o.has("subWorld") ? o.get("subWorld").getAsString() : null;
			w.visible = !o.has("visible") || o.get("visible").getAsBoolean();
			w.chroma = o.has("chroma") && o.get("chroma").getAsBoolean();
			if (o.has("icon")) w.icon = o.get("icon").getAsString();
			w.beam = !o.has("beam") || o.get("beam").getAsBoolean();
			w.edge = !o.has("edge") || o.get("edge").getAsBoolean();
			// Added while in a world (e.g. by tests) without a world inside the server: use the current one.
			if (w.subWorld == null && w.world.equals(currentWorld()) && currentSubWorld() != null) w.subWorld = currentSubWorld().id;
			ALL.add(w);
		}
	}

	/** The worlds inside servers and which one is in use on each. */
	public static JsonObject saveWorlds() {
		JsonObject root = new JsonObject();
		JsonArray arr = new JsonArray();
		for (SubWorld w : WORLDS) {
			JsonObject o = new JsonObject();
			o.addProperty("server", w.server);
			o.addProperty("id", w.id);
			o.addProperty("name", w.name);
			JsonArray seeds = new JsonArray();
			w.seeds.forEach(seeds::add);
			o.add("seeds", seeds);
			JsonArray spawns = new JsonArray();
			w.spawns.forEach(spawns::add);
			o.add("spawns", spawns);
			if (w.auto) o.addProperty("auto", true);
			arr.add(o);
		}
		root.add("worlds", arr);
		JsonObject active = new JsonObject();
		ACTIVE.forEach(active::addProperty);
		root.add("active", active);
		return root;
	}

	public static void loadWorlds(JsonObject root) {
		WORLDS.clear();
		ACTIVE.clear();
		detectedLevel = null;
		if (root.has("worlds")) {
			for (JsonElement e : root.getAsJsonArray("worlds")) {
				JsonObject o = e.getAsJsonObject();
				SubWorld w = new SubWorld(o.get("server").getAsString(), o.get("id").getAsString(), o.get("name").getAsString());
				if (o.has("seed")) w.seeds.add(o.get("seed").getAsLong());
				if (o.has("seeds")) o.getAsJsonArray("seeds").forEach(e2 -> w.seeds.add(e2.getAsLong()));
				if (o.has("spawns")) o.getAsJsonArray("spawns").forEach(e2 -> w.spawns.add(e2.getAsString()));
				w.auto = o.has("auto") && o.get("auto").getAsBoolean();
				WORLDS.add(w);
			}
		}
		if (root.has("active")) root.getAsJsonObject("active").entrySet().forEach(en -> ACTIVE.put(en.getKey(), en.getValue().getAsString()));
	}
}
