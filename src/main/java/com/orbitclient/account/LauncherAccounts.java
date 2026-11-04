package com.orbitclient.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import ru.vidtu.ias.IASMinecraft;
import ru.vidtu.ias.auth.LoginData;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The accounts Orbit Launcher signed in (%APPDATA%\.orbitlauncher\accounts.json), and switching the
 * running game to one of them. Tokens are renewed through Microsoft when they're about to expire and
 * written back to the launcher's file. Tokens are never shown or logged.
 */
public final class LauncherAccounts {
	private static final String MICROSOFT_CLIENT_ID = "25257470-05eb-4959-bc32-6b6bdb883bac";
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	public record Account(String name, UUID uuid, String rawUuid) {
	}

	private LauncherAccounts() {
	}

	public static Path file() {
		String appData = System.getenv("APPDATA");
		if (appData == null || appData.isBlank()) throw new IllegalStateException("APPDATA is not set");
		return Path.of(appData, ".orbitlauncher", "accounts.json");
	}

	/** Every account in the launcher's file; throws if the file can't be read. */
	public static List<Account> load() throws Exception {
		List<Account> accounts = new ArrayList<>();
		JsonArray list = readRoot().getAsJsonArray("accounts");
		if (list == null) return accounts;
		for (JsonElement entry : list) {
			if (!entry.isJsonObject()) continue;
			JsonObject o = entry.getAsJsonObject();
			String name = string(o, "username"), uuid = string(o, "uuid");
			if (name.isBlank() || uuid.isBlank()) continue;
			try {
				accounts.add(new Account(name, UUID.fromString(dashUuid(uuid)), uuid));
			} catch (IllegalArgumentException ignored) {
				// Not a UUID: skip it.
			}
		}
		return accounts;
	}

	/** Signs the game in as this account. May complete on any thread. */
	public static CompletableFuture<Void> switchTo(Account account) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				JsonObject saved = find(readRoot(), account.rawUuid());
				if (saved == null) throw new IllegalStateException("Account is no longer in the launcher");
				String token = string(saved, "accessToken"), refresh = string(saved, "refreshToken");
				long expiresAt = saved.has("expiresAt") && !saved.get("expiresAt").isJsonNull() ? saved.get("expiresAt").getAsLong() : 0;
				if (token.isBlank() || expiresAt <= System.currentTimeMillis() / 1000L + 300) {
					if (refresh.isBlank()) throw new IllegalStateException("No refresh token");
					Tokens renewed = renew(refresh);
					return renewed;
				}
				return new Tokens(token, refresh, expiresAt);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}).thenCompose(tokens -> IASMinecraft.account(Minecraft.getInstance(), new LoginData(account.name(), account.uuid(), tokens.access, true))
			.thenRun(() -> save(account, tokens)));
	}

	private record Tokens(String access, String refresh, long expiresAt) {
	}

	/** Marks the account active in the launcher and keeps the (possibly renewed) tokens. */
	private static void save(Account account, Tokens tokens) {
		try {
			Path file = file();
			JsonObject root = readRoot();
			root.addProperty("active", account.rawUuid());
			JsonObject saved = find(root, account.rawUuid());
			if (saved != null) {
				saved.addProperty("accessToken", tokens.access);
				saved.addProperty("expiresAt", tokens.expiresAt);
				if (!tokens.refresh.isBlank()) saved.addProperty("refreshToken", tokens.refresh);
			}
			Path temp = file.resolveSibling("accounts.json.tmp");
			Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			// The game is signed in either way; the launcher keeps its old active account.
		}
	}

	private static JsonObject readRoot() throws Exception {
		return JsonParser.parseString(Files.readString(file())).getAsJsonObject();
	}

	private static JsonObject find(JsonObject root, String rawUuid) {
		JsonArray list = root.getAsJsonArray("accounts");
		if (list == null) return null;
		for (JsonElement e : list) {
			if (e.isJsonObject() && rawUuid.equals(string(e.getAsJsonObject(), "uuid"))) return e.getAsJsonObject();
		}
		return null;
	}

	// ---------------------------------------------------------------- Microsoft → Xbox → Minecraft

	private static Tokens renew(String refreshToken) throws Exception {
		JsonObject ms = postForm("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", "client_id=" + enc(MICROSOFT_CLIENT_ID)
			+ "&grant_type=refresh_token&refresh_token=" + enc(refreshToken) + "&scope=" + enc("XboxLive.signin offline_access"));
		String xblTicket = "d=" + ms.get("access_token").getAsString();
		JsonObject xbl = postJson("https://user.auth.xboxlive.com/user/authenticate", "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":" + quote(xblTicket) + "},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}");
		String xboxToken = xbl.get("Token").getAsString();
		String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
		JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[" + quote(xboxToken) + "]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}");
		JsonObject mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", "{\"identityToken\":" + quote("XBL3.0 x=" + uhs + ";" + xsts.get("Token").getAsString()) + "}");
		String refresh = ms.has("refresh_token") ? ms.get("refresh_token").getAsString() : refreshToken;
		return new Tokens(mc.get("access_token").getAsString(), refresh, System.currentTimeMillis() / 1000L + ms.get("expires_in").getAsLong());
	}

	private static JsonObject postForm(String url, String body) throws Exception {
		return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(body)).build());
	}

	private static JsonObject postJson(String url, String body) throws Exception {
		return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body)).build());
	}

	private static JsonObject send(HttpRequest request) throws Exception {
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Authentication request failed (HTTP " + response.statusCode() + ")");
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	private static String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String quote(String value) {
		return new Gson().toJson(value);
	}

	private static String dashUuid(String value) {
		String raw = value.replace("-", "");
		if (raw.length() != 32) return value;
		return raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
	}

	private static String string(JsonObject o, String key) {
		return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
	}
}
