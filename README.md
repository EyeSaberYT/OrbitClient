# Orbit Client

A Fabric mod for Minecraft **1.21.11** with its own mod menu, **Orbit**: mods are glowing nodes on a ring
around an eclipse. Spin the ring and the mod at the pointer opens in the settings panel; the eclipse at the
center is its power button.

## Controls

| Key | Action |
| --- | --- |
| Right Shift | Open Orbit (the mod menu) |
| C (hold) | Zoom |
| Left Alt (hold) | Freelook |
| V (hold) | Snaplook |
| P | Stopwatch start / stop / reset |

Right Shift and C are in Options → Controls → Key Binds → Orbit Client; the rest are on each mod's settings.
Every mod also has its own **Toggle Key**, and any keybind can be a mouse button (side buttons too).

### Using Orbit

| Do this | To |
| --- | --- |
| Scroll, or ← → | Spin the ring (the mod at the pointer is selected) |
| Click a mod | Select it; click it again to turn it on/off |
| Click the eclipse, Enter or Space | Turn the selected mod on/off (its corona glows green when on) |
| Right-click a mod | Quick on/off without selecting |
| Middle-click a mod, or the ★ in the panel | Favorite it |
| Click an arc around the eclipse, or Tab | Switch category: HUD, Server, Mechanic, Favorites |
| Just type | Search every mod (Esc clears) |
| Dock on the left | Mods, Profiles, Waypoints, Settings, Edit HUD, Close |

In the HUD editor (dock → Edit HUD): drag mods, scroll to resize, right-click for its settings,
click **MODS** to go back to Orbit.

## Chat

**Shift + click** a chat message (with chat open) to copy its text; messages that wrap over several lines are
copied whole. Turn it off with **Shift-Click to Copy Chat** in SETTINGS.

## Mods (59)

| Filter | Mods |
| --- | --- |
| HUD | Armor Status, Clock, Combo Counter, Cooldowns, Coordinates, CPS, Day Counter, Direction HUD, FPS, Item Counter, Item Tracker, Key Strokes, Memory Usage, Pack Display, Playtime, Potion Counter, Potion Effects, Reach Display, Saturation, Speedometer, Stopwatch, WAILA |
| Server | Auto Text Hotkey, Boss Bar, Chat, Nick Hider, Ping, Scoreboard, Screenshot, Server Address, Sound Changer, Tab Editor, Titles, Waypoints |
| Mechanic | Block Outline, Chunk Borders, Crosshair, FOV Changer, Freelook, Fullbright, Glint Colorizer, Hit Color, Hitbox, Hurt Cam, Item Physics, Low Fire, Menu Blur, Motion Blur, Nametags, Particle Changer, Particle Multiplier, Shiny Pots, Snaplook, Team View, Time Changer, Toggle Sneak/Sprint, Wavey Capes, Weather Changer, Zoom |

## Animations

Everything moves smoothly; turn it off or change the speed in **SETTINGS** (Animations, Animation Speed).

- **Orbit:** mods burst out of the eclipse when it opens or the category changes and fall back in when it closes;
  the ring spins to the selected mod; the corona shimmers and glows on/off; toggles send out ripples; the panel
  slides and cross-fades; settings rows stagger in; switches, sliders, color pickers, mode values and carets animate;
  the starfield drifts and twinkles.
- **HUD editor:** fades in, MODS button pops, outlines and snap guides fade, resizing glides.
- **HUD mods:** fade and scale in and out, boxes resize smoothly, Combo/Reach pop when they change, potion effects
  and item tracker lines slide in and out (effects blink when ending), Key Strokes and the crosshair fade colors,
  waypoint markers fade.
- **Vanilla HUD:** the tab list unfolds and folds away, chat slides up on new messages, the hotbar selector glides
  between slots (each can be turned off).
- **World:** Fullbright, Time Changer and Weather Changer ease in and out; Zoom is smooth; Freelook glides back
  behind you when you let go.

## Profiles, settings and waypoints

- **Profiles** (left side of the menu): click to switch, **SAVE AS NEW PROFILE** copies your current setup,
  the pencil renames, the x deletes. Each profile has its own mods, positions and options.
- **SETTINGS** tab: menu blur, NEW badges, show HUD with F3, editor snapping, remember last filter.
- **WAYPOINTS** (dock): **+ ADD** marks where you stand. Click a waypoint to edit its name, color (full color
  picker with alpha and rainbow "chroma", plus quick color swatches), icon (pin, home, flag, diamond, star, skull,
  sword), beam, visibility and whether it shows at the screen edge when off-screen, move it to your position, or delete it. Waypoints are saved per server (or
  singleplayer world), per world inside that server, and per dimension. The world is recognized from the seed or the spawn
  point the server sends (so servers that randomize the seed, like many on Minehut, still work), so switching between a server's lobby, survival world and so on swaps waypoint sets automatically.
  The bar under the header shows the current world: arrows switch worlds by hand (useful on servers that hide the
  seed; the choice is remembered), click the name to rename it, **+** adds a world, the trash removes one. Set the Waypoints mod's **World Detection** to Manual to only switch by hand; when you die a red skull "Death" waypoint is added automatically.
- **In the world**, each waypoint is a marker with its icon and color, an optional beam, and a label with the
  distance. Waypoints off-screen or behind you show as small markers with arrows at the screen edge. The Waypoints
  mod's options set labels (always / near crosshair / never), distance, label background, marker size, smaller when
  far, beams, edge arrows and the max distance.
Everything is saved in `config/orbitclient/`. Orbit Client was called Eclipse Client before; on first start it copies
your settings from `config/eclipseclient/` and keeps your key binds.

## Building

