# RealSize v2

RealSize resizes Minecraft mobs without baking those changes into the world save.
Bees are small, large mobs move proportionally, and server owners control the
sizes through `config/realsize.json`.

> This README describes the completed v2 target in present tense. It is the
> rebuild's product contract, not evidence that this checkout implements it.
> [FIXES.md](FIXES.md) records the remaining work and verification gates.
> “v2” names the rebuild; the current artifact version remains `1.2.0` until a
> separate versioning decision changes it.

## What it does

- Applies configurable, temporary scale modifiers to mobs. Reloading the config
  updates loaded mobs; chunk loads apply the current settings to other mobs.
- Removes RealSize's old saved modifiers when those mobs load. Removing a rule
  or setting it to `1.0` restores neutral RealSize scaling.
- Gives actively resized hostiles a higher minimum size and keeps actively
  resized babies from compounding below their category's floor. Neutral `1.0`
  and deleted rules preserve vanilla baby size without RealSize modifiers.
  Smaller hitboxes still change combat and movement;
  the hostile floor limits the effect, not guarantees vanilla mob-proofing.
- Scales land-mob step height proportionally without granting a new ability to
  cross 1.5-block fences or walls. Aquatic mobs get no RealSize step boost.
- Keeps tiny mobs from being culled prematurely when the optional client
  component is installed. It does not extend server tracking distance.

For example, changing the bee rule from `0.25` to `0.4` and running
`/realsize reload` changes existing loaded bees as well as future bees.

## Installation and compatibility

RealSize targets Minecraft **26.3**, Fabric, and Java **25** in one JAR. This
rebuild does not claim support for 1.21.x or older 26.x releases.

Install the JAR and its declared Fabric dependencies on the server. Vanilla
clients can join: scaling and movement do not require a client installation.
Players can optionally install the same JAR on a Fabric client for the tiny-mob
render-distance correction. No client classes load on a dedicated server.

The culling correction works when both the multiplayer server and the client
have RealSize, and in single-player with RealSize installed. A server-only
installation still provides sizing, movement, configuration reload, and spawn
clearance to vanilla clients; only the optional rendering correction is absent.
In single-player, the integrated server owns sizing and the client owns culling.

Without the client component, vanilla hitbox-based rendering limits still apply.
With it, shrinking entities use unscaled dimensions for distance decisions,
without shortening vanilla render distance for enlarged entities; normal
frustum culling and the server's vanilla tracking limits remain in force. A
client-only install does not resize mobs on a server without RealSize.

## Configuration

The file is authoritative. Defaults are written only when the file is missing;
entries deleted from an existing `entityScales` map are not silently restored.
A versioned migration writes an updated schema only after successful validation,
preserving a backup of the original. Unknown IDs/tags produce named warnings,
and malformed values fail with the field and file path rather than silently
substituting a value.

```json
{
  "configVersion": 2,
  "floor": 0.22,
  "hostileFloor": 0.6,
  "cap": 1.45,
  "proportionalStepHeight": true,
  "maxStepHeight": 1.49,
  "clientRenderDistanceFix": true,
  "entityScales": {
    "minecraft:bee": 0.25,
    "minecraft:horse": 1.05,
    "#minecraft:skeletons": 0.8
  }
}
```

This is a short custom configuration, not the complete generated default table.
The default table is reviewed against the 26.3 registry; hostile entries cannot
bypass `hostileFloor`.

- `floor` and `hostileFloor` set minimum final size relative to the entity type's
  unscaled adult size, including baby scaling, for active non-neutral rules.
  Neutral `1.0` and absent rules bypass these limits and remove owned modifiers.
  The hostile minimum is at least `floor`. `cap` sets the maximum.
- `proportionalStepHeight` enables land-mob proportional movement.
  `maxStepHeight` is below `1.5`; existing vanilla or foreign-mod step height is
  never lowered to satisfy this cap.
- `clientRenderDistanceFix` controls the optional client component in that
  client's local configuration. Server configuration does not toggle clients.
- `entityScales` accepts exact entity IDs and `#tag` selectors. Exact IDs win;
  conflicting tags are rejected rather than relying on map iteration order.
  Players and non-living entities are excluded.

Scales are positive and finite. Floors cannot exceed the cap. Missing required
v2 fields are validation errors; legacy files go through the explicit migration
instead of an implicit default merge.

The supported legacy input is the old unversioned JSON object. It must contain
`floor`, `cap`, and `entityScales`; the old tracking and flat-step fields are
optional, validated if present, and removed with a named migration diagnostic.
Migration retains the explicit map, sets `hostileFloor` to `max(floor, 0.6)`,
enables proportional movement and the local rendering option, and uses `1.49`
for `maxStepHeight`. Incompatible bounds fail rather than silently changing
`cap`. A v2-shaped file missing `configVersion` is an error, not legacy input.
Other version numbers are unsupported.

The exact original bytes are saved as `realsize.json.legacy.bak` before atomic
replacement. An existing backup blocks migration rather than being overwritten;
inspect and move it before retrying. If atomic replacement is unsupported, the
operation fails without a non-atomic fallback. Valid v2 files are read without
rewriting. Duplicate keys, unknown top-level fields, string-coerced numbers or
booleans, and symbolic-link config paths are rejected with named diagnostics.

`/realsize reload` requires permission level 2. A successful reload replaces
one validated settings snapshot and reconciles loaded mobs on the server thread.
A failed reload leaves the previous snapshot and mobs unchanged and reports
what to fix. The startup equivalent fails rather than running silently on a
bad file. Neither path rewrites an invalid file.

## Existing worlds

On entity load, RealSize strips legacy persistent modifiers in its own
`realsize` namespace and applies current transient modifiers. It leaves other
mods' attributes alone. Repeated loads and reloads never stack changes.

Uninstalling after migration does not leave new RealSize scale data in saves.
**Old, unloaded mobs are not magically repaired by uninstalling.** Keep RealSize
installed until affected chunks have loaded and been saved, or use a separately
verified offline migration before removal. There is no claim of a whole-world
cleanup command in v2.

Enlarged natural spawns are checked against their final dimensions before
placement is accepted. Existing mobs, explicit summons, and config changes are
not teleported or deleted to solve collisions; administrators must leave enough
space for the configured sizes.

Only narrow server spawn-placement mixins are permitted for that clearance
check. They run before placement reports success, preserve candidate retries,
and use the same sizing owner as load and reload. There are no server tracking
mixins, custom networking, or client-required registries. This bounded exception
replaces the rebuild's original zero-server-mixin requirement; implementation
and verification status remain in FIXES.md.

## Building and testing

One root Gradle project uses Mojang names, the non-remapping
`net.fabricmc.fabric-loom` plugin, and Java 25. Build pins live in
`gradle.properties`; there are no mapping JARs or legacy platform modules.

```bash
./gradlew clean build --console=plain
./gradlew runServer --console=plain
```

In the PyreHaven checkout layout, the completed JAR is
`../../runtime/realsize-mod/build/libs/realsize-<version>.jar`.
The dev server runs the same classes and resources packaged into that JAR.
Pure policy tests, Minecraft GameTests, dedicated-server startup, and connected
client checks cover the release contract. Exact acceptance gates and actual
completion status live in [FIXES.md](FIXES.md), not inferred from an empty build.

[ARCHITECTURE.md](ARCHITECTURE.md) defines ownership and implementation phases.
[LEGACY_README.md](LEGACY_README.md) is an archived description of the old mod;
its visibility, compatibility, and persistence claims are not v2 guarantees.
