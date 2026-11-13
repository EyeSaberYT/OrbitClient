package com.orbitclient.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Chat internals needed to find the message under the mouse (shift-click to copy). */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
	//? if >=1.19.1 {
	@Accessor("allMessages")
	List<GuiMessage> orbit$allMessages();

	@Accessor("trimmedMessages")
	List<GuiMessage.Line> orbit$trimmedMessages();
	//?}
	//? if >=1.16.2 <1.19.1 {
	/*// Before 1.19.1 a chat message and each of its wrapped lines were the same class.
	@Accessor("allMessages")
	List<GuiMessage<net.minecraft.network.chat.Component>> orbit$allMessages();

	@Accessor("trimmedMessages")
	List<GuiMessage<net.minecraft.util.FormattedCharSequence>> orbit$trimmedMessages();
	*///?}
	//? if <1.16.2 {
	/*// Before 1.16.2 that class held plain formatted text.
	@Accessor("allMessages")
	List<GuiMessage> orbit$allMessages();

	@Accessor("trimmedMessages")
	List<GuiMessage> orbit$trimmedMessages();
	*///?}

	@Accessor("chatScrollbarPos")
	int orbit$scrollPos();

	@Invoker("getScale")
	double orbit$scale();

	//? if >=1.19.1 {
	@Invoker("getLineHeight")
	int orbit$lineHeight();
	//?}

	@Invoker("refreshTrimmedMessages")
	void orbit$refresh();

	@Invoker("getWidth")
	int orbit$width();
}
