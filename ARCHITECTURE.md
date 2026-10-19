# RealSize v2 architecture

This document describes the finished rebuild in present tense. Implementation
status and evidence belong in [FIXES.md](FIXES.md). It supersedes the previous
server-tracking, merged-default, restart-only, and additive-step contracts.
No source implementation is created or approved by this documentation change.
The rebuild label is v2; `mod_version` remains `1.2.0` pending a separate decision.

## Product boundary

One Fabric project targets Minecraft 26.3 and Java 25. The same JAR provides
server-owned mob sizing and an optional, isolated client rendering correction.
Vanilla clients can join. The server has no RealSize tracking hooks or tracking
mixins, custom networking, or client-required registries. Its only permitted
spawn mixins adapt only the existing early type-bounds checks in `NaturalSpawner`
and `SpawnUtil` to the configured adult scale. Elijah's correction in Discord
`1561148464233054261` supersedes the former finalization/containment design.
Vanilla keeps its check order, retries, collision-check toggle and finalization.
Babies, variants and riders do not trigger a new check after finalization.
`EntitySizing` owns the adult-scale calculation; no entity-load veto or tracking
replacement is added.
One server-side baby-state adapter observes changed baby metadata on AgeableMob,
Zombie, Piglin and Zoglin and delegates loaded entities to EntitySizing. It does
not poll worlds or change vanilla age progression, speed, or animation state.
The client component does
not invent entities outside vanilla server tracking.

## Source layout

This is the completed-state layout, not a claim that the source files already
exist. Each file is created only in its approved implementation phase.