You need **JDK 25** to run the build tools (the mod itself targets Java 21).

```powershell
.\gradlew.bat build
```

The jar is written to `build/libs/orbit-client-1.0.7.jar`. Put it in `.minecraft/mods` together with
[Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11, using Fabric Loader 0.18.1 or newer.

- `.\gradlew.bat runClient` starts a dev client.
- `.\gradlew.bat runClientGameTest` creates a test world, opens every menu page and saves screenshots to
  `build/run/clientGameTest/screenshots/`.

## Saturation and Wavey Capes

- **Saturation** works on the hunger bar itself: gold outlines show your hidden saturation, the food in your hand
  flashes the hunger and saturation it will restore, a faint bar shows exhaustion (singleplayer only; servers don't send
  it), and food tooltips list hunger and saturation.
- **Wavey Capes** simulates capes as cloth with Orbit Client's own physics (`src/main/java/com/orbitclient/cape`):
  a chain pinned between the shoulders that walking, jumping, strafing and turning push around. Options match the
  WaveyCapes mod's: Smooth/Blocky style, Waves wind, gravity, height and strafe multipliers. If the WaveyCapes mod
  itself is installed, Orbit Client's version turns itself off.

## Cosmetics

Open **Orbit → Wardrobe** (hanger button in the dock). It works like the Orbit menu: you turn inside the eclipse
core (drag to spin), the slots are arcs around it, and the slot's cosmetics orbit as nodes with their pictures.
Hover a node to try it on, click to wear it (again to take it off); the panel shows a wear button and colors.
All art is Orbit Client's own.

| Slot | Cosmetics |
|---|---|
| Capes (animated, with cloth physics) | Eclipse, Galaxy, Inferno, Aurora, Tide, Neon Circuit, Sakura, Midnight |
| Hats | Royal Crown, Halo, Cat Ears, Headphones, Top Hat |
| Face | Bandana, Shades |
| Back | Backpack, Blade Sheath |
| Wings (flap faster when you sprint or fly) | Dragon, Angel, Mech |
| Auras | Orbit, Rune Ring, Sparkles, Stardust Trail |
| Pets (follow beside your shoulder) | Eclipse Wisp, Cube Buddy |

Orbit Client players see each other's cosmetics through the cosmetics server in `server/` (see `server/README.md`).
The client finds it through a DNS SRV record on minearcade.org, so no address is built into the mod and it isn't a
setting. **Show Others' Cosmetics** turns other players' cosmetics off on your screen.

Cape art is made by `tools/CosmeticGenerator.java`; models are voxel art in `cosmetics/Cosmetics.java`.
## Crosshair

Open it from Orbit: **Crosshair → Open Editor**. Laid out like Lunar Client's crosshair mod:

- **Preview** at the real in-game size over sky, grass, stone or sand; hover it to aim around, hold **Tab** to
  preview over the actual game.
- **Simple**: thickness, size, gap and center dot of the vanilla-style cross.
- **Presets**: 14 designs.
- **Custom**: a **17×17** grid with the center pixel marked by a red dot. Mouse1 draws, Mouse2 erases (drag works),
  Delete clears, Ctrl+Z undoes. Click a preset below the grid to start from it.
- **Share / import**: the share button copies a code, import reads one from the clipboard. Codes use Lunar's
  `LCCH-…` format, so Lunar crosshair codes work here and yours work in Lunar.
- **Use Custom Scale**: Small / Normal / Large (the size of the vanilla crosshair at GUI scale 1 / 2 / 3) or Auto.
- **Color**: Vanilla Blending (inverts what's behind it, like vanilla), Static, or Dynamic (separate colors when
  aiming at enemies (hostile mobs, players) and friendly mobs). Optional outline.

## Zoom

Hold **C**. While zooming, **scroll up/down to zoom further in or out** (the hotbar doesn't change); it resets
when you let go unless you turn that off. Your hand and held item are hidden while zoomed.

## HUD text at small sizes

Minecraft's font is pixel art, so at sizes where a font pixel doesn't cover whole screen pixels, letters
lose rows or columns and look broken. **Pixel-Perfect HUD Scaling** (SETTINGS, on by default) snaps each HUD
mod to the nearest size where text stays sharp. At GUI scale 3 the sizes are 33%, 67%, 100%, 133%...;
at GUI scale 2 they're 50%, 100%, 150%... Scrolling in the HUD editor steps through exactly these sizes.
Turn the option off to allow any size (text may look uneven).

## Icons

Every mod has a custom white icon in `src/main/resources/assets/orbitclient/textures/gui/icons/SIZE/`
(named after the mod: "Toggle Sneak/Sprint" → `toggle_sneak_sprint.png`), pre-rendered at 13 pixel sizes so each is drawn
sharp at exactly the size it appears on screen. They're drawn by
`tools/IconGenerator.java`; edit a shape there and regenerate with:

```powershell
java tools/IconGenerator.java src/main/resources/assets/orbitclient/textures/gui/icons
```

(`tools/_icon_preview.png` shows them all.) You can also drop in your own PNG with the same name.
**SETTINGS → Mod Icons** switches between the custom icons and Minecraft item icons. A mod without an icon
file falls back to its item.

## Adding a mod

1. Make a class extending `TextHudModule` (simple text), `HudModule` (custom drawing) or `Module` (no HUD).
2. In the constructor call `setIcon(Items.SOMETHING)`, optionally `markNew()`, and add settings with
   `add(new BooleanSetting(...))`, `NumberSetting`, `ModeSetting`, `ColorSetting`, `KeybindSetting` or `TextSetting`.
3. Register it in `ModuleManager.init()`.

The menu, HUD editor, profiles and config saving pick it up automatically.
