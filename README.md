# RealSize

[Gallery](GALLERY.md) · [Configuration](CONFIGURATION.md) · [Changelog](CHANGELOG.md)

RealSize gives Minecraft mobs more believable proportions: tiny bees, smaller spiders, and taller iron golems. Their hitboxes change with their models, so this affects how they fit into the world too (a smaller spider can get through a gap it couldn't before).

Our default sizes apply automatically to existing mobs and new spawns, or you can choose your own sizes for vanilla and modded mobs.

*Tiny bees buzzing through a forest clearing.*

![Tiny bees in a forest clearing](https://cdn.modrinth.com/data/rd4cVVMM/images/929de710364aafa40774668540257ca11a9dd617.jpg)

*Smaller spiders. Easier to miss, harder to keep out.*

![Small spiders across a sandy clearing](https://cdn.modrinth.com/data/rd4cVVMM/images/14ecdf4a3b74096c32033c20ee0c13470f0f82f1.jpg)

*Even aquatic mobs aren't safe from their real size.*

![Nautiluses and fish above the seabed](https://cdn.modrinth.com/data/rd4cVVMM/images/53eabd3926696daee80f53ebc3e6b6acd62ff3ce.jpg)

*Tiny frogs settling in along the shoreline.*

![Small frogs along a grassy shoreline](https://cdn.modrinth.com/data/rd4cVVMM/images/430c02947de425e9cfe9efd61462244fc494e89d.jpg)

*A few land mobs enjoying their new proportions.*

![Goats, cats and chickens on a sandy hillside](https://cdn.modrinth.com/data/rd4cVVMM/images/32ec699aa7e9c5aca523f0266920507ae018c2cd.jpg)

## Don't like our sizes? Choose your own.

The defaults are only a starting point. These next two screenshots use custom settings.

*For anyone who thought spiders weren't big enough already.*

![A giant spider towering above a snowy forest — custom configuration](https://cdn.modrinth.com/data/rd4cVVMM/images/13f2db3b1dc9b91051eb30b573c6a3fbbb63ee51.jpg)

*Or take things in the other direction with miniature polar bears.*

![Miniature polar bears on snowy terrain — custom configuration](https://cdn.modrinth.com/data/rd4cVVMM/images/0e569634c0e41c6391c337993ca69ede3a2deb07.jpg)

After your first launch, open `config/realsize.json` on the server or in your single-player installation. Change or add entries under `entityScales`:

```json
"entityScales": {
  "minecraft:spider": 1.4,
  "minecraft:polar_bear": 0.3
}
```

This is a section of the file, not a replacement config. `1.0` means normal size; smaller values shrink mobs and larger values enlarge them. The `floor`, `hostileFloor` and `cap` settings limit the result, so raise `cap` for truly giant mobs. Leave enough room before enlarging mobs in pens or buildings.

Run `/realsize reload` as an operator to apply your changes without restarting. Loaded mobs update immediately; others update when their chunks load. Mobs without a matching rule stay unchanged.

See the [configuration guide](CONFIGURATION.md) for all options, default sizes and entity-tag rules.

## Installation

For **Minecraft 26.3**, install RealSize and **Fabric API** with **Fabric Loader 0.19.5+** and **Java 25+**. Use Fabric API **0.162.0+26.3** or a later compatible release. Older Minecraft builds are available under **Versions**.

- **Single-player:** put both JARs in your Fabric client's `mods` folder.
- **Multiplayer:** install both on the server. Players don't need RealSize to join.
- **Optional client install:** keeps tiny mobs visible farther away and adjusts walking animations to their size. Without it, tiny mobs can disappear at short distances and animations may look uneven. Client-only installation doesn't enable resizing on servers without RealSize.

**Upgrading from an older version?** Back up your world and config, and remove the old JAR. Older releases saved permanent size changes; keep RealSize installed until affected chunks have loaded and saved before uninstalling. See the [upgrade guide](CONFIGURATION.md#upgrading-an-older-configuration).

## Bug reports

Open an issue with your Minecraft, Fabric Loader, Fabric API and RealSize versions,
the relevant config, logs and steps to reproduce.

For building and testing, see [Developing RealSize](DEVELOPMENT.md).
