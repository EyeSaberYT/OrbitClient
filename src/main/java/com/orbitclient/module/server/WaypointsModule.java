package com.orbitclient.module.server;

import com.orbitclient.module.Category;
import com.orbitclient.module.Module;
import com.orbitclient.setting.BooleanSetting;
import com.orbitclient.setting.ModeSetting;
import com.orbitclient.setting.NumberSetting;
import com.orbitclient.util.Anim;
import com.orbitclient.util.Icons;
import com.orbitclient.util.Render2D;
import com.orbitclient.util.Shapes;
import com.orbitclient.waypoint.Waypoint;
import com.orbitclient.waypoint.WaypointManager;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws waypoint markers on screen: a round marker with the waypoint's icon and color, an optional
 * beacon-style beam, a name/distance label, and arrows at the screen edge for waypoints that are
 * off-screen or behind you. Manage waypoints in the WAYPOINTS tab of the mod menu.
 */
public class WaypointsModule extends Module {
	private static float lastFov = 70f;

	/** Auto: recognize a server's worlds and switch waypoint sets; Manual: only switch with the arrows in the menu. */
	public final ModeSetting worldDetection;
	private final ModeSetting labels;
	private final BooleanSetting showDistance;
	private final NumberSetting markerSize;
	private final BooleanSetting scaleWithDistance;
	private final BooleanSetting beams;
	private final BooleanSetting edgeArrows;
	private final NumberSetting labelOpacity;
	private final BooleanSetting deathWaypoints;
	private final NumberSetting maxDistance;

	private boolean wasDead;
	private float shown;
	private final Map<Waypoint, Float> markerFade = new HashMap<>();
	private final Map<Waypoint, Float> labelFade = new HashMap<>();

	public WaypointsModule() {
		super("Waypoints", "Shows waypoints on screen. Add and edit them in the WAYPOINTS tab.", Category.SERVER);
		setIcon(Items.LODESTONE);
		labels = add(new ModeSetting("Labels", "Always", "Always", "Near Crosshair", "Never"));
		showDistance = add(new BooleanSetting("Show Distance", true).visibleWhen(() -> !labels.is("Never")));
		labelOpacity = add(new NumberSetting("Label Background", 60, 0, 100, 5).visibleWhen(() -> !labels.is("Never")));
		markerSize = add(new NumberSetting("Marker Size", 1.0, 0.5, 2.0, 0.1));
		scaleWithDistance = add(new BooleanSetting("Smaller When Far", true));
		beams = add(new BooleanSetting("Beams", true));
		edgeArrows = add(new BooleanSetting("Off-screen Arrows", true));
		deathWaypoints = add(new BooleanSetting("Death Waypoints", true));
		worldDetection = add(new ModeSetting("World Detection", "Auto", "Auto", "Manual"));
		maxDistance = add(new NumberSetting("Max Distance", 5000, 100, 30000, 100));
	}

