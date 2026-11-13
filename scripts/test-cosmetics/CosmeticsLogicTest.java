import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orbitclient.cosmetics.AccountOutfits;
import com.orbitclient.cosmetics.ClaimsResponse;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Tests for the game-independent cosmetics logic (claims answers and per-account outfits), with
 * made-up accounts and items only. Run with scripts/test-cosmetics/run.sh (needs only a JDK and Gson).
 */
public final class CosmeticsLogicTest {
	static final UUID A = UUID.fromString("6507e531-fdf0-4ac2-ace7-d15041a9f182");
	static final UUID B = UUID.fromString("11111111-2222-3333-4444-555555555555");
	static int passed, failed;

	public static void main(String[] args) {
		test("claims: the documented answer is read for the signed-in account", () -> {
			eq(Set.of("stormfront", "stellar"), ClaimsResponse.parse("{\"uuid\":\"" + A + "\",\"cosmetics\":[\"stormfront\",\"stellar\"]}", A), "dashed UUID");
			eq(Set.of(), ClaimsResponse.parse("{\"uuid\":\"6507e531fdf04ac2ace7d15041a9f182\",\"cosmetics\":[]}", A), "32-hex UUID, owns nothing");
		});
		test("claims: an answer about another account is refused", () ->
			throwsIae(() -> ClaimsResponse.parse("{\"uuid\":\"" + B + "\",\"cosmetics\":[\"stormfront\"]}", A), "B's answer while signed in as A"));
		test("claims: malformed answers are refused instead of meaning 'owns nothing'", () -> {
			String[] bad = {"", "null", "[]", "<html>502</html>", "{\"cosmetics\":[\"stormfront\"]}",
				"{\"uuid\":\"" + A + "\"}", "{\"uuid\":\"" + A + "\",\"cosmetics\":{}}",
				"{\"uuid\":\"" + A + "\",\"cosmetics\":[7]}", "{\"uuid\":\"" + A + "\",\"cosmetics\":[\"Not An Id\"]}",
				"{\"uuid\":\"not-a-uuid\",\"cosmetics\":[]}"};
			for (String body : bad) throwsIae(() -> ClaimsResponse.parse(body, A), "malformed: " + body);
		});

		test("outfits: an older config is adopted by the first account, unchanged", () -> {
			AccountOutfits o = new AccountOutfits();
			JsonObject current = o.load(json("{\"CAPE\":{\"id\":\"stormfront\",\"color\":-1}}"));
			eq(null, o.owner(), "no owner yet");
			eq(current, o.switchTo(A, current), "A keeps the outfit");
			eq(A, o.owner(), "now A's");
		});
		test("outfits: switching accounts never carries one account's outfit to another", () -> {
			AccountOutfits o = new AccountOutfits();
			JsonObject aOutfit = json("{\"CAPE\":{\"id\":\"stormfront\",\"color\":-1}}");
			o.switchTo(A, aOutfit);
			JsonObject bOutfit = o.switchTo(B, aOutfit);
			eq(new JsonObject(), bOutfit, "B starts with nothing, not A's cape");
			JsonObject bWears = json("{\"HAT\":{\"id\":\"crown\",\"color\":3}}");
			eq(aOutfit, o.switchTo(A, bWears), "A gets its own cape back");
			eq(bWears, o.switchTo(B, aOutfit), "B gets its own hat back");
		});
		test("outfits: who owns what survives a restart (save, then load)", () -> {
			AccountOutfits o = new AccountOutfits();
			JsonObject aOutfit = json("{\"CAPE\":{\"id\":\"stormfront\",\"color\":-1}}");
			o.switchTo(A, aOutfit);
			JsonObject bWears = json("{\"HAT\":{\"id\":\"crown\",\"color\":3}}");
			o.switchTo(B, aOutfit);
			JsonObject saved = o.save(bWears);
			that(saved.has("HAT") && !saved.has("CAPE"), "slot keys stay where older clients read them: " + saved);
			AccountOutfits restored = new AccountOutfits();
			JsonObject current = restored.load(JsonParser.parseString(saved.toString()));
			eq(bWears, current, "B's outfit is current");
			eq(B, restored.owner(), "and belongs to B");
			eq(aOutfit, restored.switchTo(A, current), "A's outfit was kept for A");
		});
		test("outfits: a config can't make an account's outfit appear under another account", () -> {
			AccountOutfits o = new AccountOutfits();
			JsonObject current = o.load(json("{\"account\":\"" + A + "\",\"CAPE\":{\"id\":\"stormfront\",\"color\":-1},"
				+ "\"accounts\":{\"" + A + "\":{\"HAT\":{\"id\":\"crown\",\"color\":1}},\"bogus\":{}}}"));
			eq(A, o.owner(), "owner read");
			eq(new JsonObject(), o.switchTo(B, current), "B gets nothing");
			eq(json("{\"CAPE\":{\"id\":\"stormfront\",\"color\":-1}}"), o.switchTo(A, new JsonObject()), "A gets its current outfit, not the duplicate entry");
		});
		test("outfits: at most " + 16 + " other accounts are remembered", () -> {
			AccountOutfits o = new AccountOutfits();
			o.switchTo(A, new JsonObject());
			for (int i = 0; i < 40; i++) o.switchTo(new UUID(1, i), json("{\"HAT\":{\"id\":\"crown\",\"color\":" + i + "}}"));
			that(o.save(new JsonObject()).getAsJsonObject("accounts").size() <= 16, "bounded");
		});

		System.out.println(passed + " passed, " + failed + " failed");
		System.exit(failed == 0 ? 0 : 1);
	}

	static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	interface Body { void run() throws Exception; }

	static void test(String name, Body body) {
		try {
			body.run();
			passed++;
			System.out.println("PASS " + name);
		} catch (Throwable e) {
			failed++;
			System.out.println("FAIL " + name + "\n     " + e);
		}
	}

	static void that(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}

	static void eq(Object expected, Object actual, String message) {
		if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": expected " + expected + " but was " + actual);
	}

	static void throwsIae(Supplier<?> body, String message) {
		try {
			body.get();
		} catch (IllegalArgumentException expected) {
			return;
		}
		throw new AssertionError(message + ": expected IllegalArgumentException");
	}
}
