package com.orbitclient.config;

import com.orbitclient.OrbitClient;
import com.orbitclient.module.HudModule;
import com.orbitclient.module.Module;
import com.orbitclient.module.ModuleManager;
import com.orbitclient.setting.Setting;
import com.orbitclient.waypoint.WaypointManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves to config/orbitclient/:
 * global.json holds the active profile, client settings and waypoints;
 * profiles/NAME.json holds each profile's mods, positions and mod settings.
 */
public final class ConfigManager {
	public static final String DEFAULT_PROFILE = "Default";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("orbitclient");
	private static final Path PROFILES = DIR.resolve("profiles");
	private static final Path GLOBAL = DIR.resolve("global.json");

	private static String activeProfile = DEFAULT_PROFILE;

	private ConfigManager() {
	}

	// ---------------------------------------------------------------- Eclipse Client → Orbit Client

	/**
	 * First start after the rename: copies config/eclipseclient/ (settings, profiles, waypoints, cosmetics)
	 * to config/orbitclient/ and renames the two key binds in options.txt. The old folder is left as it was.
	 */
	public static void migrateFromEclipse() {
		Path old = FabricLoader.getInstance().getConfigDir().resolve("eclipseclient");
		if (Files.exists(DIR) || !Files.isDirectory(old)) return;
		try (Stream<Path> paths = Files.walk(old)) {
			for (Path from : (Iterable<Path>) paths::iterator) {
				Path to = DIR.resolve(old.relativize(from).toString());
				if (Files.isDirectory(from)) Files.createDirectories(to);
				else Files.copy(from, to);
			}
			OrbitClient.LOGGER.info("Copied Eclipse Client settings to {}", DIR);
		} catch (IOException e) {
			OrbitClient.LOGGER.error("Could not copy Eclipse Client settings", e);
		}
		Path options = FabricLoader.getInstance().getGameDir().resolve("options.txt");
		try {
			if (Files.exists(options)) {
				String text = Files.readString(options);
				if (text.contains("key_key.eclipseclient.")) Files.writeString(options, text.replace("key_key.eclipseclient.", "key_key.orbitclient."));
			}
		} catch (IOException e) {
			OrbitClient.LOGGER.error("Could not carry over Eclipse Client key binds", e);
		}
	}

	// ---------------------------------------------------------------- save / load

	public static void save() {
		saveProfile(activeProfile);
		saveGlobal();
	}

	public static void load() {
		loadGlobal();
		if (!Files.exists(profilePath(activeProfile))) activeProfile = DEFAULT_PROFILE;
		loadProfile(activeProfile);
	}

	private static void saveGlobal() {
		JsonObject root = new JsonObject();
		root.addProperty("profile", activeProfile);
		root.add("client", settingsJson(ClientSettings.INSTANCE));
		root.add("waypoints", WaypointManager.save());
		root.add("waypointWorlds", WaypointManager.saveWorlds());
		root.add("cosmetics", com.orbitclient.cosmetics.CosmeticManager.save());
		write(GLOBAL, root);
	}

	private static void loadGlobal() {
		JsonObject root = read(GLOBAL);
		if (root == null) return;
		if (root.has("profile")) activeProfile = root.get("profile").getAsString();
		if (root.has("client")) loadSettings(ClientSettings.INSTANCE, root.getAsJsonObject("client"));
		if (root.has("waypointWorlds")) WaypointManager.loadWorlds(root.getAsJsonObject("waypointWorlds"));
		if (root.has("cosmetics")) com.orbitclient.cosmetics.CosmeticManager.load(root.get("cosmetics"));
		if (root.has("waypoints")) WaypointManager.load(root.getAsJsonArray("waypoints"));
	}

	private static void saveProfile(String name) {
		JsonObject root = new JsonObject();
		for (Module module : ModuleManager.all()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("enabled", module.isEnabled());
			obj.addProperty("favorite", module.favorite);
			if (module instanceof HudModule hud) {
				obj.addProperty("x", hud.getXPercent());
				obj.addProperty("y", hud.getYPercent());
			}
			obj.add("settings", settingsJson(module));
			root.add(module.getName(), obj);
		}
		write(profilePath(name), root);
	}

