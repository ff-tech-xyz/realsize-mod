# RealSize v2 architecture

This document describes the finished rebuild in present tense. Implementation
status and evidence belong in [FIXES.md](FIXES.md). It supersedes the previous
server-tracking, merged-default, restart-only, and additive-step contracts.
No source implementation is created or approved by this documentation change.
The rebuild label is v2; `mod_version` remains `1.2.0` pending a separate decision.

## Product boundary

One Fabric project targets Minecraft 26.3 and Java 25. The same JAR provides
server-owned mob sizing and an optional, isolated client rendering correction.
Vanilla clients can join. The server has no RealSize tracking hooks or mixins,
custom networking, or client-required registries. The client component does
not invent entities outside vanilla server tracking.

## Source layout

This is the completed-state layout, not a claim that the source files already
exist. Each file is created only in its approved implementation phase.

```text
realsize-mod/
├── README.md
├── ARCHITECTURE.md
├── FIXES.md
├── LEGACY_README.md
├── LICENSE
├── .gitignore
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── branding/
│   └── icon.png
├── scripts/
│   └── server-smoke.py
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── .github/
│   └── workflows/
│       └── build.yml
└── src/
    ├── main/
    │   ├── java/xyz/pyrehaven/realsize/
    │   │   ├── RealSizeMod.java
    │   │   ├── config/
    │   │   │   ├── ConfigFile.java
    │   │   │   ├── RealSizeConfig.java
    │   │   │   ├── DefaultScales.java
    │   │   │   └── ResolvedSettings.java
    │   │   └── sizing/
    │   │       ├── SizingPolicy.java
    │   │       ├── EntitySizing.java
    │   │       └── SpawnSizing.java
    │   └── resources/
    │       ├── fabric.mod.json
    │       ├── realsize.client.mixins.json
    │       └── assets/realsize/icon.png
    ├── client/
    │   └── java/xyz/pyrehaven/realsize/client/
    │       ├── RealSizeClient.java
    │       └── mixin/EntityRenderDistanceMixin.java
    ├── test/
    │   └── java/xyz/pyrehaven/realsize/
    │       ├── ScaffoldTest.java
    │       ├── config/ConfigFileTest.java
    │       ├── config/ResolvedSettingsTest.java
    │       └── sizing/SizingPolicyTest.java
    └── gametest/
        ├── java/xyz/pyrehaven/realsize/
        │   ├── ScaffoldGameTests.java
        │   ├── SizingGameTests.java
        │   └── SpawnGameTests.java
        └── resources/
            └── fabric.mod.json
```

## Owners

- `RealSizeMod` is the composition root: lifecycle registration, reload command,
  and wiring. It performs no parsing or sizing arithmetic.
- `ConfigFile` owns JSON, schema migration, backups, validation, and diagnostics.
  `RealSizeConfig` is immutable. `DefaultScales` owns the reviewed first-run
  table, not a table re-merged on every read.
- `ResolvedSettings` resolves exact IDs and tags once against the server registry
  into immutable per-EntityType settings with identity-keyed lookup. Registry
  or tag reload rebuilds this snapshot before sizing reconciliation. No entity
  event or rendering loop converts registry IDs to strings or reads files.
- `SizingPolicy` owns category floors, baby-aware final-size limits, and step
  decisions as pure calculations. It has no Fabric, file, or entity dependency.
- `EntitySizing` owns applying, updating, and removing RealSize attribute
  modifiers. Entity load, growth, configuration reload, and dimension checks
  use that owner, not parallel maps or fallback handlers. Lifecycle integration
  is verified against 26.3 before selecting its concrete callbacks.
- `SpawnSizing` uses the same resolved policy and dimensions to reject enlarged
  natural placements that collide. It does not maintain a second sizing model.
- `RealSizeClient` owns client-local options and the client-only render adapter.
  No common initializer imports a client class.

Dependencies run from composition and Minecraft adapters to config and pure
policy. No policy imports entrypoints. The client render adapter reads actual
entity scale and unscaled dimensions; it does not duplicate server rule lookup.

## Configuration and reload

`config/realsize.json` has `configVersion: 2`. The schema and examples in the
README are the product contract. Required fields are explicit; the entity map
is authoritative, including an empty map. Defaults are generated only for a
missing file. Deleted rules stay deleted. Exact IDs beat tags; conflicting tag
rules are errors. Unknown well-formed IDs/tags are warned with their names and
retained on disk. Invalid syntax, non-finite/nonpositive scales, incompatible
floors/cap, and invalid step limits are errors, not replacements.

Legacy migration preserves explicit entity entries, translates the scale bounds,
adds the v2 hostile floor and proportional movement settings, and removes old
tracking and flat-step fields with an explicit migration diagnostic. It does
not pretend those obsolete fields still affect behavior. A validated migration
backs up the original before atomic replacement; failure leaves it intact.

Initial startup validates and resolves before enabling sizing. Permission-level-2
`/realsize reload` parses and resolves a replacement off the active snapshot,
then commits it and reconciles loaded mobs on the server thread. Invalid input
keeps the active snapshot untouched. Unloaded mobs use the new snapshot when
loaded. Tag reload shares this resolution/reconciliation path.

## Scale lifecycle

The owner removes old persistent `realsize:*` attribute modifiers on load,
including legacy scale and step modifiers, then applies only transient modifiers
with stable IDs. It never changes base attributes or foreign modifiers.
Neutral `1.0`, deleted rules, and excluded entities remove owned modifiers
instead of returning early. Repeated reconciliation is idempotent.

