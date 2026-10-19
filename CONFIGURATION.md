# Configuring RealSize

[README](README.md) · [Default sizes](#default-sizes)

For server owners and single-player users. Size settings go in that
installation's `config/realsize.json`; client options use each player's local file.

## Change a mob's size

1. Start the server or open a world once to generate `config/realsize.json`.
2. Find the mob under `entityScales`, change its value and save.
3. Run `/realsize reload` with operator permission level 2, or `realsize reload`
   in the server console. In single-player, enable cheats. Restarting also works.

Loaded mobs update immediately; other mobs update when their chunks load.
A failed reload reports the problem and keeps the previous settings.

Values scale width and height: `0.5` is half size, `1.2` is 20% larger.
Baby and variant sizes still apply, subject to the size limits below.
Set an exact rule to `1.0` to leave that type at its normal size.
Deleting a rule works too, unless a tag still selects that type.
Deleted defaults stay deleted; an empty `entityScales` map disables sizing.

## Add a mob that is not listed

Add its full entity ID under `entityScales`, then reload. For example,
`"minecraft:cow": 0.8` makes cows smaller. Mobs added by mods use the same syntax,
with their mod's namespace and entity name.

The generated list contains the default choices, not every supported mob.
Types without an exact rule or matching tag keep their normal size and step height.
Players and non-living entities are excluded; selected living entities need a
scale attribute. Unknown IDs produce a warning in the log.

## Configuration file and options

This example uses a few custom rules. For the generated rules, see
[default sizes](#default-sizes).

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

- `configVersion`: `2`, the current file format.
- `floor`: minimum size relative to the type's normal adult dimensions, including
  baby scaling. Default: `0.22`.
- `hostileFloor`: the minimum for monster-category mobs. Must be at least `floor`.
  Default: `0.22`.
- `cap`: maximum size relative to normal adult dimensions. Must be at least both
  floors. Default: `1.45`.
- `proportionalStepHeight`: lets larger land mobs step over taller terrain.
  Default: `true`.
- `maxStepHeight`: ceiling for that step-height increase, in blocks. Must be
  below `1.5`; an already higher step height is preserved. Default: `1.49`.
- `entityScales`: size rules keyed by entity IDs or `#tag` selectors.

The floors and cap apply to active resizing rules; absent rules and `1.0` bypass
them. For example, the default cave-spider rule of `0.20` reaches the `0.22` floor.

Use JSON without comments, trailing commas or duplicate keys. Numbers must be
positive and finite. All fields are required except `clientWalkTempoFix`, which
falls back to `true` in older v2 files. Unknown fields and invalid values are
reported by name. An invalid config prevents startup; fix the reported field.

To restore all defaults, stop the server, move the config aside and restart.

## Client options

Edit these in your client's `config/realsize.json`, then run
`/realsize-client reload`:

- `clientRenderDistanceFix`: uses normal-sized dimensions for tiny mobs' render
  distance, within the server's tracking range. Default: `true`.
- `clientWalkTempoFix`: adjusts walking-animation rhythm to the mob's scale.
  Smaller mobs cycle faster and larger mobs slower. Default: `true`.

Set either to `false` to disable it. These are local options; single-player size
changes still use `/realsize reload`.

## Selecting mobs with tags

Use `"#minecraft:skeletons": 0.8` to resize members of that entity-type tag.
Datapacks can define tags or change their membership.

Exact IDs take priority over tags. To exclude bees from a tag rule, add
`"minecraft:bee": 1.0`. Overlapping tags may use the same value; conflicting
values need an exact rule to resolve them or the reload fails.

Minecraft's datapack reload refreshes tag selections using the active RealSize
settings. If the new tags conflict, RealSize logs the problem and retains its
previous selections. Use `/realsize reload` to read config-file edits.

## Upgrading an older configuration

Back up your world and config before upgrading.

RealSize migrates the old unversioned config automatically. It preserves your
size rules, sets `hostileFloor` to the old `floor`, enables proportional movement
and both client fixes, and sets `maxStepHeight` to `1.49`. Obsolete tracking and
flat-step settings are removed.

The original file is saved as `realsize.json.legacy.bak`. If that backup already
exists, inspect it and move it before retrying. Invalid files are left unchanged;
check the log for the field that needs fixing. Current v2 files are read as-is.

Older versions also saved permanent mob attributes. Keep RealSize installed until
affected chunks have loaded and been saved before uninstalling; cleanup happens
as mobs load. There is no whole-world cleanup command.

## Default sizes

Values are relative to each type's normal size. Unlisted types have no rule.

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
