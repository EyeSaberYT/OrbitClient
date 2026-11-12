package com.orbitclient.compat;

//? if <1.21 {
/*import net.minecraft.client.Minecraft;

/^* Before 1.21 the frame's partial tick was a plain float: the part of 1.21's DeltaTracker Orbit uses. ^/
public final class DeltaTracker {
	private final float partialTick;

	private DeltaTracker(float partialTick) {
		this.partialTick = partialTick;
	}

	public static DeltaTracker of(float partialTick) {
		return new DeltaTracker(partialTick);
	}

	public static DeltaTracker of(Minecraft mc) {
		return new DeltaTracker(mc.getFrameTime());
	}

	public float getGameTimeDeltaPartialTick(boolean ignoreFrozenGame) {
		return partialTick;
	}
}
*///?}
