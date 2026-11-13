# Orbit Client for Minecraft 1.8.9 to 1.13.2 (legacy)

Orbit cosmetics and the wardrobe on the old engine, as a Legacy Fabric mod. It is a separate build from
the main client (the folder above): these versions have none of the classes the main client is written
against, so its menus, HUD and modules can't be shared. What is shared is the part that matters for
cosmetics:

- **The cosmetic art.** Every hat, face item, back item, wing, aura, pet and cape is the main client's
  own file, copied in at build time (`syncShared` in `build.gradle`). Nothing here is a second copy.
- **The ownership rules.** `ClaimsResponse` and `AccountOutfits` are the main client's files too: only
  what the cosmetics server says the signed-in account owns can be worn, each account keeps its own
  outfit, and a failed request never empties what is shown.
- **The server protocol.** `CosmeticSync` here speaks the same protocol as the main client's.

What this client has: the cosmetics on players (yours and other Orbit players'), the wardrobe
(**Right Shift**, in a world or on the title screen) with Refresh (also **F5**), and syncing your outfit.
It has none of the main client's HUD, modules, home screen or friends.

## Requirements

- Minecraft 1.8.2 to 1.8.9, 1.9.4, 1.10.2, 1.11.2, 1.12.2 or 1.13.2 with **Legacy Fabric** loader 0.16 or newer (no Legacy Fabric API needed).
- **Java 17 or newer.** The shared cosmetics code needs it; These Minecraft versions run fine on it.

## Building

From this folder, with JDK 21 (the build tools need it; the mod targets Java 17):

```
gradle build              # 1.8.9
gradle build -Pmc=1.12.2  # or 1.8.2 to 1.8.8, 1.9.4, 1.10.2, 1.11.2, 1.13.2
```

The jar is `build/<game version>/libs/orbit-client-<version>+<game version>.jar`. There is no Gradle wrapper
in this folder: use Gradle 9.

Everything is one source tree (`src/main`) except three thin layers:

- `src/names-legacy`: the code that has to use the game's own class names (drawing, the player layer, the
  hooks, and `Game`, which the shared code calls for everything else). `src/main` itself names almost
  nothing of the game, so the 1.14 build (`../legacy114`) reuses it with its own names folder.
- `src/v<game version>/java/.../Compat.java`: the few calls that differ from one version to the next.
- `src/gen-lwjgl2` (1.8.9 to 1.12.2) and `src/gen-lwjgl3` (1.13.2): what differs between engine generations,
  which is input, run-time textures, how the mod's assets reach the game, and the screen class. The
  wardrobe itself (`WardrobeView`) is shared; only a small `WardrobeScreen` adapter is per generation.

## Checking a build

Add `-Pmc=<game version>` to either for a version other than 1.8.9.

- `gradle runClient -PstartAudit` starts the game, waits for the title screen and quits. It prints
  `ORBIT-LEGACY-AUDIT OK` when the game got there.
- `gradle runClient -PshotAudit` also opens a flat test world, dresses the player in cosmetics and opens
  the wardrobe, saving screenshots to `run/<game version>/screenshots/orbit-*.png`. It pretends to own every cosmetic
  for those pictures and never talks to the cosmetics server.

## Notes

- **1.8 and 1.8.1 are not built.** They still have the older drawing code (like 1.7) and need another layer.
- **Finding the cosmetics server.** Minecraft 1.8 to 1.13 bundles a library that blocks Java's DNS lookup on
  Java 17 and newer. When that happens the client asks a public DNS-over-HTTPS resolver for the same record
  (only the record's name is sent).
