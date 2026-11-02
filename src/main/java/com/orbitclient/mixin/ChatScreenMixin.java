package com.orbitclient.mixin;

import com.orbitclient.config.ClientSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	/** Shift + left-click a chat message to copy its text (the whole message, even if it wraps over lines). */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void orbit$copyMessage(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (!ClientSettings.INSTANCE.copyChat.isOn() || event.button() != 0 || (event.modifiers() & GLFW.GLFW_MOD_SHIFT) == 0) return;
		Minecraft mc = Minecraft.getInstance();
		String text = messageAt(mc, event.x(), event.y());
		if (text == null) return;
		mc.keyboardHandler.setClipboard(text);
		mc.gui.setOverlayMessage(Component.literal("Copied chat message").withStyle(ChatFormatting.GREEN), false);
		cir.setReturnValue(true);
	}

	private static String messageAt(Minecraft mc, double mouseX, double mouseY) {
		ChatComponentAccessor chat = (ChatComponentAccessor) mc.gui.getChat();
		double scale = chat.orbit$scale();
		List<GuiMessage.Line> lines = chat.orbit$trimmedMessages();
		// Chat is drawn upward from 40 pixels above the bottom of the screen, newest line first.
		double x = (mouseX - 2) / scale;
		double y = (mc.getWindow().getGuiScaledHeight() - 40 - mouseY) / scale;
		int visible = Math.min(mc.gui.getChat().getLinesPerPage(), lines.size() - chat.orbit$scrollPos());
		if (x < 0 || y < 0 || x > chat.orbit$width() / scale + 8) return null;
		int row = (int) (y / chat.orbit$lineHeight());
		if (row >= visible) return null;
		int index = row + chat.orbit$scrollPos();
		if (index < 0 || index >= lines.size()) return null;
		// Lines are stored newest first; a message's last line is marked endOfEntry. Count messages up to
		// the clicked one to find it in the full message list, which keeps its original text.
		int entry = -1;
		for (int i = 0; i <= index; i++) if (lines.get(i).endOfEntry()) entry++;
		List<GuiMessage> messages = chat.orbit$allMessages();
		if (entry >= 0 && entry < messages.size()) return messages.get(entry).content().getString();
		return null;
	}
}
