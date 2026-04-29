# RealSize

**Scales every Minecraft mob to its accurate real-world size.**

A spider becomes the tiny creature it actually is. A horse stands at proper height. An elder guardian looms like the ocean titan it should be.

No client install required — drop it on your Fabric server and every player sees accurate sizes automatically.

---

## Compatibility

| | |
|---|---|
| **Minecraft** | 1.21.1 – 1.21.11 |
| **Loader** | Fabric |
| **Environment** | Server-side (works in singleplayer too) |
| **Fabric API** | Required |

---

## What It Does

Applies `EntityAttributes.GENERIC_SCALE` to each mob on load, scaling hitbox and model together based on real-world measurements.

| Mob | Real Height | Scale |
|-----|------------|-------|
| Spider | ~2.5cm body | 0.26× |
| Bee | ~1.5cm | 0.25× |
| Bat | ~6cm | 0.24× |
| Rabbit | ~25cm | 0.50× |
| Wolf | ~80cm shoulder | 0.88× |
| Horse | ~1.6m shoulder | 1.05× |
| Iron Golem | ~2.7m | 1.20× |
| Elder Guardian | enormous | 1.35× |
| Dolphin | ~2.5m long | 1.30× |

Over 60 mobs rescaled. Full rationale and real-world references in [`RealSizeMod.java`](src/main/java/xyz/pyrehaven/realsize/RealSizeMod.java).

---

## Features

- **Accurate proportions** — real-world shoulder/body heights used as reference
- **Server-side only** — no client mod needed, works in singleplayer too
- **Hitbox + model scale together** — uses the native `GENERIC_SCALE` attribute added in 1.20.5
- **Small mob visibility** — tracking range boosted for tiny mobs so they never cull at normal view distances
- **Large mob terrain fix** — step height boosted for mobs over the configured threshold so they can navigate terrain naturally
- **Fully configurable** — every scale and tuning constant lives in `config/realsize.json`
- **1.21.11 mobs supported** — Nautilus and Zombie Nautilus included by default via registry ID config entries

---

## Installation

1. Download the latest `.jar` from [Releases](https://github.com/phred2026-cyber/realsize-mod/releases)
2. Drop it in your server's `mods/` folder (alongside Fabric API)
3. Start the game/server once to generate `config/realsize.json`
4. Edit the config if desired, then restart to apply changes

---

## Configuration

RealSize now writes a JSON config file at `config/realsize.json` the first time it starts. If the file is missing, a new one is generated with the current default behavior.

Server owners can edit:

- `floor`
- `cap`
- `trackingRangeThreshold`
- `minTrackingRangeChunks`
- `minTrackingDistanceBlocks`
- `stepHeightBoostThreshold`
- `stepHeightBoostAmount`
- `entityScales` keyed by registry ID, for example `minecraft:bee`

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

Unspecified values fall back to the built-in defaults, so you can override only the parts you care about.

---

## Links

- 🔥 [PyreHaven Discord](https://discord.gg/tZ6Hx2ETA3) — support, feedback, hang out
- 📦 [Modrinth](https://modrinth.com/mod/realsize) — download page
- 🌐 [PyreHaven](https://pyrehaven.xyz) — the Minecraft server this was built for

---

*Built by [PyreHaven](https://pyrehaven.xyz) — chaotic worlds, safe community.*
