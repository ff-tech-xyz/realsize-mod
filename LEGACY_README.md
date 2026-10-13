# RealSize

RealSize makes Minecraft mobs feel closer to real-world sizes. Bees can be tiny, horses can feel properly large, and server owners decide how far to push the effect.

It is a server-side Fabric mod for servers and modpacks that want more believable mob scale without making small mobs vanish or large mobs get stuck on every little bump. RealSize uses Minecraft's scale attributes, adds extra tracking help for tiny mobs, and gives oversized mobs a small step-height boost so they move through terrain more naturally.

## What it does

- Resizes mobs from a simple JSON config at `config/realsize.json`.
- Lets server owners set per-mob scale values with registry IDs like `minecraft:bee` or `minecraft:horse`.
- Keeps very small mobs visible from farther away by increasing their tracking range.
- Helps large scaled mobs step over terrain instead of snagging on small blocks.
- Supports both the current PyreHaven 26.x target and Minecraft 1.21.1 through 1.21.11 with separate jars.

## Good to know

- Server side: install it on the server. Players do not need the mod on their client.
- Loader: Fabric.
- Config file: RealSize writes `config/realsize.json` the first time it runs.
- Changes apply after a restart.

## Builds in this repo

This repo builds two Fabric jars from shared sizing logic:

| Module | Target |
|---|---|
| `mc121` | Minecraft `1.21.1` build intended for `1.21.1` through `1.21.11` |
| `mc2612` | Minecraft `26.1.2` / `1.26.1.2` build |
| `common` | Shared config, sizing logic, and tests |

Both platform jars include the shared `common` classes in the final mod jar.

## Configuration

RealSize writes `config/realsize.json` on first launch.

### How to use the config

1. Start the game or server once with the mod installed.
2. Open `config/realsize.json`.
3. Change the global limits or add/edit entries in `entityScales`.
4. Restart the game or server to apply the new values.

### What each field does

- `floor` — minimum allowed scale after config values are read. Anything lower gets clamped up to this value.
- `cap` — maximum allowed scale. Anything higher gets clamped down to this value.
- `trackingRangeThreshold` — if a mob ends up below this scale, RealSize boosts tracking so it stays visible from farther away.
- `minTrackingRangeChunks` — minimum chunk-based tracking range used for tiny mobs.
- `minTrackingDistanceBlocks` — hard minimum block distance used for tiny-mob tracking.
- `stepHeightBoostThreshold` — if a mob is at or above this scale, RealSize adds extra step height.
- `stepHeightBoostAmount` — how much extra step height large mobs get.
- `entityScales` — per-entity scale overrides keyed by registry ID such as `minecraft:bee`.

### Example

```json
{
  "floor": 0.22,
  "cap": 1.45,
  "trackingRangeThreshold": 0.6,
  "minTrackingRangeChunks": 10,
  "minTrackingDistanceBlocks": 128,
  "stepHeightBoostThreshold": 1.1,
  "stepHeightBoostAmount": 0.5,
  "entityScales": {
    "minecraft:bee": 0.25,
    "minecraft:horse": 1.05,
    "minecraft:nautilus": 0.4,
    "minecraft:zombie_nautilus": 0.4
  }
}
```

### Practical examples

- Make bees easier to see without changing anything else: lower only `minecraft:bee` in `entityScales`.
- Keep extremely small mobs from disappearing too early: raise `minTrackingDistanceBlocks` or `minTrackingRangeChunks`.
- Stop giant mobs from getting too large: lower `cap`.
- Help oversized mobs walk over terrain more naturally: lower `stepHeightBoostThreshold` or raise `stepHeightBoostAmount`.

## Building

From the repository root:

```bash
./gradlew :common:test :mc121:build :mc2612:build
```

Artifacts:

- `mc121/build/libs/realsize-mc121-<version>.jar`
- `mc2612/build/libs/realsize-mc2612-<version>.jar`

## Installation

1. Pick the jar for your Minecraft version:
   - `realsize-mc121-<version>.jar` for Minecraft `1.21.1` through `1.21.11`
   - `realsize-mc2612-<version>.jar` for Minecraft `26.1.2` / `1.26.1.2`
2. Drop that one jar into `mods/` with the matching Fabric Loader and Fabric API modules.
3. Start once to generate `config/realsize.json`.
4. Edit config if desired and restart.
