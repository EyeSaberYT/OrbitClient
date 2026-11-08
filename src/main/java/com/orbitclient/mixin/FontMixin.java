package com.orbitclient.mixin;

import com.orbitclient.util.TextLayoutCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuses laid-out text that hasn't changed since it was last drawn (see TextLayoutCache). */
@Mixin(Font.class)
public abstract class FontMixin {
	@Shadow
	private GlyphSource getGlyphSource(FontDescription font) {
		throw new AssertionError();
	}

	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
		at = @At("HEAD"), cancellable = true)
	private void orbit$reuseLayout(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty,
	                               int background, CallbackInfoReturnable<Font.PreparedText> cir) {
		Font.PreparedText cached = TextLayoutCache.find((Font) (Object) this, getGlyphSource(FontDescription.DEFAULT), text,
			x, y, color, shadow, includeEmpty, background);
		if (cached != null) cir.setReturnValue(cached);
	}

	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
		at = @At("RETURN"))
	private void orbit$keepLayout(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty,
	                              int background, CallbackInfoReturnable<Font.PreparedText> cir) {
		TextLayoutCache.store(text, cir.getReturnValue());
	}
}
