package com.orbitclient.mixin;

import com.orbitclient.module.server.ScreenshotModule;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.util.function.Consumer;

@Mixin(Screenshot.class)
public class ScreenshotMixin {
	/** Screenshot: the chat message gets OPEN / FOLDER buttons and the image can go to the clipboard. */
	@ModifyVariable(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
		at = @At("HEAD"), argsOnly = true)
	private static Consumer<Component> orbit$screenshotExtras(Consumer<Component> callback, File gameDir) {
		return ScreenshotModule.wrap(gameDir, callback);
	}
}
