# RealSize 1.2.0 architecture

Status: proposed rebuild, not implemented. This branch deliberately contains no
mod entrypoint, mixins, production classes, test classes, or mod metadata yet.
The legacy implementation was removed in the preceding commit. The original
README is retained as a product/configuration reference, not a claim that this
branch currently ships a working mod.

## Scope

Rebuild RealSize as one server-side Fabric mod targeting Minecraft 26.3 and Java
25. Preserve the product: configurable mob scale, extended tracking for tiny
mobs, and extra step height for large mobs. Vanilla clients must be able to join;
no client renderer, packets, synced custom registries, or client entrypoint.

The old 1.21 and 26.x multi-module layout is not carried forward. Backports need
an explicit later decision, not duplicate adapters hidden in this rebuild.
The Gradle-only groundwork may resolve Minecraft and Fabric and run an empty
build; it must not create a pretend installable mod.

## Proposed scaffold

Every path below src is a planned file. None is created by this document or by
the build-tool upgrade. Only create files as their owning phase is approved.

```text
realsize-mod/
├── ARCHITECTURE.md
├── README.md
├── LICENSE
├── branding/
│   └── icon.png
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/xyz/pyrehaven/realsize/
    │   │   ├── RealSizeMod.java
    │   │   ├── config/
    │   │   │   ├── RealSizeConfig.java
    │   │   │   ├── ConfigFile.java
    │   │   │   └── DefaultScales.java
    │   │   ├── sizing/
    │   │   │   ├── SizingPolicy.java
    │   │   │   └── EntitySizing.java
    │   │   └── mixin/
    │   │       └── EntityTrackingMixin.java
    │   └── resources/
    │       ├── fabric.mod.json
    │       ├── realsize.mixins.json
    │       └── assets/realsize/icon.png
    └── test/
        └── java/xyz/pyrehaven/realsize/
            ├── config/ConfigFileTest.java
            ├── sizing/SizingPolicyTest.java
            ├── sizing/EntitySizingTest.java
            └── mixin/EntityTrackingTest.java
```

## Ownership and dependency direction

- `RealSizeMod` is the composition root. It reads one config snapshot, constructs
  the sizing owner, and registers one server entity-load callback. It contains
  no sizing arithmetic or file parsing. Tracking reads the same immutable
  snapshot through a narrow accessor; no mutable global config manager.
- `RealSizeConfig` owns validated immutable values. `DefaultScales` owns the
  reviewed registry-ID default table. `ConfigFile` alone owns JSON decoding,
  creation, field validation, diagnostics, and legacy-field compatibility.
- `SizingPolicy` owns selection, clamping, and thresholds as pure decisions.
  It has no Minecraft entity, file, event, logger, or Fabric dependency.
- `EntitySizing` owns applying and removing RealSize's scale and step-height
  modifiers. It calls the policy and Minecraft attributes directly. No duplicate
  entity-state map, retry callback, tick scan, or fallback sizing path.
- `EntityTrackingMixin` adapts the server's effective tracking-range calculation
  to the policy. It never reads files or changes sizing. Prefer one effective
  range injection rather than also patching the entity-type global range.
  Confirm the actual 26.3 method and descriptor before implementation; add a
  second hook only if a demonstrated upstream cap prevents the single owner
  from satisfying the tracking contract, and revise this document first.

Dependencies point from Fabric composition/mixins to sizing and config. Config
and policy never import the entrypoint or mixin classes. Only the composition
root publishes the immutable policy needed by the tracking injection.

## Configuration contract

Keep `config/realsize.json` and the existing fields: `floor`, `cap`,
`trackingRangeThreshold`, `minTrackingRangeChunks`,
`minTrackingDistanceBlocks`, `stepHeightBoostThreshold`,
`stepHeightBoostAmount`, and `entityScales`. Missing fields use documented
legacy defaults: 0.22, 1.45, 0.6, 10, 128, 1.1, and 0.5 respectively.
Recover the default entity table from the pre-cleanup commit when phase 1 starts;
review it against the 26.3 registry rather than silently inventing new values.

Reject non-finite numbers, nonpositive scales/limits, negative step boosts,
nonpositive tracking limits, malformed IDs, and floor greater than cap. An
invalid existing file causes an actionable startup error with path and field;
never overwrite it or quietly replace it with defaults. Unknown but well-formed
entity IDs stay in the file and receive a diagnostic after registry lookup.
Create defaults only when the file is missing. Config changes require restart;
there is no reload command in this scope.

