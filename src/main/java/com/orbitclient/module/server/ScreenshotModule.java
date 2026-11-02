package com.orbitclient.module.server;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import net.minecraft.ChatFormatting;
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
 * After a screenshot, adds [OPEN] and [FOLDER] buttons to the chat message and can copy the image
 * to your clipboard, ready to paste into Discord.
 */
public class ScreenshotModule extends Module {
	private static ScreenshotModule instance;
	private final BooleanSetting copy;
	private final BooleanSetting buttons;

	public ScreenshotModule() {
		super("Screenshot", "Copy screenshots to your clipboard and open them from chat.", Category.SERVER);
		setIcon(Items.PAINTING);
		instance = this;
		copy = add(new BooleanSetting("Copy to Clipboard", true));
		buttons = add(new BooleanSetting("Open / Folder Buttons", true));
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

	/** Puts the image on the system clipboard: through Java when it has a desktop, otherwise PowerShell on Windows. */
	private static boolean copyImage(File file) {
		try {
			if (!java.awt.GraphicsEnvironment.isHeadless()) {
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
			}
		} catch (Throwable ignored) {
			// Fall through to the Windows fallback.
		}
		if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) return false;
		try {
			String path = file.getAbsolutePath().replace("'", "''");
			new ProcessBuilder("powershell", "-NoProfile", "-STA", "-Command",
				"Add-Type -AssemblyName System.Windows.Forms,System.Drawing; [System.Windows.Forms.Clipboard]::SetImage([System.Drawing.Image]::FromFile('" + path + "'))")
				.redirectErrorStream(true).start();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}