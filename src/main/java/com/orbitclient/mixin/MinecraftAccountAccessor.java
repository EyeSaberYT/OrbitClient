package com.orbitclient.mixin;

//? if >=1.18
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
//? if >=1.16.4
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
//? if >=1.19
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
//? if >=1.19.1
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
//? if >=1.19.3
import net.minecraft.client.telemetry.ClientTelemetryManager;
//? if >=1.21.9
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

/** Everything in Minecraft that belongs to the signed-in account, so it can be swapped (see SessionSwitcher). */
@Mixin(Minecraft.class)
public interface MinecraftAccountAccessor {
	@Mutable
	@Accessor("user")
	void orbit$setUser(User user);

	//? if >=1.21.9 {
	@Mutable
	@Accessor("services")
	void orbit$setServices(Services services);
	//?} else {
	/*// Before 1.21.9 the session services were two fields instead of one Services.
	@Mutable
	@Accessor("authenticationService")
	void orbit$setAuthenticationService(com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService service);

	@Mutable
	@Accessor("minecraftSessionService")
	void orbit$setSessionService(com.mojang.authlib.minecraft.MinecraftSessionService service);
	*///?}

	@Mutable
	@Accessor("profileFuture")
	void orbit$setProfileFuture(CompletableFuture<ProfileResult> profile);

	//? if >=1.18 {
	@Mutable
	@Accessor("userApiService")
	void orbit$setUserApiService(UserApiService service);

	@Mutable
	@Accessor("userPropertiesFuture")
	void orbit$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> properties);
	//?}

	//? if >=1.16.4 {
	@Mutable
	@Accessor("playerSocialManager")
	void orbit$setPlayerSocialManager(PlayerSocialManager manager);
	//?}

	//? if >=1.19.3 {
	@Mutable
	@Accessor("telemetryManager")
	void orbit$setTelemetryManager(ClientTelemetryManager manager);
	//?}

	//? if >=1.19 {
	@Mutable
	@Accessor("profileKeyPairManager")
	void orbit$setProfileKeyPairManager(ProfileKeyPairManager manager);
	//?}

	//? if >=1.19.1 {
	@Mutable
	@Accessor("reportingContext")
	void orbit$setReportingContext(ReportingContext context);
	//?}
}
