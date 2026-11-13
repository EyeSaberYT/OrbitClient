package com.orbitclient.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reads the cosmetics server's {@code GET /shop/claims} answer:
 * {@code {"uuid":"<the signed-in account>","cosmetics":["id", ...]}}. The list is the server's merge of
 * shop ownership and website redemptions; only the server decides it.
 *
 * <p>Pure (Gson only) so it is tested without the game. Anything malformed, or an answer about another
 * account, throws: the caller then keeps the ownership it has instead of replacing it with nothing.
 */
public final class ClaimsResponse {
	private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,64}");
	private static final int MAX_IDS = 512;

	private ClaimsResponse() {
	}

	/** The cosmetics owned by {@code expected}, or IllegalArgumentException when the answer can't be trusted. */
	public static Set<String> parse(String body, UUID expected) {
		JsonElement root;
		try {
			root = JsonParser.parseString(body == null ? "" : body);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("claims answer is not JSON");
		}
		if (root == null || !root.isJsonObject()) throw new IllegalArgumentException("claims answer is not an object");
		JsonObject o = root.getAsJsonObject();
		UUID answered = uuid(o.get("uuid"));
		if (answered == null || !answered.equals(expected)) throw new IllegalArgumentException("claims answer is for another account");
		JsonElement list = o.get("cosmetics");
		if (list == null || !list.isJsonArray()) throw new IllegalArgumentException("claims answer has no cosmetics list");
		if (list.getAsJsonArray().size() > MAX_IDS) throw new IllegalArgumentException("claims answer is too long");
		Set<String> ids = new LinkedHashSet<>();
		for (JsonElement item : list.getAsJsonArray()) {
			if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString() || !ID.matcher(item.getAsString()).matches()) {
				throw new IllegalArgumentException("claims answer has a malformed cosmetic id");
			}
			ids.add(item.getAsString());
		}
		return Set.copyOf(ids);
	}

	/** A dashed or 32-hex UUID string, or null. */
	static UUID uuid(JsonElement value) {
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
		String s = value.getAsString().toLowerCase(java.util.Locale.ROOT);
		if (s.matches("[0-9a-f]{32}")) {
			s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20);
		}
		if (!s.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) return null;
		return UUID.fromString(s);
	}
}
