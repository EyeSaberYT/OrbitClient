# Changelog

What changed in each Orbit Client release. The release build copies the section for its version
onto the GitHub release page (and the Discord announcement), so write it for players: what they
will notice, then what was fixed, with the Minecraft versions it affects. A release without a
section here does not build.

## v1.1.10

**Borderless Fullscreen, like Lunar's.**

- New mod in the Right Shift menu (Mechanic): **Borderless Fullscreen**. With it on, fullscreen (F11 or
  Video Settings) becomes a borderless window covering your screen instead of exclusive fullscreen, so
  Alt+Tab and a second monitor switch instantly, with no black flash and no getting stuck in the game.
- F11 still switches back to a normal window, and turning the mod off goes back to normal fullscreen.
- On Minecraft 1.15 to 26.3 (on 26.3 it switches on the game's own borderless mode). macOS keeps its own
  fullscreen, which already works this way. Not on 1.8.9 to 1.13.2 yet.

## v1.1.9

**Fixes a crash in Edit HUD on Minecraft 26.2 and newer.**

- Opening Edit HUD from the main menu (before joining a world) no longer crashes the game. The Armor
  Status, Item Counter and Waila example items now appear once you have joined a world.
- Mod icons in the Orbit menu can no longer crash the game on the main menu for the same reason.
- Affects Minecraft 26.2 and 26.3; other versions only get the safer code.

## v1.1.8

**The Christmas advent calendar: 24 limited-edition cosmetics, one for every day from December 1st to 24th.**

- Open each day's door in the advent calendar on Orbit Launcher's Play page to claim that day's cosmetic.
  Each one can only be claimed on its own day, so they really are limited editions.
- The 24: Santa Hat, Candy Cane, Snowflake Halo, Gingerbread Buddy, Reindeer Nose, Holly Crown, Gift
  Backpack, Wreath, Tree Topper, Mini Snowman, Snow Goggles, Stocking, Bauble, Reindeer Antlers,
  Peppermint Orbit, Ice Crown, Frost Wings, Festive Cape, North Star, Knitted Scarf, Cookie Crumbs, Elf
  Hat, Snow Globe and the Christmas Star Aura on Christmas Eve.
- Claimed ones appear in your wardrobe (press Refresh). Ones you don't have yet tell you which December
  day they belong to.
- On every Minecraft version Orbit Client supports, 1.8.9 to 26.3.

## v1.1.7

**Security fix for cosmetics and friends sign-in, on every Minecraft version. Please update.**

- **Fixed: someone on your network could log into Minecraft servers as you.** Orbit Client signed in
  to the cosmetics server over plain HTTP and signed whatever "challenge" it received with Mojang.
  Someone on the same Wi-Fi, or anyone who could fake the server, could hand over a real server's
  login value and join that server (for example Hypixel) under your name. Orbit Client now mixes a
  random number of its own into what it signs, so the value can't match any other server.
- **Cosmetics and friends now use HTTPS** (`orbit-social.minearcade.org`), so friend lists, messages,
  loadouts and the sign-in can't be read or changed on the way.
- A reply from the server larger than 1 MB is refused instead of filling the game's memory, so a
  fake or broken server can no longer crash the game this way.

## v1.1.6

- **Fixed: typing on Minecraft 26.3.** Nothing could be typed in Orbit's menu (Right Shift) or in
  the wardrobe's search box. 26.3 only sends typed text to the game while a screen asks for it,
  and Orbit's screens now ask.

## v1.1.5

- **Fixed: Motion Blur on 26.3.** Turning it on threw you out of the world, because its shader
  didn't compile on that version.
- **Fixed: a crash on 26.2 and 26.3** when copying a chat message with shift-click. The same fault
  was in the Chat mod's stacking of repeated messages, which is fixed too.

## v1.1.4

- **Fixed: clicking on 26.3.** Nothing in Orbit's screens reacted to a left click, because 26.3
  numbers the mouse buttons differently.
- **Many more Minecraft versions:** everything from 1.15 to 1.19.4 joins 1.20 to 26.3, so every
  release from 1.15 to 26.3 has the full client.
- The cosmetics-only builds for 1.8 to 1.14 are part of every release, so Orbit Launcher can
  install them too.
- **Cosmetics draw about three times faster:** flat areas of a model are drawn as one face instead
  of one per voxel.
- Fixed: options failed to save while 26.3 started (the Zoom module asked for the player too early).
