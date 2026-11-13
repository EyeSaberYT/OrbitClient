package com.orbitclient.gui;

import com.orbitclient.social.Friends;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Icons;
import com.orbitclient.util.Render2D;
import com.orbitclient.util.Shapes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Friends, in the Orbit look: on the left your friends (online first), requests or blocked players,
 * with a box to add someone by their Minecraft name; on the right the chat with the selected friend,
 * and buttons to join their server, remove or block them. Opened from the Orbit menu's dock and the
 * home screen. Everything comes from {@link Friends}.
 */
public final class FriendsScreen extends net.minecraft.client.gui.screens.Screen {
	private enum Tab {FRIENDS, REQUESTS, BLOCKED}

	private record Hit(int x, int y, int w, int h, Runnable action) {
	}

	private static final int ROW_H = 28;
	private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");

	private static Tab lastTab = Tab.FRIENDS;
	private static UUID lastSelected;

	private Tab tab = lastTab;
	private UUID selected = lastSelected;
	private EditBox addBox, chatBox;
	private String addText = "", chatText = "";
	private final List<Hit> hits = new ArrayList<>();
	private final Anim.Clock clock = new Anim.Clock();
	private final Map<String, Float> anims = new HashMap<>();
	private float dt, openProgress;
	private String flash = "";
	private int flashColor;
	private long flashUntil;
	private String confirm;
	private long confirmUntil;
	private float listScroll, chatScroll;

	// Layout
	private int px, py, pw, ph, leftW, listTop, listBottom, chatTop, chatBottom;

	public FriendsScreen() {
		super(Component.literal("Friends"));
	}

	/** Opens the chat with this friend. */
	public FriendsScreen(UUID friend) {
		this();
		tab = Tab.FRIENDS;
		selected = friend;
	}

	@Override
	protected void init() {
		pw = Math.min(600, width - 20);
		ph = Math.min(360, height - 20);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		leftW = Math.min(230, Math.round(pw * 0.42f));
		listTop = py + 80;
		listBottom = py + ph - 22;
		chatTop = py + 46;
		chatBottom = py + ph - 46;

		if (addBox != null) addText = addBox.getValue();
		if (chatBox != null) chatText = chatBox.getValue();
		addBox = box(px + 17, py + 59, leftW - 24 - 50 - 10, 16, "Add by Minecraft name...");
		addBox.setValue(addText);
		chatBox = box(px + leftW + 17, py + ph - 33, pw - leftW - 24 - 54 - 10, 256, "Message...");
		chatBox.setValue(chatText);
		Friends.screenOpen = true;
	}

	private EditBox box(int x, int y, int w, int maxLength, String hint) {
		EditBox b = new EditBox(font, x, y, w, 10, Component.literal(hint));
		b.setMaxLength(maxLength);
		b.setBordered(false);
		b.setTextColor(0xFFFFFFFF);
		//? if >=1.19.3
		b.setHint(Component.literal(hint).withStyle(s -> s.withColor(Theme.O_TEXT_FAINT)));
		return addRenderableWidget(b);
	}

	@Override
	public void removed() {
		Friends.screenOpen = false;
		Friends.openChat = null;
		lastTab = tab;
		lastSelected = selected;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ================================================================ rendering

	/** Panorama on the title screen, a dark veil in a world. No blur: it can only run once a frame. */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		if (minecraft.level == null) renderPanorama(g, delta);
		g.fillGradient(0, 0, width, height, 0xB0070910, 0xE0040508);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		dt = clock.tick();
		openProgress = Anim.step(openProgress, true, 0.35f, dt);
		hits.clear();
		if (confirm != null && System.currentTimeMillis() > confirmUntil) confirm = null;

		Friends.Friend friend = selectedFriend();
		Friends.openChat = friend == null ? null : friend.uuid();
		if (friend != null) Friends.markRead(friend.uuid());
		chatBox.visible = friend != null;
		//? if >=1.19.3 {
		chatBox.setHint(Component.literal(friend == null ? "" : "Message " + name(friend.uuid(), friend.name()) + "...")
			.withStyle(s -> s.withColor(Theme.O_TEXT_FAINT)));
		//?}

		Render2D.alpha = Anim.easeOutCubic(openProgress);
		try {
			Render2D.roundedRectBordered(g, px, py, pw, ph, 10, Theme.GLASS, Theme.GLASS_BORDER);
			Render2D.fill(g, px + leftW, py + 10, px + leftW + 1, py + ph - 10, 0x18FFFFFF);
			renderHeader(g, mx, my);
			renderAddRow(g, mx, my);
			renderList(g, mx, my);
			if (friend != null) renderChat(g, mx, my, friend);
			else renderEmptyRight(g);
			renderStatus(g);
		} finally {
			Render2D.alpha = 1f;
		}
		super.render(g, mx, my, delta);
	}

