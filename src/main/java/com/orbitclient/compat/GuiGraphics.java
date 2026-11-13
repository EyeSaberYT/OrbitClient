package com.orbitclient.compat;

//? if >=1.16 <1.20 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
//? if >=1.16.2
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

// Before 1.20 there was no GuiGraphics: screens and the HUD drew through a PoseStack and static helpers.
// This gives Orbit's menus and HUD the 1.20 calls they make on top of those; build.gradle points their
// GuiGraphics import here and wraps the PoseStack vanilla hands to render methods and mixins.
public final class GuiGraphics {
	// GuiComponent's gradient fill is protected.
	private static final class Access extends GuiComponent {
		static final Access INSTANCE = new Access();

		// An instance method before 1.19.4.
		void gradient(PoseStack pose, int x1, int y1, int x2, int y2, int from, int to) {
			fillGradient(pose, x1, y1, x2, y2, from, to);
		}
	}

	// The last one made: there's one GUI pose stack per frame, so it's normally reused.
	private static GuiGraphics last;
	private final PoseStack pose;

	private GuiGraphics(PoseStack pose) {
		this.pose = pose;
	}

	public static GuiGraphics of(PoseStack pose) {
		GuiGraphics cached = last;
		if (cached == null || cached.pose != pose) last = cached = new GuiGraphics(pose);
		return cached;
	}

	public PoseStack pose() {
		return pose;
	}

	// The same stack under a name build.gradle's pose() rules leave alone.
	public PoseStack poseStack() {
		return pose;
	}

	public int guiWidth() {
		return Minecraft.getInstance().getWindow().getGuiScaledWidth();
	}

	public int guiHeight() {
		return Minecraft.getInstance().getWindow().getGuiScaledHeight();
	}

	public MultiBufferSource.BufferSource bufferSource() {
		return Minecraft.getInstance().renderBuffers().bufferSource();
	}

	public void flush() {
		RenderSystem.disableDepthTest();
		bufferSource().endBatch();
		RenderSystem.enableDepthTest();
	}

	public void setColor(float r, float g, float b, float a) {
		//? if >=1.17 {
		RenderSystem.setShaderColor(r, g, b, a);
		//?} else {
		/^RenderSystem.color4f(r, g, b, a);
		^///?}
	}

	public void fill(int x1, int y1, int x2, int y2, int color) {
		GuiComponent.fill(pose, x1, y1, x2, y2, color);
	}

	// The 1.20+ calls name a render layer; before 1.20 every GUI fill is the plain one.
	public void fill(Object layer, int x1, int y1, int x2, int y2, int color) {
		fill(x1, y1, x2, y2, color);
	}

	public void fillGradient(int x1, int y1, int x2, int y2, int from, int to) {
		Access.INSTANCE.gradient(pose, x1, y1, x2, y2, from, to);
	}

	public void enableScissor(int x1, int y1, int x2, int y2) {
		//? if >=1.19.1 {
		GuiComponent.enableScissor(x1, y1, x2, y2);
		//?} else {
		/^int[] top = SCISSORS.peek();
		int[] area = top == null ? new int[] {x1, y1, x2, y2}
			: new int[] {Math.max(x1, top[0]), Math.max(y1, top[1]), Math.min(x2, top[2]), Math.min(y2, top[3])};
		SCISSORS.push(area);
		applyScissor(area);
		^///?}
	}

	public void disableScissor() {
		//? if >=1.19.1 {
		GuiComponent.disableScissor();
		//?} else {
		/^if (!SCISSORS.isEmpty()) SCISSORS.pop();
		applyScissor(SCISSORS.peek());
		^///?}
	}

	// Before 1.19.1 the game had no scissor helper: nested areas, in GUI pixels, like the later one.
	private static final java.util.ArrayDeque<int[]> SCISSORS = new java.util.ArrayDeque<>();