```text
realsize-mod/
├── README.md
├── CONFIGURATION.md
├── DEVELOPMENT.md
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
    │   │   ├── mixin/
    │   │   │   ├── BabyStateMixin.java
    │   │   │   ├── TurnRateMixin.java
    │   │   │   ├── SpawnUtilMixin.java
    │   │   │   └── NaturalSpawnerMixin.java
    │   │   └── sizing/
    │   │       ├── SizingPolicy.java
    │   │       └── EntitySizing.java
    │   └── resources/
    │       ├── fabric.mod.json
    │       ├── realsize.spawn.mixins.json
    │       ├── realsize.client.mixins.json
    │       └── assets/realsize/icon.png
    ├── client/
    │   └── java/xyz/pyrehaven/realsize/client/
    │       ├── RealSizeClient.java
    │       └── mixin/
    │           ├── EntityRenderDistanceMixin.java
    │           └── WalkTempoMixin.java
    ├── test/
    │   └── java/xyz/pyrehaven/realsize/
    │       ├── ScaffoldTest.java
    │       ├── config/ConfigFileTest.java
    │       ├── config/ResolvedSettingsTest.java
    │       └── sizing/SizingPolicyTest.java
    └── gametest/
        ├── java/xyz/pyrehaven/realsize/
        │   ├── ScaffoldGameTests.java
        │   ├── LifecycleGameTests.java
        │   ├── TurningGameTests.java
        │   ├── ResolvedSettingsGameTests.java
        │   ├── SizingGameTests.java
        │   ├── SpawnGameTests.java
        │   ├── ChunkSpawnGameTests.java
        │   ├── SpawnDimensionGameTests.java
        │   ├── ClientRenderGameTests.java
        │   ├── NaturalSpawnProbe.java
        │   └── testmixin/
        │       ├── NaturalSpawnProbeMixin.java
        │       └── OuterSpawnProbeMixin.java
        └── resources/
            ├── fabric.mod.json
            ├── realsize-test.mixins.json
            └── data/realsize-test/tags/entity_type/
                ├── only_bee.json
                ├── mixed.json
                └── empty.json
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
- `EntitySizing.spawnBounds` scales vanilla's supplied adult spawn bounds with
  the existing policy and resolved snapshot. The two placement adapters change
  only the argument to vanilla's existing type-bounds collision checks, before
  candidate finalization. They do not mutate candidates or track preparation.
  The composition root supplies its current owner, including after reload.
- `RealSizeClient` owns client-local options and the client-only render adapter.
  No common initializer imports a client class.

Dependencies run from composition and Minecraft adapters to config and pure
policy. No policy imports entrypoints. The client render adapter reads actual
entity scale and unscaled dimensions; it does not duplicate server rule lookup.

## Configuration and reload

`config/realsize.json` has `configVersion: 2`. The schema and examples in the
[configuration guide](CONFIGURATION.md) are the product contract. Required fields
are explicit; the entity map
is authoritative, including an empty map. Defaults are generated only for a
missing file. Deleted rules stay deleted. Exact IDs beat tags; conflicting tag
rules are errors. Unknown well-formed IDs/tags are warned with their names and
retained on disk. Invalid syntax, non-finite/nonpositive scales, incompatible
floors/cap, and invalid step limits are errors, not replacements.

Legacy migration preserves explicit entity entries, translates the scale bounds,
adds an explicit hostile floor equal to the legacy floor and proportional movement settings, and removes old
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

The default lower bound is `0.22` for both ordinary and hostile mobs; the upper
bound is `1.45`. First-run scales retain v1's table, including the raw cave spider
rule `0.20` (effective `0.22` after the floor). A higher hostile floor is opt-in,
not a silent change to v1's default sizes. Existing v2 files remain authoritative.
For active non-neutral rules, the floor measures final size against unscaled
adult type dimensions, so baby factors cannot compound below it. Neutral `1.0`
and absent rules take precedence: they remove owned modifiers and preserve
vanilla baby size, even below a category floor. Growth recalculates the owned modifier through
the same owner when baby metadata changes; there is no every-tick entity scan.
Only loaded server entities are reconciled by the adapter; construction and disk
load are handled by the entity-load callback. Foreign modifiers remain untouched; another mod deliberately
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

The cap also remains below `1.5` after Minecraft's float conversion: a configured
double closer to `1.5` than a float can represent uses `Math.nextDown(1.5f)` as
the effective owned ceiling. A total attribute multiplier applies only the
effective difference from the foreign-inclusive baseline; an additive amount
would itself be multiplied by foreign modifiers. On lifecycle changes the owner
removes its stable step modifier, reads Minecraft's effective attribute value,
then applies the new modifier. It does not duplicate attribute mathematics.
Aquatic exclusions include water
categories and aquatic families whose categories are MONSTER/MISC (guardians,
nautiluses, and fish including tadpoles). A higher foreign/vanilla baseline is
still preserved, including a camel's vanilla `1.5` step.

### Early spawn bounds only

`NaturalSpawnerMixin` wraps the existing `EntityType.getSpawnAABB(DDD)` calls
in ticking and chunk-generation placement. `SpawnUtilMixin` wraps that same
call in `trySpawnMob`; when its collision toggle is false, vanilla skips the
call and RealSize does not add one. Both adapters pass the original bounds to
`EntitySizing.spawnBounds`, which applies the existing policy's configured
adult multiplier around the original horizontal center and bottom Y.
Neutral and absent rules return the original bounds unchanged. Vanilla's spawn-
dimension factors remain part of the supplied bounds.

No candidate is created early, finalized twice, resized for checking, moved,
rolled back or held in thread-local preparation state. All later vanilla checks,
finalizers, group changes, companions, registrations and callbacks retain their
original order. No post-finalization gate, ASM plugin or species-specific hook
is permitted. Vanilla's own baby/variant/rider fit quirks remain unchanged.

Transformed GameTests cover early rejection before finalization, accepted
configured adult bounds, unchanged retries, neutral/smaller rules and a late
finalizer change that vanilla accepts without another collision gate. Commands,
loaded mobs and reload do not acquire placement vetoes. Earlier finalized-spawn
verification in FIXES.md is historical evidence for a superseded design, not a
requirement to restore it.

## Client rendering

Resized mobs use an optional client-local walk rhythm correction. The required
`WalkTempoMixin` changes only the position-scale argument of the vanilla
`updateWalkAnimation(F)V` call to `WalkAnimationState.update(FFF)V` in LivingEntity
and the three 26.3 overrides in Camel, Frog and Creaking. One shared handler uses
`min(vanillaTempo / getScale(), 6)` for positive mob attribute scales: smaller
mobs cycle faster, larger mobs cycle slower and scale one retains vanilla tempo.
It reads the effective synced scale, not configured defaults or a species table,
so arbitrary config reloads and foreign scale modifiers use the same formula.
Attribute scale excludes the age factor; vanilla's baby tempo is already in the
argument and is not applied twice. Swing amplitude, per-species smoothing and
walking suppression while sitting/dashing/jumping remain vanilla. Movement speed,
players, attacks and separately controlled action animations are unchanged.
Each target requires exactly one injection. Client tests cover actual golem
render-state/model consumption after server config reloads, arbitrary scale
samples and all three override-specific walking states.
`clientWalkTempoFix` defaults to true, including when absent in older v2 files;
an explicit value must be boolean. It shares the existing validated local reload
without replacing the server snapshot. Invalid reloads retain both active options.
Vanilla clients retain vanilla leg rhythm; no server animation packets are added.

The common required `TurnRateMixin` limits only the argument to the existing
`MoveControl.tick()` yaw interpolation to `min(vanillaLimit, 35)` for shrunk
server mobs. Neutral/enlarged mobs, client-side control, other control methods
and movement speed are unchanged. Specialized controls overriding tick without
calling this implementation are not covered. `TurningGameTests` verifies actual
transformed yaw, speed preservation and small-spider navigation. No entity state
map, controller replacement or every-tick sizing reconciliation is introduced.

The optional client mixin corrects distance culling using unscaled dimensions
for shrinking entities, without shortening vanilla distance for enlarged ones.
It preserves frustum/visibility checks and vanilla server tracking. It operates
on the entity's actual scale, including when its client has no matching server
configuration. The client's local toggle disables the correction cleanly.
`RealSizeClient` reads the existing local config once during client startup;
`/realsize-client reload` validates and replaces only its local rendering options.
It neither resolves entity rules nor changes the integrated/server sizing snapshot.
Invalid local reloads retain the previous options. The distance adapter adds an
unscaled bounding-box threshold only for living entities whose actual SCALE is
below one, preserving a successful vanilla result and the existing view scale.
No rendering call reads the config file. Client GameTests exercise transformed
entities through real integrated and dedicated connections; their entrypoint is
development-only and is never packaged in the production JAR.

Supported acceptance modes are multiplayer with RealSize on server and client,
and single-player with RealSize on the client and its integrated server. With
RealSize only on a dedicated server, all server features remain available to
vanilla clients; the client culling and walk-tempo corrections are absent. The integrated
server uses the same sizing lifecycle as a dedicated server, not a second path.

Use MixinExtras `@ModifyReturnValue` if the verified 26.3 target supports it.
Prove the target descriptor and injection execution with transformed tests and
a real client. Fail closed for the required client injection. Register it only
in client metadata/mixin configuration; a dedicated server loads none of it.

## Build and package

Scaffold tests verify the installation contract before gameplay exists:
`ScaffoldTest` inspects the real packaged artifact, `ScaffoldGameTests` exercises
Loader discovery and a vanilla mob on the Minecraft test server, and
`scripts/server-smoke.py` boots/stops the packaged dedicated server and checks
its JVM class-load trace for client isolation. GameTest metadata belongs to the
separate development source set and never ships in the production JAR.

One root project owns all sources, dev runtime, resources, and tests. Version
pins live in `gradle.properties`, with checksum-pinned Gradle distribution.
The existing non-remapping Loom setup has no mappings line, identity JAR, shared
module, or `compileOnly` project dependency. Java is 25.

The finished metadata allows both environments (`environment: "*"`), declares
separate main/client entrypoints, exact 26.3 compatibility, Java 25, required
Fabric dependencies, the bounded spawn-placement mixins and isolated client
mixins. Required spawn injections fail closed when their verified targets change.
Contact metadata names the actual
`ff-tech-xyz/realsize-mod` source and issue tracker. Reuse the retained icon and
license. Modrinth metadata corrections require separately approved deployment.

`runServer` includes the real mod classes. The packaged smoke probe excludes
production source outputs and test mods, then checks JAR class-load provenance.
`runClientGameTest` also excludes production source outputs while retaining the
separate development tests; its integrated and dedicated connections exercise
the JAR's common and client owners. CI builds and tests that artifact with
nonempty JUnit/server GameTests, packaged dedicated boot and Xvfb client checks,
and retains the tested JAR and SHA-256 without a release. No empty `NO-SOURCE`
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

Elijah has approved implementation of this update. The original build used one
Kanban scaffold worker followed by separate sequential feature workers. His
explicit takeover request `1561151136940032071` stops that autonomous chain;
the early-check correction is implemented directly in the originating thread. Each worker exercises its deliverable, obtains independent review,
updates the acceptance evidence, and commits before completing its task and
releasing the next worker. Scaffold includes only executable build/test/runtime
groundwork, not placeholder owners or bundled feature implementations. Features
are configuration/migration, registry resolution, sizing policy, entity lifecycle,
reload, movement, natural spawn clearance, and optional client culling. Final
validation follows those commits. A blocked architecture or verification gate
stops the chain; it is not silently waived. Approval to build does not authorize
a merge, release, or external player-facing documentation deployment.