	private static void loadProfile(String name) {
		JsonObject root = read(profilePath(name));
		if (root == null) return;
		for (Module module : ModuleManager.all()) {
			if (!root.has(module.getName())) continue;
			try {
				JsonObject obj = root.getAsJsonObject(module.getName());
				if (obj.has("settings")) loadSettings(module, obj.getAsJsonObject("settings"));
				if (module instanceof HudModule hud && obj.has("x") && obj.has("y")) {
					hud.setPercent(obj.get("x").getAsDouble(), obj.get("y").getAsDouble());
				}
				if (obj.has("favorite")) module.favorite = obj.get("favorite").getAsBoolean();
				if (obj.has("enabled")) module.setEnabled(obj.get("enabled").getAsBoolean());
			} catch (Exception e) {
				OrbitClient.LOGGER.warn("Skipping broken config for {}", module.getName(), e);
			}
		}
	}

	private static JsonObject settingsJson(Module module) {
		JsonObject settings = new JsonObject();
		for (Setting<?> setting : module.getSettings()) settings.add(setting.getName(), setting.save());
		return settings;
	}

	private static void loadSettings(Module module, JsonObject settings) {
		for (Setting<?> setting : module.getSettings()) {
			if (settings.has(setting.getName())) setting.load(settings.get(setting.getName()));
		}
	}

	private static void write(Path path, JsonObject json) {
		if (path.startsWith(PROFILES) && !Files.exists(path)) profileCache = null;
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(json));
		} catch (IOException e) {
			OrbitClient.LOGGER.error("Could not save {}", path, e);
		}
	}

	private static JsonObject read(Path path) {
		if (!Files.exists(path)) return null;
		try {
			return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
		} catch (Exception e) {
			OrbitClient.LOGGER.error("Could not read {}, using defaults", path, e);
			return null;
		}
	}

	// ---------------------------------------------------------------- profiles

	private static Path profilePath(String name) {
		return PROFILES.resolve(name + ".json");
	}

	public static String activeProfile() {
		return activeProfile;
	}

	/** Cached so menus can show it every frame without touching the disk; cleared whenever profiles change. */
	private static List<String> profileCache;

	/** Default first, then the rest alphabetically. */
	public static List<String> profiles() {
		if (profileCache == null || !profileCache.contains(activeProfile)) profileCache = listProfiles();
		return profileCache;
	}

	private static List<String> listProfiles() {
		List<String> names = new ArrayList<>();
		names.add(DEFAULT_PROFILE);
		if (Files.isDirectory(PROFILES)) {
			try (Stream<Path> files = Files.list(PROFILES)) {
				files.map(p -> p.getFileName().toString())
					.filter(n -> n.endsWith(".json"))
					.map(n -> n.substring(0, n.length() - 5))
					.filter(n -> !n.equals(DEFAULT_PROFILE))
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.forEach(names::add);
			} catch (IOException e) {
				OrbitClient.LOGGER.error("Could not list profiles", e);
			}
		}
		if (!names.contains(activeProfile)) names.add(activeProfile);
		return names;
	}

	public static void switchProfile(String name) {
		if (name.equals(activeProfile)) return;
		saveProfile(activeProfile);
		for (Module module : ModuleManager.all()) module.resetToDefaults();
		activeProfile = name;
		loadProfile(name);
		saveGlobal();
	}

	/** Copies the current setup into a new profile and switches to it. */
	public static String createProfile() {
		saveProfile(activeProfile);
		int n = 1;
		while (Files.exists(profilePath("Profile " + n))) n++;
		activeProfile = "Profile " + n;
		save();
		return activeProfile;
	}

	public static boolean renameProfile(String from, String to) {
		to = sanitize(to);
		if (to.isEmpty() || from.equals(DEFAULT_PROFILE) || to.equals(DEFAULT_PROFILE) || Files.exists(profilePath(to))) return false;
		try {
			saveProfile(activeProfile);
			Files.move(profilePath(from), profilePath(to));
			profileCache = null;
			if (activeProfile.equals(from)) activeProfile = to;
			saveGlobal();
			return true;
		} catch (IOException e) {
			OrbitClient.LOGGER.error("Could not rename profile", e);
			return false;
		}
	}

	public static void deleteProfile(String name) {
		if (name.equals(DEFAULT_PROFILE)) return;
		if (name.equals(activeProfile)) switchProfile(DEFAULT_PROFILE);
		try {
			Files.deleteIfExists(profilePath(name));
			profileCache = null;
		} catch (IOException e) {
			OrbitClient.LOGGER.error("Could not delete profile", e);
		}
		saveGlobal();
	}

	/** Profile names become file names, so keep them simple. */
	public static String sanitize(String name) {
		return name.replaceAll("[^A-Za-z0-9 _\\-]", "").trim();
	}
}