	private static void applyScissor(int[] area) {
		if (area == null) {
			//? if >=1.17 {
			RenderSystem.disableScissor();
			//?} else {
			/^org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
			^///?}
			return;
		}
		var window = Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		int sx = (int) (area[0] * scale), sy = (int) (window.getHeight() - area[3] * scale);
		int sw = Math.max(0, (int) ((area[2] - area[0]) * scale)), sh = Math.max(0, (int) ((area[3] - area[1]) * scale));
		//? if >=1.17 {
		RenderSystem.enableScissor(sx, sy, sw, sh);
		//?} else {
		/^// Not every 1.16 has RenderSystem's scissor calls.
		org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
		org.lwjgl.opengl.GL11.glScissor(sx, sy, sw, sh);
		^///?}
	}

	public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
		return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
	}

	public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
		return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
	}

	//? if >=1.16.2 {
	public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
	}
	//?}

	public int drawString(Font font, String text, int x, int y, int color) {
		return drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, Component text, int x, int y, int color) {
		return drawString(font, text, x, y, color, true);
	}

	public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		bind(texture);
		RenderSystem.enableBlend();
		GuiComponent.blit(pose, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		bind(texture);
		RenderSystem.enableBlend();
		GuiComponent.blit(pose, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
	}

	public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
		blit(texture, x, y, (float) u, (float) v, width, height, 256, 256);
	}

	public void blit(int x, int y, int z, int width, int height, TextureAtlasSprite sprite) {
		//? if >=1.19.3 {
		bind(sprite.atlasLocation());
		//?} else {
		/^bind(sprite.atlas().location());
		^///?}
		RenderSystem.enableBlend();
		GuiComponent.blit(pose, x, y, z, width, height, sprite);
	}

	public void renderItem(ItemStack stack, int x, int y) {
		//? if >=1.19.4 {
		Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(pose, stack, x, y);
		//?} else {
		/^withModelView(() -> Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y));
		^///?}
	}

	public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
		//? if >=1.19.4 {
		Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(pose, font, stack, x, y);
		//?} else {
		/^withModelView(() -> Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x, y));
		^///?}
	}

	// Before 1.19.4 items in the GUI are drawn from the model-view matrix alone: this puts the pose on it meanwhile.
	private void withModelView(Runnable draw) {
		//? if >=1.17 {
		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.mulPoseMatrix(pose.last().pose());
		RenderSystem.applyModelViewMatrix();
		draw.run();
		modelView.popPose();
		RenderSystem.applyModelViewMatrix();
		//?} else {
		/^// Before 1.17 the model-view matrix is OpenGL's own.
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(pose.last().pose());
		draw.run();
		RenderSystem.popMatrix();
		^///?}
	}

	// The texture the next blit draws (a shader texture from 1.17, the bound one before).
	private static void bind(ResourceLocation texture) {
		//? if >=1.17 {
		RenderSystem.setShaderTexture(0, texture);
		//?} else {
		/^Minecraft.getInstance().getTextureManager().bind(texture);
		^///?}
	}
}
*///?}

//? if <1.16 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// Before 1.16 menus and the HUD draw straight to OpenGL: there is no pose stack to be handed. Orbit keeps one of
// its own for its transforms and puts it on OpenGL's matrix around each vanilla draw call.
public final class GuiGraphics {
	private static final class Access extends GuiComponent {
		static final Access INSTANCE = new Access();

		void gradient(int x1, int y1, int x2, int y2, int from, int to) {
			fillGradient(x1, y1, x2, y2, from, to);
		}
	}

	private static final GuiGraphics LEGACY = new GuiGraphics(new PoseStack());
	private static GuiGraphics last;
	private final PoseStack pose;

	private GuiGraphics(PoseStack pose) {
		this.pose = pose;
	}

	// The one GUI pose stack (vanilla has none to hand over).
	public static GuiGraphics legacy() {
		return LEGACY;
	}

	public static GuiGraphics of(PoseStack pose) {
		GuiGraphics cached = last;
		if (cached == null || cached.pose != pose) last = cached = new GuiGraphics(pose);
		return cached;
	}

