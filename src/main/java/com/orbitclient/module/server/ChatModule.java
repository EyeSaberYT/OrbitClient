package com.orbitclient.module.server;

import com.orbitclient.mixin.ChatComponentAccessor;
import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.NumberSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Chat timestamps, stacking of repeated messages ("x3") and a longer chat history. */
public class ChatModule extends Module {
	private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("h:mm a");
	private static ChatModule instance;

	private final BooleanSetting timestamps;
	private final BooleanSetting twelveHour;
	private final BooleanSetting stack;
	private final NumberSetting history;

	private String lastText;
	private int repeats;

	public ChatModule() {
		super("Chat", "Timestamps, stacked repeated messages and a longer chat history.", Category.SERVER);
		setIcon(Items.WRITABLE_BOOK);
		instance = this;
		timestamps = add(new BooleanSetting("Timestamps", true));
		twelveHour = add(new BooleanSetting("12-Hour Time", false).visibleWhen(timestamps::isOn));
		stack = add(new BooleanSetting("Stack Repeated Messages", true));
		history = add(new NumberSetting("History Length", 500, 100, 2000, 100));
	}

	/** How many messages chat keeps (vanilla: 100). */
	public static int historyLength(int vanilla) {
		return instance != null && instance.isEnabled() ? (int) Math.round(instance.history.get()) : vanilla;
	}

	/** Decorates an incoming message; a repeat of the last one replaces it with a counter. */
	public static Component onMessage(Component message) {
		ChatModule m = instance;
		if (m == null || !m.isEnabled()) return message;
		String text = message.getString();
		MutableComponent result = message.copy();
		if (m.stack.isOn() && text.equals(m.lastText) && !text.isBlank()) {
			m.repeats++;
			removeNewest();
			result.append(Component.literal(" x" + m.repeats).withStyle(ChatFormatting.GRAY));
		} else {
			m.repeats = 1;
		}
		m.lastText = text;
		if (m.timestamps.isOn()) {
			String time = LocalTime.now().format(m.twelveHour.isOn() ? TIME_12 : TIME_24);
			// Siblings under an unstyled root: appending the message to the grey timestamp would make it inherit the grey.
			result = Component.empty()
				.append(Component.literal("[" + time + "] ").withStyle(ChatFormatting.DARK_GRAY))
				.append(result);
		}
		return result;
	}

	private static void removeNewest() {
		ChatComponentAccessor chat = (ChatComponentAccessor) Minecraft.getInstance().gui.getChat();
		if (!chat.orbit$allMessages().isEmpty()) {
			chat.orbit$allMessages().removeFirst();
			chat.orbit$refresh();
		}
	}
}