	private float anim(String key, boolean target, float speed) {
		float v = Anim.approach(anims.getOrDefault(key, target ? 1f : 0f), target ? 1f : 0f, speed, dt);
		anims.put(key, v);
		return v;
	}

	private boolean hit(int mx, int my, int x, int y, int w, int h, Runnable action) {
		hits.add(new Hit(x, y, w, h, action));
		return Render2D.hovered(mx, my, x, y, w, h);
	}

	private Friends.Friend selectedFriend() {
		if (tab != Tab.FRIENDS || selected == null) return null;
		for (Friends.Friend f : Friends.friends()) if (f.uuid().equals(selected)) return f;
		return null;
	}

	private static String name(UUID uuid, String name) {
		return name.isEmpty() ? uuid.toString().substring(0, 8) : name;
	}

	private void renderHeader(GuiGraphics g, int mx, int my) {
		UiText.spaced(g, "FRIENDS", px + 14, py + 12, 0xFFFFFFFF, true);
		long online = Friends.onlineCount();
		UiText.text(g, online + " online", px + 20 + UiText.spacedWidth("FRIENDS", true), py + 12, online > 0 ? Theme.ON : Theme.O_TEXT_FAINT);

		boolean overClose = hit(mx, my, px + pw - 22, py + 8, 14, 14, this::onClose);
		Icons.drawCentered(g, "ui_close", px + pw - 15, py + 15, 9, overClose ? 0xFFFFFFFF : Theme.O_TEXT_DIM);

		// Tabs
		int x = px + 12, y = py + 32;
		int requests = Friends.incoming().size();
		x += tabPill(g, mx, my, Tab.FRIENDS, "FRIENDS " + Friends.friends().size(), 0, x, y) + 4;
		x += tabPill(g, mx, my, Tab.REQUESTS, "REQUESTS" + (requests > 0 ? " " + requests : ""), requests, x, y) + 4;
		tabPill(g, mx, my, Tab.BLOCKED, "BLOCKED", 0, x, y);
	}

	private int tabPill(GuiGraphics g, int mx, int my, Tab t, String label, int alert, int x, int y) {
		int w = font.width(label) + 12, h = 16;
		boolean over = hit(mx, my, x, y, w, h, () -> {
			tab = t;
			listScroll = 0;
		});
		float on = anim("tab" + t, tab == t, 14f), hov = anim("tabh" + t, over, 16f);
		Render2D.roundedRect(g, x, y, w, h, 8, Render2D.lerpColor(0x00000000, Theme.O_ROW_HOVER, Math.max(on, hov * 0.6f)));
		UiText.text(g, label, x + 6, y + 4, alert > 0 ? Theme.DISABLED : Render2D.lerpColor(Theme.O_TEXT_DIM, 0xFFFFFFFF, Math.max(on, hov)));
		if (on > 0.01f) Render2D.fill(g, x + 6, y + h - 2, x + w - 6, y + h - 1, Render2D.withAlpha(Theme.CAT_HUD, on));
		return w;
	}

	private void renderAddRow(GuiGraphics g, int mx, int my) {
		int x = px + 12, y = py + 54, w = leftW - 24 - 50;
		Render2D.roundedRectBordered(g, x, y, w, 18, 6, Theme.O_FIELD, addBox.isFocused() ? Theme.CAT_HUD : Theme.O_FIELD_BORDER);
		pill(g, mx, my, "ADD", x + w + 6, y + 1, 44, Theme.ON, null, this::addFriend);
	}

