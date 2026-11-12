package com.orbitclient.compat;

//? if <1.21.9 {
/*/^* Before 1.21.9: 1.21.9's net.minecraft.client.input.KeyEvent, made from the old keyPressed(key, scancode, modifiers). ^/
public record KeyEvent(int key, int scancode, int modifiers) {
}
*///?}
