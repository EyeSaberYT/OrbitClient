package com.orbitclient.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.orbitclient.legacy.OrbitConfig;
import com.orbitclient.legacy.gl.Vec3;
import net.minecraft.client.MinecraftClient;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who wears what, for the old engine. The same rules as the main client's CosmeticManager: your own
 * outfit is saved per account, other players' outfits come from the cosmetics server, and only what
 * the server says this account owns can be worn.
 */
public final class CosmeticManager {
	/** One worn cosmetic and its chosen color. */
	public record Worn(String id, int color) {
	}

	/** Everything one player wears, by slot. */
	public static final class Loadout {
		final Map<CosmeticSlot, Worn> worn = new EnumMap<>(CosmeticSlot.class);

		public Worn get(CosmeticSlot slot) {
			return worn.get(slot);
		}

		public boolean isEmpty() {
			return worn.isEmpty();
		}

		public JsonObject toJson() {
			JsonObject o = new JsonObject();
			worn.forEach((slot, w) -> {
				JsonObject e = new JsonObject();
				e.addProperty("id", w.id());
				e.addProperty("color", w.color());
				o.add(slot.name(), e);
			});
			return o;
		}

		public static Loadout fromJson(JsonElement json) {
			Loadout l = new Loadout();
			if (json == null || !json.isJsonObject()) return l;
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
				try {
					CosmeticSlot slot = CosmeticSlot.valueOf(entry.getKey());
					JsonObject e = entry.getValue().getAsJsonObject();
					Cosmetic c = Cosmetics.get(e.get("id").getAsString());
					// Only cosmetics this client knows, in the right slot.
					if (c != null && c.slot == slot) l.worn.put(slot, new Worn(c.id, e.has("color") ? e.get("color").getAsInt() : c.defaultColor));
				} catch (Exception ignored) {
					// Unknown slot or bad entry: skip it.
				}
			}
			return l;
		}
	}

	private static final Loadout OWN = new Loadout();
	/** Which account OWN belongs to, and the other accounts' outfits on this client. */
	private static final AccountOutfits OUTFITS = new AccountOutfits();
	private static volatile Set<String> OWNED = Set.of();
	private static volatile boolean ownershipLoaded;
	private static final Map<UUID, Loadout> OTHERS = new ConcurrentHashMap<>();
	private static final Map<Integer, Vec3> PET_POS = new HashMap<>();
	private static final Map<Integer, Long> PET_TIME = new HashMap<>();
	private static Runnable onChange = () -> {
	};
	/** A cosmetic being tried on in the wardrobe (shown on you, not saved). */
	private static Cosmetic preview;
	/** The local player's id, kept up to date every tick (see OrbitLegacy.tick). */
	private static volatile UUID self;

	private CosmeticManager() {
	}

	public static Loadout own() {
		return OWN;
	}

	/** Entitlements come from the verified cosmetics server; until fetched, nothing is wearable. */
	public static boolean owns(String id) {
		return ownershipLoaded && OWNED.contains(id);
	}

	public static boolean ownershipLoaded() {
		return ownershipLoaded;
	}

	/**
	 * Install the server-authoritative item ids for {@code account} and clear any now-unowned outfit
	 * entries. Ignored unless {@code account} is the account the outfit belongs to: a late answer for
	 * an account you've since switched away from must not touch this one.
	 */
	public static synchronized void setOwned(UUID account, Set<String> ids) {
		if (account == null || !account.equals(OUTFITS.owner())) return;
		OWNED = Set.copyOf(new HashSet<>(ids));
		ownershipLoaded = true;
		boolean changed = OWN.worn.entrySet().removeIf(entry -> !OWNED.contains(entry.getValue().id()));
		if (preview != null && !OWNED.contains(preview.id)) preview = null;
		if (changed) onChange.run();
	}

	/**
	 * The signed-in account is {@code account}. On a change, the previous account's outfit is put aside
	 * and this account's comes back, and ownership becomes unknown (nothing wearable, nothing taken off)
	 * until the server answers for this account.
	 */
	public static synchronized void useAccount(UUID account) {
		self = account;
		if (account == null || account.equals(OUTFITS.owner())) return;
		boolean adopting = OUTFITS.owner() == null;
		Loadout next = Loadout.fromJson(OUTFITS.switchTo(account, OWN.toJson()));
		OWN.worn.clear();
		OWN.worn.putAll(next.worn);
		OWNED = Set.of();
		ownershipLoaded = false;
		preview = null;
		// Keep the put-aside outfit across restarts. (Adopting an older config changes nothing on disk yet.)
		if (!adopting) OrbitConfig.save();
	}

	/** Called whenever your outfit changes (the sync uploads it, the config is saved). */
	public static void setOnChange(Runnable r) {
		onChange = r;
	}

	public static void wear(Cosmetic c, int color) {
		if (c == null || !owns(c.id)) return;
		OWN.worn.put(c.slot, new Worn(c.id, color));
		onChange.run();
	}

	public static void remove(CosmeticSlot slot) {
		if (OWN.worn.remove(slot) != null) onChange.run();
	}

	public static void setColor(CosmeticSlot slot, int color) {
		Worn w = OWN.worn.get(slot);
		if (w != null) {
			OWN.worn.put(slot, new Worn(w.id(), color));
			onChange.run();
		}
	}

	public static void setOther(UUID uuid, Loadout loadout) {
		if (loadout == null || loadout.isEmpty()) OTHERS.remove(uuid);
		else OTHERS.put(uuid, loadout);
	}

	public static void setPreview(Cosmetic c) {
		preview = c != null && owns(c.id) ? c : null;
	}

	/** The outfit to draw for a player, or null. */
	public static Loadout of(UUID uuid) {
		if (uuid != null && uuid.equals(self)) {
			Loadout allowed = new Loadout();
			if (!ownershipLoaded) return allowed;
			OWN.worn.forEach((slot, worn) -> {
				if (OWNED.contains(worn.id())) allowed.worn.put(slot, worn);
			});
			if (preview == null) return allowed;
			Worn current = OWN.get(preview.slot);
			allowed.worn.put(preview.slot, new Worn(preview.id, current != null && current.id().equals(preview.id) ? current.color() : preview.defaultColor));
			return allowed;
		}
		return OrbitConfig.showOthers ? OTHERS.get(uuid) : null;
	}

	public static synchronized JsonObject save() {
		return OUTFITS.save(OWN.toJson());
	}

	public static synchronized void load(JsonElement json) {
		Loadout l = Loadout.fromJson(OUTFITS.load(json));
		OWN.worn.clear();
		OWN.worn.putAll(l.worn);
	}

	/**
	 * For the start-up audit only (never called in a normal game): pretend the server said this account
	 * owns these, so the drawing code can be exercised without a signed-in account.
	 */
	public static synchronized void auditOwn(UUID account, Set<String> ids) {
		useAccount(account);
		setOwned(account, ids);
	}

	/**
	 * Where a pet should be drawn, in model space (blocks, relative to the model origin): beside the
	 * right shoulder at head height, following the player with a smooth delay.
	 */
	static Vec3 petOffset(int id, double px, double py, double pz, float bodyRot, double time) {
		double r = Math.toRadians(bodyRot);
		double sideX = Math.cos(r), sideZ = Math.sin(r), backX = Math.sin(r), backZ = -Math.cos(r);
		// Target in model pixels: right of the shoulder (-x), above it (-y), a little behind (+z).
		double mx = -11, my = -5, mz = 3;
		double ox = px, oy = py + 1.501, oz = pz;
		Vec3 target = new Vec3(ox + (mx * sideX + mz * backX) / 16, oy - my / 16, oz + (mx * sideZ + mz * backZ) / 16);
		long now = System.nanoTime();
		Vec3 pos = PET_POS.get(id);
		Long last = PET_TIME.get(id);
		// With a menu open (e.g. the wardrobe's turning preview), the pet stays put beside you.
		if (pos == null || last == null || pos.distanceToSqr(target) > 64 || !com.orbitclient.legacy.Game.noScreen(MinecraftClient.getInstance())) {
			pos = target;
		} else {
			double dt = Math.min(0.1, (now - last) / 1e9);
			double k = 1 - Math.exp(-dt * 5);
			pos = pos.add(target.subtract(pos).scale(k));
		}
		PET_POS.put(id, pos);
		PET_TIME.put(id, now);
		if (PET_POS.size() > 256) {
			PET_POS.clear();
			PET_TIME.clear();
		}
		double dx = pos.x - ox, dy = pos.y - oy, dz = pos.z - oz;
		return new Vec3(dx * sideX + dz * sideZ, -dy, dx * backX + dz * backZ);
	}
}
