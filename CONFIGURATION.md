# Configuring RealSize

[Back to the player guide](README.md)

Size rules belong to the server, or to the integrated server in single-player.
Players joining a server cannot override its mob sizes with a local file.
The optional visibility setting belongs to each player's client.

## Change a mob's size

1. Start the server or open a single-player world once to create
   `config/realsize.json` in that installation's game directory.
2. Back up the file, then find the mob under `entityScales`.
3. Change its value and save. For example, change `minecraft:bee` from `0.25`
   to `0.4` for larger bees that are still smaller than vanilla bees.
4. Run `/realsize reload` as an operator with permission level 2, or run
   `realsize reload` in the server console. In single-player, enable cheats
   to use the command. Restarting also loads the file.

Loaded mobs update when the command succeeds. Mobs in unloaded chunks use the
new settings when those chunks load; reloading does not load the whole world.
The success message is `RealSize reloaded; loaded mobs reconciled.`

A scale measures each linear dimension, not volume: `0.5` means half the normal
width and height. `1.0` disables RealSize sizing for that rule; `1.2` requests
20% larger dimensions. Floors and the cap limit active non-neutral rules.
Minecraft's own age and variant sizes still matter.

To stop resizing a type, set an exact rule to `1.0`. Deleting its rule also
works if no remaining tag rule selects that type. An empty `entityScales` map
disables all configured sizing. Deleted default rules are not added back.

## Configuration file and options

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
  "hostileFloor": 0.22,
  "cap": 1.45,
  "proportionalStepHeight": true,
  "maxStepHeight": 1.49,
  "clientRenderDistanceFix": true,
  "clientWalkTempoFix": true,
  "entityScales": {
    "minecraft:bee": 0.25,
    "minecraft:horse": 1.05,
    "#minecraft:skeletons": 0.8
  }
}
```

This is a short custom configuration, not the complete generated default table.
The generated defaults cover the mob types listed below.
Both floors default to `0.22`; a higher `hostileFloor` is opt-in. Hostile entries
cannot bypass that configured bound. Existing v2 files are not rewritten. To reset to defaults, stop the server,
move the old file somewhere safe, and start the server again. This resets all
RealSize settings, not just one rule.

- `floor` and `hostileFloor` set minimum final size relative to the entity type's
  unscaled adult size, including baby scaling, for active non-neutral rules.
  Neutral `1.0` and absent rules bypass these limits and remove owned modifiers.
  The hostile minimum is at least `floor`. `cap` sets the maximum.
- `proportionalStepHeight` enables land-mob proportional movement.
  `maxStepHeight` is below `1.5`; existing vanilla or foreign-mod step height is
  never lowered to satisfy this cap.
- `clientRenderDistanceFix` controls the optional client component in that
  client's local configuration. Server configuration does not toggle clients.
  Edit the local file and run `/realsize-client reload` to apply that option
  without restarting. Invalid reloads retain the previous option. This command
  does not apply server sizing rules; integrated-server sizing still uses
  `/realsize reload` separately.
- `clientWalkTempoFix` controls the optional client correction for the walking
  animation rhythm of resized mobs. Tempo is `min(vanilla tempo / actual attribute
  scale, 6)`: smaller mobs cycle faster and larger mobs slower. The attribute
  scale already reflects the server's current rule and other scale modifiers;
  vanilla baby tempo is preserved without applying age twice. It defaults to
  `true`, including when omitted
  from older v2 files. Edit the local file and run `/realsize-client reload` to
  apply it. It does not change movement speed or the server sizing rules.
- `entityScales` accepts exact entity IDs and `#tag` selectors. Exact IDs win;
  conflicting tags are rejected rather than relying on map iteration order.
  Players and non-living entities are excluded.

Use ordinary JSON: no comments, trailing commas, or duplicate keys. Numbers
and booleans must not be quoted. Scales are positive and finite. Floors cannot
exceed the cap. Apart from the optional `clientWalkTempoFix`, missing required
v2 fields are validation errors; legacy files go through the explicit migration
instead of an implicit default merge.

