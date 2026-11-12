package com.orbitclient.compat;

//? if <1.21.9 {
/*/^*
 * Before 1.21.9: the parts of 1.21.9's net.minecraft.client.input.MouseButtonEvent that Orbit's screens use,
 * made from the old mouseClicked(x, y, button). build.gradle swaps the import and the method signatures.
 ^/
public record MouseButtonEvent(double x, double y, int button, int modifiers) {
	public MouseButtonEvent(double x, double y, int button) {
		this(x, y, button, Modifiers.current());
	}
}
*///?}
