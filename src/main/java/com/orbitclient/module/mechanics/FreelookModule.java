package com.orbitclient.module.mechanics;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.KeybindSetting;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.util.Anim;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * Look around your player in third person without changing where you're facing.
 * When you let go, the camera glides back behind you before returning to your normal view.
 */
public class FreelookModule extends Module {
	private static FreelookModule instance;

	private final KeybindSetting key;
	private final ModeSetting mode;
	private final BooleanSetting invertPitch;
	private final BooleanSetting smoothReturn;

	private boolean active;
	private boolean returning;
	private boolean wasDown;
	private float yaw;
	private float pitch;
	private CameraType previousCamera;
	private final Anim.Clock clock = new Anim.Clock();

	public FreelookModule() {
		super("Freelook", "Hold the key to look around without turning your player.", Category.MECHANIC);
		setIcon(Items.ENDER_EYE);
		instance = this;
		key = add(new KeybindSetting("Freelook Key", GLFW.GLFW_KEY_LEFT_ALT));
		mode = add(new ModeSetting("Mode", "Hold", "Hold", "Toggle"));
		invertPitch = add(new BooleanSetting("Invert Pitch", false));
		smoothReturn = add(new BooleanSetting("Smooth Return", true));
	}

	@Override
	public void onTick() {
		boolean down = mc.screen == null && key.isBound() && mc.player != null
			&& key.isDown();
		boolean want = mode.is("Hold") ? down : (down && !wasDown) != (active && !returning);
		wasDown = down;
		if (want && (!active || returning)) start();
		else if (!want && active && !returning) release();
	}

	@Override
	protected void onDisable() {
		if (active) finish();
	}

	private void start() {
		if (!active) {
			yaw = mc.player.getYRot();
			pitch = mc.player.getXRot();
			previousCamera = mc.options.getCameraType();
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
		active = true;
		returning = false;
	}

	private void release() {
		if (smoothReturn.isOn() && Anim.enabled()) returning = true;
		else finish();
	}

	private void finish() {
		active = false;
		returning = false;
		if (previousCamera != null) mc.options.setCameraType(previousCamera);
	}

	/** True while mouse movement should turn the camera instead of the player. */
	public static boolean isActive() {
		return instance != null && instance.active && !instance.returning;
	}

	/**
	 * The camera rotation to use this frame, or null to use the player's.
	 * While returning, eases toward the player's rotation and finishes when it gets there.
	 */
	public static float[] cameraRotation(float playerYaw, float playerPitch) {
		if (instance == null || !instance.active) return null;
		float dt = instance.clock.tick();
		if (instance.returning) {
			float dy = Mth.wrapDegrees(playerYaw - instance.yaw);
			instance.yaw += dy * Math.min(1f, dt * 14f * Anim.speed());
			instance.pitch = Anim.approach(instance.pitch, playerPitch, 14f, dt);
			if (Math.abs(Mth.wrapDegrees(playerYaw - instance.yaw)) < 0.5f && Math.abs(playerPitch - instance.pitch) < 0.5f) {
				instance.finish();
				return null;
			}
		}
		return new float[]{instance.yaw, instance.pitch};
	}

	/** Mouse movement while freelooking turns the camera instead of the player. Same scale as Entity.turn. */
	public static void turn(double dYaw, double dPitch) {
		instance.yaw += (float) dYaw * 0.15f;
		float dp = (float) dPitch * 0.15f * (instance.invertPitch.isOn() ? -1 : 1);
		instance.pitch = Mth.clamp(instance.pitch + dp, -90f, 90f);
	}
}