## Upgrading an older configuration

Back up the world and configuration before upgrading.

The supported legacy input is the old unversioned JSON object. It must contain
`floor`, `cap`, and `entityScales`; the old tracking and flat-step fields are
optional, validated if present, and removed with a named migration diagnostic.
Migration retains the explicit map, sets `hostileFloor` to the legacy `floor`,
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

## Selecting mobs with tags

An exact rule such as `"minecraft:bee": 0.4` takes priority over tags.
A rule such as `"#minecraft:skeletons": 0.8` applies to eligible members of
that entity-type tag, including datapack-defined membership.

Overlapping tags with the same value are allowed. Different values for the same
eligible type are rejected unless an exact rule resolves that overlap. Unknown
well-formed IDs or tags produce warnings naming the selector; they are not
replaced with another mob. Players and non-living entities cannot be selected.
Other living entities, such as armor stands, can be selected if they have the
relevant attributes. Modded types can be named by their own namespace, but this
is not a guarantee of compatibility with every mod's sizing or spawning logic.

A successful Minecraft datapack reload refreshes tag selections using the
already active RealSize settings. It does not read unsaved or unapplied config
edits; use `/realsize reload` for changes to this file. If new tags conflict,
RealSize keeps its previous sizing snapshot and logs the conflict. It does not
roll back Minecraft's datapack changes.

## Default sizes

These are the rules generated on first run, not random ranges. Each type uses
its own rule. Values are relative to that type's normal size, not to a common
height shared by all mobs. Unlisted types have no RealSize rule.

- `minecraft:spider`: `0.26`.
- `minecraft:cave_spider`: `0.20` (effective adult size `0.22` with the default floor).
- `minecraft:silverfish`: `0.22`.
- `minecraft:endermite`: `0.22`.
- `minecraft:bee`: `0.25`.
- `minecraft:bat`: `0.24`.
- `minecraft:frog`: `0.28`.
- `minecraft:tadpole`: `0.22`.
- `minecraft:axolotl`: `0.55`.
- `minecraft:tropical_fish`: `0.22`.
- `minecraft:pufferfish`: `0.43`.
- `minecraft:cod`: `0.5`.
- `minecraft:salmon`: `0.7`.
- `minecraft:squid`: `0.4`.
- `minecraft:glow_squid`: `0.4`.
- `minecraft:nautilus`: `0.4`.
- `minecraft:zombie_nautilus`: `0.4`.
- `minecraft:turtle`: `0.55`.
- `minecraft:chicken`: `0.64`.
- `minecraft:parrot`: `0.45`.
- `minecraft:rabbit`: `0.5`.
- `minecraft:cat`: `0.45`.
- `minecraft:fox`: `0.5`.
- `minecraft:armadillo`: `0.38`.
- `minecraft:allay`: `0.42`.
- `minecraft:vex`: `0.3`.
- `minecraft:ocelot`: `0.64`.
- `minecraft:goat`: `0.65`.
- `minecraft:wolf`: `0.88`.
- `minecraft:pig`: `0.95`.
- `minecraft:sheep`: `0.75`.
- `minecraft:panda`: `0.92`.
- `minecraft:polar_bear`: `1.05`.
- `minecraft:hoglin`: `0.85`.
- `minecraft:zoglin`: `0.85`.
- `minecraft:donkey`: `0.88`.
- `minecraft:mule`: `0.93`.
- `minecraft:horse`: `1.05`.
- `minecraft:skeleton_horse`: `1.05`.
- `minecraft:zombie_horse`: `1.05`.
- `minecraft:llama`: `0.92`.
- `minecraft:trader_llama`: `0.92`.
- `minecraft:camel`: `1.1`.
- `minecraft:sniffer`: `0.94`.
- `minecraft:dolphin`: `1.3`.
- `minecraft:guardian`: `0.7`.
- `minecraft:elder_guardian`: `1.35`.
- `minecraft:piglin_brute`: `1.05`.
- `minecraft:iron_golem`: `1.2`.
- `minecraft:ravager`: `1.15`.
