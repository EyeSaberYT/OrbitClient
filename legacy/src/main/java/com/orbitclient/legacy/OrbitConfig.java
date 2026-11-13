package com.orbitclient.legacy;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbitclient.cosmetics.CosmeticManager;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** What the legacy client remembers between runs: config/orbitclient.json in the game folder. */
public final class OrbitConfig {
	/** Draw other Orbit Client players' cosmetics. */
	public static volatile boolean showOthers = true;

	private OrbitConfig() {
	}

	private static File file() {
		return new File(new File(Game.runDirectory(MinecraftClient.getInstance()), "config"), "orbitclient.json");
	}

	public static void load() {
		try {
			File file = file();
			if (!file.isFile()) return;
			JsonObject root = new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
			if (root.has("showOthersCosmetics")) showOthers = root.get("showOthersCosmetics").getAsBoolean();
			CosmeticManager.load(root.get("cosmetics"));
		} catch (Exception e) {
			// A damaged config starts the client with nothing worn; the file is rewritten on the next change.
			System.err.println("[Orbit Client] Couldn't read the config: " + e);
		}
	}

	public static synchronized void save() {
		try {
			JsonObject root = new JsonObject();
			root.addProperty("showOthersCosmetics", showOthers);
			root.add("cosmetics", CosmeticManager.save());
			File file = file();
			file.getParentFile().mkdirs();
			File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
			Files.write(tmp.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8));
			Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			System.err.println("[Orbit Client] Couldn't save the config: " + e);
		}
	}
}
