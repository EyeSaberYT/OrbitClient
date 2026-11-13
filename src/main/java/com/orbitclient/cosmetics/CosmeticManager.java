package com.orbitclient.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who wears what. Your own loadout is saved with the client settings; other players' loadouts come
 * from the cosmetics server (see CosmeticSync) when one is set up.
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
			for (var entry : json.getAsJsonObject().entrySet()) {
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
		if (account == null || account.equals(OUTFITS.owner())) return;
		boolean adopting = OUTFITS.owner() == null;
		Loadout next = Loadout.fromJson(OUTFITS.switchTo(account, OWN.toJson()));
		OWN.worn.clear();
		OWN.worn.putAll(next.worn);
		OWNED = Set.of();
		ownershipLoaded = false;
		preview = null;
		// Keep the put-aside outfit across restarts. (Adopting an older config changes nothing on disk yet.)
		if (!adopting) com.orbitclient.config.ConfigManager.save();
	}

	/** Called whenever your loadout changes (the sync uploads it). */
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

	/** The loadout to draw for a player, or null. */
	public static Loadout of(UUID uuid) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getUUID().equals(uuid)) {
			Loadout allowed = new Loadout();
			if (!ownershipLoaded) return allowed;
			OWN.worn.forEach((slot, worn) -> {
				if (OWNED.contains(worn.id())) allowed.worn.put(slot, worn);
			});
			if (preview == null) return allowed;
			Loadout l = new Loadout();
			l.worn.putAll(allowed.worn);
			Worn current = OWN.get(preview.slot);
			l.worn.put(preview.slot, new Worn(preview.id, current != null && current.id().equals(preview.id) ? current.color() : preview.defaultColor));
			return l;
		}
		return com.orbitclient.config.ClientSettings.INSTANCE.showOthersCosmetics.isOn() ? OTHERS.get(uuid) : null;
	}

	/** How far (in blocks) to raise a player's nametag so it clears their hat. */
	public static float nameTagLift(UUID uuid) {
		Loadout loadout = of(uuid);
		Worn hat = loadout == null ? null : loadout.get(CosmeticSlot.HAT);
		Cosmetic c = hat == null ? null : Cosmetics.get(hat.id());
		return c == null ? 0 : c.nameLift;
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
		if (pos == null || last == null || pos.distanceToSqr(target) > 64 || Minecraft.getInstance().screen != null) {
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

	/** Stardust Trail: colored particles behind players wearing it, while they move. */
	public static void tickTrails() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		for (Player p : mc.level.players()) {
			Loadout l = of(p.getUUID());
			Worn w = l == null ? null : l.get(CosmeticSlot.AURA);
			if (w == null || !"trail".equals(w.id()) || p.isInvisible()) continue;
			double moved = p.position().distanceToSqr(p.xo, p.yo, p.zo);
			if (moved < 0.0025) continue;
			var random = p.getRandom();
			for (int i = 0; i < 3; i++) {
				int color = i == 0 ? 0xFFFFFF : w.color() & 0xFFFFFF;
				mc.level.addParticle(new DustParticleOptions(color, 0.6f + random.nextFloat() * 0.5f),
					p.xo + (random.nextDouble() - 0.5) * 0.5, p.yo + 0.1 + random.nextDouble() * 1.2, p.zo + (random.nextDouble() - 0.5) * 0.5, 0, 0.01, 0);
			}
		}
	}
}
