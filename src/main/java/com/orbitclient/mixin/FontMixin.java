package com.orbitclient.mixin;

import com.orbitclient.util.TextLayoutCache;
import net.minecraft.client.gui.Font;
//? if >=1.21.9 {
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
//?}
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuses laid-out text that hasn't changed since it was last drawn (see TextLayoutCache). */
@Mixin(Font.class)
public abstract class FontMixin {
	//? if >=1.21.9 {
	@Shadow
	private GlyphSource getGlyphSource(FontDescription font) {
		throw new AssertionError();
	}

	/** The default font's glyphs: another object once the fonts reload (see TextLayoutCache). */
	private Object orbit$glyphs() {
		return getGlyphSource(FontDescription.DEFAULT);
	}
	//?} else {
	/*@Shadow
	net.minecraft.client.gui.font.FontSet getFontSet(net.minecraft.resources.Identifier font) {
		throw new AssertionError();
	}

	/^* The default font's glyphs: another object once the fonts reload (see TextLayoutCache). ^/
	private Object orbit$glyphs() {
		return getFontSet(net.minecraft.network.chat.Style.DEFAULT_FONT);
	}
	*///?}

	//? if >=1.21.11 {
	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
		at = @At("HEAD"), cancellable = true)
	private void orbit$reuseLayout(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty,
	                               int background, CallbackInfoReturnable<Font.PreparedText> cir) {
		Font.PreparedText cached = TextLayoutCache.find((Font) (Object) this, orbit$glyphs(), text,
			x, y, color, shadow, includeEmpty, background);
		if (cached != null) cir.setReturnValue(cached);
	}

	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
		at = @At("RETURN"))
	private void orbit$keepLayout(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty,
	                              int background, CallbackInfoReturnable<Font.PreparedText> cir) {
		TextLayoutCache.store(text, cir.getReturnValue());
	}
	//?} else {
	/*// Before 1.21.11 prepareText has no includeEmpty (it's always false there).
	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
		at = @At("HEAD"), cancellable = true)
	private void orbit$reuseLayout(FormattedCharSequence text, float x, float y, int color, boolean shadow, int background,
	                               CallbackInfoReturnable<Font.PreparedText> cir) {
		Font.PreparedText cached = TextLayoutCache.find((Font) (Object) this, orbit$glyphs(), text,
			x, y, color, shadow, false, background);
		if (cached != null) cir.setReturnValue(cached);
	}

	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
		at = @At("RETURN"))
	private void orbit$keepLayout(FormattedCharSequence text, float x, float y, int color, boolean shadow, int background,
	                              CallbackInfoReturnable<Font.PreparedText> cir) {
		TextLayoutCache.store(text, cir.getReturnValue());
	}
	*///?}
}
