package com.orbitclient.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Each Minecraft account's own outfit on this client. Switching accounts puts the previous account's
 * outfit aside and brings back the new account's, so one account's cosmetics are never shown (or
 * uploaded, or stripped) for another.
 *
 * <p>Saved inside the existing {@code cosmetics} config object as two extra keys, {@code account} and
 * {@code accounts}; older clients skip unknown keys, and an older config (no {@code account}) is adopted
 * by the first account that signs in, exactly as before. Pure (Gson only) so it is tested without the game.
 */
public final class AccountOutfits {
	static final String OWNER_KEY = "account";
	static final String SAVED_KEY = "accounts";
	/** Outfits kept for accounts other than the current one. */
	static final int MAX_ACCOUNTS = 16;

	private final Map<UUID, JsonObject> saved = new LinkedHashMap<>();
	private UUID owner;

	/** The account the current outfit belongs to, or null before any account has been seen. */
	public UUID owner() {
		return owner;
	}

	/**
	 * Switch to {@code account}, given the outfit being worn now. Returns the outfit to wear: the current
	 * one if it already belongs to this account (or to nobody yet), otherwise this account's saved outfit,
	 * or nothing.
	 */
	public JsonObject switchTo(UUID account, JsonObject current) {
		if (account.equals(owner)) return current;
		if (owner == null) {
			owner = account;
			return current;
		}
		saved.remove(owner);
		saved.put(owner, copy(current));
		while (saved.size() > MAX_ACCOUNTS) saved.remove(saved.keySet().iterator().next());
		JsonObject next = saved.remove(account);
		owner = account;
		return next == null ? new JsonObject() : next;
	}

	/** The config object: the current outfit plus who it belongs to and the other accounts' outfits. */
	public JsonObject save(JsonObject current) {
		JsonObject o = copy(current);
		if (owner != null) o.addProperty(OWNER_KEY, owner.toString());
		if (!saved.isEmpty()) {
			JsonObject others = new JsonObject();
			saved.forEach((uuid, outfit) -> others.add(uuid.toString(), copy(outfit)));
			o.add(SAVED_KEY, others);
		}
		return o;
	}

	/** Reads a config object written by {@link #save} (or an older one) and returns the current outfit. */
	public JsonObject load(JsonElement json) {
		saved.clear();
		owner = null;
		if (json == null || !json.isJsonObject()) return new JsonObject();
		JsonObject o = copy(json.getAsJsonObject());
		owner = ClaimsResponse.uuid(o.remove(OWNER_KEY));
		JsonElement others = o.remove(SAVED_KEY);
		if (others != null && others.isJsonObject()) {
			for (var e : others.getAsJsonObject().entrySet()) {
				UUID uuid = ClaimsResponse.uuid(new com.google.gson.JsonPrimitive(e.getKey()));
				if (uuid != null && !uuid.equals(owner) && e.getValue().isJsonObject() && saved.size() < MAX_ACCOUNTS) {
					saved.put(uuid, copy(e.getValue().getAsJsonObject()));
				}
			}
		}
		return o;
	}

	/** A deep copy. (JsonObject.deepCopy() is not public in the Gson that older Minecraft versions ship.) */
	private static JsonObject copy(JsonObject json) {
		return JsonParser.parseString(json.toString()).getAsJsonObject();
	}
}
