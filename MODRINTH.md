# RealSize

RealSize changes Minecraft mobs to more believable proportions, with actual hitbox changes rather than just smaller or larger models. Bees become tiny, spiders are smaller, and iron golems are larger. The defaults work straight away, or you can choose your own sizes for your world.

## Different sizes, familiar mobs

- **Real size changes:** models and hitboxes resize together. Small spiders can fit through gaps their vanilla counterparts can't.
- **Your own size rules:** choose sizes for individual vanilla or modded mobs, or use entity-type tags. Mobs without a matching rule stay unchanged.
- **Existing worlds supported:** sizes apply to existing mobs as well as new spawns. Reload the config to update loaded mobs without restarting.
- **Movement that fits:** enlarged land mobs get proportional step height without RealSize's added step height reaching the height of a 1.5-block fence or wall. Higher vanilla or other-mod step heights are preserved; aquatic mobs don't get this boost.
- **Optional client improvements:** keep tiny mobs visible farther away and adjust walking-animation rhythm to the mob's size as it moves, without changing its movement speed.

There are no new items or controls to learn. Play normally and the sizes apply automatically. Each mob type has a fixed rule, not a random size for every individual.

## In the world

These screenshots were taken by OG moocow. See the Gallery tab for the full set of scenes.

### Bees

Bees use a quarter of their normal size by default.

![Tiny bees in a forest clearing](https://raw.githubusercontent.com/ff-tech-xyz/realsize-mod/update/V.1.2.0/docs/images/bees.jpg)

### Spiders

The default spider rule is `0.26`. Smaller hitboxes change which gaps they can pass through, so don't assume a vanilla spider-proof opening is still safe.

![Small spiders across a sandy clearing](https://raw.githubusercontent.com/ff-tech-xyz/realsize-mod/update/V.1.2.0/docs/images/spiders.jpg)

### Aquatic animals

Size rules also apply underwater. Aquatic mobs keep their normal step-height behavior.

![Nautiluses and fish above the seabed](https://raw.githubusercontent.com/ff-tech-xyz/realsize-mod/update/V.1.2.0/docs/images/aquatic-animals.jpg)

### Custom sizes

You're not limited to the default proportions. The giant spider and miniature polar bears below use custom configurations, not the defaults. To enlarge a mob beyond the default limit, raise `cap` as well as its size rule; floors still limit shrinking.

![A giant spider above a snowy forest at night — custom configuration](https://raw.githubusercontent.com/ff-tech-xyz/realsize-mod/update/V.1.2.0/docs/images/giant-spider.jpg)

![Miniature polar bears on snowy terrain — custom configuration](https://raw.githubusercontent.com/ff-tech-xyz/realsize-mod/update/V.1.2.0/docs/images/miniature-polar-bears.jpg)

## Before you install

The latest build is **RealSize 1.2.0**, the **v2 rebuild**, for **Minecraft 26.3**. It needs **Fabric Loader 0.19.5 or newer**, **Fabric API 0.162.0+26.3 or a later compatible release**, and **Java 25 or newer**. Older Minecraft builds remain available under Versions; the 26.3 JAR does not support earlier Minecraft releases, Forge or NeoForge.

Put RealSize and the matching Fabric API JAR in the `mods` folder. Remove the old RealSize JAR before upgrading so there is only one version installed.

Size changes affect combat, farms, pens and narrow spaces. Existing mobs aren't moved or deleted when you enlarge them. Leave enough room before changing sizes in confined areas.

**Upgrading from an older RealSize version? Back up your world and config first.** Older releases saved permanent modifiers. The rebuild cleans those as affected mobs load, but it cannot clean untouched chunks without loading them. Keep RealSize installed until affected chunks have loaded and been saved before uninstalling. See the [upgrade reference](https://github.com/ff-tech-xyz/realsize-mod/blob/update/V.1.2.0/CONFIGURATION.md#upgrading-an-older-configuration).

## What each installation does

### Server only

The server controls mob sizes. Players can join without RealSize or Fabric on their clients, unless another server mod requires them.

Sizing works, but players keep vanilla rendering and animations. Some resized mobs can look choppy or have an odd walking rhythm. Very small mobs may disappear from view after only a few blocks, so you might need to be right beside them to see them. The distance depends on the mob and its size.

### Client only

Installing RealSize on your client does not enable this mod's mob resizing on a multiplayer server that doesn't run it. You can join compatible vanilla servers normally, but RealSize does not change their mob sizes. The local rendering and walking-animation options do not give you control over a server's sizes.

### Client and server, or single-player

This gives you the full RealSize experience. The server applies sizes, while the client uses unscaled mob dimensions for rendering-distance checks. Tiny mobs no longer lose most of their viewing distance just because they're small, so you can see them much farther away. Normal visibility and server tracking limits still apply.

Walking-animation rhythm adjusts to the mob's actual size as it moves: smaller mobs cycle their legs faster and larger mobs cycle them slower. This can make resized mobs look more natural without changing their movement speed. It adjusts walking rhythm, not every attack or special animation.

Single-player runs both parts from the same JAR. Install RealSize and Fabric API in your Fabric client's `mods` folder.

## Configuration

RealSize creates `config/realsize.json` on first launch. Edit the server's file for multiplayer sizes, or your own installation's file for single-player.

Add size rules under `entityScales` using full entity IDs. For example, these entries make bees larger than the default tiny bees and add a cow rule:

```json
"entityScales": {
  "minecraft:bee": 0.4,
  "minecraft:cow": 0.8
}
```

This is a section of the file, not a complete replacement config. You can add other vanilla or installed modded mobs using their actual registry IDs. The generated defaults aren't a complete mob list or a whitelist. **No matching entry or configured tag means RealSize leaves that mob's size and step height alone.** Deleted defaults are not added back. An exact `1.0` rule disables resizing for that type, including a type otherwise selected by a tag.

Run `/realsize reload` with operator permission level 2 to apply changes. Loaded mobs update immediately; mobs in unloaded chunks update when those chunks load. A failed reload reports the problem and keeps the previous working settings.

The optional client fixes are local settings: `clientRenderDistanceFix` and `clientWalkTempoFix`. Both default to `true`. Set either to `false` in your client's file and run `/realsize-client reload` to disable it. Client settings can't override the server's mob sizes.

Every option, default size, tag rule and migration detail is in the [configuration reference](https://github.com/ff-tech-xyz/realsize-mod/blob/update/V.1.2.0/CONFIGURATION.md).

[Source code](https://github.com/ff-tech-xyz/realsize-mod) · [Issues and suggestions](https://github.com/ff-tech-xyz/realsize-mod/issues) · [PyreHaven Discord](https://discord.gg/tZ6Hx2ETA3)

Made by PyreHaven. Find out more about us at [PyreHaven.xyz](https://pyrehaven.xyz).