Merge per-ID overrides over defaults to preserve the existing config meaning.
An entity absent from the merged table is untouched. An explicit scale of 1.0
means neutral RealSize scaling, not exemption from reconciliation. Players and
non-living entities remain excluded even if listed. Define tiny as effective
configured scale < trackingRangeThreshold, and large as effective configured
scale >= stepHeightBoostThreshold, matching the README rather than the old
code's inconsistent strict comparison.

## Runtime invariants

1. One validated snapshot supplies both attribute and tracking decisions for
   the server lifetime. No I/O occurs inside entity events or mixins.
2. For an opted-in mob, clamp the configured scale once. Apply a stable
   `realsize:scale` modifier relative to the base attribute, without rewriting
   the base or removing another mod's modifier.
3. Entity load reconciles RealSize's named modifiers, including saved ones:
   update existing values, add missing ones, remove obsolete owned step boosts,
   and remove stale owned modifiers when a previously configured mob becomes
   unconfigured. Repeated loads must not stack modifiers. Check compatibility
   with other modifiers and baby growth in transformed-entity tests.
4. Step height adds only `realsize:step_height`, using the policy's inclusive
   threshold. Missing attributes are skipped with a bounded diagnostic, not
   emulated by a second subsystem.
5. For eligible tiny mobs, effective tracking range is at least the configured
   block minimum and the chunk minimum converted to blocks; never reduce
   vanilla range. Guard integer overflow during validation/conversion.
   Server view-distance and vanilla-client rendering limits still apply; the
   mod does not promise unlimited visibility.
6. Every required mixin injection is fail-closed. Compilation alone does not
   prove its target exists; verify ownership, descriptor, and injection count
   against the resolved Minecraft JAR and transformed target.

## Build ownership

One root Gradle project owns the Minecraft/Fabric dependencies, Java toolchain,
resource expansion, tests, and eventual artifact. Versions live only in
`gradle.properties`, with a released Loom pin, released Loader/API pins, and a
checksum-pinned Gradle wrapper. Minecraft 26.3 uses the non-remapping
`net.fabricmc.fabric-loom` plugin: no Yarn dependency or identity-mapping JARs.
Keep generated build output and run directories outside source control.

Before scaffolding, `clean build` verifies build-tool setup and dependency
resolution only. Empty test/source tasks are not gameplay tests. No publish task
or release automation is introduced. Once metadata exists, it must declare
server environment, exact 26.3 compatibility, Java 25, required Fabric modules,
and no client-side dependencies. Reuse the preserved icon then, not now.

## Implementation phases and acceptance

1. **Config and policy:** create only their proposed files and focused tests.
   Prove missing-file creation, legacy defaults/overrides, invalid-file
   preservation, boundary equality, non-finite rejection, overflow rejection,
   unconfigured exclusion, and immutable config. No Minecraft boot required.
2. **Attribute lifecycle:** add entrypoint, metadata, sizing adapter, and
   transformed-entity tests. Prove scale/step attributes on adult and baby mobs,
   load/reload idempotence, neutral scale, stale modifier removal, missing
   attributes, and coexistence with a foreign modifier. One event owner.
3. **Tracking:** inspect the 26.3 server JAR, add the single verified injection
   and transformed target test. Prove vanilla range is never reduced, both
   minima apply, equality is not tiny, and unconfigured/player entities retain
   vanilla tracking. Verify the required injection actually ran.
4. **Live validation and docs:** run a disposable dedicated server and connect
   an unmodded client. Check tiny bees at range, oversized mobs stepping,
   reconnect/chunk reload, existing JSON migration, and clean startup. Update
   the README from preserved legacy reference to verified 1.2.0 behavior.
   Inspect the packaged metadata, mixin config, license, and icon. Report any
   unperformed connected-player checks explicitly.

Each phase needs Elijah's approval before implementation. This request
approves only cleanup, this architecture proposal, and Gradle setup, in that
commit order. It does not approve scaffolding or a release.

## References checked for groundwork

- Fabric's 26.3 example branch: `FabricMC/fabric-example-mod`, branch `26.3`.
- Fabric Loom docs: https://docs.fabricmc.net/develop/loom/
- Fabric 26.3 guidance: https://fabricmc.net/2026/09/15/263.html
- Live Loader metadata: https://meta.fabricmc.net/v2/versions/loader/26.3
- Fabric Maven release metadata for Loom and Fabric API.

Exact build pins and observed groundwork verification belong in the README
when the separate build setup commit is complete.
