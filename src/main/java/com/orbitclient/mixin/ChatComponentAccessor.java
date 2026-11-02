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
	@Accessor("allMessages")
	List<GuiMessage> orbit$allMessages();

	@Accessor("trimmedMessages")
	List<GuiMessage.Line> orbit$trimmedMessages();

	@Accessor("chatScrollbarPos")
	int orbit$scrollPos();

	@Invoker("getScale")
	double orbit$scale();

	@Invoker("getLineHeight")
	int orbit$lineHeight();

	@Invoker("refreshTrimmedMessages")
	void orbit$refresh();

	@Invoker("getWidth")
	int orbit$width();
}