	// ---------------------------------------------------------------- the list

	private void renderList(GuiGraphics g, int mx, int my) {
		// Rows scrolled out of the list don't react to the mouse.
		boolean inList = Render2D.hovered(mx, my, px, listTop, leftW, listBottom - listTop);
		int rmx = inList ? mx : -10000, rmy = inList ? my : -10000;

		g.enableScissor(px + 1, listTop, px + leftW, listBottom);
		int y = listTop - Math.round(listScroll);
		switch (tab) {
			case FRIENDS -> {
				if (Friends.friends().isEmpty()) y = emptyList(g, y, Friends.loaded() ? "No friends yet. Add someone by name above." : "Loading friends...");
				for (Friends.Friend f : Friends.friends()) y = friendRow(g, rmx, rmy, f, y);
			}
			case REQUESTS -> {
				y = section(g, "INCOMING", y);
				if (Friends.incoming().isEmpty()) y = emptyList(g, y, "No requests.");
				for (Friends.Person p : Friends.incoming()) {
					y = personRow(g, rmx, rmy, p, y, "ACCEPT", Theme.ON, () -> Friends.accept(p.uuid(), done("You're friends with " + name(p.uuid(), p.name()))),
						"DECLINE", () -> Friends.remove(p.uuid(), done("Request declined")));
				}
				y = section(g, "SENT", y + 4);
				if (Friends.outgoing().isEmpty()) y = emptyList(g, y, "No pending requests.");
				for (Friends.Person p : Friends.outgoing()) {
					y = personRow(g, rmx, rmy, p, y, "CANCEL", Theme.O_TEXT_DIM, () -> Friends.remove(p.uuid(), done("Request cancelled")), null, null);
				}
			}
			case BLOCKED -> {
				if (Friends.blocked().isEmpty()) y = emptyList(g, y, "Nobody blocked. Blocked players can't add or message you.");
				for (Friends.Person p : Friends.blocked()) {
					y = personRow(g, rmx, rmy, p, y, "UNBLOCK", Theme.CAT_HUD, () -> Friends.unblock(p.uuid(), done("Unblocked " + name(p.uuid(), p.name()))), null, null);
				}
			}
		}
		g.disableScissor();
		int content = y + Math.round(listScroll) - listTop;
		listScroll = Math.max(0, Math.min(listScroll, Math.max(0, content - (listBottom - listTop))));
	}

	private int section(GuiGraphics g, String title, int y) {
		UiText.spacedSmall(g, title, px + 12 + UiText.spacedWidth(title, true) * 0.3f, y + 2, Theme.O_TEXT_FAINT, true, 0.6f);
		return y + 12;
	}

	private int emptyList(GuiGraphics g, int y, String text) {
		for (String line : UiText.wrap(text, leftW - 28, 3)) {
			UiText.text(g, line, px + 14, y + 4, Theme.O_TEXT_FAINT);
			y += 11;
		}
		return y + 6;
	}

