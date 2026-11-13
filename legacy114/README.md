# Orbit Client for Minecraft 1.14 to 1.14.4

Orbit cosmetics and the wardrobe for the five 1.14 releases. These versions draw the old way (like 1.13)
but run on modern Fabric, whose mappings name the game's classes differently from Legacy Fabric's. So this
is the legacy client (`../legacy`, see its README) once more:

- everything that doesn't name the game's classes (sync, ownership, the wardrobe's logic, the audit) is
  `../legacy/src/main`, used as it is;
- the cosmetic art and ownership rules are the main client's files, copied in at build time;
- this folder's `src/main` is only the game-facing part (drawing, the player layer, the hooks, `Game`,
  `Platform`, `Compat` and the screen adapter), written in modern Fabric's 1.14 names.

It needs Fabric loader 0.16 or newer and Java 17 or newer. Fabric API is not needed.

## Building

With JDK 21 and Gradle 9 (there is no wrapper here):

```
gradle build              # 1.14.4
gradle build -Pmc=1.14.2  # or 1.14, 1.14.1, 1.14.3
```

The jar is `build/<game version>/libs/orbit-client-<version>+<game version>.jar`. All five are named with
1.14.4's mappings: the earlier releases' own mappings use older names for the same classes.

`gradle runClient -PstartAudit` and `-PshotAudit` work as in `../legacy`.
