package com.orbitclient.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.vidtu.ias.IASMinecraft;
import ru.vidtu.ias.auth.LoginData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Account picker backed by Orbit Launcher's accounts.json. Never displays or logs tokens. */
public final class LauncherAccountsScreen extends Screen {
	private static final String MICROSOFT_CLIENT_ID = "25257470-05eb-4959-bc32-6b6bdb883bac";
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private final Screen parent;
	private final List<JsonObject> accounts = new ArrayList<>();
	private String status = "";
	private Path accountsFile;

	public LauncherAccountsScreen(Screen parent) {
		super(Component.literal("Orbit Accounts"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		accounts.clear();
		loadAccounts();
		int top = Math.max(52, height / 2 - Math.min(accounts.size(), 8) * 14);
		for (int i = 0; i < Math.min(accounts.size(), 8); i++) {
			JsonObject account = accounts.get(i);
			String name = safeString(account, "username", "Unknown account");
			boolean active = safeString(JsonParser.parseString(readRoot()).getAsJsonObject(), "active", "")
				.equals(safeString(account, "uuid", ""));
			String label = active ? "✓  " + name + "  (active)" : "Switch to  " + name;
			int y = top + i * 28;
			addRenderableWidget(Button.builder(Component.literal(label), button -> switchTo(account))
				.bounds(width / 2 - 120, y, 240, 22).build());
		}
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
			.bounds(width / 2 - 100, height - 36, 200, 20).build());
		if (accounts.isEmpty() && status.isEmpty()) status = "No accounts found in Orbit Launcher's accounts.json";
	}

	private void loadAccounts() {
		try {
			String appData = System.getenv("APPDATA");
			if (appData == null || appData.isBlank()) throw new IllegalStateException("APPDATA is not set");
			accountsFile = Path.of(appData, ".orbitlauncher", "accounts.json");
			JsonObject root = JsonParser.parseString(Files.readString(accountsFile)).getAsJsonObject();
			JsonArray list = root.getAsJsonArray("accounts");
			if (list != null) for (JsonElement entry : list) if (entry.isJsonObject()) accounts.add(entry.getAsJsonObject());
		} catch (Exception e) {
			status = "Couldn't read %APPDATA%\\.orbitlauncher\\accounts.json";
		}
	}

	private String readRoot() {
		try { return Files.readString(accountsFile); }
		catch (Exception e) { return "{}"; }
	}

	private void switchTo(JsonObject account) {
		try {
			String username = safeString(account, "username", "");
			String uuid = safeString(account, "uuid", "");
			long expiry = account.has("expiresAt") ? account.get("expiresAt").getAsLong() : 0;
			if (username.isBlank() || uuid.isBlank()) throw new IllegalArgumentException("Missing account identity");
			status = "Signing in as " + username + "…";
			CompletableFuture.runAsync(() -> {
				try {
					String token = safeString(account, "accessToken", "");
					String refresh = safeString(account, "refreshToken", "");
					long expiryNow = expiry;
					String newRefresh = refresh;
					if (token.isBlank() || expiryNow <= System.currentTimeMillis() / 1000L + 300) {
						if (refresh.isBlank()) throw new IllegalStateException("No refresh token");
						var renewed = renew(refresh);
						token = renewed.accessToken;
						newRefresh = renewed.refreshToken;
						expiryNow = System.currentTimeMillis() / 1000L + renewed.expiresIn;
					}
					String sessionToken = token;
					String rotatedRefresh = newRefresh;
					long expiresAt = expiryNow;
					IASMinecraft.account(Minecraft.getInstance(), new LoginData(username, UUID.fromString(dashUuid(uuid)), sessionToken, true))
						.whenComplete((ignored, error) -> Minecraft.getInstance().execute(() -> {
							if (error != null) {
								status = "Sign-in failed. Make sure Minecraft is at the title screen.";
								return;
							}
							try {
								JsonObject root = JsonParser.parseString(Files.readString(accountsFile)).getAsJsonObject();
								root.addProperty("active", uuid);
								for (JsonElement item : root.getAsJsonArray("accounts")) {
									JsonObject saved = item.getAsJsonObject();
									if (!uuid.equals(safeString(saved, "uuid", ""))) continue;
									saved.addProperty("accessToken", sessionToken);
									saved.addProperty("expiresAt", expiresAt);
									if (!rotatedRefresh.isBlank()) saved.addProperty("refreshToken", rotatedRefresh);
								}
								Path temp = accountsFile.resolveSibling("accounts.json.tmp");
								Files.writeString(temp, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));
								try { Files.move(temp, accountsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
								catch (java.nio.file.AtomicMoveNotSupportedException ignoredMove) { Files.move(temp, accountsFile, StandardCopyOption.REPLACE_EXISTING); }
								Minecraft.getInstance().setScreen(null);
							} catch (Exception saveError) {
								status = "Signed in, but couldn't update the launcher account file.";
							}
						}));
				} catch (Exception e) {
					Minecraft.getInstance().execute(() -> status = "Account sign-in failed. Sign in again with Orbit Launcher if needed.");
				}
			});
		} catch (Exception e) {
			status = "Couldn't switch accounts. Check the launcher account file.";
		}
	}

	private static RefreshedToken renew(String refreshToken) throws Exception {
		JsonObject ms = postForm("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", "client_id=" + enc(MICROSOFT_CLIENT_ID)
			+ "&grant_type=refresh_token&refresh_token=" + enc(refreshToken) + "&scope=" + enc("XboxLive.signin offline_access"));
		String xblTicket = "d=" + ms.get("access_token").getAsString();
		JsonObject xbl = postJson("https://user.auth.xboxlive.com/user/authenticate", "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":" + quote(xblTicket) + "},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}");
		String xboxToken = xbl.get("Token").getAsString();
		String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
		JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[" + quote(xboxToken) + "]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}");
		JsonObject mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", "{\"identityToken\":" + quote("XBL3.0 x=" + uhs + ";" + xsts.get("Token").getAsString()) + "}");
		return new RefreshedToken(mc.get("access_token").getAsString(), ms.has("refresh_token") ? ms.get("refresh_token").getAsString() : refreshToken, ms.get("expires_in").getAsLong());
	}

	private static JsonObject postForm(String url, String body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		return response(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
	}

	private static JsonObject postJson(String url, String body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		return response(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
	}

	private static JsonObject response(HttpResponse<String> response) {
		if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Authentication request failed (HTTP " + response.statusCode() + ")");
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
	private static String quote(String value) { return new com.google.gson.Gson().toJson(value); }
	private record RefreshedToken(String accessToken, String refreshToken, long expiresIn) {}

	private static String dashUuid(String value) {
		String raw = value.replace("-", "");
		if (raw.length() != 32) return value;
		return raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16) + "-"
			+ raw.substring(16, 20) + "-" + raw.substring(20);
	}

	private static String safeString(JsonObject object, String key, String fallback) {
		return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// The background (and its blur) is already drawn before render(); drawing it again crashes.
		graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFFFF);
		if (!status.isBlank()) graphics.drawCenteredString(font, Component.literal(status), width / 2, height / 2 + 75, 0xFFFFC66D);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