	private int friendRow(GuiGraphics g, int mx, int my, Friends.Friend f, int y) {
		int x = px + 6, w = leftW - 12;
		boolean over = y + ROW_H > listTop && y < listBottom && hit(mx, my, x, y, w, ROW_H - 2, () -> {
			selected = f.uuid();
			chatScroll = 0;
		});
		float hov = anim("row" + f.uuid(), over, 18f), sel = anim("sel" + f.uuid(), f.uuid().equals(selected), 14f);
		float bg = Math.max(sel, hov * 0.7f);
		if (bg > 0.01f) Render2D.roundedRect(g, x, y, w, ROW_H - 2, 6, Render2D.withAlpha(Theme.O_ROW_HOVER, bg));
		if (sel > 0.01f) Render2D.roundedRect(g, x + 1, y + 7, 2, ROW_H - 16, 1, Render2D.withAlpha(Theme.CAT_HUD, sel));
		Heads.draw(g, f.uuid(), f.name(), x + 8, y + 5, 16, Render2D.a(f.online() ? 0xFFFFFFFF : 0xFF7A7F8C));
		Shapes.disc(g, x + 23, y + 20, 3.2f, 0xFF101219);
		Shapes.disc(g, x + 23, y + 20, 2.2f, f.online() ? Theme.ON : Theme.OFF);
		int unread = Friends.unread(f.uuid());
		int right = unread > 0 ? 16 : 0;
		UiText.fit(g, name(f.uuid(), f.name()), x + 30, y + 4, w - 34 - right, f.online() ? 0xFFFFFFFF : Theme.O_TEXT_DIM);
		UiText.fit(g, f.status(), x + 30, y + 15, w - 34 - right, f.online() ? (f.joinable() ? Theme.CAT_HUD : Theme.ON) : Theme.O_TEXT_FAINT);
		if (unread > 0) {
			String n = unread > 9 ? "9+" : String.valueOf(unread);
			Shapes.disc(g, x + w - 10, y + ROW_H / 2f - 1, 6, Theme.DISABLED);
			UiText.text(g, n, x + w - 10 - font.width(n) / 2, y + ROW_H / 2 - 5, 0xFFFFFFFF);
		}
		return y + ROW_H;
	}

	private int personRow(GuiGraphics g, int mx, int my, Friends.Person p, int y, String action, int color, Runnable onAction,
	                      String second, Runnable onSecond) {
		int x = px + 6, w = leftW - 12;
		boolean visible = y + ROW_H > listTop && y < listBottom;
		boolean over = visible && Render2D.hovered(mx, my, x, y, w, ROW_H - 2);
		float hov = anim("prow" + p.uuid() + action, over, 18f);
		if (hov > 0.01f) Render2D.roundedRect(g, x, y, w, ROW_H - 2, 6, Render2D.withAlpha(Theme.O_ROW_HOVER, hov * 0.7f));
		Heads.draw(g, p.uuid(), p.name(), x + 8, y + 5, 16, Render2D.a(0xFFFFFFFF));
		int pills = 8 + font.width(action) + 10 + (second == null ? 0 : font.width(second) + 14);
		UiText.fit(g, name(p.uuid(), p.name()), x + 30, y + 9, w - 34 - pills, 0xFFFFFFFF);
		int bx = x + w - 6;
		if (second != null) {
			int sw = font.width(second) + 10;
			bx -= sw;
			if (visible) pill(g, mx, my, second, bx, y + 6, sw, Theme.DISABLED, null, onSecond);
			bx -= 4;
		}
		int aw = font.width(action) + 10;
		if (visible) pill(g, mx, my, action, bx - aw, y + 6, aw, color, null, onAction);
		return y + ROW_H;
	}

	/** A small button; with a confirm key, the first click asks "SURE?" and only the second does it. */
	private int pill(GuiGraphics g, int mx, int my, String label, int x, int y, int w, int color, String confirmKey, Runnable action) {
		boolean asking = confirmKey != null && confirmKey.equals(confirm);
		String shown = asking ? "SURE?" : label;
		Runnable click = confirmKey == null || asking ? () -> {
			confirm = null;
			action.run();
		} : () -> {
			confirm = confirmKey;
			confirmUntil = System.currentTimeMillis() + 3000;
		};
		boolean over = hit(mx, my, x, y, w, 15, click);
		float hov = anim("pill" + label + x + y, over, 18f);
		int c = asking ? Theme.DISABLED : color;
		// Tinted fill under white text: readable on every color.
		Render2D.roundedRectBordered(g, x, y, w, 15, 7, Render2D.lerpColor(0xFF161A24, c, 0.35f + 0.3f * hov), Render2D.withAlpha(c, 0.7f + 0.3f * hov));
		UiText.text(g, shown, x + w / 2 - font.width(shown) / 2, y + 4, Render2D.lerpColor(0xFFDDE2EE, 0xFFFFFFFF, hov));
		return w;
	}

	// ---------------------------------------------------------------- the chat

