package com.orbitclient.mixin;

//? if <1.16.1 {
/*import com.orbitclient.compat.ClientLifecycleEvents;
import com.orbitclient.compat.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Minecraft 1.16 itself: fires Orbit's own tick, started and stopping events (see ClientTickEvents).
@Mixin(Minecraft.class)
public class OwnEventsMixin {
	@Unique
	private boolean orbit$started;

	@Inject(method = "tick", at = @At("TAIL"))
	private void orbit$endTick(CallbackInfo ci) {
		Minecraft self = (Minecraft) (Object) this;
		if (!orbit$started) {
			orbit$started = true;
			ClientLifecycleEvents.CLIENT_STARTED.fire(self);
		}
		ClientTickEvents.END_CLIENT_TICK.fire(self);
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void orbit$stopping(CallbackInfo ci) {
		ClientLifecycleEvents.CLIENT_STOPPING.fire((Minecraft) (Object) this);
	}
}
*///?}
