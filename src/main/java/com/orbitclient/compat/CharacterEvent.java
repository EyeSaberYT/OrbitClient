package com.orbitclient.compat;

//? if <1.21.9 {
/*/^* Before 1.21.9: 1.21.9's net.minecraft.client.input.CharacterEvent, made from the old charTyped(char, modifiers). ^/
public record CharacterEvent(int codepoint, int modifiers) {
	public boolean isAllowedChatCharacter() {
		//? if >=1.20.5 {
		return net.minecraft.util.StringUtil.isAllowedChatCharacter((char) codepoint);
		//?} else
		/^return net.minecraft.SharedConstants.isAllowedChatCharacter((char) codepoint);^/
	}

	public String codepointAsString() {
		return Character.toString(codepoint);
	}
}
*///?}