	private void renderChat(GuiGraphics g, int mx, int my, Friends.Friend f) {
		int x = px + leftW + 12, w = pw - leftW - 24;
		String shown = name(f.uuid(), f.name());

		// Join / remove / block, right-aligned in the header (left of the close button).
		int bx = x + w - 18;
		int bw = font.width("BLOCK") + 12;
		bx -= bw;
		pill(g, mx, my, "BLOCK", bx, py + 14, bw, Theme.DISABLED, "block" + f.uuid(),
			() -> Friends.block(f.uuid(), done("Blocked " + shown)));
		bw = font.width("REMOVE") + 12;
		bx -= bw + 4;
		pill(g, mx, my, "REMOVE", bx, py + 14, bw, Theme.O_TEXT_DIM, "remove" + f.uuid(),
			() -> Friends.remove(f.uuid(), done("Removed " + shown + " from your friends")));
		if (f.joinable()) {
			bw = font.width("JOIN") + 16;
			bx -= bw + 4;
			pill(g, mx, my, "JOIN", bx, py + 14, bw, Theme.CAT_HUD, minecraft.level != null ? "join" + f.uuid() : null, () -> join(f));
		}
		// Name and status in what's left of the header.
		Heads.draw(g, f.uuid(), f.name(), x, py + 12, 20, Render2D.a(0xFFFFFFFF));
		int headerW = bx - 8 - (x + 26);
		UiText.fit(g, shown, x + 26, py + 12, headerW, 0xFFFFFFFF);
		UiText.fit(g, f.status(), x + 26, py + 23, headerW, f.online() ? (f.joinable() ? Theme.CAT_HUD : Theme.ON) : Theme.O_TEXT_FAINT);
		Render2D.fill(g, x, py + 40, x + w, py + 41, 0x14FFFFFF);

		// Messages, newest at the bottom, scrolled up with the wheel.
		List<Friends.Message> messages = Friends.chat(f.uuid());
		UUID me = Friends.me();
		int bubbleMax = Math.round(w * 0.72f);
		g.enableScissor(x - 2, chatTop, x + w + 2, chatBottom);
		int y = chatBottom - 4 + Math.round(chatScroll);
		if (messages.isEmpty()) {
			UiText.centered(g, "Say hi to " + shown + "!", x + w / 2, (chatTop + chatBottom) / 2 - 4, Theme.O_TEXT_FAINT);
			UiText.centered(g, "Messages last a day.", x + w / 2, (chatTop + chatBottom) / 2 + 8, Theme.O_TEXT_FAINT);
		}
		int total = 0;
		for (int i = messages.size() - 1; i >= 0; i--) {
			Friends.Message m = messages.get(i);
			boolean mine = m.from().equals(me);
			List<String> lines = new ArrayList<>();
			for (FormattedText line : font.getSplitter().splitLines(m.text(), bubbleMax - 12, Style.EMPTY)) lines.add(line.getString());
			int textW = 0;
			for (String line : lines) textW = Math.max(textW, font.width(line));
			String time = TIME.format(new Date(m.time()));
			int bw2 = Math.max(textW, font.width(time)) + 12, bh = lines.size() * 10 + 14;
			y -= bh;
			total += bh + 4;
			if (y + bh >= chatTop && y <= chatBottom) {
				int bx2 = mine ? x + w - bw2 : x;
				Render2D.roundedRect(g, bx2, y, bw2, bh, 6, mine ? 0xFF2B3E6E : Theme.O_ROW);
				for (int l = 0; l < lines.size(); l++) UiText.text(g, lines.get(l), bx2 + 6, y + 4 + l * 10, 0xFFFFFFFF);
				UiText.text(g, time, mine ? bx2 + bw2 - 6 - font.width(time) : bx2 + 6, y + bh - 10, mine ? 0xFF8EA4D8 : Theme.O_TEXT_FAINT);
			}
			y -= 4;
		}
		g.disableScissor();
		chatScroll = Math.max(0, Math.min(chatScroll, Math.max(0, total - (chatBottom - chatTop) + 8)));

		// Input
		int iy = py + ph - 38, iw = w - 54;
		Render2D.roundedRectBordered(g, x, iy, iw, 18, 6, Theme.O_FIELD, chatBox.isFocused() ? Theme.CAT_HUD : Theme.O_FIELD_BORDER);
		pill(g, mx, my, "SEND", x + iw + 6, iy + 1, 48, Theme.CAT_HUD, null, this::sendMessage);
	}

