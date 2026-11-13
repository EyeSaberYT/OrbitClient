package com.orbitclient.module.server;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.orbitclient.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * After a screenshot, adds buttons to the chat message: [OPEN] the image, open its [FOLDER],
 * [COPY] the image to the clipboard (ready to paste into Discord) and copy its [PATH]. It can also
 * copy every new screenshot on its own.
 */
public class ScreenshotModule extends Module {
	/** The client-side command the [COPY] button runs; never sent to the server. */
	private static final String COMMAND = "orbit-screenshot";
	private static ScreenshotModule instance;
	private final BooleanSetting copy;
	private final BooleanSetting buttons;

	public ScreenshotModule() {
		super("Screenshot", "Open, copy and share screenshots straight from chat.", Category.SERVER);
		setIcon(Items.PAINTING);
		instance = this;
		copy = add(new BooleanSetting("Copy to Clipboard", true));
		buttons = add(new BooleanSetting("Chat Buttons", true));
		// (Fabric API has no client commands before 1.16.2: there, the chat button can't copy.)
		//? if >=1.16.2 {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registries) -> dispatcher.register(
			ClientCommandManager.literal(COMMAND).then(ClientCommandManager.literal("copy")
				.then(ClientCommandManager.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
					File file = screenshot(StringArgumentType.getString(ctx, "file"));
					boolean ok = file != null && copyImage(file);
					ctx.getSource().sendFeedback(Component.literal(ok ? "Copied the screenshot to your clipboard." : "Couldn't copy that screenshot.")
						.withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
					return ok ? 1 : 0;
				})))));
		//?}
	}

	/** A screenshot by file name, only from this game's screenshots folder; null if it isn't one. */
	private static File screenshot(String name) {
		if (name.contains("/") || name.contains("\\") || name.contains("..") || !name.endsWith(".png")) return null;
		File file = new File(new File(Minecraft.getInstance().gameDirectory, "screenshots"), name);
		return file.isFile() ? file : null;
	}

	/** Wraps the message callback of a screenshot so the finished file gets our extras. */
	public static Consumer<Component> wrap(File gameDir, Consumer<Component> original) {
		if (instance == null || !instance.isEnabled()) return original;
		return message -> {
			File dir = new File(gameDir, "screenshots");
			File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
			File newest = files == null ? null : Arrays.stream(files).max(Comparator.comparingLong(File::lastModified)).orElse(null);
			if (newest == null) {
				original.accept(message);
				return;
			}
			MutableComponent out = message.copy();
			if (instance.buttons.isOn()) {
				out.append(" ").append(button("OPEN", "Open the screenshot", new ClickEvent.OpenFile(newest)));
				out.append(" ").append(button("FOLDER", "Open the screenshots folder", new ClickEvent.OpenFile(dir)));
				out.append(" ").append(button("COPY", "Copy the image to your clipboard",
					new ClickEvent.RunCommand("/" + COMMAND + " copy " + newest.getName())));
				out.append(" ").append(button("PATH", "Copy the file's location", new ClickEvent.CopyToClipboard(newest.getAbsolutePath())));
			}
			if (instance.copy.isOn()) {
				boolean ok = copyImage(newest);
				out.append(Component.literal(ok ? " Copied!" : " (couldn't copy)").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.GRAY));
			}
			original.accept(out);
		};
	}

	private static Component button(String label, String hover, ClickEvent click) {
		return Component.literal("[" + label + "]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
			.withClickEvent(click).withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	/**
	 * Puts the image on the system clipboard through Java's own clipboard (OrbitClient turns off
	 * Minecraft's headless mode for this at start-up). No other program is ever started: where Java
	 * has no desktop (macOS, or a headless setup) copying just isn't offered.
	 */
	private static boolean copyImage(File file) {
		try {
			if (java.awt.GraphicsEnvironment.isHeadless()) return false;
			Image image = ImageIO.read(file);
			if (image == null) return false;
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
				@Override
				public DataFlavor[] getTransferDataFlavors() {
					return new DataFlavor[]{DataFlavor.imageFlavor};
				}

				@Override
				public boolean isDataFlavorSupported(DataFlavor flavor) {
					return DataFlavor.imageFlavor.equals(flavor);
				}

				@Override
				public Object getTransferData(DataFlavor flavor) {
					return image;
				}
			}, null);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
}