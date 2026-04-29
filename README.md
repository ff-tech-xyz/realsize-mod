# RealSize

**Scales Minecraft mobs to configurable real-world proportions.**

RealSize is a server-side Fabric mod that applies the native scale attribute to mobs, keeps small mobs visible by raising tracking distance when needed, and boosts step height for large scaled mobs.

## Builds in this repo

This branch now builds **two Fabric jars from shared configurable logic**:

| Module | Target |
|---|---|
| `mc121` | Minecraft `1.21.1` build intended for `1.21.1` through `1.21.11` |
| `mc2612` | Minecraft `26.1.2` / `1.26.1.2` build |
| `common` | Shared config + sizing logic + unit tests |

Both platform jars include the shared `common` classes in the final mod jar.

## Features

- JSON config at `config/realsize.json`
- Shared data-driven entity scale table keyed by registry ID
- Small-mob tracking range boost driven by config thresholds
- Large-mob step-height boost driven by config thresholds
- 1.21.11-only mob compatibility on the 1.21 build via registry-ID lookup (`minecraft:nautilus`, `minecraft:zombie_nautilus`) instead of compile-time entity constants

## Configuration

RealSize writes `config/realsize.json` on first launch.

Config fields:

- `floor`
- `cap`
- `trackingRangeThreshold`
- `minTrackingRangeChunks`
- `minTrackingDistanceBlocks`
- `stepHeightBoostThreshold`
- `stepHeightBoostAmount`
- `entityScales` keyed by registry ID such as `minecraft:bee`

Example:

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

## Building

From the repository root:

```bash
./gradlew :common:test :mc121:build :mc2612:build
```

Artifacts:

- `mc121/build/libs/realsize-mc121-<version>.jar`
- `mc2612/build/libs/realsize-mc2612-<version>.jar`

## Installation

1. Pick the jar for your Minecraft version.
2. Drop it into `mods/` with the matching Fabric Loader and Fabric API.
3. Start once to generate `config/realsize.json`.
4. Edit config if desired and restart.
