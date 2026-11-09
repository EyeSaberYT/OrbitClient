package com.orbitclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
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

	@Mutable
	@Accessor("services")
	void orbit$setServices(Services services);

	@Mutable
	@Accessor("profileFuture")
	void orbit$setProfileFuture(CompletableFuture<ProfileResult> profile);

	@Mutable
	@Accessor("userApiService")
	void orbit$setUserApiService(UserApiService service);

	@Mutable
	@Accessor("userPropertiesFuture")
	void orbit$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> properties);

	@Mutable
	@Accessor("playerSocialManager")
	void orbit$setPlayerSocialManager(PlayerSocialManager manager);

	@Mutable
	@Accessor("telemetryManager")
	void orbit$setTelemetryManager(ClientTelemetryManager manager);

	@Mutable
	@Accessor("profileKeyPairManager")
	void orbit$setProfileKeyPairManager(ProfileKeyPairManager manager);

	@Mutable
	@Accessor("reportingContext")
	void orbit$setReportingContext(ReportingContext context);
}
