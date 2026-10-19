# RealSize

[Gallery](GALLERY.md) · [Configuration](CONFIGURATION.md) · [Changelog](CHANGELOG.md)

RealSize gives Minecraft mobs more believable proportions, including their
hitboxes. Bees are a quarter of their normal size, spiders are smaller, and iron
golems are larger. Smaller spiders can fit through gaps that used to keep them out.

Sizes apply automatically to new and existing mobs. Use the defaults or choose
sizes for your world in the config. Larger land mobs can step over taller terrain;
check pens and farms that depend on vanilla mob sizes.

## Installation

For **Minecraft 26.3**, with **Java 25**, **Fabric Loader 0.19.5+** and
**Fabric API 0.162.0+26.3 or later compatible with Minecraft 26.3**.

- **Single-player:** put RealSize and Fabric API in your Fabric installation's
  `mods` folder, then open your world.
- **Server:** put both JARs in the Fabric server's `mods` folder and start it.
  **Players can join without installing RealSize or Fabric.**
- **Optional client install:** install both JARs on your Fabric client for the
  visual improvements below. Multiplayer mob sizes come from the server.

## Client improvements

Installing RealSize on your client enables two visual fixes:

- Tiny mobs stay visible at the distance used for their normal-sized counterparts,
  within the server's tracking range.
- Walking animations match mob size: smaller mobs move their legs faster and larger
  mobs slower, without changing movement speed.

Both are enabled by default. See [client options](CONFIGURATION.md#client-options)
to turn either off.

## Configuration

Edit `config/realsize.json` on the server or in your single-player installation,
then run `/realsize reload` (operator permission level 2; cheats in single-player).
Loaded mobs update immediately; other mobs update when their chunks load.

Under `entityScales`, `"minecraft:bee": 0.4` makes bees larger than the default
`0.25`. Add `"minecraft:cow": 0.8` to shrink cows. A value of `1.0` leaves that
type at its normal size. Unlisted types stay unchanged unless selected by a tag.

The [configuration guide](CONFIGURATION.md) covers all options, default sizes,
modded mobs and tags.

## Existing worlds and upgrades

RealSize works in existing worlds. Leave room before enlarging mobs in confined
areas. Current size and step-height changes are temporary and disappear when you
remove the mod.

**Upgrading from an older version:** back up your world and config first. Older
RealSize versions saved permanent attribute changes; the current version removes
those as mobs load. Before uninstalling, load and save affected chunks so their
old changes are cleaned up. See [upgrade instructions](CONFIGURATION.md#upgrading-an-older-configuration).

## Bug reports

Open an issue with your Minecraft, Fabric Loader, Fabric API and RealSize versions,
the relevant config, logs and steps to reproduce.

For building and testing, see [Developing RealSize](DEVELOPMENT.md).