	public PoseStack pose() {
		return pose;
	}

	public PoseStack poseStack() {
		return pose;
	}

	public int guiWidth() {
		return Minecraft.getInstance().getWindow().getGuiScaledWidth();
	}

	public int guiHeight() {
		return Minecraft.getInstance().getWindow().getGuiScaledHeight();
	}

	public MultiBufferSource.BufferSource bufferSource() {
		return Minecraft.getInstance().renderBuffers().bufferSource();
	}

	public void flush() {
		RenderSystem.disableDepthTest();
		bufferSource().endBatch();
		RenderSystem.enableDepthTest();
	}

	public void setColor(float r, float g, float b, float a) {
		RenderSystem.color4f(r, g, b, a);
	}

	private void transformed(Runnable draw) {
		RenderSystem.pushMatrix();
		RenderSystem.multMatrix(pose.last().pose());
		draw.run();
		RenderSystem.popMatrix();
	}

	public void fill(int x1, int y1, int x2, int y2, int color) {
		GuiComponent.fill(pose.last().pose(), x1, y1, x2, y2, color);
	}

	public void fill(Object layer, int x1, int y1, int x2, int y2, int color) {
		fill(x1, y1, x2, y2, color);
	}

	public void fillGradient(int x1, int y1, int x2, int y2, int from, int to) {
		transformed(() -> Access.INSTANCE.gradient(x1, y1, x2, y2, from, to));
	}

	private static final java.util.ArrayDeque<int[]> SCISSORS = new java.util.ArrayDeque<>();

	public void enableScissor(int x1, int y1, int x2, int y2) {
		int[] top = SCISSORS.peek();
		int[] area = top == null ? new int[] {x1, y1, x2, y2}
			: new int[] {Math.max(x1, top[0]), Math.max(y1, top[1]), Math.min(x2, top[2]), Math.min(y2, top[3])};
		SCISSORS.push(area);
		applyScissor(area);
	}

	public void disableScissor() {
		if (!SCISSORS.isEmpty()) SCISSORS.pop();
		applyScissor(SCISSORS.peek());
	}

	private static void applyScissor(int[] area) {
		if (area == null) {
			org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
			return;
		}
		var window = Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
		org.lwjgl.opengl.GL11.glScissor((int) (area[0] * scale), (int) (window.getHeight() - area[3] * scale),
			Math.max(0, (int) ((area[2] - area[0]) * scale)), Math.max(0, (int) ((area[3] - area[1]) * scale)));
	}

	public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
		transformed(() -> {
			if (shadow) font.drawShadow(text, x, y, color);
			else font.draw(text, x, y, color);
		});
		return x + font.width(text);
	}

	// Styles become formatting codes, which is what the font understands before 1.16.
	public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
		return drawString(font, text.getColoredString(), x, y, color, shadow);
	}

	public int drawString(Font font, String text, int x, int y, int color) {
		return drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, Component text, int x, int y, int color) {
		return drawString(font, text, x, y, color, true);
	}

	private static void bind(ResourceLocation texture) {
		Minecraft.getInstance().getTextureManager().bind(texture);
		RenderSystem.enableBlend();
	}

	public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		bind(texture);
		transformed(() -> GuiComponent.blit(x, y, u, v, width, height, textureWidth, textureHeight));
	}

	public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		bind(texture);
		transformed(() -> GuiComponent.blit(x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight));
	}

	public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
		blit(texture, x, y, (float) u, (float) v, width, height, 256, 256);
	}

	public void blit(int x, int y, int z, int width, int height, TextureAtlasSprite sprite) {
		bind(sprite.atlas().location());
		transformed(() -> GuiComponent.blit(x, y, z, width, height, sprite));
	}

	public void renderItem(ItemStack stack, int x, int y) {
		transformed(() -> Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y));
	}

	public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
		transformed(() -> Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x, y));
	}
}
*///?}
