package com.orbitclient.account;

//? if <26.2 {
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.orbitclient.mixin.MinecraftAccountAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.server.Services;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Signs the running game in as another Microsoft account, without restarting it. Builds everything that
 * belongs to an account off the main thread (session services, profile and skin, chat signing keys,
 * social and reporting), then swaps it all into Minecraft at once on the main thread, between frames.
 * Only from the menus: a world or server keeps the account it joined with. Up to 26.1: 26.2 added Mojang's
 * friends service to the signed-in session and 26.3 moved authlib's services, which this doesn't handle yet.
 */
public final class SessionSwap {
	private SessionSwap() {
	}

	/** Completes on the main thread once the game uses the new account. The token must be a valid Minecraft access token. */
	public static CompletableFuture<Void> switchTo(String name, UUID uuid, String accessToken) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null || mc.getConnection() != null) {
			return CompletableFuture.failedFuture(new IllegalStateException("Leave the world before switching accounts"));
		}
		CompletableFuture<Void> done = new CompletableFuture<>();
		CompletableFuture.runAsync(() -> {
			User user = new User(name, uuid, accessToken, Optional.empty(), Optional.empty());
			YggdrasilAuthenticationService auth = new YggdrasilAuthenticationService(mc.getProxy());
			Services services = Services.create(auth, mc.gameDirectory);
			ProfileResult profile = services.sessionService().fetchProfile(uuid, true);
			UserApiService userApi;
			try {
				userApi = auth.createUserApiService(accessToken);
			} catch (RuntimeException e) {
				// Same as the game at startup: without it the account still plays, with chat reporting and social off.
				userApi = UserApiService.OFFLINE;
			}
			UserApiService.UserProperties properties;
			try {
				properties = userApi.fetchProperties();
			} catch (Exception e) {
				properties = UserApiService.OFFLINE_PROPERTIES;
			}
			PlayerSocialManager social = new PlayerSocialManager(mc, userApi);
			ClientTelemetryManager telemetry = new ClientTelemetryManager(mc, userApi, user);
			ProfileKeyPairManager keys = ProfileKeyPairManager.create(userApi, user, mc.gameDirectory.toPath());
			ReportingContext reporting = ReportingContext.create(ReportEnvironment.local(), userApi);
			UserApiService finalUserApi = userApi;
			UserApiService.UserProperties finalProperties = properties;

			mc.execute(() -> {
				MinecraftAccountAccessor game = (MinecraftAccountAccessor) mc;
				game.orbit$setServices(services);
				game.orbit$setUser(user);
				game.orbit$setProfileFuture(CompletableFuture.completedFuture(profile));
				game.orbit$setUserApiService(finalUserApi);
				game.orbit$setUserPropertiesFuture(CompletableFuture.completedFuture(finalProperties));
				game.orbit$setPlayerSocialManager(social);
				game.orbit$setTelemetryManager(telemetry);
				game.orbit$setProfileKeyPairManager(keys);
				game.orbit$setReportingContext(reporting);
				mc.updateTitle();
				done.complete(null);
			});
		}).exceptionally(error -> {
			done.completeExceptionally(error);
			return null;
		});
		return done;
	}
}
//?}
