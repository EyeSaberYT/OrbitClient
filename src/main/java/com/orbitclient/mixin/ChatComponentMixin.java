package com.orbitclient.mixin;

import com.orbitclient.util.VanillaAnimations;
import net.minecraft.client.gui.components.ChatComponent;
import com.orbitclient.module.server.ChatModule;
import com.orbitclient.module.server.NickHiderModule;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	/** Every visible chat line goes through here; remember when, so chat can slide up. */
	//? if >=1.20.5 {
	@Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
	private void orbit$chatSlide(CallbackInfo ci) {
		VanillaAnimations.onChatMessage();
	}
	//?} else {
	/*// Before 1.20.5 one method adds new lines and re-wraps the old ones (onlyTrim): only new lines slide.
	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("HEAD"))
	private void orbit$chatSlide(Component message, net.minecraft.network.chat.MessageSignature signature, int addedTime,
	                             net.minecraft.client.GuiMessageTag tag, boolean onlyTrim, CallbackInfo ci) {
		if (!onlyTrim) VanillaAnimations.onChatMessage();
	}
	*///?}

	/** Chat and Nick Hider: hide names, add timestamps and stack repeated messages. */
	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
		at = @At("HEAD"), argsOnly = true)
	private Component orbit$decorateMessage(Component message) {
		return ChatModule.onMessage(NickHiderModule.hide(message));
	}

	/** Chat: keep more than vanilla's 100 messages. */
	//? if >=1.20.5 {
	@ModifyConstant(method = {"addMessageToQueue", "addMessageToDisplayQueue"}, constant = @Constant(intValue = 100))
	//?} else
	/*@ModifyConstant(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", constant = @Constant(intValue = 100))*/
	private int orbit$historyLength(int vanilla) {
		return ChatModule.historyLength(vanilla);
	}
}