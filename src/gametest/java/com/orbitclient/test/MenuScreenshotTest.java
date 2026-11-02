package com.orbitclient.test;

import com.orbitclient.gui.CrosshairEditorScreen;
import com.orbitclient.gui.HudEditorScreen;
import com.orbitclient.gui.OrbitScreen;
import com.orbitclient.module.ModuleManager;
import com.orbitclient.module.hud.CoordinatesModule;
import com.orbitclient.module.hud.CpsModule;
import com.orbitclient.module.hud.FpsModule;
import com.orbitclient.module.hud.KeystrokesModule;
import com.orbitclient.module.mechanics.ZoomModule;
import com.orbitclient.module.visual.CrosshairModule;
import com.orbitclient.waypoint.WaypointManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.lwjgl.glfw.GLFW;

/**
 * Joins a world (which applies every mixin) and screenshots each part of the Orbit menu and HUD,
 * including frames in the middle of animations.
 */
@SuppressWarnings("UnstableApiUsage")
public class MenuScreenshotTest implements FabricClientGameTest {
	/** Adds a ready-made waypoint (the manager only creates them at the player's position). */
	private static void addWaypoint(com.orbitclient.waypoint.Waypoint wp) {
		var array = new com.google.gson.JsonArray();
		array.addAll(WaypointManager.save());
		var o = new com.google.gson.JsonObject();
		o.addProperty("name", wp.name);
		o.addProperty("x", wp.x);
		o.addProperty("y", wp.y);
		o.addProperty("z", wp.z);
		o.addProperty("world", wp.world);
		o.addProperty("dimension", wp.dimension);
		o.addProperty("color", wp.color);
		o.addProperty("chroma", wp.chroma);
		o.addProperty("icon", wp.icon);
		array.add(o);
		WaypointManager.load(array);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1600, 900);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			context.runOnClient(mc -> {
				ModuleManager.get(FpsModule.class).setEnabled(true);
				ModuleManager.get(CpsModule.class).setEnabled(true);
				ModuleManager.get(KeystrokesModule.class).setEnabled(true);
				ModuleManager.get(ZoomModule.class).favorite = true;
				WaypointManager.addAtPlayer("Spawn");
			});
			context.waitTicks(10);
			context.takeScreenshot("orbit-01-hud");

			context.setScreen(ShapeDebugScreen::new);
			context.waitTicks(5);
			context.takeScreenshot("orbit-00-shapes");
			context.setScreen(() -> null);

			// Frame rate with and without the menu open (FPS is measured over the last second).
			context.waitTicks(40);
			int fpsGame = context.computeOnClient(mc -> mc.getFps());
			context.setScreen(OrbitScreen::new);
			context.waitTicks(60);
			int fpsOrbit = context.computeOnClient(mc -> mc.getFps());
			context.setScreen(() -> null);
			context.waitTicks(10);
			System.out.println("[PERF] FPS in game: " + fpsGame + ", FPS with Orbit open: " + fpsOrbit
				+ "; " + com.orbitclient.util.Shapes.stats());

			// Orbit opening (nodes bursting out of the core), then settled.
			context.setScreen(OrbitScreen::new);
			context.waitTick();
			context.takeScreenshot("orbit-02a-orbit-opening");
			context.waitTicks(12);
			context.takeScreenshot("orbit-02-orbit");

			// Spin the ring with the arrow key: catch it mid-spin.
			context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
			context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
			context.waitTick();
			context.takeScreenshot("orbit-03a-orbit-spinning");
			context.waitTicks(10);

			// Other categories and search.
			context.setScreen(() -> new OrbitScreen(ModuleManager.get(ZoomModule.class)));
			context.waitTicks(12);
			context.takeScreenshot("orbit-04-orbit-mechanic");
			context.getInput().typeChars("co");
			context.waitTicks(12);
			context.takeScreenshot("orbit-05-orbit-search");

			context.setScreen(OrbitScreen::new);
			context.waitTicks(4);
			context.runOnClient(mc -> ((OrbitScreen) mc.screen).showPanel("SETTINGS"));
			context.waitTicks(10);
			context.takeScreenshot("orbit-06-orbit-settings");
			context.runOnClient(mc -> ((OrbitScreen) mc.screen).showPanel("PROFILES"));
			context.waitTicks(10);
			context.takeScreenshot("orbit-07-orbit-profiles");
			context.runOnClient(mc -> ((OrbitScreen) mc.screen).showPanel("WAYPOINTS"));
			context.waitTicks(10);
			context.takeScreenshot("orbit-08-orbit-waypoints");

