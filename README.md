# Totem Swap Helper

A client-side Fabric mod for **Minecraft 26.1.2** that keeps a Totem of Undying
in your offhand automatically. Built for **your own singleplayer worlds /
private servers** — not intended for use on public multiplayer servers you
don't control (most treat auto-managing your offhand as a bannable exploit).

## What it does

If your offhand slot doesn't already hold a Totem of Undying, and you have
one somewhere in your inventory, the mod swaps it into your offhand for you —
the same three clicks you'd do by hand (pick up totem → place in offhand →
put old offhand item back), just automated.

## Modes

- **Inventory mode (default):** only runs while your survival inventory
  screen (opened with `E`) is on screen.
- **Always mode:** runs every tick, no need to open your inventory.

Switch modes with chat commands:

```
/totemhelper inventory   -> only while inventory (E) is open
/totemhelper always      -> always active
/totemhelper on          -> enable the mod
/totemhelper off         -> disable the mod
```

Or just press **Right Shift** any time to toggle the whole mod on/off
(rebindable in Options > Controls > Key Binds > Totem Swap Helper).

## Requirements

- Minecraft **26.1.2**
- Fabric Loader **0.18.4+**
- Fabric API (matching build, `0.154.0+26.1.2` or newer for 26.1.x)
- Java **25**

## Building it yourself

```
git clone <this-repo>
cd totem-swap-helper
./gradlew build
```

The finished jar shows up in `build/libs/`. Drop it (and Fabric API) into
your `.minecraft/mods` folder.

This repo also ships a GitHub Actions workflow (`.github/workflows/build.yml`)
that builds the mod on every push and uploads the jar as a downloadable
artifact — that's the easiest way to get a guaranteed-fresh build without
setting up a Java 25 dev environment locally.

## A note on how this was built

Minecraft 26.1 was a large modding-toolchain change: the game is unobfuscated
and Fabric now develops directly against Mojang's official class/method names
instead of the old Yarn mapping names. The code here uses those official
names (`Minecraft`, `LocalPlayer`, `AbstractContainerMenu`, `ClickType`, ...).
If you hit a compile error referencing one specific method/field name, it's
almost certainly just a small naming difference between 26.1.x patch
releases — run `./gradlew genSources` and let your IDE suggest the closest
match; the surrounding logic doesn't need to change.
