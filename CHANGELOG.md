# Changelog

## 1.2.0 — RealSize v2 rebuild

RealSize gives familiar mobs different proportions, with real hitbox changes.
The rebuild keeps the original default size choices while improving configuration,
world upgrades and the optional client experience.

- Change sizes in existing worlds with `/realsize reload`. Loaded mobs update
  immediately; mobs in unloaded chunks update when those chunks load. Invalid
  reloads keep the previous working settings.
- Add rules for registered vanilla or modded mobs, or select them with entity-type
  tags. Exact mob rules take priority. Unlisted mobs stay unchanged, and deleted
  default rules are not silently added back.
- Current size and step-height adjustments are temporary rather than saved into
  mobs. Older RealSize modifiers are cleaned as affected mobs load, without
  removing other mods' modifiers. Back up your world and config before upgrading;
  untouched legacy chunks still need migration before uninstalling.
- Active resizing respects baby growth and configurable size limits. Enlarged
  land mobs get proportional step height below a 1.5-block fence or wall, without
  lowering higher existing step heights. Aquatic mobs do not get that boost.
- Natural spawning uses the configured adult dimensions in Minecraft's existing
  early space checks. It does not guarantee clearance for every finalized baby,
  variant or rider, and does not relocate existing mobs.
- With RealSize installed on the client, tiny mobs use their unscaled dimensions
  for rendering-distance checks. Walking animations also cycle faster for smaller
  mobs and slower for larger mobs without changing movement speed. Both options
  can be disabled independently and reloaded with `/realsize-client reload`.
- Shrunk mobs using Minecraft's standard movement controller turn more gradually.
  Specialized movement controllers are not covered.

### Requirements and upgrading

This build targets **Minecraft 26.3, Fabric and Java 25**. It requires Fabric
Loader **0.19.5+** and Fabric API **0.162.0+26.3**, or a later compatible API for
Minecraft 26.3. It does not support earlier Minecraft releases, Forge or NeoForge.
The artifact version is **1.2.0**; “v2” names the rebuild, not version 2.0.0.

Install RealSize and Fabric API on the server for multiplayer sizing; players can
join without either. The client installation is optional for the rendering and
walking-animation improvements. In single-player, install both in the Fabric
client's `mods` folder.

Read the [player guide](README.md), [configuration and migration reference](CONFIGURATION.md),
and [screenshot gallery](GALLERY.md) before changing an existing world.