			context.setScreen(HudEditorScreen::new);
			context.waitTicks(10);
			context.takeScreenshot("orbit-09-hud-editor");

			// Crosshair builder from the Mechanic ring.
			context.runOnClient(mc -> {
				var crosshair = ModuleManager.get(CrosshairModule.class);
				crosshair.setEnabled(true);
				crosshair.mode.set("Custom");
				crosshair.custom.restore(CrosshairModule.preset(4));
				crosshair.custom.setPixel(8, 8, true);
			});
			context.setScreen(() -> new OrbitScreen(ModuleManager.get(CrosshairModule.class)));
			context.waitTicks(12);
			context.takeScreenshot("orbit-10-orbit-crosshair");
			context.setScreen(() -> new CrosshairEditorScreen(ModuleManager.get(CrosshairModule.class)));
			context.waitTicks(10);
			context.takeScreenshot("orbit-11a-crosshair-custom");
			context.runOnClient(mc -> mc.mouseHandler.toString());
			context.getInput().scroll(-10);
			context.waitTicks(8);
			context.takeScreenshot("orbit-11b-crosshair-custom-scrolled");
			context.runOnClient(mc -> ModuleManager.get(CrosshairModule.class).mode.set("Preset"));
			context.getInput().scroll(10);
			context.waitTicks(8);
			context.takeScreenshot("orbit-11c-crosshair-presets");
			context.runOnClient(mc -> {
				var crosshair = ModuleManager.get(CrosshairModule.class);
				crosshair.mode.set("Simple");
				crosshair.colorMode.set("Dynamic");
			});
			context.waitTicks(8);
			context.takeScreenshot("orbit-11d-crosshair-simple");
			// Share code round trip: export, change, import the same code back.
			context.runOnClient(mc -> {
				var grid = ModuleManager.get(CrosshairModule.class).custom;
				String code = grid.toCode();
				boolean[] before = grid.copy();
				grid.clear();
				boolean ok = grid.fromCode(code) && java.util.Arrays.equals(before, grid.copy());
				boolean lunar = grid.fromCode("LCCH-12-AAAAGMN7/u///u///Ic/8AEO");
				System.out.println("[CROSSHAIR] round trip: " + ok + ", Lunar heart code imported: " + lunar + ", code: " + code);
			});
			context.runOnClient(mc -> {
				var crosshair = ModuleManager.get(CrosshairModule.class);
				crosshair.mode.set("Custom");
				crosshair.colorMode.set("Vanilla Blending");
				((CrosshairEditorScreen) mc.screen).setPreviewHeld(true);
			});
			context.waitTicks(4);
			context.takeScreenshot("orbit-11e-crosshair-tab-preview");
			context.runOnClient(mc -> ((CrosshairEditorScreen) mc.screen).setPreviewHeld(false));

			// Zoom: hold the key, scroll in further; the hand should be hidden.
			context.setScreen(() -> null);
			context.waitTicks(5);
			context.getInput().holdKey(mc -> com.orbitclient.OrbitClient.zoomKey);
			context.waitTicks(10);
			context.takeScreenshot("orbit-12a-zoom");
			context.getInput().scroll(3);
			context.waitTicks(10);
			context.takeScreenshot("orbit-12b-zoom-scrolled-in");
			context.getInput().releaseKey(mc -> com.orbitclient.OrbitClient.zoomKey);
			context.waitTicks(10);

			// Closing animation back to the game.
			context.setScreen(OrbitScreen::new);
			context.waitTicks(12);
			context.runOnClient(mc -> mc.screen.onClose());
			context.waitTick();
			context.takeScreenshot("orbit-12-orbit-closing");
			context.waitTicks(10);

			// Motion blur: turn the camera quickly and capture mid-turn.
			context.runOnClient(mc -> {
				var blur = com.orbitclient.module.ModuleManager.get(com.orbitclient.module.visual.MotionBlurModule.class);
				blur.setEnabled(true);
				blur.strength.set(9.0);
			});
			context.waitTicks(5);
			for (int i = 0; i < 6; i++) {
				context.runOnClient(mc -> mc.player.setYRot(mc.player.getYRot() + 12));
				context.waitTicks(1);
			}
			context.takeScreenshot("orbit-13b-motion-blur");
			context.runOnClient(mc -> com.orbitclient.module.ModuleManager.get(com.orbitclient.module.visual.MotionBlurModule.class).setEnabled(false));
			context.waitTicks(5);
			context.takeScreenshot("orbit-13c-motion-blur-off");

