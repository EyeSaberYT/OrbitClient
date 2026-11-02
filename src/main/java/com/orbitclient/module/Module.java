package com.orbitclient.module;

import com.orbitclient.setting.KeybindSetting;
import com.orbitclient.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;
	private boolean defaultEnabled;
	private boolean isNew;
	/** Shown in the Favorites category of the mod menu. Saved per profile. */
	public boolean favorite;
	private Item icon = Items.PAPER;
	private ItemStack iconStack;
	private String iconName;

	/** Every mod gets a toggle key, unbound by default, like Lunar's per-mod keybinds. */
	public final KeybindSetting toggleKey;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.toggleKey = new KeybindSetting("Toggle Key", GLFW.GLFW_KEY_UNKNOWN);
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	/** The item drawn as this mod's icon in the menu. */
	protected void setIcon(Item icon) {
		this.icon = icon;
	}

	/** Shows the red "NEW" tag in the menu and puts the mod under the NEW filter. */
	protected void markNew() {
		this.isNew = true;
	}

	/** Name of the custom icon (see Icons), e.g. "Toggle Sneak/Sprint" -> toggle_sneak_sprint. */
	public String getIconName() {
		if (iconName == null) iconName = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
		return iconName;
	}

	public ItemStack getIcon() {
		if (iconStack == null) iconStack = new ItemStack(icon);
		return iconStack;
	}

	public boolean isNew() {
		return isNew;
	}

	/** Settings shown in the menu; the toggle key is always last. */
	public List<Setting<?>> getSettings() {
		List<Setting<?>> all = new ArrayList<>(settings);
		all.add(toggleKey);
		return all;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		if (enabled) onEnable();
		else onDisable();
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setDefaultEnabled(boolean defaultEnabled) {
		this.defaultEnabled = defaultEnabled;
		setEnabled(defaultEnabled);
	}

	/** Back to a fresh install's state, used before loading another profile. */
	public void resetToDefaults() {
		for (Setting<?> setting : getSettings()) setting.reset();
		favorite = false;
		setEnabled(defaultEnabled);
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Called at the end of every client tick while enabled. */
	public void onTick() {
	}
}
