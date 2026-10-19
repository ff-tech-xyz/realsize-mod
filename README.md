# RealSize

[Gallery](GALLERY.md) · [Configuration](CONFIGURATION.md) · [Release notes](CHANGELOG.md)

RealSize changes the size of Minecraft mobs to give familiar creatures more
believable proportions. Bees are tiny, spiders are smaller, and iron golems
are larger. These are actual size changes, including hitboxes, not just smaller
or larger models.

The defaults work without editing a file. Server owners and single-player
players can also choose their own sizes, from subtle adjustments to a different
balance for their world. RealSize changes existing mobs as well as newly
spawned ones; you do not need a new world.

## Playing with RealSize

There are no new items to craft or controls to learn. Play normally and the
configured sizes apply automatically. Each mob type has a fixed size rule;
RealSize does not randomly assign a different size to every individual.

With the default settings:

- Bees have a quarter of their normal width and height.
- Spiders use `0.26` of their normal size. Smaller hitboxes can fit through
  spaces that stopped their vanilla counterparts.
- Horses are slightly larger at `1.05`, and iron golems are larger at `1.2`.
- Types without a rule keep their normal size.

Baby mobs still grow up. For types with active resizing, a minimum size keeps
baby scaling from making them too small. Mob variants keep their own size
differences; a scale rule is not an exact height in blocks for every variant.

Larger land mobs get a proportional step-height adjustment to help them cross
small terrain changes. RealSize does not lower a small mob's normal step height
or give aquatic mobs this boost. Its added step height stays below the height
of a 1.5-block fence or wall; mobs that already have a higher step height retain
it. Size changes can still affect pens, farms and combat, so check builds that
depend on vanilla hitboxes rather than assuming they remain mob-proof.

Natural spawning uses the configured adult size in Minecraft's existing early
space checks. It is not a guarantee that every baby, variant or rider fits after
Minecraft finishes spawning it. Existing mobs are not moved or deleted when
their size changes. Leave enough room before enlarging mobs in confined areas.

## Installation

RealSize v2 is for **Minecraft 26.3**, using **Fabric** and **Java 25**.
It requires **Fabric Loader 0.19.5 or later** and **Fabric API 0.162.0+26.3 or
later compatible with Minecraft 26.3**. This build does not support Forge,
NeoForge, Minecraft 1.21.x, or earlier 26.x releases.

### Single-player

1. Set up a Fabric installation for Minecraft 26.3 with Java 25.
2. Put the RealSize JAR and the matching Fabric API JAR in its `mods` folder.
3. Launch that installation and open your world. RealSize creates
   `config/realsize.json` automatically.

The same installation handles both mob sizing and the optional visibility
correction described below.

### Multiplayer

The server owner installs RealSize and Fabric API in the Fabric server's `mods`
folder, then starts the server with Java 25. The server controls the sizes for
everyone. **Players do not need RealSize or Fabric to join a RealSize server.**
Other mods on that server may have their own requirements.

Players who want the optional tiny-mob visibility correction can install the
same RealSize JAR and Fabric API on their own matching Fabric client. Installing
RealSize only on a multiplayer client does not enable mob resizing on a server
that does not run it.

## Optional client improvements

With RealSize installed on your client, resized mobs also get a walking-animation
rhythm correction. Smaller mobs cycle their legs faster and larger mobs cycle
them slower, calculated from their actual size rather than preset mob values.
Changing a size rule also changes the rhythm; actual movement speed is unchanged.
Set `clientWalkTempoFix` to `false`
in your local config and run `/realsize-client reload` to disable this correction.
Vanilla clients keep the vanilla walking animation.

### Seeing tiny mobs at a distance

Minecraft can stop drawing a tiny mob sooner than a normal-sized one. The
optional client component uses the mob's unscaled dimensions for this distance
check, so shrinking a bee does not also shrink that rendering distance.
It is enabled by default when RealSize is installed on the client.

This does not extend the server's tracking distance or reveal mobs the server
has stopped sending to you. Normal visibility checks still apply. With a
server-only installation, mob sizing works, but players keep vanilla's rendering
behavior.

To disable the correction, set `clientRenderDistanceFix` to `false` in your
client's `config/realsize.json` and run `/realsize-client reload`. Set it back to
`true` and reload to enable it again. The command reloads both local client
options, not the server's sizes.

## Changing sizes

Edit `config/realsize.json` on the server, or in your single-player installation,
then run `/realsize reload` with operator permission level 2. For example,
changing the bee rule from `0.25` to `0.4` makes loaded bees larger immediately;
bees in unloaded chunks update when those chunks load.

The defaults are not a complete mob list or a whitelist. Add entries under
`entityScales` for other vanilla or modded mobs using their full IDs, such as
`"minecraft:cow": 0.8` or `"modid:mob_name": 0.8` (replace the latter with a real
installed mob's ID). **A mob with no matching entry or configured tag gets no
RealSize sizing or step-height adjustment.** Deleted defaults are not restored.
See [adding an unlisted mob](CONFIGURATION.md#add-a-mob-that-is-not-listed).

Use `1.0` to stop RealSize resizing a type, including one selected by a tag.
Invalid reloads keep the previous working settings and report the problem
instead of partially applying edits.

The [configuration guide](CONFIGURATION.md) covers every option, tags, default
sizes, size limits and upgrading an existing configuration.

## Existing worlds and removing the mod

You can use RealSize in an existing world. Its current size and step-height
adjustments are temporary: they are reapplied from the config when mobs load,
not permanently written into those mobs. Removing a rule or uninstalling the
mod removes those current adjustments without a whole-world cleanup pass.

**Upgrading from an older RealSize installation needs extra care.** Older
versions saved permanent attribute changes. RealSize removes its old saved
modifiers as affected mobs load, leaving other mods' modifiers alone. Back up
your world and configuration before upgrading.

Unloaded legacy mobs are not cleaned just because the mod is installed or later
removed. Keep RealSize installed until affected chunks have loaded and been
saved before uninstalling, or arrange a separately verified offline migration.
There is no whole-world cleanup command. See the [migration reference](CONFIGURATION.md#upgrading-an-older-configuration)
for config backups and upgrade behavior.

## Help and further reading

- **A size edit did nothing:** check that you edited the server's file, then run
  `/realsize reload`. A client-local file cannot override multiplayer sizes.
- **A mob is not as small as requested:** check `floor` and `hostileFloor`.
  Both default to `0.22`; `cap` defaults to `1.45`. These bounds apply to active
  non-neutral rules, including baby scaling. Other mods can also affect size.
- **A tiny mob disappears at a distance:** install RealSize on your client for
  the optional correction. Server tracking still limits how far away it exists
  on your client.
- **Reload reports an error:** fix the named field in the file. Do not delete
  your configuration just to hide an error; the previous active settings remain
  in use after a failed reload.

For bug reports, include Minecraft, Fabric Loader, Fabric API and RealSize
versions, the relevant config, logs, and steps to reproduce the problem.

Server owners: [Configuration and default sizes](CONFIGURATION.md).
Contributors: [Building and testing](DEVELOPMENT.md).