The default lower bound is `0.22`; hostiles use at least `0.6`; the upper bound
is `1.45`. These are v2 target defaults, not observations of tested gameplay.
For active non-neutral rules, the floor measures final size against unscaled
adult type dimensions, so baby factors cannot compound below it. Neutral `1.0`
and absent rules take precedence: they remove owned modifiers and preserve
vanilla baby size, even below a category floor. Growth recalculates the owned modifier through
the same owner. Foreign modifiers remain untouched; another mod deliberately
changing final size is outside RealSize's guaranteed bounds and is diagnosed
when it prevents the contract from being met.

Transient modifiers are not serialized. Uninstalling stops new sizing; it cannot
repair legacy persistent data in mobs never loaded during migration. The docs
state that limitation instead of claiming uninstall performs a world scan.

## Movement and spawning

For eligible land mobs, let `baseline` be step height without RealSize's own
modifier and `scale` be the final relative size. The desired value is:

`max(baseline, min(baseline * max(1, scale), maxStepHeight))`

Default `maxStepHeight` is `1.49` and must be strictly below `1.5`. Only the
difference from baseline is RealSize's transient modifier. No strict-threshold
or flat `+0.5` behavior remains; aquatic mobs are excluded. Existing baseline
values above the cap are preserved, not reduced. Tests must verify actual
fence/wall collision, not rely on the number alone.

Natural spawn clearance uses scaled final dimensions before placement succeeds.
The concrete 26.3 placement seam must be proved in an implementation spike.
If Fabric cannot provide a verified hook without a server mixin, revise the
architecture for approval rather than quietly breaking the zero-server-mixin
contract. Config changes and existing mobs do not trigger teleport/deletion.

## Client rendering

The optional client mixin corrects distance culling using unscaled dimensions
for shrinking entities, without shortening vanilla distance for enlarged ones.
It preserves frustum/visibility checks and vanilla server tracking. It operates
on the entity's actual scale, including when its client has no matching server
configuration. The client's local toggle disables the correction cleanly.

Supported acceptance modes are multiplayer with RealSize on server and client,
and single-player with RealSize on the client and its integrated server. With
RealSize only on a dedicated server, all server features remain available to
vanilla clients; the client culling correction alone is absent. The integrated
server uses the same sizing lifecycle as a dedicated server, not a second path.

Use MixinExtras `@ModifyReturnValue` if the verified 26.3 target supports it.
Prove the target descriptor and injection execution with transformed tests and
a real client. Fail closed for the required client injection. Register it only
in client metadata/mixin configuration; a dedicated server loads none of it.

## Build and package

Scaffold tests verify the installation contract before gameplay exists:
`ScaffoldTest` inspects the real packaged artifact, `ScaffoldGameTests` exercises
Loader discovery and a vanilla mob on the Minecraft test server, and
`scripts/server-smoke.py` boots/stops the normal dedicated dev server and checks
its JVM class-load trace for client isolation. GameTest metadata belongs to the
separate development source set and never ships in the production JAR.

One root project owns all sources, dev runtime, resources, and tests. Version
pins live in `gradle.properties`, with checksum-pinned Gradle distribution.
The existing non-remapping Loom setup has no mappings line, identity JAR, shared
module, or `compileOnly` project dependency. Java is 25.

The finished metadata allows both environments (`environment: "*"`), declares
separate main/client entrypoints, exact 26.3 compatibility, Java 25, required
Fabric dependencies, and only client mixins. Contact metadata names the actual
`ff-tech-xyz/realsize-mod` source and issue tracker. Reuse the retained icon and
license. Modrinth metadata corrections require separately approved deployment.

`runServer` includes the real mod classes. CI builds and tests the same artifact
with a dedicated-server boot and real Minecraft GameTests. No empty `NO-SOURCE`
result counts as behavioral verification; no 1.21 compatibility range is claimed.

## Implementation phases

1. **Config and policy:** versioned authoritative config, explicit migration,
   ID/tag resolution, category/baby limits, proportional movement. Pure tests
   cover error preservation, precedence, deletion, neutral scale, conflicting
   tags, and immutable snapshot replacement.
2. **Server lifecycle:** transient attributes, legacy cleanup, load/growth/reload
   reconciliation, metadata, dev runtime, and scaled natural-spawn clearance.
   GameTests cover actual mobs, foreign attributes, save/load, and collisions.
   Resolve the spawn-seam architecture gate before committing its adapter.
3. **Optional client:** isolated entrypoint, verified render mixin, local toggle,
   and transformed/client checks. Dedicated-server boot proves isolation.
4. **Release validation:** CI, artifact inspection, real server with vanilla and
   modded clients, config/world migration, metadata link corrections, and tested
   build/install instructions. Record remaining unperformed checks explicitly.

Elijah has approved implementation of this update. Execution begins with one
Kanban scaffold worker, followed by one separate worker per feature, strictly
sequentially. Each worker exercises its deliverable, obtains independent review,
updates the acceptance evidence, and commits before completing its task and
releasing the next worker. Scaffold includes only executable build/test/runtime
groundwork, not placeholder owners or bundled feature implementations. Features
are configuration/migration, registry resolution, sizing policy, entity lifecycle,
reload, movement, natural spawn clearance, and optional client culling. Final
validation follows those commits. A blocked architecture or verification gate
stops the chain; it is not silently waived. Approval to build does not authorize
a merge, release, or external player-facing documentation deployment.
