package com.orbitclient.test;

import com.orbitclient.module.ModuleManager;
import com.orbitclient.module.visual.WaveyCapesModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Records Wavey Capes through one fixed sequence of moves: idle, walking, sprinting, stopping, falling,
 * turning and strafing. The player looks south (the third-person camera sits north of them) but walks
 * west; vanilla keeps the body within 75 degrees of the look, so it faces nearly west and the camera sees
 * the cape almost from the side.
 */
@SuppressWarnings("UnstableApiUsage")
public class CapeMotionTest implements FabricClientGameTest {
	/** While set, the body is turned west every tick (vanilla then keeps it 75 degrees from the look): a side view. */
	private static volatile boolean sideView;

	@Override
	public void runTest(ClientGameTestContext context) {
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			if (sideView && mc.player != null) mc.player.setYBodyRot(90);
		});
		context.getInput().resizeWindow(1600, 900);
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			var server = sp.getServer();
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("tp @p 0 -60 0 90 0");
			server.runCommand("tp @p 0 -60 0 0 12");
			context.waitTicks(5);
			context.runOnClient(mc -> {
				var img = new com.mojang.blaze3d.platform.NativeImage(64, 32, true);
				for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++) {
					int c = 0xFF202020;
					if (x >= 1 && x < 11 && y >= 1 && y < 17) c = ((x + y) % 4 < 2) ? 0xFF3B6FD8 : 0xFFF0A93A;
					else if (x >= 12 && x < 22 && y >= 1 && y < 17) c = 0xFF2E9E4E;
					img.setPixel(x, y, c);
				}
				var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("orbitclient", "test_cape_motion");
				mc.getTextureManager().register(id, new net.minecraft.client.renderer.texture.DynamicTexture(() -> "test cape", img));
				WaveyCapesModule.testCape = id;
				ModuleManager.get(WaveyCapesModule.class).setEnabled(true);
				mc.options.hideGui = true;
				mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
			});
			sideView = true;
			context.waitTicks(40);
			context.takeScreenshot("cape-01-idle");

			move(context, server, -0.216, 0, 20);
			context.takeScreenshot("cape-02-walk");
			move(context, server, -0.28, 0, 20);
			context.takeScreenshot("cape-03-sprint");
			context.waitTicks(2);
			context.takeScreenshot("cape-04-stop-2");
			context.waitTicks(3);
			context.takeScreenshot("cape-05-stop-5");
			context.waitTicks(5);
			context.takeScreenshot("cape-06-stop-10");
			context.waitTicks(10);
			context.takeScreenshot("cape-07-stop-20");

			server.runCommand("tp @p ~ ~1.25 ~");
			context.waitTicks(3);
			context.takeScreenshot("cape-08-fall-3");
			context.waitTicks(4);
			context.takeScreenshot("cape-09-fall-7");
			context.waitTicks(15);
			context.takeScreenshot("cape-10-landed");

			sideView = false;
			context.waitTicks(20);
			for (int i = 0; i < 8; i++) {
				server.runCommand("execute as @p at @s run tp @s ~ ~ ~ ~-12 ~");
				context.waitTicks(1);
			}
			context.takeScreenshot("cape-11-turning");
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 ~");
			context.waitTicks(30);

			move(context, server, -0.2, 0, 15); // body and look face south, so moving west is a strafe
			context.takeScreenshot("cape-12-strafe");
			context.waitTicks(30);
			sideView = true;
			context.waitTicks(20);
			move(context, server, -0.28, 0, 10);
			jumpArc(context, server, new double[]{0.30, 0.25, 0.19, 0.12});
			context.takeScreenshot("cape-13a-jump-rising");
			jumpArc(context, server, new double[]{0.04, -0.08, -0.16, -0.22});
			context.takeScreenshot("cape-13b-jump-falling");
			jumpArc(context, server, new double[]{-0.18, -0.12, -0.08, -0.06});
			context.takeScreenshot("cape-13c-jump-landed");
			context.runOnClient(mc -> {
				mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
				mc.options.hideGui = false;
				WaveyCapesModule.testCape = null;
			});
		}
	}

	/** Moves the player a step per tick. */
	private static void move(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, double dx, double dz, int ticks) {
		for (int i = 0; i < ticks; i++) {
			server.runCommand("execute as @p at @s run tp @s ~" + dx + " ~ ~" + dz);
			context.waitTicks(1);
		}
	}

	/** Server-driven per-tick vertical arc, avoiding a teleport jump or synthetic key packet. */
	private static void jumpArc(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server, double[] deltas) {
		for (double dy : deltas) {
			server.runCommand("execute as @p at @s run tp @s ~ ~" + dy + " ~");
			context.waitTicks(1);
		}
	}
}