	/** Remembered from the FOV mixin so markers line up with the world, zoom included. */
	public static void setFov(float fov) {
		lastFov = fov;
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			wasDead = false;
			return;
		}
		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead && deathWaypoints.isOn()) {
			Waypoint death = WaypointManager.addAtPlayer("Death");
			if (death != null) {
				death.icon = "Skull";
				death.color = 0xFFE5484D;
			}
		}
		wasDead = dead;
	}

	public void render(GuiGraphics g, float dt) {
		shown = Anim.step(shown, isEnabled(), 0.25f, dt);
		if (shown <= 0f || mc.player == null || mc.options.hideGui) return;
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 cam = camera.position();
		double yaw = Math.toRadians(camera.yRot());
		double pitch = Math.toRadians(camera.xRot());

		// Camera basis vectors, same convention as Minecraft (yaw 0 = looking south, +z).
		double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
		double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		// up = right x forward
		double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;

		int w = g.guiWidth(), h = g.guiHeight();
		double tanHalf = Math.tan(Math.toRadians(lastFov) / 2);
		double aspect = (double) w / h;

		List<Waypoint> here = WaypointManager.visibleHere();
		for (Waypoint wp : here) {
			double dx = wp.x + 0.5 - cam.x, dy = wp.y + 1.0 - cam.y, dz = wp.z + 0.5 - cam.z;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (dist > maxDistance.get()) continue;
			double z = dx * fx + dy * fy + dz * fz;
			double x = dx * rx + dz * rz;
			double y = dx * ux + dy * uy + dz * uz;

			float fade = Anim.approach(markerFade.getOrDefault(wp, 0f), 1f, 8f, dt);
			markerFade.put(wp, fade);
			// Fade out when you're standing on it.
			float a = shown * fade * Anim.clamp01((float) (dist - 1.5) / 3f);
			if (a <= 0.01f) continue;

			float sx = (float) (w / 2.0 + (x / z) / (tanHalf * aspect) * (w / 2.0));
			float sy = (float) (h / 2.0 - (y / z) / tanHalf * (h / 2.0));
			boolean onScreen = z > 0.1 && sx > -8 && sx < w + 8 && sy > -8 && sy < h + 8;
			Render2D.alpha = a;
			try {
				if (onScreen) marker(g, wp, sx, sy, dist, w, h, dt);
				else if (edgeArrows.isOn() && wp.edge) edgeArrow(g, wp, z > 0.1 ? sx - w / 2f : (float) x, z > 0.1 ? sy - h / 2f : (float) -y, dist, w, h);
			} finally {
				Render2D.alpha = 1f;
			}
		}
		markerFade.keySet().retainAll(here);
		labelFade.keySet().retainAll(here);
	}

	private float sizeFor(double dist) {
		float s = markerSize.getFloat();
		if (scaleWithDistance.isOn()) s *= (float) Math.max(0.6, Math.min(1.2, 1.25 - dist / 400));
		return s;
	}

	private void marker(GuiGraphics g, Waypoint wp, float sx, float sy, double dist, int w, int h, float dt) {
		float s = sizeFor(dist);
		float r = 7 * s;
		int color = wp.currentColor();

		if (beams.isOn() && wp.beam && sy - r > 0) {
			int bw = Math.max(1, Math.round(2 * s));
			int bx = Math.round(sx) - bw / 2;
			Render2D.gradient(g, bx, 0, bx + bw, Math.round(sy - r), Render2D.withAlpha(color, 0f), Render2D.withAlpha(color, 0.55f));
		}
		Shapes.ring(g, sx, sy, r, r + 3.5f * s, Render2D.withAlpha(color, 0.22f));
		Shapes.disc(g, sx, sy, r, 0xE6101219);
		Shapes.ring(g, sx, sy, r - 1.4f * s, r, color);
		Icons.drawCentered(g, wp.iconTexture(), sx, sy, r * 1.15f, color);

		boolean near = Math.hypot(sx - w / 2.0, sy - h / 2.0) < 50;
		boolean want = labels.is("Always") || (labels.is("Near Crosshair") && near);
		float la = Anim.approach(labelFade.getOrDefault(wp, want ? 1f : 0f), want ? 1f : 0f, 12f, dt);
		labelFade.put(wp, la);
		if (la <= 0.01f) return;

		float base = Render2D.alpha;
		Render2D.alpha = base * la;
		String name = wp.name;
		String distance = Math.round(dist) + "m";
		float ds = 0.75f;
		int nameW = mc.font.width(name);
		int distW = showDistance.isOn() ? Math.round(mc.font.width(distance) * ds) : 0;
		int boxW = Math.max(nameW, distW) + 8;
		int boxH = showDistance.isOn() ? 19 : 12;
		int bx = Math.round(sx - boxW / 2f), by = Math.round(sy + r + 4 + (1 - la) * 3);
		Render2D.roundedRect(g, bx, by, boxW, boxH, 4, Render2D.withAlpha(0xFF0B0D12, labelOpacity.getFloat() / 100f));
		Render2D.fill(g, bx + 3, by, bx + boxW - 3, by + 1, Render2D.withAlpha(color, 0.9f));
		Render2D.text(g, mc.font, name, bx + (boxW - nameW) / 2, by + 2, 0xFFFFFFFF, true);
		if (showDistance.isOn()) {
			g.pose().pushMatrix();
			g.pose().translate(bx + (boxW - distW) / 2f, by + 11.5f);
			g.pose().scale(ds, ds);
			Render2D.text(g, mc.font, distance, 0, 0, 0xFFB8BECC, true);
			g.pose().popMatrix();
		}
		Render2D.alpha = base;
	}

	/** A small marker clamped to the screen edge, with an arrow pointing toward the waypoint. */
	private void edgeArrow(GuiGraphics g, Waypoint wp, float dirX, float dirY, double dist, int w, int h) {
		float len = (float) Math.hypot(dirX, dirY);
		if (len < 1e-3f) return;
		float cos = dirX / len, sin = dirY / len;
		float inset = 16;
		float t = Math.min((w / 2f - inset) / Math.max(1e-3f, Math.abs(cos)), (h / 2f - inset) / Math.max(1e-3f, Math.abs(sin)));
		float px = w / 2f + cos * t, py = h / 2f + sin * t;
		int color = wp.currentColor();
		float r = 6 * markerSize.getFloat();

		Shapes.disc(g, px, py, r, 0xE6101219);
		Shapes.ring(g, px, py, r - 1.2f, r, color);
		Icons.drawCentered(g, wp.iconTexture(), px, py, r * 1.15f, color);
		g.pose().pushMatrix();
		g.pose().translate(px, py);
		g.pose().rotate((float) Math.atan2(sin, cos));
		Icons.draw(g, "wp_arrow", r + 1, -3.5f, 7, color);
		g.pose().popMatrix();
		if (showDistance.isOn() && !labels.is("Never")) {
			String d = Math.round(dist) + "m";
			float ds = 0.6f;
			g.pose().pushMatrix();
			g.pose().translate(px - mc.font.width(d) * ds / 2f, py + r + 2);
			g.pose().scale(ds, ds);
			Render2D.text(g, mc.font, d, 0, 0, 0xFFDDE2EC, true);
			g.pose().popMatrix();
		}
	}
}
