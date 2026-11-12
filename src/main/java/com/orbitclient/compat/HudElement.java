package com.orbitclient.compat;

//? if <1.21.6 {
/*import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/^* Before 1.21.6: Fabric's HudElement (see HudElementRegistry). ^/
@FunctionalInterface
public interface HudElement {
	void render(GuiGraphics graphics, DeltaTracker delta);
}
*///?}