	private void renderEmptyRight(GuiGraphics g) {
		int cx = px + leftW + (pw - leftW) / 2, cy = py + ph / 2;
		String[] lines = switch (tab) {
			case FRIENDS -> Friends.friends().isEmpty()
				? new String[]{"Add friends by their Minecraft name.", "They need Orbit Client too, and to accept."}
				: new String[]{"Pick a friend to chat,", "or join the server they're on."};
			case REQUESTS -> new String[]{"Friend requests you got and sent.", "Requests reach players next time they play Orbit."};
			case BLOCKED -> new String[]{"Blocked players can't send you", "requests or messages."};
		};
		for (int i = 0; i < lines.length; i++) UiText.centered(g, lines[i], cx, cy - 10 + i * 12, Theme.O_TEXT_FAINT);
	}

	private void renderStatus(GuiGraphics g) {
		String text;
		int color;
		if (System.currentTimeMillis() < flashUntil) {
			text = flash;
			color = flashColor;
		} else {
			text = Friends.status();
			color = Theme.O_TEXT_FAINT;
		}
		if (!text.isEmpty()) UiText.fit(g, text, px + 14, py + ph - 15, leftW - 20, color);
	}

	// ================================================================ actions

	private void flash(String text, boolean error) {
		flash = text;
		flashColor = error ? Theme.DISABLED : Theme.ON;
		flashUntil = System.currentTimeMillis() + 4000;
	}

	/** The callback of an action: shows the server's error, or this message when it worked. */
	private java.util.function.Consumer<String> done(String success) {
		return error -> flash(error == null ? success : error, error != null);
	}

	private void addFriend() {
		String name = addBox.getValue().trim();
		if (!name.matches("[A-Za-z0-9_]{1,16}")) {
			flash("Type a Minecraft name", true);
			return;
		}
		Friends.add(name, error -> {
			if (error == null) {
				addBox.setValue("");
				flash("Friend request sent to " + name, false);
			} else {
				flash(error, true);
			}
		});
	}

	private void sendMessage() {
		Friends.Friend f = selectedFriend();
		String text = chatBox.getValue().strip();
		if (f == null || text.isEmpty()) return;
		chatBox.setValue("");
		chatScroll = 0;
		Friends.message(f.uuid(), text, error -> {
			if (error != null) {
				flash(error, true);
				if (chatBox.getValue().isEmpty()) chatBox.setValue(text);
			}
		});
	}

	/** Joins the server a friend is on (leaving the current world or server first). */
	private void join(Friends.Friend f) {
		String address = f.server();
		//? if >=1.21.9 {
		if (minecraft.level != null) minecraft.disconnectFromWorld(Component.literal("Joining " + f.name()));
		//?} else
		/*if (minecraft.level != null) minecraft.disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(Component.literal("Joining " + f.name())), false);*/
		ConnectScreen.startConnecting(new TitleScreen(), minecraft, ServerAddress.parseString(address),
			new ServerData(f.name() + "'s server", address, ServerData.Type.OTHER), false, null);
	}

	// ================================================================ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			for (Hit h : hits) {
				if (Render2D.hovered(event.x(), event.y(), h.x, h.y, h.w, h.h)) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
					h.action.run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (Render2D.hovered(mx, my, px, listTop, leftW, listBottom - listTop)) {
			listScroll = (float) Math.max(0, listScroll - sy * 20);
			return true;
		}
		if (selectedFriend() != null && Render2D.hovered(mx, my, px + leftW, chatTop, pw - leftW, chatBottom - chatTop)) {
			chatScroll = (float) Math.max(0, chatScroll + sy * 20);
			return true;
		}
		return super.mouseScrolled(mx, my, sx, sy);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			if (addBox.isFocused()) {
				addFriend();
				return true;
			}
			if (chatBox.isFocused()) {
				sendMessage();
				return true;
			}
		}
		return super.keyPressed(event);
	}
}