			// Shift-click copies chat messages, including messages that wrap over several lines.
			String longMessage = "This is a long chat message that wraps over several lines in the chat box, so clicking any of its lines should copy all of it.";
			context.runOnClient(mc -> {
				mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("first message"));
				mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(longMessage));
				mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("newest message"));
			});
			context.setScreen(() -> new net.minecraft.client.gui.screens.ChatScreen("", false));
			context.waitTicks(3);
			context.runOnClient(mc -> {
				var screen = mc.screen;
				int h = mc.getWindow().getGuiScaledHeight();
				var shift = new net.minecraft.client.input.MouseButtonInfo(0, org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT);
				screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(20, h - 40 - 4.5, shift), false);
				String newest = mc.keyboardHandler.getClipboard();
				screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(20, h - 40 - 9 * 2.5, shift), false);
				String wrapped = mc.keyboardHandler.getClipboard();
				System.out.println("[COPYCHAT] newest ok: " + newest.equals("newest message") + ", wrapped ok: " + wrapped.equals(longMessage) + " (got: " + wrapped + ")");
			});
			context.setScreen(() -> null);
			context.waitTicks(3);

			// New mods: item physics, block outline, hit color, glint, shiny pots, chat stacking, menu blur.
			context.runOnClient(mc -> {
				for (var c : new Class<?>[]{com.orbitclient.module.visual.ItemPhysicsModule.class, com.orbitclient.module.visual.BlockOutlineModule.class,
					com.orbitclient.module.visual.HitColorModule.class, com.orbitclient.module.visual.GlintColorizerModule.class,
					com.orbitclient.module.visual.ShinyPotsModule.class, com.orbitclient.module.server.ChatModule.class,
					com.orbitclient.module.visual.MenuBlurModule.class, com.orbitclient.module.visual.NametagsModule.class}) {
					ModuleManager.get((Class<? extends com.orbitclient.module.Module>) c).setEnabled(true);
				}
				mc.player.setXRot(35);
			});
			sp.getServer().runCommand("item replace entity @p hotbar.0 with diamond_sword[enchantments={sharpness:5}]");
			sp.getServer().runCommand("item replace entity @p hotbar.1 with splash_potion[potion_contents={potion:\"healing\"}]");
			sp.getServer().runCommand("execute as @p at @s run summon item ^-1 ^ ^3 {Item:{id:\"diamond_sword\",count:1},PickupDelay:32767}");
			sp.getServer().runCommand("execute as @p at @s run summon item ^1 ^ ^3 {Item:{id:\"gold_ingot\",count:3},PickupDelay:32767}");
			sp.getServer().runCommand("execute as @p at @s run summon item ^ ^ ^4 {Item:{id:\"oak_log\",count:1},PickupDelay:32767}");
			sp.getServer().runCommand("execute as @p at @s run summon husk ^1 ^ ^5 {NoAI:1b,PersistenceRequired:1b}");
			context.waitTicks(40);
			sp.getServer().runCommand("damage @e[type=husk,limit=1] 1");
			context.waitTicks(1);
			context.takeScreenshot("orbit-13d-new-mods-world");
			context.runOnClient(mc -> {
				for (int i = 0; i < 3; i++) mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Spam message"));
				var chat = (com.orbitclient.mixin.ChatComponentAccessor) mc.gui.getChat();
				System.out.println("[NEWMODS] newest chat: " + chat.orbit$allMessages().getFirst().content().getString()
					+ " | glint texture: " + mc.getTextureManager().getTexture(net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png")).getClass().getSimpleName()
					+ " | hurt tint: " + Integer.toHexString(((com.orbitclient.mixin.OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).orbit$texture().getPixels().getPixel(0, 0))
					+ " | mods: " + ModuleManager.all().size());
			});
			context.runOnClient(mc -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
			context.waitTicks(5);
			context.takeScreenshot("orbit-13e-menu-blur");
			context.setScreen(() -> null);
			sp.getServer().runCommand("kill @e[type=!player]");
			context.waitTicks(3);

			// Saturation: 14 food, 5 saturation, holding cooked beef (preview of +8 food).
			context.runOnClient(mc -> ModuleManager.get(com.orbitclient.module.hud.SaturationModule.class).setEnabled(true));
			sp.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.getFoodData().setFoodLevel(14);
				p.getFoodData().setSaturation(5f);
				p.getFoodData().addExhaustion(2f);
			});
			sp.getServer().runCommand("item replace entity @p hotbar.2 with cooked_beef");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(2));
			context.waitTicks(5);
			context.takeScreenshot("orbit-13f-saturation");

			// Wavey Capes: a test cape, third person from behind, running forward (west).
			sp.getServer().runCommand("tp @p ~ -60 ~ 90 10");
			context.runOnClient(mc -> {
				var img = new com.mojang.blaze3d.platform.NativeImage(64, 32, true);
				for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) {
					int c = 0xFF202020;
					if (x >= 1 && x < 11 && y >= 1 && y < 17) c = ((x + y) % 4 < 2) ? 0xFF3B6FD8 : 0xFFF0A93A; // outside: stripes
					else if (x >= 12 && x < 22 && y >= 1 && y < 17) c = 0xFF2E9E4E; // inside: green
					img.setPixel(x, y, c);
				}
				var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("orbitclient", "test_cape");
				mc.getTextureManager().register(id, new net.minecraft.client.renderer.texture.DynamicTexture(() -> "test cape", img));
				com.orbitclient.module.visual.WaveyCapesModule.testCape = id;
				ModuleManager.get(com.orbitclient.module.visual.WaveyCapesModule.class).setEnabled(true);
				mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
				mc.player.getInventory().setSelectedSlot(4);
				mc.player.setXRot(10);
			});
			for (int i = 0; i < 12; i++) {
				sp.getServer().runCommand("execute as @p at @s run tp @s ~-0.3 ~ ~");
				context.waitTicks(1);
			}
			context.takeScreenshot("orbit-13g-wavey-cape-running");
			context.waitTicks(4);
			context.takeScreenshot("orbit-13g2-wavey-cape-stopping");
			context.waitTicks(30);
			context.takeScreenshot("orbit-13h-wavey-cape-idle");
			// Cosmetics: a few outfits, each from behind and from the front.
			String[][] outfits = {
				{"eclipse", "crown", "shades", "orbit", "wisp", null, null},
				{"galaxy", "cat_ears", "bandana", "rune_ring", "cube_buddy", "dragon_wings", null},
				{"flame", "halo", null, "sparkles", null, "angel_wings", null},
				{"circuit", "headphones", null, "trail", null, "mech_wings", null},
				{"aurora", "top_hat", null, null, null, null, "backpack"},
				{"midnight", null, null, null, null, null, "sword_sheath"},
				{"midnight", null, "prism_visor", "prism_orbit", "moss_frog", "butterfly_wings", "jetpack"},
				{"sakura", null, "rune_mask", "phoenix_crown", "ember_drake", "void_wings", "quiver"},
			};
			context.runOnClient(mc -> com.orbitclient.module.visual.WaveyCapesModule.testCape = null);
			for (int o = 0; o < outfits.length; o++) {
				String[] outfit = outfits[o];
				context.runOnClient(mc -> {
					for (var slot : com.orbitclient.cosmetics.CosmeticSlot.values()) com.orbitclient.cosmetics.CosmeticManager.remove(slot);
					for (String id : outfit) {
						var c = com.orbitclient.cosmetics.Cosmetics.get(id);
						if (c != null) com.orbitclient.cosmetics.CosmeticManager.wear(c, c.defaultColor);
					}
					mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
					mc.player.setXRot(15);
					mc.player.setYBodyRot(mc.player.getYRot());
					mc.player.yBodyRotO = mc.player.getYRot();
					mc.player.setYHeadRot(mc.player.getYRot());
				});
				context.waitTicks(25);
				context.takeScreenshot("orbit-30-cosmetics-" + o + "-back");
				context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
				context.waitTicks(4);
				context.takeScreenshot("orbit-30-cosmetics-" + o + "-front");
			}
			// The wardrobe, wearing the first outfit again.
			context.runOnClient(mc -> {
				for (var slot : com.orbitclient.cosmetics.CosmeticSlot.values()) com.orbitclient.cosmetics.CosmeticManager.remove(slot);
				for (String id : new String[]{"eclipse", "crown", "wisp", "angel_wings"}) {
					var c = com.orbitclient.cosmetics.Cosmetics.get(id);
					com.orbitclient.cosmetics.CosmeticManager.wear(c, c.defaultColor);
				}
				mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			});
			// Finds the real cosmetics server through DNS (dev accounts can't sign in with Mojang).
			context.waitTicks(100);
			context.runOnClient(mc -> System.out.println("[SYNC] " + com.orbitclient.cosmetics.CosmeticSync.status()));
			context.setScreen(com.orbitclient.gui.WardrobeScreen::new);
			context.waitTicks(20);
			context.takeScreenshot("orbit-31-wardrobe-capes");
			context.runOnClient(mc -> ((com.orbitclient.gui.WardrobeScreen) mc.screen).showTab(com.orbitclient.cosmetics.CosmeticSlot.HAT));
			context.waitTicks(30);
			context.takeScreenshot("orbit-32-wardrobe-hats");
			context.runOnClient(mc -> ((com.orbitclient.gui.WardrobeScreen) mc.screen).showTab(com.orbitclient.cosmetics.CosmeticSlot.WINGS));
			context.waitTicks(30);
			context.takeScreenshot("orbit-33-wardrobe-wings");
			context.setScreen(() -> null);
			context.runOnClient(mc -> {
				for (var slot : com.orbitclient.cosmetics.CosmeticSlot.values()) com.orbitclient.cosmetics.CosmeticManager.remove(slot);
				com.orbitclient.cosmetics.CosmeticSync.testServer = null;
			});

			// Waypoints around the player: in front, far off, behind and to the left (edge arrows).
			context.runOnClient(mc -> {
				int px = mc.player.getBlockX(), py = mc.player.getBlockY(), pz = mc.player.getBlockZ();
				String world = WaypointManager.currentWorld(), dim = WaypointManager.currentDimension();
				var base = new com.orbitclient.waypoint.Waypoint("Base", px + 4, py, pz + 25, world, dim, 0xFF55AAFF);
				base.icon = "Home";
				var mine = new com.orbitclient.waypoint.Waypoint("Diamonds", px - 30, py + 6, pz + 90, world, dim, 0xFF55FFFF);
				mine.icon = "Diamond";
				mine.chroma = true;
				var death = new com.orbitclient.waypoint.Waypoint("Death", px + 3, py, pz - 40, world, dim, 0xFFE5484D);
				death.icon = "Skull";
				var flag = new com.orbitclient.waypoint.Waypoint("Outpost", px - 80, py, pz + 2, world, dim, 0xFFF0A93A);
				flag.icon = "Flag";
				WaypointManager.inWorld().forEach(WaypointManager::remove);
				for (var wp : java.util.List.of(base, mine, death, flag)) addWaypoint(wp);
			});
			context.waitTicks(15);
			context.takeScreenshot("orbit-14-waypoints-world");
			context.setScreen(OrbitScreen::new);
			context.waitTicks(4);
			context.runOnClient(mc -> ((OrbitScreen) mc.screen).showPanel("WAYPOINTS"));
			context.waitTicks(10);
			context.takeScreenshot("orbit-15-waypoints-list");
			// Worlds inside a server: a new world starts empty, switching back shows the waypoints again.
			context.runOnClient(mc -> {
				var first = WaypointManager.currentSubWorld();
				int before = WaypointManager.inWorld().size();
				var second = WaypointManager.newSubWorld();
				second.name = "Lobby";
				int inSecond = WaypointManager.inWorld().size();
				WaypointManager.selectSubWorld(first);
				System.out.println("[WORLDS] first=" + first.name + " has " + before + ", new world has " + inSecond
					+ ", back in first: " + WaypointManager.inWorld().size() + ", worlds: " + WaypointManager.worldsOf(first.server).size());
			});
			context.waitTicks(4);
			context.takeScreenshot("orbit-15b-waypoints-worlds");
			context.runOnClient(mc -> ((OrbitScreen) mc.screen).editWaypoint(WaypointManager.inWorld().getFirst()));
			context.waitTicks(10);
			context.takeScreenshot("orbit-16-waypoint-editor");
			context.setScreen(() -> null);
			context.waitTicks(5);

			context.runOnClient(mc -> {
				ModuleManager.get(FpsModule.class).scale.set(0.5);
				ModuleManager.get(CoordinatesModule.class).scale.set(0.7);
			});
			context.waitTicks(8);
			context.takeScreenshot("orbit-13-small-hud");
		}
	}
}
